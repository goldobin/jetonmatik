package jetonmatik.relational

import java.io.PrintStream
import java.nio.file.Paths

object Schema extends App {


  case class Config(
    rdbmsType: RdbmsType.Value = RdbmsType.MySql,
    fileName: Option[String] = None
  )

  implicit val rdbmsTypeRead: scopt.Read[RdbmsType.Value] =
    scopt.Read.reads(RdbmsType.withName)

  val parser = new scopt.OptionParser[Config]("DdlGenerator") {
    head("Jetonmatik Ddl", "0.1")
    opt[RdbmsType.Value]('t', "rdbms-type") action { (x, c) =>
      c.copy(rdbmsType = x)
    } text s"the type of RDBMS. Supported: ${RdbmsType.values.mkString(", ")}"
    arg[String]("<file>") optional() action { (x, c) =>
      c.copy(fileName = Some(x))
    } text "write DDL to file"

    help("help") text "prints this usage text"
  }

  parser.parse(args, Config()) map { c =>
    val driver = RdbmsType.toDriver(c.rdbmsType)
    val schema = Tables(driver).schema

    val output: PrintStream = c.fileName match {
      case Some(name) => new PrintStream(Paths.get(name).toFile)
      case None => System.out
    }

    def printLine(line: String) = {
      output.print(line)
      output.println(";")
    }

    schema.createStatements foreach printLine

    output match {
      case System.out =>
      case _ => output.close()
    }
  }
}
