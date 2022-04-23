#!/usr/bin/python3

# def peripheralSwitch():
# 	first_comb = """
# 	package caravan.bus.common
# 	import chisel3._
# 	import chisel3.experimental.ChiselEnum
# 	object Peripherals extends ChiselEnum {
# 	val DCCM = Value(0.U)
# 	val GPIO = Value(1.U)
# 	}"""

# 	second_comb = """
# 	package caravan.bus.common
# 	import chisel3._
# 	import chisel3.experimental.ChiselEnum

# 	object Peripherals extends ChiselEnum {
# 	val DCCM = Value(0.U)
# 	val GPIO = Value(1.U)
# 	val SPI  = Value(2.U)
# 	}"""

# 	third_comb = """
# 	package caravan.bus.common
# 	import chisel3._
# 	import chisel3.experimental.ChiselEnum

# 	object Peripherals extends ChiselEnum {
# 	val DCCM = Value(0.U)
# 	val GPIO = Value(1.U)
# 	val UART = Value(2.U)
# 	}"""

# 	fourth_comb = """
# 	package caravan.bus.common
# 	import chisel3._
# 	import chisel3.experimental.ChiselEnum

# 	object Peripherals extends ChiselEnum {
# 	val DCCM = Value(0.U)
# 	val GPIO = Value(1.U)
# 	val SPI  = Value(2.U)
# 	val UART = Value(3.U)
# 	}"""

# 	import json,os
# 	from xmlrpc.client import Boolean
# 	file = open("src/main/scala/config.json", "r")
# 	data = json.load(file)
# 	print(data)
# 	file.close()
# 	uart = bool(data["uart"])
# 	spi_flash = bool(data["spi_flash"])
# 	print(spi_flash)
# 	caravan_path = "caravan/src/main/scala/caravan/bus/common/PeripheralsMap.scala"
# 	if not uart and not spi_flash:
# 		print(first_comb)
# 		caravan1 = open(caravan_path, "w")
# 		caravan1.write(first_comb)
# 		caravan1.close()

# 	elif not uart and spi_flash:
# 		print(second_comb)
# 		caravan1 = open(caravan_path, "w")
# 		caravan1.write(second_comb)
# 		caravan1.close()

# 	elif uart and not spi_flash:
# 		print(third_comb)
# 		caravan1 = open(caravan_path, "w")
# 		caravan1.write(third_comb)
# 		caravan1.close()

# 	elif uart and spi_flash:
# 		print(fourth_comb)
# 		caravan1 = open(caravan_path, "w")
# 		caravan1.write(fourth_comb)
# 		caravan1.close()

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

if data["spi_flash"] == 1:
	spi_f = f"""
		val SPI  = Value({dev_count}.U)
	"""
	main_str += spi_f
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