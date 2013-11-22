
import play.api.db.DB
import scala.slick.driver.H2Driver

package object dao {
  // Use H2Driver to connect to an H2 database
  val profile = H2Driver.profile

  import profile.simple._

  // Column types
  implicit val projectBranchColumnType = MappedColumnType.base[ProjectBranch, String]({ _.branchName }, ProjectBranch.apply)
  implicit val projectVersionColumnType = MappedColumnType.base[ProjectVersion, String]({ _.versionName }, ProjectVersion.apply)
  implicit val buildStatusColumnType = MappedColumnType.base[BuildStatus, String](BuildStatus.unapply, BuildStatus.apply(_).get)

}
