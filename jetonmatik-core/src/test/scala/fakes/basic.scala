package fakes

import java.time.{Duration, Instant}
import java.util.UUID

import akka.util.ByteString

import scala.util.Random
import scala.collection.JavaConverters._

object Alphabet {
  val Nums = '0' to '9' mkString ""
  val LatinLettersInLowerCase = 'a' to 'z' mkString ""
  val LatinLettersInUpperCase = LatinLettersInLowerCase.toUpperCase
  val LatinLetters = LatinLettersInLowerCase + LatinLettersInUpperCase
  val Alphanumeric = LatinLetters + Nums
  val Whitespace = " \t\n"
  val Punctuation = ".,;!?"
}

object Util {
  def oneOf[T](variants: T*) = variants(Random.nextInt(variants.length))
  def oneOfSeq[T](variants: Seq[T]) = oneOf(variants:_*)
}

object Basic {

  def fakeByte = Random.nextInt().toByte
  def fakePositiveByte = Random.nextInt(Byte.MaxValue - 1).toByte

  def fakeInt = Random.nextInt()
  def fakePositiveInt = Random.nextInt(Int.MaxValue - 1)

  def fakeLong = Random.nextLong()
  def fakePositiveLong: Long = fakePositiveInt

  def fakeUuid = UUID.randomUUID()
  def fakeLongId = fakePositiveLong

  def fakeByteSeq(n: Int = Random.nextInt(10))(gen: => Byte = Random.nextInt().toByte): Seq[Byte] = {
    require(n > 0)
    Seq.fill[Byte](n)(gen)
  }

  def fakeByteArray(n: Int = Random.nextInt(10))(gen: => Byte = Random.nextInt().toByte): Array[Byte] = {
    fakeByteSeq(n)(gen).toArray
  }

  def fakeByteString(n: Int = Random.nextInt(10))(gen: => Byte = Random.nextInt().toByte): ByteString = {
    ByteString(fakeByteSeq(n)(gen):_*)
  }

  import fakes.Alphabet._

  def fakeCharString(n: Int)(alphabet: String = Alphanumeric): String =
    Stream.continually(Random.nextInt(alphabet.size)).map(alphabet).take(n).mkString

  def fakeHexString(n: Int) = fakeCharString(n * 2)()
}

object User {
  def fakeFirstName: String = JavaFaker.firstName()
  def fakeLastName: String = JavaFaker.lastName()

  def fakeFullName: String = s"$fakeFirstName $fakeLastName"
  def fakePassword: String = JavaFaker.words(3).asScala.mkString("-")
}

object Text {
  def fakeWord = JavaFaker.words(1).get(0)
  def fakeWords(n: Int = Random.nextInt(4) + 1): Seq[String] = JavaFaker.words(n).asScala
}

object Time {
  import fakes.Basic._

  def fakeInstant = Instant.ofEpochSecond(fakePositiveLong)
  def fakeDuration = Duration.ofSeconds(fakePositiveLong)
}

object Net {

  import fakes.Text._
  import fakes.Util._

  def fakeInternetDomain = oneOf("com", "org", "net")
  def fakeHost = s"$fakeWord.$fakeInternetDomain"
  def fakePath = fakeWords().mkString("/")

  def fakeHttpUrl = s"http://$fakeHost"
  def fakeHttpUri = fakeHttpUrl + "/" + fakePath

  def fakeHttpsUrl = s"https://$fakeHost"
  def fakeHttpsUri = fakeHttpsUrl + "/" + fakePath
}

