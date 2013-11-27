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
class ProjectDaoTest extends PlaySpecification with Mockito with BeforeAfter {

  lazy val projectCollection = TestMongoInit.mongoDb("projects")
  lazy val projects = new dao.ProjectDao(projectCollection)

  def before = {
    projectCollection.drop()
  }

  def after = {}

  "ProjectDao" should {

    "successfully create a project in mongo" in new WithApplication {
      val guid = UUID.randomUUID()
      val userGuid = UUID.randomUUID()
      val name = Gen.alphaStr.sample.get
      val description = Gen.alphaStr.sample.get
      val repoUrl = Gen.alphaStr.sample.get
      val project = projects.create(userGuid, guid, name, description, repoUrl)

      project.guid must_== guid

      val maybeProject = projects.findByGuid(guid)

      maybeProject must not be None
      maybeProject.get mustEqual project
    }

  }

}
