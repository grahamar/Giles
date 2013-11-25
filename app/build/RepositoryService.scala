package build

import java.io.{FileInputStream, File}

import collection.JavaConverters._
import play.api.Logger

import dao.{ProjectHelper, ProjectVersion, Project}

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import scala.util.Try
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.util.FS
import com.jcraft.jsch._
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.{RemoteIdentityRepository, AgentProxyException}
import util.ResourceUtil

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
    val repoDir = repositoryForProject(project)
    getVersionsForRepo(project, repoDir)
  }

  private def getVersionsForRepo(project: Project, repoDir: File) = {
    SshSessionFactory.setInstance(new CustomConfigSessionFactory)
    Logger.info("Retrieve versions for ["+project.name+"]")
    val refsIndex = "refs/tags/".length
    val repo = new Git(new FileRepository(repoDir))
    Seq(ProjectHelper.defaultProjectVersion) ++ repo.tagList().call().asScala.map { ref =>
      ProjectVersion(ref.getName.substring(refsIndex))
    }.toSeq
  }

  private def checkoutRepo(project: Project, version: ProjectVersion, repoDir: File): Git = {
    SshSessionFactory.setInstance(new CustomConfigSessionFactory)
    val repo = new Git(new FileRepository(repoDir))
    if(ProjectHelper.defaultProjectVersion.equals(version)) {
      repo.checkout().setName(project.defaultBranch.branchName).call()
    } else {
      repo.checkout().setName(version.versionName).call()
    }
    repo
  }

  def cloneProjectTo(project: Project, repoDir: File): Git = {
    SshSessionFactory.setInstance(new CustomConfigSessionFactory)
    Logger.info("Cloning git repository ["+project.url+"]. To ["+repoDir.getAbsoluteFile+"].")
    Git.cloneRepository().setURI(project.url).setDirectory(repoDir).call()
  }

  private class CustomConfigSessionFactory extends JschConfigSessionFactory {

    def configure(p1: Host, session: Session) = {
      session.setConfig("StrictHostKeyChecking", "true")
      session.setConfig("PreferredAuthentications", "publickey")
    }
    override def createDefaultJSch(fs : FS) : JSch = {
      try {
        val jsch = new JSch()
        if(SSHAgentConnector.isConnectorAvailable){
          val con =new SSHAgentConnector(new JNAUSocketFactory())
          jsch.setIdentityRepository(new RemoteIdentityRepository(con))
          knownHosts(jsch, fs)
          identities(jsch, fs)
          jsch
        } else {
          super.createDefaultJSch(fs)
        }
      } catch {
        case e: AgentProxyException => Logger.error("", e); throw e
      }
    }
    private def knownHosts(sch: JSch, fs : FS): Unit = Try {
      val home = fs.userHome()
      if (home != null) {
        val known_hosts = new File(new File(home, ".ssh"), "known_hosts")
        ResourceUtil.doWith(new FileInputStream(known_hosts)) { in =>
          sch.setKnownHosts(in)
        }
      }
    }

    private def identities(sch: JSch, fs : FS): Unit = {
      val home = fs.userHome()
      if (home != null) {
        val sshdir = new File(home, ".ssh")
        if (sshdir.isDirectory) {
          loadIdentity(sch, new File(sshdir, "identity"))
          loadIdentity(sch, new File(sshdir, "id_rsa"))
          loadIdentity(sch, new File(sshdir, "id_dsa"))
        }
      }
    }

    private def loadIdentity(sch: JSch, priv: File): Unit = Try {
      if (priv.isFile) {
        sch.addIdentity(priv.getAbsolutePath)
      }
    }
  }

}
