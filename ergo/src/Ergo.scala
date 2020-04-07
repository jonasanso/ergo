import scala.util.Random

import org.rogach.scallop._
object Ergo {

  def main(args: Array[String]): Unit = {

    for {
      agents <- Agents.fromConf(new Conf(args))
    } {
      println(s"""{"agents":${agents.count},"wealth":{"max":${agents.max},"min":${agents.min},"mean":${agents.mean},"median":${agents.median},"sum":${agents.sum}}}""")
      if (agents.count == 0) {
        Thread.sleep(3000)
        System.exit(0)
      } else
        Thread.sleep(100)
    }
  }
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val MULTIPLICATIVE = "multiplicative"
  val ADDITIVE = "additive"
  val numAgents: ScallopOption[Int] = opt[Int](name="num", short='n', descr = "Number of agents to simulate", default = Some(100))
  val initialAgentWealth: ScallopOption[BigDecimal] = opt[BigDecimal]( name="initial", short='i', descr = "Initial wealth of every agent", default = Some(BigDecimal(100)))
  val mode: ScallopOption[String] = choice(Seq(ADDITIVE, MULTIPLICATIVE), name="mode", short='m', descr = "Mode of execution, `" +ADDITIVE+ "` will add or subtract total amounts while `" + MULTIPLICATIVE + "` will add or subtract a percentage of the agent wealth", default = Some(ADDITIVE))
  val win: ScallopOption[Int] = opt[Int](name="win", short='w', descr = "Amount to increase in case of wining", default = Some(50))
  val lose: ScallopOption[Int] = opt[Int](name="lose", short='l', descr = "Amount to increase in case of loosing", default = Some(40))
  verify()
}

class Agents(values: List[BigDecimal], fn: (BigDecimal, Boolean) => BigDecimal) {
  import AgentMath._
  def count: Int = values.count(_ > Agents.minWealth)

  def max: BigDecimal = values.max

  def min: BigDecimal = values.filter(_ > Agents.minWealth).min

  def sum: BigDecimal = values.sum

  def mean: BigDecimal = values.sum / values.size

  def median: BigDecimal = getMedian[BigDecimal, BigDecimal](values)

  def lazylist: LazyList[Agents] = this #:: next

  private def next: LazyList[Agents] = {
    new Agents(values.map(nextValue), fn).lazylist
  }

  private def nextValue(v: BigDecimal): BigDecimal = {
    if (v > Agents.minWealth)
      round(fn(v, Random.nextBoolean)).max(Agents.minWealth)
    else v
  }
}


object Agents {
  val minWealth = 0

  def fromConf(conf: Conf): LazyList[Agents] = {
    val fn = if (conf.mode() == conf.MULTIPLICATIVE) multiply(conf.win(), conf.lose()) _ else add(conf.win(), conf.lose()) _
    new Agents(List.fill(conf.numAgents())(conf.initialAgentWealth()), fn).lazylist
  }

  private def add(plus: BigDecimal, minus: BigDecimal)(wealth: BigDecimal, win: Boolean): BigDecimal =
    if (win) wealth + plus else wealth - minus

  private def multiply(plus: Int, minus: Int)(wealth: BigDecimal, wih: Boolean): BigDecimal =
    if (wih) wealth + (wealth * plus / 100) else wealth - (wealth * minus / 100)

}

object AgentMath {
  def round(v: BigDecimal): BigDecimal = BigDecimal(truncate(v.toDouble))

  private def truncate(n: Double): Double = {
    math.floor(n * 100) / 100
  }

  def getMedian[T: Ordering, F](seq: Seq[T])(implicit conv: T => F, f: Fractional[F]): F = {
    val sortedSeq = seq.sorted
    if (seq.size % 2 == 1) sortedSeq(sortedSeq.size / 2) else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      import f._
      (conv(up.last) + conv(down.head)) / fromInt(2)
    }
  }
}
