/*
**********************************************************************
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File URES.H (formerly CRESBUND.H)
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   02/22/99    damiba      overhaul.
*   04/04/99    helena      Fixed internal header inclusion.
*   04/15/99    Madhu       Updated Javadoc  
*   06/14/99    stephen     Removed functions taking a filename suffix.
*   07/20/99    stephen     Language-independent ypedef to void*
*   11/09/99    weiv        Added ures_getLocale()
*   06/24/02    weiv        Added support for resource sharing
******************************************************************************
*/

#ifndef URES_H
#define URES_H

#include "unicode/utypes.h"
#include "unicode/uloc.h"

/**
 * \file
 * \brief C API: Resource Bundle 
 *
 * <h2>C API: Resource Bundle</h2>
 *
 * C API representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specific information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * Resource bundles in ICU4C are currently defined using text files which conform to the following
 * <a href="http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/bnf_rb.txt">BNF definition</a>.
 * More on resource bundle concepts and syntax can be found in the 
 * <a href="http://oss.software.ibm.com/icu/userguide/ResourceManagement.html">Users Guide</a>.
 * <P>
 */

/**
 * UResourceBundle is an opaque type for handles for resource bundles in C APIs.
 * @stable ICU 2.0
 */
struct UResourceBundle;

/**
 * @stable ICU 2.0
 */
typedef struct UResourceBundle UResourceBundle;

/**
 * Numeric constants for types of resource items.
 * @see ures_getType
 * @stable ICU 2.0
 */
typedef enum {
    /** Resource type constant for "no resource". @draft ICU 2.6 */
    URES_NONE=-1,

    /** Resource type constant for 16-bit Unicode strings. @draft ICU 2.6 */
    URES_STRING=0,

    /** Resource type constant for binary data. @draft ICU 2.6 */
    URES_BINARY=1,

    /** Resource type constant for tables of key-value pairs. @draft ICU 2.6 */
    URES_TABLE=2,

    /**
     * Resource type constant for aliases;
     * internally stores a string which identifies the actual resource
     * storing the data (can be in a different resource bundle).
     * Resolved internally before delivering the actual resource through the API.
     * @draft ICU 2.6
     */
    URES_ALIAS=3,

    /**
     * Internal use only.
     * Alternative resource type constant for tables of key-value pairs.
     * Never returned by ures_getType().
     * @internal
     */
    URES_TABLE32=4,

    /**
     * Resource type constant for a single 28-bit integer, interpreted as
     * signed or unsigned by the ures_getInt() or ures_getUInt() function.
     * @see ures_getInt
     * @see ures_getUInt
     * @draft ICU 2.6
     */
    URES_INT=7,

    /** Resource type constant for arrays of resources. @draft ICU 2.6 */
    URES_ARRAY=8,

    /**
     * Resource type constant for vectors of 32-bit integers.
     * @see ures_getIntVector
     * @draft ICU 2.6
     */
    URES_INT_VECTOR=14,

#ifndef U_HIDE_DEPRECATED_API
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_NONE=URES_NONE,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_STRING=URES_STRING,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_BINARY=URES_BINARY,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_TABLE=URES_TABLE,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_ALIAS=URES_ALIAS,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_INT=URES_INT,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_ARRAY=URES_ARRAY,
    /** @deprecated ICU 2.6 Use the URES_ constant instead. */
    RES_INT_VECTOR=URES_INT_VECTOR,
#endif /* U_HIDE_DEPRECATED_API */

    /** @deprecated ICU 2.6 Not used. */
    RES_RESERVED=15
} UResType;

/*
 * Functions to create and destroy resource bundles.
 */

