import chisel3._
import nucleusrv.components.Core
import caravan.bus.common.{AddressMap, BusDecoder, Switch1toN, Peripherals}

import caravan.bus.tilelink.{TLRequest, TLResponse, TilelinkConfig, TilelinkDevice, TilelinkError, TilelinkHost, TilelinkMaster, TilelinkSlave, TilelinkCDevice}
import caravan.bus.wishbone.{WBRequest, WBResponse, WishboneConfig, WishboneDevice, WishboneHost, WishboneMaster, WishboneSlave}
import caravan.bus.wishbone.{WishboneErr}
import chisel3.experimental.Analog
import chisel3.stage.ChiselStage
import jigsaw.fpga.boards.artyA7._
import jigsaw.rams.fpga.BlockRam
import jigsaw.peripherals.gpio._
import jigsaw.peripherals.spiflash._
import jigsaw.peripherals.UART._
import jigsaw.peripherals.timer._
import jigsaw.peripherals.i2c._
import ccache.caches.DMCache

class GeneratorWB(programFile: Option[String],
                 configs:Map[Any, Map[Any, Any]]) extends Module {
  val io = IO(new Bundle {
    val gpio_o = Output(UInt(8.W))
    val gpio_en_o = Output(UInt(8.W))
    val gpio_i = Input(UInt(8.W))
  })

  io.gpio_o := DontCare
  io.gpio_en_o := DontCare

  implicit val config:WishboneConfig = WishboneConfig(32,32)

  val gen_imem_host = Module(new WishboneHost())
  val gen_imem_slave = Module(new WishboneDevice())
  val gen_dmem_host = Module(new WishboneHost())
  val gen_dmem_slave = Module(new WishboneDevice())

  val addressMap = new AddressMap

  addressMap.addDevice(Peripherals.all(configs("DCCM")("id").asInstanceOf[Int]), configs("DCCM")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_dmem_slave)

  if(configs("GPIO")("is").asInstanceOf[Boolean]){
      // GPIO
      val gpio = Module(new Gpio(new WBRequest(), new WBResponse()))
      val gen_gpio_slave = Module(new WishboneDevice())

      gen_gpio_slave.io.reqOut <> gpio.io.req
      gen_gpio_slave.io.rspIn <> gpio.io.rsp

      io.gpio_o := gpio.io.cio_gpio_o(7,0)
      io.gpio_en_o := gpio.io.cio_gpio_en_o(7,0)
      gpio.io.cio_gpio_i := io.gpio_i

      addressMap.addDevice(Peripherals.all(configs("GPIO")("id").asInstanceOf[Int]), configs("GPIO")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_gpio_slave)
    //
  }

  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val wbErr = Module(new WishboneErr())
  val core = Module(new Core(new WBRequest, new WBResponse)(M = configs("M")("is").asInstanceOf[Boolean]))


  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new WishboneMaster(), new WishboneSlave(), devices.size))

  // wb <-> Core (fetch)
  gen_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> gen_imem_host.io.rspOut
  gen_imem_slave.io.reqOut <> imem.io.req
  gen_imem_slave.io.rspIn <> imem.io.rsp

  // wb <-> wb (fetch)
  gen_imem_host.io.wbMasterTransmitter <> gen_imem_slave.io.wbMasterReceiver
  gen_imem_slave.io.wbSlaveTransmitter <> gen_imem_host.io.wbSlaveReceiver

  // wb <-> Core (memory)
  gen_dmem_host.io.reqIn <> core.io.dmemReq
  core.io.dmemRsp <> gen_dmem_host.io.rspOut
  gen_dmem_slave.io.reqOut <> dmem.io.req
  gen_dmem_slave.io.rspIn <> dmem.io.rsp


  // Switch connection
  switch.io.hostIn <> gen_dmem_host.io.wbMasterTransmitter
  switch.io.hostOut <> gen_dmem_host.io.wbSlaveReceiver
  for (i <- 0 until devices.size) {
    switch.io.devIn(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[WishboneDevice].io.wbSlaveTransmitter
    switch.io.devOut(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[WishboneDevice].io.wbMasterReceiver
  }
  switch.io.devIn(devices.size) <> wbErr.io.wbSlaveTransmitter
  switch.io.devOut(devices.size) <> wbErr.io.wbMasterReceiver
  switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.wbMasterTransmitter.bits.adr, addressMap)
}

import spray.json._
import DefaultJsonProtocol._

object GeneratorDriver extends App {

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

  (new ChiselStage).emitVerilog(new GeneratorWB(programFile=None, configs))
}
