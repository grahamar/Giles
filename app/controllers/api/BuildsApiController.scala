package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global
import java.util.UUID

object BuildsApiController extends Controller  {

  def getBuilds(guid: Option[String], projectGuid: Option[String], version: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val buildQuery = BuildQuery(guid = guid.map(UUID.fromString), project_guid = projectGuid.map(UUID.fromString), version = version, limit = limit.map(_.toInt), offset = offset.map(_.toInt))
    Ok(Json.toJson(Global.builds.search(buildQuery).toList.map(Json.toJson(_))))
  }

}
