/*
**********************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File resbund.cpp
*
* Modification History:
*
*   Date        Name        Description
*   02/05/97    aliu        Fixed bug in chopLocale.  Added scanForLocaleInFile
*                           based on code taken from scanForLocale.  Added
*                           constructor which attempts to read resource bundle
*                           from a specific file, without searching other files.
*   02/11/97    aliu        Added UErrorCode return values to constructors.  Fixed
*                           infinite loops in scanForFile and scanForLocale.
*                           Modified getRawResourceData to not delete storage in
*                           localeData and resourceData which it doesn't own.
*                           Added Mac compatibility #ifdefs for tellp() and
*                           ios::nocreate.
*   03/04/97    aliu        Modified to use ExpandingDataSink objects instead of
*                           the highly inefficient ostrstream objects.
*   03/13/97    aliu        Rewrote to load in entire resource bundle and store
*                           it as a Hashtable of ResourceBundleData objects.
*                           Added state table to govern parsing of files.
*                           Modified to load locale index out of new file distinct
*                           from default.txt.
*   03/25/97    aliu        Modified to support 2-d arrays, needed for timezone data.
*                           Added support for custom file suffixes.  Again, needed to
*                           support timezone data.  Improved error handling to detect
*                           duplicate tags and subtags.
*   04/07/97    aliu        Fixed bug in getHashtableForLocale().  Fixed handling of
*                           failing UErrorCode values on entry to API methods.
*                           Fixed bugs in getArrayItem() for negative indices.
*   04/29/97    aliu        Update to use new Hashtable deletion protocol.
*   05/06/97    aliu        Flattened kTransitionTable for HP compiler.  Fixed usage of
*                           CharString.
* 06/11/99      stephen     Removed parsing of .txt files.
*                           Reworked to use new binary format.
*                           Cleaned up.
* 06/14/99      stephen     Removed methods taking a filename suffix.
* 06/22/99      stephen     Added missing T_FileStream_close in parse()
* 11/09/99              weiv            Added getLocale(), rewritten constructForLocale()
* March 2000    weiv        complete overhaul.
*******************************************************************************
*/

#include "unicode/resbund.h"
#include "mutex.h"

#include "rbdata.h"
#include "unistrm.h"
#include "filestrm.h"
#include "cstring.h"
#include "uhash.h"
#include "uresimp.h"

#include <string.h>
#include <wchar.h>

#include "cmemory.h"


