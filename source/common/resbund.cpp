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
* 11/09/99		weiv		Added getLocale(), rewritten constructForLocale()
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/resbund.h"

#include "rbcache.h"
#include "mutex.h"

#include "unistrm.h"
#include "filestrm.h"
#include "cstring.h"
#include "uhash.h"

#include "rbdata.h"
#include "rbread.h"

#include <string.h>

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
const char* ResourceBundle::kDefaultFilename    = "default";
const char* ResourceBundle::kDefaultLocaleName  = "default";
const char* ResourceBundle::kIndexLocaleName    = "index";
const char* ResourceBundle::kIndexFilename      = "index";
const char* ResourceBundle::kIndexTag           = "InstalledLocales";

// The default minor version and the version separator must be exactly one
// character long.
const char*         ResourceBundle::kDefaultMinorVersion    = "0";
const char*         ResourceBundle::kVersionSeparator       = ".";
const char*         ResourceBundle::kVersionTag             = "Version";

ResourceBundleCache*    ResourceBundle::fgUserCache = new ResourceBundleCache();
VisitedFileCache*       ResourceBundle::fgUserVisitedFiles = new VisitedFileCache();
// allocated on the heap so we don't have to expose the definitions of
// these classes to the world

//-----------------------------------------------------------------------------

ResourceBundle::LocaleFallbackIterator::LocaleFallbackIterator(const UnicodeString& startingLocale,
							       const UnicodeString& root,
							       bool_t useDefaultLocale)
:   fLocale(startingLocale),
    fRoot(root),
    fUseDefaultLocale(useDefaultLocale),

    // Init from the default locale, if asked for.
    fDefaultLocale( (useDefaultLocale)? (Locale::getDefault().getName()) : 0, ""),

    fTriedDefaultLocale(FALSE),
    fTriedRoot(FALSE)
{
}

bool_t
ResourceBundle::LocaleFallbackIterator::nextLocale(UErrorCode& status)
{
  if(fUseDefaultLocale)
    fTriedDefaultLocale = fTriedDefaultLocale || (fLocale == fDefaultLocale);
  
  chopLocale();
  if(status != U_USING_DEFAULT_ERROR) 
    status = U_USING_FALLBACK_ERROR;
  
  if(fLocale.length() == 0) {
    if(fUseDefaultLocale && !fTriedDefaultLocale) {
      fLocale = fDefaultLocale;
      fTriedDefaultLocale = TRUE;
      status = U_USING_DEFAULT_ERROR;
    }
    else if( ! fTriedRoot) {
      fLocale = fRoot;
      fTriedRoot = TRUE;
      status = U_USING_DEFAULT_ERROR;
    }
    else {
      status = U_MISSING_RESOURCE_ERROR;
      return FALSE;
    }
  }
  
  //  cerr << "* " << fLocale << " " << u_errorName(status) << endl;
  return TRUE;
}

void
ResourceBundle::LocaleFallbackIterator::chopLocale()
{
  int32_t size = fLocale.length();
  int32_t i;
  
  for(i = size - 1; i > 0; i--) 
    if(fLocale[i] == 0x005F/*'_'*/) 
      break;
  
  if(i < 0) 
    i = 0;
  
  fLocale.remove(i, size - i);
}

