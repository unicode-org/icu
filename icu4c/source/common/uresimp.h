#ifndef URESIMP_H
#define URESIMP_H

#include "unicode/ures.h"

#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "uresdata.h"
#include "uhash.h"
#include "umutex.h"

#define kRootLocaleName         "root"
#define kIndexLocaleName        "index"
#define kIndexTag               "InstalledLocales"

/*
 The default minor version and the version separator must be exactly one
 character long.
*/

#define kDefaultMinorVersion    "0"
#define kVersionSeparator       "."
#define kVersionTag             "Version"


enum UResEntryType {
    ENTRY_OK = 0,
    ENTRY_GOTO_ROOT = 1,
    ENTRY_GOTO_DEFAULT = 2,
    ENTRY_INVALID = 3
};

typedef enum UResEntryType UResEntryType;

struct UResourceDataEntry;
typedef struct UResourceDataEntry UResourceDataEntry;

struct UResourceDataEntry {
    char *fName; /* name of the locale for bundle - still to decide whether it is original or fallback */
    char *fPath; /* path to bundle - used for distinguishing between resources with the same name */
    uint32_t fCountExisting; /* how much is this resource used */
    ResourceData fData; /* data for low level access */
    UResourceDataEntry *fParent; /*next resource in fallback chain*/
    UResEntryType fStatus;
    UErrorCode fBogus;
    int32_t fHashKey; /* for faster access in the hashtable */
};

struct UResourceBundle {
    const char *fKey; /*tag*/
    char *fVersion;
    UBool fHasFallback;
    UBool fIsTopLevel;
    UBool fIsStackObject;
    UResourceDataEntry *fData; /*for low-level access*/
    int32_t fIndex;
    int32_t fSize;
    ResourceData fResData;
    Resource fRes;
};

/*U_CFUNC UResourceBundle* ures_openNoFallback(UResourceBundle *r, const char* path, const char* localeID, UErrorCode* status);*/
U_CFUNC UResourceBundle* ures_openNoFallback(const char* path, const char* localeID, UErrorCode* status);
U_CFUNC const char* ures_getRealLocale(const UResourceBundle* resourceBundle, UErrorCode* status);
U_CAPI void ures_initStackObject( UResourceBundle* resB);
/*U_CFUNC UChar** ures_listInstalledLocales(const char *path, int32_t* count);*/
U_CFUNC const ResourceData *getFallbackData(const UResourceBundle* resBundle, const char* * resTag, UResourceDataEntry* *realData, Resource *res, UErrorCode *status);
U_CFUNC int32_t hashBundle(const void *parm);
U_CFUNC UBool compareBundles(const void *p1, const void *p2);

/* Candidates for export */
U_CFUNC UResourceBundle *copyResb(UResourceBundle *r, const UResourceBundle *original, UErrorCode *status);
U_CFUNC void copyResbFillIn(UResourceBundle *dest, const UResourceBundle *original);
U_CFUNC const char* ures_getName(const UResourceBundle* resB);
U_CFUNC const char* ures_getPath(const UResourceBundle* resB);
U_CFUNC const char* ures_getTag(const UResourceBundle* resB);
U_CFUNC const ResourceData * ures_getResData(const UResourceBundle* resB);
/*
U_CAPI int32_t U_EXPORT2 ures_getStringArray(const UResourceBundle* resourceBundle, const char* resourceTag, const UChar** array, 
                                             int32_t maxLen, UErrorCode* err);

U_CAPI int32_t U_EXPORT2 ures_get2dStringArray(const UResourceBundle* resourceBundle, const char* resourceTag, const UChar*** matrix, 
                              int32_t maxRows, int32_t cols, UErrorCode* err);

U_CAPI int32_t U_EXPORT2 ures_getTaggedStringArray(const UResourceBundle* resourceBundle, const char* resourceTag, const char** itemTags, 
                                                   const UChar** items, int32_t maxItems, 
                                                   UErrorCode* err);

*/
#endif /*URESIMP_H*/
