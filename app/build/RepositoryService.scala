package build

import java.io.{FileNotFoundException, File}

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.api.Logger

import dao.{ProjectHelper, ProjectBranch, ProjectVersion, Project}
import settings.Global

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository

trait RepositoryService {
  def checkoutOrUpdate(project: Project, branch: ProjectBranch, version: ProjectVersion): Future[File]
  def cleanVersion(project: Project, version: ProjectVersion): Unit
}

trait GitRepositoryService extends RepositoryService {
  self: DirectoryHandler =>

  def checkoutOrUpdate(project: Project, branch: ProjectBranch, version: ProjectVersion): Future[File] = Future {
    val checkoutDir = repositoryForProjectVersion(project, version)
    if(checkoutDir.exists()) {
      updateRepo(checkoutDir)
    } else {
      cloneProjectTo(project, branch, version, checkoutDir)
    }
    checkoutDir
  }

  def cleanVersion(project: Project, version: ProjectVersion): Unit = {
    Logger.info("Clean Version")
  }

  private def updateRepo(repoDir: File): Git = {
    Logger.info("Git repository found at ["+repoDir.getAbsoluteFile+"]. Updating to latest.")
    val repo = new Git(new FileRepository(repoDir))
    repo.pull().call()
    repo
  }

  private def cloneProjectTo(project: Project, branch: ProjectBranch, version: ProjectVersion, repoDir: File): Git = {
    Logger.info("Cloning git repository ["+project.url+"] branch=["+branch.branchName+"] version=["+version.versionName+"]. To ["+repoDir.getAbsoluteFile+"].")
    val repo = Git.cloneRepository().setBranch(branch.branchName).setURI(project.url).setDirectory(repoDir).call()
    if(!ProjectHelper.defaultProjectVersion.equals(version)) {
      repo.checkout().setName(version.versionName).call()
    }
    repo
  }

}
