package controllers

import javax.inject.Inject

import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import models._
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Lang
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import util.AuthAction

import scala.concurrent.ExecutionContext.Implicits.global

@Api( value = "/persons" )
class StaffController @Inject()(cc: ControllerComponents,
                                authAction: AuthAction,
                                calendarRepo: CalendarRepository)
                               (implicit val af: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport {

  val personForm = Form(
    mapping(
      "_id" -> ignored(None: Option[BSONObjectID]),
      "uid" -> optional(text),
      "name" -> text,
      "position" -> text
    )(Person.apply)(Person.unapply)
  )

  def signup() = Action( implicit request => Ok(views.html.signup(views.html.forms.person(personForm))))

  @ApiOperation(
    value = "Add person to database"
  )
  def addPerson(@ApiParam(value = "empty parameter when signing up for the first time", required = false)
                notme: Option[String]) = Action(parse.form(personForm, onErrors = (formWithErrors: Form[Person]) => {
    implicit val messages = messagesApi.preferred(Seq(Lang.defaultLang))

    Logger.debug(formWithErrors.errors.toString())
    BadRequest(views.html.forms.person(formWithErrors))
  })) { implicit request =>

      val personData = notme match {
        case Some(_) =>  request.body.copy(uid = None)
        case None => request.body.copy(uid = request.session.get("UID"))
      }
      val a = calendarRepo.addPerson(personData)
      Logger.debug(a.toString)
      Logger.debug(personData.toString)
      Redirect(routes.AppController.index())
  }


  def addPersonPanel() = authAction { implicit request =>
    Ok(views.html.forms.person(personForm))
  }

  @ApiOperation(
    value = "Retrieve all persons from database with specified ordering and optional position",
  )
  def listPersons(@ApiParam(value = "Order in which retrieve entries from database", required = true,
    allowableValues = "asc | dsc")
                  order: String, position: Option[String]) = authAction.async { request =>

    calendarRepo.getPersons(order match {case "asc" => 1 case "dsc" => -1}, position).map { persons =>
      Ok(views.html.persons(persons))
    }
  }
}
