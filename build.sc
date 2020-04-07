import mill._
import scalalib._
import ammonite.ops._
import ammonite.ops.Shellout
import sys.process._

import mill.util.Ctx

object ergo extends ScalaModule with GraalVM {
  def scalaVersion = "2.13.1"
  override def ivyDeps = Agg(
    ivy"org.rogach::scallop:3.4.0"
  )
  override def mainClass = Some("Ergo")
  def native =  T {
    buildNative(assembly(), "ergo")
  }
}

trait GraalVM {
  def buildNative(jar: PathRef, name: String)(implicit ctx: Ctx.Dest): Unit = {
    val jarName = s"$name.jar"
    cp(jar.path, ctx.dest / jarName)
    Shellout.executeStream(ctx.dest, Command(Vector("native-image", "-jar", jarName), Map.empty, Shellout.executeStream)) match{
      case CommandResult(0, s) => s.foreach(_.left.foreach(print))
      case c@CommandResult(e, s) =>
        throw ShelloutException(c)
    }
  }
}
