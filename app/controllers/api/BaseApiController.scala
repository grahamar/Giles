package controllers.api

import play.api.mvc._

import settings.Global
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.wordnik.swagger.core.util.ScalaJsonUtil

class BaseApiController extends Controller {

  def getOptions = Action { implicit request =>
    JsonResponse(new value.ApiResponse(200, "Ok"))
  }

  protected def JsonResponse(data: Object) = {
    val jsonValue: String = toJsonString(data)
    new SimpleResult(header = ResponseHeader(200), body = play.api.libs.iteratee.Enumerator(jsonValue.getBytes)).as("application/json")
      .withHeaders(
      ("Access-Control-Allow-Origin", "*"),
      ("Access-Control-Allow-Methods", "GET, PUT, OPTIONS"),
      ("Access-Control-Allow-Headers", "Content-Type, X-API-Key, X-API-Application"))
  }

  protected def JsonResponse(data: Object, code: Int) = {
    val jsonValue: String = toJsonString(data)
    new SimpleResult(header = ResponseHeader(code), body = play.api.libs.iteratee.Enumerator(jsonValue.getBytes)).as("application/json")
      .withHeaders(
      ("Access-Control-Allow-Origin", "*"),
      ("Access-Control-Allow-Methods", "GET, PUT, OPTIONS"),
      ("Access-Control-Allow-Headers", "Content-Type, X-API-Key, X-API-Application"))
  }

  def toJsonString(data: Any): String = {
    if (data.getClass.equals(classOf[String])) {
      data.asInstanceOf[String]
    } else {
      val mapper: ObjectMapper = ScalaJsonUtil.mapper
      mapper.registerModule(new JodaModule())
      mapper.writeValueAsString(data)
    }
  }

  def withApiKeyProtection(f: String => SimpleResult)(implicit request: RequestHeader): SimpleResult = {
    val xApiKey = request.headers.get("X-API-Key")
    val xApiApp = request.headers.get("X-API-Application")
    xApiKey.flatMap( apiKey => xApiApp.flatMap { apiApp =>
      Global.apiKeys.findByApiKey(apiKey).map { key =>
        f(key.user_guid)
      }
    }).getOrElse(JsonResponse(new value.ApiResponse(401, "Unauthorized")))
  }
}
