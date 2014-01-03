package util

import java.net.URLDecoder
import java.io.IOException

import net.sourceforge.plantuml.code.TranscoderUtil

object PlantUmlHelper {
  /**
   * Build the complete UML source from the compressed source extracted from the HTTP URI.
   *
   * @param source
     * the last part of the URI containing the compressed UML
   * @return the textual UML source
   */
  def getUmlSource(source: String): String = {
    var text = URLDecoder.decode(source, "UTF-8")
    try {
      text = TranscoderUtil.getDefaultTranscoder.decode(text)
    } catch {
      case ioe: IOException => {
        text = "' unable to decode string"
      }
    }
    if (text.startsWith("@start")) {
      text
    } else {
      val plantUmlSource: StringBuilder = new StringBuilder
      plantUmlSource.append("@startuml\n")
      plantUmlSource.append(text)
      if (!text.endsWith("\n")) {
        plantUmlSource.append("\n")
      }
      plantUmlSource.append("@enduml")
      plantUmlSource.toString()
    }
  }

}
