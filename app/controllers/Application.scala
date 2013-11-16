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
    val query = (for {
      u <- Global.dal.users
      p <- Global.dal.projects
      up <- Global.dal.userProjects if u.id === up.userId && p.id === up.projectId
    } yield (u, p)).sortBy(_._2.updated.desc)

    val projectsAndAuthors: Seq[(dao.Project, Seq[dao.User])] =
      Global.db.withSession{ implicit session: dao.profile.backend.Session =>
        query.take(10).list.groupBy( _._2 ).mapValues( _.map( _._1 ).toSeq ).toSeq
      }
    val recentlyUpgradedProjects = projectsAndAuthors.map((pa: (dao.Project, Seq[dao.User])) =>
      ProjectWithAuthors(pa._1.name, pa._1.url, pa._1.created, pa._1.updated, pa._1.id, pa._2)
    )

    Ok(html.index("", recentlyUpgradedProjects))
  }

  def projects = Action { Ok(html.projects("")) }

  def search(filter: String) = Action { implicit request =>
    Ok(html.projects(""))
  }

}

