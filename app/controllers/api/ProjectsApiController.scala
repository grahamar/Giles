package controllers.api

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._

object ProjectsApiController extends BaseApiController {

  def getProjects(guid: String,
                  name: String,
                  url_key: String,
                  limit: String,
                  offset: String) = Action { implicit request =>
    val projectQuery = ProjectQuery(guid = Option(guid), name = Option(name), url_key = Option(url_key), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
    jsonResponse(Global.projects.search(projectQuery).toList)
  }

  def putProjects = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putProjectForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        projectData => createProject(projectData, userGuid)
      )
    }
  }

  def createProject(projectData: PutProjectFormData, callingUserGuid: String)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(projectData.guid) match {
      case None => {
        val createdBy = projectData.created_by.getOrElse(Global.users.findByGuid(callingUserGuid).get.username)
        Global.projects.create(createdBy, projectData.guid, projectData.name,
          projectData.description.getOrElse(""), projectData.repo_url, projectData.author_usernames, projectData.head_version.getOrElse("HEAD"))
        jsonResponse(Global.projects.findByGuid(projectData.guid))
      }
      case Some(existing: Project) => {
        val updated = existing.copy(description = projectData.description.getOrElse(""), head_version = projectData.head_version.getOrElse("HEAD"))
        Global.projects.update(updated)
        jsonResponse(Global.projects.findByGuid(projectData.guid))
      }
      case _ => InternalServerError
    }
  }

  val putProjectForm = Form {
    mapping("guid" -> nonEmptyText.verifying(o => isUUID(o)),
      "name" -> nonEmptyText,
      "description" -> optional(text),
      "created_by" -> optional(text),
      "repo_url" -> nonEmptyText,
      "author_usernames" -> seq(text),
      "head_version" -> optional(text)
    )(PutProjectFormData.apply)(PutProjectFormData.unapply)
  }

}
