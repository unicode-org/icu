/******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  staticmode.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002mar14
*   created by: Steven \u24C7 Loomis
*
*   This program packages the ICU data into a static library.
*   It is *mainly* used by POSIX, but the top function (for writing READMEs) is
*   shared with Win32.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uloc.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "filestrm.h"


void pkg_sttc_writeReadme(struct UPKGOptions_ *o, const char *libName, UErrorCode *status)
{
  char tmp[1024];
  FileStream  *out;

  if(U_FAILURE(*status))
  {
      return;
  }

  /* Makefile pathname */
  uprv_strcpy(tmp, o->targetDir);
  uprv_strcat(tmp, U_FILE_SEP_STRING);
  uprv_strcat(tmp, "README");
  uprv_strcat(tmp, "_");
  uprv_strcat(tmp, o->shortName);
  uprv_strcat(tmp, ".txt"); 

  out = T_FileStream_open(tmp, "w");
  if (!out) {
      fprintf(stderr, "err: couldn't create README file %s\n", tmp);
      *status = U_FILE_ACCESS_ERROR;
      return;
  }

  sprintf(tmp, "## README for \"%s\"'s static data (%s)\n"
               "## created by pkgdata, ICU Version %s\n",
             o->shortName,
             libName,
             U_ICU_VERSION);

  T_FileStream_writeLine(out, tmp);

  sprintf(tmp, "\n\nTo use this data in your application:\n\n"
               "1. At the top of your source file, add the following lines:\n"
               "\n"
               "     #include \"unicode/utypes.h\"\n"
               "     #include \"unicode/udata.h\"\n"
               "     U_CFUNC char %s_dat[];\n",
               o->shortName);
  T_FileStream_writeLine(out, tmp);

  sprintf(tmp, "2. *Early* in your application, call the following function:\n"
               "\n"
               "     UErrorCode myError = U_ZERO_ERROR;\n"
               "     udata_setAppData( \"%s\", (const void*) %s_dat, &myError);\n"
               "     if(U_FAILURE(myError))\n"
               "     {\n"
               "          handle error condition ...\n"
               "     }\n"
               "\n",
               o->shortName,o->shortName);
  T_FileStream_writeLine(out, tmp);

  sprintf(tmp, "3. Link your application against %s\n"
               "\n\n"
               "4. Now, you may access this data with a 'path' of \"%s\" as in the following example:\n"
               "\n"
               "     ... ures_open( \"%s\", \"%s\", &err ); \n",
               libName, o->shortName, o->shortName, uloc_getDefault());
  T_FileStream_writeLine(out, tmp);

  T_FileStream_close(out);
}


#ifndef WIN32


#include "makefile.h"

static void
writeObjRules(UPKGOptions *o,  FileStream *makefile, CharList **objects)
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
        uprv_strcat(tmp, ".");
        uprv_strcat(tmp, STATIC_O);

        *objects = pkg_appendToList(*objects, &oTail, uprv_strdup(tmp));

        /* write source list */
        strcpy(cfile,tmp);
        strcpy(cfile+strlen(cfile)-strlen("." STATIC_O), ".c" );


        /* Make up parents.. */
        parents = pkg_appendToList(parents, NULL, uprv_strdup(infiles->str));

        /* make up commands.. */
        sprintf(stanza, "$(INVOKE) $(GENCCODE) -n %s -d $(TEMP_DIR) %s%s $<", o->shortName,
                (o->version)?"-r ":"",
                o->version?o->version:"");
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

