default: classes

JAVAC=      javac -classpath $(CLASSPATH) -sourcepath $(SRCDIR) -d $(CLASSDIR) $(JAVACFLAGS)

.SUFFIXES: .java .class 

!ifdef FILES_JAVA
FILES_java=  $(FILES_java:/=\)

!endif
FILES_class= $(FILES_java:.java=.class)

CLASSLIST= classes.list

{$(SRCDIR)\$(PKGPATH)}.java{$(CLASSDIR)\$(PKGPATH)}.class:
	@echo $(?) >> $(CLASSLIST)

!ifdef FILES_dict
FILES_dict= $(FILES_dict:/=\)
SRC_FILES_dict= $(FILES_dict:classes=src)

!endif

classes: delete.classlist $(FILES_class) $(FILES_dics)
	@if exist $(CLASSLIST) echo Compiling {
	@if exist $(CLASSLIST) cat $(CLASSLIST)
	if exist $(CLASSLIST) $(JAVAC) @$(CLASSLIST)
	rm -f $(CLASSLIST)
!ifdef FILES_dict
    cp $(SRC_FILES_dict) $(TARGDIR)
!endif
!ifdef SUBDIRS
	@for %%d in ( $(SUBDIRS) ) do cd %d && nmake -nologo classes && cd ..
!endif

dict: $(SRC_FILES_dict)
	echo $(FILES_dict)
	echo $(SRC_FILES_dict)
	cp $(SRC_FILES_dict) $(CLASSDIR)/$(PKGPATH)/

delete.classlist:
	@rm -f $(CLASSLIST)

clean:
	rm -rf ..\classes\com
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


