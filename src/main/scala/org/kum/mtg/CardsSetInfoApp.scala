package org.kum.mtg

import java.io.{File, FileOutputStream, OutputStreamWriter, Writer}
import java.net.URL
import java.nio.file.{Files, Path, Paths}

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.kum.net.HttpFetcher
import org.kum.util._

import scala.util.{Failure, Success, Try}

object CardsSetInfoApp extends App {

  val parser = new scopt.OptionParser[CardSetConfig]("Card set info tool") {
    head("Card set info tool", "0.1")

    opt[File]('f', "file").optional().valueName("<filename>")
      .action((file, c) => c.copy(src = Some(Left(file))))
      .validate(file =>
        if (!file.exists()) failure(s"File ${file.getName} not found")
        else if (file.isFile) success
        else failure(s"Not a file ${file.getName}")
      )

    opt[String]('c', "charset").optional().valueName("<charset>")
      .action((charset, c) => c.copy(charset = charset))
      .text("Charset of file. UTF-8 by default.")

    opt[String]('u', "url").optional().valueName("<URL>")
      .action((url, c) => c.copy(src = Some(Right(Try { new URL(url) }))) )

    opt[Unit]("oracle").optional().text("page with oracle")
      .action((_, c) => c.copy(infoType = OracleInfo))

    opt[String]("card-set").optional().valueName("<set name>")
      .text("name of the set")
      .action((name, c) => c.copy(infoType = CardSetInfoType(name)))


    opt[File]("output").optional().valueName("<filename>")
      .action((file, c) => c.copy(output = Some(file)))

    note("One and only one of -file and -url should be defined.")

    checkConfig(c =>
      c.infoType match {
        case UnknownInfoType =>
          failure("Have to define one of the type")
        case OracleInfo =>
          c.src match {
            case None => failure("One of -file or -url should be defined.")
            case Some(Right(Failure(url))) => failure(s"Incorrect URL $url.")
            case _ => success
          }
        case CardSetInfoType(name) =>
          success
      }
    )
  }

  parser.parse(args, CardSetConfig()) match {
    case None =>
    case Some(config) => new CardSetInfo(config).run()
  }

}

case class CardSetConfig(
                          src: Option[Either[File, Try[URL]]] = None,
                          infoType: InfoType = UnknownInfoType,
                          output: Option[File] = None,
                          charset: String = "utf-8")

case class CardInfo(oracleName: String, color: String, url: String) {
  private val basicLands = List("Plains", "Island", "Swamp", "Mountain", "Forest")
  def isBasicLand = basicLands.contains(oracleName)

  override def toString: String = s"$color\t$url\t$oracleName"
}

class OutputWriter(config: CardSetConfig) {

  private var realWriter: Option[Writer] =
    config.output.flatMap(output => {
      Try {
        new OutputStreamWriter(new FileOutputStream(output))
      } match {
        case Failure(ex) =>
          println("Failed to create an output file.")
          None
        case Success(writer) =>
          Try {
            writer.write("")
            writer
          }.recoverWith {
            case ex =>
              println("Failed to write to the output file.")
              Failure(ex)
          }.toOption
      }
    })

  def write(str: String): Unit = {
    realWriter match {
      case None =>
        print(str)
      case Some(writer) =>
        Try {
          writer.write(str)
        }.recover {
          case ex =>
            println("Failed to write to the output file.")
            print(str)
            realWriter = None
        }
    }
  }

  def writeln(str: String): Unit = write(str + "\n")

  def close(): Unit = realWriter match {
    case None =>
    case Some(writer) => Try { writer.close() }
  }
}

class CardSetInfo(config: CardSetConfig) {

  val fetcher = new HttpFetcher()

  def parseDocument(source: Either[(File, String), URL]): Document = {
    source match {
      case Left((file, charsetName)) =>
        val baseUri = config.infoType match {
          case OracleInfo => "http://gatherer.wizards.com/Pages/Card/Details.aspx"
        }
        Jsoup.parse(file, charsetName, baseUri)
      case Right(url) =>
        getDocumentByUrl(url)
    }
  }

