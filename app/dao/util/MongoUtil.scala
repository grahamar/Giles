package dao.util

import java.io.File

import com.mongodb._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoCollection, MongoConnection, MongoDB}
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Logger

import scala.collection.JavaConverters._

case class Index(field: String, unique: Boolean=false)

object MongoUtil {

  def collection(name: String, indexes: Seq[Index]=Seq.empty) = {
    val c = MongoInit.mongoDb(name)
    indexes.foreach { idx =>
      MongoInit.safeEnsureIndexes(c, Seq((idx.field, idx.unique)))
    }
    c
  }

  def collectionWithIndexes(name: String, indexes: Seq[Index]=Seq.empty) = {
    val c = collection(name, indexes)
    MongoInit.safeEnsureIndexes(c, Seq(("guid", true)))
    c
  }

}

object MongoInit extends MongoInit {
  private lazy val ConfigFile = new File(sys.props.get("config.file").getOrElse("conf/application.conf"))
  override val mongoConfig = ConfigFactory.parseFile(ConfigFile)
}

object TestMongoInit extends MongoInit {
  override val mongoConfig = ConfigFactory.parseResources("test_mongodb.yml")
}

private[util] trait MongoInit {

  case class IndexedAttribute(key: String, ascending: Boolean)
  case class Index(attributes: Seq[IndexedAttribute], name: String, isUnique: Boolean)

  val mongoConfig: Config

  lazy val mongo: Mongo = {
    val connsPerHost = 16
    val hosts = mongoConfig.getStringList("db.hosts").asScala

    Logger.info(s"Using mongo hosts '$hosts'")

    val servers = hosts.map(_.split(":")).map((hp: Array[String]) => new ServerAddress(hp(0), hp(1).toInt))
    new MongoClient(servers.toList.asJava,  MongoClientOptions.builder().connectionsPerHost(connsPerHost).build())
  }

  lazy val mongoDb: MongoDB = {
    new MongoConnection(mongo).getDB(mongoConfig.getString("db.name"))
  }

  def safeEnsureIndexes(collection: MongoCollection, indexes: Seq[(String, Boolean)]): Unit = {
    safeEnsureCompoundIndexes(
      collection,
      indexes.map {
        case (key, isUnique) => Index(Seq(IndexedAttribute(key, ascending = true)), key, isUnique)
      })
  }

  def safeEnsureCompoundIndexes(collection: MongoCollection, indexes: Seq[Index]): Unit = {
    def getIndexObject(indexedAttributes: Seq[IndexedAttribute]) = {
      MongoDBObject(indexedAttributes.toList.map(attr => (attr.key, if (attr.ascending) 1 else -1)))
    }

    if (mongoConfig.getBoolean("db.ensureIndex")) {
      indexes.foreach { index =>
        val Index(indexedAttributes, name, isUnique) = index
        collection.ensureIndex(getIndexObject(indexedAttributes), name, unique = isUnique)
      }
    } else {
      def checkIndexExists(index: Index): Unit = {
        val Index(indexedAttributes, name, _) = index
        val indexObject = getIndexObject(indexedAttributes)

        collection.getIndexInfo.find { dbo =>
          dbo.get("key") match {
            case key: DBObject => indexObject == key
            case _ => false
          }
        }.getOrElse(sys.error("required mongo index '%s' on collection '%s' not found".format(name, collection.getFullName)))
      }

      indexes.foreach(checkIndexExists)
    }
  }
}
