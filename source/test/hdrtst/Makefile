#
#  Copyright (c) 1999-2002, International Business Machines Corporation and
#  others. All Rights Reserved.
#
#
#  THE PURPOSE OF THIS TEST
#
#     This tests all public header files (
#  
#     To run it simply type 'make check'. You SHOULD see:
#
#  unicode/uchar.h -	0
#  unicode/uchriter.h -	0
#  unicode/ucnv.h -	0
#
#    .. etc.  Anything other than zero is an error. (except for the deprecation tests, where '1' is the correct value)
#              
#
#  If a header fails the C compile because it's a C++ header, add it to the
#  file named 'cxxfiles.txt' in this directory.
#
#  If a header fails because it is deprecated, add it to the 'dfiles.txt'
#
#

ifeq ($(USE_ICU_CONFIG),1)
	include $(shell icu-config --incfile)
	DIRS=$(shell icu-config --prefix)/include/unicode
else
	include ../../config/Makefile.inc
	DIRS=../../../common/unicode ../../../i18n/unicode ../../../extra/ustdio/unicode
endif

all: 
	@echo Please read this Makefile for more information.
	@echo run \'$(MAKE) check\' to run the test 

check: hdrdir dtest ctest cpptest doclean
	@echo "All header tests have finished. If deprecation #errors appeared, "
	@echo "       add files to hdrtst/dfiles.txt"
	@echo "#################################################################"
	@echo

# break dependency
doclean:
	make clean

#layout not tested due to path dependencies

hdrdir:
	@echo CPPFLAGS are $(CPPFLAGS)
	@-$(RMV) ./unicode
	@mkdir ./unicode
	@echo Preparing unicode/ directory ..
	@cd ./unicode ; \
	for dir in $(DIRS); do \
	  echo "linking from " $$dir '(' `ls $$dir/*.h | wc -l` 'headers )'  ; \
	  ln -s $$dir/*.h . ; \
	done
	rm unicode/pwin32.h

cpptest:
	@echo "C++ test #######################################################"
	@rm -f Makefile.sub
	@echo 'Testing all header files under C++'
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

dtest:
	@rm -f Makefile.sub
	@echo "Deprecation #######################################################"
	@echo 'Should be 1 in each case if file is properly obsoleted.'
	@for stub in `cat dfiles.txt | grep -v '^#'` ; do \
	  file=unicode/$$stub \
	  incfile=`basename $$file .h` ; \
	  echo -n "C++  unicode/$$incfile.h 	- " ; \
	  echo '#include "'unicode/$$incfile'.h"' > ht_$$incfile.cpp ; \
	  echo 'void junk(){}' >> ht_$$incfile.cpp ; \
	  echo > ht_dep.junk ; \
          $(COMPILE.cc) -c -I. ht_$$incfile.cpp 2>&1 | sed -e 's/^.*#error[^"]*"//' | fgrep -v ht_ | tee ht_dep.junk | fgrep -v "$$incfile.h header is obsolete" ; \
	  RES=$$? ; \
	  echo $$RES ; \
	  fgrep -q "$$incfile.h header is obsolete" ht_dep.junk || echo "** FAIL Header unicode/$$incfile.h is not obsoleted properly" ; \
	  rm -f unicode/$$stub ; \
	done

#  < ht_dep.junk &&  ; \


ctest:
	@echo "C Test #######################################################"
	@echo "  (if any C++ files show up here, stop the test and add them to hdrtst/cxxfiles.txt) "
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
	-@rm -f *.h *.c *.cpp *.o *.junk
	-@$(RMV) ./unicode

