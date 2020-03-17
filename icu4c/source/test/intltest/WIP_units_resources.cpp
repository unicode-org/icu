#include "wip_units_resources.h"
#include "cstring.h"
#include "intltest.h"
#include "unicode/ctest.h"
#include "unicode/measunit.h"
#include "unicode/testlog.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"

// Resources. It should be migrated to a permanent location with updated API,
// once we know what that will look like and where that will be.


void loadResources(const char *usage, const char *inputUnit, const char *outputRegion,
                   UnitConversionResourceBundle *result, UErrorCode &status) {
    // WIP/TODO: pull category from unit? unitQuantities? Or at least cross-check?
    if (U_FAILURE(status)) return;

    MeasureUnit inpUnit = MeasureUnit::forIdentifier(inputUnit, status);
    fprintf(stderr, "MeasureUnit Identifier: %s\n", inpUnit.getIdentifier());
    fprintf(stderr, "MeasureUnit Type: %s\n", inpUnit.getType());
    fprintf(stderr, "MeasureUnit Subtype: %s\n", inpUnit.getSubtype());
    fprintf(stderr, "MeasureUnit SI prefix: %d\n", inpUnit.getSIPrefix(status));

    // FIXME: if kilogram don't drop the SI!
    //
    // FIXME: To find the base unit, we must traverse targets - so this is wrong.
    MeasureUnit inputUnitNoSI = inpUnit.withSIPrefix(UMEASURE_SI_PREFIX_ONE, status);
    if (strcmp(inputUnitNoSI.getIdentifier(), "gram") == 0) inputUnitNoSI = MeasureUnit::getKilogram();
    UMeasureSIPrefix siPrefix = inpUnit.getSIPrefix(status);
    fprintf(stderr, "SI Prefix: %d\n", siPrefix);
    fprintf(stderr, "base unit: %s\n", inputUnitNoSI.getIdentifier());



    // WIP: consider using the LocalUResourceBundlePointer "Smart pointer" class:
    // LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    UResourceBundle *units = ures_openDirect(NULL, "units", &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "open units failed\n");
        return;
    }

    int32_t len = -1;

    // UResourceBundle struct can be reused via "fillin" if we need only one:
    // ures_getByKey(convertUnits, "convertUnits", convertUnits, &status);
    UResourceBundle *convertUnits = ures_getByKey(units, "convertUnits", NULL, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "open convertUnits failed\n");
        return;
    }
    UResourceBundle *unitDetails = ures_getByKey(convertUnits, inputUnitNoSI.getIdentifier(), NULL, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "get convertUnits/%s failed\n", inputUnitNoSI.getIdentifier());
        return;
    }
    const UChar *factor = ures_getStringByKey(unitDetails, "factor", &len, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "get factor failed\n");
        return;
    }
    const UChar *offset = ures_getStringByKey(unitDetails, "offset", &len, &status);
    if (status == U_MISSING_RESOURCE_ERROR) status = U_ZERO_ERROR;
    const UChar *uTarget = ures_getStringByKey(unitDetails, "target", &len, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "get target failed\n");
        return;
    }
    LocalArray<char> targetP(new char[len+1]);
    int32_t outputLen;
    u_strToUTF8(targetP.getAlias(), len + 1, &outputLen, uTarget, len, &status);
    UnicodeString debugOutp = "factor: \"" + UnicodeString(factor) + "\", offset: \"" +
                              UnicodeString(offset) + "\", target: \"" + UnicodeString(uTarget) +
                              "\", len: " + Int64ToUnicodeString(len) + "\n";
    std::string debugOut;
    debugOutp.toUTF8String(debugOut);
    fprintf(stderr, "%s", debugOut.c_str());




    UResourceBundle *unitQuantities = ures_getByKey(units, "unitQuantities", NULL, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "open unitQuantities failed\n");
        return;
    }
    const UChar *uCategory =
        ures_getStringByKey(unitQuantities, targetP.getAlias(), &len, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "unitQuantities/getStringByKey failed: key: %s, len: %d\n", targetP.getAlias(), len);
        return;
    }

    // LocalArray<char> category(new char[len+1]);
    
    int32_t resCap;
    char *appBuf = result->category.getAppendBuffer(len+1, len+1, resCap, status);
    u_strToUTF8(appBuf, len, &outputLen, uCategory, len, &status);
    result->category.append(appBuf, outputLen, status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "category: u_strToUTF8 failed: key: %s, len: %d\n", inputUnitNoSI.getIdentifier(), len);
        return;
    }
    fprintf(stderr, "=== category: %s, srclen: %d, outputLen: %d\n", result->category.data(), len,
            outputLen);

    // look up rates for other preferences.

    ures_close(convertUnits);


    // MeasureUnit testUnit = MeasureUnit::forIdentifier("gram", status);
    // if (U_FAILURE(status)) return;

    // fprintf(stderr, "testUnit Identifier: %s\n", testUnit.getIdentifier());
    // fprintf(stderr, "testUnit Type: %s\n", testUnit.getType());
    // fprintf(stderr, "testUnit Subtype: %s\n", testUnit.getSubtype());
    // fprintf(stderr, "testUnit SI prefix: %d\n", testUnit.getSIPrefix(status));
    // if (U_FAILURE(status)) return;

    // MeasureUnit withPrfx = testUnit.withSIPrefix(UMEASURE_SI_PREFIX_CENTI, status);
    // if (U_FAILURE(status)) return;
    // withPrfx = MeasureUnit::forIdentifier("centigram", status);
    // if (U_FAILURE(status)) return;
    // withPrfx = MeasureUnit::forIdentifier("centiliter", status);
    // if (U_FAILURE(status)) return;
    // fprintf(stderr, "withPrfx Identifier: %s\n", withPrfx.getIdentifier());
    // fprintf(stderr, "withPrfx Type: %s\n", withPrfx.getType());
    // fprintf(stderr, "withPrfx Subtype: %s\n", withPrfx.getSubtype());
    // fprintf(stderr, "withPrfx SI prefix: %d\n", withPrfx.getSIPrefix(status));
    // if (U_FAILURE(status)) return;


    // Temporarily storing unitPreferenceData then category data here:
    UResourceBundle *usageData = ures_getByKey(units, "unitPreferenceData", NULL, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "open unitPreferenceData failed\n");
        return;
    }
    ures_getByKey(usageData, result->category.data(), usageData, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "get unitPreferenceData/%s failed\n", result->category.data());
        return;
    }
    ures_getByKey(usageData, usage, usageData, &status);
    if (U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        ures_getByKey(usageData, "default", usageData, &status);
        if (U_SUCCESS(status)) {
            // FIXME: save "default" somewhere?
            fprintf(stderr, "get usage unitPreferenceData/%s/default succeeded, %s failed\n",
                    result->category.data(), usage);
        } else {
            fprintf(stderr, "get usage unitPreferenceData/%s/%s failed\n", result->category.data(), usage);
            return;
        }
    }
    ures_getByKey(usageData, outputRegion, usageData, &status);
    if (U_FAILURE(status)) {
        status = U_ZERO_ERROR;
        ures_getByKey(usageData, "001", usageData, &status);
        if (U_SUCCESS(status)) {
            fprintf(stderr, "get region unitPreferenceData/%s/<usage>/001 succeeded, %s failed\n",
                    result->category.data(), outputRegion);
        } else {
            fprintf(stderr, "get unitPreferenceData/%s/<usage>/%s failed\n", result->category.data(),
                    outputRegion);
            return;
        }
    }

    UResourceBundle *pref = NULL;
    int32_t numPrefs = ures_getSize(usageData);
    for (int i = 0; i < numPrefs; i++) { // FIXME
        pref = ures_getByIndex(usageData, i, pref, &status);
        if (U_FAILURE(status)) {
            status = U_ZERO_ERROR;
            break;
        }
        int32_t len1;
        const UChar *unitIdent = ures_getStringByKey(pref, "unit", &len1, &status);
        if (U_FAILURE(status)) {
            fprintf(stderr, "open unit failed\n");
            return;
        }
        UnicodeString debugOutp1 = "unit: " + UnicodeString(unitIdent);
        debugOutp1 += "\n";
        std::string debugOut1;
        debugOutp1.toUTF8String(debugOut1);
        fprintf(stderr, "%s", debugOut1.c_str());
    }

    ures_close(pref);
    ures_close(usageData);

    ures_close(units);
}