//-----------------------------------------------------------------------------

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                const Locale&           locale,
                                UErrorCode&              error)
  : fgCache(fgUserCache),
    fgVisitedFiles(fgUserVisitedFiles)
{
  constructForLocale(PathInfo(path, kDefaultSuffix), locale, error);
}

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                UErrorCode&              error)
  : fgCache(fgUserCache),
    fgVisitedFiles(fgUserVisitedFiles)
{
  constructForLocale(PathInfo(path, kDefaultSuffix), 
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
  :   fgCache(fgUserCache),
      fgVisitedFiles(fgUserVisitedFiles),
      fPath(path, UnicodeString(kDefaultSuffix,"")),
      fIsDataOwned(TRUE),
      fRealLocale(localeName),
      fVersionID(0)
{
  status = U_ZERO_ERROR;
  
  int32_t i;
  for(i = 0; i < kDataCount; ++i) {
    fData[i] = 0;
    fLoaded[i] = FALSE;
    fDataStatus[i] = U_INTERNAL_PROGRAM_ERROR;
  }
  
  fLocaleIterator = 0;
  
  // If the file doesn't exist, return an error
  if(fPath.fileExists(UnicodeString(localeName,""))) {
    parse(fPath, UnicodeString(localeName, ""), saveCollationHashtable, 
	  (void*)this, fgCache, status);
  }
  else {
    status = U_MISSING_RESOURCE_ERROR;
  }
  
  // Prevent further attempts to load hashtables
  for(i = 0; i < kDataCount; ++i) 
    fLoaded[i] = TRUE;
}

void
ResourceBundle::saveCollationHashtable(const UnicodeString&,
				       UHashtable* hashtable,
				       void* context,
				       ResourceBundleCache*)
{
  ResourceBundle* bundle = (ResourceBundle*)context;
  for(int32_t i = 0; i < kDataCount; ++i) {
    if( ! bundle->fLoaded[i]) {
      bundle->fData[i] = hashtable;
      bundle->fLoaded[i] = TRUE;
      bundle->fDataStatus[i] = U_ZERO_ERROR; /* ??? */
      return;
    }
  }
  // Out of room; discard extra data.  We only expect to see one anyway.
  uhash_close(hashtable);
}

ResourceBundle::ResourceBundle(const wchar_t* path,
			       const Locale& locale, 
			       UErrorCode& err)
  : fgCache(fgUserCache),
    fgVisitedFiles(fgUserVisitedFiles)
{
  int32_t wideNameLen = uprv_mbstowcs(NULL, kDefaultSuffix, kDefaultSuffixLen);
  wchar_t* wideName = new wchar_t[wideNameLen + 1];
  uprv_mbstowcs(wideName, kDefaultSuffix, kDefaultSuffixLen);
  wideName[wideNameLen] = 0;
  constructForLocale(PathInfo(path, wideName), locale, err);
  delete [] wideName;
}

ResourceBundle::~ResourceBundle()
{
  delete fLocaleIterator;
  delete [] fVersionID;
  
  if(fIsDataOwned)
    for(int32_t i = 0; i < kDataCount; ++i) {
      if(fData[i]) 
	uhash_close((UHashtable*)fData[i]);
    }
}

void 
ResourceBundle::constructForLocale(const PathInfo& path,
				   const Locale& locale,
				   UErrorCode& error)
{
  int32_t i;
  fPath = path;
  fIsDataOwned = FALSE;
  fVersionID = 0;

  // fRealLocale can be inited in three ways, see 1), 2), 3)
  UnicodeString returnedLocale(locale.getName(), "");
  if (returnedLocale.length()!=0) {
	// 1) Desired Locale has a name
        fRealLocale = locale;
  } else {
	// 2) Desired Locale name is empty, so we use default locale for the system
	fRealLocale = Locale(kDefaultLocaleName); 
  }
  error = U_ZERO_ERROR;
  for(i = 1; i < kDataCount; ++i) {
    fData[i] = 0;
    fDataStatus[i] = U_INTERNAL_PROGRAM_ERROR;
    fLoaded[i] = FALSE;
  }
  
  error = U_ZERO_ERROR;
  fData[0] = getHashtableForLocale(fRealLocale.getName(), returnedLocale, error);
  fLoaded[0] = TRUE;
  fDataStatus[0] = U_ZERO_ERROR;
  if(U_SUCCESS(error))
		// 3) We're unable to get the desired Locale, so we're using what is provided (fallback occured)
    {
      /* To avoid calling deprecated api's */
      char *ch;
      ch = new char[returnedLocale.size() + 1];
      ch[returnedLocale.extract(0, 0x7fffffff, ch, "")] = 0;
      fRealLocale = Locale(ch);
      delete [] ch;
    }
  
  fLocaleIterator = new LocaleFallbackIterator(fRealLocale.getName(), 
					       kDefaultLocaleName, FALSE);
}

/**
 * Return the hash table with data for the given locale.  This method employs
 * fallback both in files and in locale names.  It returns the locale name
 * which is actually used to return the data, if any.
 *
 * Parse all files found at the given path for the given path, in an effort
 * to find data for the given locale.  Use fallbacks and defaults as needed.
 * Store read in file data in the cache for future use.  Return the hashtable
 * for the given locale, if found, or 0 if not.
 */
const UHashtable* 
ResourceBundle::getHashtableForLocale(const UnicodeString& desiredLocale,
				      UnicodeString& returnedLocale,
				      UErrorCode& error)
{
  if(U_FAILURE(error)) return 0;

  error = U_ZERO_ERROR;
  const UHashtable* h = getFromCache(fPath, desiredLocale, fgCache);
  if(h != 0) {
    returnedLocale = desiredLocale;
    return h;
  }
  
  LocaleFallbackIterator iterator(desiredLocale, kDefaultFilename, TRUE);
  bool_t didTryCacheWithFallback = FALSE;
  
  // A note on fileError.  We are tracking two different error states
  // here.  One is that returned while iterating over different files.
  // For instance, when going from de_CH.txt to de.txt we will get a
  // U_USING_FALLBACK_ERROR, but we don't care -- because if de.txt
  // contains the de_CH locale, it isn't a fallback, from our
  // perspective.  Therefore we keep file associated errors in
  // fileError, apart from the error parameter.
  UErrorCode fileError = U_ZERO_ERROR;

  for(;;) {
    // Build a filename for the locale.
    if(parseIfUnparsed(fPath, iterator.getLocale(), 
		       fgCache, fgVisitedFiles, error)) {
      if(U_FAILURE(error)) 
          return 0;
      
      error = U_ZERO_ERROR;
      h = getFromCacheWithFallback(fPath, desiredLocale, 
				   returnedLocale, fgCache, error);
      didTryCacheWithFallback = TRUE;
      if(h != 0 && U_SUCCESS(error)) 
	return h;
    }
    
    if(!iterator.nextLocale(fileError)) {
      error = U_MISSING_RESOURCE_ERROR;
      break;
    }
  }
  
  // We want to try loading from the cache will fallback at least
  // once.  These lines of code handle the case in which all of the
  // fallback FILES have been loaded, so fgVisitedFiles keeps us from
  // parsing them again.  In this case we still want to make an
  // attempt to load our locale from the cache.
  if(didTryCacheWithFallback) 
    return 0;
  error = U_ZERO_ERROR;
  return getFromCacheWithFallback(fPath, desiredLocale, 
				  returnedLocale, fgCache, error);
}

/**
 * Return the hash table with data for the given locale.  This method employs
 * fallback in file names only.  If data is returned, it will be exactly for
 * the given locale.
 */
const UHashtable* 
ResourceBundle::getHashtableForLocale(const UnicodeString& desiredLocale,
				      UErrorCode& error)
{
  if(U_FAILURE(error)) 
    return 0;
  error = U_ZERO_ERROR;
  
  // First try the cache
  const UHashtable* h = getFromCache(fPath, desiredLocale, fgCache);
  if(h != 0) 
    return h;
  
  // Now try files
  LocaleFallbackIterator iterator(desiredLocale, kDefaultFilename, FALSE);
  
  for(;;) {
    UErrorCode parseError = U_ZERO_ERROR;
    if(parseIfUnparsed(fPath, iterator.getLocale(), 
		       fgCache, fgVisitedFiles, parseError)) {
      if(U_FAILURE(parseError)) {
	error = parseError;
	return 0;
      }
      
      const UHashtable* h = getFromCache(fPath, desiredLocale, fgCache);
      if(h != 0) 
	return h;
    }
    
    if(!iterator.nextLocale(error)) 
      return 0;
  }
}

/**
 * Try to retrieve a locale data hash from the cache, using fallbacks
 * if necessary.  Ultimately we will try to load the data under
 * kDefaultLocaleName.  
 */
const UHashtable* 
ResourceBundle::getFromCacheWithFallback(const PathInfo& path,
					 const UnicodeString& desiredLocale,
					 UnicodeString& returnedLocale,
					 ResourceBundleCache* fgCache,
					 UErrorCode& error)
{
  if(U_FAILURE(error)) 
    return 0;
  error = U_ZERO_ERROR;
  
  LocaleFallbackIterator iterator(desiredLocale, kDefaultLocaleName, TRUE);
  
  for(;;) {
    const UHashtable* h = getFromCache(path, iterator.getLocale(), fgCache);
    if(h != 0) {
      returnedLocale = iterator.getLocale();
      return h;
    }
    
    if(!iterator.nextLocale(error)) 
      return 0;
  }
}

/**
 * Parse the given file, if it hasn't been attempted already, and if
 * it actually exists.  Return true if a parse is attempted.  Upon
 * return, if the return value is true, the error code may be set as a
 * result of a parse failure to a failing value.  If the parse was
 * successful, additional entries may have been created in the cache.
 */
bool_t
ResourceBundle::parseIfUnparsed(const PathInfo& path,
				const UnicodeString& locale,
				ResourceBundleCache* fgCache,
				VisitedFileCache* fgVisitedFiles,
				UErrorCode& error)
{
  UnicodeString key(path.makeCacheKey(locale));
  
  if(!fgVisitedFiles->wasVisited(key) && path.fileExists(locale)) {
    parse(path, locale, addToCache, (void*)&path, fgCache, error);
    { 
      Mutex lock;
      fgVisitedFiles->markAsVisited(key);
    }
    return TRUE;
  }
  return FALSE;
}

/**
 * Given a tag, try to retrieve the data for that tag.  This method is
 * semantically const, but may actually modify this object.  All
 * public API methods such as getString() rely on getDataForTag()
 * ultimately.  This method implements inheritance of data between
 * locales.  
 */
const ResourceBundleData* 
ResourceBundle::getDataForTag(const char *tag,
			      UErrorCode& err) const
{
  err = U_ZERO_ERROR; /* just to make sure there's no fallback/etc left over */
  // Iterate over the kDataCount hashtables which may be associated with this
  // bundle.  At most we have kDataCount, but we may have as few as one.
  for(int32_t i = 0; i < kDataCount; ++i) {

  // First try to load up this hashtable, if it hasn't been loaded yet.
    if(!fLoaded[i] && fData[i] == 0) {
      ResourceBundle* nonconst = (ResourceBundle*)this;
      nonconst->fLoaded[i] = TRUE;
      if(fLocaleIterator->nextLocale(err)) {
	UErrorCode getHashtableStatus = U_ZERO_ERROR;

	nonconst->fDataStatus[i] = err;
	nonconst->fData[i] = 
	  nonconst->getHashtableForLocale(fLocaleIterator->getLocale(), getHashtableStatus);
      }
    }

    
    if(fData[i] != 0) {
      UnicodeString t(tag, "");
      const ResourceBundleData* s = 
	(const ResourceBundleData*)uhash_get(fData[i], &t);
      if(s != 0) {
	err = fDataStatus[i];  /* restore the error from the original lookup. */
	return s;
      }
    }
  }
  
#ifdef _DEBUG
  //  cerr << "Failed to find tag " << tag << " in " << fPath << fRealLocaleID << fFilenameSuffix << endl;
  //  cerr << *this;
#endif
  err = U_MISSING_RESOURCE_ERROR;
  return 0;
}

void
ResourceBundle::getString(  const char             *resourceTag,
                            UnicodeString&          theString,
                            UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return;
  
  const UnicodeString* temp = getString(resourceTag, err);
  if(U_SUCCESS(err))
    theString = *temp;
}

const UnicodeString*
ResourceBundle::getString(  const char              *resourceTag,
                            UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return NULL;
  
  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == StringList::getStaticClassID() 
     && ((StringList*)data)->fCount == 1) {
    return &(((StringList*)data)->fStrings[0]);
  }
  else err = U_MISSING_RESOURCE_ERROR;
  return NULL;
}

const UnicodeString*
ResourceBundle::getStringArray( const char             *resourceTag,
                                int32_t&                count,
                                UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return 0;
  
  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == StringList::getStaticClassID()) {
    count = ((StringList*)data)->fCount;
    return ((StringList*)data)->fStrings;
  }
  err = U_MISSING_RESOURCE_ERROR;
  return 0;
}

