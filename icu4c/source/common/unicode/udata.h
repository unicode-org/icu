/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  udata.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct25
*   created by: Markus W. Scherer
*/

#ifndef __UDATA_H__
#define __UDATA_H__

#include "unicode/utypes.h"

U_CDECL_BEGIN

/**
 * Information about data memory.
 * This structure may grow in the future, indicated by the
 * <code>size</code> field.
 *
 * <p>The platform data property fields help determine if a data
 * file can be efficiently used on a given machine.
 * The particular fields are of importance only if the data
 * is affected by the properties - if there is integer data
 * with word sizes > 1 byte, char* text, or UChar* text.</p>
 *
 * <p>The implementation for the <code>udata_open[Choice]()</code>
 * functions may reject data based on the value in <code>isBigEndian</code>.
 * No other field is used by the <code>udata</code> API implementation.</p>
 *
 * <p>The <code>dataFormat</code> may be used to identify
 * the kind of data, e.g. a converter table.</p>
 *
 * <p>The <code>formatVersion</code> field should be used to
 * make sure that the format can be interpreted.
 * I may be a good idea to check only for the one or two highest
 * of the version elements to allow the data memory to
 * get more or somewhat rearranged contents, for as long
 * as the using code can still interpret the older contents.</p>
 *
 * <p>The <code>dataVersion</code> field is intended to be a
 * common place to store the source version of the data;
 * for data from the Unicode character database, this could
 * reflect the Unicode version.</p>
 */
typedef struct {
    /** @memo sizeof(UDataInfo) */
    uint16_t size;

    /** @memo unused, set to 0 */
    uint16_t reservedWord;

    /* platform data properties */
    /** @memo 0 for little-endian machine, 1 for big-endian */
    uint8_t isBigEndian;

    /** @memo see U_CHARSET_FAMILY values in utypes.h */
    uint8_t charsetFamily;

    /** @memo sizeof(UChar), one of { 1, 2, 4 } */
    uint8_t sizeofUChar;

    /** @memo unused, set to 0 */
    uint8_t reservedByte;

    /** @memo data format identifier */
    uint8_t dataFormat[4];

    /** @memo versions: [0] major [1] minor [2] milli [3] micro */
    uint8_t formatVersion[4];
    uint8_t dataVersion[4];
} UDataInfo;

/* API for reading data -----------------------------------------------------*/

/**
 * Forward declaration of the data memory type.
 */
typedef struct UDataMemory UDataMemory;

/**
 * Callback function for udata_openChoice().
 * @param context parameter passed into <code>udata_openChoice()</code>.
 * @param type The type of the data as passed into <code>udata_openChoice()</code>.
 *             It may be <code>NULL</code>.
 * @param name The name of the data as passed into <code>udata_openChoice()</code>.
 * @param pInfo A pointer to the <code>UDataInfo</code> structure
 *              of data that has been loaded and will be returned
 *              by <code>udata_openChoice()</code> if this function
 *              returns <code>TRUE</code>.
 * @return TRUE if the current data memory is acceptable
 * @stable
 */
typedef UBool
UDataMemoryIsAcceptable(void *context,
                        const char *type, const char *name,
                        const UDataInfo *pInfo);


/**
 * Convenience function.
 * This function works the same as <code>udata_openChoice</code>
 * except that any data that matches the type and name
 * is assumed to be acceptable.
 * @stable
 */
U_CAPI UDataMemory * U_EXPORT2
udata_open(const char *path, const char *type, const char *name,
           UErrorCode *pErrorCode);

