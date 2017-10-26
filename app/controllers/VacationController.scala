package controllers

import java.time.temporal.ChronoUnit
import javax.inject.Inject

import io.swagger.annotations._
import models.JsonFormats._
import models.{CalendarRepository, Vacation}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Lang
import play.api.mvc.{AbstractController, ControllerComponents}
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import util.AuthAction
import util.dateOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Api( value = "/vacations")
class VacationController @Inject()(cc: ControllerComponents,
                                   calendarRepo: CalendarRepository,
                                   authAction: AuthAction)(implicit val af: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport {


  val vacationForm = Form(
    mapping(
      "_id" -> ignored(None: Option[BSONObjectID]),
      "uid" -> text,
      "startDate" -> of[BSONDateTime],
      "endDate" -> of[BSONDateTime]
    )(Vacation.apply)(Vacation.unapply)
  )

  @ApiOperation (
    value = "Add vacation data to database"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Form with errors")
  )
  )
  def addVacation() = authAction.async(parse.form(vacationForm, onErrors = (formWithErrors: Form[Vacation]) => {
    implicit val messages = messagesApi.preferred(Seq(Lang.defaultLang))

    for (er <- formWithErrors.errors) Logger.debug("error: " + er.message)
    Logger.debug(formWithErrors.errors.toString())
    BadRequest(views.html.forms.vacation(formWithErrors))
  })) { implicit request =>
    val vacData = request.body
    val newVacationLength = ChronoUnit.DAYS.between(vacData.startDate.value.toInstant,
      vacData.endDate.value.toInstant)
    val newYear = vacData.startDate.value.getYear

    Logger.debug("Adding vac in the year: " + newYear)
    calendarRepo.personsWithVacations().flatMap { personsl =>
      personsl.foreach { p =>
        Logger.debug("Found uid: " + p.uid)
      }
      val persons = personsl.filter(_.uid == vacData.uid)
      if (persons.isEmpty) {
        calendarRepo.addVacation(vacData)
        Logger.debug(vacData.toString)
        Future.successful(Ok("Vacation successsfully added."))
      } else {
        val person = persons.head
        Logger.debug("person found: " + person)
        val thisYearVacs = (person.startDates zip person.endDates) filter {
          _._1.getYear == newYear
        }

        // Length of all other this year's vacations
        val vacSum = thisYearVacs map {
          case (sd, ed) =>
            val dd = sd dayDiff ed
            Logger.debug("dd betw: " + dd)
            dd
        } sum

        val newDate = (vacData.startDate.value, newVacationLength)

        // New vacations is in valid relation to other vacations
        val validForPerson =
          if (thisYearVacs.isEmpty) true
          else newDate validInRelationToOthers
            thisYearVacs.map{case (sd, ed) => (sd, sd dayDiff ed)}

        // Number of other persons on vacation at the same time as newVacation (same position).
        calendarRepo.samePositionPWVacations(person.position) map { othersVacs =>

          val fullLength = Await.result(calendarRepo.samePositionPersons(person.position), 5 seconds).length

          val tol = othersVacs.map(pwv => (pwv.startDates, pwv.endDates)).unzip
          val res = (tol._1.flatten, tol._2.flatten)
          val r   = res._1 zip res._2 map {case (sd, ed) => (sd, sd dayDiff ed)}
          val intersections = newDate intersectsMany r count(_ == true)



          Logger.debug("New vac len: " + newVacationLength + ", vacsum: " + vacSum)
          Logger.debug("Valid for one person: " + validForPerson)
          Logger.debug("Number of others with the same position: " + fullLength)
          Logger.debug("Intersections: " + intersections)
          val validNumOfVacs = fullLength / intersections >= 2
          val result = newVacationLength + vacSum - 24
          Logger.debug("Result length in days: " + result)
          if (result < 0 && validNumOfVacs && validForPerson) {

            calendarRepo.addVacation(vacData)
            Logger.debug(vacData.toString)
            Ok("Vacation successsfully added.")
          } else {

            Ok("Conditions didn't met: " + result)
          }

        }
      }
    }
  }

  @ApiOperation (
    value = "Get vacations from database"
  )
  def listVacations(order: String,
                    @ApiParam(value = "Start date of the range restricting retrieving values")
                    from: Long,
                    @ApiParam(value = "End date of the range restricting retrieving values")
                    to: Long) = authAction.async {
    calendarRepo.getVacations(order match {case "asc" => 1 case "dsc" => -1},
                              BSONDateTime(from),
                              BSONDateTime(to)).map { vacs =>
      val nowTime = java.time.Instant.now().toEpochMilli
      Ok(views.html.vacations(vacs, nowTime))
    }
  }

  @ApiOperation(
    value = "Delete vacation from database"
  )
  def deleteVacation(@ApiParam(value = "id of entry to delete")
                      id: BSONObjectID) = authAction.async {
    calendarRepo.deleteVacation(id).map {
      case Some(todo) => Ok(todo.toString)
      case None => NotFound
    }
  }

  @ApiOperation(
    value = "Update vacation entry with new data"
  )
  def updateVacation(id: BSONObjectID, from: Long, to: Long) = authAction.async {

    calendarRepo.updateVacation(id, Vacation(None, "", BSONDateTime(from), BSONDateTime(to))).map { vac =>
      Ok(vac.toString)
    }
  }

  def addVacationPanel() = authAction { implicit request =>
    Ok(views.html.forms.vacation(vacationForm))
  }

  def form() = Action { implicit request =>
    Ok(views.html.forms.vacation(vacationForm))
  }
}