void
ResourceBundle::getArrayItem(   const char             *resourceTag,
                                int32_t                 index,
                                UnicodeString&          theArrayItem,
                                UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return;

  const UnicodeString* temp = getArrayItem(resourceTag, index, err);
  if(U_SUCCESS(err))
    theArrayItem = *temp;
}

const UnicodeString*
ResourceBundle::getArrayItem(   const char             *resourceTag,
                                int32_t                 index,
                                UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return NULL;

  // Casting to unsigned turns a signed value into a large unsigned
  // value.  This allows us to do one comparison to check that 0 <=
  // index < count, instead of two separate comparisons for each index
  // check.
  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == StringList::getStaticClassID() 
     && (uint32_t)index < (uint32_t)((StringList*)data)->fCount) {
    return &(((StringList*)data)->fStrings[index]);
  }
  else
    err = U_MISSING_RESOURCE_ERROR;
  return NULL;
}

const UnicodeString** 
ResourceBundle::get2dArray(const char *resourceTag,
			   int32_t&             rowCount,
			   int32_t&             columnCount,
			   UErrorCode&           err) const
{
  if(U_FAILURE(err)) 
    return 0;

  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == String2dList::getStaticClassID()) {
    String2dList *list = (String2dList*)data;
    rowCount = list->fRowCount;
    columnCount = list->fColCount;
    // Why is this cast required? It shouldn't be. [LIU]
    return (const UnicodeString**)list->fStrings; 
  }
  err = U_MISSING_RESOURCE_ERROR;
  return 0;
}

