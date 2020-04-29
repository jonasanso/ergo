import scala.util.Random
import org.rogach.scallop._
import BasicMath._
object Ergo {

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val agents: LazyList[Agents] = Agents.fromConf(conf)
    if (conf.games.isDefined)
      runSummary(agents)
    else
      runGraphics(agents)
  }

  private def runSummary(agents: LazyList[Agents]): Unit = {
    val init = agents.head
    val originalWealth = init.max
    val end = agents.last

    val percentageAlive = percentage(end.count, init.count)
    val percentageNetWinners = percentage(end.wealthAbove(originalWealth).length, init.count)

    val formatter = java.text.NumberFormat.getCurrencyInstance
    def format(d: BigDecimal): String = formatter.format(d)
    
    println(
      s"""Initial wealth ${format(init.sum)} ${init.count} agents with ${format(init.max)} each
          |Survivors      $percentageAlive%
          |Winners        $percentageNetWinners%
          |Poorest wealth ${format(end.min)}
          |Average wealth ${format(end.mean)}
          |Median  wealth ${format(end.median)}
          |Richest wealth ${format(end.max)} or ${percentage(end.max, end.sum)}% of total
          |Final wealth   ${format(end.sum)}
          |Total games    ${agents.length}""".stripMargin)
  }

  private def runGraphics(agents: LazyList[Agents]): Unit = {
    for {
      a <- agents
    } {
      println(a.toJson)
      Thread.sleep(100)
    }

    // Give some timo to jplot to render the results
    Thread.sleep(3000)
  }
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val MULTIPLICATIVE = "multiplicative"
  val ADDITIVE = "additive"
  val numAgents: ScallopOption[Int] = opt[Int](name = "num", short = 'n', descr = "Number of agents to simulate", default = Some(1000))
  val games: ScallopOption[Int] = opt[Int](name = "games", short = 'g', descr = "Number of games to simulate")
  val initialAgentWealth: ScallopOption[BigDecimal] = opt[BigDecimal](name = "initial", short = 'i', descr = "Initial wealth of every agent", default = Some(BigDecimal(100)))
  val mode: ScallopOption[String] = choice(Seq(ADDITIVE, MULTIPLICATIVE), name = "mode", short = 'm', descr = "Mode of execution, `" + ADDITIVE + "` will add or subtract total amounts while `" + MULTIPLICATIVE + "` will add or subtract a percentage of the agent wealth", default = Some(ADDITIVE))
  val win: ScallopOption[Int] = opt[Int](name = "win", short = 'w', descr = "Amount to increase in case of wining", default = Some(50))
  val lose: ScallopOption[Int] = opt[Int](name = "lose", short = 'l', descr = "Amount to increase in case of loosing", default = Some(40))

  verify()
}

class Agents(values: Vector[BigDecimal], fn: (BigDecimal, Boolean) => BigDecimal, next: () => Boolean = Random.nextBoolean) {

  import AgentMath._

  private val alive: Vector[BigDecimal] = wealthAbove(Agents.minWealth)

  def wealthAbove(min: BigDecimal): Vector[BigDecimal] = values.filter(_ >= min)

  val count: Int = alive.length

  def lazylist: LazyList[Agents] = {
    if (count == 0) this #:: LazyList.empty
    else this #:: new Agents(values.map(nextValue), fn, next).lazylist
  }

  private def nextValue(v: BigDecimal): BigDecimal = {
    if (v < Agents.minWealth) v
    else {
      val res = round(fn(v, next())).max(0d)
      if (res < Agents.minWealth) 0 else res
    }
  }

  val max: BigDecimal = values.max

  val min: BigDecimal = alive.minOption.getOrElse(0)

  val sum: BigDecimal = values.sum

  val mean: BigDecimal = sum / values.length

  def median: BigDecimal = getMedian(values)

  val toJson: String =
    s"""{"agents":$count,""" +
      s""""wealth":{"max":$max,"min":$min,"mean":$mean,"median":$median,"sum":$sum,""" +
      allPercentiles(values).map { case (k, v) => s""""p$k":$v""" }.mkString(",") +
      """}}"""
}


object Agents {
  val minWealth = 1

  def fromConf(conf: Conf): LazyList[Agents] = {
    val fn = if (conf.mode() == conf.MULTIPLICATIVE) multiply(conf.win(), conf.lose()) _ else add(conf.win(), conf.lose()) _
    val lazylist = new Agents(Vector.fill(conf.numAgents())(conf.initialAgentWealth()), fn).lazylist
    conf.games.toOption match {
      case Some(g) => lazylist.take(g)
      case None => lazylist
    }
  }

  def add(plus: BigDecimal, minus: BigDecimal)(wealth: BigDecimal, win: Boolean): BigDecimal =
    if (win) wealth + plus else wealth - minus

  def multiply(plus: Int, minus: Int)(wealth: BigDecimal, wih: Boolean): BigDecimal =
    if (wih) wealth + (wealth * plus / 100) else wealth - (wealth * minus / 100).max(minWealth)

}

object BasicMath {

  def percentage(a: Int, b: Int): BigDecimal = percentage(BigDecimal(a), BigDecimal(b))
  def percentage(a: BigDecimal, b: BigDecimal): BigDecimal = if (b == 0) 0d else round(a * 100 / b)

  def round(v: BigDecimal): BigDecimal = BigDecimal(truncate(v.toDouble))

  private def truncate(n: Double): Double = {
    math.floor(n * 100) / 100 match {
      case Double.PositiveInfinity => Double.MaxValue
      case x => x
    }
  }

}

object AgentMath {
  def getMedian[T: Ordering, F](seq: Seq[T])(implicit conv: T => F, f: Fractional[F]): F = {
    val sortedSeq = seq.sorted
    if (seq.size % 2 == 1) sortedSeq(sortedSeq.size / 2) else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      import f._
      (conv(up.last) + conv(down.head)) / fromInt(2)
    }
  }

  def allPercentiles[T: Ordering](seq: Seq[T]): Map[Int, T] = {
    val sorted = seq.sorted

    def percentile(perc: Int)(seq: Seq[T]) = {
      val k = math.ceil((seq.length - 1) * (perc / 100.0)).toInt
      perc -> sorted(k)
    }

    def permile(mille: Int)(seq: Seq[T]) = {
      val k = math.ceil((seq.length - 1) * (mille / 1000.0)).toInt
      mille -> sorted(k)
    }

    Vector(
      percentile(10) _,
      percentile(20) _,
      percentile(30) _,
      percentile(40) _,
      percentile(50) _,
      percentile(60) _,
      percentile(70) _,
      percentile(80) _,
      percentile(90) _,
      percentile(95) _,
      percentile(99) _,
      permile(995) _,
      permile(999) _
    ).map(p => p(sorted)).toMap
  }

}
