/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucmstrip.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov09
*   created by: Markus W. Scherer
*
*   This tool reads a .ucm file, expects there to be a line in the header with
*   "File created on..." and removes the lines before and including that.
*   Then it removes lines with <icu:state> and <uconv_class> and <code_set_name>.
*   This helps comparing .ucm files with different copyright statements and
*   different state specifications.
*
*   To compile, just call a C compiler/linker with this source file.
*   On Windows: cl ucmstrip.c
*/

#error File moved to charset/source/ucmtools/ on 2002-nov-06

/* see http://oss.software.ibm.com/cvs/icu/charset/source/ucmtools/ */
