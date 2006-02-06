/*
 **********************************************************************
 *   Copyright (C) 2005-2006, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __CSDETECT_H
#define __CSDETECT_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

class InputText;
class CharsetRecognizer;
struct UCharsetMatch;

struct UCharsetDetector : public UMemory
{
private:
    InputText *textIn;
    UCharsetMatch **resultArray;
    UBool fStripTags;   // If true, setText() will strip tags from input text.
    UBool fFreshTextSet;
    static CharsetRecognizer **fCSRecognizers;
    static int32_t fCSRecognizers_size;
    static void setRecognizers();

public:
    UCharsetDetector();

    ~UCharsetDetector();

    void setText(const char *in, int32_t len);

    const UCharsetMatch * const *detectAll(int32_t &maxMatchesFound, UErrorCode &status);

    const UCharsetMatch *detect(UErrorCode& status);

    void setDeclaredEncoding(const char *encoding, int32_t len) const;

    UBool setStripTagsFlag(UBool flag);

    UBool getStripTagsFlag() const;

    const char *getCharsetName(int32_t index, UErrorCode& status) const;

    static int32_t getDetectableCount(); 

    static UBool cleanup();
};

U_NAMESPACE_END

#endif /* __CSDETECT_H */
