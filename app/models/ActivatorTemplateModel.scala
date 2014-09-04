package models

import org.joda.time.DateTime

case class ActivatorTemplateModel(guid: String, repo: String, created_at: DateTime)

case class ActivatorTemplateModelQuery(guid: Option[String] = None,
                               repo: Option[String] = None,
                               limit: Option[Int] = None,
                               offset: Option[Int] = None) {

  lazy val pagination = Pagination(limit, offset)

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    repo.foreach { v => params += ("repo" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    params.toList
  }

}
