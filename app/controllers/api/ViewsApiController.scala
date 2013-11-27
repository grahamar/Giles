package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global

object ViewsApiController extends Controller {

  def putViews = Action { implicit request =>
    putViewForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      data => createView(data)
    )
  }

  def createView(data: PutViewFormData)(implicit request: Request[Any]) = {
    Global.views.findByGuid(data.guid.toGuid) match {
      case None => {
        Global.views.create(data.guid.toGuid, data.file_guid.toGuid, data.user_guid.map(_.toGuid))
        Ok(Json.toJson(Global.views.findByGuid(data.file_guid.toGuid).get))
      }
      case Some(existing: View) if existing.file_guid == data.file_guid.toGuid => {
        BadRequest("Cannot change file guid from %s to %s".format(existing.file_guid, data.file_guid.toGuid))
      }
      case Some(existing: View) if existing.user_guid == data.user_guid.map(_.toGuid) => {
        BadRequest("Cannot change user guid from %s to %s".format(existing.user_guid, data.user_guid.map(_.toGuid)))
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
