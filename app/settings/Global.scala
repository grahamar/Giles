package settings

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
    }
  }

}
