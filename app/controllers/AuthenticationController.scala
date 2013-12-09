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
import play.api.libs.openid.OpenID
import org.apache.commons.lang3.RandomStringUtils
import java.util.UUID
import play.api.templates.HtmlFormat
import util.HashingUtils

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

  val createApiKeyForm = Form {
    mapping("application_name" -> nonEmptyText
    )(ApiKeyData.apply)(ApiKeyData.unapply)
  }

  def login = StackAction { implicit request =>
    ApplicationController.Home
  }

  def loginWithGoogle = AsyncStack { implicit request =>
    OpenID.redirectURL("https://www.google.com/accounts/o8/id", routes.AuthenticationController.openIDCallback.absoluteURL(),
      Seq("email" -> "http://schema.openid.net/contact/email",
        "first_name" -> "http://axschema.org/namePerson/first",
        "last_name" -> "http://axschema.org/namePerson/last",
        "username" -> "http://schema.openid.net/namePerson/friendly")).
      map(Redirect(_)).recover {
        case e: Exception => ApplicationController.Home
      }
  }

  def openIDCallback = AsyncStack { implicit request =>
    for {
      user <- createGoogleUser
      result <- gotoLoginSucceeded(user.guid)
    } yield result
  }

  def createGoogleUser()(implicit request: Request[_]): Future[User] = {
    OpenID.verifiedId.map { info =>
      val email =  info.attributes.getOrElse("email", "guest@giles.io")
      Global.users.findByEmail(email).getOrElse {
        val userGuid = UUID.randomUUID().toString
        val username = email.toLowerCase.takeWhile((ch: Char) => !'@'.equals(ch))
        val password = RandomStringUtils.randomAlphabetic(20)
        val firstname = info.attributes.get("first_name")
        val lastname = info.attributes.get("last_name")
        Global.users.create(userGuid, username, email, password, firstname, lastname)
      }
    }
  }

  def logout = AsyncStack { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = AsyncStack { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(ApplicationController.indexPage(formWithErrors))),
      user => user.map(usr => gotoLoginSucceeded(usr.guid)).getOrElse(Future.failed(new UserIdNotSetException))
    )
  }

  def signUp = StackAction { implicit request =>
    Ok(html.signUp(createUserForm, loginForm))
  }

  def createUser = StackAction { implicit request =>
    createUserForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.signUp(formWithErrors, loginForm)),
      user => Global.users.create(user.toUser)
    )
    ApplicationController.Home
  }

  def profile(username: String) = StackAction { implicit request =>
    val maybeBoom = loggedIn
    maybeBoom.filter(boom => "rsetti".equals(boom.username)).map { _ =>
      new Status(418)(html.boom(AuthenticationController.loginForm))
    }.getOrElse {
      Global.users.findByUsername(username).map(usr => Ok(profilePage(usr))).
        getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
    }
  }

  def profilePage(user: User, apiKeyForm: Form[ApiKeyData] = createApiKeyForm)(implicit flash: Flash, currentUser: Option[models.User]): HtmlFormat.Appendable = {
    val projects = (Global.projects.findByAuthorUsername(user.username) ++ Global.projects.findByCreatedBy(user.username)).toSet
    val userFavourites = currentUser.map(ProjectHelper.getFavouriteProjectsForUser(_).toSeq).getOrElse(Seq.empty)
    val userApiKeys = currentUser.map(Global.apiKeys.findUser(_).toSeq).getOrElse(Seq.empty)
    html.profile(user, ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, userFavourites, userApiKeys, apiKeyForm, AuthenticationController.loginForm)
  }

  def createApiKey = StackAction { implicit request =>
    loggedIn.map { currentUser =>
      createApiKeyForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(profilePage(currentUser, formWithErrors))
        },
        apiKeyData => {
          Global.apiKeys.create(UUID.randomUUID().toString, currentUser.guid, apiKeyData.application_name, HashingUtils.calculateUserApiKey(apiKeyData.application_name))
          Redirect(routes.AuthenticationController.profile(currentUser.username))
        }
      )
    }.getOrElse {
      Unauthorized(ApplicationController.indexPage(loginForm))
    }
  }

  private class UserIdNotSetException extends RuntimeException

}
