package build

import java.io.FileInputStream
import java.io.{File => JFile}

import scala.util.Try
import play.api.Logger

import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.util.FS
import com.jcraft.jsch._
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.{RemoteIdentityRepository, AgentProxyException}
import util.ResourceUtil

private[build] class CustomConfigSessionFactory extends JschConfigSessionFactory {

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
      val known_hosts = new JFile(new JFile(home, ".ssh"), "known_hosts")
      ResourceUtil.doWith(new FileInputStream(known_hosts)) { in =>
        sch.setKnownHosts(in)
      }
    }
  }

  private def identities(sch: JSch, fs : FS): Unit = {
    val home = fs.userHome()
    if (home != null) {
      val sshdir = new JFile(home, ".ssh")
      if (sshdir.isDirectory) {
        loadIdentity(sch, new JFile(sshdir, "identity"))
        loadIdentity(sch, new JFile(sshdir, "id_rsa"))
        loadIdentity(sch, new JFile(sshdir, "id_dsa"))
      }
    }
  }

  private def loadIdentity(sch: JSch, priv: JFile): Unit = Try {
    if (priv.isFile) {
      sch.addIdentity(priv.getAbsolutePath)
    }
  }

}
