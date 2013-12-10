package controllers.api

import javax.ws.rs.QueryParam

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import settings.Global
import models._
import com.wordnik.swagger.annotations._

@Api(value = "/api/users", description = "Operations for users")
object UsersApiController extends BaseApiController {

  @ApiOperation(value = "Find users", notes = "Returns a list of users", response = classOf[JsonUser], responseContainer = "List", httpMethod = "GET")
  def getUsers(@ApiParam(value = "GUID of the user to fetch", required = false) @QueryParam("guid") guid: String,
               @ApiParam(value = "Username of the user to fetch", required = false) @QueryParam("username") username: String,
               @ApiParam(value = "Email address of the user to fetch", required = false) @QueryParam("email") email: String,
               @ApiParam(value = "Number of users to return", required = false) @QueryParam("limit") limit: String,
               @ApiParam(value = "Page offset of the users to fetch", required = false) @QueryParam("offset") offset: String) = Action { implicit request =>
    val usersQuery =
      UserQuery(guid = Option(guid), username = Option(username), email = Option(email), limit = Option(limit).map(_.toInt), offset = Option(offset).map(_.toInt))
    JsonResponse(Global.users.search(usersQuery).toList.map(_.toJsonUser))
  }

  @ApiOperation(value = "Add/Update a user", response = classOf[JsonUser], httpMethod = "PUT", authorizations = "apiKey")
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Validation exception")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(value = "Unique GUID for the new user", name = "guid", required = true, dataType = "UUID", paramType = "body"),
    new ApiImplicitParam(value = "Username of the new user", name = "username", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Email address for the new user", name = "email", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Password for the new user", name = "password", required = true, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "First name of the user", name = "first_name", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Last name of the user", name = "last_name", required = false, dataType = "String", paramType = "body"),
    new ApiImplicitParam(value = "Homepage for the user", name = "homepage", required = false, dataType = "String", paramType = "body")
  ))
  def putUsers = Action { implicit request =>
    withApiKeyProtection { userGuid =>
      putUserForm.bindFromRequest.fold(
        formWithErrors => BadRequest(formWithErrors.errorsAsJson),
        data => createUser(data)
      )
    }
  }

  def createUser(data: PutUserFormData)(implicit request: Request[Any]) = {
    Global.users.findByGuid(data.guid) match {
      case None => {
        Global.users.create(data.guid, data.username, data.email, data.password, data.first_name, data.last_name)
        JsonResponse(Global.projects.findByGuid(data.guid))
      }
      case Some(existing: User) => {
        val updated = existing.copy(username = data.username, email = data.email, first_name = data.first_name, last_name = data.last_name, homepage = data.homepage)
        Global.users.update(updated)
        JsonResponse(Global.projects.findByGuid(data.guid))
      }
      case _ => InternalServerError
    }
  }

  val putUserForm = Form {
    mapping("guid" -> nonEmptyText.verifying(o => isUUID(o)),
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8),
      "first_name" -> optional(text),
      "last_name" -> optional(text),
      "homepage" -> optional(text)
    )(PutUserFormData.apply)(PutUserFormData.unapply)
  }

}
