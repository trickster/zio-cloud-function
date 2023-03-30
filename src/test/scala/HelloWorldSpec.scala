import zio.*
import zio.test.Assertion.*
import zio.test.*
import zio.test.TestAspect.*
import java.io.IOException
import zio.stream.*

object HelloWorld {
  def sayHello: ZIO[Any, IOException, Unit] =
    Console.printLine("Hello, World!")
}

case class Counter(value: Ref[Int]) {
  def inc: UIO[Unit] = value.update(_ + 1)
  def get: UIO[Int]  = value.get
}

object Counter {
  val layer =
    ZLayer.scoped(
      ZIO.acquireRelease(
        Ref.make(0).map(Counter(_)) <* ZIO.debug("Counter initialized!")
      )(c => c.get.debug("Number of tests executed"))
    )

  def inc = ZIO.service[Counter].flatMap(_.inc)
}

trait SharedCounterSpec extends ZIOSpec[Counter] {
  override def bootstrap: ZLayer[Any, Nothing, Counter] = Counter.layer
}

object HelloWorldSpec extends SharedCounterSpec {

  override def spec =
    suite("HelloWorldSpec")(
      test("updating ref") {
        for {
          r <- Ref.make(0)
          _ <- r.update(_ + 2)
          v <- r.get
        } yield assertTrue(v == 2)
      } @@ TestAspect.after(Counter.inc),
      test("sayHello displays correctly") {
        for {
          _      <- HelloWorld.sayHello
          output <- TestConsole.output
        } yield assertTrue(output == Vector("Hello, World!\n"))
      } @@ TestAspect.after(Counter.inc),
      test("test1") {
        assertTrue(true)
      } @@ TestAspect.after(Counter.inc),
      // You can run these test using `test:runMain HelloWorldSpec -tags DB`
      test("some http tests") {
        import ZIOHttpCloudFunctionSpanner.*

        val getFromDB = for {
          db <- ZIO.service[DatabaseService]
          res = db.query("""
              SELECT id1, firstname from users where id1 = "123123"
            """)
          rr <- res >>> ZSink.take[String](1)
        } yield rr.headOption

        val databaseClientToLayer = ZLayer.scoped(
          ZIO.attempt(spannerService.getDatabaseClient(databaseId))
        )

        val result = getFromDB.provide(DatabaseService.live, databaseClientToLayer).debug("Data from DB is: ")
        result.flatMap(r => assertTrue(r == Some("Siva")))
        // assertTrue(true)
      } @@ TestAspect.after(Counter.inc) @@ TestAspect.timed @@ TestAspect.tag("DB"),
      test("refs") {
        case class Counter(value: Ref[Int]) {
          def inc: UIO[Unit] = value.update(_ + 1)
          def dec: UIO[Unit] = value.update(_ - 1)
          def get: UIO[Int]  = value.get
        }

        object Counter {
          def make: UIO[Counter] = Ref.make(0).map(Counter(_))
        }

        import scala.io.AnsiColor.*
        for {
          c <- Counter.make
          _ <- c.inc <&> c.inc <&> c.dec <&> c.inc
          v <- c.get.debug(s"${REVERSED}${MAGENTA}The value of ref in parallel is ${RESET}")
        } yield assertTrue(2 == v)

      } @@ TestAspect.tag("refs")
    )

}

object Spec2 extends SharedCounterSpec {
  override def spec =
    test("another spec") {
      val zs     = ZStream.iterate(1)(_ + 1) // .takeWhile(_ < 6)
      val sink   = ZSink.take[Int](5)
      val result = zs.run(sink).debug("what's in it")

      result.flatMap(r => assertTrue(r == Chunk(1, 2, 3, 4, 5)))
      // assertTrue(result == 10).debug("once done")
    } @@ TestAspect.after(Counter.inc) @@ TestAspect.tag(
      "streams"
    ) // @@ repeat(Schedule.recurs(3) && Schedule.spaced(10.millis))
}
