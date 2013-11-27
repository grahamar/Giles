package dao.util

import com.mongodb.casbah.commons.conversions.MongoConversionHelper
import org.bson.{BSON, Transformer}
import models._

object RegisterAnyValConversionHelpers extends AnyValConversionHelpers {
  def apply() = {
    super.register()
  }
}
trait AnyValConversionHelpers extends AnyValConversionSerializer with AnyValConversionDeserializer

trait AnyValConversionSerializer extends MongoConversionHelper {

  private val encodeTypeBuildStatus = classOf[BuildStatus]

  /** Encoding hook for MongoDB To be able to persist AnyVal models to MongoDB */
  private val transformer = new Transformer {
    def transform(o: Any): AnyRef = o match {
      case b: BuildStatus => b.toString
    }
  }

  override def register() = {
    /** Encoding hook for MongoDB To be able to persist AnyVal models to MongoDB */
    BSON.addEncodingHook(encodeTypeBuildStatus, transformer)
    super.register()
  }

  override def unregister() = {
    BSON.removeEncodingHooks(encodeTypeBuildStatus)
    super.unregister()
  }
}

trait AnyValConversionDeserializer extends MongoConversionHelper {
  override def register() = {
    super.register()
  }
  override def unregister() = {
    super.unregister()
  }
}
