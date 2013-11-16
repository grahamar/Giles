package dao

import scala.slick.lifted.Tag
import profile.simple._
import com.google.common.base.CaseFormat
import java.sql.Date

case class Project(name: String, url: String, created: Date, updated: Date, id: Option[Long])
object ProjectFactory {
  def apply(name: String): Project = {
    new Project(name, urlForName(name), new Date(System.currentTimeMillis), new Date(System.currentTimeMillis), None)
  }
  def urlForName(name: String): String = {
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name)
  }
}

trait ProjectComponent { this: UserProjectsComponent =>

  class Projects(tag: Tag) extends Table[Project](tag, "PROJECTS") with IdAutoIncrement[Project] {
    def name = column[String]("PROJ_NAME", O.NotNull)
    def url = column[String]("PROJ_URL", O.NotNull)
    def created = column[Date]("PROJ_CREATED", O.NotNull)
    def updated = column[Date]("PROJ_UPDATED", O.NotNull)
    def authors = userProjects.filter(_.projectId === id).flatMap(_.userFK)

    def * = (name, url, created, updated, id.?) <> (Project.tupled, Project.unapply _)
  }
  val projects = TableQuery[Projects]

  def projectsForInsert = projects.map(p => (p.name, p.url, p.created, p.updated).shaped <>
    ({ t => Project(t._1, t._2, t._3, t._4, None)}, { (p: Project) =>
      Some((p.name, p.url, p.created, p.updated))}))

  def insertProject(project: Project)(implicit session: Session) =
    project.copy(id = Some((projectsForInsert returning projects.map(_.id)) += project))
}