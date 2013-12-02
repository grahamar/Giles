package models

import java.util.UUID

case class UserQuery(guid: Option[UUID] = None,
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
    offset.foreach { v => params += ("offset" -> v) }
    params.toList
  }

}

case class FavouriteQuery(guid: Option[UUID] = None,
                          user_guid: Option[UUID] = None,
                          project_guid: Option[UUID] = None,
                          limit: Option[Int] = None,
                          offset: Option[Int] = None,
                          order_by: Option[String] = None,
                          order_direction: Int = 1) {

  lazy val pagination = Pagination(limit, offset)

  lazy val sortOrder = order_by.map { field => SortOrder(field, order_direction) }

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    user_guid.foreach { v => params += ("user_guid" -> v) }
    project_guid.foreach { v => params += ("project_guid" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class BuildQuery(guid: Option[UUID] = None,
                      project_guid: Option[UUID] = None,
                      version: Option[String] = None,
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
    offset.foreach { v => params += ("offset" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class ProjectQuery(guid: Option[UUID] = None,
                        name: Option[String] = None,
                        author_usernames: Option[Seq[String]] = None,
                        url_key: Option[String] = None,
                        created_by: Option[String] = None,
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
    author_usernames.foreach { v => params += ("author_usernames" -> v) }
    created_by.foreach { v => params += ("created_by" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    order_by.foreach { v =>
      params += ("order_by" -> v.toString)
      params += ("order_direction" -> order_direction)
    }
    params.toList
  }

}

case class FileQuery(guid: Option[UUID] = None,
                     project_guid: Option[UUID] = None,
                     version: Option[String] = None,
                     title: Option[String] = None,
                     filename: Option[String] = None,
                     relative_path: Option[String] = None,
                     url_key: Option[String] = None,
                     limit: Option[Int] = None,
                     offset: Option[Int] = None) {

  lazy val pagination = Pagination(limit, offset)

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    project_guid.foreach { v => params += ("project_guid" -> v) }
    version.foreach { v => params += ("version" -> v) }
    title.foreach { v => params += ("title" -> v) }
    filename.foreach { v => params += ("filename" -> v) }
    relative_path.foreach { v => params += ("relative_path" -> v) }
    url_key.foreach { v => params += ("url_key" -> v) }
    limit.foreach { v => params += ("limit" -> v) }
    offset.foreach { v => params += ("offset" -> v) }
    params.toList
  }

}

case class FileContentsQuery(guid: Option[UUID] = None,
                     hash_key: Option[String] = None,
                     content_size: Option[Long] = None) {

  def params = {
    val params = scala.collection.mutable.ListBuffer[(String, Any)]()
    guid.foreach { v => params += ("guid" -> v) }
    hash_key.foreach { v => params += ("hash_key" -> v) }
    content_size.foreach { v => params += ("content_size" -> v) }
    params.toList
  }

}

case class ViewQuery(guid: Option[UUID] = None,
                     file_guid: Option[UUID] = None,
                     user_guid: Option[UUID] = None,
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

case class FileRollupQuery(file_guid: Option[UUID] = None,
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

case class UserFileRollupQuery(user_guid: Option[UUID] = None,
                               file_guid: Option[UUID] = None,
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