/**
 * Opens a UResourceBundle, from which users can extract strings by using
 * their corresponding keys.
 * Note that the caller is responsible of calling <TT>ures_close</TT> on each succesfully
 * opened resource bundle.
 * @param path    string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 *                e.g. "/usr/resource/my_app/resources/guimessages" on a Unix system.
 *                if NULL, ICU default data files will be used.
 * @param locale  specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 *                
 * @param status  fills in the outgoing error code.
 * The UErrorCode err parameter is used to return status information to the user. To
 * check whether the construction succeeded or not, you should check the value of
 * U_SUCCESS(err). If you wish more detailed information, you can check for
 * informational status results which still indicate success. U_USING_FALLBACK_WARNING
 * indicates that a fall back locale was used. For example, 'de_CH' was requested,
 * but nothing was found there, so 'de' was used. U_USING_DEFAULT_WARNING indicates that
 * the default locale data or root locale data was used; neither the requested locale 
 * nor any of its fall back locales could be found. Please see the users guide for more 
 * information on this topic.
 * @return      a newly allocated resource bundle.
 * @see ures_close
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle*  U_EXPORT2 
ures_open(const char*    path,
          const char*  locale, 
          UErrorCode*     status);


/** This function does not care what kind of localeID is passed in. It simply opens a bundle with 
 *  that name. Fallback mechanism is disabled for the new bundle. If the requested bundle contains
 *  an %%ALIAS directive, the results are undefined.
 * @param path    string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 *                e.g. "/usr/resource/my_app/resources/guimessages" on a Unix system.
 *                if NULL, ICU default data files will be used.
 * @param locale  specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 *                
 * @param status fills in the outgoing error code. Either U_ZERO_ERROR or U_MISSING_RESOURCE_ERROR
 * @return      a newly allocated resource bundle or NULL if it doesn't exist.
 * @see ures_close
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle* U_EXPORT2 
ures_openDirect(const char* path, 
                const char* locale, 
                UErrorCode* status);

/**
 * Same as ures_open() but takes a const UChar *path.
 * This path will be converted to char * using the default converter,
 * then ures_open() is called.
 *
 * @param path    string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 * @param locale  specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 * @param status  fills in the outgoing error code.
 * @return      a newly allocated resource bundle.
 * @see ures_open
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle* U_EXPORT2 
ures_openU(const UChar* path, 
           const char* locale, 
           UErrorCode* status);

/**
 * Returns the number of strings/arrays in resource bundles.
 * Better to use ures_getSize, as this function will be deprecated. 
 *
 *@param resourceBundle resource bundle containing the desired strings
 *@param resourceKey key tagging the resource
 *@param err fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_FALLBACK_WARNING </TT>
 *@return: for    <STRONG>Arrays</STRONG>: returns the number of resources in the array
 *                <STRONG>Tables</STRONG>: returns the number of resources in the table
 *                <STRONG>single string</STRONG>: returns 1
 *@see ures_getSize
 * @deprecated ICU 2.8 User ures_getSize instead
 */
U_DEPRECATED int32_t U_EXPORT2 
ures_countArrayItems(const UResourceBundle* resourceBundle,
                     const char* resourceKey,
                     UErrorCode* err);
/**
 * Close a resource bundle, all pointers returned from the various ures_getXXX calls
 * on this particular bundle should be considered invalid henceforth.
 *
 * @param resourceBundle a pointer to a resourceBundle struct. Can be NULL.
 * @see ures_open
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ures_close(UResourceBundle* resourceBundle);

/**
 * Return the version number associated with this ResourceBundle as a string. Please
 * use ures_getVersion as this function is going to be deprecated.
 *
 * @param resourceBundle The resource bundle for which the version is checked.
 * @return  A version number string as specified in the resource bundle or its parent.
 *          The caller does not own this string.
 * @see ures_getVersion
 * @deprecated ICU 2.8 Use ures_getVersion instead.
 */
U_DEPRECATED const char* U_EXPORT2 
ures_getVersionNumber(const UResourceBundle*   resourceBundle);

/**
 * Return the version number associated with this ResourceBundle as an 
 * UVersionInfo array.
 *
 * @param resB The resource bundle for which the version is checked.
 * @param versionInfo A UVersionInfo array that is filled with the version number
 *                    as specified in the resource bundle or its parent.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ures_getVersion(const UResourceBundle* resB, 
                UVersionInfo versionInfo);

/**
 * Return the name of the Locale associated with this ResourceBundle. This API allows
 * you to query for the real locale of the resource. For example, if you requested 
 * "en_US_CALIFORNIA" and only "en_US" bundle exists, "en_US" will be returned. 
 * For subresources, the locale where this resource comes from will be returned.
 * If fallback has occured, getLocale will reflect this.
 *
 * @param resourceBundle resource bundle in question
 * @param status just for catching illegal arguments
 * @return  A Locale name
 * @deprecated ICU 2.8 Use ures_getLocaleByType instead.
 */
U_DEPRECATED const char* U_EXPORT2 
ures_getLocale(const UResourceBundle* resourceBundle, 
               UErrorCode* status);


