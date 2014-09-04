package controllers

import java.io.File

import controllers.auth.{AuthConfigImpl, OptionalAuthUser}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import sbt.{IO, Path}
import settings.Global
import util.ActivatorGitUtils
import activator.cache.{CacheProperties, IndexDbProvider}
import activator._

import scala.util.Try

object ActivatorTemplatesController extends Controller with OptionalAuthUser with AuthConfigImpl {

  private lazy val IndexProvider = IndexDbProvider.default

  def index = StackAction { implicit request =>
    val maybeUser = loggedIn
    maybeUser.map{ usr =>
      Ok(views.html.activatorIndex(AuthenticationController.loginForm))
    }.getOrElse(ApplicationController.Home)
  }

  val templateForm = Form(
    mapping(
      "templateRepo" -> text
    )(TemplateContribData.apply)(TemplateContribData.unapply)
  )

  def template = StackAction { implicit request =>
    try {
      templateForm.bindFromRequest()(request).fold(
        formWithErrors => {
          BadRequest(views.html.activatorIndex(AuthenticationController.loginForm))
        },
        data => {
          val metadata = ActivatorGitUtils.cacheTemplateFromRepo(data)
          val existsInIndex = Try {
            val reader = IndexProvider.open(Global.ActivatorCacheIndexDir)
            try {
              reader.metadata.exists(_.id == metadata.id)
            } finally {
              reader.close()
            }
          }.getOrElse(false)
          if (!existsInIndex) {
            Logger.info(s"Template '${metadata.name}' doesn't already exist. Creating...")
            val writer = IndexProvider.write(Global.ActivatorCacheIndexDir)
            try {
              writer.insert(metadata)
            } finally {
              writer.close()
            }
          } else {
            Logger.warn(s"Template '${metadata.name}' already exists, keeping metadata the same, uploading new template files.")
          }
          Global.activatorS3Client.backupIndex(Global.ActivatorCacheIndexDir)
          Redirect(routes.ActivatorTemplatesController.index())
        }
      )
    } catch {
      case ex: Exception =>
        Logger.error("", ex)
        throw ex
    }
  }

  def templateBundle(firstBit: String, secondBit: String, id: String, activatorVersion: String, templateName: String) = StackAction { implicit request =>
    ???
  }

  def templateFile(firstBit: String, secondBit: String, templateName: String) = StackAction { implicit request =>
    Ok.sendFile(
      content = Path(Global.ActivatorCacheBaseFile) / s"templates/$firstBit/$secondBit/$templateName",
      inline = true
    )
  }

  def currentIndexFile = StackAction { implicit request =>
    try {
      val result: ProcessResult[File] = IO.withTemporaryFile("index", "zip") { indexZip =>
        val indexFiles = Global.ActivatorCacheIndexDir.listFiles().map(f => f -> f.getName)
        ZipHelper.zip(indexFiles, indexZip)
        for {
          hash <- hashFile(indexZip)
          propsFile <- makeTmpFile
          props <- CacheProperties.write(propsFile, hash.hashCode, hash)
        } yield props
      }

      result match {
        case s: ProcessSuccess[File] =>
          Ok.sendFile(
            content = s.value,
            inline = true
          )

        case e: ProcessFailure =>
          e.failures.foreach(e => e.cause.map(Logger.error(e.msg, _)).getOrElse(Logger.error(e.msg)))
          InternalServerError(e.failures.map(f => f.msg).mkString("\n"))
      }
    } catch {
      case ex: Exception =>
        Logger.error("", ex)
        throw ex
    }
  }

  def indexFile(indexFile: String) = StackAction { implicit request =>
    IO.withTemporaryFile("index", "zip") { indexZip =>
      val indexFiles = Global.ActivatorCacheIndexDir.listFiles().map(f => f -> f.getName)
      ZipHelper.zip(indexFiles, indexZip)
      Ok.sendFile(
        content = indexZip,
        inline = true
      )
    }
  }

  private def hashFile(file: java.io.File): ProcessResult[String] = {
    Validating.withMsg(s"Failed to hash index files: $file") {
      hashing.hash(file)
    }
  }

  private def makeTmpFile: ProcessResult[java.io.File] = {
    Validating.withMsg(s"Unable to create temporary file") {
      val file = java.io.File.createTempFile("activator-cache", "properties")
      file.deleteOnExit()
      file
    }
  }

  case class TemplateContribData(repo: String)

}