/*-----------------------------------------------------------------------------
 * Implementation Notes
 *
 * Resource bundles are read in once, and thereafter cached.
 * ResourceBundle statically keeps track of which files have been
 * read, so we are guaranteed that each file is read at most once.
 * Resource bundles can be loaded from different data directories and
 * will be treated as distinct, even if they are for the same locale.
 *
 * Resource bundles are lightweight objects, which have pointers to
 * one or more shared Hashtable objects containing all the data.
 * Copying would be cheap, but there is no copy constructor, since
 * there wasn't one in the original API.
 *
 * The ResourceBundle parsing mechanism is implemented as a transition
 * network, for easy maintenance and modification.  The network is
 * implemented as a matrix (instead of in code) to make this even
 * easier.  The matrix contains Transition objects.  Each Transition
 * object describes a destination node and an action to take before
 * moving to the destination node.  The source node is encoded by the
 * index of the object in the array that contains it.  The pieces
 * needed to understand the transition network are the enums for node
 * IDs and actions, the parse() method, which walks through the
 * network and implements the actions, and the network itself.  The
 * network guarantees certain conditions, for example, that a new
 * resource will not be closed until one has been opened first; or
 * that data will not be stored into a TaggedList until a TaggedList
 * has been created.  Nonetheless, the code in parse() does some
 * consistency checks as it runs the network, and fails with an
 * U_INTERNAL_PROGRAM_ERROR if one of these checks fails.  If the input
 * data has a bad format, an U_INVALID_FORMAT_ERROR is returned.  If you
 * see an U_INTERNAL_PROGRAM_ERROR the transition matrix has a bug in
 * it.
 *
 * Old functionality of multiple locales in a single file is still
 * supported.  For this reason, LOCALE names override FILE names.  If
 * data for en_US is located in the en.txt file, once it is loaded,
 * the code will not care where it came from (other than remembering
 * which directory it came from).  However, if there is an en_US
 * resource in en_US.txt, that will take precedence.  There is no
 * limit to the number or type of resources that can be stored in a
 * file, however, files are only searched in a specific way.  If
 * en_US_CA is requested, then first en_US_CA.txt is searched, then
 * en_US.txt, then en.txt, then default.txt.  So it only makes sense
 * to put certain locales in certain files.  In this example, it would
 * be logical to put en_US_CA, en_US, and en into the en.txt file,
 * since they would be found there if asked for.  The extreme example
 * is to place all locale resources into default.txt, which should
 * also work.
 *
 * Inheritance is implemented.  For example, xx_YY_zz inherits as
 * follows: xx_YY_zz, xx_YY, xx, default.  Inheritance is implemented
 * as an array of hashtables.  There will be from 1 to 4 hashtables in
 * the array.
 *
 * Fallback files are implemented.  The fallback pattern is Language
 * Country Variant (LCV) -> LC -> L.  Fallback is first done for the
 * requested locale.  Then it is done for the default locale, as
 * returned by Locale::getDefault().  Then the special file
 * default.txt is searched for the default locale.  The overall FILE
 * fallback path is LCV -> LC -> L -> dLCV -> dLC -> dL -> default.
 *
 * Note that although file name searching includes the default locale,
 * once a ResourceBundle object is constructed, the inheritance path
 * no longer includes the default locale.  The path is LCV -> LC -> L
 * -> default.
 *
 * File parsing is lazy.  Nothing is parsed unless it is called for by
 * someone.  So when a ResourceBundle for xx_YY_zz is constructed,
 * only that locale is parsed (along with anything else in the same
 * file).  Later, if the FooBar tag is asked for, and if it isn't
 * found in xx_YY_zz, then xx_YY.txt will be parsed and checked, and
 * so forth, until the chain is exhausted or the tag is found.
 *
 * Thread-safety is implemented around caches, both the cache that
 * stores all the resouce data, and the cache that stores flags
 * indicating whether or not a file has been visited.  These caches
 * delete their storage at static cleanup time, when the process
 * quits.
 *
 * ResourceBundle supports TableCollation as a special case.  This
 * involves having special ResourceBundle objects which DO own their
 * data, since we don't want large collation rule strings in the
 * ResourceBundle cache (these are already cached in the
 * TableCollation cache).  TableCollation files (.ctx files) have the
 * same format as normal resource data files, with a different
 * interpretation, from the standpoint of ResourceBundle.  .ctx files
 * are loaded into otherwise ordinary ResourceBundle objects.  They
 * don't inherit (that's implemented by TableCollation) and they own
 * their data (as mentioned above).  However, they still support
 * possible multiple locales in a single .ctx file.  (This is in
 * practice a bad idea, since you only want the one locale you're
 * looking for, and only one tag will be present
 * ("CollationElements"), so you don't need an inheritance chain of
 * multiple locales.)  Up to 4 locale resources will be loaded from a
 * .ctx file; everything after the first 4 is ignored (parsed and
 * deleted).  (Normal .txt files have no limit.)  Instead of being
 * loaded into the cache, and then looked up as needed, the locale
 * resources are read straight into the ResourceBundle object.
 *
 * The Index, which used to reside in default.txt, has been moved to a
 * new file, index.txt.  This file contains a slightly modified format
 * with the addition of the "InstalledLocales" tag; it looks like:
 *
 * Index {
 *   InstalledLocales {
 *     ar
 *     ..
 *     zh_TW
 *   }
 * }
 */
//-----------------------------------------------------------------------------

const char* ResourceBundle::kDefaultSuffix      = ".res";
const int32_t ResourceBundle::kDefaultSuffixLen = 4;
const char* ResourceBundle::kDefaultFilename    = "root";
const char* ResourceBundle::kDefaultLocaleName  = "root";



//-----------------------------------------------------------------------------

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                const Locale&           locale,
                                UErrorCode&              error)
{
  constructForLocale(path, locale, error);
}

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                UErrorCode&              error)
{
  constructForLocale(path, 
                     Locale::getDefault(), error);
}

/**
 * This constructor is used by TableCollation to load a resource
 * bundle from a specific file, without trying other files.  This is
 * used by the TableCollation caching mechanism.  This is not a public
 * API constructor.  
 */
ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                const char *localeName,
                                UErrorCode&              status)
  :   fRealLocale(localeName)
{
    fItemCache = 0;
    int32_t patlen = path.length();

    if(patlen > 0) {
        char pathName[128];
        path.extract(0, patlen, pathName, "");
        pathName[patlen] = '\0';
        resource = ures_openNoFallback(pathName, localeName, &status);
    } else {
        resource = ures_openNoFallback(0, localeName, &status);
    }

    if(U_SUCCESS(status)) {
        fRealLocale = Locale(localeName);
    }
}

