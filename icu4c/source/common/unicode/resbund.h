/*
*******************************************************************************
*
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File resbund.h
*
*   CREATED BY
*       Richard Gillam
*
* Modification History:
*
*   Date        Name        Description
*   2/5/97      aliu        Added scanForLocaleInFile.  Added
*                           constructor which attempts to read resource bundle
*                           from a specific file, without searching other files.
*   2/11/97     aliu        Added UErrorCode return values to constructors.  Fixed
*                           infinite loops in scanForFile and scanForLocale.
*                           Modified getRawResourceData to not delete storage in
*                           localeData and resourceData which it doesn't own.
*                           Added Mac compatibility #ifdefs for tellp() and
*                           ios::nocreate.
*   2/18/97     helena      Updated with 100% documentation coverage.
*   3/13/97     aliu        Rewrote to load in entire resource bundle and store
*                           it as a Hashtable of ResourceBundleData objects.
*                           Added state table to govern parsing of files.
*                           Modified to load locale index out of new file distinct
*                           from default.txt.
*   3/25/97     aliu        Modified to support 2-d arrays, needed for timezone data.
*                           Added support for custom file suffixes.  Again, needed to
*                           support timezone data.
*   4/7/97      aliu        Cleaned up.
* 03/02/99      stephen     Removed dependency on FILE*.
* 03/29/99      helena      Merged Bertrand and Stephen's changes.
* 06/11/99      stephen     Removed parsing of .txt files.
*                           Reworked to use new binary format.
*                           Cleaned up.
* 06/14/99      stephen     Removed methods taking a filename suffix.
* 11/09/99		weiv		Added getLocale(), fRealLocale, removed fRealLocaleID
*******************************************************************************
*/

#ifndef RESBUND_H
#define RESBUND_H
  
#include "unicode/ures.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/locid.h"

#if U_HAVE_WCHAR_H
# include <wchar.h>
#endif


#ifndef _FILESTRM
typedef struct _FileStream FileStream;
#endif

/* forward declarations */
class Locale;
class RuleBasedCollator;
class ResourceBundle;
struct UHashtable;


/**
 * A class representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specfic information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * The resource bundle file is a text (ASCII or Unicode) file with the format:
 * <pre>
 * .   locale {
 * .      tag1 {...}
 * .      tag2 {...}
 * .   }
 * </pre>
 * The tags are used to retrieve the data later. You may not have multiple instances of
 * the same tag.
 * <P>
 * Four data types are supported. These are solitary strings, comma-delimited lists of
 * strings, 2-dimensional arrays of strings, and tagged lists of strings.
 * <P>
 * Note that all data is textual. Adjacent strings are merged by the low-level
 * tokenizer, so that the following effects occur: foo bar, baz // 2 elements, "foo
 * bar", and "baz" "foo" "bar", baz // 2 elements, "foobar", and "baz" Note that a
 * single intervening space is added between merged strings, unless they are both double
 * quoted. This extends to more than two strings in a row.
 * <P>
 * Whitespace is ignored, as in a C source file.
 * <P>
 * Solitary strings have the format:
 * <pre>
 * .   Tag { Data }
 * </pre>
 * This is indistinguishable from a comma-delimited list with only one element, and in
 * fact may be retrieved as such (as an array, or as element 0 or an array).
 * <P>
 * Comma-delimited lists have the format:
 * <pre>
 * .   Tag { Data, Data, Data }
 * </pre>
 * Parsing is lenient; a final string, after the last element, is allowed.
 * <P>
 * Tagged lists have the format:
 * <pre>
 * .   Tag { Subtag { Data } Subtag {Data} }
 * </pre>
 * Data is retrieved by specifying the subtag.
 * <P>
 * Two-dimensional arrays have the format:
 * <pre>
 * .   TwoD {
 * .       { r1c1, r1c2, ..., r1cm },
 * .       { r2c1, r2c2, ..., r2cm },
 * .       ...
 * .       { rnc1, rnc2, ..., rncm }
 * .   }
 * </pre>
 * where n is the number of rows, and m is the number of columns. Parsing is lenient (as
 * in other data types). A final comma is always allowed after the last element; either
 * the last string in a row, or the last row itself. Furthermore, since there is no
 * ambiguity, the commas between the rows are entirely optional. (However, if a comma is
 * present, there can only be one comma, no more.) It is possible to have zero columns,
 * as follows:
 * <pre>
 * .   Odd { {} {} {} } // 3 x 0 array
 * </pre>
 * But it is impossible to have zero rows. The smallest array is thus a 1 x 0 array,
 * which looks like this:
 * <pre>
 * .   Smallest { {} } // 1 x 0 array
 * </pre>
 * The array must be strictly rectangular; that is, each row must have the same number
 * of elements.
 * <P>
 * This is an example for using a possible custom resource:
 * <pre>
 * .    Locale currentLocale;
 * .    UErrorCode success = U_ZERO_ERROR;
 * .    ResourceBundle myResources("MyResources", currentLocale, success );
 * .
 * .    UnicodeString button1Title, button2Title;
 * .    myResources.getString("OkKey", button1Title, success );
 * .    myResources.getString("CancelKey", button2Title, success );
 * </pre>
 * @draft
 */
