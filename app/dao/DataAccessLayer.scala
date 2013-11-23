package dao

import play.api.Logger
import scala.util.Try

/**
 * The Data Access Layer contains all components and a profile
 */
class DataAccessLayer(var database: driver.backend.Database) extends UserComponent with ProjectComponent
  with UserProjectsComponent with ProjectVersionsComponent with BuildComponent {

  Logger.info("Init Data Access Layer")

  import driver.profile.Implicit._

  def create(implicit session: driver.backend.Session): Unit =
    Try{(users.ddl ++ projects.ddl ++ userProjects.ddl ++ projectVersions.ddl ++ builds.ddl).create}

  def drop(implicit session: driver.backend.Session): Unit =
    (users.ddl ++ projects.ddl ++ userProjects.ddl ++ projectVersions.ddl ++ builds.ddl).drop

}
