package build

import java.io.{File, FileOutputStream}
import java.util.zip.ZipOutputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.config.ConfigFactory

import scala.util.Try

object DefaultAmazonS3Client {
  import util.ResourceUtil._

  private lazy val config = ConfigFactory.load("aws")
  private lazy val credentials = new BasicAWSCredentials(config.getString("aws.s3.accesskey"), config.getString("aws.s3.secretkey"))
  private lazy val amazonS3Client = new AmazonS3Client(credentials)
  private val DefaultRemoteIndexFilename = DirectoryHandlerHelper.Config.getString("index.remote.filename")

  /**
   * Upload a file to standard bucket on S3
   */
  private def upload(file: File, filename: Option[String] = None): Boolean = {
    try {
      val s3Filename = filename.getOrElse(file.getName)
      println(s"Uploading index '${file.getAbsolutePath}' -> '$s3Filename' to S3.")
      amazonS3Client.putObject(config.getString("aws.s3.bucket"), s3Filename, file)
      true
    } catch {
      case ex: Exception =>
        System.err.println(ex.getMessage, ex)
        false
    }
  }

  /**
   * Download a file from standard bucket on S3
   */
  private def download(file: File, filename: String): Boolean = {
    try {
      println(s"Downloading index '${file.getAbsolutePath}' from S3.")
      amazonS3Client.getObject(
        new GetObjectRequest(config.getString("aws.s3.bucket"), filename),
        file
      )
      true
    } catch {
      case ex: Exception =>
        System.err.println(ex.getMessage, ex)
        false
    }
  }

  /**
   * Deletes a file to standard bucket on S3
   */
  private def delete(fileKeyName: String): Boolean = {
    try {
      println(s"Deleting S3 file '$fileKeyName'.")
      amazonS3Client.deleteObject(config.getString("aws.s3.bucket"), fileKeyName)
      true
    } catch {
      case ex: Exception =>
        System.err.println(ex.getMessage, ex)
        false
    }
  }

  /**
   * Checks if the file exists on the  standard bucket of S3
   */
  private def doesFileExist(fileKeyName: String): Boolean = {
    try {
      amazonS3Client.getObjectMetadata(config.getString("aws.s3.bucket"), fileKeyName)
      true
    } catch {
      case ex: AmazonServiceException if ex.getStatusCode == 404 =>
        false
      case ex: Exception =>
        System.err.println(ex.getMessage, ex)
        false
    }
  }

  private def uploadOrOverwrite(file: File, filename: Option[String] = None): Boolean = {
    if(doesFileExist(filename.getOrElse(file.getName))) {
      println("Remote index exists, deleting...")
      if(delete(filename.getOrElse(file.getName))) {
        println("Deleted remote index, uploading new index...")
        val uploaded = upload(file, filename)
        if(uploaded) {
          println("Remote index backed up.")
        } else {
          System.err.println("Remote index not uploaded!")
        }
        uploaded
      } else {
        System.err.println("Couldn't remove existing remote index!")
        false
      }
    } else {
      println("Remote index doesn't exist, uploading...")
      val uploaded = upload(file, filename)
      if(uploaded) {
        println("Remote index backed up.")
      } else {
        System.err.println("Remote index not uploaded!")
      }
      uploaded
    }
  }

  def backupIndex(indexDir: File): Unit = {
    withTemporaryFile("giles_index_backup", ".zip") { tmpFile =>
      doWith(new ZipOutputStream(new FileOutputStream(tmpFile))) { zipOut =>
        compressDirectory(indexDir, zipOut)
      }
      uploadOrOverwrite(tmpFile, Some(DefaultRemoteIndexFilename))
    }
  }

  def tryRestoreIndex(file: File) = {
    val tryRestoreOnStart = Try(DirectoryHandlerHelper.Config.getBoolean("index.tryrestoreonstart")).getOrElse(false)
    val remoteIndexExists = if(tryRestoreOnStart) doesFileExist(DefaultRemoteIndexFilename) else false
    println(s"Local Index Exists? - ${file.exists()}")
    println(s"Restore index enabled? - $tryRestoreOnStart")
    println(s"Remote index exists? - $remoteIndexExists")
    if(tryRestoreOnStart && !file.exists() && remoteIndexExists) {
      println("Restoring index from S3.")
      withTemporaryFile("giles_index_backup", ".zip") { tmpFile =>
        download(tmpFile, DefaultRemoteIndexFilename)
        extractDirectory(tmpFile, file)
      }
    }
  }

}