void pkg_mode_static(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{
    char tmp[1024];
    CharList *tail = NULL;
    CharList *objects = NULL;

    if(U_FAILURE(*status)) {
        return;
    }

    uprv_strcpy(tmp, LIB_PREFIX);
    uprv_strcat(tmp, o->shortName);
    uprv_strcat(tmp, UDATA_LIB_SUFFIX);

    o->outFiles = pkg_appendToList(o->outFiles, &tail, uprv_strdup(tmp));

    pkg_sttc_writeReadme(o, tmp, status);
    if(U_FAILURE(*status)) {
        return;
    }


    if(o->nooutput || o->verbose) {
        fprintf(stdout, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
    }

    if(o->nooutput) {
        *status = U_ZERO_ERROR;
        return;
    }

    /* begin writing makefile ========================= */


    T_FileStream_writeLine(makefile, "# Version numbers:\nVERSIONED=");
    if (o->version) {
        sprintf(tmp, ".%s", o->version);
        if (!uprv_strchr(o->version, '.')) {
            uprv_strcat(tmp, ".0");
        }
        T_FileStream_writeLine(makefile, tmp);
        T_FileStream_writeLine(makefile, "\nDLL_LDFLAGS=$(LD_SONAME) $(RPATH_LDFLAGS)\n");
    } else {
        T_FileStream_writeLine(makefile, "\nDLL_LDFLAGS=$(BIR_LDFLAGS)\n");
    }
    T_FileStream_writeLine(makefile, "\n");

    sprintf(tmp, "# File to make:\nTARGET=%s\n\n", o->outFiles->str);
    T_FileStream_writeLine(makefile, tmp);
    T_FileStream_writeLine(makefile, "LIB_TARGET=$(TARGET)\n");


    uprv_strcpy(tmp, "all: $(TARGETDIR)/$(LIB_TARGET)");
    uprv_strcat(tmp, "\n\n");
    T_FileStream_writeLine(makefile, tmp);

    /* Write compile rules */
    writeObjRules(o, makefile, &objects);

    sprintf(tmp, "# List file for gencmn:\n"
        "CMNLIST=%s%s%s_static.lst\n\n",
        o->tmpDir,
        U_FILE_SEP_STRING,
        o->shortName);
    T_FileStream_writeLine(makefile, tmp);

    if(o->hadStdin == FALSE) { /* shortcut */
        T_FileStream_writeLine(makefile, "$(CMNLIST): $(LISTFILES)\n"
            "\tcat $(LISTFILES) > $(CMNLIST)\n\n");
    } else {
        T_FileStream_writeLine(makefile, "$(CMNLIST): \n"
            "\t@echo \"generating $@ (list of data files)\"\n"
            "\t@-$(RMV) $@\n"
            "\t@for file in $(DATAFILEPATHS); do \\\n"
            "\t  echo $$file >> $@; \\\n"
            "\tdone;\n\n");
    }

    sprintf(tmp,"$(TEMP_DIR)/%s_dat.$(STATIC_O) : $(TEMP_DIR)/%s_dat.c\n"
        "\t$(COMPILE.c) -o $@ $<\n\n",
        o->shortName,
        o->shortName);
    T_FileStream_writeLine(makefile, tmp);

    T_FileStream_writeLine(makefile, "# 'TOCOBJ' contains C Table of Contents objects [if any]\n");

    sprintf(tmp, "$(TEMP_DIR)/%s_dat.c: $(CMNLIST)\n"
            "\t$(INVOKE) $(GENCMN) -e %s -n %s -S -d $(TEMP_DIR) %s%s 0 $(CMNLIST)\n\n", o->shortName, o->entryName, o->shortName,
            (o->version)?"-r ":"",
            o->version?o->version:"");

    T_FileStream_writeLine(makefile, tmp);
    sprintf(tmp, "TOCOBJ= %s_dat%s \n\n", o->shortName,OBJ_SUFFIX);
    T_FileStream_writeLine(makefile, tmp);
    sprintf(tmp, "TOCSYM= %s_dat \n\n", o->entryName); /* entrypoint not always shortname! */
    T_FileStream_writeLine(makefile, tmp);

    T_FileStream_writeLine(makefile, "BASE_OBJECTS= $(TOCOBJ) ");

    pkg_writeCharListWrap(makefile, objects, " ", " \\\n",0);
    T_FileStream_writeLine(makefile, "\n\n");
    T_FileStream_writeLine(makefile, "OBJECTS=$(BASE_OBJECTS:%=$(TEMP_DIR)/%)\n\n");

    T_FileStream_writeLine(makefile,"$(TEMP_DIR)/%.$(STATIC_O): $(TEMP_DIR)/%.c\n\t  $(COMPILE.c) -o $@ $<\n\n");

    T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(LIB_TARGET):$(TARGETDIR)/$(LIB_TARGET)($(OBJECTS)) $(LISTFILES)\n"
                            "\t$(RANLIB) $@\n\n");


    T_FileStream_writeLine(makefile, "CLEANFILES= $(CMNLIST) $(OBJECTS) $(TARGETDIR)/$(LIB_TARGET) $(TARGETDIR)/$(MIDDLE_STATIC_LIB_TARGET) $(TARGETDIR)/$(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
    T_FileStream_writeLine(makefile, "\n\n");

    T_FileStream_writeLine(makefile, "# static mode shouldn't need to be installed, but we will install the header and static library for them.\n");

    T_FileStream_writeLine(makefile, "install: $(TARGETDIR)/$(LIB_TARGET)\n"
                "\t$(INSTALL-L) $(TARGETDIR)/$(LIB_TARGET) $(INSTALLTO)/$(LIB_TARGET)\n");
    if (o->version) {
        T_FileStream_writeLine(makefile, "\tcd $(INSTALLTO) && $(RM) $(MIDDLE_STATIC_LIB_TARGET) && ln -s $(LIB_TARGET) $(MIDDLE_STATIC_LIB_TARGET)\n\tcd $(INSTALLTO) && $(RM) $(STATIC_LIB_TARGET) && ln -s $(LIB_TARGET) $(STATIC_LIB_TARGET)\n");
    }

    *status = U_ZERO_ERROR;

}



#endif
