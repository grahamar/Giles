package controllers.api

import java.util.UUID

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._

object UsersApiController extends Controller {

  def getUsers(guid: Option[String], username: Option[String], email: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val usersQuery =
      UserQuery(guid = guid.map(UUID.fromString), username = username, email = email, limit = limit.map(_.toInt), offset = offset.map(_.toInt))
    Ok(Json.toJson(Global.users.search(usersQuery).toList.map(Json.toJson(_))))
  }

  def putUsers = Action { implicit request =>
    putUserForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      data => createUser(data)
    )
  }

  def createUser(data: PutUserFormData)(implicit request: Request[Any]) = {
    Global.users.findByGuid(UUID.fromString(data.guid)) match {
      case None => {
        Global.users.create(UUID.fromString(data.guid), data.username, data.email, data.password, data.first_name, data.last_name)
        Ok(Json.toJson(Global.projects.findByGuid(UUID.fromString(data.guid))))
      }
      case Some(existing: User) => {
        val updated = existing.copy(username = data.username, email = data.email, first_name = data.first_name, last_name = data.last_name, homepage = data.homepage)
        Global.users.update(updated)
        Ok(Json.toJson(Global.projects.findByGuid(UUID.fromString(data.guid))))
      }
      case _ => InternalServerError
    }
  }

  val putUserForm = Form {
    mapping("guid" -> nonEmptyText,
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8),
      "first_name" -> optional(text),
      "last_name" -> optional(text),
      "homepage" -> optional(text)
    )(PutUserFormData.apply)(PutUserFormData.unapply)
  }

}
