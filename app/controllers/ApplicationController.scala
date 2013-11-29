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

    val fileRollups = Global.fileRollup.search(FileRollupQuery(limit = Some(10), order_by = Some("count"), order_direction = -1)).toSeq
    val popularFiles = fileRollups.filter(_.count > 10).take(10).flatMap { f =>
      Global.files.findByGuid(f.file_guid).flatMap { f =>
        Global.projects.findByGuid(f.project_guid).map(p => PopularFile(f, p))
      }
    }

    html.index(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, popularFiles, loginForm)
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
    maybeUser.map{ usr =>
      val projects = Global.projects.findByAuthorGuid(usr.guid)
      Ok(html.dashboard(ProjectHelper.getAuthorsAndBuildsForProjects(projects).toSeq, AuthenticationController.loginForm))
    }.getOrElse(Home)
  }

}