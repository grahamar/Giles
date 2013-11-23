package dao

import java.sql.Timestamp
import play.api.Logger
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.slick.lifted.{TableQuery, Tag}
import driver.profile.simple._

import settings.Global

trait BuildComponent { this: ProjectComponent with ProjectVersionsComponent =>

  class Builds(tag: Tag) extends AbstTable[Build](tag, "BUILDS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def projectId = column[Long]("BLD_PROJID", O.NotNull)
    def version = column[ProjectVersion]("BLD_VERSION", O.NotNull)
    def message = column[String]("BLD_MSG", O.NotNull)
    def created = column[Timestamp]("BLD_CREATED", O.NotNull)
    def status = column[BuildStatus]("BLD_STATUS", O.NotNull)
    def startPage = column[Option[String]]("BLD_PAGE", O.Nullable)

    def * = (projectId, version, message, created, status, startPage, id.?) <> (Build.tupled, Build.unapply _)
  }
  val builds = TableQuery[Builds]

  private def buildsForInsert = builds.map(b => (b.projectId, b.version, b.message, b.created, b.status, b.startPage).shaped <>
    ({ t => Build(t._1, t._2, t._3, t._4, t._5, t._6, None)}, { (b: Build) =>
      Some((b.projectId, b.version, b.message, b.created, b.status, b.startPage))}))

  def insertBuild(build: Build)(implicit session: driver.backend.Session) =
    build.copy(id = Some((buildsForInsert returning builds.map(_.id)) += build))

}

object BuildDAO {

  implicit val TimestampOrdering = Ordering.fromLessThan( (ths: Timestamp, that: Timestamp) => that.before(ths))
  implicit val getBuildResult = GetResult(b => Build(b.nextLong(), ProjectVersion(b.nextString()), b.nextString(),
    b.nextTimestamp(), BuildStatus(b.nextString()).get, b.nextStringOption(), b.nextLongOption()))

  def insertBuildSuccess(project: Project, version: ProjectVersion, startPage: String): Unit = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: driver.backend.Session =>
        Logger.info("Build successful for project ["+project.name+"] and version ["+version.versionName+"]")
        Global.dal.insertBuild(Build(projectId, version, "",
          new Timestamp(System.currentTimeMillis()), status = BuildSuccess, startPage = Some(startPage)))
      }
    }
  }

  def insertBuildFailure(project: Project, version: ProjectVersion, message: String): Unit = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: driver.backend.Session =>
        Logger.warn("Build failed for project ["+project.name+"] and version ["+version.versionName+"] - "+message)
        Global.dal.insertBuild(Build(projectId, version, message, new Timestamp(System.currentTimeMillis()), status = BuildFailure))
      }
    }
  }

  def latestBuildsForProject(project: Project): Seq[Build] = {

    import scala.slick.driver.JdbcDriver.backend.Database
    import Database.dynamicSession

    project.id.map { projectId =>
      Global.db.withDynSession {
        Q.query[Long, Build]("""
          |select *
          |  from
          |    builds a
          |    inner join
          |       (select BLD_PROJID, BLD_VERSION, max(BLD_CREATED) as mcreated from builds group by BLD_PROJID, BLD_VERSION) b
          |    on a.BLD_PROJID = b.BLD_PROJID and a.BLD_VERSION = b.BLD_VERSION and a.BLD_CREATED = b.mcreated
          |  where
          |     a.BLD_PROJID = ?
        """.stripMargin).list(projectId).toSeq
      }
    }.getOrElse(Seq.empty)
  }

  def latestBuildForProjectVersion(project: Project, version: ProjectVersion): Option[Build] = {
    project.id.map { projectId =>
      val query = (for {
        b <- Global.dal.builds if b.projectId === projectId && b.version === version
      } yield b).
        groupBy( b => (b.projectId, b.version, b.message, b.status, b.startPage, b.id)).
        map{ case (cols, b) => cols -> b.map(_.created).max }

      Global.db.withSession{ implicit session: driver.backend.Session =>
        query.list.toSeq.map {
          case tuples => BuildHelper(tuples)
        }.headOption
      }
    }.flatten
  }

}