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
    T_FileStream_writeLine(makefile, "LIB_TARGET=$(TARGET)\n"
                            "HEADER=$(TARGETDIR)/$(NAME).h\n");


    uprv_strcpy(tmp, "all: $(TARGETDIR)/$(LIB_TARGET) $(HEADER)");
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
                "\t$(INVOKE) $(GENCMN) -e %s -n %s -S -d $(TEMP_DIR) 0 $(CMNLIST)\n\n", o->shortName, o->entryName, o->shortName);
    T_FileStream_writeLine(makefile, tmp);
    sprintf(tmp, "TOCOBJ= %s_dat%s \n\n", o->shortName,OBJ_SUFFIX);
    T_FileStream_writeLine(makefile, tmp);
    sprintf(tmp, "TOCSYM= %s_dat \n\n", o->entryName); /* entrypoint not always shortname! */
    T_FileStream_writeLine(makefile, tmp);

    T_FileStream_writeLine(makefile, "BASE_OBJECTS= $(TOCOBJ) $(NAME).$(STATIC_O) ");

    pkg_writeCharListWrap(makefile, objects, " ", " \\\n",0);
    T_FileStream_writeLine(makefile, "\n\n");
    T_FileStream_writeLine(makefile, "OBJECTS=$(BASE_OBJECTS:%=$(TEMP_DIR)/%)\n\n");

    T_FileStream_writeLine(makefile,"$(TEMP_DIR)/%.$(STATIC_O): $(TEMP_DIR)/%.c\n\t  $(COMPILE.c) -o $@ $<\n\n");

    T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(LIB_TARGET):$(TARGETDIR)/$(LIB_TARGET)($(OBJECTS)) $(HPUX_JUNK_OBJ) $(LISTFILES)\n"
                            "\t$(RANLIB) $@\n\n");


    T_FileStream_writeLine(makefile, "CLEANFILES= $(CMNLIST) $(OBJECTS) $(HPUX_JUNK_OBJ) $(TARGETDIR)/$(LIB_TARGET) $(TARGETDIR)/$(MIDDLE_STATIC_LIB_TARGET) $(TARGETDIR)/$(TARGET)\n\nclean:\n\t-$(RMV) $(CLEANFILES) $(MAKEFILE)");
    T_FileStream_writeLine(makefile, "\n\n");

    T_FileStream_writeLine(makefile, "# static mode shouldn't need to be installed, but we will install the header and static library for them.\n");

    T_FileStream_writeLine(makefile, "install: $(TARGETDIR)/$(LIB_TARGET) $(HEADER)\n"
                            "\t$(INSTALL-L) $(TARGETDIR)/$(LIB_TARGET) $(INSTALLTO)/$(LIB_TARGET)\n"
                            "\t$(INSTALL_DATA) $(HEADER) $(INSTALLTO)/$(includedir)/unicode\n");
    if (o->version) {
        T_FileStream_writeLine(makefile, "\tcd $(INSTALLTO) && $(RM) $(MIDDLE_STATIC_LIB_TARGET) && ln -s $(LIB_TARGET) $(MIDDLE_STATIC_LIB_TARGET)\n\tcd $(INSTALLTO) && $(RM) $(STATIC_LIB_TARGET) && ln -s $(LIB_TARGET) $(STATIC_LIB_TARGET)\n");
    }
    T_FileStream_writeLine(makefile, "\n");
    T_FileStream_writeLine(makefile, "# We generate the following files from the Makefile, so that they don't get needlessly recompiled.\n");
    T_FileStream_writeLine(makefile, "\n");

    T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(NAME).h:\n"
                            "\t@echo '#include \"unicode/utypes.h\"' > $@\n"
                            "\t@echo  >> $@\n"
                            "\t@echo '/* Call this function to install Application data */' >> $@\n"
                            "\t@echo \"U_CAPI void udata_install_$(NAME)(UErrorCode* err);\" >> $@\n\n");

    T_FileStream_writeLine(makefile, "$(TARGETDIR)/$(NAME).c:\n"
                            "\t@echo '#include \"unicode/udata.h\"' > $@\n"
                            "\t@echo \"extern char $(NAME)_dat[];\" >> $@\n"
                            "\t@echo \"extern void udata_install_$(NAME)(UErrorCode* err){udata_setAppData(\"'\"'\"$(NAME)\"'\"'\", (const void*) $(NAME)_dat, err);}\" >> $@\n"
                            "\n");

    *status = U_ZERO_ERROR;

}



