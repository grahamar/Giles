package controllers

import java.util.UUID
import java.io.{IOException, ByteArrayOutputStream}
import javax.imageio.IIOException

import scala.collection.immutable.TreeMap

import play.api.mvc._
import play.api.Logger
import play.api.cache.Cached
import play.api.Play.current
import play.api.templates.Html
import play.api.libs.iteratee.Enumerator

import views._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import settings.Global
import util.ResourceUtil
import net.sourceforge.plantuml.{FileFormat, FileFormatOption, SourceStringReader}
import build.DocumentationFactory

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

  def rebuildVersion(projectUrlKey: String, projectVersion: String) = StackAction { implicit request =>
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      DocumentationFactory.documentsBuilder.cleanAndBuildVersion(project, projectVersion)
      Redirect(routes.ProjectController.project(projectUrlKey))
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

  def plantuml(encodedSource: String) = Cached(req => "plantuml." + encodedSource) {
    StackAction { implicit request =>
      import util.PlantUmlHelper._

      val uml = getUmlSource(encodedSource)
      try {
        val output = new ByteArrayOutputStream()
        val reader: SourceStringReader = new SourceStringReader(uml)
        try {
          reader.generateImage(output, new FileFormatOption(FileFormat.PNG, false))
          SimpleResult(
            header = ResponseHeader(200, Map(CONTENT_LENGTH -> output.toByteArray.length.toString)),
            body = Enumerator(output.toByteArray)
          )
        } catch {
          case ex: IOException =>
            Logger.warn("Graphviz is not installed locally, redirecting to remote PlantUML servlet.")
            Redirect("http://www.plantuml.com/plantuml/img/"+encodedSource)
        }
      } catch {
        case iioe: IIOException =>
          BadRequest
          // Browser has closed the connection, so the HTTP OutputStream is closed
          // Silently catch the exception to avoid annoying log
      }
    }
  }

  def swagger(projectUrl: String, projectVersion: String) = StackAction { implicit request =>
    val projectUrlKey = ResourceUtil.decodeFileName(projectUrl)
    Global.projects.findByUrlKey(projectUrlKey).map { project =>
      Ok(html.swagger(controllers.api.routes.SwaggerApiController.getResourceListing(project.guid, projectVersion).url, AuthenticationController.loginForm))
    }.getOrElse(NotFound(html.notfound(AuthenticationController.loginForm)))
  }

}
