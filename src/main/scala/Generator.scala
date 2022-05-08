import chisel3._
//import buraq_mini.core.Core
import nucleusrv.components.Core
import caravan.bus.common.{AddressMap, BusDecoder, Switch1toN, Peripherals}

import caravan.bus.tilelink.{TLRequest, TLResponse, TilelinkConfig, TilelinkDevice, TilelinkErr, TilelinkHost, TilelinkMaster, TilelinkSlave, TilelinkCDevice}
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
// import scala.util.parsing.json._



class SoCNow(programFile: Option[String], GPIO:Boolean = true, UART:Boolean = false, SPI:Boolean = false, TIMER:Boolean = false, I2C:Boolean = false, TL:Boolean = true, WB:Boolean = false, M:Boolean = false) extends Module {
  val io = IO(new Bundle {
    val gpio_io = Vec(4, Analog(1.W))

    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  val top = Module(new Generator(programFile, GPIO, UART, SPI, TIMER, I2C, TL, WB, M))
  val pll = Module(new PLL_8MHz())

  pll.io.clk_in1 := clock
  top.clock := pll.io.clk_out1

  val gpioInputWires = Wire(Vec(4, Bool()))
  val gpioOutputWires = Wire(Vec(4, Bool()))
  val gpioEnableWires = Wire(Vec(4, Bool()))

  val gpioPads = TriStateBuffer(quantity=4)
  val triStateBufferWires = for {
    ((((a,b),c),d),e) <- gpioPads zip gpioInputWires zip gpioOutputWires zip gpioEnableWires zip io.gpio_io
  } yield (a,b,c,d,e)

  triStateBufferWires map { case(buf: IOBUF, in: Bool, out: Bool, en: Bool, io: Analog) => {
    buf.io.connect(in, out, io, en)
  }}

  top.io.gpio_i := gpioInputWires.asUInt()
  gpioOutputWires := top.io.gpio_o.asBools()
  gpioEnableWires := top.io.gpio_en_o.asBools()

  io.spi_cs_n := top.io.spi_cs_n
  io.spi_sclk := top.io.spi_sclk
  io.spi_mosi := top.io.spi_mosi
  top.io.spi_miso := io.spi_miso

  io.cio_uart_intr_tx_o := top.io.cio_uart_intr_tx_o
  io.cio_uart_tx_o := top.io.cio_uart_tx_o
  top.io.cio_uart_rx_i := io.cio_uart_rx_i

  io.timer_intr_cmp := top.io.timer_intr_cmp
  io.timer_intr_ovf := top.io.timer_intr_ovf

  io.i2c_sda := top.io.i2c_sda
  io.i2c_scl := top.io.i2c_scl
  io.i2c_intr := top.io.i2c_intr



}



class Generator(programFile: Option[String], GPIO:Boolean = true, UART:Boolean = false, SPI:Boolean = false, TIMER:Boolean = false, I2C:Boolean = false, TL:Boolean = true, WB:Boolean = false, M:Boolean = false, TLC:Boolean = false) extends Module {
  val io = IO(new Bundle {
    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val gpio_o = Output(UInt(4.W))
    val gpio_en_o = Output(UInt(4.W))
    val gpio_i = Input(UInt(4.W))

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })


  if (WB){
    val genWB = Module(new GeneratorWB(programFile = programFile, GPIO=GPIO, UART=UART, TIMER=TIMER, I2C=I2C, SPI=SPI, M=M))

    io.spi_cs_n := genWB.io.spi_cs_n
    io.spi_sclk := genWB.io.spi_sclk
    io.spi_mosi := genWB.io.spi_mosi
    genWB.io.spi_miso := io.spi_miso

    io.cio_uart_tx_o := genWB.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := genWB.io.cio_uart_intr_tx_o
    genWB.io.cio_uart_rx_i := io.cio_uart_rx_i 

    io.gpio_o := genWB.io.gpio_o
    io.gpio_en_o := genWB.io.gpio_en_o
    genWB.io.gpio_i := io.gpio_i

    io.timer_intr_cmp := genWB.io.timer_intr_cmp
    io.timer_intr_ovf := genWB.io.timer_intr_ovf

    io.i2c_sda := genWB.io.i2c_sda
    io.i2c_scl := genWB.io.i2c_scl
    io.i2c_intr := genWB.io.i2c_intr

  }else if(TL){
    val genTL = Module(new GeneratorTL(programFile = programFile, GPIO=GPIO, UART=UART, TIMER=TIMER, I2C=I2C, SPI=SPI, M=M))

    io.spi_cs_n := genTL.io.spi_cs_n
    io.spi_sclk := genTL.io.spi_sclk
    io.spi_mosi := genTL.io.spi_mosi
    genTL.io.spi_miso := io.spi_miso

    io.cio_uart_tx_o := genTL.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := genTL.io.cio_uart_intr_tx_o
    genTL.io.cio_uart_rx_i := io.cio_uart_rx_i 

    io.gpio_o := genTL.io.gpio_o
    io.gpio_en_o := genTL.io.gpio_en_o
    genTL.io.gpio_i := io.gpio_i

    io.timer_intr_cmp := genTL.io.timer_intr_cmp
    io.timer_intr_ovf := genTL.io.timer_intr_ovf

    io.i2c_sda := genTL.io.i2c_sda
    io.i2c_scl := genTL.io.i2c_scl
    io.i2c_intr := genTL.io.i2c_intr

  } else {
    val genTLC = Module(new GeneratorTLC(programFile = programFile, GPIO=GPIO, UART=UART, TIMER=TIMER, I2C=I2C, SPI=SPI, M=M))

    io.spi_cs_n := genTLC.io.spi_cs_n
    io.spi_sclk := genTLC.io.spi_sclk
    io.spi_mosi := genTLC.io.spi_mosi
    genTLC.io.spi_miso := io.spi_miso

    io.cio_uart_tx_o := genTLC.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := genTLC.io.cio_uart_intr_tx_o
    genTLC.io.cio_uart_rx_i := io.cio_uart_rx_i 

    io.gpio_o := genTLC.io.gpio_o
    io.gpio_en_o := genTLC.io.gpio_en_o
    genTLC.io.gpio_i := io.gpio_i

    io.timer_intr_cmp := genTLC.io.timer_intr_cmp
    io.timer_intr_ovf := genTLC.io.timer_intr_ovf

    io.i2c_sda := genTLC.io.i2c_sda
    io.i2c_scl := genTLC.io.i2c_scl
    io.i2c_intr := genTLC.io.i2c_intr
  }

}


class GeneratorWB(programFile: Option[String], GPIO:Boolean = true, UART:Boolean = true, SPI:Boolean = true, TIMER:Boolean = true, I2C:Boolean = true, M:Boolean = false) extends Module {
  val io = IO(new Bundle {
    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val gpio_o = Output(UInt(8.W))
    val gpio_en_o = Output(UInt(8.W))
    val gpio_i = Input(UInt(8.W))

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())


  })

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare


  // implicit val config: TilelinkConfig = TilelinkConfig()
  implicit val config:WishboneConfig = WishboneConfig(32,32)

  val gen_imem_host = Module(new WishboneHost())
  val gen_imem_slave = Module(new WishboneDevice())
  val gen_dmem_host = Module(new WishboneHost())
  val gen_dmem_slave = Module(new WishboneDevice())


// GPIO
  val gpio = Module(new Gpio(new WBRequest(), new WBResponse()))
  val gen_gpio_slave = Module(new WishboneDevice())

  gen_gpio_slave.io.reqOut <> gpio.io.req
  gen_gpio_slave.io.rspIn <> gpio.io.rsp

  io.gpio_o := gpio.io.cio_gpio_o(7,0)
  io.gpio_en_o := gpio.io.cio_gpio_en_o(7,0)
  gpio.io.cio_gpio_i := io.gpio_i
//

var slaves = Seq(gen_dmem_slave, gen_gpio_slave)

if (SPI){
  implicit val spiConfig = Config()
  val spi = Module(new Spi(new WBRequest(), new WBResponse()))

  val gen_spi_slave = Module(new WishboneDevice())
  // val gen_uart_slave = Module(new WishboneDevice())

  gen_spi_slave.io.reqOut <> spi.io.req
  gen_spi_slave.io.rspIn <> spi.io.rsp

  io.spi_cs_n := spi.io.cs_n
  io.spi_sclk := spi.io.sclk
  io.spi_mosi := spi.io.mosi
  spi.io.miso := io.spi_miso

  slaves = slaves :+ gen_spi_slave
}



if (UART){
  println("-----------In uart", UART)
  val uart = Module(new uart(new WBRequest(), new WBResponse()))

  val gen_uart_slave = Module(new WishboneDevice())

  gen_uart_slave.io.reqOut <> uart.io.request
  gen_uart_slave.io.rspIn <> uart.io.response

  uart.io.cio_uart_rx_i := io.cio_uart_rx_i
  io.cio_uart_tx_o := uart.io.cio_uart_tx_o
  io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

  slaves = slaves :+ gen_uart_slave
}

if (TIMER){
  println("-----------------In timer")

  val timer = Module(new Timer(new WBRequest(), new WBResponse()))

  val gen_timer_slave = Module(new WishboneDevice())

  gen_timer_slave.io.reqOut <> timer.io.req
  gen_timer_slave.io.rspIn <> timer.io.rsp 

  io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
  io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

  slaves = slaves :+ gen_timer_slave
}

if (I2C){
  println("-----------In I2C", I2C)
  val i2c = Module(new i2c(new WBRequest(), new WBResponse()))

  val gen_i2c_slave = Module(new WishboneDevice())

  gen_i2c_slave.io.reqOut <> i2c.io.request
  gen_i2c_slave.io.rspIn <> i2c.io.response 

  io.i2c_sda := i2c.io.cio_i2c_sda
  io.i2c_scl := i2c.io.cio_i2c_scl
  io.i2c_intr := i2c.io.cio_i2c_intr

  slaves = slaves :+ gen_i2c_slave
}


println("-------------" , slaves)
println("-----------------I2C" , I2C)
println("-----------------Timer" , TIMER)
println("-----------------UART" , UART)
println("-----------------SPI" , SPI)
// if (SPI & UART){
//   implicit val spiConfig = Config()
//   val spi = Module(new Spi(new WBRequest(), new WBResponse()))

//   val gen_spi_slave = Module(new WishboneDevice())
//   val gen_uart_slave = Module(new WishboneDevice())

//   gen_spi_slave.io.reqOut <> spi.io.req
//   gen_spi_slave.io.rspIn <> spi.io.rsp

//   io.spi_cs_n := spi.io.cs_n
//   io.spi_sclk := spi.io.sclk
//   io.spi_mosi := spi.io.mosi
//   spi.io.miso := io.spi_miso

//   val uart = Module(new uart(new WBRequest(), new WBResponse()))

//   gen_uart_slave.io.reqOut <> uart.io.request
//   gen_uart_slave.io.rspIn <> uart.io.response

//   uart.io.cio_uart_rx_i := io.cio_uart_rx_i
//   io.cio_uart_tx_o := uart.io.cio_uart_tx_o
//   io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

//   slaves = Seq(gen_dmem_slave, gen_gpio_slave, gen_spi_slave, gen_uart_slave)
// }else if(UART){
//   val uart = Module(new uart(new WBRequest(), new WBResponse()))

//   val gen_uart_slave = Module(new WishboneDevice())

//   gen_uart_slave.io.reqOut <> uart.io.request
//   gen_uart_slave.io.rspIn <> uart.io.response

//   uart.io.cio_uart_rx_i := io.cio_uart_rx_i
//   io.cio_uart_tx_o := uart.io.cio_uart_tx_o
//   io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

//   slaves = Seq(gen_dmem_slave, gen_gpio_slave, gen_uart_slave)
// }else if(SPI){
//   implicit val spiConfig = Config()
//   val spi = Module(new Spi(new WBRequest(), new WBResponse()))

//    val gen_spi_slave = Module(new WishboneDevice())

//   gen_spi_slave.io.reqOut <> spi.io.req
//   gen_spi_slave.io.rspIn <> spi.io.rsp

//   io.spi_cs_n := spi.io.cs_n
//   io.spi_sclk := spi.io.sclk
//   io.spi_mosi := spi.io.mosi
//   spi.io.miso := io.spi_miso

//   slaves = Seq(gen_dmem_slave, gen_gpio_slave, gen_spi_slave)
// }

  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val wbErr = Module(new WishboneErr())
  val core = Module(new Core(new WBRequest, new WBResponse)(M = M))


  val addresses = Seq("h40000000".U(32.W), "h40001000".U(32.W), "h40002000".U(32.W), "h40003000".U(32.W) , "h40004000".U(32.W), "h40005000".U(32.W))
  val addressMap = new AddressMap

  for (i <- Peripherals.all.indices){
    println("***",addresses(i))
    addressMap.addDevice(Peripherals.all(i), addresses(i), "h00000FFF".U(32.W), slaves(i))
  }

  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new WishboneMaster(), new WishboneSlave(), devices.size))

  // tl <-> Core (fetch)
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
  // switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.wbMasterTransmitter.bits.a_address, addressMap)
  switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.wbMasterTransmitter.bits.adr, addressMap)

  // core.io.stall_core_i := false.B
  // core.io.irq_external_i := false.B

}

