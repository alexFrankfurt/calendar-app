package util

import javax.inject.Inject

import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]) = {
    request.session.get("UID") match {
      case None => Future.successful(Unauthorized("/"))
      case Some(id) =>
        block(request)
    }
  }
}
