package controllers

import java.util.UUID

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.Logger
import play.api.libs.json.Json

import views._
import models._
import settings.Global
import dao.util.FileHelper
import dao.util.PublicationConverters._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import build.DocumentationFactory

object PublicationController extends Controller with OptionalAuthUser with AuthConfigImpl {

  implicit val dataFormat = Json.format[XEditableData]

  case class XEditableData(name: String, value: String, pk: String)

  def postPublication = StackAction { implicit request =>
    createPublicationForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      publication => loggedIn.map(createPublication(publication, _)).getOrElse(BadRequest)
    )
  }

  def create = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(html.create_publication(AuthenticationController.loginForm, createPublicationForm))
    }.getOrElse(ApplicationController.Home)
  }

  def createPublication(data: PublicationData, currentUser: User)(implicit request: RequestHeader): SimpleResult = {
    val publication = FileHelper.getOrCreateContent(data.content) { contentGuid =>
      val pub = Global.publications.create(UUID.randomUUID(), currentUser, data.title, contentGuid)
      DocumentationFactory.indexService.cleanPublicationIndex(pub).map { _ =>
        DocumentationFactory.indexService.index(pub.withContent)
      }
      pub
    }
    Redirect(routes.PublicationController.publication(publication.url_key))
  }

  def publication(pubUrlKey: String) = StackAction { implicit request =>
    Global.publications.findByUrlKey(UrlKey.generate(pubUrlKey)).map { pub =>
      Ok(html.publication(pub.withContent, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def publicationsForUser(username: String) = StackAction { implicit request =>
    Global.users.findByUsername(username).map { user =>
      val userPublications = Global.publications.findAllByUser(user).toSeq
      Ok(html.publications(userPublications, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def editPublication = StackAction(parse.json) { implicit request =>
    val data = request.body.as[XEditableData]
    Global.publications.findByUrlKey(data.pk).map { publication =>
      loggedIn.map { currentUser =>
        if(currentUser.guid == publication.user_guid) {
          data.name match {
            case "publication-title" =>
              Global.publications.update(publication.copy(title = data.value))
            case "publication-content" =>
              FileHelper.getOrCreateContent(data.value) { contentGuid =>
                Global.publications.update(publication.copy(content_guid = contentGuid))
              }
              FileHelper.cleanupContent(publication.content_guid)
          }
        }
      }
    }
    Ok
  }

  val createPublicationForm = Form {
    mapping(
      "title" -> nonEmptyText,
      "content" -> text
    )(PublicationData.apply)(PublicationData.unapply)
  }

}
