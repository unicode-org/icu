/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  nameprep.h
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#ifndef NAMEPREP_H
#define NAMEPREP_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_IDNA

#include "strprep.h"
#include "unicode/uniset.h"


U_NAMESPACE_BEGIN
/*
   A profile of stringprep MUST include all of the following:

   - The intended applicability of the profile

   - The character repertoire that is the input and output to stringprep
     (which is Unicode 3.2 for this version of stringprep)

   - The mapping tables from this document used (as described in section
     3)

   - Any additional mapping tables specific to the profile

   - The Unicode normalization used, if any (as described in section 4)

   - The tables from this document of characters that are prohibited as
     output (as described in section 5)

   - The bidirectional string testing used, if any (as described in
     section 6)

   - Any additional characters that are prohibited as output specific to
     the profile
*/


class NamePrep: public StringPrep {
public :
    NamePrep(UErrorCode& status);
    
    virtual inline ~NamePrep(){};

    virtual inline UBool isNotProhibited(UChar32 ch);
    
    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.6
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.6
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

private:        
    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

inline UBool NamePrep::isNotProhibited(UChar32 ch){
    return (UBool)(ch == 0x0020); /* ASCII_SPACE */
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_IDNA */

#endif

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
