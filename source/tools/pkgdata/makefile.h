/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  makefile.h
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000may17
*   created by: Steven \u24C7 Loomis
*
*  definition for code to create a makefile.
*  implementation is OS dependent (i.e. gmake.c, nmake.c, .. )
*/

#ifndef _MAKEFILE
#define _MAKEFILE

/* headers */
#include "unicode/utypes.h"
#include "pkgtypes.h"


/* Write any setup/initialization stuff */
void
pkg_mak_writeHeader(FileStream *f, const UPKGOptions *o);

/* Write a stanza in the makefile, with specified   "target: parents...  \n\n\tcommands" [etc] */
void
pkg_mak_writeStanza(FileStream *f, const UPKGOptions *o, 
                    const char *target,
                    CharList* parents,
                    CharList* commands);

/* write any cleanup/post stuff */
void
pkg_mak_writeFooter(FileStream *f, const UPKGOptions *o);

#endif
