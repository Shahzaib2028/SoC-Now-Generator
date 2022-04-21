#!/usr/bin/python3

from time import time


def peripheralSwitch():
	first_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	}"""

	second_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI  = Value(2.U)
	}"""

	third_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val UART = Value(2.U)
	}"""

	fourth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI  = Value(2.U)
	val UART = Value(3.U)
	}"""

	fifth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val TIMER = Value(2.U)
	}"""

	sixth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val I2C = Value(2.U)
	}"""

	seventh_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val I2C = Value(2.U)
	val TIMER = Value(3.U)
	}"""

	eighth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val I2C = Value(2.U)
	val UART = Value(3.U)
	}"""

	nineth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val I2C = Value(2.U)
	val UART = Value(3.U)
	val TIMER = Value(4.U)
	}"""

	tenth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI = Value(2.U)
	val UART = Value(3.U)
	val TIMER = Value(4.U)
	}"""

	eleventh_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI = Value(2.U)
	val I2C = Value(3.U)
	val TIMER = Value(4.U)
	}"""

	twelfth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI = Value(2.U)
	val I2C = Value(3.U)
	val UART = Value(4.U)
	}"""

	thirteenth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI = Value(2.U)
	val I2C = Value(3.U)
	}"""

	fourteenth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val SPI = Value(2.U)
	val TIMER = Value(3.U)
	}"""

	sixteenth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val UART = Value(2.U)
	val TIMER = Value(3.U)
	}"""

	seventeenth_comb = """
	package caravan.bus.common
	import chisel3._
	import chisel3.experimental.ChiselEnum

	object Peripherals extends ChiselEnum {
	val DCCM = Value(0.U)
	val GPIO = Value(1.U)
	val UART = Value(2.U)
	val TIMER = Value(3.U)
	val SPI = Value(4.U)
	val I2C = Value(5.U)
	}"""

	import json,os
	from xmlrpc.client import Boolean
	file = open("src/main/scala/config.json", "r")
	data = json.load(file)
	print(data)
	file.close()
	uart = bool(data["uart"])
	spi_flash = bool(data["spi_flash"])
	timer = bool(data["timer"])
	i2c = bool(data["i2c"])
	print(spi_flash)
	caravan_path = "caravan/src/main/scala/caravan/bus/common/PeripheralsMap.scala"


	if not uart and not spi_flash and not timer and not i2c:
		print(first_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(first_comb)
		caravan1.close()

	elif not uart and not timer and not i2c and spi_flash:
		print(second_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(second_comb)
		caravan1.close()

	elif uart and not spi_flash and not timer and not i2c:
		print(third_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(third_comb)
		caravan1.close()

	elif uart and spi_flash and not timer and not i2c:
		print(fourth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(fourth_comb)
		caravan1.close()

	elif not uart and not spi_flash and not i2c and timer:
		print(fifth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(fifth_comb)
		caravan1.close()

	elif not uart and not spi_flash and i2c and not timer:
		print(sixth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(sixth_comb)
		caravan1.close()

	elif not uart and not spi_flash and i2c and timer:
		print(seventh_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(seventh_comb)
		caravan1.close()

	elif uart and not spi_flash and i2c and not timer:
		print(eighth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(eighth_comb)
		caravan1.close()

	elif uart and not spi_flash and i2c and timer:
		print(nineth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(nineth_comb)
		caravan1.close()

	elif uart and spi_flash and not i2c and timer:
		print(tenth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(tenth_comb)
		caravan1.close()

	elif not uart and spi_flash and i2c and timer:
		print(eleventh_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(eleventh_comb)
		caravan1.close()

	elif uart and spi_flash and i2c and not timer:
		print(twelfth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(twelfth_comb)
		caravan1.close()

	elif not uart and spi_flash and i2c and not timer:
		print(thirteenth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(thirteenth_comb)
		caravan1.close()

	elif not uart and spi_flash and not i2c and timer:
		print(fourteenth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(fourteenth_comb)
		caravan1.close()

	elif uart and not spi_flash and not i2c and timer:
		print(sixteenth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(sixteenth_comb)
		caravan1.close()

	elif uart and spi_flash and i2c and timer:
		print(seventeenth_comb)
		caravan1 = open(caravan_path, "w")
		caravan1.write(seventeenth_comb)
		caravan1.close()

peripheralSwitch()