package controllers.api

import play.api.libs.json.{Writes, Json}
import play.api.mvc._

import settings.Global

class BaseApiController extends Controller {

  def getOptions = Action { implicit request =>
    jsonResponse(new value.ApiResponse(200, "Ok"))
  }

  protected def jsonResponse[T](data: T)(implicit writes: Writes[T]) = {
    new SimpleResult(
      header = ResponseHeader(200),
      body = play.api.libs.iteratee.Enumerator(Json.toJson(data).toString().getBytes)
    ).as("application/json").withHeaders(
      ("Access-Control-Allow-Origin", "*"),
      ("Access-Control-Allow-Methods", "GET, PUT, OPTIONS"),
      ("Access-Control-Allow-Headers", "Content-Type, X-API-Key, X-API-Application")
    )
  }

  protected def jsonResponse[T](data: T, code: Int)(implicit writes: Writes[T]) = {
    new SimpleResult(
      header = ResponseHeader(code),
      body = play.api.libs.iteratee.Enumerator(Json.toJson(data).toString().getBytes)
    ).as("application/json").withHeaders(
      ("Access-Control-Allow-Origin", "*"),
      ("Access-Control-Allow-Methods", "GET, PUT, OPTIONS"),
      ("Access-Control-Allow-Headers", "Content-Type, X-API-Key, X-API-Application")
    )
  }

  def withApiKeyProtection(f: String => SimpleResult)(implicit request: RequestHeader): SimpleResult = {
    val xApiKey = request.headers.get("X-API-Key")
    val xApiApp = request.headers.get("X-API-Application")
    xApiKey.flatMap( apiKey => xApiApp.flatMap { apiApp =>
      Global.apiKeys.findByApiKey(apiKey).map { key =>
        f(key.user_guid)
      }
    }).getOrElse(jsonResponse(new value.ApiResponse(401, "Unauthorized")))
  }
}
