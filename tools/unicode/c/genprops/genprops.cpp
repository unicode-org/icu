// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 1999-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec08
*   created by: Markus W. Scherer
*
*   This program parses the ppucd.txt preparsed Unicode Character Database file
*   and writes several source and binary files into the ICU source tree.
*/

#include <stdio.h>
#include <stdlib.h>
#include <algorithm>
#include <cctype>
#include <string>
#include <string_view>
#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "charstr.h"
#include "genprops.h"
#include "pnames_impl.h"
#include "ppucd.h"
#include "toolutil.h"
#include "uoptions.h"
#include "writesrc.h"

U_NAMESPACE_USE

UBool beVerbose=false;
UBool beQuiet=false;

PropsBuilder::PropsBuilder() {}
PropsBuilder::~PropsBuilder() {}
void PropsBuilder::setUnicodeVersion(const UVersionInfo) {}
void PropsBuilder::setAlgNamesRange(UChar32, UChar32,
                                    const char *, const char *, UErrorCode &) {}
void PropsBuilder::setProps(const UniProps &, const UnicodeSet &, UErrorCode &) {}
void PropsBuilder::parseUnidataFiles(const char *, UErrorCode &) {}
void PropsBuilder::build(UErrorCode &) {}
void PropsBuilder::writeCSourceFile(const char *, UErrorCode &) {}
void PropsBuilder::writeJavaSourceFile(const char *, UErrorCode &) {}
void PropsBuilder::writeBinaryData(const char *, UBool, UErrorCode &) {}

void writePropertyNumbersFile(const char *path, UErrorCode &errorCode);

enum {
    HELP_H,
    HELP_QUESTION_MARK,
    VERBOSE,
    QUIET,
    COPYRIGHT
};

/* Keep these values in sync with the above enums */
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_QUIET,
    UOPTION_COPYRIGHT
};

