// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <iostream>
#include "toolutil.h"
#include "uoptions.h"
#include "cmemory.h"
#include "charstr.h"
#include "cstring.h"
#include "unicode/uchar.h"
#include "unicode/errorcode.h"
#include "unicode/uniset.h"
#include "unicode/putil.h"
#include "unicode/umutablecptrie.h"
#include "writesrc.h"

U_NAMESPACE_USE

/*
 * Global - verbosity
 */
UBool VERBOSE = FALSE;
UBool QUIET = FALSE;

UBool haveCopyright = TRUE;
UCPTrieType trieType = UCPTRIE_TYPE_SMALL;

void handleError(ErrorCode& status, const char* context) {
    if (status.isFailure()) {
        std::cerr << "Error: " << context << ": " << status.errorName() << std::endl;
        exit(status.reset());
    }
}

void dumpBinaryProperty(UProperty uproperty, FILE* f) {
    IcuToolErrorCode status("icuwriteuprops: dumpBinaryProperty");
    const char* fullPropName = u_getPropertyName(uproperty, U_LONG_PROPERTY_NAME);
    const char* shortPropName = u_getPropertyName(uproperty, U_SHORT_PROPERTY_NAME);
    const USet* uset = u_getBinaryPropertySet(uproperty, status);
    handleError(status, fullPropName);

    fputs("[[binary_property]]\n", f);
    fprintf(f, "long_name = \"%s\"\n", fullPropName);
    fprintf(f, "short_name = \"%s\"\n", shortPropName);
    usrc_writeUnicodeSet(f, uset, UPRV_TARGET_SYNTAX_TOML);
}

void dumpEnumeratedProperty(UProperty uproperty, FILE* f) {
    IcuToolErrorCode status("icuwriteuprops: dumpEnumeratedProperty");
    const char* fullPropName = u_getPropertyName(uproperty, U_LONG_PROPERTY_NAME);
    const char* shortPropName = u_getPropertyName(uproperty, U_SHORT_PROPERTY_NAME);
    const UCPMap* umap = u_getIntPropertyMap(uproperty, status);
    handleError(status, fullPropName);

    fputs("[[enum_property]]\n", f);
    fprintf(f, "long_name = \"%s\"\n", fullPropName);
    fprintf(f, "short_name = \"%s\"\n", shortPropName);
    usrc_writeUCPMap(f, umap, uproperty, UPRV_TARGET_SYNTAX_TOML);
    fputs("\n", f);

    U_ASSERT(u_getIntPropertyMinValue(uproperty) >= 0);
    int32_t maxValue = u_getIntPropertyMaxValue(uproperty);
    U_ASSERT(maxValue >= 0);
    UCPTrieValueWidth width = UCPTRIE_VALUE_BITS_32;
    if (maxValue <= 0xff) {
        width = UCPTRIE_VALUE_BITS_8;
    } else if (maxValue <= 0xffff) {
        width = UCPTRIE_VALUE_BITS_16;
    }
    LocalUMutableCPTriePointer builder(umutablecptrie_fromUCPMap(umap, status));
    LocalUCPTriePointer utrie(umutablecptrie_buildImmutable(
        builder.getAlias(),
        trieType,
        width,
        status));
    handleError(status, fullPropName);

    fputs("[enum_property.code_point_trie]\n", f);
    usrc_writeUCPTrie(f, shortPropName, utrie.getAlias(), UPRV_TARGET_SYNTAX_TOML);
}

enum {
    OPT_HELP_H,
    OPT_HELP_QUESTION_MARK,
    OPT_COPYRIGHT,
    OPT_TRIE_TYPE,
    OPT_VERSION,
    OPT_DESTDIR,
    OPT_VERBOSE,
    OPT_QUIET,

    OPT_COUNT
};