/**
 * Return the name of the Locale associated with this ResourceBundle. 
 * You can choose between requested, valid and real locale.
 *
 * @param resourceBundle resource bundle in question
 * @param type You can choose between requested, valid and actual
 *             locale. For description see the definition of
 *             ULocDataLocaleType in uloc.h
 * @param status just for catching illegal arguments
 * @return  A Locale name
 * @draft ICU 2.8
 */
U_DRAFT const char* U_EXPORT2 
ures_getLocaleByType(const UResourceBundle* resourceBundle, 
                     ULocDataLocaleType type, 
                     UErrorCode* status);


/**
 * Same as ures_open() but uses the fill-in parameter instead of allocating
 * a bundle, if r!=NULL.
 * TODO need to revisit usefulness of this function
 *      and usage model for fillIn parameters without knowing sizeof(UResourceBundle)
 * @param r The resourcebundle to open
 * @param path String containing the full path pointing to the directory
 *             where the resources reside followed by the package name
 * @param localeID specifies the locale for which we want to open the resource
 * @param status The error code
 * @return a newly allocated resource bundle or NULL if it doesn't exist.
 * @internal
 */
U_INTERNAL void U_EXPORT2 
ures_openFillIn(UResourceBundle *r, 
                const char* path,
                const char* localeID, 
                UErrorCode* status);

/**
 * Returns a string from a string resource type
 *
 * @param resourceBundle a string resource
 * @param len    fills in the length of resulting string
 * @param status fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @see ures_getBinary
 * @see ures_getIntVector
 * @see ures_getInt
 * @see ures_getUInt
 * @stable ICU 2.0
 */
U_STABLE const UChar* U_EXPORT2 
ures_getString(const UResourceBundle* resourceBundle, 
               int32_t* len, 
               UErrorCode* status);

/**
 * Returns a binary data from a binary resource. 
 *
 * @param resourceBundle a string resource
 * @param len    fills in the length of resulting byte chunk
 * @param status fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return a pointer to a chuck of unsigned bytes which live in a memory mapped/DLL file.
 * @see ures_getString
 * @see ures_getIntVector
 * @see ures_getInt
 * @see ures_getUInt
 * @stable ICU 2.0
 */
U_STABLE const uint8_t* U_EXPORT2 
ures_getBinary(const UResourceBundle* resourceBundle, 
               int32_t* len, 
               UErrorCode* status);

/**
 * Returns a 32 bit integer array from a resource. 
 *
 * @param resourceBundle an int vector resource
 * @param len    fills in the length of resulting byte chunk
 * @param status fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return a pointer to a chunk of unsigned bytes which live in a memory mapped/DLL file.
 * @see ures_getBinary
 * @see ures_getString
 * @see ures_getInt
 * @see ures_getUInt
 * @stable ICU 2.0
 */
U_STABLE const int32_t* U_EXPORT2 
ures_getIntVector(const UResourceBundle* resourceBundle, 
                  int32_t* len, 
                  UErrorCode* status);

/**
 * Returns an unsigned integer from a resource. 
 * This integer is originally 28 bits.
 *
 * @param resourceBundle a string resource
 * @param status fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return an integer value
 * @see ures_getInt
 * @see ures_getIntVector
 * @see ures_getBinary
 * @see ures_getString
 * @stable ICU 2.0
 */
U_STABLE uint32_t U_EXPORT2 
ures_getUInt(const UResourceBundle* resourceBundle, 
             UErrorCode *status);

/**
 * Returns a signed integer from a resource. 
 * This integer is originally 28 bit and the sign gets propagated.
 *
 * @param resourceBundle a string resource
 * @param status  fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return an integer value
 * @see ures_getUInt
 * @see ures_getIntVector
 * @see ures_getBinary
 * @see ures_getString
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ures_getInt(const UResourceBundle* resourceBundle, 
            UErrorCode *status);

/**
 * Returns the size of a resource. Size for scalar types is always 1, 
 * and for vector/table types is the number of child resources.
 * @warning Integer array is treated as a scalar type. There are no 
 *          APIs to access individual members of an integer array. It
 *          is always returned as a whole.
 * @param resourceBundle a resource
 * @return number of resources in a given resource.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ures_getSize(const UResourceBundle *resourceBundle);

/**
 * Returns the type of a resource. Available types are defined in enum UResType
 *
 * @param resourceBundle a resource
 * @return type of the given resource.
 * @see UResType
 * @stable ICU 2.0
 */
