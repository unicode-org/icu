#
#  Copyright (c) 1999-2000, International Business Machines Corporation and
#  others. All Rights Reserved.
#
#
#  THE PURPOSE OF THIS TEST
#
#     This test will  try building all ICU public files
#      (../../common/unicode/*.h and ../../common/i18n/unicode/*.h)
#     by themselves. it uses C++ becuase we don't have a list of which
#     headers are C and which are C++.  [Future opportunity!]
#  
#     To run it simply type 'make check'. You SHOULD see:
#
#  unicode/uchar.h -	0
#  unicode/uchriter.h -	0
#  unicode/ucnv.h -	0
#
#    .. etc.  Anything other than zero is an error.
#              
#
#

include ../../config/Makefile

CPPFLAGS=-I.

all: 
	@echo Please read this Makefile for more information.
	@echo run \'$(MAKE) check\' to run the test 

check: hdrdir genfiles

hdrdir:
	-$(RMV) ./unicode
	mkdir ./unicode
	( cd ./unicode ; ln -s ../../../common/unicode/*.h ../../../i18n/unicode/*.h . )

genfiles:
	@rm -f Makefile.sub
	@echo Building test harness for header files in ../../common and ../../i18n
	@echo 'the number after the hyphen (-) refers to the exit code - should be zero!'
	@for file in ../../common/unicode/*.h ../../i18n/unicode/*.h; do \
	  incfile=`basename $$file .h` ; \
	  echo -n "  unicode/$$incfile.h 	- " ; \
	  echo '#include "'unicode/$$incfile'.h"' > ht_$$incfile.cpp ; \
	  echo 'junk(){}' >> ht_$$incfile.cpp ; \
          $(COMPILE.cc) -c -I. ht_$$incfile.cpp ; \
	  RES=$$? ; \
	  echo $$RES ; \
	done
	make clean

distclean clean:
	-rm -f *.h *.cpp *.o
	-$(RMV) ./unicode
