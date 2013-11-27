package controllers.auth

import scala.reflect.{ClassTag, classTag}
import scala.concurrent.{Future, ExecutionContext}

import play.api.mvc._
import play.api.mvc.Results._

import controllers.ApplicationController

import jp.t2v.lab.play2.auth.AuthConfig
import models.Guid
import settings.Global

sealed trait Permission
case object Administrator extends Permission
case object NormalUser extends Permission

trait AuthConfigImpl extends AuthConfig {

  type Id = Guid

  type User = models.User

  type Authority = Permission

  val idTag: ClassTag[Id] = classTag[Id]

  /**
   * The session timeout in seconds
   */
  val sessionTimeoutInSeconds: Int = 3600

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = Future {Global.users.findByGuid(id)}

  /**
   * Where to redirect the user after a successful login.
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(ApplicationController.Dashboard)

  /**
   * Where to redirect the user after logging out
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(ApplicationController.Home)

  /**
   * If the user is not logged in and tries to access a protected resource then redirct them as follows:
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Redirect(controllers.routes.AuthenticationController.authenticate))

  /**
   * If authorization failed (usually incorrect password) redirect the user as follows:
   */
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[SimpleResult] =
    Future.successful(Forbidden("no permission"))

  /**
   * A function that determines what `Authority` a user has.
   * You should alter this procedure to suit your application.
   */
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    true
  }

  override lazy val cookieSecureOption: Boolean = play.api.Play.current.configuration.getBoolean("controllers.auth.cookie.secure").getOrElse(true)

  override lazy val cookieHttpOnlyOption: Boolean = play.api.Play.current.configuration.getBoolean("controllers.auth.cookie.httpOnly").getOrElse(false)

}
