package build

import java.io.{File => JFile}

import collection.JavaConverters._
import play.api.Logger

import models._

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import scala.util.Try
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.transport._
import settings.Global

trait RepositoryService {
  def clone(project: Project): Try[JFile]
  def checkout(project: Project, version: String): Try[JFile]
  def clean(project: Project): Try[Unit]
  def getVersions(project: Project): Try[Seq[String]]
}

trait GitRepositoryService extends RepositoryService {
  self: DirectoryHandler =>

  SshSessionFactory.setInstance(new CustomConfigSessionFactory)

  def clone(project: Project): Try[JFile] = Try {
    val checkoutDir = repositoryForProject(project)
    if(checkoutDir.exists()) {
      clean(project)
    }
    cloneProjectTo(project, checkoutDir)
    checkoutDir
  }.recover {
    case e: Exception =>
      Global.builds.createFailure(project.guid, project.head_version, "Clone failed - "+ e.getMessage)
      throw e
  }

  def checkout(project: Project, version: String): Try[JFile] = Try {
    val repoDir = new JFile(repositoryForProject(project), ".git")
    Logger.info("Checking out repository for version="+version+" ["+repoDir.getAbsolutePath+"]")
    if(repoDir.exists()) {
      checkoutRepo(project, version, repoDir)
    }
    repoDir
  }.recover {
    case e: Exception =>
      Global.builds.createFailure(project.guid, version, "Checkout failed - "+ e.getMessage)
      throw e
  }

  def clean(project: Project): Try[Unit] = Try {
    val repoDir = repositoryForProject(project)
    if(repoDir.exists()) {
      Logger.info("Cleaning repository ["+repoDir.getAbsolutePath+"]")
      FileUtils.deleteDirectory(repoDir)
      if(repoDir.exists()) {
        throw new BuildFailedException("Cannot clean directory ["+repoDir.getAbsolutePath+"]")
      }
    }
  }.recover {
    case e: Exception =>
      Global.builds.createFailure(project.guid, project.head_version, "Clean failed - "+ e.getMessage)
      throw e
  }

  def getVersions(project: Project): Try[Seq[String]] = Try {
    val repoDir = repositoryForProject(project)
    getVersionsForRepo(project, repoDir)
  }.recover {
    case e: Exception =>
      Global.builds.createFailure(project.guid, project.head_version, "Getting versions failed - "+ e.getMessage)
      throw e
  }

  private def getVersionsForRepo(project: Project, repoDir: JFile) = {
    Logger.info("Retrieve versions for ["+project.name+"]")
    val refsIndex = "refs/tags/".length
    val repo = new Git(new FileRepository(repoDir))
    Seq(project.head_version) ++ repo.tagList().call().asScala.map { ref =>
      ref.getName.substring(refsIndex)
    }.toSeq
  }

  private def checkoutRepo(project: Project, version: String, repoDir: JFile): Git = {
    val repo = new Git(new FileRepository(repoDir))
    repo.checkout().setName(version).call()
    repo
  }

  private def cloneProjectTo(project: Project, repoDir: JFile): Git = {
    Logger.info("Cloning git repository ["+project.repo_url+"]. To ["+repoDir.getAbsoluteFile+"].")
    Git.cloneRepository().setURI(project.repo_url).setDirectory(repoDir).call()
  }

}
