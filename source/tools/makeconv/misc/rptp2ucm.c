/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  rptp2ucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001feb16
*   created by: Markus W. Scherer
*
*   This tool reads two CDRA conversion table files (RPMAP & TPMAP or RXMAP and TXMAP) and
*   generates a canonicalized ICU .ucm file from them.
*   If the RPMAP/RXMAP file does not contain a comment line with the substitution character,
*   then this tool also attempts to read the header of the corresponding UPMAP/UXMAP file
*   to extract subchar and subchar1.
*
*   R*MAP: Unicode->codepage
*   T*MAP: codepage->Unicode
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl rptp2ucm.c
*/

#error File moved to charset/source/ucmtools/ on 2002-nov-06

/* see http://oss.software.ibm.com/cvs/icu/charset/source/ucmtools/ */
