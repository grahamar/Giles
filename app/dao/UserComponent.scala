package dao

import java.sql.Timestamp

import scala.slick.lifted.Tag
import profile.simple._
import settings.Global

import org.mindrot.jbcrypt.BCrypt
import play.api.Logger

case class User(username: String,
                email: String,
                password: String,
                firstName: Option[String] = None,
                lastName: Option[String] = None,
                homepage: Option[String] = None,
                joined: Timestamp = new Timestamp(new java.util.Date().getTime),
                salt: Option[String] = None,
                id: Option[Long] = None)

trait UserComponent { this: UserProjectsComponent =>

  class Users(tag: Tag) extends Table[User](tag, "USERS") with IdAutoIncrement[User] {
    def username = column[String]("USR_USERNAME", O.NotNull)
    def email = column[String]("USR_EMAIL", O.NotNull)
    def password = column[String]("USR_PWD", O.NotNull)
    def firstName = column[Option[String]]("USR_FIRSTNAME")
    def lastName = column[Option[String]]("USR_LASTNAME")
    def homepage = column[Option[String]]("USR_HOMEPAGE")
    def joined = column[Timestamp]("USR_JOINED", O.NotNull)
    def salt = column[Option[String]]("USR_SALT", O.NotNull)
    def project = userProjects.filter(_.userId === id).flatMap(_.projectFK)

    def * = (username, email, password, firstName, lastName, homepage, joined, salt, id.?) <> (User.tupled, User.unapply _)
  }
  val users = TableQuery[Users]

  def usersForInsert = users.map(u => (u.username, u.email, u.password, u.firstName, u.lastName, u.homepage, u.joined, u.salt).shaped <>
    ({ t => User(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, None)}, { (u: User) =>
      Some((u.username, u.email, u.password, u.firstName, u.lastName, u.homepage, u.joined, u.salt))}))

  def insertUser(user: User)(implicit session: Session) = {
    val salt: String = BCrypt.gensalt()
    val hashedPwd: String = BCrypt.hashpw(user.password, salt)
    val saltedUser = user.copy(salt = Some(salt), password = hashedPwd)
    saltedUser.copy(id = Some((usersForInsert returning users.map(_.id)) += saltedUser))
  }
}

object UserDAO {

  def projectsWithAuthors: Seq[ProjectWithAuthors] = {
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    mapProjectWithAuthors(projectsAndAuthors)
  }

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
    mapProjectWithAuthors(projectsAndAuthors).sorted
  }

  def findById(id: Long): Option[User] = {
    val query = for { u <- Global.dal.users if u.id === id } yield u
    Global.db.withSession{ implicit session: dao.profile.backend.Session =>
      query.firstOption
    }
  }

  def findByEmail(email: String): Option[User] = {
    val query = for { u <- Global.dal.users if u.email === email } yield u
    Global.db.withSession{ implicit session: dao.profile.backend.Session =>
      query.firstOption
    }
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
      // Remove the current user from authors
      mapProjectWithAuthors(projectsAndAuthors).map(p => p.copy(authors = p.authors.diff(Seq(user))))
    }.getOrElse(Seq.empty)
  }

  def authenticate(email: String, password: String): Option[User] = {
    findByEmail(email).filter{user => user.salt.exists(salt => BCrypt.checkpw(password, user.password))}
  }

  def createUser(user: auth.UserData): User = {
    Global.db.withSession{ implicit session: dao.profile.backend.Session =>
      Global.dal.insertUser(
        User(user.username, user.email, user.password, Option(user.firstName), Option(user.lastName), Option(user.homepage))
      )
    }
  }

  def mapProjectWithAuthors(projectsAndAuthors: Seq[(dao.SimpleProject, Seq[dao.User])]): Seq[ProjectWithAuthors] = {
    projectsAndAuthors.map((pa: (dao.SimpleProject, Seq[dao.User])) =>
      mapProjectWithAuthors(Some(pa)).get
    )
  }

  def mapProjectWithAuthors(projectWithAuthors: Option[(dao.SimpleProject, Seq[dao.User])]): Option[ProjectWithAuthors] = {
    projectWithAuthors.map { pa =>
      ProjectWithAuthors(pa._1.name, pa._1.slug, pa._1.url, pa._1.tags, pa._1.defaultBranch, pa._1.defaultVersion, pa._1.created, pa._1.updated, pa._1.id, pa._2)
    }
  }
}
