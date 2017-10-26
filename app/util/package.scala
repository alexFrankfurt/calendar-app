import java.time._
import java.time.temporal.ChronoUnit

package object util {

  val `24 days in ms` = 2073600000L
  val `1 day in ms` = 86400000L

  object dateOps {
    implicit class longAsDate(val d: Long) {
      def getYear: Int = Instant.ofEpochMilli(d).atOffset(java.time.ZoneOffset.UTC).getYear
      def toInstant: Instant = Instant.ofEpochMilli(d)
      def dayDiff(other: Long) = ChronoUnit.DAYS.between(d.toInstant, other.toInstant)
    }

    implicit class offDayDiff(val i: Instant) {
      def dayDiff(other: Instant): Long = ChronoUnit.DAYS.between(i, other)
    }

    implicit class vacTuple(val vac: (Long, Long)) {
      assert(vac._2 < 400)

      def intersects(other: (Long, Long)): Boolean = {
        if (vac._1 < other._1) (vac._1 dayDiff other._1) > vac._2
        else (other._1 dayDiff vac._1) > other._2
      }
      def intersectsMany(other: List[(Long, Long)]): List[Boolean] = {
        other.map { case (osd, ovl) =>
          intersects(osd, ovl)
        }
      }

      def inValidRangeWith(other: (Long, Long)): Boolean = {
        if (vac._1 < other._1) (vac._1 dayDiff other._1) > (2 * vac._2)
        else (other._1 dayDiff vac._1) > (2 * other._2)
      }

      def validInRelationToOthers(others: List[(Long, Long)]): Boolean = {
        others.map{
          case (sd, len) => inValidRangeWith(sd, len)
        } reduce (_ && _)
      }
    }
  }
}
