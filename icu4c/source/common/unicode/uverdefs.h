/*
*******************************************************************************
*   Copyright (C) 2000-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  uverdefs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: Vladimir Weinstein
*   Updated by: Steven R. Loomis
*
*  Gets included by utypes.h and Windows .rc files
*/

#ifndef UVERDEFS_H
#define UVERDEFS_H

/** The standard copyright notice that gets compiled into each library. 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.4
 */
#define U_COPYRIGHT_STRING \
  " Copyright (C) 2009, International Business Machines Corporation and others. All Rights Reserved. "

/** The current ICU major version as an integer. 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.4
 */
#define U_ICU_VERSION_MAJOR_NUM 4

/** The current ICU minor version as an integer. 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.6
 */
#define U_ICU_VERSION_MINOR_NUM 3

/** The current ICU patchlevel version as an integer.  
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.4
 */
#define U_ICU_VERSION_PATCHLEVEL_NUM 2

/** The current ICU build level version as an integer.  
 *  This value is for use by ICU clients. It defaults to 0.
 *  @draft ICU 4.0
 */
#ifndef U_ICU_VERSION_BUILDLEVEL_NUM
#define U_ICU_VERSION_BUILDLEVEL_NUM 0
#endif

/** Glued version suffix for renamers 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.6
 */
#define U_ICU_VERSION_SUFFIX _4_3

/** Glued version suffix function for renamers 
 *  This value will change in the subsequent releases of ICU.
 *  If a custom suffix (such as matching library suffixes) is desired, this can be modified.
 *  Note that if present, platform.h may contain an earlier definition of this macro.
 *  @draft ICU 4.2
 */
#ifndef U_ICU_FUNCTION_RENAME
#define U_ICU_FUNCTION_RENAME(x)    x ## _4_3
#endif

/** The current ICU library version as a dotted-decimal string. The patchlevel
 *  only appears in this string if it non-zero. 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.4
 */
#define U_ICU_VERSION "4.3.2"

/** The current ICU library major/minor version as a string without dots, for library name suffixes. 
 *  This value will change in the subsequent releases of ICU
 *  @stable ICU 2.6
 */
#define U_ICU_VERSION_SHORT "43"

/** Data version in ICU4C.
 *  * @draft ICU 4.4
 *   */
#define U_ICU_DATA_VERSION "4.3.2"

/*===========================================================================*/
/* C++ namespace if supported. Versioned unless versioning is disabled.      */
/*===========================================================================*/

/**
 * \def U_NAMESPACE_BEGIN
 * This is used to begin a declaration of a public ICU C++ API.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_END
 * This is used to end a declaration of a public ICU C++ API 
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_USE
 * This is used to specify that the rest of the code uses the
 * public ICU C++ API namespace.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_QUALIFIER
 * This is used to qualify that a function or class is part of
 * the public ICU C++ API namespace.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/* Define namespace symbols if the compiler supports it. */
#if U_HAVE_NAMESPACE && defined(XP_CPLUSPLUS)
#   if U_DISABLE_RENAMING
#       define U_ICU_NAMESPACE icu
        namespace U_ICU_NAMESPACE { }
#   else
#       define U_ICU_NAMESPACE icu_4_1
        namespace U_ICU_NAMESPACE { }
        namespace icu = U_ICU_NAMESPACE;
#   endif

#   define U_NAMESPACE_BEGIN namespace U_ICU_NAMESPACE {
#   define U_NAMESPACE_END  }
#   define U_NAMESPACE_USE using namespace U_ICU_NAMESPACE;
#   define U_NAMESPACE_QUALIFIER U_ICU_NAMESPACE::

#   ifndef U_USING_ICU_NAMESPACE
#       define U_USING_ICU_NAMESPACE 1
#   endif
#   if U_USING_ICU_NAMESPACE
        U_NAMESPACE_USE
#   endif
#else
#   define U_NAMESPACE_BEGIN
#   define U_NAMESPACE_END
#   define U_NAMESPACE_USE
#   define U_NAMESPACE_QUALIFIER
#endif


/*===========================================================================
 * ICU collation framework version information                               
 * Version info that can be obtained from a collator is affected by these    
 * numbers in a secret and magic way. Please use collator version as whole
 *===========================================================================
 */

/** Collation runtime version (sort key generator, strcoll). 
 * If the version is different, sortkeys for the same string could be different 
 * version 2 was in ICU 1.8.1. changed is: compression intervals, French secondary 
 * compression, generating quad level always when strength is quad or more 
 * version 4 - ICU 2.2 - tracking UCA changes, ignore completely ignorables 
 * in contractions, ignore primary ignorables after shifted 
 * version 5 - ICU 2.8 - changed implicit generation code
 * version 6 - ICU 3.4 - with the UCA 4.1, Thai tag is no longer generated or used
 * This value may change in the subsequent releases of ICU
 * @stable ICU 2.4
 */
#define UCOL_RUNTIME_VERSION 6

/** Builder code version. When this is different, same tailoring might result
 * in assigning different collation elements to code points                  
 * version 2 was in ICU 1.8.1. added support for prefixes, tweaked canonical 
 * closure. However, the tailorings should probably get same CEs assigned    
 * version 5 - ICU 2.2 - fixed some bugs, renamed some indirect values.      
 * version 6 - ICU 2.8 - fixed bug in builder that allowed 0xFF in primary values
 * version 7 - ICU 3.4 - with the UCA 4.1 Thai tag is no longer processed, complete ignorables
 *                       now break contractions
 * Backward compatible with the old rules. 
 * This value may change in the subsequent releases of ICU
 * @stable ICU 2.4
 */
#define UCOL_BUILDER_VERSION 7

/** This is the version of the tailorings 
 *  This value may change in the subsequent releases of ICU
 *  @stable ICU 2.4
 */
#define UCOL_TAILORINGS_VERSION 1


#endif
