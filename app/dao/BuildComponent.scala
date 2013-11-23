package dao

import scala.slick.lifted.Tag
import profile.simple._
import java.sql.Timestamp
import settings.Global
import play.api.Logger
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

trait Enum[A] {
  trait Value { self: A =>
    _values :+= this
  }
  private var _values = Seq.empty[A]
  def values = _values
  def apply(name: String): Option[A] = {
    values.find(_.toString.equals(name))
  }
  def unapply(value: A): String = {
    value.toString
  }
}

sealed trait BuildStatus extends BuildStatus.Value
object BuildStatus extends Enum[BuildStatus]
case object BuildSuccess extends BuildStatus
case object BuildFailure extends BuildStatus

case class Build(projectId: Long, version: ProjectVersion, message: String, created: Timestamp,
                 status: BuildStatus = BuildSuccess, startPage: Option[String] = None, id: Option[Long] = None)

case class ProjectAndBuilds(project: ProjectWithAuthors, builds: Seq[Build]) {
  def versions: Seq[ProjectVersion] = builds.map(_.version)
  def latestBuild: Option[Build] = builds.find(_.version == ProjectHelper.defaultProjectVersion)
}

object BuildHelper {
  def apply(result: ((Long, ProjectVersion, String, BuildStatus, Option[String], Long), Option[Timestamp])): Build = {
    val ((projectId, version, msg, status, startPage, id), created) = result
    Build(projectId, version, msg, created.get, status, startPage, Some(id))
  }
}

trait BuildComponent { this: ProjectComponent with ProjectVersionsComponent =>

  class Builds(tag: Tag) extends Table[Build](tag, "BUILDS") with IdAutoIncrement[Build] {
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

  def insertBuild(build: Build)(implicit session: Session) =
    build.copy(id = Some((buildsForInsert returning builds.map(_.id)) += build))

}

object BuildDAO {

  implicit val TimestampOrdering = Ordering.fromLessThan( (ths: Timestamp, that: Timestamp) => that.before(ths))
  implicit val getBuildResult = GetResult(b => Build(b.nextLong(), ProjectVersion(b.nextString()), b.nextString(),
    b.nextTimestamp(), BuildStatus(b.nextString()).get, b.nextStringOption(), b.nextLongOption()))

  def insertBuildSuccess(project: Project, version: ProjectVersion, startPage: String): Unit = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: Session =>
        Logger.info("Build successful for project ["+project.name+"] and version ["+version.versionName+"]")
        Global.dal.insertBuild(Build(projectId, version, "",
          new Timestamp(System.currentTimeMillis()), status = BuildSuccess, startPage = Some(startPage)))
      }
    }
  }

  def insertBuildFailure(project: Project, version: ProjectVersion, message: String): Unit = {
    project.id.map { projectId =>
      Global.db.withSession{ implicit session: Session =>
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

      Global.db.withSession{ implicit session: Session =>
        query.list.toSeq.map {
          case tuples => BuildHelper(tuples)
        }.headOption
      }
    }.flatten
  }

}