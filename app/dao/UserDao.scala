package dao

import models._

import com.novus.salat._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

class UserDao(users: MongoCollection) {

  def create(guid: UUID, username: String, email: String, password: String, firstName: Option[String] = None, lastName: Option[String] = None): User = {
    val user = User(guid = guid,
      username = username,
      email = email,
      password = password,
      project_guids = Seq.empty,
      first_name = firstName,
      last_name = lastName,
      created_at = new DateTime())
    create(user)
  }

  def create(user: User): User = {
    val salt: String = BCrypt.gensalt()
    val usr = user.copy(password = BCrypt.hashpw(user.password, salt), salt = Option(salt))
    users.insert(grater[User].asDBObject(usr))
    usr
  }

  def update(user: User) {
    val obj = MongoDBObject("username" -> user.username, "email" -> user.email, "password" -> user.password,
                "first_name" -> user.first_name, "last_name" -> user.last_name, "homepage" -> user.homepage)
    users.update(q = MongoDBObject("guid" -> user.guid),
      o = MongoDBObject("$set" -> obj),
      upsert = false,
      multi = false)
  }

  def findByGuid(guid: UUID): Option[User] = {
    search(UserQuery(guid = Some(guid))).headOption
  }

  def findByUsername(username: String): Option[User] = {
    search(UserQuery(username = Some(username))).headOption
  }

  def findByEmail(email: String): Option[User] = {
    search(UserQuery(email = Some(email))).headOption
  }

  def authenticate(email: String, password: String): Option[User] = {
    findByEmail(email).filter{user => user.salt.exists(salt => BCrypt.checkpw(password, user.password))}
  }

  def delete(guid: UUID) = {
    // TODO: Soft delete?
    users.remove(MongoDBObject("guid" -> guid))
  }

  def search(query: UserQuery): Iterable[User] = {
    val builder = MongoDBObject.newBuilder
    query.guid.foreach { v => builder += "guid" -> v }
    query.username.foreach { v => builder += "username" -> v }
    query.email.foreach { v => builder += "email" -> v }

    val sortBuilder = MongoDBObject.newBuilder
    sortBuilder += "username" -> 1

    users.find(builder.result()).
      skip(query.pagination.offsetOrDefault).
      sort(sortBuilder.result()).
      limit(query.pagination.limitOrDefault).
      toList.map(grater[User].asObject(_))
  }

}
