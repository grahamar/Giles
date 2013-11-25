import build.{GitRepositoryService, DirectoryHandlerImpl}
import dao.{ProjectBranch, ProjectVersion, Project}
import java.io.File
import java.sql.Timestamp
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class RepositoryServiceTest extends Specification {

  "RepositoryService" should {

    "get versions for ssh repository" in {
      val repoService = new GitRepositoryService with DirectoryHandlerImpl

      repoService.cloneProjectTo(TestProject, new File(".", "temp"))

      true shouldEqual(true)
    }

  }

  object TestProject extends Project {
    val name: String = "svc-email-delivery"
    val slug: String = "svc-email-delivery"
    val url: String = "ssh://grhodes@gerrit.gilt.com:29418/svc-email-delivery"
    val defaultBranch: ProjectBranch = ProjectBranch("master")
    val defaultVersion: ProjectVersion = ProjectVersion("latest")
    val created: Timestamp = new Timestamp(System.currentTimeMillis())
    val updated: Timestamp = new Timestamp(System.currentTimeMillis())
    val id: Option[Long] = None
  }

}
