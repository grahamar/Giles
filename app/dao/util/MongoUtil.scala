package dao.util

import scala.collection.JavaConverters._

import com.mongodb.{Mongo, DBObject, ServerAddress}
import com.mongodb.casbah.{MongoCollection, MongoOptions, MongoConnection, MongoDB}
import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.config.{ConfigFactory, Config}

case class Index(field: String, unique: Boolean=false)

object MongoUtil {

  def collection(name: String, indexes: Seq[Index]=Seq.empty) = {
    val c = MongoInit.mongoDb(name)
    indexes.foreach { idx =>
      MongoInit.safeEnsureIndexes(c, Seq((idx.field, idx.unique)))
    }
    c
  }

  def collectionWithGuid(name: String, indexes: Seq[Index]=Seq.empty) = {
    val c = collection(name, indexes)
    MongoInit.safeEnsureIndexes(c, Seq(("guid", true)))
    c
  }

}

object MongoInit extends MongoInit {
  override val mongoConfig = ConfigFactory.load("mongodb")
}

private[util] trait MongoInit {

  case class IndexedAttribute(key: String, ascending: Boolean)
  case class Index(attributes: Seq[IndexedAttribute], name: String, isUnique: Boolean)

  val mongoConfig: Config

  lazy val mongo: Mongo = {
    val hosts = mongoConfig.getStringList("hosts").asScala
    val dbName = mongoConfig.getString("dbname")
    val connsPerHost = 16
    val servers = hosts.map(_.split(":")).map((hp: Array[String]) => new ServerAddress(hp(0), hp(1).toInt))
    new Mongo(servers.toList.asJava,  MongoOptions(connectionsPerHost = connsPerHost))
  }

  lazy val mongoDb: MongoDB = {
    new MongoConnection(mongo).getDB(mongoConfig.getString("dbname"))
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

    if (mongoConfig.getBoolean("ensureIndex")) {
      indexes.foreach { index =>
        val Index(indexedAttributes, name, isUnique) = index
        collection.ensureIndex(getIndexObject(indexedAttributes), name, unique = isUnique)
      }
    } else {
      def checkIndexExists(index: Index): Unit = {
        val Index(indexedAttributes, name, _) = index
        val indexObject = getIndexObject(indexedAttributes)

        collection.getIndexInfo().find { dbo =>
          dbo.get("key") match {
            case key: DBObject => indexObject == key
            case _ => false
          }
        }.getOrElse(sys.error("required mongo index '%s' on collection '%s' not found".format(name, collection.getFullName())))
      }

      indexes.foreach(checkIndexExists)
    }
  }
}
