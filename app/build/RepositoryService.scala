package build

import java.io.File

import collection.JavaConverters._
import play.api.Logger

import dao.{ProjectHelper, ProjectVersion, Project}

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import scala.util.Try
import org.apache.commons.io.FileUtils

trait RepositoryService {
  def clone(project: Project): Try[File]
  def checkout(project: Project, version: ProjectVersion): Try[File]
  def clean(project: Project): Try[Unit]
  def getVersions(project: Project): Try[Seq[ProjectVersion]]
}

trait GitRepositoryService extends RepositoryService {
  self: DirectoryHandler =>

  def clone(project: Project): Try[File] = Try {
    val checkoutDir = repositoryForProject(project)
    if(checkoutDir.exists()) {
      clean(project)
    }
    cloneProjectTo(project, checkoutDir)
    checkoutDir
  }

  def checkout(project: Project, version: ProjectVersion): Try[File] = Try {
    val repoDir = new File(repositoryForProject(project), ".git")
    Logger.info("Checking out repository for version="+version.versionName+" ["+repoDir.getAbsolutePath+"]")
    if(repoDir.exists()) {
      checkoutRepo(project, version, repoDir)
    }
    repoDir
  }

  def clean(project: Project): Try[Unit] = Try {
    val repoDir = repositoryForProject(project)
    Logger.info("Cleaning repository ["+repoDir.getAbsolutePath+"]")
    FileUtils.deleteDirectory(repoDir)
    if(repoDir.exists()) {
      throw new BuildFailedException("Cannot clean directory ["+repoDir.getAbsolutePath+"]")
    }
  }

  def getVersions(project: Project): Try[Seq[ProjectVersion]] = Try {
    val refsIndex = "refs/tags/".length
    val versions = Seq(ProjectHelper.defaultProjectVersion) ++
      Git.lsRemoteRepository().setRemote(project.url).setTags(true).call().asScala.map { ref =>
        ProjectVersion(ref.getName.substring(refsIndex))
      }.toSeq
    versions
  }

  private def checkoutRepo(project: Project, version: ProjectVersion, repoDir: File): Git = {
    val repo = new Git(new FileRepository(repoDir))
    if(ProjectHelper.defaultProjectVersion.equals(version)) {
      repo.checkout().setName(project.defaultBranch.branchName).call()
    } else {
      repo.checkout().setName(version.versionName).call()
    }
    repo
  }

  private def cloneProjectTo(project: Project, repoDir: File): Git = {
    Logger.info("Cloning git repository ["+project.url+"]. To ["+repoDir.getAbsoluteFile+"].")
    Git.cloneRepository().setURI(project.url).setDirectory(repoDir).call()
  }

}
