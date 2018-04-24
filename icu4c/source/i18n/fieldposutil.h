// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __SOURCE_FIELDPOSUTIL_H__
#define __SOURCE_FIELDPOSUTIL_H__

U_NAMESPACE_BEGIN

/**
 * Wraps a UFieldPosition and makes it usable as a FieldPosition. Example:
 *
 * <pre>
 * UFieldPositionWrapper wrapper(myUFPos);
 * u_favorite_function_taking_ufpos(wrapper);
 * // when destructed, the wrapper saves the data back into myUFPos
 * </pre>
 */
class UFieldPositionWrapper : public UMemory {
  public:
    explicit UFieldPositionWrapper(UFieldPosition& ufpos)
            : _ufpos(ufpos) {
        _fpos.setField(_ufpos.field);
        _fpos.setBeginIndex(_ufpos.beginIndex);
        _fpos.setEndIndex(_ufpos.endIndex);
    }

    /** When destructed, copies the information from the fpos into the ufpos. */
    ~UFieldPositionWrapper() {
        _ufpos.field = _fpos.getField();
        _ufpos.beginIndex = _fpos.getBeginIndex();
        _ufpos.endIndex = _fpos.getEndIndex();
    }

    /** Conversion operator to FieldPosition */
    operator FieldPosition&() {
        return _fpos;
    }

  private:
    FieldPosition _fpos;
    UFieldPosition& _ufpos;
};

U_NAMESPACE_END

#endif //__SOURCE_FIELDPOSUTIL_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
