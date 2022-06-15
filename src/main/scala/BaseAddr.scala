case class BaseAddr(
    DCCM            : String = "h40000000",
    GPIO            : String = "h40000000",
    SPI             : String = "h40000000",
    UART            : String = "h40000000",
    SPIF            : String = "h40000000",
    I2C             : String = "h40000000",
    TIMER           : String = "h40000000"
)

case class Mask(
    DCCM            : String = "h00000fff",
    GPIO            : String = "h00000fff",
    SPI             : String = "h00000fff",
    UART            : String = "h00000fff",
    SPIF            : String = "h00000fff",
    I2C             : String = "h00000fff",
    TIMER           : String = "h00000fff"
)