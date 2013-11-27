package models

case class UserQuery(guid: Option[Guid] = None,
                     username: Option[String] = None,
                     email: Option[String] = None,
                     limit: Option[Int] = None,
                     offset: Option[Int] = None) {

  lazy val pagination = Pagination(limit, offset)

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    username.foreach { v => params += ("username" -> v) }
    email.foreach { v => params += ("email" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offst" -> v) }
    params.toList
  }

}

case class BuildQuery(guid: Option[Guid] = None,
                      project_guid: Option[Guid] = None,
                      version: Option[Version] = None,
                      limit: Option[Int] = None,
                      offset: Option[Int] = None,
                      order_by: Option[String] = None,
                      order_direction: Int = 1) {

  lazy val pagination = Pagination(limit, offset)

  lazy val sortOrder = order_by.map { field => SortOrder(field, order_direction) }

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    project_guid.foreach { v => params += ("project_guid" -> v) }
    version.foreach { v => params += ("version" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offst" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class ProjectQuery(guid: Option[Guid] = None,
                        name: Option[String] = None,
                        author_guids: Option[Seq[Guid]] = None,
                        query: Option[String] = None,
                        url_key: Option[UrlKey] = None,
                        limit: Option[Int] = None,
                        offset: Option[Int] = None,
                        order_by: Option[String] = None,
                        order_direction: Int = 1) {

  lazy val pagination = Pagination(limit, offset)

  lazy val sortOrder = order_by.map { field => SortOrder(field, order_direction) }

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    name.foreach { v => params += ("name" -> v) }
    url_key.foreach { v => params += ("url_key" -> v) }
    author_guids.foreach { v => params += ("author_guids" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offst" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class FileQuery(guid: Option[Guid] = None,
                     project_guid: Option[Guid] = None,
                     version: Option[Version] = None,
                     query: Option[String] = None,
                     title: Option[String] = None,
                     url_key: Option[UrlKey] = None,
                     limit: Option[Int] = None,
                     offset: Option[Int] = None) {

  lazy val pagination = Pagination(limit, offset)

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    project_guid.foreach { v => params += ("project_guid" -> v) }
    version.foreach { v => params += ("version" -> v) }
    title.foreach { v => params += ("title" -> v) }
    query.foreach { v => params += ("query" -> v) }
    url_key.foreach { v => params += ("url_key" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offst" -> v) }
    params.toList
  }

}

case class ViewQuery(guid: Option[Guid] = None,
                     file_guid: Option[Guid] = None,
                     user_guid: Option[Guid] = None,
                     limit: Option[Int] = None,
                     offset: Option[Int] = None) {

  lazy val pagination = Pagination(limit, offset)

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    file_guid.foreach { v => params += ("file_guid" -> v) }
    user_guid.foreach { v => params += ("user_guid" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    params.toList
  }

}

case class FileRollupQuery(file_guid: Option[Guid] = None,
                           limit: Option[Int] = None,
                           offset: Option[Int] = None,
                           order_by: Option[String] = None,
                           order_direction: Int = 1) {

  lazy val pagination = Pagination(limit, offset)

  lazy val sortOrder = order_by.map { field => SortOrder(field, order_direction) }

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    file_guid.foreach { v => params += ("file_guid" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class UserFileRollupQuery(user_guid: Option[Guid] = None,
                               file_guid: Option[Guid] = None,
                               limit: Option[Int] = None,
                               offset: Option[Int] = None,
                               order_by: Option[String] = None,
                               order_direction: Int = 1) {

  lazy val pagination = Pagination(limit, offset)

  lazy val sortOrder = order_by.map { field => SortOrder(field, order_direction) }

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    file_guid.foreach { v => params += ("file_guid" -> v) }
    user_guid.foreach { v => params += ("user_guid" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class Pagination(limit: Option[Int], offset: Option[Int]) {

  private val DefaultLimit = 50
  private val DefaultOffset = 0

  def limitOrDefault = limit.getOrElse(DefaultLimit)

  def offsetOrDefault = offset.getOrElse(DefaultOffset)
}

case class SortOrder(field: String, direction: Int) {
  require(direction == 1 || direction == -1, "direction[%s] is invalid - must be 1 or -1".format(direction))
}
