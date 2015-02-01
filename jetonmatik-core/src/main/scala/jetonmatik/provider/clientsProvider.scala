package jetonmatik.provider

import java.io.Reader
import java.nio.file.{Files, Path}
import java.time.Duration

import jetonmatik.model.Client
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

trait ClientsProvider {
  def obtainClients(): Set[Client]
}

class ClientsParseException(message: String = null, cause: Throwable = null)
  extends RuntimeException(
    message,
    cause
  ) {
}

// TODO: Write unit test
trait YamlClientsProvider extends ClientsProvider {

  private val yaml = new Yaml()
  protected def clientsReader: Reader

  override def obtainClients(): Set[Client] = {

    val clients = try {
      yaml
        .load(clientsReader)
        .asInstanceOf[java.util.List[java.util.Map[String, Object]]]
        .asScala
        .map(_.asScala)
    } catch {
      case NonFatal(e) =>
        throw new ClientsParseException(cause = e)
    }

    for (clientFieldMap <- clients) yield {
      def obtainRequiredValue[T](key: String): T = {
        clientFieldMap.get(key) match {
          case Some(value) => value.asInstanceOf[T]
          case None =>
            throw new ClientsParseException(
              s"""The field "$key" should be defined for client""")
        }
      }

      def obtainOptionalValue[T](key: String, default: T): T = {
        clientFieldMap.get(key) match {
          case Some(value) => value.asInstanceOf[T]
          case None => default
        }
      }

      val scope: Set[String] = clientFieldMap.get("scope") match {
        case Some(value) => value.asInstanceOf[java.util.List[String]].asScala.toSet
        case None => Set.empty
      }


      val tokenTtl: Duration = {
        val v = obtainOptionalValue("tokenTtl", "PT1M")
        try {
          Duration.parse(v)
        } catch {
          case NonFatal(e) =>
            throw new ClientsParseException(
              s"""Token TTL value="$v" can't be parsed. """ +
                "Please make sure the TTL is specified in ISO-8601 format", e)
        }
      }

      if (tokenTtl.isNegative || tokenTtl.isZero) {
        throw new ClientsParseException(
          "Failed to process clients file. Token TTL should be positive")
      }

      Client(
        id = obtainRequiredValue("clientId"),
        secretHash = obtainRequiredValue("clientSecretHash"),
        name = obtainOptionalValue("name", ""),
        scope = scope,
        tokenTtl = tokenTtl
      )
    }
  }.toSet
}

object YamlClientsProvider {
  def apply(path: Path): YamlClientsProvider = {
    new YamlClientsProvider {
      override def clientsReader = Files.newBufferedReader(path)
    }
  }
}