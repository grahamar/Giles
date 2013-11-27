package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global

object FilesApiController extends Controller {

  def getFiles(guid: Option[String], name: Option[String], project_guid: Option[String], query: Option[String], title: Option[String],
               urlKey: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val fileQuery = FileQuery(guid = guid.map(_.toGuid), query = query, url_key = urlKey.map(_.toUrlKey), project_guid = project_guid.map(_.toGuid),
      title = title, limit = limit.map(_.toInt), offset = offset.map(_.toInt))
    Ok(Json.toJson(Global.files.search(fileQuery).toList.map(Json.toJson(_))))
  }

  def putFiles = Action { implicit request =>
    putFileForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      data => createFile(data)
    )
  }

  def createFile(data: PutFileFormData)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(data.project_guid.toGuid) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(data.project_guid))
      }
      case Some(project: Project) if !project.versions.contains(data.version.toVersion) => {
        NotFound("Project version [%s] not found".format(data.version))
      }
      case Some(project: Project) if project.versions.contains(data.version.toVersion) => {
        Global.files.findByGuid(data.guid.toGuid) match {
          case None => {
            Global.files.create(data.guid.toGuid, project, data.version.toVersion, data.title, data.html)
            Ok(Json.toJson(Global.files.findByGuid(data.guid.toGuid)))
          }
          case Some(existing: File) => {
            Global.files.update(existing.copy(html = data.html))
            Ok(Json.toJson(Global.files.findByGuid(data.guid.toGuid)))
          }
          case _ => InternalServerError
        }
      }
      case _ => InternalServerError
    }
  }

  val putFileForm = Form {
    mapping("guid" -> nonEmptyText,
      "project_guid" -> nonEmptyText,
      "version" -> nonEmptyText,
      "title" -> nonEmptyText,
      "html" -> nonEmptyText
    )(PutFileFormData.apply)(PutFileFormData.unapply)
  }

}
