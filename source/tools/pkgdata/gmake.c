/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  gmake.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000may17
*   created by: Steven \u24C7 Loomis
*
* Emit a GNU makefile
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"
#include <stdio.h>
#include <string.h>

char linebuf[2048];

/* Write any setup/initialization stuff */
void
pkg_mak_writeHeader(FileStream *f, const UPKGOptions *o)
{
  sprintf(linebuf, "## Makefile for %s created by pkgdata\n"
                   "## from ICU Version %s\n"
                   "\n",
          o->shortName,
          U_ICU_VERSION);
  T_FileStream_writeLine(f, linebuf);

  sprintf(linebuf, "NAME=%s\n"
          "CNAME=%s\n"
          "TARGETDIR=%s\n"
          "TEMP_DIR=%s\n"
          "srcdir=$(TEMP_DIR)\n"
          "MODE=%s\n"
          "MAKEFILE=%s\n"
          "ENTRYPOINT=%s\n"
          "include %s\n"
          "\n\n\n",
          o->shortName,
          o->cShortName,
          o->targetDir,
          o->tmpDir,
          o->mode,
          o->makeFile,
          o->entryName,
          o->options);
  T_FileStream_writeLine(f, linebuf);

  /* TEMP_PATH  and TARG_PATH will be empty if the respective dir is . */
  /* Avoid //'s and .'s which confuse make ! */
  if(!strcmp(o->tmpDir,"."))
  {
    T_FileStream_writeLine(f, "TEMP_PATH=\n");
  }
  else
  {
    T_FileStream_writeLine(f, "TEMP_PATH=$(TEMP_DIR)/\n");
  }

  if(!strcmp(o->targetDir,"."))
  {
    T_FileStream_writeLine(f, "TARG_PATH=\n");
  }
  else
  {
    T_FileStream_writeLine(f, "TARG_PATH=$(TARGETDIR)/\n");
  }

  sprintf(linebuf, "## List files [%d] containing data files to process (note: - means stdin)\n"
                            "LISTFILES= ",
                         pkg_countCharList(o->fileListFiles));
  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->fileListFiles, " ", " \\\n",0);

  T_FileStream_writeLine(f, "\n\n\n");

  sprintf(linebuf, "## Data Files [%d]\n"
                            "DATAFILES= ",
                         pkg_countCharList(o->files));

  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->files, " ", " \\\n",-1);

  T_FileStream_writeLine(f, "\n\n\n");

  sprintf(linebuf, "## Data File Paths [%d]\n"
                            "DATAFILEPATHS= ",
                         pkg_countCharList(o->filePaths));

  T_FileStream_writeLine(f, linebuf);

  pkg_writeCharListWrap(f, o->filePaths, " ", " \\\n",0);

  T_FileStream_writeLine(f, "\n\n\n");

}

/* Write a stanza in the makefile, with specified   "target: parents...  \n\n\tcommands" [etc] */
void
pkg_mak_writeStanza(FileStream *f, const UPKGOptions *o, 
                    const char *target,
                    CharList* parents,
                    CharList* commands)
{
  T_FileStream_write(f, target, strlen(target));
  T_FileStream_write(f, " : ", 3);
  pkg_writeCharList(f, parents, " ",0);
  T_FileStream_write(f, "\n", 1);

  if(commands)
  {
    T_FileStream_write(f, "\t", 1);
    pkg_writeCharList(f, commands, "\n\t",0);
  }
  T_FileStream_write(f, "\n\n", 2);
}

/* write any cleanup/post stuff */
void
pkg_mak_writeFooter(FileStream *f, const UPKGOptions *o)
{
  /* nothing */
}


void
pkg_mak_writeObjRules(UPKGOptions *o,  FileStream *makefile, CharList **objects, const char* objSuffix)
{
  const char *p, *baseName;
  char *tmpPtr;
  char tmp[1024];
  char stanza[1024];
  char cfile[1024];
  CharList *oTail = NULL;
  CharList *infiles;
  CharList *parents = NULL, *commands = NULL;
  int32_t genFileOffset = 0;  /* offset from beginning of .c and .o file name, use to chop off package name for AS/400 */

  infiles = o->filePaths;

#if defined (OS400)
  if(infiles != NULL) {
    baseName = findBasename(infiles->str);
    p = uprv_strchr(baseName, '_');
    if(p != NULL) { 
      genFileOffset = (p-baseName)+1; /* "package_"  - name + underscore */
    }
  }
#endif

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
    uprv_strcat(tmp, objSuffix );

    /* iSeries cannot have '-' in the .o objects. */
    for( tmpPtr = tmp; *tmpPtr; tmpPtr++ ) {
      if ( *tmpPtr == '-' ) {
        *tmpPtr = '_';
      }
    }

    *objects = pkg_appendToList(*objects, &oTail, uprv_strdup(tmp + genFileOffset)); /* Offset for AS/400 */

    /* write source list */
    strcpy(cfile,tmp);
    strcpy(cfile+strlen(cfile)-strlen(objSuffix), ".c" ); /* replace .o with .c */

    /* Make up parents.. */
    parents = pkg_appendToList(parents, NULL, uprv_strdup(infiles->str));

    /* make up commands.. */
    sprintf(stanza, "@$(INVOKE) $(GENCCODE) -n $(ENTRYPOINT) -d $(TEMP_DIR) $<");
    commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));

    if(genFileOffset > 0) {    /* for AS/400 */
      sprintf(stanza, "@mv $(TEMP_PATH)%s $(TEMP_PATH)%s", cfile, cfile+genFileOffset);
      commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));
    }

    sprintf(stanza, "@$(COMPILE.c) -o $@ $(TEMP_DIR)/%s", cfile+genFileOffset); /* for AS/400 */
    commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));

    sprintf(stanza, "@$(RMV) $(TEMP_DIR)/%s", cfile+genFileOffset);
    commands = pkg_appendToList(commands, NULL, uprv_strdup(stanza));

    sprintf(stanza, "$(TEMP_PATH)%s", tmp+genFileOffset); /* for AS/400 */
    pkg_mak_writeStanza(makefile, o, stanza, parents, commands);

    pkg_deleteList(parents);
    pkg_deleteList(commands);
    parents = NULL;
    commands = NULL;
  }

}
