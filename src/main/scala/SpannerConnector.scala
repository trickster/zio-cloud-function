import zio.*
import zio.stream.*
import com.google.cloud.spanner.*

trait SpannerConnector:
  def query(q: String): ZStream[Any, Throwable, String]

case class DatabaseService(db: DatabaseClient) extends SpannerConnector {

  // Can use ZIO instead of ZStream
  override def query(q: String): ZStream[Any, Throwable, String] = {
    val statement = Statement.newBuilder(q).build()
    val rs        = ZIO.attempt(db.singleUse.executeQuery(statement))
    ZStream.acquireReleaseWith(rs)(conn => ZIO.succeed(conn.close)).flatMap { is =>
      var chunked = Chunk.empty[String]
      while (is.next) {
        chunked = chunked :+ is.getString("COLNAME")
      }
      ZStream.fromIterable(chunked)
    }
  }
}

object DatabaseService:
  lazy val live = ZLayer.fromZIO(ZIO.service[DatabaseClient].map(DatabaseService(_)))
