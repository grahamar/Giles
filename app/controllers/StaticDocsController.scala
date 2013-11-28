package controllers

import play.api.mvc._
import play.api.templates.Html

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import settings.Global
import java.util.UUID

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  def projectIndex(projectUrlKey: String, projectVersion: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      if(!files.isEmpty) {
        Global.views.create(UUID.randomUUID(), files.head.guid, loggedIn.map(_.guid))
        Ok(html.docs_main(files.head, project, projectVersion, Html(files.head.html), files))
      } else NotFound
    }.getOrElse(NotFound)
  }

  def projectDocs(projectUrlKey: String, projectVersion: String, fileUrlKey: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      Global.files.findForProjectGuidAndVersion(project.guid, projectVersion, fileUrlKey).map { file =>
        Global.views.create(UUID.randomUUID(), file.guid, loggedIn.map(_.guid))
        Ok(html.docs_main(file, project, projectVersion, Html(file.html), files))
      }.getOrElse(NotFound)
    }.getOrElse(NotFound)
  }

}