  def downloadDocument(url: URL): Try[Document] = {

    val cacheFileName = "cache/" + url.toString.replaceAll("[/:?&+\\[\\]]+", "_").replaceAll("[\"%'=]+", "-")
    val file: File = new File(cacheFileName)
    val cached = if (file.exists()) {
      println(s">> cache: $cacheFileName for $url")
      Success(file)
    } else {
      println(s">> downloads document from $url")
      fetcher.fetch(url.toString, Map.empty)
        .map(is => {
          Files.copy(is, file.toPath)
          file
        })
    }

    cached.map(_ => Jsoup.parse(file, "UTF-8", url.toString))
  }

  def getDocumentByUrl(url: URL): Document = Jsoup.parse(url, 10000)

  def run(): Unit = {
    import JsonUtil.JsonArray

    val writer = new OutputWriter(config)
    Try {
      config.infoType match {
        case OracleInfo =>
          val source = config.src // [File, Try[URL]]
            .get.map(_.get) // [File, URL]
            .swap.map(file => file -> config.charset).swap // [(File, String), URL]

          val document = parseDocument(source)
          val oracle = extractCardInfo(document)
          writer.writeln(oracle.toJson)
        case CardSetInfoType(name) =>
          val cards = downloadCardSet(name)
          writer.writeln(cards.toJsonElement(true).toJson)
      }
    } match {
      case Failure(ex) =>
        ex.printStackTrace()
      case Success(_) =>
    }

    writer.close()
  }

  def extractCardInfo(document: Document): JsonElement = {
    import JsonUtil._
    val id = MTGCardPageParser.getCardId(document)
    val oracle = MTGCardPageParser.parseOracle(document)

    val languages: (String, JsonElement) = MTGCardPageParser.getLanguagesLink(document)
      .flatMap(langsUrl => downloadDocument(langsUrl))
      .map(langsDoc =>
        MTGCardPageParser.parseLanguages(langsDoc)
          .filter(_.language.equals("Russian"))
      )
      .map(langs => {
        println(s""">> ::get language cards for $id: ${langs.map(_.name).mkString(";")}""")
        langs.map(lang =>
          downloadDocument(new URL(lang.url + "&printed=true"))
            .map(doc =>
              MTGCardPageParser.parseOracle(doc)
            ) match {
            case Success(cards) =>
              lang.copy(cardInfo = cards.map(_._1).toJsonElement)
            case Failure(ex) =>
              lang.copy(error = ex.toString.toJsonElement)
          }
        )
      }) match {
      case Success(langs) =>
        "languages" -> langs.toJsonElement
      case Failure(ex) =>
        ex.printStackTrace()
        "languagesError" -> ex.toString.toJsonElement
    }

    val cardsArray = oracle.map(o =>
      List(
        "characteristics" -> o._2,
        "card" -> o._1,
        languages
      ).toJsonElement
    )

    List(
      "id" -> id,
      "info" -> cardsArray.toJsonElement
    ).toJsonElement
  }

  def downloadCardSet(name: String): List[JsonElement] = {
    import JsonUtil._

    def pageUrl(num: Int): String =
      s"""http://gatherer.wizards.com/Pages/Search/Default.aspx?page=$num&action=advanced&set=+["$name"]"""

    downloadDocument(new URL(pageUrl(0)))
      .map(document => {
        val totalPages: Int = document.select("[id$='_topPagingControlsContainer'] a").size() - 1
        document :: (1 until totalPages).flatMap(i => {
          downloadDocument(new URL(pageUrl(i))) match {
            case Success(doc) =>
              Some(doc)
            case Failure(ex) =>
              ex.printStackTrace()
              None
          }
        }).toList
      }) match {
      case Success(pages) =>
        val cardsUrls = pages
          .flatMap(page => MTGCardPageParser.parseCardList(page))

        println(s">> found ${pages.size} pages and ${cardsUrls.length} cards for '${name}' set")

        cardsUrls
          .map(cardUrl => {
            downloadDocument(cardUrl) match {
              case Failure(ex) =>
                ex.printStackTrace()
                List(
                  "error" -> ex.toString.toJsonElement,
                  "url" -> cardUrl.toString.toJsonElement
                ).toJsonElement
              case Success(cardDoc) =>
                extractCardInfo(cardDoc)
            }
          })
      case Failure(ex) =>
        ex.printStackTrace()
        Nil
    }
  }
}