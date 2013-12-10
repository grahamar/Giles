package controllers.api

import javax.ws.rs.QueryParam

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._
import com.wordnik.swagger.annotations._

@Api(value = "/api/projects", description = "Operations for projects")
object ProjectsApiController extends BaseApiController {

  @ApiOperation(value = "Find projects", notes = "Returns a list of projects", response = classOf[Project], responseContainer = "List", httpMethod = "GET")
  def getProjects(@ApiParam(value = "GUID of the project to fetch", required = false) @QueryParam("guid") guid: String,
                  @ApiParam(value = "Name of the project to fetch", required = false) @QueryParam("name") name: String,
                  @ApiParam(value = "URL key of the project to fetch", required = false) @QueryParam("url_key") url_key: String,
                  @ApiParam(value = "Number of projects to return", required = false) @QueryParam("limit") limit: String,
                  @ApiParam(value = "Page offset of the projects to fetch", required = false) @QueryParam("offset") offset: String) = Action { implicit request =>
    val projectQuery = ProjectQuery(guid = Option(guid), name = Option(name), url_key = Option(url_key), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
    JsonResponse(Global.projects.search(projectQuery).toList)
  }

  @ApiOperation(value = "Add/Update a project", response = classOf[Project], httpMethod = "PUT", authorizations = "apiKey")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Validation exception")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Unique GUID for the new project", name = "guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Name of the new build", name = "name", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Description of the new project", name = "description", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Username of the user putting the new project", name = "created_by", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Repository URL of the project", name = "repo_url", required = true, dataType = "URL", paramType = "body"),
    new ApiImplicitParam(value = "A list of project author names", name = "author_usernames", required = true, dataType = "List", paramType = "body"),
    new ApiImplicitParam(value = "The latest/head version.", name = "head_version", defaultValue = "HEAD", required = false, dataType = "String", paramType = "body")
  ))
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
        JsonResponse(Global.projects.findByGuid(projectData.guid))
      }
      case Some(existing: Project) => {
        val updated = existing.copy(description = projectData.description.getOrElse(""), head_version = projectData.head_version.getOrElse("HEAD"))
        Global.projects.update(updated)
        JsonResponse(Global.projects.findByGuid(projectData.guid))
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
