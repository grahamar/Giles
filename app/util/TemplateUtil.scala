package util

import java.sql.Timestamp
import com.ocpsoft.pretty.time.PrettyTime

object TemplateUtil {
  def displayTimeSince(since: Timestamp): String = {
    new PrettyTime().format(since)
  }
}
