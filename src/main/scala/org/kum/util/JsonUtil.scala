package org.kum.util

object JsonUtil {

  implicit class JsonString(str: String) {
    def jsonEncode(): String = str.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"")
    def toJsonElement: JsonElement = new JsonElement {
      override def toJson: String = s""""${str.jsonEncode()}""""
    }
  }

  implicit class JsonOption(obj: Option[JsonElement]) {
    def toJsonElement: JsonElement =
      obj match {
        case Some(e) => e
        case None => JsonNone
      }
  }

  implicit class JsonBooleon(obj: Boolean) {
    def toJsonElement: JsonElement = new JsonElement {
      override def toJson: String = obj.toString
    }
  }

  implicit class JsonInt(obj: Int) {
    def toJsonElement: JsonElement = new JsonElement {
      override def toJson: String = obj.toString
    }
  }

  implicit class JsonDictionary(obj: Iterable[(String, Any)]) {
    def toJsonElement: JsonElement = new JsonElement {
      override def toJson: String =
        obj
          .filter {
            case (_, JsonNone) => false
            case _ => true
          }
          .map {
            case (name, value: JsonElement) =>
              name -> value
            case (name, value: String) =>
              name -> value.toJsonElement
            case (name, value: Boolean) =>
              name -> new JsonElement { override def toJson: String = value.toString }
          }
          .map {
            case (name, value: JsonElement) => s""""${name.jsonEncode()}":${value.toJson}"""
          }
          .mkString("{", ",", "}")
    }

  }

  implicit class JsonArray(arr: Iterable[JsonElement]) {
    def toJsonElement: JsonElement = toJsonElement(false)

    def toJsonElement(newLine: Boolean): JsonElement = new JsonElement {
      private val sep = if (newLine) ",\n" else ","
      override def toJson: String = arr.map(_.toJson).mkString("[", sep, "]")
    }
  }
}

trait JsonElement {
  def toJson: String
}

case object JsonNone extends JsonElement {
  override def toJson: String = "null"
}
