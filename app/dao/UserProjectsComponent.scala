package dao

import scala.slick.lifted.{TableQuery, Tag}
import driver.profile.simple._

trait UserProjectsComponent { this: UserComponent with ProjectComponent =>

  class UserProjects(tag: Tag) extends AbstTable[(Long, Long)](tag, "USER_PROJECTS") {
    def userId = column[Long]("USR_ID")
    def projectId = column[Long]("PROJ_ID")

    def * = (userId, projectId)

    def userFK = foreignKey("usr_fk", userId, users)(_.id)
    def projectFK = foreignKey("proj_fk", projectId, projects)(_.id)
  }
  val userProjects = TableQuery[UserProjects]
}
