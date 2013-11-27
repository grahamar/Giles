package controllers

import play.api.mvc._
import play.api.templates.Html

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import settings.Global

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def projectIndex(projectUrlKey: String, projectVersion: String) = Action {
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      Ok(html.docs_main(project.name, files.head.title, project, projectVersion, Html(files.head.html), files))
    }.getOrElse(NotFound)
  }

  def projectDocs(projectUrlKey: String, projectVersion: String, fileTitle: String) = Action {
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      Global.files.findForProjectGuidAndVersion(project.guid, projectVersion, fileTitle).map { file =>
        Ok(html.docs_main(project.name, file.title, project, projectVersion, Html(file.html), files))
      }.getOrElse(NotFound)
    }.getOrElse(NotFound)
  }

}
