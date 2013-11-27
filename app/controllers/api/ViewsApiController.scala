package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import java.util.UUID

object ViewsApiController extends Controller {

  def putViews = Action { implicit request =>
    putViewForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      data => createView(data)
    )
  }

  def createView(data: PutViewFormData)(implicit request: Request[Any]) = {
    Global.views.findByGuid(UUID.fromString(data.guid)) match {
      case None => {
        Global.views.create(UUID.fromString(data.guid), UUID.fromString(data.file_guid), data.user_guid.map(UUID.fromString))
        Ok(Json.toJson(Global.views.findByGuid(UUID.fromString(data.file_guid)).get))
      }
      case Some(existing: View) if existing.file_guid == UUID.fromString(data.file_guid) => {
        BadRequest("Cannot change file guid from %s to %s".format(existing.file_guid, data.file_guid))
      }
      case Some(existing: View) if existing.user_guid == data.user_guid => {
        BadRequest("Cannot change user guid from %s to %s".format(existing.user_guid, data.user_guid))
      }
      case _ => InternalServerError
    }
  }

  val putViewForm = Form {
    mapping("guid" -> nonEmptyText,
      "file_guid" -> nonEmptyText,
      "user_guid" -> optional(text)
    )(PutViewFormData.apply)(PutViewFormData.unapply)
  }

}
