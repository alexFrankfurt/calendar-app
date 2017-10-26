import models.CalendarRepository
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._

class ApplicationSpec extends PlaySpec with GuiceOneAppPerSuite {


  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/fakeurl")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "HomeController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Sign In with Google")
    }

  }

  "PersonAddition" should {

    "be successfull" in {
      val h = Headers(("Access-Control-Allow-Credentials", "true"))
      val a = route(app, FakeRequest(POST, "/persons?notme=true").withHeaders(h)
        .withFormUrlEncodedBody(("name", "Eve"), ("position", "Test Automation Engineer"))).get

      redirectLocation(a) mustBe Some("/")

    }
  }

  "PersonRetrieval" should {
    "return list of persons" in {

      val h = Headers(("Access-Control-Allow-Credentials", "true"))
      val r = route(app, FakeRequest(GET,"/persons?order=asc").withHeaders(h)).get

      play.api.Logger.debug(contentAsString(r))
      contentType(r) mustBe Some("text/plain")
    }

    "retrieve right person by id" in {
      val i = app.injector.instanceOf(classOf[CalendarRepository])

      val p = Await.result( i.getPerson(BSONObjectID("59f1e7bcf2e14f9078947d6b")), 5 seconds)
      p.get.name mustBe "Matthew A. Carroll"
    }
  }

  "VacationAddition" should {

    // Максимальное количество дней отпуска в году - 24 календарных дня
    "not haappen because of length" in {
      val h = Headers(("Access-Control-Allow-Credentials", "true"))
      val a = route(app, FakeRequest(POST, "/vacations").withHeaders(h)
        .withFormUrlEncodedBody(("uid", "0301934321321423423"), ("startDate", "2017-11-01"), ("endDate", "2017-11-30"))).get

      contentAsString(a) must include("Conditions didn't met")
    }

    // Минимальный период между периодами отпуска равен размеру первого отпуска
    "not happen because of closeness" in {
      val h = Headers(("Access-Control-Allow-Credentials", "true"))
      val a = route(app, FakeRequest(POST, "/vacations").withHeaders(h)
        .withFormUrlEncodedBody(("uid", "234790817438917947"), ("startDate", "2017-10-25"), ("endDate", "2017-10-29"))).get

      contentAsString(a) must include("Conditions didn't met")
    }

    // В отпуске имеют право находиться не более 50% сотрудников одной должности
    "not happen because of persons position" in {
      val h = Headers(("Access-Control-Allow-Credentials", "true"))
      val a = route(app, FakeRequest(POST, "/vacations").withHeaders(h)
        .withFormUrlEncodedBody(("uid", "12189321838919038128888"), ("startDate", "2017-10-25"), ("endDate", "2017-10-29"))).get

      contentAsString(a) must include("Conditions didn't met")
    }
  }

}
