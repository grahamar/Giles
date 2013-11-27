package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._
import java.util.UUID

object ProjectsApiController extends Controller {

  def getProjects(guid: Option[String], name: Option[String], author_guids: Option[String], query:Option[String], urlKey: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val projectQuery =
      ProjectQuery(guid = guid.map(UUID.fromString), name = name, query = query, url_key = urlKey, limit = limit.map(_.toInt), offset = offset.map(_.toInt))
    Ok(Json.toJson(Global.projects.search(projectQuery).toList.map(Json.toJson(_))))
  }

  def putProjects = Action { implicit request =>
    putProjectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      projectData => createProject(projectData)
    )
  }

  def createProject(projectData: PutProjectFormData)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(UUID.fromString(projectData.guid)) match {
      case None => {
        Global.projects.create(UUID.fromString(projectData.author_guid), UUID.fromString(projectData.guid), projectData.name,
          projectData.description, projectData.repoUrl, projectData.headVersion.getOrElse("HEAD"))
        Ok(Json.toJson(Global.projects.findByGuid(UUID.fromString(projectData.guid))))
      }
      case Some(existing: Project) => {
        val updated = existing.copy(description = projectData.description, head_version = projectData.headVersion.getOrElse("HEAD"))
        Global.projects.update(updated)
        Ok(Json.toJson(Global.projects.findByGuid(UUID.fromString(projectData.guid))))
      }
      case _ => InternalServerError
    }
  }

  val putProjectForm = Form {
    mapping("guid" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "author_guid" -> nonEmptyText,
      "repoUrl" -> nonEmptyText,
      "headVersion" -> optional(text)
    )(PutProjectFormData.apply)(PutProjectFormData.unapply)
  }

}