ResourceBundle::ResourceBundle(const wchar_t* path,
                               const Locale& locale, 
                               UErrorCode& err)
{
        constructForLocale(path, locale, err);
}

ResourceBundle::ResourceBundle(const ResourceBundle &other) {
    UErrorCode status = U_ZERO_ERROR;

        if(other.resource->fIsTopLevel == TRUE) {
        constructForLocale(ures_getPath(other.resource), Locale(ures_getName(other.resource)), status);
            } else {
              resource = 0;
              fItemCache = 0;
                resource = copyResb(0, other.resource);
            }
}

ResourceBundle::ResourceBundle(UResourceBundle *res) {
    resource = 0;
    fItemCache = 0;
    resource = copyResb(0, res);
}

ResourceBundle& ResourceBundle::operator=(const ResourceBundle& other)
{
    if(this == &other) {
        return *this;
    }

    if(fItemCache != 0) {
        uhash_close(fItemCache);
        fItemCache = 0;
    }
    if(resource != 0) {
        ures_close(resource);
        resource = 0;
    }
    UErrorCode status = U_ZERO_ERROR;
    if(other.resource->fIsTopLevel == TRUE) {
        constructForLocale(ures_getPath(other.resource), Locale(ures_getName(other.resource)), status);
    } else {
        resource = copyResb(resource, other.resource);
    }
    return *this;
}

ResourceBundle::~ResourceBundle()
{
    if(fItemCache != 0) {
        uhash_close(fItemCache);
    }
    if(resource != 0) {
        ures_close(resource);
    }
}

void
ResourceBundle::deleteValue(void *value) {
    delete (ResourceBundleData *)value;
}

void ResourceBundle::initItemCache(UErrorCode& error) {
    if(fItemCache == 0) {
        fItemCache = uhash_open(uhash_hashChars, uhash_compareChars, &error);
        uhash_setValueDeleter(fItemCache, deleteValue);
    }
}

void 
ResourceBundle::constructForLocale(const UnicodeString& path,
                                   const Locale& locale,
                                   UErrorCode& error)
{
    char name[128];
    fItemCache = 0;
    int32_t patlen = path.length();


    if(patlen > 0) {
        path.extract(0, patlen, name);
        name[patlen] = '\0';
        resource = ures_open(name, locale.getName(), &error);
    } else {
        resource = ures_open(0, locale.getName(), &error);
    }
    if(U_SUCCESS(error)) {
        fRealLocale = Locale(ures_getRealLocale(resource, &error));
    }
}

void 
ResourceBundle::constructForLocale(const wchar_t* path,
                                   const Locale& locale,
                                   UErrorCode& error)
{
    fItemCache = 0;

    if(path != 0) {
        resource = ures_openW(path, locale.getName(), &error);
    } else {
        resource = ures_open(0, locale.getName(), &error);
    }

    if(U_SUCCESS(error)) {
        fRealLocale = Locale(ures_getRealLocale(resource, &error));
    }
}

UnicodeString ResourceBundle::getString(UErrorCode& status) const {
    int32_t len = 0;
    const UChar *r = ures_getString(resource, &len, &status);
    return UnicodeString(TRUE, r, len);
}

const uint8_t *ResourceBundle::getBinary(int32_t& len, UErrorCode& status) const {
  return ures_getBinary(resource, &len, &status);
}

const char *ResourceBundle::getName(void) {
  return ures_getName(resource);
}

const char *ResourceBundle::getKey(void) {
    return ures_getKey(resource);
}

UResType ResourceBundle::getType(void) {
    return ures_getType(resource);
}

int32_t ResourceBundle::getSize(void) const {
    return ures_getSize(resource);
}

UBool ResourceBundle::hasNext(void) const {
    return ures_hasNext(resource);
}

void ResourceBundle::resetIterator(void) {
    ures_resetIterator(resource);
}

ResourceBundle ResourceBundle::getNext(UErrorCode& status) {
    return ResourceBundle(ures_getNextResource(resource, 0, &status));
}

UnicodeString ResourceBundle::getNextString(UErrorCode& status) {
    int32_t len = 0;
    const UChar* r = ures_getNextString(resource, &len, 0, &status);
    return UnicodeString(TRUE, r, len);
}

UnicodeString ResourceBundle::getNextString(const char ** key, UErrorCode& status) {
    int32_t len = 0;
    const UChar* r = ures_getNextString(resource, &len, key, &status);
    return UnicodeString(TRUE, r, len);
}

ResourceBundle ResourceBundle::get(int32_t indexR, UErrorCode& status) const {
    return ResourceBundle(ures_getByIndex(resource, indexR, 0, &status));
}

