/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  nameprep.cpp
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_IDNA

#include "nameprep.h"
// *****************************************************************************
// class NamePrep
// *****************************************************************************

U_NAMESPACE_BEGIN

const char NamePrep::fgClassID=0;

// default constructor
NamePrep::NamePrep(UErrorCode& status){
    bidiCheck = TRUE;
    doNFKC = TRUE;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_IDNA */
