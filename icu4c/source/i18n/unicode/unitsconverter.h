// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


#ifndef UNITSCONVERTER_H
#define UNITSCONVERTER_H

#include "unicode/measunit.h"
#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: UnicodeReplacer
 */

U_NAMESPACE_BEGIN

class UnitsConverter;
class UnitsConverterWithFromUnit;
class CompoundUnitsConverterBuilder;
class CompoundUnitsConverter;

class U_I18N_API UnitsConverter {
    public:
        static inline UnitsConverterWithFromUnit from(const MeasureUnit &from) {
                return UnitsConverterWithFromUnit(from);
        }
};

class U_I18N_API UnitsConverterWithFromUnit {
  public:
    inline CompoundUnitsConverterBuilder to(const MeasureUnit &to) {
        return CompoundUnitsConverterBuilder(from_, to);
    }

  private:
    inline UnitsConverterWithFromUnit(const MeasureUnit &from) : from_(from) {}
    MeasureUnit from_;
    
    friend class UnitsConverter;
};

class U_I18N_API CompoundUnitsConverterBuilder {
    public:
        inline CompoundUnitsConverter build(UErrorCode & status) { return CompoundUnitsConverter(from_, to_, status); }

    private:
        MeasureUnit from_;
        MeasureUnit to_;
        inline CompoundUnitsConverterBuilder(const MeasureUnit &from, const MeasureUnit &to)
                : from_(from), to_(to) {}

        friend class UnitsConverterWithFromUnit;
};

class U_I18N_API CompoundUnitsConverter {
    public:
        double convert(double value) const;

    private:
        inline CompoundUnitsConverter(const MeasureUnit &from, const MeasureUnit &to, UErrorCode &status) {}

        friend class CompoundUnitsConverterBuilder;
};

U_NAMESPACE_END

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // UNITSCONVERTER_H