class GeneratorTL(programFile: Option[String], GPIO:Boolean = true, UART:Boolean = true, SPI:Boolean = true, TIMER:Boolean = true, I2C:Boolean = true, M:Boolean = false) extends Module {
  val io = IO(new Bundle {
    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val gpio_o = Output(UInt(8.W))
    val gpio_en_o = Output(UInt(8.W))
    val gpio_i = Input(UInt(8.W))

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare


  implicit val config: TilelinkConfig = TilelinkConfig()
  // implicit val config:WishboneConfig = WishboneConfig(32,32)

  val gen_imem_host = Module(new TilelinkHost())
  val gen_imem_slave = Module(new TilelinkDevice())
  val gen_dmem_host = Module(new TilelinkHost())
  val gen_dmem_slave = Module(new TilelinkDevice())


// GPIO
  val gpio = Module(new Gpio(new TLRequest(), new TLResponse()))
  val gen_gpio_slave = Module(new TilelinkDevice())

  gen_gpio_slave.io.reqOut <> gpio.io.req
  gen_gpio_slave.io.rspIn <> gpio.io.rsp

  io.gpio_o := gpio.io.cio_gpio_o(7,0)
  io.gpio_en_o := gpio.io.cio_gpio_en_o(7,0)
  gpio.io.cio_gpio_i := io.gpio_i
//

var slaves = Seq(gen_dmem_slave, gen_gpio_slave)

  if (SPI){
  implicit val spiConfig = Config()
  val spi = Module(new Spi(new TLRequest(), new TLResponse()))

  val gen_spi_slave = Module(new TilelinkDevice())
  // val gen_uart_slave = Module(new WishboneDevice())

  gen_spi_slave.io.reqOut <> spi.io.req
  gen_spi_slave.io.rspIn <> spi.io.rsp

  io.spi_cs_n := spi.io.cs_n
  io.spi_sclk := spi.io.sclk
  io.spi_mosi := spi.io.mosi
  spi.io.miso := io.spi_miso

  slaves = slaves :+ gen_spi_slave
}



if (UART){
  println("-----------In uart", UART)
  val uart = Module(new uart(new TLRequest(), new TLResponse()))

  val gen_uart_slave = Module(new TilelinkDevice())

  gen_uart_slave.io.reqOut <> uart.io.request
  gen_uart_slave.io.rspIn <> uart.io.response

  uart.io.cio_uart_rx_i := io.cio_uart_rx_i
  io.cio_uart_tx_o := uart.io.cio_uart_tx_o
  io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

  slaves = slaves :+ gen_uart_slave
}

if (TIMER){
  println("-----------------In timer")

  val timer = Module(new Timer(new TLRequest(), new TLResponse()))

  val gen_timer_slave = Module(new TilelinkDevice())

  gen_timer_slave.io.reqOut <> timer.io.req
  gen_timer_slave.io.rspIn <> timer.io.rsp 

  io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
  io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

  slaves = slaves :+ gen_timer_slave
}

if (I2C){
  println("-----------In I2C", I2C)
  val i2c = Module(new i2c(new TLRequest(), new TLResponse()))

  val gen_i2c_slave = Module(new TilelinkDevice())

  gen_i2c_slave.io.reqOut <> i2c.io.request
  gen_i2c_slave.io.rspIn <> i2c.io.response 

  io.i2c_sda := i2c.io.cio_i2c_sda
  io.i2c_scl := i2c.io.cio_i2c_scl
  io.i2c_intr := i2c.io.cio_i2c_intr

  slaves = slaves :+ gen_i2c_slave
}

  println("-----------",slaves)
  println("-----------------I2C" , I2C)
  println("-----------------Timer" , TIMER)
  println("-----------------UART" , UART)
  println("-----------------SPI" , SPI)


  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val tlErr = Module(new TilelinkErr())
  val core = Module(new Core(new TLRequest, new TLResponse)(M = M))


  val addresses = Seq("h40000000".U(32.W), "h40001000".U(32.W), "h40002000".U(32.W), "h40003000".U(32.W) , "h40004000".U(32.W) , "h40005000".U(32.W))
  // val slaves = Seq(gen_dmem_slave, gen_gpio_slave, gen_spi_slave, gen_uart_slave)
  val addressMap = new AddressMap

  for (i <- Peripherals.all.indices){
    addressMap.addDevice(Peripherals.all(i), addresses(i), "h00000FFF".U(32.W), slaves(i))
  }

  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new TilelinkMaster(), new TilelinkSlave(), devices.size))

