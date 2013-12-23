package util

object Util {
  lazy val filteredUsernames = Seq("jenkins", "gerrit", "gerrit2")
  def topAuthorUsernames(amount: Int, authors: Seq[String]): Seq[String] = {
    authors.groupBy(auth => auth).mapValues(_.size).toSeq.
      sortBy(_._2)(Ordering[Int].reverse).take(amount).map(_._1).diff(filteredUsernames)
  }

  implicit val ord = BuildOrdering
  implicit val versionOrd = VersionOrdering

  object BuildOrdering extends Ordering[models.Build] {
    def compare(build1: models.Build, build2: models.Build) =
      compareBuildVersions(build1.version, build2.version)
  }

  object VersionOrdering extends Ordering[String] {
    def compare(version1: String, version2: String) =
      compareBuildVersions(version1, version2)
  }

  private def groupIt(str:String) = {
    if (str.nonEmpty && str.head.isDigit) str.takeWhile(_.isDigit)
    else str.takeWhile(!_.isDigit)
  }

  private val dec="""(\d+)""".r

  def compareBuildVersions(version2: String, version1: String): Int = {
    (groupIt(version1), groupIt(version2)) match {
      case ("","") => 0
      case (_, "HEAD") => -1
      case ("HEAD", _) => 1
      case (dec(x),dec(y)) if x.toInt == y.toInt =>
        compareBuildVersions(version1.substring(x.size), version2.substring(y.size))
      case (dec(x),dec(y)) => x.toInt - y.toInt
      case (x,y) if x == y =>
        compareBuildVersions(version1.substring(x.size), version2.substring(y.size))
      case (x,y) => y compareTo x
    }
  }

}
