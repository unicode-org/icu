# Copyright (c) 2008-2009 IBM Corp. and Others. All Rights Reserved

# use version 4.2.0.1
TESTER_VER=4_2_0_1
TESTER=coldiff
TESTERS=$(TESTER)_$(TESTER_VER)
testers: $(TESTERS)

$(TESTER)_$(TESTER_VER).o: $(TESTER).cpp
	$(shell $(BUILD)/$(TESTER_VER)/config/icu-config $(ICU_CONFIG_CXX))  -c -I$(TOP)/$(BUILD)/$(TESTER_VER)/gluren/include -DICUGLUE_VER=$$ver -o $@ $< 

$(TESTER)_$(TESTER_VER): $(TESTER)_$(TESTER_VER).o glout/$(TESTER_VER)/*.o build/3_8_1/*/*.ao build/4_0_1/*/*.ao build/4_0_1/data/out/tmp/icudt40l_dat.o build/3_8_1/data/out/tmp/icudt38l_dat.o 
	g++  -g  -dead_strip -o $@  $(TESTER)_$(TESTER_VER).o  glout/$(TESTER_VER)/*.o -Linst/$(TESTER_VER)/lib/ -Lbuild/$(TESTER_VER)/data/out/ -lsicuuc -lsicui18n -licudt42l build/3_8_1/*/*.ao build/4_0_1/*/*.ao build/4_0_1/data/out/tmp/icudt40l_dat.o build/3_8_1/data/out/tmp/icudt38l_dat.o  build/4_3_1/*/*.ao 


