#!/usr/bin/python3

import json

base = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum
	"""

dccm = """
	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
"""
closer = "}"
file1 = open("src/main/scala/config.json")
data = json.load(file1)
file1.close()

main_str = base + dccm
dev_count = 1

if data["gpio"] == 1:
	gpio = f"""
		val GPIO = Value({dev_count}.U)
	"""
	main_str += gpio
	dev_count += 1

if data["spi"] == 1:
	spi = f"""
		val SPI  = Value({dev_count}.U)
	"""
	main_str += spi
	dev_count += 1

if data["uart"] == 1:
	uart = f"""
		val UART = Value({dev_count}.U)
	"""
	main_str += uart
	dev_count += 1

if data["timer"] == 1:
	timer = f"""
		val TIMER = Value({dev_count}.U)
	"""
	main_str += timer
	dev_count += 1

if data["spi_flash"] == 1:
	spi_f = f"""
		val SPIFLASH  = Value({dev_count}.U)
	"""
	main_str += spi_f
	dev_count += 1

if data["i2c"] == 1:
	i2c = f"""
		val I2C = Value({dev_count}.U)
	"""
	main_str += i2c
	dev_count += 1

main_str += closer

print(main_str)

caravan_path = "caravan/src/main/scala/caravan/bus/common/PeripheralsMap.scala"
file2 = open(caravan_path, "w")
file2.write(main_str)
file2.close()
