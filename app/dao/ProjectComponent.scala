package dao

import scala.slick.lifted.Tag
import profile.simple._
import com.google.common.base.CaseFormat
import java.sql.Date

trait Project {
  val name: String
  val url: String
  val created: Date
  val updated: Date
  val id: Option[Long]
}

case class SimpleProject(name: String, url: String, created: Date, updated: Date, id: Option[Long]) extends Project

object ProjectFactory {
  def apply(name: String): SimpleProject = {
    new SimpleProject(name, urlForName(name), new Date(System.currentTimeMillis), new Date(System.currentTimeMillis), None)
  }
  def urlForName(name: String): String = {
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name)
  }
}

trait ProjectComponent { this: UserProjectsComponent =>

  class Projects(tag: Tag) extends Table[SimpleProject](tag, "PROJECTS") with IdAutoIncrement[SimpleProject] {
    def name = column[String]("PROJ_NAME", O.NotNull)
    def url = column[String]("PROJ_URL", O.NotNull)
    def created = column[Date]("PROJ_CREATED", O.NotNull)
    def updated = column[Date]("PROJ_UPDATED", O.NotNull)
    def authors = userProjects.filter(_.projectId === id).flatMap(_.userFK)

    def * = (name, url, created, updated, id.?) <> (SimpleProject.tupled, SimpleProject.unapply _)
  }
  val projects = TableQuery[Projects]

  def projectsForInsert = projects.map(p => (p.name, p.url, p.created, p.updated).shaped <>
    ({ t => SimpleProject(t._1, t._2, t._3, t._4, None)}, { (p: SimpleProject) =>
      Some((p.name, p.url, p.created, p.updated))}))

  def insertProject(project: SimpleProject)(implicit session: Session) =
    project.copy(id = Some((projectsForInsert returning projects.map(_.id)) += project))
}