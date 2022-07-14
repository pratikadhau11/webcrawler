package org.web.crawler

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import spray.json.{DefaultJsonProtocol, _}

class WebCrawlerSpec
  extends org.scalatest.freespec.AsyncFreeSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar with JsonSupport{

  "Web crawler" - {
    "for urls" - {
      "should fetch data" in {

        val request = Post("/crawl",
          HttpEntity(ContentTypes.`application/json`,
            """
              |{
              |"urls": ["https://google.com", "https://github.com", "aaaa"]
              |}
              |""".stripMargin))
        request ~> Webcrawler.crawl ~> check {
          status shouldBe StatusCodes.OK
          val response1 = entityAs[Response]
          response1.result(0).url shouldBe "https://google.com"
          response1.result(1).url shouldBe "https://github.com"
          response1.error(0) shouldBe "aaaa error out because Cannot determine request scheme and target endpoint as HttpMethod(GET) request to aaaa doesn't have an absolute URI"

        }
      }

    }
  }

}