/**
 * Data loading function.
 * This function is used to find and load efficiently data for
 * ICU and applications using ICU.
 * It provides an abstract interface that allows to specify a data
 * type and name to find and load the data.
 *
 * <p>The implementation depends on platform properties and user preferences
 * and may involve loading shared libraries (DLLs), mapping
 * files into memory, or fopen()/fread() files.
 * It may also involve using static memory or database queries etc.
 * Several or all data items may be combined into one entity
 * (DLL, memory-mappable file).</p>
 *
 * <p>The data is always preceded by a header that includes
 * a <code>UDataInfo</code> structure.
 * The caller's <code>isAcceptable()</code> function is called to make
 * sure that the data is useful. It may be called several times if it
 * rejects the data and there is more than one location with data
 * matching the type and name.</p>
 *
 * <p>If <code>path==NULL</code>, then ICU data is loaded.
 * Otherwise, it is separated into a basename and a basename-less path string.
 * If the path string is empty, then <code>u_getDataDirectory()</code>
 * is set in its place.
 * When data is loaded from files or DLLs (shared libraries) and
 * may be stored in common files, then the data finding is roughly as follows:
 * <ul>
 *     <li>common file at path/basename has entry name_type?</li>
 *     <li>common file at basename has entry name_type?</li>
 *     <li>separate file at path/basename_name_type?</li>
 *     <li>separate file at basename_name_type?</li>
 *     <li>separate file at path/name_type?</li>
 *     <li>separate file at name_type?</li>
 * </ul>
 * If the basename is empty, then only the last two options are attempted.
 * Otherwise, it serves as a name for a common data file or as a basename
 * (collection name) prefix for individual files.</p>
 *
 * @param path Specifies an absolute path and/or a basename for the
 *             finding of the data in the file system.
 *             <code>NULL</code> for ICU data.
 * @param type A string that specifies the type of data to be loaded.
 *             For example, resource bundles are loaded with type "res",
 *             conversion tables with type "cnv".
 *             This may be <code>NULL</code> or empty.
 * @param name A string that specifies the name of the data.
 * @param isAcceptable This function is called to verify that loaded data
 *                     is useful for the client code. If it returns FALSE
 *                     for all data items, then <code>udata_openChoice()</code>
 *                     will return with an error.
 * @param context Arbitrary parameter to be passed into isAcceptable.
 * @param pErrorCode An ICU UErrorCode parameter. It must not be <code>NULL</code>.
 * @return A pointer (handle) to a data memory object, or <code>NULL</code>
 *         if an error occurs. Call <code>udata_getMemory()</code>
 *         to get a pointer to the actual data.
 * @stable
 */
U_CAPI UDataMemory * U_EXPORT2
udata_openChoice(const char *path, const char *type, const char *name,
                 UDataMemoryIsAcceptable *isAcceptable, void *context,
                 UErrorCode *pErrorCode);

/**
 * Close the data memory.
 * This function must be called to allow the system to
 * release resources associated with this data memory.
 * @stable
 */
U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData);

/**
 * Get the pointer to the actual data inside the data memory.
 * The data is read-only.
 * @stable
 */
U_CAPI const void * U_EXPORT2
udata_getMemory(UDataMemory *pData);

/**
 * Get the information from the data memory header.
 * This allows to get access to the header containing
 * platform data properties etc. which is not part of
 * the data itself and can therefore not be accessed
 * via the pointer that <code>udata_getMemory()</code> returns.
 *
 * @param pData pointer to the data memory object
 * @param pInfo pointer to a UDataInfo object;
 *              its <code>size</code> field must be set correctly,
 *              typically to <code>sizeof(UDataInfo)</code>.
 *
 * <code>*pInfo</code> will be filled with the UDataInfo structure
 * in the data memory object. If this structure is smaller than
 * <code>pInfo->size</code>, then the <code>size</code> will be
 * adjusted and only part of the structure will be filled.
 * @stable
 */
U_CAPI void U_EXPORT2
udata_getInfo(UDataMemory *pData, UDataInfo *pInfo);

/**
 * This function bypasses the normal ICU data loading process
 * and allows you to force the data to come out of a user-specified
 * pointer.
 * 
 * The format of this data is that of the icu common data file, 'icudata.dat'
 * or that of a DLL (shared library) with a table of contents as generated
 * by "gencmn -s".
 * You can read in that whole file and pass the address to the start of the
 * data, or (with the appropriate link options) pass in the pointer to
 * a 'genccode'-generated library, or pass the pointer to the entry point
 * of the table of contents file that gencmn -s generated, as follows:
 *
 *       extern const uint8_t U_IMPORT icudata_dat[];
 * 
 *       UErrorCode  status = U_ZERO_ERROR;
 *       udata_setCommonData((const void*)icudata_dat, &status);
 * 
 *   ( link with icudata.dll / libicudata.so/a/sl .. see the release notes
 *     for how to build the 'common mapped dll'. )
 *
 * This function will automatically skip over the 'double' padding
 * present in genccode symbols, if present.
 *
 * Warning: ICU must NOT have even attempted to load its data yet
 * when this call is made, or U_USING_DEFAULT_ERROR [non-failing] will
 * be returned. Be careful of UnicodeStrings in static initialization which 
 * may attempt to load a converter (use the UNICODE_STRING(x) macro instead).
 *
 * This function has no effect on application (non ICU) data, and will
 * return U_UNSUPPORTED_ERROR if mapped data support is not available on
 * the currently compiled ICU.
 *
 * @draft
 * @param data pointer to ICU common data
 * @param err outgoing error status <code>U_USING_DEFAULT_ERROR, U_UNSUPPORTED_ERROR</code>
 *
 */

U_CAPI void U_EXPORT2
udata_setCommonData(const void *data, UErrorCode *err);

U_CDECL_END

#endif