  // tl <-> Core (fetch)
  gen_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> gen_imem_host.io.rspOut
  gen_imem_slave.io.reqOut <> imem.io.req
  gen_imem_slave.io.rspIn <> imem.io.rsp

  // wb <-> wb (fetch)
  gen_imem_host.io.tlMasterTransmitter <> gen_imem_slave.io.tlMasterReceiver
  gen_imem_slave.io.tlSlaveTransmitter <> gen_imem_host.io.tlSlaveReceiver

  // tl <-> Core (memory)
  gen_dmem_host.io.reqIn <> core.io.dmemReq
  core.io.dmemRsp <> gen_dmem_host.io.rspOut
  gen_dmem_slave.io.reqOut <> dmem.io.req
  gen_dmem_slave.io.rspIn <> dmem.io.rsp


  // Switch connection
  switch.io.hostIn <> gen_dmem_host.io.tlMasterTransmitter
  switch.io.hostOut <> gen_dmem_host.io.tlSlaveReceiver
  for (i <- 0 until devices.size) {
    switch.io.devIn(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[TilelinkDevice].io.tlSlaveTransmitter
    switch.io.devOut(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[TilelinkDevice].io.tlMasterReceiver
  }
  switch.io.devIn(devices.size) <> tlErr.io.tlSlaveTransmitter
  switch.io.devOut(devices.size) <> tlErr.io.tlMasterReceiver
  switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.tlMasterTransmitter.bits.a_address, addressMap)
  // switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.tlMasterTransmitter.bits.adr, addressMap)

  // core.io.stall_core_i := false.B
  // core.io.irq_external_i := false.B


}


class GeneratorTLC(programFile: Option[String], GPIO:Boolean = true, UART:Boolean = true, SPI:Boolean = true, TIMER:Boolean = true, I2C:Boolean = true, M:Boolean = false) extends Module {
  val io = IO(new Bundle {
    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val gpio_o = Output(UInt(8.W))
    val gpio_en_o = Output(UInt(8.W))
    val gpio_i = Input(UInt(8.W))

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare


  implicit val config: TilelinkConfig = TilelinkConfig()
  // implicit val config:WishboneConfig = WishboneConfig(32,32)

  val gen_imem_host = Module(new TilelinkHost())
  val gen_imem_slave = Module(new TilelinkDevice())

  val gen_dmem_host = Module(new DMCache(4, 10, 32)(new TLRequest(), new TLResponse()))
  // val gen_dmem_cache = Module(new DMCache())
  val gen_dmem_slave = Module(new TilelinkCDevice())


// GPIO
  val gpio = Module(new Gpio(new TLRequest(), new TLResponse()))
  val gen_gpio_slave = Module(new TilelinkCDevice())

  gen_gpio_slave.io.reqOut <> gpio.io.req
  gen_gpio_slave.io.rspIn <> gpio.io.rsp

  io.gpio_o := gpio.io.cio_gpio_o(7,0)
  io.gpio_en_o := gpio.io.cio_gpio_en_o(7,0)
  gpio.io.cio_gpio_i := io.gpio_i
//

var slaves = Seq(gen_dmem_slave, gen_gpio_slave)

  if (SPI){
  implicit val spiConfig = Config()
  val spi = Module(new Spi(new TLRequest(), new TLResponse()))

  val gen_spi_slave = Module(new TilelinkCDevice())
  // val gen_uart_slave = Module(new WishboneDevice())

  gen_spi_slave.io.reqOut <> spi.io.req
  gen_spi_slave.io.rspIn <> spi.io.rsp

  io.spi_cs_n := spi.io.cs_n
  io.spi_sclk := spi.io.sclk
  io.spi_mosi := spi.io.mosi
  spi.io.miso := io.spi_miso

  slaves = slaves :+ gen_spi_slave
}



if (UART){
  println("-----------In uart", UART)
  val uart = Module(new uart(new TLRequest(), new TLResponse()))

  val gen_uart_slave = Module(new TilelinkCDevice())

  gen_uart_slave.io.reqOut <> uart.io.request
  gen_uart_slave.io.rspIn <> uart.io.response

  uart.io.cio_uart_rx_i := io.cio_uart_rx_i
  io.cio_uart_tx_o := uart.io.cio_uart_tx_o
  io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

  slaves = slaves :+ gen_uart_slave
}

if (TIMER){
  println("-----------------In timer")

  val timer = Module(new Timer(new TLRequest(), new TLResponse()))

  val gen_timer_slave = Module(new TilelinkCDevice())

  gen_timer_slave.io.reqOut <> timer.io.req
  gen_timer_slave.io.rspIn <> timer.io.rsp 

  io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
  io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

  slaves = slaves :+ gen_timer_slave
}

if (I2C){
  println("-----------In I2C", I2C)
  val i2c = Module(new i2c(new TLRequest(), new TLResponse()))

  val gen_i2c_slave = Module(new TilelinkCDevice())

  gen_i2c_slave.io.reqOut <> i2c.io.request
  gen_i2c_slave.io.rspIn <> i2c.io.response 

  io.i2c_sda := i2c.io.cio_i2c_sda
  io.i2c_scl := i2c.io.cio_i2c_scl
  io.i2c_intr := i2c.io.cio_i2c_intr

  slaves = slaves :+ gen_i2c_slave
}

  println("-----------",slaves)
  println("-----------------I2C" , I2C)
  println("-----------------Timer" , TIMER)
  println("-----------------UART" , UART)
  println("-----------------SPI" , SPI)


  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val tlErr = Module(new TilelinkErr())
  val core = Module(new Core(new TLRequest, new TLResponse)(M = M))


  val addresses = Seq("h40000000".U(32.W), "h40001000".U(32.W), "h40002000".U(32.W), "h40003000".U(32.W) , "h40004000".U(32.W) , "h40005000".U(32.W))
  // val slaves = Seq(gen_dmem_slave, gen_gpio_slave, gen_spi_slave, gen_uart_slave)
  val addressMap = new AddressMap

  for (i <- Peripherals.all.indices){
    addressMap.addDevice(Peripherals.all(i), addresses(i), "h00000FFF".U(32.W), slaves(i))
  }

  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new TilelinkMaster(), new TilelinkSlave(), devices.size))

