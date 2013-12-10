package controllers.api

import javax.ws.rs.QueryParam

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import dao.util.FileHelper
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.{ApiListing, ResourceListing}
import util.Decompress
import play.api.Logger

@Api(value = "/api/swagger-api", description = "Operations for Swagger JSON Api files")
object SwaggerApiController extends BaseApiController {

  @ApiOperation(value = "Find Swagger Resource Listing JSON Api", notes = "Returns a Swagger Resource Listing JSON Api file", response = classOf[ResourceListing], httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "No Swagger Resource Listing exists for the project & version supplied"),
    new ApiResponse(code = 400, message = "Validation Exception")
  ))
  def getResourceListing(@ApiParam(value = "GUID of the resource's project to fetch", required = true) @QueryParam("project_guid") project_guid: String,
                         @ApiParam(value = "Version of the resource to fetch", required = true) @QueryParam("version") version: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else if(version.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid version value"))
    } else {
      val resource = Global.swaggerApiFiles.findByProjectAndVersion(project_guid, version, None)
      if(resource.isEmpty) {
        JsonResponse(new value.ApiResponse(404, "No Swagger Resource Listing exists for the project & version supplied"), 404)
      } else {
        JsonResponse(Decompress(Global.fileContents.findByGuid(resource.get.content_guid).get.content))
      }
    }
  }

  @ApiOperation(value = "Find Swagger Api Listing JSON Api", notes = "Returns a Swagger Api Listing JSON Api file", response = classOf[ApiListing], httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "No Swagger Api Listing exists for the project, version & path supplied"),
    new ApiResponse(code = 400, message = "Validation Exception")
  ))
  def getApiListing(@ApiParam(value = "GUID of the resource's project to fetch", required = true) @QueryParam("project_guid") project_guid: String,
                    @ApiParam(value = "Version of the resource to fetch", required = true) @QueryParam("version") version: String,
                    @ApiParam(value = "Api path respective to the Api's base path", required = true) @QueryParam("path") path: String) = Action { implicit request =>
    if(project_guid.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid project_guid value"))
    } else if(version.isEmpty) {
      JsonResponse(new value.ApiResponse(400, "Invalid version value"))
    } else {
      val resource = Global.swaggerApiFiles.findByProjectAndVersion(project_guid, version, Some("/"+path))
      if(resource.isEmpty) {
        JsonResponse(new value.ApiResponse(404, "No Swagger Resource Listing exists for the project & version supplied"), 404)
      } else {
        JsonResponse(Decompress(Global.fileContents.findByGuid(resource.get.content_guid).get.content))
      }
    }
  }

  @ApiOperation(value = "Add/Update a swagger JSON file", response = classOf[SwaggerApiFile], httpMethod = "PUT", authorizations = "apiKey")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Validation exception")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Unique GUID for the new Swagger api resource", name = "guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Project GUID of the new Swagger api resource", name = "project_guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Version of the api", name = "version", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "True if it's a resource listing or false if it's an Api listing", name = "listing", required = true, dataType = "Boolean", paramType = "body"),
    new ApiImplicitParam(value = "Api path respective to the Api's base path", name = "path", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "The JSON content of the Swagger listing", name = "json", required = true, dataType = "String", paramType = "body")
  ))
  def putApi = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putFileForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        data => createSwaggerFile(data)
      )
    }
  }

  def createSwaggerFile(data: PutSwaggerFileFormData)(implicit request: Request[Any]) = {
    Global.projects.findByGuid(data.project_guid) match {
      case None => {
        NotFound("Project for guid [%s] not found".format(data.project_guid))
      }
      case Some(project: Project) => {
        // Create the project version if it doesn't exist
        if(!project.versions.contains(data.version)) {
          Global.projects.update(project.copy(versions = project.versions ++ Seq(data.version)))
        }

        Global.swaggerApiFiles.findByGuid(data.guid) match {
          case None => {
            FileHelper.getOrCreateContent(data.json) { contentGuid =>
              val relativePath = if(data.listing) None else Some(data.relative_path.getOrElse(""))
              Global.swaggerApiFiles.create(project.guid, data.version, relativePath, contentGuid)
            }
            JsonResponse(Global.swaggerApiFiles.findByGuid(data.guid))
          }
          case Some(existingFile: SwaggerApiFile) => {
            FileHelper.getOrCreateContent(data.json) { contentGuid =>
              Global.swaggerApiFiles.update(existingFile.copy(content_guid = contentGuid))
              FileHelper.cleanupContent(existingFile.content_guid)
            }
            JsonResponse(Global.swaggerApiFiles.findByGuid(data.guid))
          }
        }
      }
    }
  }

  val putFileForm = Form {
    mapping("guid" -> nonEmptyText,
      "project_guid" -> nonEmptyText,
      "version" -> nonEmptyText,
      "listing" -> boolean,
      "path" -> optional(text),
      "json" -> nonEmptyText
    )(PutSwaggerFileFormData.apply)(PutSwaggerFileFormData.unapply)
  }

}