U_STABLE UResType U_EXPORT2 
ures_getType(const UResourceBundle *resourceBundle);

/**
 * Returns the key associated with a given resource. Not all the resources have a key - only 
 * those that are members of a table.
 *
 * @param resourceBundle a resource
 * @return a key associated to this resource, or NULL if it doesn't have a key
 * @stable ICU 2.0
 */
U_STABLE const char * U_EXPORT2 
ures_getKey(const UResourceBundle *resourceBundle);

/* ITERATION API 
    This API provides means for iterating through a resource
*/

/**
 * Resets the internal context of a resource so that iteration starts from the first element.
 *
 * @param resourceBundle a resource
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ures_resetIterator(UResourceBundle *resourceBundle);

/**
 * Checks whether the given resource has another element to iterate over.
 *
 * @param resourceBundle a resource
 * @return TRUE if there are more elements, FALSE if there is no more elements
 * @stable ICU 2.0
 */
U_STABLE UBool U_EXPORT2 
ures_hasNext(const UResourceBundle *resourceBundle);

/**
 * Returns the next resource in a given resource or NULL if there are no more resources 
 * to iterate over. Features a fill-in parameter. 
 *
 * @param resourceBundle    a resource
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code. You may still get a non NULL result even if an
 *                          error occured. Check status instead.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle* U_EXPORT2 
ures_getNextResource(UResourceBundle *resourceBundle, 
                     UResourceBundle *fillIn, 
                     UErrorCode *status);

/**
 * Returns the next string in a given resource or NULL if there are no more resources 
 * to iterate over. 
 *
 * @param resourceBundle    a resource
 * @param len               fill in length of the string
 * @param key               fill in for key associated with this string. NULL if no key
 * @param status            fills in the outgoing error code. If an error occured, we may return NULL, but don't
 *                          count on it. Check status instead!
 * @return a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @stable ICU 2.0
 */
U_STABLE const UChar* U_EXPORT2 
ures_getNextString(UResourceBundle *resourceBundle, 
                   int32_t* len, 
                   const char ** key, 
                   UErrorCode *status);

/**
 * Returns the resource in a given resource at the specified index. Features a fill-in parameter. 
 *
 * @param resourceBundle    the resource bundle from which to get a sub-resource
 * @param indexR            an index to the wanted resource.
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code. Don't count on NULL being returned if an error has
 *                          occured. Check status instead.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle* U_EXPORT2 
ures_getByIndex(const UResourceBundle *resourceBundle, 
                int32_t indexR, 
                UResourceBundle *fillIn, 
                UErrorCode *status);

/**
 * Returns the string in a given resource at the specified index.
 *
 * @param resourceBundle    a resource
 * @param indexS            an index to the wanted string.
 * @param len               fill in length of the string
 * @param status            fills in the outgoing error code. If an error occured, we may return NULL, but don't
 *                          count on it. Check status instead!
 * @return                  a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @stable ICU 2.0
 */
U_STABLE const UChar* U_EXPORT2 
ures_getStringByIndex(const UResourceBundle *resourceBundle, 
                      int32_t indexS, 
                      int32_t* len, 
                      UErrorCode *status);

/**
 * Returns a resource in a given resource that has a given key. This procedure works only with table
 * resources. Features a fill-in parameter. 
 *
 * @param resourceBundle    a resource
 * @param key               a key associated with the wanted resource
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @stable ICU 2.0
 */
U_STABLE UResourceBundle* U_EXPORT2 
ures_getByKey(const UResourceBundle *resourceBundle, 
              const char* key, 
              UResourceBundle *fillIn, 
              UErrorCode *status);

/**
 * Returns a string in a given resource that has a given key. This procedure works only with table
 * resources. 
 *
 * @param resB              a resource
 * @param key               a key associated with the wanted string
 * @param len               fill in length of the string
 * @param status            fills in the outgoing error code. If an error occured, we may return NULL, but don't
 *                          count on it. Check status instead!
 * @return                  a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @stable ICU 2.0
 */
U_STABLE const UChar* U_EXPORT2 
ures_getStringByKey(const UResourceBundle *resB, 
                    const char* key, 
                    int32_t* len, 
                    UErrorCode *status);

#ifdef XP_CPLUSPLUS
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN
/**
 * returns a string from a string resource type
 *
 * @param resB              a resource
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</TT> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_WARNING</TT>,<TT>U_USING_DEFAULT_WARNING </TT>
 * @return        an UnicodeString object. If there is an error, string is bogus
 * @stable ICU 2.0
 */
