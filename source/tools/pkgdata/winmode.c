/******************************************************************************
*
*   Copyright (C) 2000-2004, International Business Machines
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

#include "unicode/utypes.h"

#ifdef WIN32

#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"
#include <stdio.h>
#include <stdlib.h>

/*#define WINBUILDMODE (*(o->options)=='R'?"Release":"Debug")*/
#define CONTAINS_REAL_PATH(o) (*(o->options)==PKGDATA_DERIVED_PATH)

void writeCmnRules(UPKGOptions *o,  FileStream *makefile)
{
    char tmp[1024];
    CharList *infiles;

    if(!o->embed) {
      infiles = o->files; 
    } else {
      infiles = o->filePaths;
    }
    sprintf(tmp, "\"$(TARGETDIR)\\$(CMNTARGET)\" : $(DATAFILEPATHS)\n"
        "\t%s\"$(GENCMN)\" %s%s%s-d \"$(TARGETDIR)\" %s %s -n \"$(NAME)\" 0 <<\n",
        (o->verbose ? "" : "@"),
        (o->comment ? "-C \"" : ""),
        (o->comment ? o->comment : ""),
        (o->comment ? "\" " : ""),
        (o->embed ? "-E" : ""),
         o->embed ? "" : "-s \"$(SRCDIR)\"");
    T_FileStream_writeLine(makefile, tmp);

    pkg_writeCharList(makefile, infiles, "\n", -1);
/*
    for(;infiles;infiles = infiles->next) {
    if(infiles->str[0] != '"' && infiles->str[uprv_strlen(infiles->str)-1] != '"') {
        sprintf(tmp, "\"%s\"\n", infiles->str);
    } else {
        sprintf(tmp, "%s\n", infiles->str);
    }
    T_FileStream_writeLine(makefile, tmp);
    }
*/
    sprintf(tmp, "\n<<\n");
    T_FileStream_writeLine(makefile, tmp);
}



