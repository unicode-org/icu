/*
*******************************************************************************
* Copyright (C) 1997-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TXTBDRY.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Added DONE.
*   01/13/2000  helena      Added UErrorCode parameter to createXXXInstance methods.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file BreakIterator.java
// *****************************************************************************

#include "unicode/dbbi.h"
#include "unicode/brkiter.h"
#include "unicode/udata.h"
#include "unicode/resbund.h"
#include "cstring.h"
#include <string.h>

// *****************************************************************************
// class BreakIterator
// This class implements methods for finding the location of boundaries in text. 
// Instances of BreakIterator maintain a current position and scan over text
// returning the index of characters where boundaries occur.
// *****************************************************************************

const UTextOffset BreakIterator::DONE = (int32_t)-1;

// -------------------------------------

// Creates a simple text boundary for word breaks.
BreakIterator*
BreakIterator::createWordInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "word";

    UnicodeString temp;
    if (U_FAILURE(status)) return NULL;
    if (!uprv_strcmp(key.getLanguage(), "th"))
    {
        filename = "word_th";
    }

    UDataMemory* file = udata_open(NULL, "brk", filename, &status);

    if (!U_FAILURE(status)) {
        const void* image = udata_getMemory(file);

        if (image != NULL) {
            if(!uprv_strcmp(filename, "word_th")) {
                const char* dataDir = u_getDataDirectory();
                filename = "thaidict.brk";
                char* fullPath = new char[strlen(dataDir) + strlen(filename) + 1];
                strcpy(fullPath, dataDir);
                strcpy(fullPath, filename);
                
                result = new DictionaryBasedBreakIterator(image, fullPath, status);
                delete [] fullPath;
            }
            else {
                result = new RuleBasedBreakIterator(image);
            }
        }
    }
    
    //udata_close(file); // This prevents a leak, but it should be checked whether it is harmful

    return result;
}

// -------------------------------------

// Creates a simple text boundary for line breaks.
BreakIterator*
BreakIterator::createLineInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "line";

    UnicodeString temp;
    if (U_FAILURE(status)) return NULL;
    if (!uprv_strcmp(key.getLanguage(), "th"))
    {
        filename = "line_th";
    }

    UDataMemory* file = udata_open(NULL, "brk", filename, &status);

    if (!U_FAILURE(status)) {
        const void* image = udata_getMemory(file);

        if (image != NULL) {
          if (!uprv_strcmp(key.getLanguage(), "th")) {
                const char* dataDir = u_getDataDirectory();
                filename = "thaidict.brk";
                char* fullPath = new char[strlen(dataDir) + strlen(filename) + 1];
                strcpy(fullPath, dataDir);
                strcat(fullPath, filename);
                
                result = new DictionaryBasedBreakIterator(image, fullPath, status);
                delete [] fullPath;
            }
            else {
                result = new RuleBasedBreakIterator(image);
            }
        }
    }
    
    //udata_close(file); // This prevents a leak, but it should be checked whether it is harmful

    return result;
}

// -------------------------------------

// Creates a simple text boundary for character breaks.
BreakIterator*
BreakIterator::createCharacterInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "char";

    if (U_FAILURE(status)) return NULL;
    UDataMemory* file = udata_open(NULL, "brk", filename, &status);

    if (!U_FAILURE(status)) {
        const void* image = udata_getMemory(file);

        if (image != NULL) {
            result = new RuleBasedBreakIterator(image);
        }
    }
    
    //udata_close(file); // This prevents a leak, but it should be checked whether it is harmful

    return result;
}

// -------------------------------------

// Creates a simple text boundary for sentence breaks.
BreakIterator*
BreakIterator::createSentenceInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "sent";

    if (U_FAILURE(status)) return NULL;
    UDataMemory* file = udata_open(NULL, "brk", filename, &status);

    if (!U_FAILURE(status)) {
        const void* image = udata_getMemory(file);

        if (image != NULL) {
            result = new RuleBasedBreakIterator(image);
        }
    }
    
    //udata_close(file); // This prevents a leak, but it should be checked whether it is harmful

    return result;
}

// -------------------------------------

// Gets all the available locales that has localized text boundary data.
const Locale*
BreakIterator::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// -------------------------------------
// Gets the objectLocale display name in the default locale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(name);
}

// -------------------------------------
// Gets the objectLocale display name in the displayLocale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             const Locale& displayLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(displayLocale, name);
}

// -------------------------------------

// Needed because we declare the copy constructor (in order to prevent synthesizing one) and
// so the default constructor is no longer synthesized.

BreakIterator::BreakIterator()
{
}

BreakIterator::~BreakIterator()
{
}

//eof
