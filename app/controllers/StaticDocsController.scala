package controllers

import play.api.mvc._
import play.api.templates.Html

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import settings.Global
import java.util.UUID
import scala.collection.immutable.TreeMap
import play.api.Logger
import util.ResourceUtil

object StaticDocsController extends Controller with OptionalAuthUser with AuthConfigImpl {

  import dao.util.FileConverters._

  def projectIndex(projectUrlKey: String, projectVersion: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq.sorted
      if(!files.isEmpty) {
        Redirect(routes.StaticDocsController.projectDocs(projectUrlKey, projectVersion, files.head.url_key))
      } else NotFound(html.notfound(AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def projectDocs(projectUrlKey: String, projectVersion: String, fileUrl: String) = StackAction { implicit request =>
    val fileUrlKey = ResourceUtil.decodeFileName(fileUrl)
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      val files = Global.files.findAllByProjectGuidAndVersion(project.guid, projectVersion).toSeq
      val filesByDirectory = TreeMap(files.groupBy(_.relative_path).toSeq.sortBy(_._1):_*)
      Global.files.findForProjectGuidAndVersion(project.guid, projectVersion, fileUrlKey).map { file =>
        Global.views.create(UUID.randomUUID().toString, file.guid, loggedIn.map(_.guid))
        Ok(html.docs_main(file, project, projectVersion, Html(file.withContent.content), filesByDirectory))
      }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

  def pdf(projectUrlKey: String, projectVersion: String) = TODO

  def swagger(projectUrl: String, projectVersion: String) = StackAction { implicit request =>
    val projectUrlKey = ResourceUtil.decodeFileName(projectUrl)
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      Ok(html.swagger(controllers.api.routes.SwaggerApiController.getResourceListing(project.guid, projectVersion).url, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

}
