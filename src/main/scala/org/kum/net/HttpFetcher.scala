package org.kum.net

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URLEncoder
import org.kum.util.StringUtil

import scalaj.http._
import scala.util.{Failure, Success, Try}

class HttpFetcher(delay: Long = 1000 /* one second */) {

  var nextTimestamp = System.currentTimeMillis()

  def fetch(url: String, headers: Map[String, String]): Try[InputStream] = {
    import StringUtil.split2

    val sleepTime = nextTimestamp - System.currentTimeMillis()
    if (sleepTime > 0) {
      Thread.sleep(sleepTime)
    }
    val (page, paramsOpt) = split2(url, "[?]")
    val resopnse = Http(url)
      .headers(headers)
      .timeout(10000, 20000)
      .asBytes

    nextTimestamp = System.currentTimeMillis() + delay
    response.code match {
      case 200 => Success(new ByteArrayInputStream(response.body))
      case 302 => Failure(new IllegalArgumentException(response.body + "/" + response))
      case _ => Failure(new IllegalArgumentException(s"Return code ${response.code} for $url"))
    }
  }
}
