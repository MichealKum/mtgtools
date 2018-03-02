package org.kum.mtg

import java.net.URL
import org.jsoup.nodes.{Document, Element, TextNode}
import org.jsoup.select.Elements
import org.kum.util._

import scala.util.Try
import scala.collection.JavaConverters._

object MTGCardPageParser {

  def getCardId(doc: Document): String =
    doc.selectFirst("[id$='_Details'].current a").attr("href").split("=").last

  def parseOracle(doc: Document): List[(MTGCardPageInfo, MTGCardCharacters)] = {
    doc.select(".cardComponentContainer")
      .filterNot(_.children().size == 0)
      .map(elem  => (parseCardInfo(elem), parseCardCharacters(elem)))
  }

  def parseLanguages(langsDoc: Document): List[CardLanguage] =
    langsDoc.select("tr.cardItem")
      .map(_.select("td"))
      .filter(_.length == 3)
      .map(langElem => {
        CardLanguage(
          new URL(langElem(0).selectFirst("a").absUrl("href")),
          langElem(0).text().trim(),
          langElem(1).text().trim(),
          langElem(2).text().trim())
      })

  def parseCardList(doc: Document): List[URL] =
    doc.select("[id$='_cardTitle']").map(elem => new URL(elem.absUrl("href")))

  def parseCardInfo(elem: Element): MTGCardPageInfo = {
    val cardName = elem.selectFirst("[id$='nameRow'] div.value").text()
    val img = elem.selectFirst("img[src$='type=card']").absUrl("src")
    val cardText = elem.select("[id$='_textRow'] div.value").flatMap(flattenText)
    val flavorText = elem.select("[id$='_flavorRow'] div.value").flatMap(flattenText)

    MTGCardPageInfo(
    cardName = cardName,
    cardText = cardText,
    flavorText = flavorText,
    image = img
    )
  }

  val cardNumRegex = "([0-9]+)(a|b)?".r

  def parseCardCharacters(elem: Element): MTGCardCharacters = {
    val manaCost = elem.select("[id$='_manaRow'] div.value img").map(manaCostValue)
    val ptValue = Option(elem.selectFirst("[id$='_ptRow'] div.value"))
      .map(_.text().split("/").map(_.trim))
    val rarity: Rarity = elem.selectFirst("[id$='_rarityRow'] div.value").text() match {
      case "Land"   => LandCard
      case "Basic Land" => BasicLandCard
      case "Common"   => CommonCard
      case "Uncommon" => UncommonCard
      case "Rare"     => RareCard
      case "Mythic Rare"   => MythicRareCard
    }
    val (mainTypes, subTypes, legendary) = {
      val text = elem.selectFirst("[id$='_typeRow'] div.value").text().split("[^a-zA-Z ]").map(_.trim())
      val mainTypes = text(0).split(" +").toList
      val subTypes = if (text.length == 1) Nil else text(1).split(" +").toList
      (mainTypes.filterNot(_ == "Legendary"), subTypes, mainTypes.contains("Legendary"))
    }
    val cardNumber = elem.selectFirst("[id$='_numberRow'] div.value").text() match {
      case cardNumRegex(num, "a") => num.toShort -> Some(true)
      case cardNumRegex(num, "b") => num.toShort -> Some(false)
      case cardNumRegex(num, _) => num.toShort -> None
    }
    val expansion = Expansion(elem.selectFirst("[id$='_setRow'] div.value img"))
    val otherExpansions = elem.select("[id$='_otherSetsRow'] div.value img").toList match {
      case Nil => List(expansion)
      case all => all.map(Expansion.apply)
    }
    val artist = elem.selectFirst("[id$='_artistRow'] div.value").text()

    MTGCardCharacters(
      manaCost = manaCost,
      cmc = manaCost.map(_.totalCost).sum,
      mainTypes = mainTypes,
      subTypes = subTypes,
      legendary = legendary,
      power = ptValue.filter(_.length == 2).map(_(0)),
      toughness = ptValue.filter(_.length == 2).map(_(1)),
      loyality = ptValue.filter(_.length == 1).map(_(0).toShort),
      expansion = expansion,
      allSets = otherExpansions,
      rarity = rarity,
      cardNum = cardNumber._1,
      cardSide = cardNumber._2,
      artist = artist
    )
  }

  def getLanguagesLink(doc: Document): Try[URL] =
    Try {
      new URL(doc.selectFirst("a[id$='_LanguagesLink']").absUrl("href"))
    }

  implicit def elemsToList(elems: Elements): List[Element] = {
    (0 until elems.size()).map(i => elems.get(i)).toList
  }

  def flattenText(node: Element): List[String] = {
    node.childNodes().asScala.toList.flatMap { c => {
      c match {
        case tn: TextNode =>
          List(tn.text().trim)
        case el: Element if el.tagName().equals("img") =>
          List(s"{img:${el.attr("alt")}}")
        case el: Element if el.tagName().equals("i") =>
          List(s"{i}${el.text()}{/i}")
        case el: Element =>
          flattenText(el)
      }
    }}.filterNot(_.isEmpty)
  }