extern int
main(int argc, char* argv[]) {
    U_MAIN_INIT_ARGS(argc, argv);
    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<2 || options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur) {
        /*
         * Broken into chunks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
            "Usage: %s [-options] path/to/ICU/src/root\n"
            "\n"
            "Reads the preparsed UCD file path/to/ICU/src/root/source/data/unidata/ppucd.txt and\n"
            "writes source and binary data files with the character properties.\n"
            "(UCD=Unicode Character Database)\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-v or --verbose     verbose output\n"
            "\t-q or --quiet       no output\n"
            "\t-c or --copyright   include a copyright notice\n");
        return argc<2 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the options values */
    beVerbose=options[VERBOSE].doesOccur;
    beQuiet=options[QUIET].doesOccur;

    /* initialize */
    IcuToolErrorCode errorCode("genprops");
    LocalPointer<PNamesBuilder> pnamesBuilder(createPNamesBuilder(errorCode));
    LocalPointer<PropsBuilder> corePropsBuilder(createCorePropsBuilder(errorCode));
    LocalPointer<PropsBuilder> bidiPropsBuilder(createBiDiPropsBuilder(errorCode));
    LocalPointer<PropsBuilder> casePropsBuilder(createCasePropsBuilder(errorCode));
    LocalPointer<PropsBuilder> layoutPropsBuilder(createLayoutPropsBuilder(errorCode));
    LocalPointer<PropsBuilder> emojiPropsBuilder(createEmojiPropsBuilder(errorCode));
    LocalPointer<PropsBuilder> namesPropsBuilder(createNamesPropsBuilder(errorCode));
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops: unable to create PropsBuilders - %s\n", errorCode.errorName());
        return errorCode.reset();
    }

    CharString icuSrcRoot(argv[1], errorCode);

    CharString icuSource(icuSrcRoot, errorCode);
    icuSource.appendPathPart("source", errorCode);

    CharString icuSourceData(icuSource, errorCode);
    icuSourceData.appendPathPart("data", errorCode);

    CharString unidataPath(icuSourceData, errorCode);
    unidataPath.appendPathPart("unidata", errorCode);

    CharString ppucdPath(unidataPath, errorCode);
    ppucdPath.appendPathPart("ppucd.txt", errorCode);

    PreparsedUCD ppucd(ppucdPath.data(), errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops: unable to open %s - %s\n",
                ppucdPath.data(), errorCode.errorName());
        return errorCode.reset();
    }

    // The PNamesBuilder uses preparsed pnames_data.h.
    pnamesBuilder->build(errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: PNamesBuilder::build() failed - %s\n",
                errorCode.errorName());
        return errorCode.reset();
    }
    ppucd.setPropertyNames(pnamesBuilder->getPropertyNames());

    writePropertyNumbersFile(unidataPath.data(), errorCode);

    PreparsedUCD::LineType lineType;
    UnicodeSet newValues;
    while((lineType=ppucd.readLine(errorCode))!=PreparsedUCD::NO_LINE) {
        if(ppucd.lineHasPropertyValues()) {
            const UniProps *props=ppucd.getProps(newValues, errorCode);
            corePropsBuilder->setProps(*props, newValues, errorCode);
            bidiPropsBuilder->setProps(*props, newValues, errorCode);
            casePropsBuilder->setProps(*props, newValues, errorCode);
            layoutPropsBuilder->setProps(*props, newValues, errorCode);
            emojiPropsBuilder->setProps(*props, newValues, errorCode);
            namesPropsBuilder->setProps(*props, newValues, errorCode);
        } else if(lineType==PreparsedUCD::UNICODE_VERSION_LINE) {
            const UVersionInfo &version=ppucd.getUnicodeVersion();
            corePropsBuilder->setUnicodeVersion(version);
            bidiPropsBuilder->setUnicodeVersion(version);
            casePropsBuilder->setUnicodeVersion(version);
            layoutPropsBuilder->setUnicodeVersion(version);
            emojiPropsBuilder->setUnicodeVersion(version);
            namesPropsBuilder->setUnicodeVersion(version);
        } else if(lineType==PreparsedUCD::ALG_NAMES_RANGE_LINE) {
            UChar32 start, end;
            if(ppucd.getRangeForAlgNames(start, end, errorCode)) {
                const char *type=ppucd.nextField();
                const char *prefix=ppucd.nextField();  // nullptr if type==hangul
                namesPropsBuilder->setAlgNamesRange(start, end, type, prefix, errorCode);
            }
        }
        if(errorCode.isFailure()) {
            fprintf(stderr,
                    "genprops: error parsing or setting values from ppucd.txt line %ld - %s\n",
                    (long)ppucd.getLineNumber(), errorCode.errorName());
            return errorCode.reset();
        }
    }

    emojiPropsBuilder->parseUnidataFiles(unidataPath.data(), errorCode);

    if (!beQuiet) { puts(""); }
    corePropsBuilder->build(errorCode);
    if (!beQuiet) { puts(""); }
    bidiPropsBuilder->build(errorCode);
    if (!beQuiet) { puts(""); }
    casePropsBuilder->build(errorCode);
    if (!beQuiet) { puts(""); }
    layoutPropsBuilder->build(errorCode);
    if (!beQuiet) { puts(""); }
    emojiPropsBuilder->build(errorCode);
    if (!beQuiet) { puts(""); }
    namesPropsBuilder->build(errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops error: failure finalizing the data - %s\n",
                errorCode.errorName());
        return errorCode.reset();
    }

    // Write the files with the generated data.
    CharString sourceCommon(icuSource, errorCode);
    sourceCommon.appendPathPart("common", errorCode);

    CharString sourceDataIn(icuSourceData, errorCode);
    sourceDataIn.appendPathPart("in", errorCode);

    UBool withCopyright=options[COPYRIGHT].doesOccur;

    pnamesBuilder->writeCSourceFile(sourceCommon.data(), errorCode);
    pnamesBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    corePropsBuilder->writeCSourceFile(sourceCommon.data(), errorCode);
    corePropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    bidiPropsBuilder->writeCSourceFile(sourceCommon.data(), errorCode);
    bidiPropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    casePropsBuilder->writeCSourceFile(sourceCommon.data(), errorCode);
    casePropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    namesPropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    layoutPropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);
    emojiPropsBuilder->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);

    return errorCode;
}

bool startsWith(std::string_view s, std::string_view prefix) {
    return s.rfind(prefix, 0) ==0;
}

bool removePrefix(std::string_view &s, std::string_view prefix) {
    // C++20 string_view has .starts_with(prefix)...
    if (startsWith(s, prefix)) {
        s.remove_prefix(prefix.length());
        return true;
    }
    return false;
}

std::string toUpper(std::string_view s) {
    // The std::transform() needs at least as many existing output elements
    // as input elements. We can copy s, or create a same-length string of whatever.
    std::string u(s.length(), '.');
    std::transform(s.begin(), s.end(), u.begin(), [](uint8_t c) {
        return std::toupper(c);
    });
    return u;
}

