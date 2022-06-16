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
import jigsaw.peripherals.spi._
import jigsaw.peripherals.UART._
import jigsaw.peripherals.timer._
import jigsaw.peripherals.i2c._
import ccache.caches.DMCache

// class Generator(programFile: Option[String],
//                  configs:Map[Any, Map[Any, Any]]) extends Module {
//   val io = IO(new Bundle {
//       val spi_cs_n = Output(Bool())
//       val spi_sclk = Output(Bool())
//       val spi_mosi = Output(Bool())
//       val spi_miso = Input(Bool())

//       val cio_uart_rx_i = Input(Bool())
//       val cio_uart_tx_o = Output(Bool())
//       val cio_uart_intr_tx_o = Output(Bool())

//       val gpio_o = Output(UInt(4.W))
//       val gpio_en_o = Output(UInt(4.W))
//       val gpio_i = Input(UInt(4.W))

//       val timer_intr_cmp = Output(Bool())
//       val timer_intr_ovf = Output(Bool())

//       val i2c_sda = Output(Bool())
//       val i2c_scl = Output(Bool())
//       val i2c_intr = Output(Bool())
//     })


//     if (configs("WB")("is").asInstanceOf[Boolean]){
//       val genWB = Module(new GeneratorWB(programFile = programFile, configs = configs)

//       io.spi_cs_n := genWB.io.spi_cs_n
//       io.spi_sclk := genWB.io.spi_sclk
//       io.spi_mosi := genWB.io.spi_mosi
//       genWB.io.spi_miso := io.spi_miso

//       io.cio_uart_tx_o := genWB.io.cio_uart_tx_o
//       io.cio_uart_intr_tx_o := genWB.io.cio_uart_intr_tx_o
//       genWB.io.cio_uart_rx_i := io.cio_uart_rx_i 

//       io.gpio_o := genWB.io.gpio_o
//       io.gpio_en_o := genWB.io.gpio_en_o
//       genWB.io.gpio_i := io.gpio_i

//       io.timer_intr_cmp := genWB.io.timer_intr_cmp
//       io.timer_intr_ovf := genWB.io.timer_intr_ovf

//       io.i2c_sda := genWB.io.i2c_sda
//       io.i2c_scl := genWB.io.i2c_scl
//       io.i2c_intr := genWB.io.i2c_intr

//     }
// }

