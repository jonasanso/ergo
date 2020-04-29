import ammonite.ops._
import mill._
import mill.scalalib._
import mill.util.Ctx

import $ivy.`com.lihaoyi::mill-contrib-bloop:0.6.1`

object ergo extends ScalaModule with GraalVM {
  def scalaVersion = "2.13.1"
  override def ivyDeps = Agg(
    ivy"org.rogach::scallop:3.4.0"
  )
  override def mainClass = Some("Ergo")
  def native =  T {
    buildNative(assembly(), "ergo")
  }

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.1.1")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

}

trait GraalVM {
  val options =  Vector(
    "--verbose",
    "--no-server",
    "--no-fallback",
    //"--static", //requires static libc and zlib
    "--report-unsupported-elements-at-runtime",
    "-H:+ReportExceptionStackTraces",
    "-H:+ReportUnsupportedElementsAtRuntime",
    "-H:+TraceClassInitialization",
    "-H:+PrintClassInitialization",
    "--initialize-at-build-time=scala.runtime.Statics$VM",
    "--initialize-at-run-time=java.lang.Math$RandomNumberGeneratorHolder",
  )

  def buildNative(jar: PathRef, name: String)(implicit ctx: Ctx.Dest): Unit = {
    val jarName = s"$name.jar"
    cp(jar.path, ctx.dest / jarName)
    Shellout.executeStream(ctx.dest, Command(Vector("native-image", "-jar", jarName) ++ options, Map.empty, Shellout.executeStream)) match{
      case CommandResult(0, s) => s.foreach(_.left.foreach(print))
      case c@CommandResult(e, s) =>
        throw ShelloutException(c)
    }
  }
}
