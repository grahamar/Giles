package dao

import scala.slick.lifted.Tag
import profile.simple._
import com.google.common.base.CaseFormat
import java.sql.Date
import settings.Global

case class ProjectBranch(branchName: String) extends AnyVal
case class ProjectVersion(versionName: String) extends AnyVal

trait Project {
  val name: String
  val slug: String
  val url: String
  val tags: String
  val defaultBranch: ProjectBranch
  val defaultVersion: ProjectVersion
  val created: Date
  val updated: Date
  val id: Option[Long]
}

case class SimpleProject(name: String,
                         slug: String,
                         url: String = "",
                         tags: String = "",
                         defaultBranch: ProjectBranch = ProjectHelper.defaultProjectBranch,
                         defaultVersion: ProjectVersion = ProjectHelper.defaultProjectVersion,
                         created: Date = new Date(System.currentTimeMillis),
                         updated: Date = new Date(System.currentTimeMillis),
                         id: Option[Long] = None) extends Project

object ProjectHelper {
  def apply(name: String): SimpleProject = {
    new SimpleProject(name, urlForName(name))
  }
  def urlForName(name: String): String = {
    //TODO this is Rudimental at best...
    name.toLowerCase.replaceAll(" ", "-").replaceAll("\\.", "").replaceAll("'", "").replaceAll("\"", "")
  }

  lazy val defaultProjectBranch = ProjectBranch("master")
  lazy val defaultProjectVersion = ProjectVersion("latest")
}

trait ProjectComponent { this: UserProjectsComponent =>

  implicit val projectBranchColumnType = MappedColumnType.base[ProjectBranch, String]({ _.branchName }, ProjectBranch.apply)

  implicit val projectVersionColumnType = MappedColumnType.base[ProjectVersion, String]({ _.versionName }, ProjectVersion.apply)

  class Projects(tag: Tag) extends Table[SimpleProject](tag, "PROJECTS") with IdAutoIncrement[SimpleProject] {
    def name = column[String]("PROJ_NAME", O.NotNull)
    def slug = column[String]("PROJ_SLUG", O.NotNull)
    def url = column[String]("PROJ_URL", O.NotNull)
    def tags = column[String]("PROJ_TAGS", O.NotNull)
    def defaultBranch = column[ProjectBranch]("PROJ_DFLT_BRANCH", O.NotNull)
    def defaultVersion = column[ProjectVersion]("PROJ_DFLT_VERSION", O.NotNull)
    def created = column[Date]("PROJ_CREATED", O.NotNull)
    def updated = column[Date]("PROJ_UPDATED", O.NotNull)
    def authors = userProjects.filter(_.projectId === id).flatMap(_.userFK)

    def * = (name, slug, url, tags, defaultBranch, defaultVersion, created, updated, id.?) <> (SimpleProject.tupled, SimpleProject.unapply _)
  }
  val projects = TableQuery[Projects]

  def projectsForInsert = projects.map(p => (p.name, p.slug, p.url, p.tags, p.defaultBranch, p.defaultVersion, p.created, p.updated).shaped <>
    ({ t => SimpleProject(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, None)}, { (p: SimpleProject) =>
      Some((p.name, p.slug, p.url, p.tags, p.defaultBranch, p.defaultVersion, p.created, p.updated))}))

  def insertProject(project: SimpleProject)(implicit session: Session) =
    project.copy(id = Some((projectsForInsert returning projects.map(_.id)) += project))
}

object ProjectDAO {

  def insertProject(project: SimpleProject): Project = {
    Global.db.withSession{ implicit session: Session =>
      Global.dal.insertProject(project)
    }
  }
}