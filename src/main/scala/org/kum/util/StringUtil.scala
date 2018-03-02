package org.kum.util

object StringUtil {

  def split2(str: String, regex: String, limit: Int = 2): (String, Option[String]) = {
    str.split(regex, limit).toList match {
      case head :: tail =>
        head -> tail.headOption
    }
  }
}
