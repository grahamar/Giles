package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global

object VersionsApiController extends Controller {

  def getVersions(projectGuid: String, version: Option[String]) = Action {
    Global.projects.findByGuid(projectGuid.toGuid) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(projectGuid))
      }
      case Some(project: Project) => {
        version match {
          case None =>
            Ok(Json.toJson(project.versions.map(Json.toJson(_))))
          case Some(ver) if !project.versions.contains(ver.toVersion) =>
            NotFound("Project version [%s] not found".format(version))
          case Some(ver) if project.versions.contains(ver.toVersion) =>
            // TODO what do we return here? files? project?
            Ok(Json.toJson(project.versions.map(Json.toJson(_))))
        }
      }
      case _ => InternalServerError
    }
  }

  def putVersions = Action { implicit request =>
    putVersionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      data => createVersion(data)
    )
  }

  def createVersion(data: PutVersionFormData)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(data.project_guid.toGuid) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(data.project_guid))
      }
      case Some(project: Project) if project.versions.contains(data.version) => {
        Ok(Json.toJson(project))
      }
      case Some(project: Project) if !project.versions.contains(data.version) => {
        Global.projects.update(project.copy(versions = project.versions.+:(data.version.toVersion)))
        Ok(Json.toJson(Global.projects.findByGuid(data.project_guid.toGuid)))
      }
      case _ => InternalServerError
    }
  }

  val putVersionForm = Form {
    mapping("project_guid" -> nonEmptyText,
      "version" -> nonEmptyText
    )(PutVersionFormData.apply)(PutVersionFormData.unapply)
  }

}
