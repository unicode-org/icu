// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT_CHECKER_H
#define MESSAGEFORMAT_CHECKER_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_data_model.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "hash.h"
#include "uvector.h"

U_NAMESPACE_BEGIN  namespace message2 {

// Used for checking missing selector annotation errors
class TypeEnvironment : public UMemory {
    public:
    // MessageFormat has a simple type system;
    // variables are either annotated or unannotated
    enum Type {
        Annotated,
        Unannotated
    };
    void extend(const VariableName&, Type, UErrorCode&);
    Type get(const VariableName&) const;
    TypeEnvironment(UErrorCode&);

    virtual ~TypeEnvironment();

    private:
    // Stores variables known to be annotated.
    // All others are assumed to be unannotated
    LocalPointer<UVector> annotated;
}; // class TypeEnvironment

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT_CHECKER_H

#endif // U_HIDE_DEPRECATED_API
// eof

