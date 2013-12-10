package controllers.api

import javax.ws.rs.QueryParam

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import dao.util.FileHelper
import build.DocumentationFactory
import com.wordnik.swagger.annotations._

@Api(value = "/api/files", description = "Operations for project files")
object FilesApiController extends BaseApiController {

  @ApiOperation(value = "Find files", notes = "Returns a list of files", response = classOf[File], responseContainer = "List", httpMethod = "GET")
  def getFiles(@ApiParam(value = "GUID of the file to fetch", required = false) @QueryParam("guid") guid: String,
               @ApiParam(value = "GUID of the file's project to fetch", required = true) @QueryParam("project_guid") project_guid: String,
               @ApiParam(value = "Filename of the file to fetch", required = false) @QueryParam("filename") filename: String,
               @ApiParam(value = "Version of the file to fetch", required = true) @QueryParam("version") version: String,
               @ApiParam(value = "Title of the file to fetch", required = false) @QueryParam("title") title: String,
               @ApiParam(value = "Url key of the file to fetch", required = false) @QueryParam("url_key") url_key: String,
               @ApiParam(value = "Number of builds to return", required = false) @QueryParam("limit") limit: String,
               @ApiParam(value = "Page offset of the build to fetch", required = false) @QueryParam("offset") offset: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else if(version.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid version value"))
    } else {
      val fileQuery =
        FileQuery(guid = Option(guid), filename = Option(filename), url_key = Option(url_key),
          project_guid = Option(project_guid), version = Option(version), title = Option(title),
          limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
      JsonResponse(Global.files.search(fileQuery).toList)
    }
  }

  @ApiOperation(value = "Add/Update a file", response = classOf[File], httpMethod = "PUT", authorizations = "apiKey")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Validation exception")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Unique GUID for the new file", name = "guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Project GUID of the new file", name = "project_guid", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Version of the file", name = "version", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "The title/header of the file", name = "title", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "The filename of the file", name = "filename", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "The file path relative to the repository root", name = "relative_path", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "The HTML content of the file", name = "html", required = true, dataType = "String", paramType = "body")
  ))
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
            JsonResponse(Global.files.findByGuid(data.guid))
          }
          case Some(existingFile: File) => {
            FileHelper.getOrCreateContent(data.html) { contentGuid =>
              Global.files.update(existingFile.copy(content_guid = contentGuid))
              FileHelper.cleanupContent(existingFile.content_guid)
            }
            JsonResponse(Global.files.findByGuid(data.guid))
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
