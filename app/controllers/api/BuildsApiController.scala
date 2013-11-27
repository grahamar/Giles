package controllers.api

import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._

import models._
import settings.Global

object BuildsApiController extends Controller  {

  def getBuilds(guid: Option[String], projectGuid: Option[String], version: Option[String], limit: Option[String], offset: Option[String]) = Action {
    val buildQuery = BuildQuery(guid = guid.map(_.toGuid), project_guid = projectGuid.map(_.toGuid), version = version.map(_.toVersion), limit = limit.map(_.toInt), offset = offset.map(_.toInt))
    Ok(Json.toJson(Global.builds.search(buildQuery).toList.map(Json.toJson(_))))
  }

}