class U_COMMON_API ResourceBundle {
public:
    /**
     * Constructor
     *
     * @param path    This is a full pathname in the platform-specific format for the
     *                directory containing the resource data files we want to load
     *                resources from. We use locale IDs to generate filenames, and the
     *                filenames have this string prepended to them before being passed
     *                to the C++ I/O functions. Therefore, this string must always end
     *                with a directory delimiter (whatever that is for the target OS)
     *                for this class to work correctly.
     * @param locale  This is the locale this resource bundle is for. To get resources
     *                for the French locale, for example, you would create a
     *                ResourceBundle passing Locale::FRENCH for the "locale" parameter,
     *                and all subsequent calls to that resource bundle will return
     *                resources that pertain to the French locale. If the caller doesn't
     *                pass a locale parameter, the default locale for the system (as
     *                returned by Locale::getDefault()) will be used.
     * The UErrorCode& err parameter is used to return status information to the user. To
     * check whether the construction succeeded or not, you should check the value of
     * U_SUCCESS(err). If you wish more detailed information, you can check for
     * informational error results which still indicate success. U_USING_FALLBACK_ERROR
     * indicates that a fall back locale was used. For example, 'de_CH' was requested,
     * but nothing was found there, so 'de' was used. U_USING_DEFAULT_ERROR indicates that
     * the default locale data was used; neither the requested locale nor any of its
     * fall back locales could be found.
     * @draft
     */
                        ResourceBundle( const UnicodeString&    path,
                                        const Locale&           locale,
                                        UErrorCode&              err);
                        ResourceBundle( const UnicodeString&    path,
                                        UErrorCode&              err);
			ResourceBundle(UErrorCode &err);
                        ResourceBundle( const wchar_t* path,
                                        const Locale& locale,
                                        UErrorCode& err);
			ResourceBundle( const char* path,
					const Locale& locale,
					UErrorCode& err);
                        ResourceBundle(const ResourceBundle &original);
                        ResourceBundle(UResourceBundle *res, UErrorCode &status);

                        ResourceBundle& operator=(const ResourceBundle& other);
                        ~ResourceBundle();

/**
 * Returns the size of a resource. Size for scalar types is always 1, and for vector/table types is
 * the number of child resources.
 *
 * @return number of resources in a given resource.
 * @draft
 */
    int32_t getSize(void) const;
/**
 * returns a string from a string resource type
 *
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return a pointer to a zero-terminated UChar array which lives in a memory mapped/DLL file.
 * @draft
 */
    UnicodeString getString(UErrorCode& status) const;
    const uint8_t *getBinary(int32_t& len, UErrorCode& status) const;

/**
 * Checks whether the resource has another element to iterate over.
 *
 * @return TRUE if there are more elements, FALSE if there is no more elements
 * @draft
 */
    UBool hasNext(void) const;
/**
 * Resets the internal context of a resource so that iteration starts from the first element.
 *
 * @draft
 */
    void resetIterator(void);

/**
 * Returns the key associated with this resource. Not all the resources have a key - only 
 * those that are members of a table.
 *
 * @return a key associated to this resource, or NULL if it doesn't have a key
 * @draft
 */
    const char *getKey(void);

