current_dir = $(shell pwd)


rtl:
	python3 ./peripheralScript.py
	sbt "runMain GeneratorDriver"

relocate: Generator.v 
	mv -t fpga/ Generator.v 

cleanup: relocate
	rm -f firrtl_black_box_resource_files.f Generator.fir Generator.anno.json

initmem:
	python3 insertmem.py

clean: 
	rm -rf $(current_dir)/fpga/build/
	rm $(current_dir)/fpga/Generator.v 

bitstream:  rtl initmem cleanup 
	TARGET="arty_35" $(MAKE) -C fpga

upload: $(current_dir)/fpga/build/arty_35/Generator.bit
	openocd -f ${INSTALL_DIR}/${FPGA_FAM}/conda/envs/${FPGA_FAM}/share/openocd/scripts/board/digilent_arty.cfg -c "init; pld load 0 $(current_dir)/fpga/build/arty_35/Generator.bit; exit"


