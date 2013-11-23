import scala.slick.lifted.{ColumnBase, ToShapedValue}

package object dao {

  // Use H2Driver to connect to an H2 database
  val driver = scala.slick.driver.H2Driver

  import driver.simple._

  // Column types
  implicit val projectBranchColumnType = MappedColumnType.base[ProjectBranch, String]({ _.branchName }, ProjectBranch.apply)
  implicit val projectVersionColumnType = MappedColumnType.base[ProjectVersion, String]({ _.versionName }, ProjectVersion.apply)
  implicit val buildStatusColumnType = MappedColumnType.base[BuildStatus, String](BuildStatus.unapply, BuildStatus.apply(_).get)

  abstract class AbstTable[T](_tableTag : scala.slick.lifted.Tag, _tableName : scala.Predef.String)
    extends driver.profile.Table[T](_tableTag, _tableName)

  implicit def tableQueryToTableQueryExtensionMethods[T <: driver.profile.Table[_], U](q: scala.slick.lifted.TableQuery[T, U]) =
    new driver.profile.TableQueryExtensionMethods[T, U](q)

  @inline implicit final def anyToToShapedValue[T](value: T) = new ToShapedValue[T](value)
}
