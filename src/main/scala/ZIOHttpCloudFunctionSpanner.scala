import zio.*
import zio.http.*
import zio.stream.*
import scala.jdk.CollectionConverters.*
import zio.http.model.*
import zio.http.model.Headers.Header
import com.google.cloud.functions.*
import com.google.cloud.spanner.*

class ZIOHttpCloudFunctionSpanner extends HttpFunction {

  import ZIOHttpCloudFunctionSpanner.*

  def getFromDb() =
    for {
      db <- ZIO.service[DatabaseService]
      res = db.query("""SELECT "p1" as firstname""")
      rr <- res >>> ZSink.take[String](1)
    } yield rr.headOption

  private val databaseClientToLayer = ZLayer.scoped(
    ZIO.attempt(spannerService.getDatabaseClient(databaseId))
  )

  val helloSpannerApp: HttpApp[Any, Nothing] = Http.collectZIO[Request] { case Method.GET -> !! =>
    getFromDb()
      .map(r =>
        r match
          case None        => Response.text("No data")
          case Some(value) => Response.text(s"Firstname is $value")
      )
      .provide(DatabaseService.live, databaseClientToLayer)
      .catchAll(r => ZIO.succeed(Response.text("Database error")))
  }

  private def requestBody(request: HttpRequest): Body =
    val zs = ZStream.fromInputStream(request.getInputStream)
    Body.fromStream(zs)

  private def requestHeaders(request: HttpRequest): Headers =
    Headers(
      request.getHeaders.asScala.view.mapValues(_.asScala.mkString(",")).map { case (k, v) => Header(k, v) }
    )

  private def fromRequest(request: HttpRequest): ZIO[Any, Throwable, Request] =
    val method  = Method.fromString(request.getMethod)
    val body    = requestBody(request)
    val headers = requestHeaders(request)

    val url = URL.fromString(request.getUri)
    url match
      case Left(_)      => ZIO.fail(new Throwable("url not found"))
      case Right(value) =>
        ZIO.succeed(
          Request(
            body = body,
            headers = headers,
            method = method,
            url = value,
            version = Version.Http_1_1,
            remoteAddress = None
          )
        )

  private def toResponse(httpResponse: HttpResponse, response: Response): ZIO[Any, Throwable, Unit] =
    // writing response
    // convert from zio response to HttpResponse
    for {
      _  <- ZIO.blocking {
              httpResponse.setStatusCode(response.status.code)
              response.headers.foreach(h => httpResponse.appendHeader(h.key.toString, h.value.toString))
              ZIO.unit
            }
      fos = ZSink.fromOutputStream(httpResponse.getOutputStream)
      _  <- response.body.asStream.run(fos)
    } yield ()

  extension [A](io: ZIO[Any, Any, A])
    def run: A = Unsafe.unsafely {
      Runtime.default.unsafe.run(io).getOrThrowFiberFailure()
    }

  override def service(request: HttpRequest, response: HttpResponse): Unit =
    val service = for {
      req         <- fromRequest(request)
      appResponse <- helloSpannerApp(req)
      _           <- toResponse(response, appResponse)
    } yield ()

    service.run
}

object ZIOHttpCloudFunctionSpanner {
  private val SPANNER_PROJECT_ID  = "PROJECT"
  private val SPANNER_INSTANCE_ID = "INSTANCE"
  private val SPANNER_DATABASE_ID = "DB"

  // this lazy initialization is necessary for Cloud function performance
  // A single client is used for multiple function invocations
  lazy val spannerService = SpannerOptions.newBuilder.build.getService

  val databaseId = DatabaseId.of(SPANNER_PROJECT_ID, SPANNER_INSTANCE_ID, SPANNER_DATABASE_ID)
}
