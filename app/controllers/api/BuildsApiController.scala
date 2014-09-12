package controllers.api

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global

object BuildsApiController extends BaseApiController {

  def getBuilds(project_guid: String,
                guid: String,
                version: String,
                limit: String,
                offset: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      jsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else {
      val buildQuery = BuildQuery(guid = Option(guid), project_guid = Some(project_guid),
        version = Option(version), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
      jsonResponse(Global.builds.search(buildQuery).toList)
    }
  }

  def putBuilds = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putBuildForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        buildData => createBuild(buildData)
      )
    }
  }

  def createBuild(buildData: PutBuildFormData)(implicit request: Request[Any]) = {
    Global.builds.findByGuid(buildData.guid) match {
      case None => {
        Global.builds.create(buildData.guid, buildData.project_guid, buildData.version, buildData.author_usernames, "", "success")
        jsonResponse(Global.builds.findByGuid(buildData.guid))
      }
      case Some(existing: Build) => {
        val updated = existing.copy(authors = buildData.author_usernames)
        Global.builds.update(updated)
        jsonResponse(Global.builds.findByGuid(buildData.guid))
      }
      case _ => InternalServerError
    }
  }

  val putBuildForm = Form {
    mapping("guid" -> nonEmptyText.verifying(o => isUUID(o)),
      "project_guid" -> nonEmptyText.verifying(o => isUUID(o)),
      "version" -> nonEmptyText,
      "author_usernames" -> seq(text)
    )(PutBuildFormData.apply)(PutBuildFormData.unapply)
  }

}
