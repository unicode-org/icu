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

void writeObjRules(UPKGOptions *o,  FileStream *makefile, CharList **objects)
{
  const char *p, *baseName;
  char tmp[1024];
  char stanza[1024];
  char cfile[1024];
  CharList *oTail = NULL;
  CharList *infiles;
  CharList *parents = NULL, *commands = NULL;

  infiles = o->filePaths;

  for(;infiles;infiles = infiles->next) {
    baseName = findBasename(infiles->str);
    p = uprv_strrchr(baseName, '.');
    if( (p == NULL) || (*p == '\0' ) ) {
      continue;
    }

    uprv_strncpy(tmp, baseName, p-baseName);
    p++;

    uprv_strcpy(tmp+(p-1-baseName), "_"); /* to append */
    uprv_strcat(tmp, p);
    uprv_strcat(tmp, OBJ_SUFFIX);

    *objects = pkg_appendToList(*objects, &oTail, uprv_strdup(tmp));

    /* write source list */
    strcpy(cfile,tmp);
    strcpy(cfile+strlen(cfile)-strlen(OBJ_SUFFIX), ".c" );


    /* Make up parents.. */
    parents = pkg_appendToList(parents, NULL, uprv_strdup(infiles->str));

    /* make up commands.. */
    sprintf(stanza, "$(INVOKE) $(GENCCODE) -n %s -d $(TEMP_DIR) $<", o->shortName);
    commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));

    sprintf(stanza, "$(COMPILE.c) -o $@ $(TEMP_DIR)/%s", cfile);
    commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));

    sprintf(stanza, "$(TEMP_DIR)/%s", tmp);
    pkg_mak_writeStanza(makefile, o, stanza, parents, commands);

    pkg_deleteList(parents);
    pkg_deleteList(commands);
    parents = NULL;
    commands = NULL;
  }

}

void pkg_mode_dll(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{
  char tmp[1024];
  CharList *tail = NULL;
  CharList *objects = NULL;
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
    fprintf(stdout, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
  }

  if(o->nooutput) {
    *status = U_ZERO_ERROR;
    return;
  }

  /* begin writing makefile ========================= */


/*390port start*/
#ifdef OS390
  if(uprv_isOS390BatchMode()) {
    if (uprv_strcmp(o->shortName, U_ICUDATA_NAME) == 0)
      sprintf(tmp, "# File to make:\nBATCH_TARGET=\"//'${LOADMOD}(IXMICUDA)'\"\n\n"); 
    else if (uprv_strcmp(o->shortName, "testdata") == 0)
      sprintf(tmp, "# File to make:\nBATCH_TARGET=\"//'${LOADMOD}(IXMICUTE)'\"\n\n"); 
    else if (uprv_strcmp(o->shortName, U_ICUDATA_NAME"_390") == 0)
      sprintf(tmp, "# File to make:\nBATCH_TARGET=\"//'${LOADMOD}(IXMICUD1)'\"\n\n"); 
    T_FileStream_writeLine(makefile, tmp);
  }
#endif

  sprintf(tmp, "# File to make:\nTARGET=%s\n\n", o->outFiles->str);
  T_FileStream_writeLine(makefile, tmp);

  sprintf(tmp, "all: $(TARGETDIR)/$(TARGET) $(BATCH_TARGET)\n\n");
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

  sprintf(tmp,"$(TEMP_DIR)/%s_dat.o : $(TEMP_DIR)/%s_dat.c\n"
                         "\t$(COMPILE.c) -o $@ $<\n\n",
          o->shortName,
          o->shortName);
  T_FileStream_writeLine(makefile, tmp);

  T_FileStream_writeLine(makefile, "# 'TOCOBJ' contains C Table of Contents objects [if any]\n");

  sprintf(tmp, "$(TEMP_DIR)/%s_dat.c: $(CMNLIST)\n"
                         "\t$(INVOKE) $(GENCMN) -e %s -n %s -S -d $(TEMP_DIR) 0 $(CMNLIST)\n\n", o->shortName, o->entryName, o->shortName);
  T_FileStream_writeLine(makefile, tmp);
  sprintf(tmp, "TOCOBJ= %s_dat%s \n\n", o->shortName,OBJ_SUFFIX);
  T_FileStream_writeLine(makefile, tmp);
  sprintf(tmp, "TOCSYM= %s_dat \n\n", o->entryName); /* entrypoint not always shortname! */
  T_FileStream_writeLine(makefile, tmp);

  T_FileStream_writeLine(makefile, "BASE_OBJECTS= $(TOCOBJ) ");

  pkg_writeCharListWrap(makefile, objects, " ", " \\\n",0);
  T_FileStream_writeLine(makefile, "\n\n");
  T_FileStream_writeLine(makefile, "OBJECTS=$(BASE_OBJECTS:%=$(TEMP_DIR)/%)\n\n");

  T_FileStream_writeLine(makefile,"$(TEMP_DIR)/%.o: $(TEMP_DIR)/%.c\n\t  $(COMPILE.c) -o $@ $<\n\n");

  T_FileStream_writeLine(makefile,"build-objs: $(SOURCES) $(OBJECTS)\n\n$(OBJECTS): $(SOURCES)\n\n");
 
#ifdef HPUX 
  T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(TARGET): $(OBJECTS) $(HPUX_JUNK_OBJ) $(LISTFILES) $(BIR_DEPS)\n"
                                   "\t$(SHLIB.cc) -o $@ $(OBJECTS) $(HPUX_JUNK_OBJ) $(BIR_LDFLAGS)\n"
                                   "\t-ls -l $@\n\n");

  T_FileStream_writeLine(makefile, "$(TEMP_DIR)/hpux_junk_obj.cpp:\n"
                                   "\techo \"void to_emit_cxx_stuff_in_the_linker(){}\" >> $(TEMP_DIR)/hpux_junk_obj.cpp\n"
                                   "\n"
                                   "$(TEMP_DIR)/hpux_junk_obj.o: $(TEMP_DIR)/hpux_junk_obj.cpp\n"
                                   "\t$(COMPILE.cc) -o $@ $<\n"
                                   "\n");
#else

/*390port*/
#ifdef OS390BATCH
  T_FileStream_writeLine(makefile, "$(BATCH_TARGET): $(OBJECTS) $(LISTFILES) $(BIR_DEPS)\n"
                                   "\t$(SHLIB.c) -o $@ $(OBJECTS) $(BIR_LDFLAGS)\n"
                                   "#  \t-ls -l $@\n\n");
#endif

  T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(TARGET): $(OBJECTS) $(LISTFILES) $(BIR_DEPS)\n"
                                   "\t$(SHLIB.c) -o $@ $(OBJECTS) $(BIR_LDFLAGS)\n"
                                   "\t-ls -l $@\n\n");
#endif

  T_FileStream_writeLine(makefile, "CLEANFILES= $(OBJECTS) $(HPUX_JUNK_OBJ) $(TARGETDIR)/$(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
  T_FileStream_writeLine(makefile, "\n\n");
  
  T_FileStream_writeLine(makefile, "install: $(TARGETDIR)/$(TARGET)\n"
                                   "\t$(INSTALL-L) $(TARGETDIR)/$(TARGET) $(INSTALLTO)/$(TARGET)\n\n");

#ifdef U_SOLARIS
  T_FileStream_writeLine(makefile, "$(NAME).map:\n\techo \"{global: $(TOCSYM); local: *; };\" > $@\n\n");
#endif

#ifdef AIX
  T_FileStream_writeLine(makefile, "$(NAME).map:\n\techo \"$(TOCSYM)\" > $@\n\n");
#endif


*status = U_ZERO_ERROR;

}



