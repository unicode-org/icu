/*
**********************************************************************
*   Copyright (C) 1997-2002, International Business Machines
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
 * <a href="http://oss.software.ibm.com/icu/userguide/Fallbackmechanism.html">users guide</a>.
 * <P>
 *
 * <H2>Usage model:</H2>
 * Resource bundles contain resources. In code, both types of entities are treated the
 * same and are represented with a same data structure <pre>UResourceBundle</pre>. 
 * Resource bundle has a tree structure, where leaf nodes can be strings, binaries 
 * and integers while non-leaf nodes (including the root node) can be tables and arrays.
 * One or more resource bundles are used to represent data needed by the application
 * for running in the particular locale. Complete set of resource bundles for an application
 * would contain all the data needed to run in intended locales. <P>
 * If the data for the requested locale is missing, an effort will be made to obtain most
 * usable data. This process is called fallback. Also, fallback happens when a resource 
 * is not present in the given bundle. Then, the other bundles in the fallback chain are
 * also searched for the requested resource.<P>
 * Retrieving data from resources is possible in several ways, depending on the type of
 * the resources:<P>
 * 1) Access by a key: this approach works only for table resources<P>
 * 2) Access by an index: tables and arrays can be addressed by an index<P>
 * 3) Iteration: works for tables and arrays<P>
 * To use data in resource bundles, following steps are needed:<P>
 * 1) opening a bundle for a particular locale:
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      UResourceBundle* resB = ures_open("myPackage", "de_AT_EURO", &status);
 * \endcode
 * </pre>
 * Status allows, besides testing for plain error, to see whether fallback occured. There
 * are two extra non error values for status after this operation: U_USING_FALLBACK_ERROR,
 * which implies that the bundle for the requested locale was not found, but that one of
 * the bundles in the fallback chain was used (de_AT and de in this case) and
 * U_USING_DEFAULT_ERROR which implies that not one bundle in the fallback chain was found
 * and that default locale was used. In any case, 'root' locale is always at the end of the
 * chain.
 *
 * This is an example for using a possible custom resource:
 * <pre>
 * \code
 *     const char *currentLocale;
 *     UErrorCode status = U_ZERO_ERROR;
 *     UResourceBundle* myResources=ures_open("MyResources", currentLocale, &status);
 * 
 *     const UChar *button1Title = 0, *button2Title = 0;
 *     int32_t button1TitleLen = 0, button2TitleLen = 0;
 *     button1Title= ures_getStringByKey(myResources, "OkKey", &button1TitleLen, &status);
 *     button2Title= ures_getStringByKey(myResources, "CancelKey", &button2TitleLen, &status);
 * \endcode
 * </pre>
 * <h3>Fill-in parameter</h3>
 * A lot of resource bundle APIs allow usage of a fill-in parameter. This 
 * construct helps in reducing allocation of new structures if once can reuse
 * the current resource bundle. Here is an example:
 * <pre>
 * \code
 * UErrorCode status = U_ZERO_ERROR;
 * UResourceBundle *root = ures_open(NULL, "root", &status);
 * if(U_SUCCESS(status)) {
 *   UResourceBundle *zones = ures_getByKey(root, "zoneStrings", NULL, &status);
 *   if(U_SUCCESS(status)) {
 *       UResourceBundle *currentZone = NULL;
 *       while(ures_hasNext(zones)) {
 *         currentZone = ures_getNextResource(zones, currentZone, &status);
 *         ... do interesting stuff  here ...
 *       }
 *       ures_close(currentZone);
 *   }
 *   ures_close(zones);
 * }
 * ures_close(root);
 * \endcode
 * </pre>
 * In the above example, resource bundle zones is reused. Just one allocation is done.
 * If a NULL pointer is passed as a fill-in parameter, a new resource bundle will be
 * allocated. If a resource bundle is passed, it is going to be reused.
 *    
 */

/**
 * UResourceBundle is an opaque type for handles for resource bundles in C APIs.
 * @stable
 */
struct UResourceBundle;

/**
 * @stable
 */
typedef struct UResourceBundle UResourceBundle;

