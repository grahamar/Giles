package dao

import scala.slick.lifted.Tag
import profile.simple._
import java.sql.Timestamp
import settings.Global
import play.api.Logger

case class ProjectBranch(branchName: String) extends AnyVal
case class ProjectVersion(versionName: String) extends AnyVal

trait Project {
  val name: String
  val slug: String
  val url: String
  val defaultBranch: ProjectBranch
  val defaultVersion: ProjectVersion
  val created: Timestamp
  val updated: Timestamp
  val id: Option[Long]
}

case class SimpleProject(name: String,
                         slug: String,
                         url: String = "",
                         defaultBranch: ProjectBranch = ProjectHelper.defaultProjectBranch,
                         defaultVersion: ProjectVersion = ProjectHelper.defaultProjectVersion,
                         created: Timestamp = new Timestamp(System.currentTimeMillis),
                         updated: Timestamp = new Timestamp(System.currentTimeMillis),
                         id: Option[Long] = None) extends Project

case class VersionWithProject(projectId: Long, version: String)

case class ProjectAndVersions(project: ProjectWithAuthors, versions: Seq[ProjectVersion])

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

trait ProjectComponent { this: UserProjectsComponent with ProjectVersionsComponent =>

  class Projects(tag: Tag) extends Table[SimpleProject](tag, "PROJECTS") with IdAutoIncrement[SimpleProject] {
    def name = column[String]("PROJ_NAME", O.NotNull)
    def slug = column[String]("PROJ_SLUG", O.NotNull)
    def url = column[String]("PROJ_URL", O.NotNull)
    def defaultBranch = column[ProjectBranch]("PROJ_DFLT_BRANCH", O.NotNull)
    def defaultVersion = column[ProjectVersion]("PROJ_DFLT_VERSION", O.NotNull)
    def created = column[Timestamp]("PROJ_CREATED", O.NotNull)
    def updated = column[Timestamp]("PROJ_UPDATED", O.NotNull)
    def authors = userProjects.filter(_.projectId === id).flatMap(_.userFK)

    def * = (name, slug, url, defaultBranch, defaultVersion, created, updated, id.?) <> (SimpleProject.tupled, SimpleProject.unapply _)

    def idx_name = index("idx_proj", name, unique = true)
    def idx_slug = index("idx_slug", slug, unique = true)
  }
  val projects = TableQuery[Projects]

  private def projectsForInsert = projects.map(p => (p.name, p.slug, p.url, p.defaultBranch, p.defaultVersion, p.created, p.updated).shaped <>
    ({ t => SimpleProject(t._1, t._2, t._3, t._4, t._5, t._6, t._7, None)}, { (p: SimpleProject) =>
      Some((p.name, p.slug, p.url, p.defaultBranch, p.defaultVersion, p.created, p.updated))}))

  def insertProject(project: SimpleProject)(implicit session: Session) =
    project.copy(id = Some((projectsForInsert returning projects.map(_.id)) += project))
}

trait ProjectVersionsComponent { this: ProjectComponent =>

  class ProjectVersions(tag: Tag) extends Table[VersionWithProject](tag, "PROJECT_VERSIONS") {
    def projectId = column[Long]("PROJ_ID", O.NotNull)
    def version = column[String]("VERSION", O.NotNull)

    def * = (projectId, version) <> (VersionWithProject.tupled, VersionWithProject.unapply _)

    def pk = primaryKey("pk_proj_version", (projectId, version))

    def project = foreignKey("PROJ_FK", projectId, projects)(_.id)
  }
  val projectVersions = TableQuery[ProjectVersions]

}

object ProjectDAO {

  def recentlyUpdatedProjectsWithAuthors(limit: Int): Seq[ProjectAndVersions] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.take(limit).list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
      getProjectVersions(projectWithAuthors)
    }
  }

  def projectsWithAuthors: Seq[ProjectAndVersions] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
      getProjectVersions(projectWithAuthors)
    }
  }

  def projectsForUser(user: User): Seq[ProjectAndVersions] = {
    user.id.map{ userId =>
      val query = (for {
        u <- Global.dal.users
        p <- Global.dal.projects
        up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId && u.id === userId
      } yield (u, p)).sortBy(_._2.name.asc)

      val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
        Global.db.withSession{ implicit session: dao.profile.backend.Session =>
          query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
        }
      // Remove the current user from authors
      UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
        getProjectVersions(projectWithAuthors.copy(authors = projectWithAuthors.authors.diff(Seq(user))))
      }
    }.getOrElse(Seq.empty)
  }

  def insertProject(project: SimpleProject): Project = {
    Global.db.withSession{ implicit session: Session =>
      Global.dal.insertProject(project)
    }
  }

  def insertUserProject(userIdProjId: (Long, Long)) = {
    Global.db.withSession{ implicit session: Session =>
      Global.dal.userProjects.insert(userIdProjId)
    }
  }

  def findBySlug(projectSlug: String): Option[ProjectWithAuthors] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects if p.slug === projectSlug
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectAndAuthors: Option[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq.headOption
      }
    UserDAO.mapProjectWithAuthors(projectAndAuthors)
  }

  def findBySlugWithVersions(projectSlug: String): Option[ProjectAndVersions] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects if p.slug === projectSlug
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectAndAuthors: Option[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq.headOption
      }
    UserDAO.mapProjectWithAuthors(projectAndAuthors).map { projectAndAuthors =>
      getProjectVersions(projectAndAuthors)
    }
  }

  def getProjectVersions(project: ProjectWithAuthors): ProjectAndVersions = {
    val query = for {
      v <- Global.dal.projectVersions if v.projectId === project.id
    } yield v

    Global.db.withSession{ implicit session: dao.profile.backend.Session =>
      ProjectAndVersions(project, query.list.map{ v: VersionWithProject => ProjectVersion(v.version)}.toSeq)
    }
  }

  def insertProjectVersions(project: ProjectWithAuthors, versions: Seq[ProjectVersion]): ProjectAndVersions = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        versions.foreach { version =>
          Global.dal.projectVersions.insert(VersionWithProject(projectId, version.versionName))
        }
      }
    }
    ProjectAndVersions(project, versions)
  }

}