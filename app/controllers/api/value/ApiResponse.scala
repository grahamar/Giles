package controllers.api.value

object ApiResponse {
  val ERROR = 1
  val WARNING = 2
  val INFO = 3
  val OK = 4
  val TOO_BUSY = 5
}

/**
 * This class has mutable values to accommodate how the XML is built using getter and setter methods.
 * @param code the HTTP status code
 * @param message the response message
 */
case class ApiResponse(code: Int, message: String) {
  def this() = this(0, null)

  val `type` = code match {
    case ApiResponse.ERROR => "error"
    case ApiResponse.WARNING => "warning"
    case ApiResponse.INFO => "info"
    case ApiResponse.OK => "ok"
    case ApiResponse.TOO_BUSY => "too busy"
    case _ => "unknown"
  }

}