    const char *getName(void);


/**
 * Returns the type of a resource. Available types are defined in enum UResType
 *
 * @return type of the given resource.
 * @draft
 */
    UResType getType(void);

/**
 * Returns the next resource in a given resource or NULL if there are no more resources 
 *
 * @param status            fills in the outgoing error code
 * @return                  ResourceBundle object.
 * @draft
 */
    ResourceBundle getNext(UErrorCode& status);

/**
 * Returns the next string in a resource or NULL if there are no more resources 
 * to iterate over. 
 *
 * @param status            fills in the outgoing error code
 * @return an UnicodeString object.
 * @draft
 */
    UnicodeString getNextString(UErrorCode& status);
/**
 * Returns the next string in a resource or NULL if there are no more resources 
 * to iterate over. 
 *
 * @param key               fill in for key associated with this string
 * @param status            fills in the outgoing error code
 * @return an UnicodeString object.
 * @draft
 */
    UnicodeString getNextString(const char ** key, UErrorCode& status);

/**
 * Returns the resource in a resource at the specified index. 
 *
 * @param index             an index to the wanted resource.
 * @param status            fills in the outgoing error code
 * @return                  ResourceBundle object. If there is an error, resource is invalid.
 * @draft
 */
    ResourceBundle get(int32_t index, UErrorCode& status) const;

/**
 * Returns the string in a given resource at the specified index.
 *
 * @param index             an index to the wanted string.
 * @param status            fills in the outgoing error code
 * @return                  an UnicodeString object. If there is an error, string is bogus
 * @draft
 */
    UnicodeString getStringEx(int32_t index, UErrorCode& status) const;

/**
 * Returns a resource in a resource that has a given key. This procedure works only with table
 * resources. 
 *
 * @param key               a key associated with the wanted resource
 * @param status            fills in the outgoing error code.
 * @return                  ResourceBundle object. If there is an error, resource is invalid.
 * @draft
 */
    ResourceBundle get(const char* key, UErrorCode& status) const;

/**
 * Returns a string in a resource that has a given key. This procedure works only with table
 * resources. 
 *
 * @param key               a key associated with the wanted string
 * @param status            fills in the outgoing error code
 * @return                  an UnicodeString object. If there is an error, string is bogus
 * @draft
 */
    UnicodeString getStringEx(const char* key, UErrorCode& status) const;

    /**
     * Returns the contents of a string resource. Resource data is undifferentiated
     * Unicode text. The resource file may contain quoted strings or escape sequences;
     * these will be parsed prior to the data's return.
     *
     * @param resourceTag  The resource tag of the string resource the caller wants
     * @param err          Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                     specified tag couldn't be found.
     * @return A pointer to the string from the resource bundle, or NULL if there was
     *           an error.(its lifetime is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString*    getString(  const char                *resourceTag,
                                        UErrorCode&                err) const;

    /**
     * Returns the contents of a string-array resource. This will return the contents of
     * a string-array (comma-delimited-list) resource as a C++ array of UnicodeString
     * objects. The number of elements in the array is returned in numArrayItems.
     * Calling getStringArray on a resource of type string will return an array with one
     * element; calling it on a resource of type tagged-array results in a
     * U_MISSING_RESOURCE_ERROR error.
     *
     * @param resourceTag    The resource tag of the string-array resource the caller
     *                       wants
     * @param numArrayItems  Receives the number of items in the array the function
     *                       returns.
     * @param err            Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                       specified tag couldn't be found.
     * @return               The resource requested, as a pointer to an array of
     *                       UnicodeStrings. The caller does not own the storage and
     *                       must not delete it. (its lifetime is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString*    getStringArray( const char             *resourceTag,
                                            int32_t&                numArrayItems,
                                            UErrorCode&              err) const;


    /**
     * Returns a single item from a string-array resource. This will return the contents
     * of a single item in a resource of string-array (comma-delimited-list) type. If
     * the resource is not an array, a U_MISSING_RESOURCE_ERROR will be returned in err.
     *
     * @param resourceTag   The resource tag of the resource the caller wants to extract
     *                      an item from.
     * @param index         The index (zero-based) of the particular array item the user
     *                      wants to extract from the resource.
     * @param err           Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                      specified tag couldn't be found, or if the index was out of range.
     * @return A pointer to the text of the array item, or NULL is there was an error. 
     *                      (its lifetime is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString*    getArrayItem(   const char                *resourceTag,
                                            int32_t                    index,
                                            UErrorCode&                err) const;

    /**
     * Return the contents of a 2-dimensional array resource. The return value will be a
     * UnicodeString** array. (This is really an array of pointers; each pointer is a
     * ROW of the data.) The number of rows and columns is returned. If the resource is
     * of the wrong type, or not present, U_MISSING_RESOURCE_ERROR is placed in err.
     *
     * @param resourceTag  The resource tag of the string-array resource the caller
     *                     wants
     * @param rowCount     Receives the number of rows in the array the function
     *                     returns.
     * @param columnCount  Receives the number of columns in the array the function
     *                     returns.
     * @param err          Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                     specified tag couldn't be found.
     * @return             The resource requested, as a UnicodeStrings**. The caller
     *                     does not own the storage and must not delete it. (its lifetime 
     *                      is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString**   get2dArray(const char          *resourceTag,
                                       int32_t&             rowCount,
                                       int32_t&             columnCount,
                                       UErrorCode&           err) const;


    /**
     * Return a single string from a 2-dimensional array resource. If the resource does
     * not exists, or if it is not a 2-d array, or if the row or column indices are out
     * of bounds, err is set to U_MISSING_RESOURCE_ERROR.
     *
     * @param resourceTag   The resource tag of the resource the caller wants to extract
     *                      an item from.
     * @param rowIndex      The row index (zero-based) of the array item the user wants
     *                      to extract from the resource.
     * @param columnIndex   The column index (zero-based) of the array item the user
     *                      wants to extract from the resource.
     * @param err           Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                      specified tag couldn't be found, if the resource data was in
     *                      the wrong format, or if either index is out of bounds.
     * @return A pointer to the text of the array item, or NULL is there was an error.
     *                      (its lifetime is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString*    get2dArrayItem( const char                *resourceTag,
                                            int32_t                    rowIndex,
                                            int32_t                    columnIndex,
                                            UErrorCode&                err) const;


    /**
     * Returns a single item from a tagged-array resource This will return the contents
     * of a single item in a resource of type tagged-array. If this function is called
     * for a resource that is not of type tagged-array, it will set err to
     * MISSING_RESOUCE_ERROR.
     *
     * @param resourceTag   The resource tag of the resource the caller wants to extract
     *                      an item from.
     * @param itemTag       The item tag for the item the caller wants to extract.
     * @param err           Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                      specified resource tag couldn't be found, or if an item
     *                      with the specified item tag coldn't be found in the resource.
     * @return A pointer to the text of the array item, or NULL is there was an error.
     *                      (its lifetime is that of the resource bundle.)
     * @deprecated to be removed in first release in 2001
     */
    const UnicodeString*    getTaggedArrayItem( const char             *resourceTag,
                                                const UnicodeString&    itemTag,
                                                UErrorCode&                err) const;

