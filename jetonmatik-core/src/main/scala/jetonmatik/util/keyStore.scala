package jetonmatik.util

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}
import java.security.cert.Certificate
import java.security.{Key, KeyStore => JcaKeyStore, PublicKey}

import akka.util.ByteString

trait KeyStoreFactory {
  protected def createEmptyKeyStore(): JcaKeyStore = java.security.KeyStore.getInstance("JCEKS")

  def createKeyStore(password: String): KeyStore
}

class FileKeyStoreFactory(rawKeyStore: ByteString) extends KeyStoreFactory with Serializable {

  def this(path: Path) {
    this(ByteString(Files.readAllBytes(path)))
  }

  override def createKeyStore(password: String): KeyStore = {
    val ks = createEmptyKeyStore()

    val is = new ByteArrayInputStream(rawKeyStore.toArray)
    ks.load(is, password.toCharArray)

    is.close()

    new JcaKeyStoreWrapper(ks)
  }
}

object KeyStoreFactory {
  def apply(path: Path) = new FileKeyStoreFactory(path)
  def apply(rawKeyStore: ByteString) = new FileKeyStoreFactory(rawKeyStore)
}

case class KeyAliasAndPassword (alias: String, password: String) {
  override def toString: String = s"${this.getClass.getName} {alias=$alias,password:***}"
}

trait KeyStore {
  def obtainKey(aliasAndPassword: KeyAliasAndPassword): Key =
    obtainKey(
      aliasAndPassword.alias,
      aliasAndPassword.password
    )

  def obtainKey(alias: String, password: String): Key
  def obtainCertificate(alias: String): Certificate
  def obtainPublicKey(alias: String): PublicKey = obtainCertificate(alias).getPublicKey
}

class JcaKeyStoreWrapper(keyStore: JcaKeyStore) extends KeyStore {

  override def obtainKey(alias: String, password: String): Key = {
    val key = keyStore.getKey(alias, password.toCharArray)
    assume(key != null, s"The key with alias '$alias' not found in key store. Incorrect alias or password")

    key
  }

  override def obtainCertificate(alias: String): Certificate = {
    val cert = keyStore.getCertificate(alias)
    assume(cert != null, s"The certificate with alias '$alias' not found in key store. Incorrect alias")

    cert
  }
}

