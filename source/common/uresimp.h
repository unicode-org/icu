/*
**********************************************************************
*   Copyright (C) 2000-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#ifndef URESIMP_H
#define URESIMP_H

#include "unicode/ures.h"

#include "uresdata.h"

#define kRootLocaleName         "root"

/*
 The default minor version and the version separator must be exactly one
 character long.
*/

#define kDefaultMinorVersion    "0"
#define kVersionSeparator       "."
#define kVersionTag             "Version"

#define MAGIC1 19700503
#define MAGIC2 19641227

#define URES_MAX_ALIAS_LEVEL 256

/*
enum UResEntryType {
    ENTRY_OK = 0,
    ENTRY_GOTO_ROOT = 1,
    ENTRY_GOTO_DEFAULT = 2,
    ENTRY_INVALID = 3
};

typedef enum UResEntryType UResEntryType;
*/

struct UResourceDataEntry;
typedef struct UResourceDataEntry UResourceDataEntry;

struct UResourceDataEntry {
    char *fName; /* name of the locale for bundle - still to decide whether it is original or fallback */
    char *fPath; /* path to bundle - used for distinguishing between resources with the same name */
    uint32_t fCountExisting; /* how much is this resource used */
    ResourceData fData; /* data for low level access */
    UResourceDataEntry *fParent; /*next resource in fallback chain*/
/*    UResEntryType fStatus;*/
    UErrorCode fBogus;
    int32_t fHashKey; /* for faster access in the hashtable */
};

#define RES_BUFSIZE 64
#define RES_PATH_SEPARATOR   '/'
#define RES_PATH_SEPARATOR_S   "/"

struct UResourceBundle {
    const char *fKey; /*tag*/
    UResourceDataEntry *fData; /*for low-level access*/
    char *fVersion;
    char *fResPath; /* full path to the resource: "zh_TW/CollationElements/Sequence" */
    char fResBuf[RES_BUFSIZE];
    int32_t fResPathLen;
    UBool fHasFallback;
    UBool fIsTopLevel;
    uint32_t fMagic1;   /* For determining if it's a stack object */
    uint32_t fMagic2;   /* For determining if it's a stack object */
    int32_t fIndex;
    int32_t fSize;
    ResourceData fResData;
    Resource fRes;

    UResourceDataEntry *fTopLevelData; /* for getting the valid locale */
    const UResourceBundle *fParentRes; /* needed to get the actual locale for a child resource */

};

U_CAPI void U_EXPORT2 ures_initStackObject(UResourceBundle* resB);
U_CFUNC void ures_setIsStackObject( UResourceBundle* resB, UBool state);
U_CFUNC UBool ures_isStackObject(const UResourceBundle* resB);

/* Some getters used by the copy constructor */
U_CFUNC const char* ures_getName(const UResourceBundle* resB);
U_CFUNC const char* ures_getPath(const UResourceBundle* resB);
U_CFUNC void ures_appendResPath(UResourceBundle *resB, const char* toAdd, int32_t lenToAdd);
/*U_CFUNC void ures_setResPath(UResourceBundle *resB, const char* toAdd);*/
U_CFUNC void ures_freeResPath(UResourceBundle *resB);
U_CFUNC void ures_freeRequestedLocale(UResourceBundle *resB);

/* Candidates for export */
U_CFUNC UResourceBundle *ures_copyResb(UResourceBundle *r, const UResourceBundle *original, UErrorCode *status);

/**
 * Returns a resource that can be located using the pathToResource argument. One needs optional package, locale
 * and path inside the locale, for example: "/myData/en/zoneStrings/3". Keys and indexes are supported. Keys
 * need to reference data in named structures, while indexes can reference both named and anonymous resources.
 * Features a fill-in parameter. 
 * 
 * Note, this function does NOT have a syntax for specifying items within a tree.  May want to consider a
 * syntax that delineates between package/tree and resource.  
 *
 * @param pathToResource    a path that will lead to the requested resource
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @draft ICU 2.2
 */
U_CAPI UResourceBundle* U_EXPORT2
ures_findResource(const char* pathToResource, 
                  UResourceBundle *fillIn, UErrorCode *status); 

/**
 * Returns a sub resource that can be located using the pathToResource argument. One needs a path inside 
 * the supplied resource, for example, if you have "en_US" resource bundle opened, you might ask for
 * "zoneStrings/3". Keys and indexes are supported. Keys
 * need to reference data in named structures, while indexes can reference both 
 * named and anonymous resources.
 * Features a fill-in parameter. 
 *
 * @param resourceBundle    a resource
 * @param pathToResource    a path that will lead to the requested resource
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @draft ICU 2.2
 */
U_CAPI UResourceBundle* U_EXPORT2
ures_findSubResource(const UResourceBundle *resB, 
                     char* pathToResource, 
                     UResourceBundle *fillIn, UErrorCode *status);

U_CAPI UResourceBundle* U_EXPORT2 
ures_getByKeyWithFallback(const UResourceBundle *resB, 
                          const char* inKey, 
                          UResourceBundle *fillIn, 
                          UErrorCode *status);


#endif /*URESIMP_H*/
