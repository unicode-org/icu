/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#ifndef _INTLTESTCONVERT
#define _INTLTESTCONVERT


#include "cppcnvt.h"
#include "intltest.h"


#ifdef ICU_UNICODECONVERTER_USE_DEPRECATES
class IntlTestConvert: public IntlTest {
public:
virtual   void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );
};
#endif /* ICU_UNICODECONVERTER_USE_DEPRECATES */


#endif