#define UOPTION_TRIE_TYPE UOPTION_DEF("trie-type", 't', UOPT_REQUIRES_ARG)

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_COPYRIGHT,
    UOPTION_TRIE_TYPE,
    UOPTION_VERSION,
    UOPTION_DESTDIR,
    UOPTION_VERBOSE,
    UOPTION_QUIET,
};

int main(int argc, char* argv[]) {

    U_MAIN_INIT_ARGS(argc, argv);

    /* preset then read command line options */
    options[OPT_DESTDIR].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, UPRV_LENGTHOF(options), options);

    if(options[OPT_VERSION].doesOccur) {
        printf("icuwriteuprops version %s, ICU tool to write Unicode property .toml files\n",
               U_ICU_DATA_VERSION);
        printf("%s\n", U_COPYRIGHT_STRING);
        exit(0);
    }

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    } else if(argc<2) {
        argc=-1;
    }

    if(argc<0 || options[OPT_HELP_H].doesOccur || options[OPT_HELP_QUESTION_MARK].doesOccur) {
        FILE *stdfile=argc<0 ? stderr : stdout;
        fprintf(stdfile,
            "usage: %s [-options] properties...\n"
            "\tdump Unicode property data to .toml files\n"
            "options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-V or --version     show a version message\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t-t or --trie-type   set the trie type (small or fast, default small)\n"
            "\t-d or --destdir     destination directory, followed by the path\n"
            "\t-v or --verbose     Turn on verbose output\n"
            "\t-q or --quiet       do not display warnings and progress\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the options values */
    haveCopyright = options[OPT_COPYRIGHT].doesOccur;
    const char *destdir = options[OPT_DESTDIR].value;
    VERBOSE = options[OPT_VERBOSE].doesOccur;
    QUIET = options[OPT_QUIET].doesOccur;

    if (options[OPT_TRIE_TYPE].doesOccur) {
        if (uprv_strcmp(options[OPT_TRIE_TYPE].value, "fast") == 0) {
            trieType = UCPTRIE_TYPE_FAST;
        } else if (uprv_strcmp(options[OPT_TRIE_TYPE].value, "small") == 0) {
            trieType = UCPTRIE_TYPE_SMALL;
        } else {
            fprintf(stderr, "Invalid option for --trie-type (must be small or fast)\n");
            return U_ILLEGAL_ARGUMENT_ERROR;
        }
    }

    for (int i=1; i<argc; i++) {
        auto propName = argv[i];
        UProperty propEnum = u_getPropertyEnum(propName);
        if (propEnum == UCHAR_INVALID_CODE) {
            std::cerr << "Error: Invalid property alias: " << propName << std::endl;
            return U_ILLEGAL_ARGUMENT_ERROR;
        }

        IcuToolErrorCode status("icuwriteuprops");
        CharString outFileName;
        if (destdir != nullptr && *destdir != 0) {
            outFileName.append(destdir, status).ensureEndsWithFileSeparator(status);
        }
        outFileName.append(propName, status);
        outFileName.append(".toml", status);
        handleError(status, propName);

        FILE* f = fopen(outFileName.data(), "w");
        if (f == nullptr) {
            std::cerr << "Unable to open file: " << outFileName.data() << std::endl;
            return U_FILE_ACCESS_ERROR;
        }
        if (!QUIET) {
            std::cout << "Writing to: " << outFileName.data() << std::endl;
        }

        if (haveCopyright) {
            usrc_writeCopyrightHeader(f, "#", 2021);
        }
        usrc_writeFileNameGeneratedBy(f, "#", propName, "icuwriteuprops.cpp");

        if (propEnum < UCHAR_BINARY_LIMIT) {
            dumpBinaryProperty(propEnum, f);
        } else if (UCHAR_INT_START <= propEnum && propEnum <= UCHAR_INT_LIMIT) {
            dumpEnumeratedProperty(propEnum, f);
        } else {
            std::cerr << "Don't know how to write property: " << propEnum << std::endl;
            return U_INTERNAL_PROGRAM_ERROR;
        }

        fclose(f);
    }
}
