package ru.finochenko.short.url

import java.util.concurrent.Executors

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.util.transactor.Transactor
import io.circe.generic.auto._
import io.circe.syntax._
import monix.eval.Task
import monix.execution.Scheduler
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import org.http4s.{Header, Method, Request, Uri}
import org.scalatest.{FlatSpec, Matchers}
import ru.finochenko.short.url.dao.UrlsDaoImpl
import ru.finochenko.short.url.handler.UrlsHandler
import ru.finochenko.short.url.model.{DataBaseConfig, RequestOriginalUrl, ResponseShortUrl}

import scala.concurrent.duration._

class IntegrationSpec extends FlatSpec with Matchers with ForAllTestContainer {

  implicit val scheduler: Scheduler = Scheduler(Executors.newSingleThreadExecutor())

  private val timeout = 10.second

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:9.6.16")

  private val buildRoutes = Task {
    val transactor = Transactor.fromDriverManager[Task](
      container.driverClassName, container.jdbcUrl, container.username, container.password
    )
    val dao = UrlsDaoImpl[Task](transactor)
    val handler = UrlsHandler[Task](dao)
    Routes[Task](handler).services.orNotFound
  }

  override def afterStart(): Unit = {
    val dbConfig = DataBaseConfig(container.driverClassName, container.jdbcUrl, container.username, container.password)
    val migrate = for {
      migration <- Migration[Task]
      _         <- migration.migrate(dbConfig)
    } yield ()
    migrate.runSyncUnsafe(timeout)
  }

  "POST /" should "return the same short url" in {
    val requestOriginalUrl = RequestOriginalUrl("http://some-url.org")
    val request = Request[Task](method = Method.POST).withEntity(requestOriginalUrl.asJson)
    val twoRequestsForShortUrl = for {
      routes            <- buildRoutes
      generatedShortUrl <- routes.run(request)
      oldShortUrl       <- routes.run(request)
    } yield generatedShortUrl -> oldShortUrl
    val firstAndSecondResponses = twoRequestsForShortUrl.runSyncUnsafe(timeout)
    val generatedShortUrl = firstAndSecondResponses._1.as[ResponseShortUrl].runSyncUnsafe(timeout)
    val oldShortUrl = firstAndSecondResponses._2.as[ResponseShortUrl].runSyncUnsafe(timeout)
    generatedShortUrl should be(oldShortUrl)
  }

  "GET /shortUrl" should "return original url in header Location" in {
    val requestOriginalUrl = RequestOriginalUrl("http://original-url.org")
    val request = Request[Task](method = Method.POST).withEntity(requestOriginalUrl.asJson)
    val response = for {
      routes            <- buildRoutes
      generatedShortUrl <- routes.run(request)
      responseShortUrl  <- generatedShortUrl.as[ResponseShortUrl]
      requestRedirect   = Request[Task](uri = Uri(path = s"/${responseShortUrl.shortUrl}"))
      redirectResponse  <- routes.run(requestRedirect)
    } yield  redirectResponse
    val expectedHeader = Header("Location", requestOriginalUrl.originalUrl)
    val headers = response.runSyncUnsafe(timeout).headers.toList
    headers should contain(expectedHeader)
  }

}