UnicodeString ResourceBundle::getStringEx(int32_t indexS, UErrorCode& status) const {
    int32_t len = 0;
    const UChar* r = ures_getStringByIndex(resource, indexS, &len, &status);
    return UnicodeString(TRUE, r, len);
}

ResourceBundle ResourceBundle::get(const char* key, UErrorCode& status) const {
    return ResourceBundle(ures_getByKey(resource, key, 0, &status));
}

UnicodeString ResourceBundle::getStringEx(const char* key, UErrorCode& status) const {
    int32_t len = 0;
    const UChar* r = ures_getStringByKey(resource, key, &len, &status);
    return UnicodeString(TRUE, r, len);
}

const char*
ResourceBundle::getVersionNumber()  const
{
  return ures_getVersionNumber(resource);
}

const Locale &ResourceBundle::getLocale(void) const
{
        return fRealLocale;
}

// Start deprecated API
const UnicodeString*
ResourceBundle::getString(  const char              *resourceTag,
                            UErrorCode&              err) const
{
    StringList *sldata;


    if(U_FAILURE(err)) {
        return 0;
    }

    ((ResourceBundle &)(*this)).initItemCache(err); // Semantically const

    sldata = (StringList *)uhash_get(fItemCache, resourceTag);

    if(sldata == 0) {
        const UChar *result = ures_get(resource, resourceTag, &err);
        if(result != 0) {
            UnicodeString *t = new UnicodeString[1];
            t->setTo(TRUE, result, -1);
            sldata = new StringList(t, 1);
        } else {
            err = U_MISSING_RESOURCE_ERROR;
            return 0;
        }
        sldata->fCreationStatus = err;
        uhash_put(fItemCache, (void *)resourceTag, sldata, &err);
    } else {
        err = sldata->fCreationStatus;
    }
    return sldata->fStrings;
}

const UnicodeString*
ResourceBundle::getStringArray( const char             *resourceTag,
                                int32_t&                count,
                                UErrorCode&              err) const
{
    UnicodeString *result;
    StringList *sldata;
    int32_t len=0;

    if(U_FAILURE(err)) {
        return 0;
    }

    ((ResourceBundle &)(*this)).initItemCache(err); // Semantically const

    sldata = (StringList *)uhash_get(fItemCache, resourceTag);

    if(sldata == 0) {
        UResourceBundle array;
        UErrorCode fallbackInfo = U_ZERO_ERROR;
        ures_getByKey(resource, resourceTag, &array, &fallbackInfo);
        if(U_SUCCESS(fallbackInfo)) {
            count = ures_getSize(&array);
            result = new UnicodeString[count];
            const UChar *string = 0;
            for(int32_t i=0; i<count; i++) {
                string = ures_getStringByIndex(&array, i, &len, &err);
                (result+i)->setTo(TRUE, string, len);
            }
            ures_close(&array);
            sldata = new StringList(result, count);
        } else {
            err = U_MISSING_RESOURCE_ERROR;
            return 0;
        }
        sldata->fCreationStatus = fallbackInfo;
        uhash_put(fItemCache, (void *)resourceTag, sldata, &err);
        err = fallbackInfo;
    } else {
        count = sldata->fCount;
        err = sldata->fCreationStatus;
    }
    return sldata->fStrings;
}

const UnicodeString*
ResourceBundle::getArrayItem(   const char             *resourceTag,
                                int32_t                 indexS,
                                UErrorCode&              err) const
{
    int32_t num;
    const UnicodeString *array = getStringArray(resourceTag, num, err);

    if(U_FAILURE(err) || indexS<0 || indexS>=num) {
        err = U_MISSING_RESOURCE_ERROR;
        return 0;
    } else {
        return &array[indexS];
    }
}

