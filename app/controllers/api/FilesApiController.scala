package controllers.api

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import dao.util.FileHelper
import build.DocumentationFactory

object FilesApiController extends BaseApiController {

  def getFiles(guid: String,
               project_guid: String,
               filename: String,
               version: String,
               title: String,
               url_key: String,
               limit: String,
               offset: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      jsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else if(version.isEmpty) {
      jsonResponse(new value.ApiResponse(400, "Invalid version value"))
    } else {
      val fileQuery =
        FileQuery(guid = Option(guid), filename = Option(filename), url_key = Option(url_key),
          project_guid = Option(project_guid), version = Option(version), title = Option(title),
          limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
      jsonResponse(Global.files.search(fileQuery).toList)
    }
  }

  def putFiles = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putFileForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        data => createFile(data)
      )
    }
  }

  def createFile(data: PutFileFormData)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(data.project_guid) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(data.project_guid))
      }
      case Some(project: Project) => {
        // Create the project version if it doesn't exist
        if(!project.versions.contains(data.version)) {
          Global.projects.update(project.copy(versions = project.versions ++ Seq(data.version)))
        }
        /*
         * Artificially create a build if one doesn't exist for this project & version
         * as technically a build did occur, just external to Giles.
         */
        val existingBuild = Global.builds.findByProjectGuidAndVersion(project.guid, data.version)
        if(existingBuild.isEmpty) {
          Global.builds.createSuccess(project.guid, data.version, project.author_usernames)
        }

        Global.files.findByGuid(data.guid) match {
          case None => {
            FileHelper.getOrCreateContent(data.html) { contentGuid =>
              val file = Global.files.create(data.guid, project, data.version,
                data.relative_path.getOrElse(""), data.filename, data.title, contentGuid)
              val fileAndContent = FileWithContent(file, data.html)
              DocumentationFactory.indexService.index(project, data.version, fileAndContent)
            }
            jsonResponse(Global.files.findByGuid(data.guid))
          }
          case Some(existingFile: File) => {
            FileHelper.getOrCreateContent(data.html) { contentGuid =>
              Global.files.update(existingFile.copy(content_guid = contentGuid))
              FileHelper.cleanupContent(existingFile.content_guid)
            }
            jsonResponse(Global.files.findByGuid(data.guid))
          }
        }
      }
    }
  }

  val putFileForm = Form {
    mapping("guid" -> nonEmptyText,
      "project_guid" -> nonEmptyText,
      "version" -> nonEmptyText,
      "title" -> nonEmptyText,
      "filename" -> nonEmptyText,
      "relative_path" -> optional(text),
      "html" -> nonEmptyText
    )(PutFileFormData.apply)(PutFileFormData.unapply)
  }

}