void
ResourceBundle::get2dArrayItem(const char *resourceTag,
			       int32_t              rowIndex,
			       int32_t              columnIndex,
			       UnicodeString&       theArrayItem,
			       UErrorCode&           err) const
{
  if(U_FAILURE(err)) 
    return;
  
  const UnicodeString* temp = get2dArrayItem(resourceTag, rowIndex, 
					     columnIndex, err);
  
  if(U_SUCCESS(err))
    theArrayItem = *temp;
}

const UnicodeString*
ResourceBundle::get2dArrayItem(const char *resourceTag,
			       int32_t              rowIndex,
			       int32_t              columnIndex,
			       UErrorCode&           err) const
{
  if(U_FAILURE(err)) 
    return NULL;

  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == String2dList::getStaticClassID()) {
    String2dList *list = (String2dList*)data;
    // Casting to unsigned turns a signed value into a large unsigned
    // value.  This allows us to do one comparison to check that 0 <=
    // index < count, instead of two separate comparisons for each
    // index check.
    if(((uint32_t)rowIndex) < (uint32_t)(list->fRowCount) 
       && ((uint32_t)columnIndex) < (uint32_t)(list->fColCount)) {
      return &(list->fStrings[rowIndex][columnIndex]);
    }
  }
  err = U_MISSING_RESOURCE_ERROR;
  return NULL;
}

