package jetonmatik.server.actor

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

trait GeneratedKeys {
  private val keyGen = KeyPairGenerator.getInstance("RSA")
  keyGen.initialize(2048)

  private val keyPair = keyGen.genKeyPair()

  val keyPublic = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  val keyPrivate = keyPair.getPrivate.asInstanceOf[RSAPrivateKey]
}

