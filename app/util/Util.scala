package util

object Util {
  lazy val filteredUsernames = Seq("jenkins", "gerrit", "gerrit2")
  def topAuthorUsernames(amount: Int, authors: Seq[String]): Seq[String] = {
    authors.groupBy(auth => auth).mapValues(_.size).toSeq.
      sortBy(_._2)(Ordering[Int].reverse).take(amount).map(_._1).diff(filteredUsernames)
  }
}
