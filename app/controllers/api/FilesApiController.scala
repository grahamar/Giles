package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import java.util.UUID

object FilesApiController extends Controller {

  def getFiles(guid: Option[String], name: Option[String], project_guid: Option[String], query: Option[String], title: Option[String],
               urlKey: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val fileQuery = FileQuery(guid = guid.map(UUID.fromString), query = query, url_key = urlKey, project_guid = project_guid.map(UUID.fromString),
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
    Global.projects.findByGuid(UUID.fromString(data.project_guid)) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(data.project_guid))
      }
      case Some(project: Project) if !project.versions.contains(data.version) => {
        NotFound("Project version [%s] not found".format(data.version))
      }
      case Some(project: Project) if project.versions.contains(data.version) => {
        Global.files.findByGuid(UUID.fromString(data.guid)) match {
          case None => {
            Global.files.create(UUID.fromString(data.guid), project, data.version, data.title, data.html)
            Ok(Json.toJson(Global.files.findByGuid(UUID.fromString(data.guid))))
          }
          case Some(existing: File) => {
            Global.files.update(existing.copy(html = data.html))
            Ok(Json.toJson(Global.files.findByGuid(UUID.fromString(data.guid))))
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
