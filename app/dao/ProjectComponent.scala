package dao

import scala.slick.lifted.{TableQuery, Tag}
import driver.profile.simple._
import java.sql.Timestamp
import settings.Global

trait ProjectComponent { this: UserProjectsComponent with ProjectVersionsComponent =>

  class Projects(tag: Tag) extends AbstTable[SimpleProject](tag, "PROJECTS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("PROJ_NAME", O.NotNull)
    def slug = column[String]("PROJ_SLUG", O.NotNull)
    def url = column[String]("PROJ_URL", O.NotNull)
    def defaultBranch = column[ProjectBranch]("PROJ_DFLT_BRANCH", O.NotNull)
    def defaultVersion = column[ProjectVersion]("PROJ_DFLT_VERSION", O.NotNull)
    def created = column[Timestamp]("PROJ_CREATED", O.NotNull)
    def updated = column[Timestamp]("PROJ_UPDATED", O.NotNull)
    def authors = userProjects.filter(_.projectId === id).flatMap(_.userFK)

    def * = (name, slug, url, defaultBranch, defaultVersion, created, updated, id.?) <> (SimpleProject.tupled, SimpleProject.unapply)

    def idx_name = index("idx_proj", name, unique = true)
    def idx_slug = index("idx_slug", slug, unique = true)
  }
  val projects = TableQuery[Projects]

  private def projectsForInsert = projects.map(p => (p.name, p.slug, p.url, p.defaultBranch, p.defaultVersion, p.created, p.updated).shaped <>
    ({ t => SimpleProject(t._1, t._2, t._3, t._4, t._5, t._6, t._7, None)}, { (p: SimpleProject) =>
      Some((p.name, p.slug, p.url, p.defaultBranch, p.defaultVersion, p.created, p.updated))}))

  def insertProject(project: SimpleProject)(implicit session: dao.driver.backend.Session) =
    project.copy(id = Some((projectsForInsert returning projects.map(_.id)) += project))
}

trait ProjectVersionsComponent { this: ProjectComponent =>

  class ProjectVersions(tag: Tag) extends AbstTable[VersionWithProject](tag, "PROJECT_VERSIONS") {
    def projectId = column[Long]("PROJ_ID", O.NotNull)
    def version = column[String]("VERSION", O.NotNull)

    def * = (projectId, version) <> (VersionWithProject.tupled, VersionWithProject.unapply)

    def pk = primaryKey("pk_proj_version", (projectId, version))

    def project = foreignKey("PROJ_FK", projectId, projects)(_.id)
  }
  val projectVersions = TableQuery[ProjectVersions]

}

object ProjectDAO {

  def recentlyUpdatedProjectsWithAuthors(limit: Int): Seq[ProjectAndBuilds] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.driver.backend.Session =>
        query.take(limit).list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
      ProjectAndBuilds(projectWithAuthors, BuildDAO.latestBuildsForProject(projectWithAuthors))
    }
  }

  def projectsWithAuthors: Seq[ProjectAndBuilds] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.driver.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
      ProjectAndBuilds(projectWithAuthors, BuildDAO.latestBuildsForProject(projectWithAuthors))
    }
  }

  def projectsForUser(user: User): Seq[ProjectAndBuilds] = {
    user.id.map{ userId =>
      val query = (for {
        u <- Global.dal.users
        p <- Global.dal.projects
        up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId && u.id === userId
      } yield (u, p)).sortBy(_._2.name.asc)

      val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
        Global.db.withSession{ implicit session: dao.driver.backend.Session =>
          query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
        }
      // Remove the current user from authors
      UserDAO.mapProjectWithAuthors(projectsAndAuthors).sorted.map { projectWithAuthors =>
        ProjectAndBuilds(projectWithAuthors.copy(authors = projectWithAuthors.authors.diff(Seq(user))),
          BuildDAO.latestBuildsForProject(projectWithAuthors))
      }
    }.getOrElse(Seq.empty)
  }

  def insertProject(project: SimpleProject): Project = {
    Global.db.withSession{ implicit session: dao.driver.backend.Session =>
      Global.dal.insertProject(project)
    }
  }

  def insertUserProject(userIdProjId: (Long, Long)) = {
    Global.db.withSession{ implicit session: dao.driver.backend.Session =>
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
      Global.db.withSession{ implicit session: dao.driver.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq.headOption
      }
    UserDAO.mapProjectWithAuthors(projectAndAuthors)
  }

  def findBySlugWithVersions(projectSlug: String): Option[ProjectAndBuilds] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects if p.slug === projectSlug
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectAndAuthors: Option[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.driver.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq.headOption
      }
    UserDAO.mapProjectWithAuthors(projectAndAuthors).map { projectAndAuthors =>
      ProjectAndBuilds(projectAndAuthors, BuildDAO.latestBuildsForProject(projectAndAuthors))
    }
  }

  def insertProjectVersions(project: ProjectWithAuthors, versions: Seq[ProjectVersion]): ProjectAndVersions = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: dao.driver.backend.Session =>
        versions.foreach { version =>
          Global.dal.projectVersions.insert(VersionWithProject(projectId, version.versionName))
        }
      }
    }
    ProjectAndVersions(project, versions)
  }

}