const UnicodeString** 
ResourceBundle::get2dArray(const char *resourceTag,
                           int32_t&             rowCount,
                           int32_t&             columnCount,
                           UErrorCode&           err) const
{
    UnicodeString **result = 0;
    String2dList *sldata = 0;
    int32_t len=0;

    if(U_FAILURE(err)) {
        return 0;
    }

    ((ResourceBundle &)(*this)).initItemCache(err); // Semantically const

    sldata = (String2dList *)uhash_get(fItemCache, resourceTag);

    if(sldata == 0) {
        UResourceBundle array;
        UErrorCode fallbackInfo = U_ZERO_ERROR;
        ures_getByKey(resource, resourceTag, &array, &fallbackInfo);
        if(U_SUCCESS(fallbackInfo)) {
            rowCount = ures_getSize(&array);
            if(rowCount > 0) {
                result = new UnicodeString*[rowCount];
                UResourceBundle row;
                ures_getByIndex(&array, 0, &row, &err);
                columnCount = ures_getSize(&row);
                const UChar* string = 0;
                for(int32_t i=0; i<rowCount; i++) {
                    *(result+i) = new UnicodeString[columnCount];                    
                    ures_getByIndex(&array, i, &row, &err);
                    for(int32_t j=0; j<columnCount; j++) {
                        string = ures_getStringByIndex(&row, j, &len, &err);
                        (*(result+i)+j)->setTo(TRUE, string, len);
                    }
                    ures_close(&row);
                }
                sldata = new String2dList(result, rowCount, columnCount);
                sldata->fCreationStatus = fallbackInfo;
                uhash_put(fItemCache, (void *)resourceTag, sldata, &err);
                err = fallbackInfo;
            }
            ures_close(&array);
        } else {
            err = U_MISSING_RESOURCE_ERROR;
            return 0;
        }
    } else {
        rowCount = sldata->fRowCount;
        columnCount = sldata->fColCount;
        err = sldata->fCreationStatus;
    }

    return (const UnicodeString**)sldata->fStrings;
}


const UnicodeString*
ResourceBundle::get2dArrayItem(const char *resourceTag,
                               int32_t              rowIndex,
                               int32_t              columnIndex,
                               UErrorCode&           err) const
{ 
    int32_t rows = 0;
    int32_t columns = 0;

    const UnicodeString** array= get2dArray(resourceTag, rows, columns, err);

    if(array == 0 || rowIndex < 0 || columnIndex < 0 || rowIndex >= rows || columnIndex >= columns) {
        err = U_MISSING_RESOURCE_ERROR;
        return 0;
    } else {
        return &array[rowIndex][columnIndex];
    }
}

const UnicodeString*
ResourceBundle::getTaggedArrayItem( const char             *resourceTag,
                                    const UnicodeString&    itemTag,
                                    UErrorCode&              err) const
{
    StringList *sldata = 0;
    int32_t len = 0;

    if(U_FAILURE(err)) {
        return 0;
    }
    
    char item[256];
    char key[256];
    int32_t taglen = itemTag.length();
    itemTag.extract(0, taglen, key, "");
    key[taglen] = '\0';
    itemTag.extract(0, taglen, item, "");
    item[taglen] = '\0';
    uprv_strcat(key, resourceTag);

    ((ResourceBundle &)(*this)).initItemCache(err); // Semantically const

    sldata = (StringList *)uhash_get(fItemCache, key);

    if(sldata == 0) {
        UResourceBundle table;
        UErrorCode fallbackInfo = U_ZERO_ERROR;
        ures_getByKey(resource, resourceTag, &table, &fallbackInfo);
        if(U_SUCCESS(fallbackInfo)) {
            const UChar *result = ures_getStringByKey(&table, item, &len, &err);
            if(result != 0) {
                UnicodeString *t = new UnicodeString[1];
                t->setTo(TRUE, result, len);
                sldata = new StringList(t, 1);
            } else {
                err = U_MISSING_RESOURCE_ERROR;
                return 0;
            }
            ures_close(&table);
            sldata->fCreationStatus = fallbackInfo;
            uhash_put(fItemCache, key, sldata, &err);
            err = fallbackInfo;
        } else {
            err = U_MISSING_RESOURCE_ERROR;
            return 0;
        }
    } else {
        err = sldata->fCreationStatus;
    }
    return sldata->fStrings;
}

void
ResourceBundle::getTaggedArray( const char             *resourceTag,
                                UnicodeString*&         itemTags,
                                UnicodeString*&         items,
                                int32_t&                numItems,
                                UErrorCode&              err) const
{
    if(U_FAILURE(err)) {
        return;
    }
    UResourceBundle table;
    ures_getByKey(resource, resourceTag, &table, &err);
    if(U_SUCCESS(err)) {
        numItems = ures_getSize(&table);
        itemTags = new UnicodeString[numItems];
        items = new UnicodeString[numItems];
        const UChar *value = 0;
        const char *key = 0;
        int32_t len = 0;
        int16_t indexR = -1;
        res_getNextStringTableItem(&(table.fResData), table.fRes, &value, &key, &len, &indexR);
        while(value != 0) {
            items[indexR-1].setTo(value);
            itemTags[indexR-1].setTo(key);
            res_getNextStringTableItem(&(table.fResData), table.fRes, &value, &key, &len, &indexR);
        }
    } else {
        err = U_MISSING_RESOURCE_ERROR;
        return;
    }
}

//eof







