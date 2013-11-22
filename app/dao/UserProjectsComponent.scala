package dao

import scala.slick.lifted.{TableQuery, Tag}
import profile.simple._
import java.sql.Timestamp

trait UserProjectsComponent { this: UserComponent with ProjectComponent =>

  class UserProjects(tag: Tag) extends Table[(Long, Long)](tag, "USER_PROJECTS") {
    def userId = column[Long]("USR_ID")
    def projectId = column[Long]("PROJ_ID")

    def * = (userId, projectId)

    def userFK = foreignKey("usr_fk", userId, users)(_.id)
    def projectFK = foreignKey("proj_fk", projectId, projects)(_.id)
  }
  val userProjects = TableQuery[UserProjects]
}

case class ProjectWithAuthors(name: String, slug: String, url: String, defaultBranch: ProjectBranch,
                              defaultVersion: ProjectVersion, created: Timestamp, updated: Timestamp, id: Option[Long],
                              authors: Seq[User]) extends Project with Ordered[ProjectWithAuthors] {
  def compare(that: ProjectWithAuthors): Int = that.updated.compareTo(this.updated)
}
