package util

import com.ocpsoft.pretty.time.PrettyTime
import org.joda.time.DateTime

object TemplateUtil {
  def displayTimeSince(since: DateTime): String = {
    new PrettyTime().format(since.toDate)
  }
}
