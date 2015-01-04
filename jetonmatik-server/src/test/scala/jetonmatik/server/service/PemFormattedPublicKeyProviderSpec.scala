package jetonmatik.server.service

import fakes.{Alphabet, Basic}
import jetonmatik.server.GeneratedKeys
import jetonmatik.util.Bytes
import org.scalatest.{Matchers, FlatSpec}

class PemFormattedPublicKeyProviderSpec extends FlatSpec with Matchers {
  "PemFormattedPublicKeyProvider" should "format key" in new PemFormattedPublicKeyProvider with GeneratedKeys {

    val expectingBase64Key = Bytes.toBase64String(rsaPublicKey.getEncoded)

    override lazy val publicKeyAlgorithm: String = Basic.fakeCharString(3)(Alphabet.LatinLettersInUpperCase)
    override lazy val publicKey = rsaPublicKey

    formattedPublicKey should startWith (s"-----BEGIN $publicKeyAlgorithm PUBLIC KEY-----")
    formattedPublicKey should endWith (s"-----END $publicKeyAlgorithm PUBLIC KEY-----\n")

    val lines = formattedPublicKey.split("\n")

    val keyText = lines.drop(1).dropRight(1).mkString("")

    keyText should be (expectingBase64Key)
  }
}
