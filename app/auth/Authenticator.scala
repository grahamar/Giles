package auth

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import views._
import dao.UserDAO

import controllers.Application

object Authenticator extends Controller with LoginLogout with OptionalAuthUser with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(UserDAO.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = StackAction { implicit request =>
    Application.Home
  }

  def logout = AsyncStack { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = AsyncStack { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.index(UserDAO.recentlyUpdatedProjectsWithAuthors(10), formWithErrors))),
      user => user.get.id.map(gotoLoginSucceeded).getOrElse(Future.failed(new UserIdNotSetException))
    )
  }

  private class UserIdNotSetException extends RuntimeException

}