/**
 * Numeric constants for types of resource items.
 * @stable
 */
typedef enum {
    RES_NONE=-1,
    RES_STRING=0,
    RES_BINARY=1,
    RES_TABLE=2,
  /* this resource is an alias - contains a string 
   * that is the name of resource containing data 
   */
    RES_ALIAS=3, 

    RES_INT=7,
    RES_ARRAY=8,

    RES_INT_VECTOR=14,
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
 * @param path  : string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 *                e.g. "/usr/resource/my_app/resources/guimessages" on a Unix system.
 *                if NULL, ICU default data files will be used.
 * @param locale: specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 *                
 * @param status : fills in the outgoing error code.
 * The UErrorCode err parameter is used to return status information to the user. To
 * check whether the construction succeeded or not, you should check the value of
 * U_SUCCESS(err). If you wish more detailed information, you can check for
 * informational error results which still indicate success. U_USING_FALLBACK_ERROR
 * indicates that a fall back locale was used. For example, 'de_CH' was requested,
 * but nothing was found there, so 'de' was used. U_USING_DEFAULT_ERROR indicates that
 * the default locale data or root locale data was used; neither the requested locale 
 * nor any of its fall back locales could be found.
 * @return      a newly allocated resource bundle.
 * @see ures_close
 * @stable
 */
U_CAPI UResourceBundle*  U_EXPORT2 
ures_open(const char*    path,
          const char*  locale, 
          UErrorCode*     status);


/** This function does not care what kind of localeID is passed in. It simply opens a bundle with 
 *  that name. Fallback mechanism is disabled for the new bundle. If the requested bundle contains
 *  an %%ALIAS directive, the results are undefined.
 * @param path  : string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 *                e.g. "/usr/resource/my_app/resources/guimessages" on a Unix system.
 *                if NULL, ICU default data files will be used.
 * @param locale: specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 *                
 * @param status fills in the outgoing error code. Either U_ZERO_ERROR or U_MISSING_RESOURCE_ERROR
 * @return      a newly allocated resource bundle or NULL if it doesn't exist.
 * @see ures_close
 * @draft ICU 2.0
 */
U_CAPI UResourceBundle* U_EXPORT2 
ures_openDirect(const char* path, 
                const char* locale, 
                UErrorCode* status);

/**
*Opens a UResourceBundle, from which users can extract strings by using
*their corresponding keys. This version of open requires the path 
*string to be of type <TT>const wchar_t*</TT>.
*Note that the caller is responsible of calling <TT>ures_close</TT> on each succesfully
*opened resource bundle.
*@param path: string containing the full path pointing to the directory
*             where the resources reside (should end with a directory
*             separator.
*                e.g. "/usr/resource/my_app/resources/" on a Unix system
*             if NULL will use the system's current data directory
*@param locale: specifies the locale for which we want to open the resource
*                if NULL will use the default locale
*                
*@param status: fills in the outgoing error code.
*@see ures_close
*@return : a newly allocated resource bundle.
*@deprecated remove after Aug 2002
*/
U_CAPI UResourceBundle* U_EXPORT2 
ures_openW(const wchar_t* path, 
           const char* locale, 
           UErrorCode* status);

/**
 * Same as ures_open() but takes a const UChar *path.
 * This path will be converted to char * using the default converter,
 * then ures_open() is called.
 *
 * @param path  :  string containing the full path pointing to the directory
 *                where the resources reside followed by the package name
 * @param locale: specifies the locale for which we want to open the resource
 *                if NULL, the default locale will be used. If strlen(locale) == 0
 *                root locale will be used.
 * @param status : fills in the outgoing error code.
 * @return      a newly allocated resource bundle.
 * @stable
 */
U_CAPI UResourceBundle* U_EXPORT2 
ures_openU(const UChar* path, 
           const char* locale, 
           UErrorCode* status);

/**
 * Returns the number of strings/arrays in resource bundles.
 * Better to use user_getSize, as this function will be deprecated. 
 *
 *@param resourceBundle: resource bundle containing the desired strings
 *@param resourceKey: key tagging the resource
 *@param err: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 *@return: for    <STRONG>Arrays</STRONG>: returns the number of resources in the array
 *                <STRONG>Tables</STRONG>: returns the number of resources in the table
 *                <STRONG>single string</STRONG>: returns 1
 *@see ures_getSize
 * @stable
 */
U_CAPI int32_t U_EXPORT2 
ures_countArrayItems(const UResourceBundle* resourceBundle,
                     const char* resourceKey,
                     UErrorCode* err);
/**
 * Close a resource bundle, all pointers returned from the various ures_getXXX calls
 * on this particular bundle should be considered invalid henceforth.
 *
 * @param resourceBundle: a pointer to a resourceBundle struct. Can be NULL.
 * @see ures_open
 * @stable
 */
U_CAPI void U_EXPORT2 
ures_close(UResourceBundle* resourceBundle);

/**
 * Return the version number associated with this ResourceBundle as a string. Please
 * use ures_getVersion as this function is going to be deprecated.
 *
 * @param resourceBundle The resource bundle for which the version is checked.
 * @return  A version number string as specified in the resource bundle or its parent.
 *          The caller does not own this string.
 * @see ures_getVersion
 * @stable
 */
U_CAPI const char* U_EXPORT2 
ures_getVersionNumber(const UResourceBundle*   resourceBundle);

/**
 * Return the version number associated with this ResourceBundle as an 
 * UVersionInfo array.
 *
 * @param resB The resource bundle for which the version is checked.
 * @param versionInfo A UVersionInfo array that is filled with the version number
 *                    as specified in the resource bundle or its parent.
 * @stable
 */
U_CAPI void U_EXPORT2 
ures_getVersion(const UResourceBundle* resB, 
                UVersionInfo versionInfo);

/**
 * Return the name of the Locale associated with this ResourceBundle. This API allows
 * you to query for the real locale of the resource. For example, if you requested 
 * "en_US_CALIFORNIA" and only "en_US" bundle exists, "en_US" will be returned. 
 * For subresources, the locale where this resource comes from will be returned.
 * If fallback has occured, getLocale will reflect this.
 * @param resourceBundle: resource bundle in question
 * @param status: just for catching illegal arguments
 * @return  A Locale name
 * @stable
 */
U_CAPI const char* U_EXPORT2 
ures_getLocale(const UResourceBundle* resourceBundle, 
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
U_CAPI void U_EXPORT2 
ures_openFillIn(UResourceBundle *r, 
                const char* path,
                const char* localeID, 
                UErrorCode* status);

/**
 * Returns a string from a string resource type
 *
 * @param resourceBundle: a string resource
 * @param len:    fills in the length of resulting string
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @see ures_getBinary
 * @see ures_getIntVector
 * @see ures_getInt
 * @see ures_getUInt
 * @stable
 */
U_CAPI const UChar* U_EXPORT2 
ures_getString(const UResourceBundle* resourceBundle, 
               int32_t* len, 
               UErrorCode* status);

/**
 * Returns a binary data from a binary resource. 
 *
 * @param resourceBundle: a string resource
 * @param len:    fills in the length of resulting byte chunk
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return a pointer to a chuck of unsigned bytes which live in a memory mapped/DLL file.
 * @see ures_getString
 * @see ures_getIntVector
 * @see ures_getInt
 * @see ures_getUInt
 * @stable
 */
U_CAPI const uint8_t* U_EXPORT2 
ures_getBinary(const UResourceBundle* resourceBundle, 
               int32_t* len, 
               UErrorCode* status);

/**
 * Returns a 32 bit integer array from a resource. 
 *
 * @param resourceBundle: an int vector resource
 * @param len:    fills in the length of resulting byte chunk
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                Always check the value of status. Don't count on returning NULL.
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return a pointer to a chunk of unsigned bytes which live in a memory mapped/DLL file.
 * @see ures_getBinary
 * @see ures_getString
 * @see ures_getInt
 * @see ures_getUInt
 * @draft ICU 2.0
 */
U_CAPI const int32_t* U_EXPORT2 
ures_getIntVector(const UResourceBundle* resourceBundle, 
                  int32_t* len, 
                  UErrorCode* status);

/**
 * Returns an unsigned integer from a resource. 
 * This integer is originally 28 bits.
 *
 * @param resourceBundle: a string resource
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return an integer value
 * @see ures_getInt
 * @see ures_getIntVector
 * @see ures_getBinary
 * @see ures_getString
 * @draft ICU 2.0
 */
U_CAPI uint32_t U_EXPORT2 
ures_getUInt(const UResourceBundle* resourceBundle, 
             UErrorCode *status);

/**
 * Returns a signed integer from a resource. 
 * This integer is originally 28 bit and the sign gets propagated.
 *
 * @param resourceBundle: a string resource
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return an integer value
 * @see ures_getUInt
 * @see ures_getIntVector
 * @see ures_getBinary
 * @see ures_getString
 * @draft ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
ures_getInt(const UResourceBundle* resourceBundle, 
            UErrorCode *status);

/**
 * Returns the size of a resource. Size for scalar types is always 1, 
 * and for vector/table types is the number of child resources.
 * @warning Currently, this function works correctly for string, table and 
 *          array resources. For other types of resources, the result is
 *          undefined. This is a bug and will be fixed.
 *
 * @param resourceBundle: a resource
 * @return number of resources in a given resource.
 * @stable
 */
U_CAPI int32_t U_EXPORT2 
ures_getSize(UResourceBundle *resourceBundle);

/**
 * Returns the type of a resource. Available types are defined in enum UResType
 *
 * @param resourceBundle: a resource
 * @return type of the given resource.
 * @see UResType
 * @stable
 */
U_CAPI UResType U_EXPORT2 
ures_getType(UResourceBundle *resourceBundle);

/**
 * Returns the key associated with a given resource. Not all the resources have a key - only 
 * those that are members of a table.
 *
 * @param resourceBundle: a resource
 * @return a key associated to this resource, or NULL if it doesn't have a key
 * @stable
 */
U_CAPI const char * U_EXPORT2 
ures_getKey(UResourceBundle *resB);

/* ITERATION API 
    This API provides means for iterating through a resource
*/

/**
 * Resets the internal context of a resource so that iteration starts from the first element.
 *
 * @param resourceBundle: a resource
 * @stable
 */
U_CAPI void U_EXPORT2 
ures_resetIterator(UResourceBundle *resourceBundle);

/**
 * Checks whether the given resource has another element to iterate over.
 *
 * @param resourceBundle a resource
 * @return TRUE if there are more elements, FALSE if there is no more elements
 * @stable
 */
U_CAPI UBool U_EXPORT2 
ures_hasNext(UResourceBundle *resourceBundle);

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
 * @stable
 */
U_CAPI UResourceBundle* U_EXPORT2 
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
 * @stable
 */
U_CAPI const UChar* U_EXPORT2 
ures_getNextString(UResourceBundle *resourceBundle, 
                   int32_t* len, 
                   const char ** key, 
                   UErrorCode *status);

/**
 * Returns the resource in a given resource at the specified index. Features a fill-in parameter. 
 *
 * @param resB              a resource
 * @param indexR            an index to the wanted resource.
 * @param fillIn            if NULL a new UResourceBundle struct is allocated and must be deleted by the caller.
 *                          Alternatively, you can supply a struct to be filled by this function.
 * @param status            fills in the outgoing error code. Don't count on NULL being returned if an error has
 *                          occured. Check status instead.
 * @return                  a pointer to a UResourceBundle struct. If fill in param was NULL, caller must delete it
 * @stable
 */
U_CAPI UResourceBundle* U_EXPORT2 
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
 * @stable
 */
U_CAPI const UChar* U_EXPORT2 
ures_getStringByIndex(const UResourceBundle *resB, 
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
 * @stable
 */
U_CAPI UResourceBundle* U_EXPORT2 
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
 * @stable
 */
U_CAPI const UChar* U_EXPORT2 
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
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return        an UnicodeString object. If there is an error, string is bogus
 * @draft ICU 2.0
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
 * @draft ICU 2.0
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
 * @draft ICU 2.0
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
 * @draft ICU 2.0
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

#endif /*_URES*/
/*eof*/
