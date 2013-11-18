package settings

import org.apache.commons.lang3.RandomStringUtils
import play.api.db.DB
import play.api.Play.current
import play.api.{Logger, GlobalSettings}

import dao._
import profile.simple._

object Global extends GlobalSettings {

  lazy val db = Database.forDataSource(DB.getDataSource())
  lazy val dal = new DAL(db)

  override def onStart(app: play.api.Application) {
    import dal._

    database withSession { implicit session: Session =>
      Logger.info("Create DB schema and populate test data.")

      dal.create

      val grahamar = insertUser(User("grahamar", "graham.a.r@gmail.com", "grahamar", Some("Graham"), Some("Rhodes"), Some("http://RedHogs.github.io")))
      val grahamarProjects = Seq.fill(20)(ProjectHelper(RandomStringUtils.randomAlphabetic(20).toLowerCase))

      insertUser(User("aclery", "alison.clery@gmail.com", "aclery"))

      insertProject(ProjectHelper("Test Project 1"))
      val persistedProject = grahamarProjects.map(insertProject)

      persistedProject.foreach(proj =>
        proj.id.foreach( projId =>
          grahamar.id.map(userId => userProjects.insert(userId -> projId))
        )
      )
    }
  }

  override def onStop(app : play.api.Application) {
    import dal._

    database withSession { implicit session: Session =>
      dal.drop
    }
  }
}
