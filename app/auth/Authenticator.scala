package auth

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import views._
import dao.{ProjectDAO, UserDAO}

import controllers.Application

case class UserData(username: String,
                    email: String,
                    password: String,
                    rePassword: String,
                    firstName: String,
                    lastName: String,
                    homepage: String)

object Authenticator extends Controller with LoginLogout with OptionalAuthUser with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(UserDAO.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  val createUserForm = Form {
    mapping("username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8),
      "rePassword" -> nonEmptyText(minLength = 8),
      "firstname" -> text,
      "lastname" -> text,
      "homepage" -> text
    )(UserData.apply)(UserData.unapply).
    verifying("Passwords don't match", data => data.password.equals(data.rePassword))
  }

  def login = StackAction { implicit request =>
    Application.Home
  }

  def logout = AsyncStack { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = AsyncStack { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.index(ProjectDAO.recentlyUpdatedProjectsWithAuthors(10), formWithErrors))),
      user => user.get.id.map(gotoLoginSucceeded).getOrElse(Future.failed(new UserIdNotSetException))
    )
  }

  def signUp = StackAction { implicit request =>
    Ok(html.signUp(createUserForm, loginForm))
  }

  def createUser = StackAction { implicit request =>
    createUserForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.signUp(formWithErrors, loginForm))),
      user => Future.successful(UserDAO.createUser(user))
    )
    Application.Home
  }

  private class UserIdNotSetException extends RuntimeException

}
