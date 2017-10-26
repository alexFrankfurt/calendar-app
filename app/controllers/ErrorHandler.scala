package controllers

import play.api.mvc.Results.Ok
import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ErrorHandler extends HttpErrorHandler{
  override def onClientError(request: RequestHeader, statusCode: Int, message: String) = ???

  override def onServerError(request: RequestHeader, exception: Throwable) = {
    Future(Ok(views.html.error()))
  }
}