void pkg_mode_windows(UPKGOptions *o, FileStream *makefile, UErrorCode *status) {
    char tmp[1024];
    char tmp2[1024];
    const char *separator = o->icuroot[uprv_strlen(o->icuroot)-1]=='\\'?"":"\\";
    UBool isDll = (UBool)(uprv_strcmp(o->mode, "dll") == 0);
    UBool isStatic = (UBool)(uprv_strcmp(o->mode, "static") == 0);

    if(U_FAILURE(*status)) {
        return;
    }

    sprintf(tmp2, "ICUROOT=%s\n\n", o->icuroot);
    T_FileStream_writeLine(makefile, tmp2);

    if (CONTAINS_REAL_PATH(o)) {
        sprintf(tmp2,
            "GENCMN = $(ICUROOT)%sgencmn.exe\n", separator);
    }
    else {
        sprintf(tmp2,
            "GENCMN = $(ICUROOT)%sbin\\gencmn.exe\n", separator);
    }
    T_FileStream_writeLine(makefile, tmp2);

    if(isDll) {
        uprv_strcpy(tmp, LIB_PREFIX);
        uprv_strcat(tmp, o->libName);
        if (o->version) {
            uprv_strcat(tmp, "$(TARGET_VERSION)");
        }
        uprv_strcat(tmp, UDATA_SO_SUFFIX);

        if(o->nooutput || o->verbose) {
            fprintf(stdout, "# Output %s file: %s%s%s\n", UDATA_SO_SUFFIX, o->targetDir, U_FILE_SEP_STRING, tmp);
        }

        if(o->nooutput) {
            *status = U_ZERO_ERROR;
            return;
        }

        sprintf(tmp2, "# DLL file to make:\nDLLTARGET=%s\n\n", tmp);
        T_FileStream_writeLine(makefile, tmp2);

        sprintf(tmp2,
            "LINK32 = link.exe\n"
            "LINK32_FLAGS = /nologo /out:\"$(TARGETDIR)\\$(DLLTARGET)\" /DLL /NOENTRY /base:\"0x4ad00000\" /implib:\"$(TARGETDIR)\\$(LIBNAME).lib\" %s%s%s\n",
            (o->comment ? "/comment:\"" : ""),
            (o->comment ? o->comment : ""),
            (o->comment ? "\"" : ""),
            o->comment
            );
        T_FileStream_writeLine(makefile, tmp2);

        if (CONTAINS_REAL_PATH(o)) {
            sprintf(tmp2,
                "GENCCODE = $(ICUROOT)%sgenccode.exe\n", separator);
        }
        else {
            sprintf(tmp2,
                "GENCCODE = $(ICUROOT)%sbin\\genccode.exe\n", separator);
        }
        T_FileStream_writeLine(makefile, tmp2);

        /* If you modify this, remember to modify makedata.mak too. */
        T_FileStream_writeLine(makefile, "\n"
            "# Windows specific DLL version information.\n"
            "!IF EXISTS(\"$(TEMP_DIR)\\icudata.res\")\n"
            "DATA_VER_INFO=\"$(TEMP_DIR)\\icudata.res\"\n"
            "!ELSE\n"
            "DATA_VER_INFO=\n"
            "!ENDIF\n\n");


        uprv_strcpy(tmp, UDATA_CMN_PREFIX "$(NAME)" UDATA_CMN_INTERMEDIATE_SUFFIX OBJ_SUFFIX);

        sprintf(tmp2, "# intermediate obj file:\nCMNOBJTARGET=%s\n\n", tmp);
        T_FileStream_writeLine(makefile, tmp2);
    }
    else if (isStatic)
    {
        uprv_strcpy(tmp, LIB_PREFIX);
        uprv_strcat(tmp, o->libName);
        uprv_strcat(tmp, UDATA_LIB_SUFFIX);

        if (!o->quiet) {
            pkg_sttc_writeReadme(o, tmp, status);
        }
        if(U_FAILURE(*status))
        {
            return;
        }

        if(o->nooutput || o->verbose) {
            fprintf(stdout, "# Output %s file: %s%s%s\n", UDATA_SO_SUFFIX, o->targetDir, U_FILE_SEP_STRING, tmp);
        }

        if(o->nooutput) {
            *status = U_ZERO_ERROR;
            return;
        }

        sprintf(tmp2, "# LIB file to make:\nDLLTARGET=%s\n\n", tmp);
        T_FileStream_writeLine(makefile, tmp2);

        sprintf(tmp2,
            "LINK32 = LIB.exe\n"
            "LINK32_FLAGS = /nologo /out:\"$(TARGETDIR)\\$(DLLTARGET)\" /EXPORT:\"%s\"\n",
            o->libName
            );
        T_FileStream_writeLine(makefile, tmp2);


        if (CONTAINS_REAL_PATH(o)) {
            sprintf(tmp2,
                "GENCCODE = $(ICUROOT)%sgenccode.exe\n", separator);
        }
        else {
            sprintf(tmp2,
                "GENCCODE = $(ICUROOT)%sbin\\genccode.exe\n", separator);
        }
        T_FileStream_writeLine(makefile, tmp2);

        uprv_strcpy(tmp, UDATA_CMN_PREFIX "$(NAME)" UDATA_CMN_INTERMEDIATE_SUFFIX OBJ_SUFFIX);

        sprintf(tmp2, "# intermediate obj file\nCMNOBJTARGET=%s\n\n", tmp);
        T_FileStream_writeLine(makefile, tmp2);
    }
    uprv_strcpy(tmp, UDATA_CMN_PREFIX);
    uprv_strcat(tmp, o->cShortName);
    if (o->version && !uprv_strstr(o->shortName,o->version)) {
        uprv_strcat(tmp, "$(TARGET_VERSION)");
    }
    uprv_strcat(tmp, UDATA_CMN_SUFFIX);

    if(o->nooutput || o->verbose) {
        fprintf(stdout, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
    }

    if(o->nooutput) {
        *status = U_ZERO_ERROR;
        return;
    }

    sprintf(tmp2, "# common file to make:\nCMNTARGET=%s\n\n", tmp);
    T_FileStream_writeLine(makefile, tmp2);


    if(isDll || isStatic) {
        sprintf(tmp, "all: \"$(TARGETDIR)\\$(DLLTARGET)\"\n\n");
        T_FileStream_writeLine(makefile, tmp);

        sprintf(tmp, "\"$(TARGETDIR)\\$(DLLTARGET)\": \"$(TEMP_DIR)\\$(CMNOBJTARGET)\"\n"
            "\t$(LINK32) $(LINK32_FLAGS) \"$(TEMP_DIR)\\$(CMNOBJTARGET)\" $(DATA_VER_INFO)\n\n");
        T_FileStream_writeLine(makefile, tmp);
        sprintf(tmp, "\"$(TEMP_DIR)\\$(CMNOBJTARGET)\": \"$(TARGETDIR)\\$(CMNTARGET)\"\n"
            "\t@\"$(GENCCODE)\" $(GENCOPTIONS) -e $(ENTRYPOINT) -o -d \"$(TEMP_DIR)\" \"$(TARGETDIR)\\$(CMNTARGET)\"\n\n");
        T_FileStream_writeLine(makefile, tmp);

        sprintf(tmp2,
            "clean:\n"
            "\t-@erase \"$(TARGETDIR)\\$(DLLTARGET)\"\n"
            "\t-@erase \"$(TARGETDIR)\\$(CMNOBJTARGET)\"\n"
            "\t-@erase \"$(TARGETDIR)\\$(CMNTARGET)\"\n\n");
        T_FileStream_writeLine(makefile, tmp2);

        T_FileStream_writeLine(makefile, "install: \"$(TARGETDIR)\\$(DLLTARGET)\"\n"
                                         "\tcopy \"$(TARGETDIR)\\$(DLLTARGET)\" \"$(INSTALLTO)\\$(DLLTARGET)\"\n\n");
    } else { /* common */
        sprintf(tmp, "all: \"$(TARGETDIR)\\$(CMNTARGET)\"\n\n");
        T_FileStream_writeLine(makefile, tmp);

        sprintf(tmp2,
            "clean:\n"
            "\t-@erase \"$(TARGETDIR)\\$(CMNTARGET)\"\n\n");
        T_FileStream_writeLine(makefile, tmp2);

        T_FileStream_writeLine(makefile, "install: \"$(TARGETDIR)\\$(CMNTARGET)\"\n"
                                         "\tcopy \"$(TARGETDIR)\\$(CMNTARGET)\" \"$(INSTALLTO)\\$(CMNTARGET)\"\n\n");
    }

    T_FileStream_writeLine(makefile, "rebuild: clean all\n\n");

    /* Write compile rules */
    writeCmnRules(o, makefile);
}

#endif
