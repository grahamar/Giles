package util

import scala.util.{Failure, Success, Try}

import org.xbill.DNS._
import java.util.Properties
import javax.mail.{Transport, Session}
import javax.mail.internet.{InternetAddress, MimeMessage}

import models._
import views.html.NewVersionEmail

/**
 * This is a complete hack in order to not require an intermediary SMTP server...
 */
object EmailUtil {
  private lazy val FromAddress = "Giles <giles@gilt.com>"
  private def hostNameForEmail(emailAddress: String): String = emailAddress.split("@")(1)
  private def mxRecordsForEmail(emailAddress: String): Seq[MXRecord] = {
    val records = Option(new Lookup(hostNameForEmail(emailAddress), Type.MX).run())
    records.map(_.map(_.asInstanceOf[MXRecord]).toSeq).getOrElse(Seq.empty)
  }
  def sendEmailTo(emailAddress: String, subject: String, body: String): Unit = {
    val messages = mxRecordsForEmail(emailAddress).map { record =>
      val props = new Properties()
      props.put("mail.smtp.host", record.getTarget.toString)
      val session = Session.getDefaultInstance(props)

      val msg = new MimeMessage(session)
      msg.setFrom(new InternetAddress(FromAddress))
      msg.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(emailAddress))
      msg.setSubject(subject)
      msg.setContent(body, "text/html; charset=utf-8")
      msg
    }
    // Try sending with each MX Record we've found until success
    messages.takeWhile { msg =>
      val sent = Try(Transport.send(msg))
      sent match {
        case Success(_) =>
          false
        case Failure(e) =>
          true
      }
    }
  }
  def sendNewVersionEmail(user: User, project: Project, version: String): Unit = {
    sendEmailTo(user.email,
      "Documentation for version %s of the project %s has been added!".format(version, project.name),
      NewVersionEmail(project, version).body)
  }
}