class GeneratorWB(programFile: Option[String],
                 configs:Map[Any, Map[Any, Any]]) extends Module {

  val n = configs("GPIO")("n").asInstanceOf[Int]

  val io = IO(new Bundle {
    val gpio_o = Output(UInt(n.W))
    val gpio_en_o = Output(UInt(n.W))
    val gpio_i = Input(UInt(n.W))

    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val spi_flash_cs_n = Output(Bool())
    val spi_flash_sclk = Output(Bool())
    val spi_flash_mosi = Output(Bool())
    val spi_flash_miso = Input(Bool())

    val i2c_sda_in = Input(Bool())
    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  io.gpio_o := DontCare
  io.gpio_en_o := DontCare

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.spi_flash_cs_n := DontCare
  io.spi_flash_sclk := DontCare
  io.spi_flash_mosi := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare

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

      // val n = configs("GPIO")("n").asInstanceOf[Int]
      io.gpio_o := gpio.io.cio_gpio_o(n-1,0)
      io.gpio_en_o := gpio.io.cio_gpio_en_o(n-1,0)
      gpio.io.cio_gpio_i := io.gpio_i

      addressMap.addDevice(Peripherals.all(configs("GPIO")("id").asInstanceOf[Int]), configs("GPIO")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_gpio_slave)
    //
  }
   if(configs("SPI")("is").asInstanceOf[Boolean]){
    val spi = Module(new Spi(new WBRequest(), new WBResponse()))
    val gen_spi_slave = Module(new WishboneDevice())

    gen_spi_slave.io.reqOut <> spi.io.req
    gen_spi_slave.io.rspIn <> spi.io.rsp

    io.spi_cs_n := spi.io.cs_n
    io.spi_sclk := spi.io.sclk
    io.spi_mosi := spi.io.mosi
    spi.io.miso := io.spi_miso

    addressMap.addDevice(Peripherals.all(configs("SPI")("id").asInstanceOf[Int]), configs("SPI")("baseAddr").asInstanceOf[String].U(32.W), configs("SPI")("mask").asInstanceOf[String].U(32.W), gen_spi_slave)
  }
  if (configs("UART")("is").asInstanceOf[Boolean]){
    val uart = Module(new uart(new WBRequest(), new WBResponse()))
    val gen_uart_slave = Module(new WishboneDevice())

    gen_uart_slave.io.reqOut <> uart.io.request
    gen_uart_slave.io.rspIn <> uart.io.response

    uart.io.cio_uart_rx_i := io.cio_uart_rx_i
    io.cio_uart_tx_o := uart.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

    addressMap.addDevice(Peripherals.all(configs("UART")("id").asInstanceOf[Int]), configs("UART")("baseAddr").asInstanceOf[String].U(32.W), configs("UART")("mask").asInstanceOf[String].U(32.W), gen_uart_slave)
  }
  if (configs("TIMER")("is").asInstanceOf[Boolean]){
    val timer = Module(new Timer(new WBRequest(), new WBResponse()))
    val gen_timer_slave = Module(new WishboneDevice())

    gen_timer_slave.io.reqOut <> timer.io.req
    gen_timer_slave.io.rspIn <> timer.io.rsp 

    io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
    io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

    addressMap.addDevice(Peripherals.all(configs("TIMER")("id").asInstanceOf[Int]), configs("TIMER")("baseAddr").asInstanceOf[String].U(32.W), configs("TIMER")("mask").asInstanceOf[String].U(32.W), gen_timer_slave)
  }
  if (configs("SPIF")("is").asInstanceOf[Boolean]){
    val spi_flash = Module(new SpiFlash(new WBRequest(), new WBResponse()))
    val gen_spi_flash_slave = Module(new WishboneDevice())

    gen_spi_flash_slave.io.reqOut <> spi_flash.io.req
    gen_spi_flash_slave.io.rspIn <> spi_flash.io.rsp

    io.spi_flash_cs_n := spi_flash.io.cs_n
    io.spi_flash_sclk := spi_flash.io.sclk
    io.spi_flash_mosi := spi_flash.io.mosi
    spi_flash.io.miso := io.spi_flash_miso

    addressMap.addDevice(Peripherals.all(configs("SPIF")("id").asInstanceOf[Int]), configs("SPIF")("baseAddr").asInstanceOf[String].U(32.W), configs("SPIF")("mask").asInstanceOf[String].U(32.W), gen_spi_flash_slave)
  }
  if (configs("I2C")("is").asInstanceOf[Boolean]){
    val i2c = Module(new i2c(new WBRequest(), new WBResponse()))
    val gen_i2c_slave = Module(new WishboneDevice())

    gen_i2c_slave.io.reqOut <> i2c.io.request
    gen_i2c_slave.io.rspIn <> i2c.io.response 

    i2c.io.cio_i2c_sda_in := io.i2c_sda_in
    io.i2c_sda := i2c.io.cio_i2c_sda
    io.i2c_scl := i2c.io.cio_i2c_scl
    io.i2c_intr := i2c.io.cio_i2c_intr

    addressMap.addDevice(Peripherals.all(configs("I2C")("id").asInstanceOf[Int]), configs("I2C")("baseAddr").asInstanceOf[String].U(32.W), configs("I2C")("mask").asInstanceOf[String].U(32.W), gen_i2c_slave)

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

class GeneratorTL(programFile: Option[String],
                 configs:Map[Any, Map[Any, Any]]) extends Module {

  val n = configs("GPIO")("n").asInstanceOf[Int]

  val io = IO(new Bundle {
    val gpio_o = Output(UInt(n.W))
    val gpio_en_o = Output(UInt(n.W))
    val gpio_i = Input(UInt(n.W))

    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val spi_flash_cs_n = Output(Bool())
    val spi_flash_sclk = Output(Bool())
    val spi_flash_mosi = Output(Bool())
    val spi_flash_miso = Input(Bool())

    val i2c_sda_in = Input(Bool())
    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  io.gpio_o := DontCare
  io.gpio_en_o := DontCare

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.spi_flash_cs_n := DontCare
  io.spi_flash_sclk := DontCare
  io.spi_flash_mosi := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare

  implicit val config:TilelinkConfig = TilelinkConfig(10,32)

  val gen_imem_host = Module(new TilelinkHost())
  val gen_imem_slave = Module(new TilelinkDevice())
  val gen_dmem_host = Module(new TilelinkHost())
  val gen_dmem_slave = Module(new TilelinkDevice())

  val addressMap = new AddressMap

  addressMap.addDevice(Peripherals.all(configs("DCCM")("id").asInstanceOf[Int]), configs("DCCM")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_dmem_slave)

  if(configs("GPIO")("is").asInstanceOf[Boolean]){
      // GPIO
      val gpio = Module(new Gpio(new TLRequest(), new TLResponse()))
      val gen_gpio_slave = Module(new TilelinkDevice())

      gen_gpio_slave.io.reqOut <> gpio.io.req
      gen_gpio_slave.io.rspIn <> gpio.io.rsp

      // val n = configs("GPIO")("n").asInstanceOf[Int]
      io.gpio_o := gpio.io.cio_gpio_o(n-1,0)
      io.gpio_en_o := gpio.io.cio_gpio_en_o(n-1,0)
      gpio.io.cio_gpio_i := io.gpio_i

      addressMap.addDevice(Peripherals.all(configs("GPIO")("id").asInstanceOf[Int]), configs("GPIO")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_gpio_slave)
    //
  }
   if(configs("SPI")("is").asInstanceOf[Boolean]){
    val spi = Module(new Spi(new TLRequest(), new TLResponse()))
    val gen_spi_slave = Module(new TilelinkDevice())

    gen_spi_slave.io.reqOut <> spi.io.req
    gen_spi_slave.io.rspIn <> spi.io.rsp

    io.spi_cs_n := spi.io.cs_n
    io.spi_sclk := spi.io.sclk
    io.spi_mosi := spi.io.mosi
    spi.io.miso := io.spi_miso

    addressMap.addDevice(Peripherals.all(configs("SPI")("id").asInstanceOf[Int]), configs("SPI")("baseAddr").asInstanceOf[String].U(32.W), configs("SPI")("mask").asInstanceOf[String].U(32.W), gen_spi_slave)
  }
  if (configs("UART")("is").asInstanceOf[Boolean]){
    val uart = Module(new uart(new TLRequest(), new TLResponse()))
    val gen_uart_slave = Module(new TilelinkDevice())

    gen_uart_slave.io.reqOut <> uart.io.request
    gen_uart_slave.io.rspIn <> uart.io.response

    uart.io.cio_uart_rx_i := io.cio_uart_rx_i
    io.cio_uart_tx_o := uart.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

    addressMap.addDevice(Peripherals.all(configs("UART")("id").asInstanceOf[Int]), configs("UART")("baseAddr").asInstanceOf[String].U(32.W), configs("UART")("mask").asInstanceOf[String].U(32.W), gen_uart_slave)
  }
  if (configs("TIMER")("is").asInstanceOf[Boolean]){
    val timer = Module(new Timer(new TLRequest(), new TLResponse()))
    val gen_timer_slave = Module(new TilelinkDevice())

    gen_timer_slave.io.reqOut <> timer.io.req
    gen_timer_slave.io.rspIn <> timer.io.rsp 

    io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
    io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

    addressMap.addDevice(Peripherals.all(configs("TIMER")("id").asInstanceOf[Int]), configs("TIMER")("baseAddr").asInstanceOf[String].U(32.W), configs("TIMER")("mask").asInstanceOf[String].U(32.W), gen_timer_slave)
  }
  if (configs("SPIF")("is").asInstanceOf[Boolean]){
    val spi_flash = Module(new SpiFlash(new TLRequest(), new TLResponse()))
    val gen_spi_flash_slave = Module(new TilelinkDevice())

    gen_spi_flash_slave.io.reqOut <> spi_flash.io.req
    gen_spi_flash_slave.io.rspIn <> spi_flash.io.rsp

    io.spi_flash_cs_n := spi_flash.io.cs_n
    io.spi_flash_sclk := spi_flash.io.sclk
    io.spi_flash_mosi := spi_flash.io.mosi
    spi_flash.io.miso := io.spi_flash_miso

    addressMap.addDevice(Peripherals.all(configs("SPIF")("id").asInstanceOf[Int]), configs("SPIF")("baseAddr").asInstanceOf[String].U(32.W), configs("SPIF")("mask").asInstanceOf[String].U(32.W), gen_spi_flash_slave)
  }
  if (configs("I2C")("is").asInstanceOf[Boolean]){
    val i2c = Module(new i2c(new TLRequest(), new TLResponse()))
    val gen_i2c_slave = Module(new TilelinkDevice())

    gen_i2c_slave.io.reqOut <> i2c.io.request
    gen_i2c_slave.io.rspIn <> i2c.io.response 

    i2c.io.cio_i2c_sda_in := io.i2c_sda_in
    io.i2c_sda := i2c.io.cio_i2c_sda
    io.i2c_scl := i2c.io.cio_i2c_scl
    io.i2c_intr := i2c.io.cio_i2c_intr

    addressMap.addDevice(Peripherals.all(configs("I2C")("id").asInstanceOf[Int]), configs("I2C")("baseAddr").asInstanceOf[String].U(32.W), configs("I2C")("mask").asInstanceOf[String].U(32.W), gen_i2c_slave)

  }

  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val tlErr = Module(new TilelinkError())
  val core = Module(new Core(new TLRequest, new TLResponse)(M = configs("M")("is").asInstanceOf[Boolean]))


  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new TilelinkMaster(), new TilelinkSlave(), devices.size))

  // TL <-> Core (fetch)
  gen_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> gen_imem_host.io.rspOut
  gen_imem_slave.io.reqOut <> imem.io.req
  gen_imem_slave.io.rspIn <> imem.io.rsp

  // TL <-> TL (fetch)
  gen_imem_host.io.tlMasterTransmitter <> gen_imem_slave.io.tlMasterReceiver
  gen_imem_slave.io.tlSlaveTransmitter <> gen_imem_host.io.tlSlaveReceiver

  // TL <-> Core (memory)
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
}

class GeneratorTLC(programFile: Option[String],
                 configs:Map[Any, Map[Any, Any]]) extends Module {

  val n = configs("GPIO")("n").asInstanceOf[Int]

  val io = IO(new Bundle {
    val gpio_o = Output(UInt(n.W))
    val gpio_en_o = Output(UInt(n.W))
    val gpio_i = Input(UInt(n.W))

    val spi_cs_n = Output(Bool())
    val spi_sclk = Output(Bool())
    val spi_mosi = Output(Bool())
    val spi_miso = Input(Bool())

    val cio_uart_rx_i = Input(Bool())
    val cio_uart_tx_o = Output(Bool())
    val cio_uart_intr_tx_o = Output(Bool())

    val timer_intr_cmp = Output(Bool())
    val timer_intr_ovf = Output(Bool())

    val spi_flash_cs_n = Output(Bool())
    val spi_flash_sclk = Output(Bool())
    val spi_flash_mosi = Output(Bool())
    val spi_flash_miso = Input(Bool())

    val i2c_sda_in = Input(Bool())
    val i2c_sda = Output(Bool())
    val i2c_scl = Output(Bool())
    val i2c_intr = Output(Bool())
  })

  io.gpio_o := DontCare
  io.gpio_en_o := DontCare

  io.spi_cs_n := DontCare
  io.spi_sclk := DontCare
  io.spi_mosi := DontCare

  io.cio_uart_tx_o := DontCare
  io.cio_uart_intr_tx_o := DontCare

  io.timer_intr_cmp := DontCare
  io.timer_intr_ovf := DontCare

  io.spi_flash_cs_n := DontCare
  io.spi_flash_sclk := DontCare
  io.spi_flash_mosi := DontCare

  io.i2c_sda := DontCare
  io.i2c_scl := DontCare
  io.i2c_intr := DontCare

  implicit val config:TilelinkConfig = TilelinkConfig(10,32)

  val gen_imem_host = Module(new TilelinkHost())
  val gen_imem_slave = Module(new TilelinkDevice())
  val gen_dmem_host = Module(new DMCache(4, 10, 32)(new TLRequest(), new TLResponse()))
  val gen_dmem_slave = Module(new TilelinkDevice())

  val addressMap = new AddressMap

  addressMap.addDevice(Peripherals.all(configs("DCCM")("id").asInstanceOf[Int]), configs("DCCM")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_dmem_slave)

  if(configs("GPIO")("is").asInstanceOf[Boolean]){
      // GPIO
      val gpio = Module(new Gpio(new TLRequest(), new TLResponse()))
      val gen_gpio_slave = Module(new TilelinkDevice())

      gen_gpio_slave.io.reqOut <> gpio.io.req
      gen_gpio_slave.io.rspIn <> gpio.io.rsp

      // val n = configs("GPIO")("n").asInstanceOf[Int]
      io.gpio_o := gpio.io.cio_gpio_o(n-1,0)
      io.gpio_en_o := gpio.io.cio_gpio_en_o(n-1,0)
      gpio.io.cio_gpio_i := io.gpio_i

      addressMap.addDevice(Peripherals.all(configs("GPIO")("id").asInstanceOf[Int]), configs("GPIO")("baseAddr").asInstanceOf[String].U(32.W), configs("GPIO")("mask").asInstanceOf[String].U(32.W), gen_gpio_slave)
    //
  }
   if(configs("SPI")("is").asInstanceOf[Boolean]){
    val spi = Module(new Spi(new TLRequest(), new TLResponse()))
    val gen_spi_slave = Module(new TilelinkDevice())

    gen_spi_slave.io.reqOut <> spi.io.req
    gen_spi_slave.io.rspIn <> spi.io.rsp

    io.spi_cs_n := spi.io.cs_n
    io.spi_sclk := spi.io.sclk
    io.spi_mosi := spi.io.mosi
    spi.io.miso := io.spi_miso

    addressMap.addDevice(Peripherals.all(configs("SPI")("id").asInstanceOf[Int]), configs("SPI")("baseAddr").asInstanceOf[String].U(32.W), configs("SPI")("mask").asInstanceOf[String].U(32.W), gen_spi_slave)
  }
  if (configs("UART")("is").asInstanceOf[Boolean]){
    val uart = Module(new uart(new TLRequest(), new TLResponse()))
    val gen_uart_slave = Module(new TilelinkDevice())

    gen_uart_slave.io.reqOut <> uart.io.request
    gen_uart_slave.io.rspIn <> uart.io.response

    uart.io.cio_uart_rx_i := io.cio_uart_rx_i
    io.cio_uart_tx_o := uart.io.cio_uart_tx_o
    io.cio_uart_intr_tx_o := uart.io.cio_uart_intr_tx_o  

    addressMap.addDevice(Peripherals.all(configs("UART")("id").asInstanceOf[Int]), configs("UART")("baseAddr").asInstanceOf[String].U(32.W), configs("UART")("mask").asInstanceOf[String].U(32.W), gen_uart_slave)
  }
  if (configs("TIMER")("is").asInstanceOf[Boolean]){
    val timer = Module(new Timer(new TLRequest(), new TLResponse()))
    val gen_timer_slave = Module(new TilelinkDevice())

    gen_timer_slave.io.reqOut <> timer.io.req
    gen_timer_slave.io.rspIn <> timer.io.rsp 

    io.timer_intr_cmp := timer.io.cio_timer_intr_cmp
    io.timer_intr_ovf := timer.io.cio_timer_intr_ovf

    addressMap.addDevice(Peripherals.all(configs("TIMER")("id").asInstanceOf[Int]), configs("TIMER")("baseAddr").asInstanceOf[String].U(32.W), configs("TIMER")("mask").asInstanceOf[String].U(32.W), gen_timer_slave)
  }
  if (configs("SPIF")("is").asInstanceOf[Boolean]){
    val spi_flash = Module(new SpiFlash(new TLRequest(), new TLResponse()))
    val gen_spi_flash_slave = Module(new TilelinkDevice())

    gen_spi_flash_slave.io.reqOut <> spi_flash.io.req
    gen_spi_flash_slave.io.rspIn <> spi_flash.io.rsp

    io.spi_flash_cs_n := spi_flash.io.cs_n
    io.spi_flash_sclk := spi_flash.io.sclk
    io.spi_flash_mosi := spi_flash.io.mosi
    spi_flash.io.miso := io.spi_flash_miso

    addressMap.addDevice(Peripherals.all(configs("SPIF")("id").asInstanceOf[Int]), configs("SPIF")("baseAddr").asInstanceOf[String].U(32.W), configs("SPIF")("mask").asInstanceOf[String].U(32.W), gen_spi_flash_slave)
  }
  if (configs("I2C")("is").asInstanceOf[Boolean]){
    val i2c = Module(new i2c(new TLRequest(), new TLResponse()))
    val gen_i2c_slave = Module(new TilelinkDevice())

    gen_i2c_slave.io.reqOut <> i2c.io.request
    gen_i2c_slave.io.rspIn <> i2c.io.response 

    i2c.io.cio_i2c_sda_in := io.i2c_sda_in
    io.i2c_sda := i2c.io.cio_i2c_sda
    io.i2c_scl := i2c.io.cio_i2c_scl
    io.i2c_intr := i2c.io.cio_i2c_intr

    addressMap.addDevice(Peripherals.all(configs("I2C")("id").asInstanceOf[Int]), configs("I2C")("baseAddr").asInstanceOf[String].U(32.W), configs("I2C")("mask").asInstanceOf[String].U(32.W), gen_i2c_slave)

  }

  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  
  val tlErr = Module(new TilelinkError())
  val core = Module(new Core(new TLRequest, new TLResponse)(M = configs("M")("is").asInstanceOf[Boolean]))


  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new TilelinkMaster(), new TilelinkSlave(), devices.size))

  // TL <-> Core (fetch)
  gen_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> gen_imem_host.io.rspOut
  gen_imem_slave.io.reqOut <> imem.io.req
  gen_imem_slave.io.rspIn <> imem.io.rsp

  // TL <-> TL (fetch)
  gen_imem_host.io.tlMasterTransmitter <> gen_imem_slave.io.tlMasterReceiver
  gen_imem_slave.io.tlSlaveTransmitter <> gen_imem_host.io.tlSlaveReceiver

  // TL <-> Core (memory)
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
                                           "GPIO" -> Map("id" -> 1, "is" -> oneZero("gpio"), "baseAddr" -> baseAddr.GPIO, "mask" -> mask.GPIO, "n" -> 4),
                                           "SPI"  -> Map("id" -> 2, "is" -> oneZero("spi") , "baseAddr" -> baseAddr.SPI , "mask" -> mask.SPI ),
                                           "UART" -> Map("id" -> 3, "is" -> oneZero("uart"), "baseAddr" -> baseAddr.UART, "mask" -> mask.UART),
                                           "TIMER"-> Map("id" -> 4, "is"-> oneZero("timer"), "baseAddr"-> baseAddr.TIMER, "mask"-> mask.TIMER),
                                           "SPIF" -> Map("id" -> 5, "is" -> oneZero("spi_flash"), "baseAddr" -> baseAddr.SPIF, "mask" -> mask.SPIF),
                                           "I2C"  -> Map("id" -> 6, "is" -> oneZero("i2c") , "baseAddr" -> baseAddr.I2C , "mask" -> mask.I2C ),
                                           "M"    -> Map("is" -> oneZero("m")),
                                           "TL"   -> Map("is" -> oneZero("tl")),
                                           "WB"   -> Map("is" -> oneZero("wb")))

  (new ChiselStage).emitVerilog(new GeneratorTLC(programFile=None, configs))
}
