package settings

import play.api.db.DB
import play.api.Play.current
import play.api.{Logger, GlobalSettings}

import dao._
import driver.simple._

object Global extends GlobalSettings {

  lazy val db = driver.backend.Database.forDataSource(DB.getDataSource())
  lazy val dal = new DataAccessLayer(db)

  override def onStart(app: play.api.Application) {
    import dal._

    database withSession { implicit session: driver.backend.Session =>
      Logger.info("Create DB schema and populate test data.")

      dal.create
    }
  }

}
