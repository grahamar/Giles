package auth

import play.api.mvc.{SimpleResult, RequestHeader, Controller}

import jp.t2v.lab.play2.auth.AuthConfig
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.Crypto
import util.SessionUtil

trait LoginLogout {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    gotoLoginSucceeded(userId, loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: Id, result: => Future[SimpleResult])(implicit ctx: ExecutionContext): Future[SimpleResult] = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    val value = Crypto.sign(token) + token
    result.map( (req: SimpleResult) => req.withSession(cookieName -> value))
  }

  def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    gotoLogoutSucceeded(logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Future[SimpleResult])(implicit request: RequestHeader, ctx: ExecutionContext): Future[SimpleResult] = {
    request.session.get(cookieName) flatMap SessionUtil.verifyHmac foreach idContainer.remove
    result.map(_.withNewSession)
  }

}
