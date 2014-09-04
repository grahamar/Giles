package controllers

import java.io.{FileInputStream, InputStreamReader}
import java.util.UUID

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.templates.HtmlFormat

import views._
import models._
import controllers.auth._
import dao.util.ProjectHelper
import settings.Global
import util.HashingUtils
import org.apache.commons.lang3.RandomStringUtils
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.plus.model.Person
import com.google.api.services.plus.{Plus, PlusScopes}

object AuthenticationController extends Controller with LoginLogout with OptionalAuthUser with AuthConfigImpl {
  import play.api.Play.current

  private val ApplicationName = "Gilt-Giles/1.0"
  private val JsonFactory = GsonFactory.getDefaultInstance
  private val HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
  private lazy val ClientSecrets = GoogleClientSecrets.load(JsonFactory,
    new InputStreamReader(new FileInputStream(current.getFile("conf/client_secrets.json")))
  )
  private lazy val GoogleFlow = new GoogleAuthorizationCodeFlow.Builder(
    HttpTransport,
    JsonFactory,
    ClientSecrets,
    Seq(PlusScopes.USERINFO_EMAIL, PlusScopes.USERINFO_PROFILE).asJavaCollection
  ).build()
  private val GoogleOAuthRedirectUrl = { (request: RequestHeader) =>
    routes.AuthenticationController.oauth2callback(None, None).absoluteURL()(request)
  }

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

  def loginWithGoogle = StackAction { implicit request =>
    Redirect(GoogleFlow.newAuthorizationUrl().
      setRedirectUri(GoogleOAuthRedirectUrl(request)).
      build())
  }

  def oauth2callback(code: Option[String], error: Option[String]) = AsyncStack { implicit request =>
    code.map { c =>
      val tokenResponse = GoogleFlow.newTokenRequest(c).
        setRedirectUri(GoogleOAuthRedirectUrl(request)).execute()
      val credentials = GoogleFlow.createAndStoreCredential(tokenResponse, null)
      val plus = new Plus.Builder(HttpTransport, JsonFactory, credentials).setApplicationName(ApplicationName).build()
      val profile = plus.people().get("me").execute()
      Option(profile.getEmails).map(_.asScala.head).map(_.getValue).map { email =>
        for {
          user <- createGoogleUser(email, profile)
          result <- gotoLoginSucceeded(user.guid)
        } yield result
      }.getOrElse {
        Future.successful(Redirect(routes.ApplicationController.index()))
      }
    }.getOrElse {
      Future.successful(Redirect(routes.ApplicationController.index()))
    }
  }

  def createGoogleUser(email: String, profile: Person)(implicit request: Request[_]): Future[User] = Future {
    Global.users.findByEmail(email).getOrElse {
      val userGuid = UUID.randomUUID().toString
      val username = email.toLowerCase.takeWhile((ch: Char) => !'@'.equals(ch))
      val password = RandomStringUtils.randomAlphabetic(20)
      val firstname = Option(profile.getName.getGivenName)
      val lastname = Option(profile.getName.getFamilyName)
      Global.users.create(userGuid, username, email, password, firstname, lastname)
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
    Global.users.findByUsername(username).map(usr => Ok(profilePage(usr))).
      getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
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
