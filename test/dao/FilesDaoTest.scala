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
class FilesDaoTest extends PlaySpecification with Mockito with BeforeAfter {

  lazy val projectCollection = TestMongoInit.mongoDb("projects")
  lazy val projects = new dao.ProjectDao(projectCollection)
  lazy val filesCollection = TestMongoInit.mongoDb("files")
  lazy val files = new dao.FilesDao(filesCollection)

  lazy val project = {
    val name = Gen.alphaStr.sample.get
    val description = Gen.alphaStr.sample.get
    val repoUrl = Gen.alphaStr.sample.get
    projects.create(Gen.alphaStr.sample.get, UUID.randomUUID(), name, description, repoUrl)
  }

  def before = {
    filesCollection.drop()
    projectCollection.drop()
  }

  def after = {}

  "FilesDao" should {

    "successfully create files in mongo" in new WithApplication {
      val guid = UUID.randomUUID()
      val title = Gen.alphaStr.sample.get
      val html = Gen.alphaStr.sample.get

      val file = files.create(guid, project, "test-version", "", "", title, UUID.randomUUID())

      file.guid must_== guid

      val maybeFile = files.findByGuid(guid)

      maybeFile must not be None
      maybeFile.get mustEqual file
    }

    "successfully find all files by project & version" in new WithApplication {
      val guid = UUID.randomUUID()
      val title = Gen.alphaStr.sample.get
      val html = Gen.alphaStr.sample.get

      val file = files.create(guid, project, "special-test-version", "", "", title, UUID.randomUUID())

      file.guid must_== guid

      val filesByPV = files.findAllByProjectGuidAndVersion(project.guid, "special-test-version").toSeq

      filesByPV.size must_== 1
      filesByPV(0) mustEqual file
    }

  }

}
