import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec
import GeneratorPuart.Generator
import scala.io.Source

class GeneratorPuartTest extends FreeSpec with ChiselScalatestTester {

  import spray.json._
  import DefaultJsonProtocol._
  import sys.process._

  val file = scala.io.Source.fromFile((os.pwd.toString)+"//src//main//scala//config.json").mkString

  val fileToJson = file.parseJson.convertTo[Map[String, JsValue]]
  val config = fileToJson.map({case (a,b) => a -> {if (b == JsNumber(1)) true else false}})
//   println(config)

  def getFile: Option[String] = {
    if (scalaTestContext.value.get.configMap.contains("memFile")) {
      Some(scalaTestContext.value.get.configMap("memFile").toString)
    } else {
      None
    }
  }
  "P U A R T" in {
    val programFile = getFile
    test(new GeneratorPuart.Generator(programFile=programFile, GPIO = config("gpio"), UART = config("uart"), SPI = config("spi_flash"), TIMER = config("timer"), I2C = config("i2c"), TL = config("tl"), WB = config("wb"), M = config("m")))/*.withAnnotations(Seq(VerilatorBackendAnnotation))*/ {c =>
    c.io.CLK_PER_BIT.poke(4.U)


    val bufferedSource =  Source.fromFile("/home/talha/abc.txt")
    val fileData = bufferedSource.getLines.toArray
    val insts = for (i <- fileData) yield java.lang.Long.parseLong(i.substring(2), 16)
    bufferedSource.close

    c.io.rx_i.poke(1.U)


    c.clock.step(10)

//   val insts = Array(0x400007B7,0x400007B7, 0x00078793,0x00800413,0x0087A223,0x0047A283, 0x00000fff)
    for (inst <- insts) {
        // val inst = 0xf0f0f0f0
        val half_byte1 = inst & 0x0f // 3
        val half_byte2 = (inst & 0xf0) >> 4 // 1
        val byte1 = (half_byte2 << 4) | half_byte1 // 0x13

        val half_byte3 = (inst & 0xf00) >> 8  // 1
        val half_byte4 = (inst & 0xf000) >> 12  // 0
        val byte2 = (half_byte4 << 4) | half_byte3  // 0x01

        val half_byte5 = (inst & 0xf0000) >> 16 // 0
        val half_byte6 = (inst & 0xf00000) >> 20  // 2
        val byte3 = (half_byte6 << 4) | half_byte5  // 0x20

        val half_byte7 = (inst & 0xf000000) >> 24 // 0
        val half_byte8 = (inst & 0xf0000000) >> 28  // 0
        val byte4 = (half_byte8 << 4) | half_byte7  // 0x00

        //printf("The instruction is %x".format(byte4))
        pokeUart(byte1.toInt)
        pokeUart(byte2.toInt)
        pokeUart(byte3.toInt)
        pokeUart(byte4.toInt)
    }

        def pokeUart(value: Int): Unit = {

            // start bit
            // poke(c.io.rx_i, 0)
            c.io.rx_i.poke(0.U)
            // step(4)
            c.clock.step(4)
            // 8 data bits
            for (i <- 0 until 8) {
            // poke(c.io.rx_i, (value >> i) & 0x01)
            c.io.rx_i.poke(((value >> i) & 0x01).U)
            // step(4)
            c.clock.step(4)
            }
            // stop bit
            // poke(c.io.rx_i, 1)
            c.io.rx_i.poke(1.U)
            // step(4)
            c.clock.step(4)
        }
      c.clock.step(200)
      c.clock.setTimeout(0)
      c.clock.step(1000)
    }
  }
}