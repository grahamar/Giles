
import play.api.db.DB
import scala.slick.driver.H2Driver

package object dao {
  // Use H2Driver to connect to an H2 database
  val profile = H2Driver.profile

}
