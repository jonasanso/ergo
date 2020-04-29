import org.scalatest._
import org.scalatest.matchers.must.Matchers
import BasicMath._

class BasicMathSpec extends FlatSpec with Matchers {

  "round" should "work with big numbers" in {
    round(BigDecimal(Double.MaxValue)) must be(BigDecimal(Double.MaxValue))
  }
}
