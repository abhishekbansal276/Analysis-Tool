package utils

import org.apache.spark.sql.DataFrame

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import scala.util.Try

class CommonUtils {
  private val secretKey = "MySuperSecretKey1".take(16).padTo(16, '0')
  private val salt = "s@ltValue123".take(16).padTo(16, '0')

  def encrypt(text: String): String = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val ivParameterSpec = new IvParameterSpec(salt.getBytes("UTF-8"))

    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
    val encryptedBytes = cipher.doFinal(text.getBytes("UTF-8"))

    Base64.getEncoder.encodeToString(encryptedBytes)
  }

  def decrypt(encryptedText: String): Try[String] = Try {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES")
    val ivParameterSpec = new IvParameterSpec(salt.getBytes("UTF-8"))

    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
    val encryptedBytes = Base64.getDecoder.decode(encryptedText)

    new String(cipher.doFinal(encryptedBytes), "UTF-8")
  }

  def dataFrameToCsv(df: DataFrame): String = {
    val rows = df.collect().map(_.toSeq.map {
      case null => "null"
      case value: String => s""""$value""""
      case value => value.toString
    })

    val header = df.columns.mkString(",")
    val data = rows.map(_.mkString(",")).mkString("\n")

    s"$header\n$data"
  }

  def generateCSVZIP(csvBytes: Array[Byte]): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val zipOut = new ZipOutputStream(baos)

    try {
      val entry = new ZipEntry("data.csv")
      zipOut.putNextEntry(entry)
      zipOut.write(csvBytes)
      zipOut.closeEntry()
    } finally {
      zipOut.close()
    }

    baos.toByteArray
  }

  def generateZIPImage(imageBytesList: Seq[Array[Byte]]): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val zipOut = new ZipOutputStream(baos)
    try {
      imageBytesList.zipWithIndex.foreach { case (bytes, index) =>
        val entry = new ZipEntry(s"image$index.jpg")
        zipOut.putNextEntry(entry)
        zipOut.write(bytes)
        zipOut.closeEntry()
      }
    } finally {
      zipOut.close()
    }
    baos.toByteArray
  }

  def isSelectStatement(sql: String): Boolean = {
    sql.trim.toLowerCase.startsWith("select")
  }

  def isValidTable(sql: String): Boolean = {
    val regex = s"\\bdata\\b".r
    regex.findFirstIn(sql.toLowerCase).isDefined
  }
}