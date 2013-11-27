package controllers

import play.api.mvc._
import play.api.templates.Html

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import settings.Global
import play.api.Logger

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def projectIndex(projectUrlKey: String, projectVersion: String) = Action {
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      files.foreach { file =>
        Logger.info("Found: "+file.title)
      }
      Ok(html.docsMain(files.head.title, Html(files.head.html), files))
    }.getOrElse(NotFound)
  }

  def projectDocs(projectSlug: String, projectVersion: String, fileTitle: String) = TODO
}