  def manaCostValue(elem: Element): Mana = {
    elem.attr("alt") match {
      case "White" => WhiteMana
      case "Blue"  => BlueMana
      case "Black" => BlackMana
      case "Red"   => RedMana
      case "Green" => GreenMana
      case "Variable Colorless" => GreenMana
      case colorless => ColorlessMana(colorless.toShort)
    }
  }
}

case class MTGCardPageInfo(
                          cardName: String,
                          cardText: List[String],
                          flavorText: List[String],
                          image: String
                          ) extends JsonElement {
  import JsonUtil._

  override def toString: String =
    "MTGCardPageInfo:" +
      List(
        cardName,
        cardText.mkString("[\"", "\"; \"", "\"]"),
        if (flavorText.isEmpty) "-" else flavorText.mkString("[\"", "\"; \"", "\"]"),
        image
      ).mkString("\n    ", "\n    ", "")

  override def toJson: String = List(
    "name" -> cardName,
    "text" -> cardText.map(_.toJsonElement).toJsonElement,
    "flavor" -> flavorText.map(_.toJsonElement).toJsonElement,
    "image" -> image.toJsonElement
  ).toJsonElement.toJson
}

object Expansion {

  val cardSetTitle     = "([^(]*) \\(.*".r
  val cardSetShortName = ".*[&?]set=([^&]*).*".r
  def apply(img: Element): Expansion = {
    val name = cardSetTitle.findFirstMatchIn(img.attr("title")).get.group(1)
    val shortName = cardSetShortName.findFirstMatchIn(img.attr("src")).get.group(1)
    Expansion(name, shortName)
  }
}
case class Expansion(name: String, shortName: String) extends JsonElement {
  import JsonUtil._

  override def toString: String = s"$name ($shortName)"

  override def toJson: String =
    List(
      "name" -> name.toJsonElement,
      "shortName" -> shortName.toJsonElement
    ).toJsonElement.toJson
}

case class MTGCardCharacters(
                              manaCost: List[Mana],
                              cmc: Short,
                              mainTypes: List[String],
                              subTypes: List[String],
                              legendary: Boolean,
                              power: Option[String],
                              toughness: Option[String],
                              loyality: Option[Short],
                              expansion: Expansion,
                              allSets: List[Expansion],
                              artist: String,
                              rarity: Rarity,
                              cardNum: Short,
                              cardSide: Option[Boolean],
                            ) extends JsonElement {

  import JsonUtil._

  override def toString: String =
  "MTGCardCharacters:" +
    List(
        manaCost.mkString(","),
        cmc,
      s"${if (legendary) "{legendary} " else ""}${mainTypes.mkString(" ")}" +
        s"${if (subTypes.isEmpty) "" else " - " + subTypes.mkString(":")}",
      s"${power.getOrElse("-")}/${toughness.getOrElse("-")} :: ${loyality.getOrElse("-")}",
      rarity,
      expansion,
      allSets.mkString(","),
      s"$cardNum${cardSide.map(s => if (s) "a" else "b").getOrElse("")}",
      s"by $artist"
    ).mkString("\n    ", "\n    ", "")

  override def toJson: String =
    List(
      "manaCost" -> manaCost.map(_.toString.toJsonElement).toJsonElement,
      "cmc" -> cmc.toString.toJsonElement,
      "legendary" -> legendary,
      "mainTypes" -> mainTypes.map(_.toJsonElement).toJsonElement,
      "subTypes" -> subTypes.map(_.toJsonElement).toJsonElement,
      "power" -> power.map(_.toJsonElement).toJsonElement,
      "toughness" -> toughness.map(_.toJsonElement).toJsonElement,
      "rarity" -> rarity.toString.toJsonElement,
      "expansion" -> expansion,
      "allSets" -> allSets.toJsonElement,
      "card" -> List(
          "number" ->  cardNum.toJsonElement,
          "side" -> cardSide.map(_.toJsonElement).toJsonElement
        ).toJsonElement,
      "artist" -> artist.toJsonElement
    ).toJsonElement.toJson
}

case class CardLanguage(
                         url: URL,
                         name: String,
                         language: String,
                         localLanguage: String,
                         cardInfo: JsonElement = JsonNone,
                         error: JsonElement = JsonNone
                       ) extends JsonElement {
  import JsonUtil.{JsonArray, JsonDictionary, JsonString}

  override def toJson: String = List(
    "url" -> url.toString.toJsonElement,
    "name" -> name.toJsonElement,
    "language" -> List(language,localLanguage).map(_.toJsonElement).toJsonElement,
    "card" -> cardInfo,
    "error" -> error
  ).toJsonElement.toJson
}

case class CardDescription(
                            oracle: MTGCardPageInfo,
                            characteristicts: MTGCardCharacters,
                            languages: Map[String, MTGCardPageInfo])
