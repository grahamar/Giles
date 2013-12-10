package controllers.api

import javax.ws.rs.QueryParam

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import com.wordnik.swagger.annotations._

@Api(value = "/api/builds", description = "Operations for document builds")
object BuildsApiController extends BaseApiController {

  @ApiOperation(value = "Find builds by project GUID", notes = "Returns a list of builds for a project", response = classOf[Build], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Invalid project_guid value")))
  def getBuilds(@ApiParam(value = "GUID of the build's project to fetch", required = true) @QueryParam("project_guid") project_guid: String,
                @ApiParam(value = "GUID of the build to fetch", required = false) @QueryParam("guid") guid: String,
                @ApiParam(value = "Version of the build to fetch", required = false) @QueryParam("version") version: String,
                @ApiParam(value = "Number of builds to return", required = false) @QueryParam("limit") limit: String,
                @ApiParam(value = "Page offset of the build to fetch", required = false) @QueryParam("offset") offset: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else {
      val buildQuery = BuildQuery(guid = Option(guid), project_guid = Some(project_guid),
        version = Option(version), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
      JsonResponse(Global.builds.search(buildQuery).toList)
    }
  }

  @ApiOperation(value = "Add/Update a build", response = classOf[Build], httpMethod = "PUT", authorizations = "apiKey")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Validation exception")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Unique GUID for the new build", name = "guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Project GUID for the new build", name = "project_guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Usernames of the build's authors", name = "author_usernames", required = true, dataType = "List", paramType = "body")
  ))
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
        JsonResponse(Global.builds.findByGuid(buildData.guid))
      }
      case Some(existing: Build) => {
        val updated = existing.copy(authors = buildData.author_usernames)
        Global.builds.update(updated)
        JsonResponse(Global.builds.findByGuid(buildData.guid))
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
