/******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  pkgdata.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000may15
*   created by: Steven \u24C7 Loomis
*
*   This program packages the ICU data into different forms
*   (DLL, common data, etc.) 
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"

void findSuffixesAndObjects(CharList *infiles, CharList **suffixes, CharList **objects)
{
  char *p;
  char tmp[1024];
  CharList *sTail = NULL, *oTail = NULL;

  for(;infiles;infiles = infiles->next) {
    p = uprv_strrchr(infiles->str, '.');
    if( (p == NULL) || (*p == '\0' ) ) {
      continue;
    }

    uprv_strncpy(tmp, infiles->str, p-infiles->str);
    p++;

    uprv_strcpy(tmp+(p-1-infiles->str), "_"); /* to append */
    uprv_strcat(tmp, p);
    uprv_strcat(tmp, OBJ_SUFFIX);

    *objects = pkg_appendToList(*objects, &oTail, uprv_strdup(tmp));

    if(pkg_listContains(*suffixes, p)) {
      continue; 
    }

    *suffixes = pkg_appendToList(*suffixes, &sTail, uprv_strdup(p));
  }

}

void pkg_mode_dll(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{
  char tmp[1024];
  CharList *tail = NULL;
  CharList *suffixes =NULL, *objects = NULL;
  CharList *iter;

  if(U_FAILURE(*status)) { 
    return;
  }

  uprv_strcpy(tmp, LIB_PREFIX);
  uprv_strcat(tmp, o->shortName);
  uprv_strcat(tmp, UDATA_SO_SUFFIX);

  /* We should be the only item. So we don't care about the order. */
  o->outFiles = pkg_appendToList(o->outFiles, &tail, uprv_strdup(tmp));

  if(o->nooutput || o->verbose) {
    fprintf(stderr, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
  }

  if(o->nooutput) {
    *status = U_ZERO_ERROR;
    return;
  }

#ifdef WIN32
  sprintf(tmp, "# File to make:\nDLLTARGET=%s\n\n", o->outFiles->str);
  T_FileStream_writeLine(makefile, tmp);

  sprintf(tmp, "all: $(TARGETDIR)\$(DLLTARGET)\n\n");
  T_FileStream_writeLine(makefile, tmp);

  /* now, import the rules for making a common file.. */
  pkg_mode_dll(o, makefile, status);
  
  if(U_FAILURE(*status)) {
    fprintf(stderr, "# Dllmode: Error importing rules for 'common'.\n");
    return;
  }
  
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
  findSuffixesAndObjects(o->files, &suffixes, &objects);

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

  T_FileStream_writeLine(makefile, "icudata_dat.c: $(CMNLIST)\n\n"
               "\t$(TOOL) $(GENCMN) -S -d $(TARGETDIR) 0 $(CMNLIST)\n\n");

  sprintf(tmp, "OBJECTS= icudata_dat%s ", OBJ_SUFFIX);
  T_FileStream_writeLine(makefile, tmp);

  pkg_writeCharListWrap(makefile, objects, " ", " \\\n");
  T_FileStream_writeLine(makefile, "\n\n");

  for(iter=suffixes; iter; iter = iter->next) {
    sprintf(tmp, "%%_%s.c: %%.%s\n\t$(TOOL) $(GENCCODE) $<\n\n",
            iter->str,iter->str);
    T_FileStream_writeLine(makefile, tmp);
  }
  
  T_FileStream_writeLine(makefile,"build-objs: $(OBJECTS)\n\n");
  
  T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(TARGET): $(OBJECTS) $(HPUX_JUNK_OBJ) $(LISTFILES)\n"
                                   "\t$(SHLIB.c) -o $@ $(OBJECTS) $(HPUX_JUNK_OBJ)\n"
                                   "\t-ls -l $@\n\n");

  T_FileStream_writeLine(makefile, "hpux_junk_obj.cpp:\n"
                                   "	echo \"void to_emit_cxx_stuff_in_the_linker(){}\" >> hpux_junk_obj.cpp\n"
                                   "\n"
                                   "hpux_junk_obj.o: hpux_junk_obj.cpp\n"
                                   "	$(COMPILE.cc) -o $@ $<\n"
                                   "\n");

  T_FileStream_writeLine(makefile, "CLEANFILES= $(OBJECTS) $(HPUX_JUNK_OBJ) $(TARGETDIR)/$(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
  T_FileStream_writeLine(makefile, "\n\n");
  
  T_FileStream_writeLine(makefile, "install: $(TARGETDIR)/$(TARGET)\n"
                                   "\t$(INSTALL-S) $(TARGET) $(INSTALLTO)/$(TARGET)\n\n");

#endif /* NOT win32 */

*status = U_ZERO_ERROR;

}

