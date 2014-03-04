package util

import scala.concurrent._
import ExecutionContext.Implicits.global
import org.apache.commons.mail.{DefaultAuthenticator, HtmlEmail}
import org.slf4j.LoggerFactory

import app.Context
import model.Profile
import service.{AccountService, RepositoryService, IssuesService, SystemSettingsService}
import servlet.Database
import SystemSettingsService.Smtp
import _root_.util.ControlUtil.defining
import scala.slick.driver.ExtendedProfile

trait Notifier extends RepositoryService with AccountService with IssuesService with Profile {
  def toNotify(r: RepositoryService.RepositoryInfo, issueId: Int, content: String)
      (msg: String => String)(implicit context: Context): Unit

  protected def recipients(issue: model.Issue)(notify: String => Unit)(implicit context: Context) =
    (
        // individual repository's owner
        issue.userName ::
        // collaborators
        getCollaborators(issue.userName, issue.repositoryName) :::
        // participants
        issue.openedUserName ::
        getComments(issue.userName, issue.repositoryName, issue.issueId).map(_.commentedUserName)
    )
    .distinct
    .withFilter ( _ != context.loginAccount.get.userName )	// the operation in person is excluded
    .foreach ( getAccountByUserName(_) filterNot (_.isGroupAccount) foreach (x => notify(x.mailAddress)) )

}

object Notifier {
  // TODO We want to be able to switch to mock.
  def apply(profile: ExtendedProfile): Notifier = new SystemSettingsService {}.loadSystemSettings match {
    case settings if settings.notification => new Mailer(profile, settings.smtp.get)
    case _ => new MockMailer(profile)
  }

  def msgIssue(url: String) = (content: String) => s"""
    |${content}<br/>
    |--<br/>
    |<a href="${url}">View it on GitBucket</a>
    """.stripMargin

  def msgPullRequest(url: String) = (content: String) => s"""
    |${content}<hr/>
    |View, comment on, or merge it at:<br/>
    |<a href="${url}">${url}</a>
    """.stripMargin

  def msgComment(url: String) = (content: String) => s"""
    |${content}<br/>
    |--<br/>
    |<a href="${url}">View it on GitBucket</a>
    """.stripMargin

  def msgStatus(url: String) = (content: String) => s"""
    |${content} <a href="${url}">#${url split('/') last}</a>
    """.stripMargin
}

class Mailer(override val profile: ExtendedProfile, private val smtp: Smtp) extends Notifier {
  private val logger = LoggerFactory.getLogger(classOf[Mailer])

  def toNotify(r: RepositoryService.RepositoryInfo, issueId: Int, content: String)
      (msg: String => String)(implicit context: Context) = {
    val database = Database(context.request.getServletContext)

    val f = future {
      // TODO Can we use the Database Session in other than Transaction Filter?
      database withSession {
        getIssue(r.owner, r.name, issueId.toString) foreach { issue =>
          defining(
              s"[${r.name}] ${issue.title} (#${issueId})" ->
              msg(view.Markdown.toHtml(content, r, false, true))) { case (subject, msg) =>
            recipients(issue) { to =>
              val email = new HtmlEmail
              email.setHostName(smtp.host)
              email.setSmtpPort(smtp.port.get)
              smtp.user.foreach { user =>
                email.setAuthenticator(new DefaultAuthenticator(user, smtp.password.getOrElse("")))
              }
              smtp.ssl.foreach { ssl =>
                email.setSSLOnConnect(ssl)
              }
              smtp.fromAddress
                .map (_ -> smtp.fromName.orNull)
                .orElse (Some("notifications@gitbucket.com" -> context.loginAccount.get.userName))
                .foreach { case (address, name) =>
                  email.setFrom(address, name)
                }
              email.setCharset("UTF-8")
              email.setSubject(subject)
              email.setHtmlMsg(msg)

              email.addTo(to).send
            }
          }
        }
      }
      "Notifications Successful."
    }
    f onSuccess {
      case s => logger.debug(s)
    }
    f onFailure {
      case t => logger.error("Notifications Failed.", t)
    }
  }
}
class MockMailer(override val profile: ExtendedProfile) extends Notifier {
  def toNotify(r: RepositoryService.RepositoryInfo, issueId: Int, content: String)
      (msg: String => String)(implicit context: Context): Unit = {}
}