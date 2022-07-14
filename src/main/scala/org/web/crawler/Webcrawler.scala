package org.web.crawler

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, _}

import scala.collection.immutable

object Webcrawler extends JsonSupport  {
  val crawl: Route = path("crawl") {
    post {
      entity(as[Request]) { req: Request =>
        val result = req.urls.map(url => URLData(url, s"<html>$url</html>"))
        complete(Response(result, Seq.empty))
      }

      }
    }

}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat: RootJsonFormat[Request] = jsonFormat1(Request)
  implicit val itemFormat: RootJsonFormat[URLData] = jsonFormat2(URLData)
  implicit val orderFormat: RootJsonFormat[Response] = jsonFormat2(Response)
}

case class Request(urls: immutable.Seq[String])

case class URLData(url: String, data: String)

case class Response(result: Seq[URLData], error: Seq[String])



