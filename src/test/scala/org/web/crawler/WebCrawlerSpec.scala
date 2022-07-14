package org.web.crawler

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.language.postfixOps

class WebCrawlerSpec
  extends org.scalatest.freespec.AsyncFreeSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar with JsonSupport{

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second.dilated(system))

  "Web crawler" - {
    "for urls" - {
      "should fetch data for correct url" in {
        val request = Post("/crawl",
          HttpEntity(ContentTypes.`application/json`,
            """
              |{
              |"urls": ["https://google.com"]
              |}
              |""".stripMargin))
        request ~> Webcrawler.crawl ~> check {
          status shouldBe StatusCodes.OK
          val response = entityAs[Response]
          response.result(0).url shouldBe "https://google.com"
          val data = response.result(0).data
          data.contains("<HTML>") shouldBe true
          data.contains("</HTML>") shouldBe true
        }
      }

      "should add error for incorrect url" in {
        val request = Post("/crawl",
          HttpEntity(ContentTypes.`application/json`,
            """
              |{
              |"urls": ["aaaaaa"]
              |}
              |""".stripMargin))
        request ~> Webcrawler.crawl ~> check {
          status shouldBe StatusCodes.OK
          val response = entityAs[Response]
          response.result.length shouldBe 0
          response.error(0) shouldBe "aaaaaa error out because Cannot determine request scheme and target endpoint as HttpMethod(GET) request to aaaaaa doesn't have an absolute URI"
        }
      }

      "should add error for unknown url" in {


          val request = Post("/crawl",
            HttpEntity(ContentTypes.`application/json`,
              """
                |{
                |"urls": ["http://foogle.in"]
                |}
                |""".stripMargin))
          request ~> Webcrawler.crawl ~> check {
            status shouldBe StatusCodes.OK
            val response = entityAs[Response]
              response.error(0) shouldBe "http://foogle.in error out because Tcp command [Connect(foogle.in:80,None,List(),Some(15 seconds),true)] failed because of java.net.UnknownHostException: foogle.in"

          }
      }
    }

  }
}
