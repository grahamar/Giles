package dao

import profile.simple._
import play.api.Logger

/**
 * The Data Access Layer contains all components and a profile
 */
class DAL(var database: dao.profile.backend.Database) extends UserComponent with ProjectComponent with UserProjectsComponent {

  Logger.info("Init Data Access Layer")

  def create(implicit session: Session): Unit = {
    (users.ddl ++ projects.ddl ++ userProjects.ddl).create
  }

  def drop(implicit session: Session): Unit = {
    (users.ddl ++ projects.ddl ++ userProjects.ddl).drop
  }
}

trait IdAutoIncrement[T <: AnyRef] {
  self: Table[T] =>

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def findById(idToFind: Column[Long]): Column[Boolean] = id === idToFind
}
