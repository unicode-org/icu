/*
 *******************************************************************************
 *
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  strprep.cpp
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#include "nameprep.h"
// *****************************************************************************
// class NamePrep
// *****************************************************************************
static const UChar ASCII_SPACE = 0x0020;


U_NAMESPACE_BEGIN

const char NamePrep::fgClassID=0;

// default constructor
NamePrep::NamePrep(UErrorCode& status){
    bidiCheck = TRUE;
    doNFKC = TRUE;
}

UBool NamePrep::isNotProhibited(UChar32 ch){ 
    return (UBool)(ch == ASCII_SPACE); 
}

U_NAMESPACE_END