  // tl <-> Core (fetch)
  gen_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> gen_imem_host.io.rspOut
  gen_imem_slave.io.reqOut <> imem.io.req
  gen_imem_slave.io.rspIn <> imem.io.rsp

  // wb <-> wb (fetch)
  gen_imem_host.io.tlMasterTransmitter <> gen_imem_slave.io.tlMasterReceiver
  gen_imem_slave.io.tlSlaveTransmitter <> gen_imem_host.io.tlSlaveReceiver

  // tl <-> Core (memory)
  gen_dmem_host.io.reqIn <> core.io.dmemReq
  core.io.dmemRsp <> gen_dmem_host.io.rspOut
  gen_dmem_slave.io.reqOut <> dmem.io.req
  gen_dmem_slave.io.rspIn <> dmem.io.rsp


  // Switch connection
  switch.io.hostIn <> gen_dmem_host.io.tlMasterTransmitter
  switch.io.hostOut <> gen_dmem_host.io.tlSlaveReceiver
  for (i <- 0 until devices.size) {
    switch.io.devIn(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[TilelinkCDevice].io.tlSlaveTransmitter
    switch.io.devOut(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[TilelinkCDevice].io.tlMasterReceiver
  }
  switch.io.devIn(devices.size) <> tlErr.io.tlSlaveTransmitter
  switch.io.devOut(devices.size) <> tlErr.io.tlMasterReceiver
  switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.tlMasterTransmitter.bits.a_address, addressMap)
  // switch.io.devSel := BusDecoder.decode(gen_dmem_host.io.tlMasterTransmitter.bits.adr, addressMap)

  // core.io.stall_core_i := false.B
  // core.io.irq_external_i := false.B


}

import spray.json._
import DefaultJsonProtocol._
// import sys.process._

object GeneratorDriver extends App {

