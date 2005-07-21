/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  canonucm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov08
*   created by: Markus W. Scherer
*
*   This tool reads a .ucm file and canonicalizes it: In the CHARMAP section,
*   - sort by Unicode code points
*   - print all code points in uppercase hexadecimal
*   - print all Unicode code points with 4, 5, or 6 digits as needed
*   - remove the comments
*   - remove unnecessary spaces
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl canonucm.c
*/

#error File moved to charset/source/ucmtools/ on 2002-nov-06

/* see http://oss.software.ibm.com/cvs/icu/charset/source/ucmtools/ */
