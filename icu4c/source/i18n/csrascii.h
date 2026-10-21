// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __CSRASCII_H
#define __CSRASCII_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_CONVERSION

#include "csrecog.h"

U_NAMESPACE_BEGIN

/**
 * Charset recognizer for plain ASCII
 *
 * @internal
 */
class CharsetRecog_ASCII: public CharsetRecognizer {

 public:

    virtual ~CharsetRecog_ASCII();

    const char *getName() const override;

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#match(com.ibm.icu.text.CharsetDetector)
     */
    UBool match(InputText *input, CharsetMatch *results) const override;

};

U_NAMESPACE_END

#endif
#endif /* __CSRASCII_H */
