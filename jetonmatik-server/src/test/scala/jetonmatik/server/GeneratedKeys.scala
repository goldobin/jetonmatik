package jetonmatik.server

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

trait GeneratedKeys {
  private lazy val rsaKeyGen = KeyPairGenerator.getInstance("RSA")
  rsaKeyGen.initialize(2048)

  private lazy val rsaKeyPair = rsaKeyGen.genKeyPair()

  lazy val rsaPublicKey = rsaKeyPair.getPublic.asInstanceOf[RSAPublicKey]
  lazy val rsaPrivateKey = rsaKeyPair.getPrivate.asInstanceOf[RSAPrivateKey]
}

