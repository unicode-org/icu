/******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  winmode.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000july14
*   created by: Vladimir Weinstein
*
*   This program packages the ICU data into different forms
*   (DLL, common data, etc.) 
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"

#define WINBUILDMODE "debug"

void writeCmnRules(UPKGOptions *o,  FileStream *makefile, CharList **objects)
{
  char *p, *baseName;
  char tmp[1024];
  char stanza[1024];
  char cfile[1024];
  CharList *oTail = NULL;
  CharList *infiles;
  CharList *parents = NULL, *commands = NULL;

  infiles = o->filePaths;

  sprintf(tmp, "$(CMNTARGET) : $(DATAFILEPATHS)\n\t@$(GENCMN) -C \"%s\" -d %s -n %s 10000 <<\n", 
	  o->comment, o->targetDir, o->shortName);
    T_FileStream_writeLine(makefile, tmp);

  for(;infiles;infiles = infiles->next) {
	  sprintf(tmp, "%s\n", infiles->str);
	  T_FileStream_writeLine(makefile, tmp);
  }
  sprintf(tmp, "<<\n");
  T_FileStream_writeLine(makefile, tmp);
}



void pkg_mode_windows(UPKGOptions *o, FileStream *makefile, UErrorCode *status) {
  char tmp[1024];
  char tmp2[1024];
  CharList *tail = NULL;
  CharList *objects = NULL;
  CharList *iter;
  const char *dataDir =   u_getDataDirectory(); /* we need data directory to know where to look for tools */
  const char *separator = dataDir[uprv_strlen(dataDir)-1]=='\\'?"":"\\";
  UBool isDll = (uprv_strcmp(o->mode, "dll") == 0);

  if(U_FAILURE(*status)) { 
    return;
  }

  sprintf(tmp2,
	  "GENCMN = %s%s..\\source\\tools\\gencmn\\%s\\gencmn.exe\n",
	  dataDir, separator, WINBUILDMODE);
  T_FileStream_writeLine(makefile, tmp2);

  if(isDll) {
      uprv_strcpy(tmp, LIB_PREFIX);
      uprv_strcat(tmp, o->shortName);
      uprv_strcat(tmp, UDATA_SO_SUFFIX);

      if(o->nooutput || o->verbose) {
        fprintf(stderr, "# Output %s file: %s%s%s\n", UDATA_SO_SUFFIX, o->targetDir, U_FILE_SEP_STRING, tmp);
      }

      if(o->nooutput) {
        *status = U_ZERO_ERROR;
        return;
      }

      sprintf(tmp2, "# DLL file to make:\nDLLTARGET=%s\n\n", tmp);
      T_FileStream_writeLine(makefile, tmp2);

	  sprintf(tmp2, 
		  "LINK32 = link.exe\n"
		  "LINK32_FLAGS = /out:\"$(TARGETDIR)\\$(DLLTARGET)\" /DLL /NOENTRY /base:\"0x4ad00000\" /comment:\"%s\"\n",
		  o->comment
		);
      T_FileStream_writeLine(makefile, tmp2);

	  sprintf(tmp2,
		  "GENCCODE = %s%s..\\source\\tools\\genccode\\%s\\genccode.exe\n",
		  dataDir, separator, WINBUILDMODE);
      T_FileStream_writeLine(makefile, tmp2);



      uprv_strcpy(tmp, UDATA_CMN_PREFIX);
      uprv_strcat(tmp, o->shortName);
      uprv_strcat(tmp, UDATA_CMN_INTERMEDIATE_SUFFIX);
      uprv_strcat(tmp, OBJ_SUFFIX);

      sprintf(tmp2, "# intermediate obj file:\nCMNOBJTARGET=%s\n\n", tmp);
      T_FileStream_writeLine(makefile, tmp2);
  }
  uprv_strcpy(tmp, UDATA_CMN_PREFIX);
  uprv_strcat(tmp, o->shortName);
  uprv_strcat(tmp, UDATA_CMN_SUFFIX);
  
   

	if(o->nooutput || o->verbose) {
	  fprintf(stderr, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
	}

	if(o->nooutput) {
	  *status = U_ZERO_ERROR;
	  return;
	}

    sprintf(tmp2, "# common file to make:\nCMNTARGET=%s\n\n", tmp);
    T_FileStream_writeLine(makefile, tmp2);

  if(isDll) {
      sprintf(tmp, "all: $(DLLTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);

      sprintf(tmp, "$(DLLTARGET): $(CMNOBJTARGET)\n"
				    "\t@$(LINK32) $(LINK32_FLAGS) $?\n\n");
      T_FileStream_writeLine(makefile, tmp);
      sprintf(tmp, "$(CMNOBJTARGET): $(CMNTARGET)\n"
				    "\t@$(GENCCODE) $(GENCOPTIONS) -o $(TARGETDIR) $(TARGETDIR)\\$(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);
  } else {
      sprintf(tmp, "all: $(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);
  }

	/* Write compile rules */
  writeCmnRules(o, makefile, &objects);
}

void pkg_mode_dll(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{
  char tmp[1024];
  CharList *tail = NULL;
  CharList *objects = NULL;
  CharList *iter;

#ifdef WIN32
  
  if(U_FAILURE(*status)) {
    fprintf(stderr, "# Dllmode: Error importing rules for 'common'.\n");
    return;
  }
  
  T_FileStream_writeLine(makefile, "ifneq ($(strip $(HPUX_JUNK_OBJ)),)\n"
                                   "  HPUX_JUNK_OBJ=$(TEMP_DIR)/$(HPUX_JUNK_OBJ)\n"
                                   "endif\n\n");


  T_FileStream_writeLine(makefile, "CLEANFILES= $(CMNLIST) $(TARGET) $(TARGETDIR)\$(DLLTARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
  T_FileStream_writeLine(makefile, "\n\n");
  
  T_FileStream_writeLine(makefile, "install: $(TARGETDIR)\$(DLLTARGET)\n"
                                   "\tCOPY $(TARGETDIR)\$(DLLTARGET) $(INSTALL)/$(DLLTARGET)\n\n");
  
#else
  /* begin writing makefile ========================= */
  

  sprintf(tmp, "# File to make:\nTARGET=%s\n\n", o->outFiles->str);
  T_FileStream_writeLine(makefile, tmp);

  sprintf(tmp, "all: $(TARGETDIR)/$(TARGET)\n\n");
  T_FileStream_writeLine(makefile, tmp);

  /* Write compile rules */
  writeObjRules(o, makefile, &objects);

  sprintf(tmp, "# List file for gencmn:\n"
          "CMNLIST=%s%s%s_dll.lst\n\n",
          o->tmpDir,
          U_FILE_SEP_STRING,
          o->shortName);
  T_FileStream_writeLine(makefile, tmp);

  if(o->hadStdin == FALSE) { /* shortcut */
    T_FileStream_writeLine(makefile, "$(CMNLIST): $(LISTFILES)\n"
                                   "\tcat $(LISTFILES) > $(CMNLIST)\n\n");
  } else {
    T_FileStream_writeLine(makefile, "$(CMNLIST): \n"
                                   "\t@echo Generating $@ list of data files\n"
                                   "\t@-$(RMV) $@\n"
                                   "\t@for file in $(DATAFILEPATHS); do \\\n"
                                   "\t  echo $$file >> $@; \\\n"
                                   "\tdone;\n\n");
  }

  T_FileStream_writeLine(makefile, "# 'TOCOBJ' contains C Table of Contents objects [if any]\n");
  if(!strcmp(o->shortName, "icudata")) {
    T_FileStream_writeLine(makefile, "$(TEMP_DIR)/icudata_dat.c: $(CMNLIST)\n\n"
                           "\t$(TOOL) $(GENCMN) -S -d $(TEMP_DIR) 0 $(CMNLIST)\n\n");
    sprintf(tmp, "TOCOBJ= icudata_dat%s \n\n", OBJ_SUFFIX);
    T_FileStream_writeLine(makefile, tmp);
  }

  T_FileStream_writeLine(makefile, "BASE_OBJECTS= $(TOCOBJ) ");

  pkg_writeCharListWrap(makefile, objects, " ", " \\\n");
  T_FileStream_writeLine(makefile, "\n\n");
  T_FileStream_writeLine(makefile, "OBJECTS=$(BASE_OBJECTS:%=$(TEMP_DIR)/%)\n\n");

#if 0
  for(iter=suffixes; iter; iter = iter->next) {
    sprintf(tmp, "$(TEMP_DIR)/%%_%s.c: %%.%s\n\t$(TOOL) $(GENCCODE) -d $(TEMP_DIR) $<\n\n",
            iter->str,iter->str);
    T_FileStream_writeLine(makefile, tmp);
  }
#endif

#if 0
  for(iter=objects; iter; iter = iter->next) {
    char sourcefile[200];
    strcpy(sourcefile,iter->str);
    strcpy(sourcefile+strlen(sourcefile)-strlen(OBJ_SUFFIX), ".c" );
    sprintf(tmp, "$(TEMP_DIR)/%s: $(TEMP_DIR)/%s\n\t$(COMPILE.cc) -o $@ $<\n\n",
            iter->str,sourcefile);
    T_FileStream_writeLine(makefile, tmp);
  }
#endif

  T_FileStream_writeLine(makefile,"$(TEMP_DIR)/%.o: $(TEMP_DIR)/%.c\n\t  $(COMPILE.cc) -o $@ $<\n\n");

  T_FileStream_writeLine(makefile,"build-objs: $(SOURCES) $(OBJECTS)\n\n$(OBJECTS): $(SOURCES)\n\n");
  
  T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(TARGET): $(OBJECTS) $(HPUX_JUNK_OBJ) $(LISTFILES)\n"
                                   "\t$(SHLIB.c) -o $@ $(OBJECTS) $(HPUX_JUNK_OBJ)\n"
                                   "\t-ls -l $@\n\n");

  T_FileStream_writeLine(makefile, "$(TEMP_DIR)/hpux_junk_obj.cpp:\n"
                                   "	echo \"void to_emit_cxx_stuff_in_the_linker(){}\" >> hpux_junk_obj.cpp\n"
                                   "\n"
                                   "$(TEMP_DIR)/hpux_junk_obj.o: $(TEMP_DIR)/hpux_junk_obj.cpp\n"
                                   "	$(COMPILE.cc) -o $@ $<\n"
                                   "\n");

  T_FileStream_writeLine(makefile, "CLEANFILES= $(OBJECTS) $(HPUX_JUNK_OBJ) $(TARGETDIR)/$(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
  T_FileStream_writeLine(makefile, "\n\n");
  
  T_FileStream_writeLine(makefile, "install: $(TARGETDIR)/$(TARGET)\n"
                                   "\t$(INSTALL-S) $(TARGETDIR)/$(TARGET) $(INSTALLTO)/$(TARGET)\n\n");

#endif /* NOT win32 */

*status = U_ZERO_ERROR;

}

void pkg_mode_common(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{

	  char tmp[1024];
  CharList *tail = NULL;

  uprv_strcpy(tmp, UDATA_CMN_PREFIX);
  uprv_strcat(tmp, o->shortName);
  uprv_strcat(tmp, UDATA_CMN_SUFFIX);
  
  if(!uprv_strcmp(o->mode, "common")) {
    /* If we're not the main mode.. don't change the output file list */
    
    /* We should be the only item. So we don't care about the order. */
    o->outFiles = pkg_appendToList(o->outFiles, &tail, uprv_strdup(tmp));
    
    if(o->nooutput || o->verbose) {
      fprintf(stderr, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
    }
    
    if(o->nooutput) {
      *status = U_ZERO_ERROR;
      return;
    }
    
    sprintf(tmp, "# File to make:\nTARGET=%s%s%s\n\nTARGETNAME=%s\n", o->targetDir,
            U_FILE_SEP_STRING,
            o->outFiles->str,
            o->outFiles->str);
    T_FileStream_writeLine(makefile, tmp);
  } else {
    /* We're in another mode. but, set the target so they can find us.. */
    T_FileStream_writeLine(makefile, "TARGET=");
    T_FileStream_writeLine(makefile, tmp);
    T_FileStream_writeLine(makefile, "\n\n");
    
  } /* end [check to make sure we are in mode 'common' ] */
  
  sprintf(tmp, "# List file for gencmn:\n"
          "CMNLIST=%s%s%s_common.lst\n\n",
          o->tmpDir,
          U_FILE_SEP_STRING,
          o->shortName);
  T_FileStream_writeLine(makefile, tmp);

  sprintf(tmp, "all: $(TARGET)\n\n");
  T_FileStream_writeLine(makefile, tmp);
  
  T_FileStream_writeLine(makefile, "$(TARGET): $(CMNLIST) $(DATAFILEPATHS)\n"
               "\t$(TOOL) $(GENCMN) -n $(NAME) -c -d $(TARGETDIR) 10000000 $(CMNLIST)\n\n");

#ifdef WIN32
  if(o->hadStdin == FALSE) { /* shortcut */
    T_FileStream_writeLine(makefile, "$(CMNLIST): $(LISTFILES)\n"
                                   "\tcat $(LISTFILES) > $(CMNLIST)\n\n");
  } else {
    T_FileStream_writeLine(makefile, "$(CMNLIST): \n"
                                   "\t@echo Generating $@ list of data files\n"
                                   "\t@-$(RMV) $@\n"
                                   "\t@for file in $(DATAFILEPATHS); do \\\n"
                                   "\t  echo $$file >> $@; \\\n"
                                   "\tdone;\n\n");
  }

  if(!uprv_strcmp(o->mode, "common")) { /* only install/clean in our own mode */
    T_FileStream_writeLine(makefile, "CLEANFILES= $(CMNLIST) $(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
    T_FileStream_writeLine(makefile, "\n\n");
    
    sprintf(tmp, "install: $(TARGET)\n"
            "\t$(INSTALL-S) $(TARGET) $(INSTALLTO)%s$(TARGETNAME)\n\n",
            U_FILE_SEP_STRING);

    T_FileStream_writeLine(makefile, tmp);

  }
#else

  if(o->hadStdin == FALSE) { /* shortcut */
    T_FileStream_writeLine(makefile, "$(CMNLIST): $(LISTFILES)\n"
                                   "\tcat $(LISTFILES) > $(CMNLIST)\n\n");
  } else {
    T_FileStream_writeLine(makefile, "$(CMNLIST): \n"
                                   "\t@echo Generating $@ list of data files\n"
                                   "\t@-$(RMV) $@\n"
                                   "\t@for file in $(DATAFILEPATHS); do \\\n"
                                   "\t  echo $$file >> $@; \\\n"
                                   "\tdone;\n\n");
  }

  if(!uprv_strcmp(o->mode, "common")) { /* only install/clean in our own mode */
    T_FileStream_writeLine(makefile, "CLEANFILES= $(CMNLIST) $(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
    T_FileStream_writeLine(makefile, "\n\n");
    
    sprintf(tmp, "install: $(TARGET)\n"
            "\t$(INSTALL-S) $(TARGET) $(INSTALLTO)%s$(TARGETNAME)\n\n",
            U_FILE_SEP_STRING);

    T_FileStream_writeLine(makefile, tmp);

  }

#endif
}