std::string_view javaNameFromCName(std::string_view cName,
                                   std::string_view propName, std::string_view valueName) {
    // C API names are in the global namespace and usually have a distinguishing prefix
    // that is related to the short property name.
    // Almost all of the Java API names are the same after stripping the prefix.
    // Some very old API names differ.
    if (cName.empty()) {
        return cName;
    }
    if (propName == "gc") {
        if (cName == "U_CONTROL_CHAR") {
            return "CONTROL";
        }
        if (cName == "U_FORMAT_CHAR") {
            return "FORMAT";
        }
        if (cName == "U_PRIVATE_USE_CHAR") {
            return "PRIVATE_USE";
        }
    }
    if (propName == "gcm" && !valueName.empty()) {
        // Prefix "U_GC_" but there are no API constants for gcm values in ICU4J.
        return "";
    }
    if (removePrefix(cName, "UCHAR_")) {
        return cName;
    }
    if (removePrefix(cName, "UBLOCK_")) {
        return cName;
    }
    if (removePrefix(cName, "USCRIPT_")) {
        return cName;
    }
    if (removePrefix(cName, "UNORM_")) {
        return cName;
    }
    std::string prefix = "U_" + toUpper(propName) + "_";
    if (removePrefix(cName, prefix)) {
        return cName;
    }
    if (removePrefix(cName, "U_")) {
        return cName;
    }
    return cName;
}

void
writePropertyNumbersFile(const char *path, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    FILE *f = usrc_createTextData(path, "prop_numbers.txt", 2026,
                                  "icu/tools/unicode/c/genprops/genprops.cpp");
    if (f == nullptr) {
        errorCode = U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("# Informative data for coordination among ICU4C, ICU4J, and ICU4X.\n"
          "# Associates character properties with numeric values of property API constants.\n"
          "#\n"
          "# Format:\n"
          "#\n"
          "# property;value;number;apiName;javaName\n"
          "#\n"
          "# The property and value fields contain property names and property value names\n"
          "# (\"aliases\" in Unicode Character Database parlance).\n"
          "# Each name/alias is the short name/alias of the property,\n"
          "# or the long one is no short one is defined.\n"
          "#\n"
          "# Most of these properties are Unicode properties.\n"
          "# Some are ICU-specific properties.\n"
          "#\n"
          "# If the value field is empty, then the number is the numeric value of the property.\n"
          "# Otherwise, it is the numeric value of the property's value.\n"
          "#\n"
          "# The apiName is the name of the ICU4C API constant.\n"
          "# It is empty if the ICU4C API simply uses integer values (e.g., binary values & ccc).\n"
          "#\n"
          "# The javaName is the unqualified name of the ICU4J API constant.\n"
          "# Some constants exist in C but not in Java.\n"
          "#\n"
          "# Values for binary properties are omitted.\n"
          "# They use 0 for no/false and 1 for yes/true.\n"
          "#\n"
          "# The properties are grouped as in the ICU4C API,\n"
          "# according to their property value types as reflected in ICU APIs.\n",
          f);

    // Enumerate the properties from the generated pnames_data.h
    // which defines their names and numeric values.
    int32_t propertiesLength = 0;
    const Property *properties = getPNamesProperties(propertiesLength);

    bool addBlankLine = false;
    for (int32_t propIndex = 0; propIndex < propertiesLength; ++propIndex) {
        const Property &prop = properties[propIndex];
        UProperty propEnum = static_cast<UProperty>(prop.enumValue);
        bool firstOfType = true;
        switch (propEnum) {
            case UCHAR_BINARY_START:
                fputs("\n# Binary properties\n", f);
                break;
            case UCHAR_INT_START:
                fputs("\n# Enumerated & integer properties\n", f);
                break;
            case UCHAR_MASK_START:
                fputs("\n# Bit-mask-valued properties\n", f);
                break;
            case UCHAR_DOUBLE_START:
                fputs("\n# Numeric properties\n", f);
                break;
            case UCHAR_STRING_START:
                fputs("\n# String-valued properties\n", f);
                break;
            case UCHAR_OTHER_PROPERTY_START:
                fputs("\n# Properties with unusual value types\n", f);
                break;
            default:
                firstOfType = false;
                break;
        }

        if (addBlankLine && !firstOfType) {
            fputs("\n", f);
        }
        addBlankLine = false;

        // Property
        const char *propName = prop.aliases[0];
        if (propName[0] == 0) {
            // no short name
            propName = prop.aliases[1];
        }
        std::string javaName = std::string(javaNameFromCName(prop.apiName, propName, ""));
        fprintf(f, "%s;;%ld;%s;%s\n",
                propName, (long)propEnum,
                prop.apiName, javaName.c_str());

        // Property values
        if (propEnum >= UCHAR_BINARY_LIMIT && prop.values != nullptr) {
            for (int32_t valueIndex = 0; valueIndex < prop.valueCount; ++valueIndex) {
                const Value &value = prop.values[valueIndex];
                int32_t valueEnum = value.enumValue;
                const char *valueName = value.aliases[0];
                if (valueName[0] == 0) {
                    // no short name
                    valueName = value.aliases[1];
                }
                std::string javaName = std::string(
                    javaNameFromCName(value.apiName, propName, valueName));
                fprintf(f, "%s;%s;%ld;%s;%s\n",
                        propName, valueName, (long)valueEnum,
                        value.apiName, javaName.c_str());
            }
            addBlankLine = true;
        }
    }
    fclose(f);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
