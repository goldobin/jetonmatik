package fakes

import java.time.Duration

import jetonmatik.util.Bytes

import scala.util.Random

object OAuth {

  import Basic._
  import Text._

  def fakeAccessToken = List.fill(3) {
    Bytes.toBase64String(fakeByteArray(32)())
  }.mkString(":")

  def fakeAccessTokenTtl = Duration.ofSeconds(Random.nextInt(3600) + 1)

  def fakeScopeEntry = fakeWords(Random.nextInt(2) + 1).mkString(":")
  def fakeScopeSet(n: Int = Random.nextInt(10)) = List.fill(n)(fakeScopeEntry).toSet
  def fakeScopeString(n: Int = Random.nextInt(10)) = fakeScopeSet(n).mkString(" ")
}
