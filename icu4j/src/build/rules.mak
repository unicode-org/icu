# Copyright © 1999-2000, International Business Machines Corporation and others. All Rights Reserved

default: classes

JAVAC=      javac -classpath $(CLASSPATH) -sourcepath $(SRCDIR) -d $(CLASSDIR) $(JAVACFLAGS)

.SUFFIXES: .java .class 

!ifdef FILES_JAVA
FILES_java=  $(FILES_java:/=\)

!endif
FILES_class= $(FILES_java:.java=.class)
CLASSDIR= $(CLASSDIR:/=\)
CLASSLIST= classes.list

CLASS_DEST_DIR= $(CLASSDIR)/$(PKGPATH)

$(CLASSDIR) :
	mkdir $@

{$(SRCDIR)\$(PKGPATH)}.java{$(CLASSDIR)\$(PKGPATH)}.class:
	@echo $(?) >> $(CLASSLIST)

!ifdef FILES_dict
FILES_dict= $(FILES_dict:/=\)
SRC_FILES_dict= $(FILES_dict:classes=src)

!endif

classes: delete.classlist $(CLASSDIR) $(FILES_class) $(FILES_dics)
	@if exist $(CLASSLIST) echo Compiling {
	@if exist $(CLASSLIST) cat $(CLASSLIST)
	if exist $(CLASSLIST) $(JAVAC) @$(CLASSLIST)
	rm -f $(CLASSLIST)
!ifdef FILES_dict
# 	cp $(SRC_FILES_dict) $(TARGDIR)
	cp $(SRC_FILES_dict) $(CLASSDIR)\com\ibm\text\resources\
!endif
!ifdef SUBDIRS
	@for %%d in ( $(SUBDIRS) ) do cd %d && $(MAKE) classes && cd ..
!endif

dict: $(SRC_FILES_dict)
	echo $(FILES_dict)
	echo $(SRC_FILES_dict)
	cp $(SRC_FILES_dict) $(CLASSDIR)\com\ibm\text\resources\
#	cp $(SRC_FILES_dict) $(TARGDIR)

delete.classlist:
	@rm -f $(CLASSLIST)

clean:
	rm -rf ..\classes\com
	rm -rf ..\docs
#!ifdef FILES_java
#	rm -f $(FILES_class)
#!endif
#!ifdef FILES_dict
#    rm -f $(FILES_dict)
#!endif
#!ifdef SUBDIRS
#	@for %%d in ( $(SUBDIRS) ) do cd %d && nmake -nologo clean && cd ..
#!endif
	
FORCE: ;


