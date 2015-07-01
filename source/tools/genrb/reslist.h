/*
*******************************************************************************
*
*   Copyright (C) 2000-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File reslist.h
*
* Modification History:
*
*   Date        Name        Description
*   02/21/00    weiv        Creation.
*******************************************************************************
*/

#ifndef RESLIST_H
#define RESLIST_H

#define KEY_SPACE_SIZE 65536
#define RESLIST_MAX_INT_VECTOR 2048

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "uresdata.h"
#include "cmemory.h"
#include "cstring.h"
#include "unewdata.h"
#include "ustr.h"

U_CDECL_BEGIN

class TableResource;

typedef struct KeyMapEntry {
    int32_t oldpos, newpos;
} KeyMapEntry;

/* Resource bundle root table */
struct SRBRoot {
  TableResource *fRoot;
  char *fLocale;
  int32_t fIndexLength;
  int32_t fMaxTableLength;
  UBool noFallback; /* see URES_ATT_NO_FALLBACK */
  int8_t fStringsForm; /* default STRINGS_UTF16_V1 */
  UBool fIsPoolBundle;

  char *fKeys;
  KeyMapEntry *fKeyMap;
  int32_t fKeysBottom, fKeysTop;
  int32_t fKeysCapacity;
  int32_t fKeysCount;
  int32_t fLocalKeyLimit; /* key offset < limit fits into URES_TABLE */

  // TODO: UnicodeString
  uint16_t *f16BitUnits;
  int32_t f16BitUnitsCapacity;
  int32_t f16BitUnitsLength;

  const char *fPoolBundleKeys;
  int32_t fPoolBundleKeysLength;
  int32_t fPoolBundleKeysCount;
  int32_t fPoolChecksum;
};

struct SRBRoot *bundle_open(const struct UString* comment, UBool isPoolBundle, UErrorCode *status);
void bundle_write(struct SRBRoot *bundle, const char *outputDir, const char *outputPkg, char *writtenFilename, int writtenFilenameLen, UErrorCode *status);

/* write a java resource file */
void bundle_write_java(struct SRBRoot *bundle, const char *outputDir, const char* outputEnc, char *writtenFilename, 
                       int writtenFilenameLen, const char* packageName, const char* bundleName, UErrorCode *status);

/* write a xml resource file */
/* commented by Jing*/
/* void bundle_write_xml(struct SRBRoot *bundle, const char *outputDir,const char* outputEnc, 
                  char *writtenFilename, int writtenFilenameLen,UErrorCode *status); */

/* added by Jing*/
void bundle_write_xml(struct SRBRoot *bundle, const char *outputDir,const char* outputEnc, const char* rbname,
                  char *writtenFilename, int writtenFilenameLen, const char* language, const char* package, UErrorCode *status);

void bundle_close(struct SRBRoot *bundle, UErrorCode *status);
void bundle_setlocale(struct SRBRoot *bundle, UChar *locale, UErrorCode *status);
int32_t bundle_addtag(struct SRBRoot *bundle, const char *tag, UErrorCode *status);

const char *
bundle_getKeyBytes(struct SRBRoot *bundle, int32_t *pLength);

int32_t
bundle_addKeyBytes(struct SRBRoot *bundle, const char *keyBytes, int32_t length, UErrorCode *status);

void
bundle_compactKeys(struct SRBRoot *bundle, UErrorCode *status);

/* Various resource types */

/*
 * Return a unique pointer to a dummy object,
 * for use in non-error cases when no resource is to be added to the bundle.
 * (NULL is used in error cases.)
 */
struct SResource* res_none(void);

class ArrayResource;
class IntVectorResource;

TableResource *table_open(struct SRBRoot *bundle, const char *tag, const struct UString* comment, UErrorCode *status);

ArrayResource *array_open(struct SRBRoot *bundle, const char *tag, const struct UString* comment, UErrorCode *status);

struct SResource *string_open(struct SRBRoot *bundle, const char *tag, const UChar *value, int32_t len, const struct UString* comment, UErrorCode *status);

struct SResource *alias_open(struct SRBRoot *bundle, const char *tag, UChar *value, int32_t len, const struct UString* comment, UErrorCode *status);

IntVectorResource *intvector_open(struct SRBRoot *bundle, const char *tag,  const struct UString* comment, UErrorCode *status);

struct SResource *int_open(struct SRBRoot *bundle, const char *tag, int32_t value, const struct UString* comment, UErrorCode *status);

struct SResource *bin_open(struct SRBRoot *bundle, const char *tag, uint32_t length, uint8_t *data, const char* fileName, const struct UString* comment, UErrorCode *status);

/* Resource place holder */

struct SResource {
    SResource();
    SResource(SRBRoot *bundle, const char *tag, int8_t type, const UString* comment,
              UErrorCode &errorCode);
    virtual ~SResource();

