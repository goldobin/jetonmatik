package jetonmatik.provider

import java.io.StringWriter
import java.security.PublicKey

import org.bouncycastle.util.io.pem.{PemObject, PemWriter}

trait FormattedPublicKeyProvider {
  val formattedPublicKey: String
}

trait PemFormattedPublicKeyProvider extends FormattedPublicKeyProvider {

  val publicKey: PublicKey
  val publicKeyAlgorithm: String

  override lazy val formattedPublicKey = {
    val writer = new StringWriter()
    val pemWriter = new PemWriter(writer)
    pemWriter.writeObject(
      new PemObject(
        s"$publicKeyAlgorithm PUBLIC KEY",
        publicKey.getEncoded))

    pemWriter.flush()
    pemWriter.close()

    writer.toString
  }
}
