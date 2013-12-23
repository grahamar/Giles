package dao

import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.mock._
import org.scalacheck.Gen
import org.junit.runner._
import play.api.test.{WithApplication, PlaySpecification}

import dao.util.TestMongoInit
import java.util.UUID

@RunWith(classOf[JUnitRunner])
class UserDaoTest extends PlaySpecification with Mockito with BeforeAfter {

  lazy val usersCollection = TestMongoInit.mongoDb("users")
  lazy val users = new dao.UserDao(usersCollection)

  def before = usersCollection.drop()

  def after = {}

  "UserDao" should {

    "successfully create a user in mongo" in new WithApplication {
      val guid = UUID.randomUUID().toString
      val username = Gen.alphaStr.sample.get
      val email = Gen.alphaStr.sample.get
      val user = users.create(guid, username, email, Gen.alphaStr.sample.get)

      user.guid must_== guid
      user.username must_== username
      user.email must_== email
      user.salt must not be None

      val maybeUser = users.findByGuid(guid)

      maybeUser must not be None
      maybeUser.get mustEqual user
    }

  }

}
