/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
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
*   11/09/99	weiv		Added ures_getLocale()
*******************************************************************************
*/

#ifndef URES_H
#define URES_H

#include "utypes.h"


#include "uloc.h"

/**
 * @name ResourceBundle C API
 *
 * C API representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specific information in
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
 * .    const char *currentLocale;
 * .    UErrorCode success = U_ZERO_ERROR;
 * .    UResourceBundle* myResources=ures_open("MyResources", currentLocale, &success );
 * .
 * .    UChar *button1Title, *button2Title;
 * .    button1Title= ures_get(myResources, "OkKey", &success );
 * .    button2Title= ures_get(myResources, "CancelKey", &success );
 * </pre>
 */

/** A UResourceBundle.
 *  For usage in C programs.
 */
typedef void* UResourceBundle;

 
 /**
 * Functions to create and destroy resource bundles.
 */

/**
*Opens a UResourceBundle, from which users can extract strings by using
*their corresponding keys.
*Note that the caller is responsible of calling <TT>ures_close</TT> on each succesfully
*opened resource bundle.
*@param path  : string containing the full path pointing to the directory
*                where the resources reside (should end with a directory
*                separator.
*                e.g. "/usr/resource/my_app/resources/" on a Unix system
*                if NULL will use the system's current data directory
*@param locale: specifies the locale for which we want to open the resource
*                if NULL will use the default locale
*                
*@param status : fills in the outgoing error code.
* The UErrorCode err parameter is used to return status information to the user. To
     * check whether the construction succeeded or not, you should check the value of
     * U_SUCCESS(err). If you wish more detailed information, you can check for
     * informational error results which still indicate success. U_USING_FALLBACK_ERROR
     * indicates that a fall back locale was used. For example, 'de_CH' was requested,
     * but nothing was found there, so 'de' was used. U_USING_DEFAULT_ERROR indicates that
     * the default locale data was used; neither the requested locale nor any of its
     * fall back locales could be found.
*@return      : a newly allocated resource bundle.
*@see ures_close
*/
U_CAPI UResourceBundle*  U_EXPORT2 ures_open(const char*    path,   /* NULL if none */
					   const char*  locale, /* NULL if none */
					   UErrorCode*     status);


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
*/
U_CAPI UResourceBundle* U_EXPORT2 ures_openW(const wchar_t* path, 
                  const char* locale, 
                  UErrorCode* status);

/**
 * returns a resource string, given a resource bundle and a key.
 *
 * @param resourceBundle: resourceBundle containing the desired string
 * @param resourceTag: key tagging the desired string
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return: a library-owned zero-terminated unicode string (its lifetime
 * is that of the resource bundle.)
 * @see ures_getArrayItem
 * @see ures_get2dArrayItem
 * @see ures_getTaggedItem
 */
U_CAPI const UChar* U_EXPORT2 ures_get(const UResourceBundle*    resourceBundle,
               const char*              resourceTag,
               UErrorCode*               status);

/**
 * Returns a resource string which is part of an array, given a resource bundle
 * a key to the array and the index of the desired string.
 *
 * @param resourceBundle: resourceBundle containing the desired string
 * @param resourceTag: key tagging the desired array
 * @param resourceIndex: index of the desired string
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return: a library-owned zero-terminated unicode string (its lifetime
 * is that of the resource bundle.)
 * @see ures_get
 * @see ures_get2dArrayItem
 * @see ures_getTaggedItem
 */
U_CAPI const UChar* U_EXPORT2 ures_getArrayItem(const UResourceBundle*     resourceBundle,
                    const char*               resourceTag,
                    int32_t                   resourceIndex,
                    UErrorCode*                status);