    UBool isTable() const { return fType == URES_TABLE; }
    UBool isString() const { return fType == URES_STRING; }

    // TODO: virtual methods for dispatch, maybe remove fType

    int8_t   fType;     /* nominal type: fRes (when != 0xffffffff) may use subtype */
    UBool    fWritten;  /* res_write() can exit early */
    uint32_t fRes;      /* resource item word; RES_BOGUS=0xffffffff if not known yet */
    int32_t  fKey;      /* Index into bundle->fKeys; -1 if no key. */
    int      line;      /* used internally to report duplicate keys in tables */
    struct SResource *fNext; /*This is for internal chaining while building*/
    struct UString fComment;
};

class ContainerResource : public SResource {
public:
    ContainerResource(SRBRoot *bundle, const char *tag, int8_t type,
                      const UString* comment, UErrorCode &errorCode)
            : SResource(bundle, tag, type, comment, errorCode),
              fCount(0), fFirst(NULL) {}
    virtual ~ContainerResource();

    // TODO: private with getter?
    uint32_t fCount;
    SResource *fFirst;
};

class TableResource : public ContainerResource {
public:
    TableResource(SRBRoot *bundle, const char *tag,
                  const UString* comment, UErrorCode &errorCode)
            : ContainerResource(bundle, tag, URES_TABLE, comment, errorCode),
              fTableType(URES_TABLE), fRoot(bundle) {}
    virtual ~TableResource();

    void add(SResource *res, int linenumber, UErrorCode &errorCode);

    int8_t fTableType;  // determined by table_write16() for table_preWrite() & table_write()
    SRBRoot *fRoot;
};

class ArrayResource : public ContainerResource {
public:
    ArrayResource(SRBRoot *bundle, const char *tag,
                  const UString* comment, UErrorCode &errorCode)
            : ContainerResource(bundle, tag, URES_ARRAY, comment, errorCode),
              fLast(NULL) {}
    virtual ~ArrayResource();

    void add(SResource *res);

    SResource *fLast;
};

class StringBaseResource : public SResource {
public:
    StringBaseResource(SRBRoot *bundle, const char *tag, int8_t type,
                       const UChar *value, int32_t len,
                       const UString* comment, UErrorCode &errorCode);
    virtual ~StringBaseResource();

    const UChar *getBuffer() const { return fString.getBuffer(); }
    int32_t length() const { return fString.length(); }

    // TODO: private with getter?
    icu::UnicodeString fString;
};

class StringResource : public StringBaseResource {
public:
    StringResource(SRBRoot *bundle, const char *tag, const UChar *value, int32_t len,
                   const UString* comment, UErrorCode &errorCode)
            : StringBaseResource(bundle, tag, URES_STRING, value, len, comment, errorCode),
              fSame(NULL), fSuffixOffset(0), fNumCharsForLength(0) {}
    virtual ~StringResource();

    StringResource *fSame;  // used for duplicates
    int32_t fSuffixOffset;  // this string is a suffix of fSame at this offset
    int8_t fNumCharsForLength;
};

class AliasResource : public StringBaseResource {
public:
    AliasResource(SRBRoot *bundle, const char *tag, const UChar *value, int32_t len,
                  const UString* comment, UErrorCode &errorCode)
            : StringBaseResource(bundle, tag, URES_ALIAS, value, len, comment, errorCode) {}
    virtual ~AliasResource();
};

class IntResource : public SResource {
public:
    IntResource(SRBRoot *bundle, const char *tag, int32_t value,
                const UString* comment, UErrorCode &errorCode);
    virtual ~IntResource();

    // TODO: private with getter?
    int32_t fValue;
};

class IntVectorResource : public SResource {
public:
    IntVectorResource(SRBRoot *bundle, const char *tag,
                      const UString* comment, UErrorCode &errorCode);
    virtual ~IntVectorResource();

    void add(int32_t value, UErrorCode &errorCode);

    // TODO: UVector32
    uint32_t fCount;
    uint32_t *fArray;
};

class BinaryResource : public SResource {
public:
    BinaryResource(SRBRoot *bundle, const char *tag,
                   uint32_t length, uint8_t *data, const char* fileName,
                   const UString* comment, UErrorCode &errorCode);
    virtual ~BinaryResource();

    // TODO: CharString?
    uint32_t fLength;
    uint8_t *fData;
    // TODO: CharString
    char* fFileName;  // file name for binary or import binary tags if any
};

const char *
res_getKeyString(const struct SRBRoot *bundle, const struct SResource *res, char temp[8]);

void res_close(struct SResource *res);

void setIncludeCopyright(UBool val);
UBool getIncludeCopyright(void);

void setFormatVersion(int32_t formatVersion);

void setUsePoolBundle(UBool use);

/* in wrtxml.cpp */
uint32_t computeCRC(char *ptr, uint32_t len, uint32_t lastcrc);

U_CDECL_END
#endif /* #ifndef RESLIST_H */
