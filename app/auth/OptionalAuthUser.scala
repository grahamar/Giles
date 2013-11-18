package auth

import scala.util.Success
import scala.concurrent.{Future, ExecutionContext}

import play.api.mvc.{Controller, RequestHeader}

import util.SessionUtil

import jp.t2v.lab.play2.auth.{AuthConfig, OptionalAuthElement}

trait OptionalAuthUser extends OptionalAuthElement {
  self: Controller with AuthConfig =>

  override def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[Option[User]] = {
    val userIdOpt = for {
      value <- request.session.get(cookieName)
      token <- SessionUtil.verifyHmac(value)
      userId <- idContainer.get(token)
    } yield (token, userId)
    userIdOpt map { case (token, userId) =>
      resolveUser(userId) andThen {
        case Success(Some(_)) => idContainer.prolongTimeout(token, sessionTimeoutInSeconds)
      }
    } getOrElse {
      Future.successful(Option.empty)
    }
  }

}
