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
#  If a header fails the C compile because it's a C++ header, add it to the
#  file named 'cxxfiles.txt' in this directory.
#
#

include ../../config/Makefile.inc

CPPFLAGS=-I.

all: 
	@echo Please read this Makefile for more information.
	@echo run \'$(MAKE) check\' to run the test 

check: hdrdir ctest cpptest doclean

# break dependency
doclean:
	make clean

#layout not tested for path dependencies

hdrdir:
	-$(RMV) ./unicode
	mkdir ./unicode
	( cd ./unicode ; ln -s ../../../common/unicode/*.h ../../../i18n/unicode/*.h  ../../../extra/ustdio/unicode/*.h  . )
	rm unicode/pwin32.h

cpptest:
	@rm -f Makefile.sub
	@echo Building test harness for header files in ../../common and ../../i18n
	@echo 'the number after the hyphen (-) refers to the exit code - should be zero!'
	@for file in unicode/*.h; do \
	  incfile=`basename $$file .h` ; \
	  echo -n "C++  unicode/$$incfile.h 	- " ; \
	  echo '#include "'unicode/$$incfile'.h"' > ht_$$incfile.cpp ; \
	  echo 'void junk(){}' >> ht_$$incfile.cpp ; \
          $(COMPILE.cc) -c -I. ht_$$incfile.cpp ; \
	  RES=$$? ; \
	  echo $$RES ; \
	done

ctest:
	@echo "Now, C test"
	@rm -f Makefile.sub
	@echo Building test harness for header files in ../../common and ../../i18n
	@echo 'the number after the hyphen (-) refers to the exit code - should be zero!'
	@for file in `ls unicode/*.h | fgrep -v -f ./cxxfiles.txt | tee cfiles.txt`; do \
	  incfile=`basename $$file .h` ; \
	  echo -n "C    unicode/$$incfile.h 	- " ; \
	  echo '#include "'unicode/$$incfile'.h"' > ht_$$incfile.c ; \
	  echo 'void junk(){}' >> ht_$$incfile.c ; \
          $(COMPILE.c) -c -I. ht_$$incfile.c ; \
	  RES=$$? ; \
	  echo $$RES ; \
	done

distclean clean:
	-rm -f *.h *.c *.cpp *.o
	-$(RMV) ./unicode