inline UnicodeString 
ures_getUnicodeString(const UResourceBundle *resB, 
                      UErrorCode* status) 
{
    int32_t len = 0;
    const UChar *r = ures_getString(resB, &len, status);
    return UnicodeString(TRUE, r, len);
}

/**
 * Returns the next string in a resource or NULL if there are no more resources 
 * to iterate over. 
 *
 * @param resB              a resource
 * @param key               fill in for key associated with this string
 * @param status            fills in the outgoing error code
 * @return an UnicodeString object.
 * @stable ICU 2.0
 */
inline UnicodeString 
ures_getNextUnicodeString(UResourceBundle *resB, 
                          const char ** key, 
                          UErrorCode* status) 
{
    int32_t len = 0;
    const UChar* r = ures_getNextString(resB, &len, key, status);
    return UnicodeString(TRUE, r, len);
}

/**
 * Returns the string in a given resource at the specified index.
 *
 * @param resB              a resource
 * @param index             an index to the wanted string.
 * @param status            fills in the outgoing error code
 * @return                  an UnicodeString object. If there is an error, string is bogus
 * @stable ICU 2.0
 */
inline UnicodeString 
ures_getUnicodeStringByIndex(const UResourceBundle *resB, 
                             int32_t indexS, 
                             UErrorCode* status) 
{
    int32_t len = 0;
    const UChar* r = ures_getStringByIndex(resB, indexS, &len, status);
    return UnicodeString(TRUE, r, len);
}

/**
 * Returns a string in a resource that has a given key. This procedure works only with table
 * resources. 
 *
 * @param resB              a resource
 * @param key               a key associated with the wanted string
 * @param status            fills in the outgoing error code
 * @return                  an UnicodeString object. If there is an error, string is bogus
 * @stable ICU 2.0
 */
inline UnicodeString 
ures_getUnicodeStringByKey(const UResourceBundle *resB, 
                           const char* key, 
                           UErrorCode* status) 
{
    int32_t len = 0;
    const UChar* r = ures_getStringByKey(resB, key, &len, status);
    return UnicodeString(TRUE, r, len);
}

U_NAMESPACE_END

#endif


/**
 * Create a string enumerator, owned by the caller, of all locales located within 
 * the specified resource tree.
 * @param path path to the tree, such as (NULL) or U_ICUDATA or U_ICUDATA_COLL.  If NULL,
 * this call is similar to uloc_getAvailable().
 * @param status error code
 * @internal ICU 3.0
 */
U_INTERNAL UEnumeration* U_EXPORT2
ures_openAvailableLocales(const char *path, UErrorCode *status);

/**
 * Returns a functionally equivalent locale (considering keywords) for the specified keyword.
 * @param result fillin for the equivalent locale
 * @param resultCapacity capacity of the fillin buffer
 * @param path path to the tree, or NULL for ICU data
 * @param resName top level resource. Example: "collations"
 * @param keyword locale keyword. Example: "collation"
 * @param locid The requested locale
 * @param isAvailable If non-null, pointer to fillin parameter that indicates whether the 
 * requested locale was available. The locale is defined as 'available' if it physically 
 * exists within the specified tree.
 * @param omitDefault if TRUE, omit keyword and value if default. 'de_DE\@collation=standard' -> 'de_DE'
 * @param status error code
 * @return  the actual buffer size needed for the full locale.  If it's greater 
 * than resultCapacity, the returned full name will be truncated and an error code will be returned.
 * @internal ICU 3.0
 */
U_INTERNAL int32_t U_EXPORT2
ures_getFunctionalEquivalent(char *result, int32_t resultCapacity, 
                             const char *path, const char *resName, const char *keyword, const char *locid,
                             UBool *isAvailable, UBool omitDefault, UErrorCode *status);

/**
 * Given a tree path and keyword, return a string enumeration of all possible values for that keyword.
 * @param path path to the tree, or NULL for ICU data
 * @param keyword a particular keyword to consider, must match a top level resource name 
 * within the tree.
 * @param status error code
 * @internal ICU 3.0
 */
U_INTERNAL UEnumeration* U_EXPORT2
ures_getKeywordValues(const char *path, const char *keyword, UErrorCode *status);

#endif /*_URES*/
/*eof*/
