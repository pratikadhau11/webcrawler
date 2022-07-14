package org.web.crawler

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{as, complete, entity, extractActorSystem, extractExecutionContext, extractMaterializer, path, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.{DefaultJsonProtocol, _}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

object Webcrawler extends JsonSupport {
  val timeOut = 300.millis
  val NOT_FOUND = 404
  val crawl: Route = path("crawl") {
    post {
      extractActorSystem { implicit actorSystem =>
        extractExecutionContext { implicit ec =>
          extractMaterializer { implicit mt =>
            entity(as[Request]) { req: Request =>

              val rs: Seq[Future[Either[String, URLData]]] = req.urls.map { url =>


                    Http().singleRequest(HttpRequest(uri = url)).flatMap { (response: HttpResponse) =>

                          response.entity.toStrict(timeOut).flatMap(
                            httpEntity => {
                              val eventualString = Unmarshal(httpEntity).to[String]
                              eventualString.map(page => Right(URLData(url, page )))
                            }
                          )

                    } recover { case e : Throwable =>
                      Left(s"$url error out because ${e.getMessage}")
                    }

                }


              val eventualEitherURLsData: Future[Seq[Either[String,URLData]]] = Future.sequence(rs)
              complete(eventualEitherURLsData.map(eithers => {
                val uRLDataList = eithers.filter(_.isRight)
                val errors: Seq[String] = eithers.filter(_.isLeft).map(_.swap).flatMap(a => a.toSeq)
                val urlDataList: Seq[URLData] = uRLDataList.flatMap(a => a.toSeq)
                Response(urlDataList, errors)
              }))
            }
          }
        }

      }
    }
  }

}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat: RootJsonFormat[Request] = jsonFormat1(Request)
  implicit val urlDataFormat: RootJsonFormat[URLData] = jsonFormat2(URLData)
  implicit val responseFormat: RootJsonFormat[Response] = jsonFormat2(Response)
}

case class Request(urls: immutable.Seq[String])

case class URLData(url: String, data: String)

case class Response(result: Seq[URLData], error: Seq[String])