  // "python3 peripheralScript.py" !

  val file = scala.io.Source.fromFile((os.pwd.toString)+"//src//main//scala//config.json").mkString

  val fileToJson = file.parseJson.convertTo[Map[String, JsValue]]
  val config = fileToJson.map({case (a,b) => a -> {if (b == JsNumber(1)) true else false}})
  println("--------------config---------" , fileToJson)

  (new ChiselStage).emitVerilog(new Generator(programFile=None, GPIO = config("gpio"), UART = config("uart"), SPI = config("spi_flash"), TIMER = config("timer"), I2C = config("i2c"), TL = config("tl"), WB = config("wb"), M = config("m"), TLC=config("tlc")))
}


object SoCNowDriver extends App {

  val file = scala.io.Source.fromFile((os.pwd.toString)+"//src//main//scala//config.json").mkString

  val fileToJson = file.parseJson.convertTo[Map[String, JsValue]]
  val config = fileToJson.map({case (a,b) => a -> {if (b == JsNumber(1)) true else false}})
  println("--------------config---------" , fileToJson)
  (new ChiselStage).emitVerilog(new SoCNow(programFile=None, GPIO = config("gpio"), UART = config("uart"), SPI = config("spi_flash"), TIMER = config("timer"), I2C = config("i2c"), TL = config("tl"), WB = config("wb"), M = config("m")))
}