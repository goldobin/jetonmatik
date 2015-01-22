package jetonmatik.util

import java.nio.ByteBuffer
import java.util.{Base64, UUID}

import akka.util.ByteString

/*
 * Parts of code has been taken from sbt.Hash object
 * http://www.scala-sbt.org/0.13.2/sxr/sbt/Hash.scala.html
 * Copyright 2009 Mark Harrah
 */
object Bytes {

  def toBytes(id : UUID) : Array[Byte] =
    ByteBuffer
      .allocate(16)
      .putLong(id.getMostSignificantBits)
      .putLong(8, id.getLeastSignificantBits)
      .array()

  def toBytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  /** Converts an array of `bytes` to a hexadecimal representation String.*/
  def toHexString(bytes: Array[Byte]): String = {
    val buffer = new StringBuilder(bytes.length * 2)
    for (i <- 0 until bytes.length) {
      val b = bytes(i)
      val bi: Int = if (b < 0) b + 256 else b
      buffer append toHexChar((bi >>> 4).asInstanceOf[Byte])
      buffer append toHexChar((bi & 0x0F).asInstanceOf[Byte])
    }
    buffer.toString()
  }

  def toHexString(byteString: ByteString): String = toHexString(byteString.toArray)

  /** Converts the provided hexadecimal representation `hex` to an array of bytes.
    * The hexadecimal representation must have an even number of characters in the range 0-9, a-f, or A-F. */
  def parseHexString(hex: String): Array[Byte] = {
    require((hex.length & 1) == 0, "Hex string should have length 2*n")
    val array = new Array[Byte](hex.length >> 1)
    for (i <- 0 until hex.length by 2) {
      val c1 = hex.charAt(i)
      val c2 = hex.charAt(i + 1)
      array(i >> 1) = ((parseHexChar(c1) << 4) | parseHexChar(c2)).asInstanceOf[Byte]
    }
    array
  }

  def parseHexStringStripWhitespace(hex: String): Array[Byte] = {
    parseHexString(hex.replaceAll("\\s+", ""))
  }

  private val base64Encoder = Base64.getEncoder
  private val base64Decoder = Base64.getDecoder

  def toBase64String(bytes: Array[Byte]): String  = {
    base64Encoder.encodeToString(bytes)
  }

  def parseBase64String(s: String): Array[Byte] = {
    base64Decoder.decode(s)
  }

  private def toHexChar(b: Byte): Char = {
    assert(b >= 0 && b <= 15, "Byte should be in range from 0x0 to 0xf")
    if (b < 10)
      ('0'.asInstanceOf[Int] + b).asInstanceOf[Char]
    else
      ('a'.asInstanceOf[Int] + (b - 10)).asInstanceOf[Char]
  }

  private def parseHexChar(c: Char): Int = {
    val b =
      if (c >= '0' && c <= '9')
        c - '0'
      else if (c >= 'a' && c <= 'f')
        (c - 'a') + 10
      else if (c >= 'A' && c <= 'F')
        (c - 'A') + 10
      else
        throw new IllegalArgumentException(s"Not a hex character: '$c'")
    b
  }
}