    /**
     * Returns a tagged-array resource.  The contents of the resource is returned as two
     * separate arrays of UnicodeStrings, the addresses of which are placed in "itemTags"
     * and "items".  After calling this function, the items in the resource will be in the
     * list pointed to by "items", and for each items[i], itemTags[i] will be the tag that
     * corresponds to it.  The total number of entries in both arrays is returned in
     * numItems.
     *
     * @param resourceTag   The resource tag of the resource the caller wants to extract
     *                      an item from.
     * @param itemTags      Set to point to an array of UnicodeStrings representing the
     *                      tags in the specified resource.  The caller DOES own this array,
     *                      and must delete it.
     * @param items         Set to point to an array of UnicodeStrings containing the
     *                      individual resource items themselves.  itemTags[i] will
     *                      contain the tag corresponding to items[i].  The caller DOES
     *                      own this array, and must delete it.
     * @param numItems      Receives the number of items in the arrays pointed to by
     *                      items and itemTags.
     * @param err           Set to U_MISSING_RESOURCE_ERROR if a resource with the
     *                      specified tag couldn't be found.
     * @deprecated to be removed in first release in 2001
     */
    void                getTaggedArray( const char             *resourceTag,
                                        UnicodeString*&         itemTags,
                                        UnicodeString*&         items,
                                        int32_t&                numItems,
                                        UErrorCode&              err) const;
    
    /**
     * Return the version number associated with this ResourceBundle. This version
     * number is a string of the form MAJOR.MINOR, where MAJOR is the version number of
     * the current analytic code package, and MINOR is the version number contained in
     * the resource file as the value of the tag "Version". A change in the MINOR
     * version indicated an updated data file. A change in the MAJOR version indicates a
     * new version of the code which is not binary-compatible with the previous version.
     * If no "Version" tag is present in a resource file, the MINOR version "0" is assigned.
     *
     * For example, if the Collation sort key algorithm changes, the MAJOR version
     * increments. If the collation data in a resource file changes, the MINOR version
     * for that file increments.
     *
     * @return  A string of the form N.n, where N is the major version number,
     *          representing the code version, and n is the minor version number,
     *          representing the resource data file. The caller does not own this
     *          string.
     * @draft
     */
    const char*         getVersionNumber(void) const;

    void getVersion(UVersionInfo versionInfo) const;

	/**
	 * Return the Locale associated with this ResourceBundle. 
	 *
	 * @return a Locale object
     * @draft
	 */
	const Locale &getLocale(void) const ;

private:
    UResourceBundle *resource;
    void constructForLocale(const UnicodeString& path, const Locale& locale, UErrorCode& error);
    void constructForLocale(const wchar_t* path, const Locale& locale, UErrorCode& error);
    void initItemCache(UErrorCode& error);

    friend class RuleBasedCollator;

    /**
     * This constructor is used by Collation to load a resource bundle from a specific
     * file, without trying other files. This is used by the Collation caching
     * mechanism.
     */
                            ResourceBundle( const UnicodeString&    path,
                                            const char *localeName,
                                            UErrorCode&              status);

private:
    static void U_CALLCONV deleteValue(void* value);
	Locale					fRealLocale;

    UHashtable*          fItemCache;
    static const char*          kDefaultSuffix;
    static const int32_t        kDefaultSuffixLen;
    static const char*          kDefaultFilename;
    static const char*          kDefaultLocaleName;
};

#endif
