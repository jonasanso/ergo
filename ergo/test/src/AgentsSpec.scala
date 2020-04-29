import org.scalatest._
import org.scalatest.matchers.must.Matchers

class AgentsSpec extends FlatSpec with Matchers {
  private val alwaysLose: () => Boolean = () => false
  private val doubleOrLoose90percent = Agents.multiply(100, 90) _
  private val win100lose40 = Agents.add(100, 40) _

  implicit class AgentTestOps(a: Agents) {
    def maxes: LazyList[BigDecimal] = a.lazylist.map(_.max)
  } 
  
  it should "lose at least 1" in {
    new Agents(Vector(1d), doubleOrLoose90percent, alwaysLose).maxes must be(LazyList(1d, 0d))
  }

  it should "not play if it has less than 1" in {
    new Agents(Vector(0.1d), doubleOrLoose90percent, alwaysLose).maxes must be(LazyList(0.1d))
  }

  "An agent" should "recalculate until zero wealth is reached" in {
    new Agents(Vector(100d), doubleOrLoose90percent, alwaysLose).maxes must be(LazyList(100d, 10d, 1d, 0d))
    new Agents(Vector(50d), doubleOrLoose90percent, alwaysLose).maxes must be(LazyList(50d, 5d, 0d))
    new Agents(Vector(100d), win100lose40, alwaysLose).maxes must be(LazyList(100d, 60d, 20d, 0d))
  }

  "An agent" should "recalculate even with max values" in {
    new Agents(Vector(BigDecimal(Double.MaxValue)), doubleOrLoose90percent, alwaysLose).maxes.take(2) must be(LazyList(100d, 10d, 1d, 0d))
  }
}