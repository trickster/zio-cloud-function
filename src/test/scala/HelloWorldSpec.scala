import zio.*
import zio.test.Assertion.*
import zio.test.*
import zio.test.TestAspect.*
import java.io.IOException
import zio.stream.*

object HelloWorldSpec extends ZIOSpecDefault {
  override def spec =
    suite("HelloWorldSpec")(
      test("updating ref") {
        for {
          r <- Ref.make(0)
          _ <- r.update(_ + 2)
          v <- r.get
        } yield assertTrue(v == 2)
      }
    )
}