void
ResourceBundle::getTaggedArrayItem( const char             *resourceTag,
                                    const UnicodeString&    itemTag,
                                    UnicodeString&          theArrayItem,
                                    UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return;
  
  const UnicodeString* temp = getTaggedArrayItem(resourceTag, itemTag, err);
    
  if(U_SUCCESS(err)) 
    theArrayItem = *temp;
}

const UnicodeString*
ResourceBundle::getTaggedArrayItem( const char             *resourceTag,
                                    const UnicodeString&    itemTag,
                                    UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return NULL;

  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(data != 0 
     && data->getDynamicClassID() == TaggedList::getStaticClassID()) {
    const UnicodeString* s = ((TaggedList*)data)->get(itemTag);
    if(s != 0)  
      return s;
  }
  
  err = U_MISSING_RESOURCE_ERROR;
  return NULL;
}

extern "C" void 
T_ResourceBundle_getTaggedArrayUChars(const ResourceBundle*   bundle,
				      const char           *resourceTag,
				      UChar const**         itemTags,
				      UChar const**         items,
				      int32_t                    maxItems,
				      int32_t*                numItems,
				      UErrorCode*              err)
{
  // this function is here solely because there seems to be no way to
  // declare an extern "C" function as a friend of a class.  So we
  // have a function with ordinary C++ linkage that is a friend of
  // ResourceBundle and does the work, and a hidden method with C
  // linkage that calls it and is used by the C wrappers.  Disgusting,
  // isn't it?  This was all rtg's idea.  --jf 12/16/98
  getTaggedArrayUCharsImplementation(bundle, resourceTag,
				     itemTags, items, maxItems, 
				     *numItems, *err);
}

void 
getTaggedArrayUCharsImplementation( const ResourceBundle*   bundle,
				    const char           *resourceTag,
				    UChar const**         itemTags,
				    UChar const**     items,
				    int32_t        maxItems,
				    int32_t&          numItems,
				    UErrorCode&              err)
{
  // this is here solely to support the C implementation of
  // ResourceBundle.  This function isn't defined as part of the API;
  // The C wrappers know it's here and define it on their own.  --jf
  // 12/16/98
  if(U_FAILURE(err)) 
    return;

  const ResourceBundleData* data = bundle->getDataForTag(resourceTag, err);
  if(U_FAILURE(err) || data == 0 
     || data->getDynamicClassID() != TaggedList::getStaticClassID()) {
    err = U_MISSING_RESOURCE_ERROR;
    return;
  }
  
  numItems = 0;
  int32_t pos = -1;
  const UnicodeString *key, *value;
  while (((TaggedList*)data)->nextElement(key, value, pos) &&
         numItems < maxItems) {
      itemTags[numItems] = key->getUChars();
      items[numItems] = value->getUChars();
      numItems++;
  }
}

void
ResourceBundle::getTaggedArray( const char             *resourceTag,
                                UnicodeString*&         itemTags,
                                UnicodeString*&         items,
                                int32_t&                numItems,
                                UErrorCode&              err) const
{
  if(U_FAILURE(err)) 
    return;

  const ResourceBundleData* data = getDataForTag(resourceTag, err);
  if(U_FAILURE(err) || data == 0 
     || data->getDynamicClassID() != TaggedList::getStaticClassID()) {
    err = U_MISSING_RESOURCE_ERROR;
    return;
  }
  
  // go through the resource once and count how many items there are
  
  numItems = ((TaggedList*)data)->count();
  
  // now create the string arrays and go through the hash table again, this
  // time copying the keys and values into the string arrays
  itemTags = new UnicodeString[numItems];
  items = new UnicodeString[numItems];
  
  numItems = 0;
  int32_t pos = -1;
  const UnicodeString *key, *value;
  while (((TaggedList*)data)->nextElement(key, value, pos)) {
      itemTags[numItems] = *key;
      items[numItems] = *value;
      numItems++;
  }
}

