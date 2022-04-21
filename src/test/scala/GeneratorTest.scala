import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

class GeneratorTest extends FreeSpec with ChiselScalatestTester {

  import spray.json._
  import DefaultJsonProtocol._
  import sys.process._

  // "python3 peripheralScript.py" !

  val file = scala.io.Source.fromFile((os.pwd.toString)+"//src//main//scala//config.json").mkString

  val fileToJson = file.parseJson.convertTo[Map[String, JsValue]]
  val config = fileToJson.map({case (a,b) => a -> {if (b == JsNumber(1)) true else false}})
  println(config)

  def getFile: Option[String] = {
    if (scalaTestContext.value.get.configMap.contains("memFile")) {
      Some(scalaTestContext.value.get.configMap("memFile").toString)
    } else {
      None
    }
  }
  "Generator Test" in {
    val programFile = getFile
    test(new Generator(programFile=programFile, GPIO = config("gpio"), UART = config("uart"), SPI = config("spi_flash"), TIMER = config("timer"), I2C = config("i2c"), TL = config("tl"), WB = config("wb"), M = config("m"))).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.setTimeout(0)
      c.clock.step(1000)
    }
  }
}