package dao

import java.sql.Date

import scala.slick.lifted.Tag
import profile.simple._
import settings.Global

case class User(username: String,
                email: String,
                firstName: Option[String] = None,
                lastName: Option[String] = None,
                homepage: Option[String] = None,
                joined: Date = new Date(new java.util.Date().getTime),
                id: Option[Long] = None)

trait UserComponent { this: UserProjectsComponent =>

  class Users(tag: Tag) extends Table[User](tag, "USERS") with IdAutoIncrement[User] {
    def username = column[String]("USR_USERNAME", O.NotNull)
    def email = column[String]("USR_EMAIL", O.NotNull)
    def firstName = column[Option[String]]("USR_FIRSTNAME")
    def lastName = column[Option[String]]("USR_LASTNAME")
    def homepage = column[Option[String]]("USR_HOMEPAGE")
    def joined = column[Date]("USR_JOINED", O.NotNull)
    def project = userProjects.filter(_.userId === id).flatMap(_.projectFK)

    def * = (username, email, firstName, lastName, homepage, joined, id.?) <> (User.tupled, User.unapply _)
  }
  val users = TableQuery[Users]

  def usersForInsert = users.map(u => (u.username, u.email, u.firstName, u.lastName, u.homepage, u.joined).shaped <>
    ({ t => User(t._1, t._2, t._3, t._4, t._5, t._6, None)}, { (u: User) =>
      Some((u.username, u.email, u.firstName, u.lastName, u.homepage, u.joined))}))

  def insertUser(user: User)(implicit session: Session) =
    user.copy(id = Some((usersForInsert returning users.map(_.id)) += user))
}

object UserDAO {
  def recentlyUpdatedProjectsWithAuthors(limit: Int): Seq[ProjectWithAuthors] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.take(limit).list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    projectsAndAuthors.map((pa: (dao.SimpleProject, Seq[dao.User])) =>
      ProjectWithAuthors(pa._1.name, pa._1.url, pa._1.created, pa._1.updated, pa._1.id, pa._2)
    )
  }
  def userForUsername(username: String): Option[User] = {
    val query = for { u <- Global.dal.users if u.username === username } yield u
    Global.db.withSession{ implicit session: dao.profile.backend.Session =>
      query.firstOption
    }
  }
  def projectsForUser(user: User): Seq[ProjectWithAuthors] = {
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
      projectsAndAuthors.map((pa: (dao.SimpleProject, Seq[dao.User])) =>
        //Remove the current user from authors
        ProjectWithAuthors(pa._1.name, pa._1.url, pa._1.created, pa._1.updated, pa._1.id, pa._2.diff(Seq(user)))
      )
    }.getOrElse(Seq.empty)
  }
}