const char*
ResourceBundle::getVersionNumber()  const
{
  if(fVersionID == 0) {
    // If the version ID has not been built yet, then do so.  Retrieve
    // the minor version from the file.
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString minor_version;
    getString(kVersionTag, minor_version, status);
    
    // Determine the length of of the final version string.  This is
    // the length of the major part + the length of the separator
    // (==1) + the length of the minor part (+ 1 for the zero byte at
    // the end).
    int32_t len = uprv_strlen(U_ICU_VERSION);
    int32_t minor_len = 0;
    if(U_SUCCESS(status) && minor_version.length() > 0) 
      minor_len = minor_version.length();
    len += (minor_len > 0) ? minor_len : 1 /*==uprv_strlen(kDefaultMinorVersion)*/;
    ++len; // Add length of separator
    
    // Allocate the string, and build it up.
    // + 1 for zero byte
    ((ResourceBundle*)this)->fVersionID = new char[1 + len]; 
    
    uprv_strcpy(fVersionID, U_ICU_VERSION);
    uprv_strcat(fVersionID, kVersionSeparator);
    if(minor_len > 0) {
      minor_version.extract(0, minor_len, fVersionID + len - minor_len);
      fVersionID[len] =  0;
    }
    else {
      uprv_strcat(fVersionID, kDefaultMinorVersion);
    }
  }
  return fVersionID;
}

const UnicodeString*
ResourceBundle::listInstalledLocales(const UnicodeString& path,
                                     int32_t&   numInstalledLocales)
{
  const UnicodeString kDefaultSuffixString = UnicodeString(kDefaultSuffix,"");


  const UHashtable* h = getFromCache(PathInfo(path, kDefaultSuffixString), 
				     UnicodeString(kIndexLocaleName,""), fgUserCache);
  
  if(h == 0) {
    UErrorCode error = U_ZERO_ERROR;
    if(parseIfUnparsed(PathInfo(path, kDefaultSuffixString), 
		       UnicodeString(kIndexFilename,""), fgUserCache, 
		       fgUserVisitedFiles, error)) {
      h = getFromCache(PathInfo(path, kDefaultSuffixString), 
		       UnicodeString(kIndexLocaleName,""), fgUserCache);
    }
  }
  
  if(h != 0) {
    UnicodeString ukIndexTag = UnicodeString(kIndexTag,"");
    ResourceBundleData *data = 
      (ResourceBundleData*) uhash_get(h, &ukIndexTag);
    if(data != 0 
       && data->getDynamicClassID() == StringList::getStaticClassID()) {
      numInstalledLocales = ((StringList*)data)->fCount;
      return ((StringList*)data)->fStrings;
    }
  }
  
  numInstalledLocales = 0;
  return 0;
}

extern "C" const UnicodeString** 
T_ResourceBundle_listInstalledLocales(const char* path,
				      int32_t* numInstalledLocales) 
{
  // this is here solely to support the C implementation of Locale.
  // This function isn't defined as part of the API; T_Locale knows
  // it's here and defines it on its own.  --rtg 11/28/98
  
  return listInstalledLocalesImplementation(path, numInstalledLocales);
}

const UnicodeString** 
listInstalledLocalesImplementation(const char* path,
				   int32_t* numInstalledLocales) 
{
  // this function is here solely because there seems to be no way to
  // declare an extern "C" function as a friend of a class.  So we
  // have a function with ordinary C++ linkage that is a friend of
  // ResourceBundle and does the work, and a hidden method with C
  // linkage that calls it and is used by the C implementation of
  // Locale.  Disgusting, isn't it?  --rtg 11/30/98
  const UnicodeString* array = (ResourceBundle::listInstalledLocales(UnicodeString(path,""), *numInstalledLocales));
  const UnicodeString**  arrayOfPtrs = (const UnicodeString**) new UnicodeString*[*numInstalledLocales];
  for(int i = 0; i < *numInstalledLocales; i++)
    arrayOfPtrs[i] = &array[i];
  return arrayOfPtrs;
}

int32_t
T_ResourceBundle_countArrayItemsImplementation(const ResourceBundle* resourceBundle, 
					       const char* resourceKey,
					       UErrorCode& err) 
{
  if(U_FAILURE(err)) 
    return 0;

  if(!resourceKey) {
    err = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }
  const ResourceBundleData* data  = resourceBundle->getDataForTag(resourceKey,
								  err);
  if(U_FAILURE(err)) 
    return 0;
  
  UClassID rbkeyClassID = data->getDynamicClassID();
  int32_t numItems = 0;
  
  if(rbkeyClassID == StringList::getStaticClassID()) {
    numItems = ((StringList*)data)->fCount;
  }
  else if(rbkeyClassID == TaggedList::getStaticClassID()) {
    numItems =  ((TaggedList*)data)->count();
  }
  else if(rbkeyClassID == String2dList::getStaticClassID()) {
    numItems = ((String2dList*)data)->fRowCount; 
  }
  else {
    err = U_MISSING_RESOURCE_ERROR;
    return 0;
  }
  
  return numItems;
}


extern "C" int32_t 
T_ResourceBundle_countArrayItems(const ResourceBundle* resourceBundle, 
				 const char* resourceKey,
				 UErrorCode* err) 
{
  return T_ResourceBundle_countArrayItemsImplementation(resourceBundle,
							resourceKey,
							*err);
}

