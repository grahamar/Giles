package dao

import java.sql.Date

import scala.slick.lifted.Tag
import profile.simple._

case class User(username: String,
                firstName: Option[String] = None,
                lastName: Option[String] = None,
                joined: Date = new Date(new java.util.Date().getTime),
                id: Option[Long] = None)

trait UserComponent { this: UserProjectsComponent =>

  class Users(tag: Tag) extends Table[User](tag, "USERS") with IdAutoIncrement[User] {
    def username = column[String]("USR_USERNAME", O.NotNull)
    def firstName = column[Option[String]]("USR_FIRSTNAME")
    def lastName = column[Option[String]]("USR_LASTNAME")
    def joined = column[Date]("USR_JOINED", O.NotNull)
    def project = userProjects.filter(_.userId === id).flatMap(_.projectFK)

    def * = (username, firstName, lastName, joined, id.?) <> (User.tupled, User.unapply _)
  }
  val users = TableQuery[Users]

  def usersForInsert = users.map(u => (u.username, u.firstName, u.lastName, u.joined).shaped <>
    ({ t => User(t._1, t._2, t._3, t._4, None)}, { (u: User) =>
      Some((u.username, u.firstName, u.lastName, u.joined))}))

  def insertUser(user: User)(implicit session: Session) =
    user.copy(id = Some((usersForInsert returning users.map(_.id)) += user))
}
