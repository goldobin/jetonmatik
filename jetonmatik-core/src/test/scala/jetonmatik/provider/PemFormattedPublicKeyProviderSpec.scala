package jetonmatik.provider

import fakes.{Alphabet, Basic}
import jetonmatik.util.{GeneratedKeys, Bytes}
import org.scalatest.{FlatSpec, Matchers}

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
