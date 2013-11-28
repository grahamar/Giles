package controllers

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import views._
import models._
import controllers.auth._
import settings.Global
import dao.util.ProjectHelper

object AuthenticationController extends Controller with LoginLogout with OptionalAuthUser with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(Global.users.authenticate)(_.map(u => (u.email, "")))
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
    ApplicationController.Home
  }

  def logout = AsyncStack { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = AsyncStack { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(ApplicationController.index(formWithErrors))),
      user => user.map(usr => gotoLoginSucceeded(usr.guid)).getOrElse(Future.failed(new UserIdNotSetException))
    )
  }

  def signUp = StackAction { implicit request =>
    Ok(html.signUp(createUserForm, loginForm))
  }

  def createUser = StackAction { implicit request =>
    createUserForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.signUp(formWithErrors, loginForm))),
      user => Future.successful(Global.users.create(user.toUser))
    )
    ApplicationController.Home
  }

  def profile(username: String) = StackAction { implicit request =>
    val user = Global.users.findByUsername(username)
    user.map{usr =>
      val projects = Global.projects.findByAuthorGuid(usr.guid)
      Ok(html.profile(usr, ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  private class UserIdNotSetException extends RuntimeException

}
