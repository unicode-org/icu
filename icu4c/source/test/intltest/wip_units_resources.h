#ifndef WIP_UNITS_RESOURCES_H
#define WIP_UNITS_RESOURCES_H

#include "charstr.h"
#include "unicode/localpointer.h"

class UnitConversionResourceBundle {
  public:
    char *inputUnit;
    icu::CharString category;
    char *usage;
    char *outputRegion;
    char *baseUnit;
    icu::LocalArray<UChar*> preferences;
};

/**
 * Loads resources FIXME.
 * @param usage FIXME: result keeps a pointer to this, so must outlive result.
 * @param inputUnit FIXME: result keeps a pointer to this, so must outlive
 * result.
 */
void loadResources(const char *usage, const char *inputUnit, const char *ouptutRegion,
                   UnitConversionResourceBundle *result, UErrorCode &status);

#endif // WIP_UNITS_RESOURCES_H
