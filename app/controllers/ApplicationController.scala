package controllers

import play.api.mvc._
import play.api.data._
import play.api.templates.HtmlFormat

import views._
import models._
import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import build.{ProjectSearchResult, DocumentationFactory}
import settings.Global
import dao.util.ProjectHelper
import play.api.Routes

/**
 * Manage a database of computers
 */
object ApplicationController extends Controller with OptionalAuthUser with AuthConfigImpl {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.ApplicationController.index)
  val Dashboard = Redirect(routes.ApplicationController.dashboard)

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */
  def index = StackAction { implicit request =>
    Ok(indexPage(AuthenticationController.loginForm))
  }

  def indexPage(loginForm: Form[Option[User]])(implicit flash: Flash, currentUser: Option[models.User]): HtmlFormat.Appendable = {
    val projects = Global.projects.findRecentlyUpdated(10)
    val userFavourites = currentUser.map(Global.favourites.findAllByUser(_).map(_.project_guid).toSeq).getOrElse(Seq.empty)
    val fileRollups = Global.fileRollup.search(FileRollupQuery(limit = Some(10), order_by = Some("count"), order_direction = -1)).toSeq
    val popularFiles = fileRollups.filter(_.count > 10).take(10).flatMap { f =>
      Global.files.findByGuid(f.file_guid).flatMap { f =>
        Global.projects.findByGuid(f.project_guid).map(p => PopularFile(f, p))
      }
    }

    html.index(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, popularFiles, userFavourites, loginForm)
  }

  def search(filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocumentationFactory.searchService.search(filter)
    Ok(html.search(results, filter, AuthenticationController.loginForm))
  }

  def searchProject(projectUrlKey: String, filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocumentationFactory.searchService.searchProject(projectUrlKey, filter)
    Ok(html.search(results, filter, AuthenticationController.loginForm))
  }

  def searchProjectVersion(projectUrlKey: String, projectVersion: String, filter: String) = StackAction { implicit request =>
    val results: Seq[ProjectSearchResult] = DocumentationFactory.searchService.searchProjectVersion(projectUrlKey, projectVersion, filter)
    Ok(html.search(results, filter, AuthenticationController.loginForm))
  }

  def dashboard = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr => {
      val projects = (Global.projects.findByAuthorUsername(usr.username) ++ Global.projects.findByCreatedBy(usr.username)).toSet
      val userFavourites = Global.favourites.findAllByUser(usr).map(_.project_guid).toSeq
      Ok(html.dashboard(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, userFavourites, AuthenticationController.loginForm))
    }}.getOrElse(Home)
  }

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        ProjectController.favouriteProject,
        ProjectController.unfavouriteProject,
        PublicationController.editPublication
      )
    ).as("text/javascript")
  }

}