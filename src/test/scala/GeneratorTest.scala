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
  val oneZero = fileToJson.map({case (a,b) => a -> {if (b == JsNumber(1)) true else false}})

  val baseAddr = BaseAddr()
  val mask     = Mask()

  val configs:Map[Any, Map[Any,Any]] = Map("DCCM" -> Map("id" -> 0, "is" -> true           , "baseAddr" -> baseAddr.DCCM, "mask" -> mask.DCCM),
                                           "GPIO" -> Map("id" -> 1, "is" -> oneZero("gpio"), "baseAddr" -> baseAddr.GPIO, "mask" -> mask.GPIO),
                                           "M"    -> Map("is" -> oneZero("m")),
                                           "TL"   -> Map("is" -> oneZero("tl")),
                                           "WB"   -> Map("is" -> oneZero("wb")))

  def getFile: Option[String] = {
    if (scalaTestContext.value.get.configMap.contains("memFile")) {
      Some(scalaTestContext.value.get.configMap("memFile").toString)
    } else {
      None
    }
  }

  "Generator Test" in {
    val programFile = getFile
    test(new GeneratorWB(programFile=programFile, configs)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.setTimeout(0)
      c.clock.step(1000)
    }
  }
}