/**
 * Retrieve a ResourceBundle from the cache.  Return NULL if not found.
 */
const UHashtable* 
ResourceBundle::getFromCache(const PathInfo& path,
			     const UnicodeString& localeName,
			     ResourceBundleCache* fgCache)
{
    UnicodeString keyname(path.makeHashkey(localeName));
    Mutex lock;
    return fgCache->get(keyname);
}

/**
 * Parse a file, storing the resource data in the cache.
 */
void 
ResourceBundle::parse(const PathInfo& path,
		      const UnicodeString& locale,
		      Handler handler,
		      void *context,
		      ResourceBundleCache *fgCache,
		      UErrorCode& status)
{
  FileStream *f;
  UnicodeString localeName, realLocale;
  UHashtable *data;
  
  if (U_FAILURE(status)) return;
  
  f = path.openFile(locale);
  if(f == 0) {
    status = U_FILE_ACCESS_ERROR;
    return;
  }
  
  realLocale = locale;
  /* Get the data from the compiled resource bundle file */
  data = rb_parse(f, localeName, status);

  /* Close the file */
  T_FileStream_close(f);

  if(U_FAILURE(status)) {
    return;
  }
  /* If an alias file is encountered, parse the new locale file. */
  if (data == 0) {
      f = path.openFile(localeName);
      if(f == 0) {
        status = U_FILE_ACCESS_ERROR;
        return;
      }
      data = rb_parse(f, localeName, status);
      T_FileStream_close(f);

      if(U_FAILURE(status)) {
        return;
      }
      localeName = realLocale;
  }

  /* Invoke the handler function */
  handler(localeName, data, context, fgCache);
}

void
ResourceBundle::addToCache(const UnicodeString& localeName,
			   UHashtable* hashtable,
			   void* context,
			   ResourceBundleCache* fgCache)
{
  PathInfo *c = (PathInfo*)context;
  UnicodeString keyName(c->makeHashkey(localeName));
  Mutex lock;
  if (fgCache->get(keyName) == 0) {
      fgCache->put(keyName, hashtable);
  }
}

const Locale &ResourceBundle::getLocale(void) const
{
	return fRealLocale;
}

ResourceBundle::PathInfo::PathInfo()
  : fWPrefix(NULL), fWSuffix(NULL)
{}

ResourceBundle::PathInfo::PathInfo(const PathInfo& source) 
  : fPrefix(source.fPrefix), 
    fSuffix(source.fSuffix), 
    fWPrefix(NULL), fWSuffix(NULL)
{
  if(source.fWPrefix) {
    fWPrefix = new wchar_t[uprv_wcslen(source.fWPrefix)+1];
    fWSuffix = new wchar_t[uprv_wcslen(source.fWSuffix)+1];
    uprv_wcscpy(fWPrefix, source.fWPrefix);
    uprv_wcscpy(fWSuffix, source.fWSuffix);
  }
}

ResourceBundle::PathInfo::PathInfo(const UnicodeString& path)
  : fPrefix(path), 
    fWPrefix(NULL), 
    fWSuffix(NULL)
{}

ResourceBundle::PathInfo::PathInfo(const UnicodeString& path, 
				   const UnicodeString& suffix)
  : fPrefix(path), 
    fSuffix(suffix), 
    fWPrefix(NULL), 
    fWSuffix(NULL)
{}

ResourceBundle::PathInfo::PathInfo(const wchar_t* path,
				   const wchar_t* suffix)
  : fPrefix(), 
    fSuffix(), 
    fWPrefix(NULL), 
    fWSuffix(NULL)
{
  fWPrefix = new wchar_t[uprv_wcslen(path)+1];
  fWSuffix = new wchar_t[uprv_wcslen(suffix)+1];
  uprv_wcscpy(fWPrefix, path);
  uprv_wcscpy(fWSuffix, suffix);
}

ResourceBundle::PathInfo::~PathInfo()
{
  delete [] fWPrefix;
  delete [] fWSuffix;
}

ResourceBundle::PathInfo&
ResourceBundle::PathInfo::operator=(const PathInfo& source)
{
  if(this != &source) {
    wchar_t* tempPref = NULL;
    wchar_t* tempSuff = NULL;
    if(source.fWPrefix) {
      tempPref = new wchar_t[uprv_wcslen(source.fWPrefix)+1];
      tempSuff = new wchar_t[uprv_wcslen(source.fWSuffix)+1];
      uprv_wcscpy(tempPref, source.fWPrefix);
      uprv_wcscpy(tempSuff, source.fWSuffix);
    }
    delete fWPrefix;
    fWPrefix = tempPref;
    delete fWSuffix;
    fWSuffix = tempSuff;
    fPrefix = source.fPrefix;
    fSuffix = source.fSuffix;
  }
  return *this;
}

