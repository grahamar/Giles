package controllers

import play.api.mvc._

import settings._
import views._
import dao._
import profile.simple._

/**
 * Manage a database of computers
 */
object Application extends Controller {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.index())

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */
  def index = Action {
    val recentlyUpgradedProjects = UserDAO.recentlyUpdatedProjectsWithAuthors(10)
    Ok(html.index("", recentlyUpgradedProjects))
  }

  def projects = Action { Ok(html.projects("")) }

  def profile(username: String) = Action {
    val user = UserDAO.userForUsername(username)
    user.map{usr =>
      val projects = UserDAO.projectsForUser(usr)
      Ok(html.profile(usr, projects))
    }.getOrElse(NotFound)
  }

  def search(filter: String) = Action { implicit request =>
    Ok(html.projects(""))
  }

}

