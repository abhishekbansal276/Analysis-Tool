package utils

import java.util.Base64
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
}