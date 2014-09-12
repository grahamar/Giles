package controllers.api

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._

object UsersApiController extends BaseApiController {

  def getUsers(guid: String,
               username: String,
               email: String,
               limit: String,
               offset: String) = Action { implicit request =>
    val usersQuery =
      UserQuery(guid = Option(guid), username = Option(username), email = Option(email), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
    jsonResponse(Global.users.search(usersQuery).toList.map(_.toJsonUser))
  }

  def putUsers = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putUserForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        data => createUser(data)
      )
    }
  }

  def createUser(data: PutUserFormData)(implicit request: Request[Any]) = {
    Global.users.findByGuid(data.guid) match {
      case None => {
        Global.users.create(data.guid, data.username, data.email, data.password, data.first_name, data.last_name)
        jsonResponse(Global.projects.findByGuid(data.guid))
      }
      case Some(existing: User) => {
        val updated = existing.copy(username = data.username, email = data.email, first_name = data.first_name, last_name = data.last_name, homepage = data.homepage)
        Global.users.update(updated)
        jsonResponse(Global.projects.findByGuid(data.guid))
      }
      case _ => InternalServerError
    }
  }

  val putUserForm = Form {
    mapping("guid" -> nonEmptyText.verifying(o => isUUID(o)),
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8),
      "first_name" -> optional(text),
      "last_name" -> optional(text),
      "homepage" -> optional(text)
    )(PutUserFormData.apply)(PutUserFormData.unapply)
  }

}