bool_t 
ResourceBundle::PathInfo::fileExists(const UnicodeString& localeName) const
{
  FileStream *temp = openFile(localeName);
  if(temp) {
    T_FileStream_close(temp);
    return TRUE;
  }
  else {
    return FALSE;
  }
}

UnicodeString 
ResourceBundle::PathInfo::makeCacheKey(const UnicodeString& name) const
{
  if(fWPrefix) {
    UnicodeString key;
    
    size_t prefSize = uprv_wcstombs(NULL, fWPrefix, ((size_t)-1) >> 1);
    size_t suffSize = uprv_wcstombs(NULL, fWSuffix, ((size_t)-1) >> 1);
    size_t tempSize = uprv_max((int32_t)prefSize, (int32_t)suffSize);
    char *temp = new char[tempSize + 1];

    tempSize = uprv_wcstombs(temp, fWPrefix, prefSize);
    temp[tempSize] = 0;
    key += UnicodeString(temp);
    
    key += name;
    
    tempSize = uprv_wcstombs(temp, fWSuffix, suffSize);
    temp[tempSize] = 0;
    key += UnicodeString(temp);
    
    delete [] temp;
    
    return key;
  } 
  else {
    UnicodeString workingName(fPrefix);
    workingName += name;
    workingName += fSuffix;
    
    return workingName;
  }
}

UnicodeString 
ResourceBundle::PathInfo::makeHashkey(const UnicodeString& localeName) const
{
  if(fWPrefix) {
    UnicodeString key(localeName);
    
    key += kSeparator;
    
    size_t prefSize = uprv_wcstombs(NULL, fWPrefix, ((size_t)-1) >> 1);
    size_t suffSize = uprv_wcstombs(NULL, fWSuffix, ((size_t)-1) >> 1);
    size_t tempSize = uprv_max((int32_t)prefSize, (int32_t)suffSize);
    char *temp = new char[tempSize + 1];
    
    tempSize = uprv_wcstombs(temp, fWSuffix, suffSize);
    temp[tempSize] = 0;
    key += UnicodeString(temp);
    
    key += kSeparator;
    
    tempSize = uprv_wcstombs(temp, fWPrefix, prefSize);
    temp[tempSize] = 0;
    key += UnicodeString(temp);
    
    delete [] temp;
    
    return key;
  }
  else {
    UnicodeString keyName = localeName;
    keyName += kSeparator;
    keyName += fSuffix;
    keyName += kSeparator;
    keyName += fPrefix;
    return keyName;
  }
}

FileStream* 
ResourceBundle::PathInfo::openFile(const UnicodeString& localeName) const
{
  if(fWPrefix) {
    //use the wide version of fopen in TPlatformUtilities.
    int32_t nameSize = localeName.length();
    char* temp = new char[nameSize + 1];
    localeName.extract(0, nameSize, temp);
    temp[nameSize] = 0;
    int32_t wideNameLen = uprv_mbstowcs(NULL, temp, nameSize);
    wchar_t* wideName = new wchar_t[wideNameLen + 1];
    uprv_mbstowcs(wideName, temp, nameSize);
    wideName[wideNameLen] = 0;
    delete [] temp;
    
    size_t prefLen = uprv_wcslen(fWPrefix);
    size_t suffLen = uprv_wcslen(fWSuffix);
    
    int32_t destSize = prefLen + suffLen + wideNameLen;
    wchar_t* dest = new wchar_t[destSize + 1];
    uprv_wcscpy(dest, fWPrefix);
    dest[prefLen] = 0;
    
    uprv_wcscat(dest, wideName);
    dest[prefLen + wideNameLen] = 0;
    
    uprv_wcscat(dest, fWSuffix);
    dest[destSize] = 0;
    
    int32_t fmodeLen = uprv_mbstowcs(NULL, "rb", 2);
    wchar_t* fmode = new wchar_t[fmodeLen + 1];
    uprv_mbstowcs(fmode, "rb", 2);
    fmode[fmodeLen] = 0;

    FileStream* result = T_FileStream_wopen(dest, fmode);
    
    delete [] fmode;
    delete [] dest;
    delete [] wideName;
    return result;
  } 
  else {
    //open file using standard char* routines
    UnicodeString workingName(makeCacheKey(localeName));
    int32_t size = workingName.length();
    char* returnVal = new char[size + 1];
    workingName.extract(0, size, returnVal, "");
    returnVal[size] = 0;
    FileStream* result = T_FileStream_open(returnVal, "rb");
    delete [] returnVal;
    return result;
  }
}

const UChar ResourceBundle::PathInfo::kSeparator = 0xF8FF;

//eof
