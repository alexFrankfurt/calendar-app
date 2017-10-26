package models

import java.time.ZoneOffset
import javax.inject.Inject

import io.swagger.annotations.ApiModel
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

@ApiModel(value = "Representation of person")
case class Person(_id: Option[BSONObjectID], uid: Option[String], name: String, position: String)
@ApiModel(value = "Representation of vacation entry")
case class Vacation(_id: Option[BSONObjectID], uid: String, startDate: BSONDateTime, endDate: BSONDateTime)
case class PersonWithVacations(_id: String, uid: String, name: String, position: String,
                               startDates: List[Long], endDates: List[Long])

object JsonFormats {
  import play.api.libs.json._

  implicit val personFormat: OFormat[Person] = Json.format[Person]
  implicit val vacationFormat: OFormat[Vacation] = Json.format[Vacation]

  import play.api.data.format.Formats._
  import play.api.data.format.Formatter

  implicit object VacationFormatter extends Formatter[BSONDateTime] {
    override val format = Some(("format.bsondatetime", Nil))
    override def bind(key: String, data: Map[String, String])
      = parsing(str => new BSONDateTime(
            java.time.LocalDate.parse(str).atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli
          ), "error.url", Nil)(key, data)
    override def unbind(key: String, value: BSONDateTime) = Map(key -> value.toString)
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._ // Combinator syntax

  def multiDates[T](path: String)(implicit rt: Reads[T]) = Reads[List[T]] { js =>
    val l: List[JsValue] = (__ \ "res_uid" \\ path \ "$date")(js)
    Json.fromJson[List[T]](JsArray(l))
  }

  implicit val personWithVacationsReads: Reads[PersonWithVacations] = (
    (JsPath \ "_id" \ "$oid").read[String] and
      (JsPath \ "uid").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "position").read[String] and
      multiDates[Long]("startDate") and
      multiDates[Long]("endDate")
  )(PersonWithVacations.apply _)

}


class CalendarRepository @Inject()(implicit ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){

  import JsonFormats._

  def personsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("persons"))
  def vacationsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("vacations"))

  def getPersons(order: Int, position: Option[String]): Future[Seq[Person]] = {
    val query = Json.obj()
    val filter = position match {
      case None => BSONDocument()
      case Some(qStr) => BSONDocument("position" -> qStr)
    }
    personsCollection
      .flatMap(_.find(filter) //.options(QueryOpts(skipN = n))
               .sort(Json.obj("name" -> order))
               .cursor[Person](ReadPreference.primary)
               .collect[Seq]())
  }

  def getVacations(order: Int, from: BSONDateTime, to: BSONDateTime): Future[Seq[Vacation]] = {
    val filter = BSONDocument("startDate" -> BSONDocument("$gt" -> from, "$lt" -> to))
    vacationsCollection
        .flatMap(_.find(filter)
                    .sort(Json.obj("startDate" -> order))
                      .cursor[Vacation](ReadPreference.primary)
                        .collect[Seq]())
  }

  def personExists(uid: String): Future[Option[Person]] = {
    personsCollection.flatMap(_.find(BSONDocument("uid" -> uid)).one[Person])
  }

  def getPerson(id: BSONObjectID): Future[Option[Person]] = {
    val query = BSONDocument("_id" -> id)
    personsCollection.flatMap(_.find(query).one[Person])
  }

  def addPerson(person: Person): Future[WriteResult] = {
    personsCollection.flatMap(_.insert(person))
  }

  def updatePerson(id: BSONObjectID, person: Person): Future[Option[Person]] = {

    val selector = BSONDocument("_id" -> id)
    val updateModifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> person.name,
        "position" -> person.position)
    )

    personsCollection.flatMap(
      _.findAndUpdate(selector, updateModifier, fetchNewObject = true).map(_.result[Person])
    )
  }

  def updateVacation(id: BSONObjectID, vac: Vacation): Future[Option[Vacation]] = {
    val updateModifier = BSONDocument(
      "$set" -> BSONDocument(
        "startDate" -> vac.startDate,
        "endDate" -> vac.endDate)
    )
    vacationsCollection.flatMap(
      _.findAndUpdate(BSONDocument("_id" -> id), updateModifier, fetchNewObject = true).map(_.result[Vacation])
    )
  }

  def aggrPersonWithVacs(): Future[List[JsObject]] = {
    personsCollection.
      flatMap( pcol => {
        import pcol.BatchCommands.AggregationFramework.{Lookup, Match}
        vacationsCollection.flatMap(vcol => {
          pcol.aggregate(Lookup(vcol.name, "uid", "uid", "res_uid"),List(
            Match(Json.obj("res_uid" -> Json.obj("$ne" -> Json.arr())))
          )).map(_.firstBatch)
        })
      })
  }


  def personsWithVacations(): Future[List[PersonWithVacations]] = aggrPersonWithVacs().map{
    _.map {
      _.validate[PersonWithVacations] match {
        case s: JsSuccess[PersonWithVacations] =>
          s.value
        case e: JsError =>
          // impossible :)
          ???
      }
    }
  }

  def samePositionPersons(position: String): Future[Seq[Person]] = {
    personsCollection
      .flatMap(_.find(BSONDocument("position" -> position)) //.options(QueryOpts(skipN = n))
        .cursor[Person](ReadPreference.primary)
        .collect[Seq]())
  }

  def samePositionPWVacations(position: String): Future[List[PersonWithVacations]] = {
    personsCollection.
      flatMap( pcol => {
        import pcol.BatchCommands.AggregationFramework.{Lookup, Match}
        vacationsCollection.flatMap(vcol => {
          pcol.aggregate(Lookup(vcol.name, "uid", "uid", "res_uid"),List(
            Match(Json.obj("res_uid" -> Json.obj("$ne" -> Json.arr()))),
            Match(Json.obj("position" -> position))
          )).map(_.firstBatch)
        })
      }).map{
      _.map {
        _.validate[PersonWithVacations] match {
          case s: JsSuccess[PersonWithVacations] =>
            s.value
          case e: JsError =>
            // impossible :)
            ???
        }
      }
    }
  }

  def deletePerson(id: BSONObjectID): Future[Option[Person]] = {
    val selector = BSONDocument("_id" -> id)
    personsCollection.flatMap(_.findAndRemove(selector).map(_.result[Person]))
  }

  def deleteVacation(id: BSONObjectID): Future[Option[Vacation]] = {
    vacationsCollection.flatMap(_.findAndRemove(BSONDocument("_id" -> id)).map(_.result[Vacation]))
  }

  def addVacation(vacation: Vacation): Future[WriteResult] = {
    vacationsCollection.flatMap(_.insert(vacation))
  }
}