/**
 * Returns a resource string which is part of a 2D array, given a resource bundle
 * a key to the array and the index pair of the desired string.
 *
 * @param resourceBundle: resourceBundle containing the desired string
 * @param resourceTag: key tagging the desired array
 * @param resourceIndex: x index of the desired string
 * @param resourceIndex: y index of the desired string
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return: a library-owned zero-terminated unicode string (its lifetime
 * is that of the resource bundle.)
 * @see ures_get
 * @see ures_getArrayItem
 * @see ures_getTaggedItem
 */

U_CAPI const UChar* U_EXPORT2 ures_get2dArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      int32_t                 rowIndex,
                      int32_t                 columnIndex,
                      UErrorCode*              status);

/**
 * Returns a resource string which is part of a tagged array, given a resource bundle
 * a key to the array and the key of the desired string.
 *
 * @param resourceBundle: resource bundle containing the desired string
 * @param resourceTag: key tagging the desired array
 * @param resourceIndex: key tagging the desired string
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @return: a library-owned zero-terminated unicode string (its lifetime
 * is that of the resource bundle.)
 * @see ures_get
 * @see ures_getArrayItem
 * @see ures_get2dItem
 */

U_CAPI const UChar* U_EXPORT2 ures_getTaggedArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      const char*             itemTag,
                      UErrorCode*              status);



/**
 * Returns the number of strings/arrays in resource bundles.
 *
 *@param resourceBundle: resource bundle containing the desired strings
 *@param resourceKey: key tagging the resource
 *@param err: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 *@return: for    <STRONG>Arrays</STRONG>: returns the number of strings in the array
 *                <STRONG>2d Arrays</STRONG>: returns the number of 1d arrays
 *                <STRONG>taggedArrays</STRONG>: returns the number of strings in the array
 *                <STRONG>single string</STRONG>: returns 1
 *@see ures_get
 *@see ures_getArrayItem
 *@see ures_getTaggedArrayItem
 *@see ures_get2dArrayItem
 */

U_CAPI int32_t U_EXPORT2 ures_countArrayItems(const UResourceBundle* resourceBundle,
                  const char* resourceKey,
                  UErrorCode* err);
/**
 * close a resource bundle, all pointers returned from the various ures_getXXX calls
 * on this particular bundle are INVALID henceforth.
 *
 * @param resourceBundle: a succesfully opened resourceBundle.
 * @param status: fills in the outgoing error code
 *                could be <TT>U_MISSING_RESOURCE_ERROR</T> if the key is not found
 *                could be a non-failing error 
 *                e.g.: <TT>U_USING_FALLBACK_ERROR</TT>,<TT>U_USING_DEFAULT_ERROR </TT>
 * @see ures_open
 * @see ures_openW
 */
U_CAPI void U_EXPORT2 ures_close(UResourceBundle*    resourceBundle);
/**
 * Return the version number associated with this ResourceBundle. This version
 * number is a string of the form MAJOR.MINOR, where MAJOR is the version number of
 * the current analytic code package, and MINOR is the version number contained in
 * the resource file as the value of the tag "Version". A change in the MINOR
 * version indicated an updated data file. A change in the MAJOR version indicates a
 * new version of the code which is not binary-compatible with the previous version.
 * If no "Version" tag is present in a resource file, the MINOR version "0" is assigned.
 * For example, if the Collation sort key algorithm changes, the MAJOR version
 * increments. If the collation data in a resource file changes, the MINOR version
 * for that file increments.
 * @param resourceBundle: resource bundle in question
 * @return  A string of the form N.n, where N is the major version number,
 *          representing the code version, and n is the minor version number,
 *          representing the resource data file. The caller does not own this
 *          string.
 */
U_CAPI const char* U_EXPORT2 ures_getVersionNumber(const UResourceBundle*   resourceBundle);

/**
 * Return the name of the Locale associated with this ResourceBundle.
 * @param resourceBundle: resource bundle in question
 * @param status: just for catching illegal arguments
 * @return  A Locale name
 */
U_CAPI const char* ures_getLocale(const UResourceBundle* resourceBundle, UErrorCode* status);
#endif /*_URES*/
/*eof*/
