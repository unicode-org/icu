/*
******************************************************************************
*
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
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
*                           Modified getRawResourceData to not delete storage
*                           in localeData and resourceData which it doesn't own.
*                           Added Mac compatibility #ifdefs for tellp() and
*                           ios::nocreate.
*   2/18/97     helena      Updated with 100% documentation coverage.
*   3/13/97     aliu        Rewrote to load in entire resource bundle and store
*                           it as a Hashtable of ResourceBundleData objects.
*                           Added state table to govern parsing of files.
*                           Modified to load locale index out of new file
*                           distinct from default.txt.
*   3/25/97     aliu        Modified to support 2-d arrays, needed for timezone
*                           data. Added support for custom file suffixes.  Again,
*                           needed to support timezone data.
*   4/7/97      aliu        Cleaned up.
* 03/02/99      stephen     Removed dependency on FILE*.
* 03/29/99      helena      Merged Bertrand and Stephen's changes.
* 06/11/99      stephen     Removed parsing of .txt files.
*                           Reworked to use new binary format.
*                           Cleaned up.
* 06/14/99      stephen     Removed methods taking a filename suffix.
* 11/09/99      weiv        Added getLocale(), fRealLocale, removed fRealLocaleID
******************************************************************************
*/

#ifndef RESBUND_H
#define RESBUND_H
  
#include "unicode/ures.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/locid.h"

U_NAMESPACE_BEGIN
/* forward declarations */
class RuleBasedCollator;

/**
 * A class representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specfic information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * The resource bundle file is a text (ASCII or Unicode) file with the format:
 * <pre>
 * \code
 *    locale {
 *       tag1 {...}
 *       tag2 {...}
 *    }
 * \endcode
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
 * \code
 *    Tag { Data }
 * \endcode
 * </pre>
 * This is indistinguishable from a comma-delimited list with only one element, and in
 * fact may be retrieved as such (as an array, or as element 0 or an array).
 * <P>
 * Comma-delimited lists have the format:
 * <pre>
 * \code
 *    Tag { Data, Data, Data }
 * \endcode
 * </pre>
 * Parsing is lenient; a final string, after the last element, is allowed.
 * <P>
 * Tagged lists have the format:
 * <pre>
 * \code
 *    Tag { Subtag { Data } Subtag {Data} }
 * \endcode
 * </pre>
 * Data is retrieved by specifying the subtag.
 * <P>
 * Two-dimensional arrays have the format:
 * <pre>
 * \code
 *    TwoD {
 *        { r1c1, r1c2, ..., r1cm },
 *        { r2c1, r2c2, ..., r2cm },
 *        ...
 *        { rnc1, rnc2, ..., rncm }
 *    }
 * \endcode
 * </pre>
 * where n is the number of rows, and m is the number of columns. Parsing is lenient (as
 * in other data types). A final comma is always allowed after the last element; either
 * the last string in a row, or the last row itself. Furthermore, since there is no
 * ambiguity, the commas between the rows are entirely optional. (However, if a comma is
 * present, there can only be one comma, no more.) It is possible to have zero columns,
 * as follows:
 * <pre>
 * \code
 *    Odd { {} {} {} } // 3 x 0 array
 * \endcode
 * </pre>
 * But it is impossible to have zero rows. The smallest array is thus a 1 x 0 array,
 * which looks like this:
 * <pre>
 *  \code
 *    Smallest { {} } // 1 x 0 array
 * \endcode
 * </pre>
 * The array must be strictly rectangular; that is, each row must have the same number
 * of elements.
 * <P>
 * This is an example for using a possible custom resource:
 * <pre>
 * \code
 *     Locale currentLocale;
 *     UErrorCode success = U_ZERO_ERROR;
 *     ResourceBundle myResources("MyResources", currentLocale, success );
 * 
 *     UnicodeString button1Title, button2Title;
 *     myResources.getString("OkKey", button1Title, success );
 *     myResources.getString("CancelKey", button2Title, success );
 * \endcode
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
    /**
     * Constructs a ResourceBundle
     *
     * @deprecated Remove after Aug  2002
     */
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
     * @stable
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
     * @stable
     */
    UnicodeString getString(UErrorCode& status) const;

    /**
     * returns a binary data from a resource. Can be used at most primitive resource types (binaries,
     * strings, ints)
     *
     * @param resourceBundle: a string resource
     * @param len:    fills in the length of resulting byte chunk
     * @param status: fills in the outgoing error code
     *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
     *                could be a non-failing error 
     *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
     * @return a pointer to a chunk of unsigned bytes which live in a memory mapped/DLL file.
     * @stable
     */
    const uint8_t *getBinary(int32_t& len, UErrorCode& status) const;


    /**
     * returns an integer vector from a resource. 
     *
     * @param resourceBundle: a string resource
     * @param len:    fills in the length of resulting integer vector
     * @param status: fills in the outgoing error code
     *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
     *                could be a non-failing error 
     *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
     * @return a pointer to a vector of integers that lives in a memory mapped/DLL file.
     * @stable
     */
    const int32_t *getIntVector(int32_t& len, UErrorCode& status) const;

    /**
     * returns an unsigned integer from a resource. 
     * This integer is originally 28 bits.
     *
     * @param status: fills in the outgoing error code
     *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
     *                could be a non-failing error 
     *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
     * @return an unsigned integer value
     * @draft
     */
    uint32_t getUInt(UErrorCode& status) const;

    /**
     * returns a signed integer from a resource. 
     * This integer is originally 28 bit and the sign gets propagated.
     *
     * @param status: fills in the outgoing error code
     *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
     *                could be a non-failing error 
     *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
     * @return a signed integer value
     * @draft
     */
    int32_t getInt(UErrorCode& status) const;

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
     * @stable
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
     * @stable
     */
    ResourceBundle get(int32_t index, UErrorCode& status) const;

    /**
     * Returns the string in a given resource at the specified index.
     *
     * @param index             an index to the wanted string.
     * @param status            fills in the outgoing error code
     * @return                  an UnicodeString object. If there is an error, string is bogus
     * @stable
     */
    UnicodeString getStringEx(int32_t index, UErrorCode& status) const;

    /**
     * Returns a resource in a resource that has a given key. This procedure works only with table
     * resources. 
     *
     * @param key               a key associated with the wanted resource
     * @param status            fills in the outgoing error code.
     * @return                  ResourceBundle object. If there is an error, resource is invalid.
     * @stable
     */
    ResourceBundle get(const char* key, UErrorCode& status) const;

    /**
     * Returns a string in a resource that has a given key. This procedure works only with table
     * resources. 
     *
     * @param key               a key associated with the wanted string
     * @param status            fills in the outgoing error code
     * @return                  an UnicodeString object. If there is an error, string is bogus
     * @stable
     */
    UnicodeString getStringEx(const char* key, UErrorCode& status) const;
    
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
    const char*   getVersionNumber(void) const;

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
    /**
     *@deprecated Remove after Aug 2002
     */
    void constructForLocale(const wchar_t* path, const Locale& locale, UErrorCode& error);
    Locale *locName;
};

U_NAMESPACE_END
#endif
