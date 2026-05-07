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
#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "charstr.h"
#include "genprops.h"
#include "ppucd.h"
#include "toolutil.h"
#include "uoptions.h"

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

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
