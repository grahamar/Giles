package controllers.api

import play.api.mvc._

import models._
import settings.Global
import org.joda.time.DateTime

object ViewsApiController extends BaseApiController {

  def getViews = Action { implicit request =>
    val monthViews = Global.views.search(ViewQuery(start_date = Option(new DateTime().minusMonths(1)), end_at = Option(new DateTime())))
    JsonResponse(monthViews.map(v => ViewJson(v.created_at.toString("DD-MMM-YY"), 1)).groupBy(_.viewed_at).
      map{ case (dt: String, views: Iterable[ViewJson]) => ViewJson(dt, views.size)}.toList)
  }

}

case class ViewJson(viewed_at: String, views: Int)
