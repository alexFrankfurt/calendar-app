package controllers

import java.time.Instant
import javax.inject.Inject

import models.CalendarRepository
import play.api.libs.json.Reads._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.twirl.api.Html
import util.AuthAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppController @Inject()(cc: ControllerComponents,
                              ws: WSClient,
                              calenarRepo: CalendarRepository,
                              authAction: AuthAction)(implicit val af: AssetsFinder) extends AbstractController(cc) {

  def index() = Action {
    Ok(views.html.index(Html("")))
  }

  def tokenSignIn() = Action.async { request =>
    val idToken = request.body.asFormUrlEncoded.flatMap {
      _.get("idtoken").flatMap(_.headOption)
    }

    request.session.get("UID") match {
      case Some(uid) => Future(Ok("/manage"))
      case None =>

        idToken match {
          case None => Future(Ok(views.html.index()))
          case Some(idtoken) =>
            val resp = ws.url("https://www.googleapis.com/oauth2/v3/tokeninfo").withQueryStringParameters(
              "id_token" -> idtoken
            ).get().map { response =>
              (response.json \ "sub").as[String]
            }

            resp.flatMap { uid =>
              calenarRepo.personExists(uid).flatMap{
                case None => Future(Ok("/signup").withSession("UID" -> uid))
                case Some(person) => Future(Ok("/manage").withSession("UID" -> uid))
              }
            }
        }
    }
  }

  def signOut() = Action(Ok("Signed out").withNewSession)

  def personsPanel() = authAction {
    Ok(views.html.panels.persons())
  }

  def vacationsPanel() = authAction {
    Ok(views.html.panels.vacations())
  }

  def manage() = Action { implicit request =>
    Ok(views.html.iface())
  }

  def info() = Action.async { implicit request =>

    calenarRepo.personsWithVacations().map { ps =>
      val p = ps.filter(_.name == "John E. Burris").head

      Ok(p.startDates.map(Instant.ofEpochMilli) + "\n" + p.endDates.map(Instant.ofEpochMilli))
    }
  }
}