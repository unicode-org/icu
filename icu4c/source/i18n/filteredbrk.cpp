/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

#include "unicode/filteredbrk.h"

#if !UCONFIG_NO_BREAK_ITERATION && U_HAVE_STD_STRING && !UCONFIG_NO_FILTERED_BREAK_ITERATION

#include <unicode/ucharstriebuilder.h>

#include <set>
#include <string>
#include <functional>
#include "uresimp.h"
#include "ubrkimpl.h"

U_NAMESPACE_BEGIN

using namespace std;

static const int32_t kPARTIAL = (1<<0); //< partial - need to run through forward trie
static const int32_t kMATCH   = (1<<1); //< exact match - skip this one.
static const int32_t kSuppressInReverse = (1<<0);
static const int32_t kAddToForward = (1<<1);
static const UChar kFULLSTOP = 0x002E; // '.'

class ULISentenceBreakIterator : public BreakIterator {
public:
  ULISentenceBreakIterator(BreakIterator *adopt, UCharsTrie *forwards, UCharsTrie *backwards, UErrorCode &status);
  virtual ~ULISentenceBreakIterator() {}
  ULISentenceBreakIterator(const ULISentenceBreakIterator& other);
private:
  LocalPointer<BreakIterator> fDelegate;
  LocalUTextPointer           fText;
  LocalPointer<UCharsTrie>    fBackwardsTrie; //  i.e. ".srM" for Mrs.
  LocalPointer<UCharsTrie>    fForwardsPartialTrie; //  Has ".a" for "a.M."

  /* -- subclass interface -- */
public:
  /* -- cloning and other subclass stuff -- */
  virtual BreakIterator *  createBufferClone(void * /*stackBuffer*/,
                                             int32_t &/*BufferSize*/,
                                             UErrorCode &status) {
    // for now - always deep clone
    status = U_SAFECLONE_ALLOCATED_WARNING;
    return clone();
  }
  virtual BreakIterator* clone(void) const { return new ULISentenceBreakIterator(*this); }
  virtual UClassID getDynamicClassID(void) const { return NULL; }
  virtual UBool operator==(const BreakIterator& o) const { if(*this==o) return true; return false; }

  /* -- text modifying -- */
  virtual void setText(UText *text, UErrorCode &status) { fDelegate->setText(text,status); }
  virtual BreakIterator &refreshInputText(UText *input, UErrorCode &status) { fDelegate->refreshInputText(input,status); return *this; }
  virtual void adoptText(CharacterIterator* it) { fDelegate->adoptText(it); }
  virtual void setText(const UnicodeString &text) { fDelegate->setText(text); }

  /* -- other functions that are just delegated -- */
  virtual UText *getUText(UText *fillIn, UErrorCode &status) const { return fDelegate->getUText(fillIn,status); }
  virtual CharacterIterator& getText(void) const { return fDelegate->getText(); }

  /* -- ITERATION -- */
  virtual int32_t first(void) { return fDelegate->first(); }
  virtual int32_t preceding(int32_t offset) { return fDelegate->preceding(offset); }
  virtual int32_t previous(void) { return fDelegate->previous(); }
  virtual UBool isBoundary(int32_t offset) { return fDelegate->isBoundary(offset); }
  virtual int32_t current(void) const { return fDelegate->current(); }

  virtual int32_t next(void);

  virtual int32_t next(int32_t n) { return fDelegate->next(n); }
  virtual int32_t following(int32_t offset) { return fDelegate->following(offset); }
  virtual int32_t last(void) { return fDelegate->last(); }

};

ULISentenceBreakIterator::ULISentenceBreakIterator(const ULISentenceBreakIterator& other)
  : BreakIterator(other), fDelegate(other.fDelegate->clone())
{
  /*
    TODO: not able to clone Tries. Should be a refcounted hidden master instead.
  if(other.fBackwardsTrie.isValid()) {
    fBackwardsTrie.adoptInstead(other.fBackwardsTrie->clone());
  }
  if(other.fForwardsPartialTrie.isValid()) {
    fForwardsPartialTrie.adoptInstead(other.fForwardsPartialTrie->clone());
  }
  */
}


ULISentenceBreakIterator::ULISentenceBreakIterator(BreakIterator *adopt, UCharsTrie *forwards, UCharsTrie *backwards, UErrorCode &status) :
  BreakIterator(adopt->getLocale(ULOC_VALID_LOCALE,status),adopt->getLocale(ULOC_ACTUAL_LOCALE,status)),
  fDelegate(adopt),
  fBackwardsTrie(backwards),
  fForwardsPartialTrie(forwards)
{
  // all set..
}

int32_t ULISentenceBreakIterator::next() {
  int32_t n = fDelegate->next();
  if(n == UBRK_DONE || // at end  or
     fBackwardsTrie.isNull()) { // .. no backwards table loaded == no exceptions
    return n;
  }
  // OK, do we need to break here?
  UErrorCode status = U_ZERO_ERROR;
  // refresh text
  fText.adoptInstead(fDelegate->getUText(fText.orphan(), status));
  //if(debug2) u_printf("str, native len=%d\n", utext_nativeLength(fText.getAlias()));
  do { // outer loop runs once per underlying break (from fDelegate).
    // loops while 'n' points to an exception.
    utext_setNativeIndex(fText.getAlias(), n); // from n..
    fBackwardsTrie->reset();
    UChar32 uch;
    //if(debug2) u_printf(" n@ %d\n", n);
    // Assume a space is following the '.'  (so we handle the case:  "Mr. /Brown")
    if((uch=utext_previous32(fText.getAlias()))==(UChar32)0x0020) {  // TODO: skip a class of chars here??
      // TODO only do this the 1st time?
      //if(debug2) u_printf("skipping prev: |%C| \n", (UChar)uch);
    } else {
      //if(debug2) u_printf("not skipping prev: |%C| \n", (UChar)uch);
      uch = utext_next32(fText.getAlias());
      //if(debug2) u_printf(" -> : |%C| \n", (UChar)uch);
    }
    UStringTrieResult r = USTRINGTRIE_INTERMEDIATE_VALUE;

    int32_t bestPosn = -1;
    int32_t bestValue = -1;

    while((uch=utext_previous32(fText.getAlias()))!=U_SENTINEL  &&   // more to consume backwards and..
          USTRINGTRIE_HAS_NEXT(r=fBackwardsTrie->nextForCodePoint(uch))) {// more in the trie
      if(USTRINGTRIE_HAS_VALUE(r)) { // remember the best match so far
        bestPosn = utext_getNativeIndex(fText.getAlias());
        bestValue = fBackwardsTrie->getValue();
      }
      //if(debug2) u_printf("rev< /%C/ cont?%d @%d\n", (UChar)uch, r, utext_getNativeIndex(fText.getAlias()));
    }

    if(USTRINGTRIE_MATCHES(r)) { // exact match?
      //if(debug2) u_printf("rev<?/%C/?end of seq.. r=%d, bestPosn=%d, bestValue=%d\n", (UChar)uch, r, bestPosn, bestValue);
      bestValue = fBackwardsTrie->getValue();
      bestPosn = utext_getNativeIndex(fText.getAlias());
      //if(debug2) u_printf("rev<+/%C/+end of seq.. r=%d, bestPosn=%d, bestValue=%d\n", (UChar)uch, r, bestPosn, bestValue);
    }

    if(bestPosn>=0) {
      //if(debug2) u_printf("rev< /%C/ end of seq.. r=%d, bestPosn=%d, bestValue=%d\n", (UChar)uch, r, bestPosn, bestValue);

      //if(USTRINGTRIE_MATCHES(r)) {  // matched - so, now what?
      //int32_t bestValue = fBackwardsTrie->getValue();
      ////if(debug2) u_printf("rev< /%C/ matched, skip..%d  bestValue=%d\n", (UChar)uch, r, bestValue);

      if(bestValue == kMATCH) { // exact match!
        //if(debug2) u_printf(" exact backward match\n");
        n = fDelegate->next(); // skip this one. Find the next lowerlevel break.
        if(n==UBRK_DONE) return n;
        continue; // See if the next is another exception.
      } else if(bestValue == kPARTIAL
                && fForwardsPartialTrie.isValid()) { // make sure there's a forward trie
        //if(debug2) u_printf(" partial backward match\n");
        // We matched the "Ph." in "Ph.D." - now we need to run everything through the forwards trie
        // to see if it matches something going forward.
        fForwardsPartialTrie->reset();
        UStringTrieResult rfwd = USTRINGTRIE_INTERMEDIATE_VALUE;
        utext_setNativeIndex(fText.getAlias(), bestPosn); // hope that's close ..
        //if(debug2) u_printf("Retrying at %d\n", bestPosn);
        while((uch=utext_next32(fText.getAlias()))!=U_SENTINEL &&
              USTRINGTRIE_HAS_NEXT(rfwd=fForwardsPartialTrie->nextForCodePoint(uch))) {
          //if(debug2) u_printf("fwd> /%C/ cont?%d @%d\n", (UChar)uch, rfwd, utext_getNativeIndex(fText.getAlias()));
        }
        if(USTRINGTRIE_MATCHES(rfwd)) {
          //if(debug2) u_printf("fwd> /%C/ == forward match!\n", (UChar)uch);
          // only full matches here, nothing to check
          // skip the next:
          n = fDelegate->next();
          if(n==UBRK_DONE) return n;
          continue;
        } else {
          //if(debug2) u_printf("fwd> /%C/ no match.\n", (UChar)uch);
          // no match (no exception) -return the 'underlying' break
          return n;
        }
      } else {
        return n; // internal error and/or no forwards trie
      }
    } else {
      //if(debug2) u_printf("rev< /%C/ .. no match..%d\n", (UChar)uch, r);  // no best match
      return n; // No match - so exit. Not an exception.
    }
  } while(n != UBRK_DONE);
  return n;
}

U_NAMESPACE_END

#if 0
// Would improve performance - but, platform issues.
// for the 'set'
namespace std {
  template <> struct hash<icu::UnicodeString> {
    size_t operator()( const UnicodeString& str ) const {
      return (size_t)str.hashCode();
    }
  };
}
#endif

U_NAMESPACE_BEGIN

class SimpleFilteredBreakIteratorBuilder : public FilteredBreakIteratorBuilder {
public:
  virtual ~SimpleFilteredBreakIteratorBuilder();
  SimpleFilteredBreakIteratorBuilder(const Locale &fromLocale, UErrorCode &status);
  SimpleFilteredBreakIteratorBuilder();
  virtual UBool suppressBreakAfter(const UnicodeString& exception, UErrorCode& status);
  virtual UBool unsuppressBreakAfter(const UnicodeString& exception, UErrorCode& status);
  virtual BreakIterator *build(BreakIterator* adoptBreakIterator, UErrorCode& status);
private:
  set<UnicodeString> fSet;
};

SimpleFilteredBreakIteratorBuilder::~SimpleFilteredBreakIteratorBuilder()
{
}

SimpleFilteredBreakIteratorBuilder::SimpleFilteredBreakIteratorBuilder(const Locale &fromLocale, UErrorCode &status)
  : fSet()
{
  if(U_SUCCESS(status)) {
    LocalUResourceBundlePointer b(ures_open(U_ICUDATA_BRKITR, fromLocale.getBaseName(), &status));
    LocalUResourceBundlePointer exceptions(ures_getByKeyWithFallback(b.getAlias(), "exceptions", NULL, &status));
    LocalUResourceBundlePointer breaks(ures_getByKeyWithFallback(exceptions.getAlias(), "SentenceBreak", NULL, &status));
    if(U_FAILURE(status)) return; // leaves the builder empty, if you try to use it.

    LocalUResourceBundlePointer strs;
    UErrorCode subStatus = status;
    do {
      strs.adoptInstead(ures_getNextResource(breaks.getAlias(), strs.orphan(), &subStatus));
      if(strs.isValid() && U_SUCCESS(subStatus)) {
        UnicodeString str(ures_getUnicodeString(strs.getAlias(), &status));
        suppressBreakAfter(str, status); // load the string
      }
    } while (strs.isValid() && U_SUCCESS(subStatus));
    if(U_FAILURE(subStatus)&&subStatus!=U_INDEX_OUTOFBOUNDS_ERROR&&U_SUCCESS(status)) {
      status = subStatus;
    }
  }
}

SimpleFilteredBreakIteratorBuilder::SimpleFilteredBreakIteratorBuilder()
  : fSet()
{
}

UBool
SimpleFilteredBreakIteratorBuilder::suppressBreakAfter(const UnicodeString& exception, UErrorCode& status)
{
  if( U_FAILURE(status) ) return FALSE;
  return fSet.insert(exception).second;
}

UBool
SimpleFilteredBreakIteratorBuilder::unsuppressBreakAfter(const UnicodeString& exception, UErrorCode& status)
{
  if( U_FAILURE(status) ) return FALSE;
  return ((fSet.erase(exception)) != 0);
}

BreakIterator *
SimpleFilteredBreakIteratorBuilder::build(BreakIterator* adoptBreakIterator, UErrorCode& status) {
  LocalPointer<BreakIterator> adopt(adoptBreakIterator);

  if(U_FAILURE(status)) {
    return NULL;
  }

  LocalPointer<UCharsTrieBuilder> builder(new UCharsTrieBuilder(status));
  LocalPointer<UCharsTrieBuilder> builder2(new UCharsTrieBuilder(status));

  int32_t revCount = 0;
  int32_t fwdCount = 0;

  int32_t subCount = fSet.size();
  LocalArray<UnicodeString> ustrs(new UnicodeString[subCount]);
  LocalArray<int> partials(new int[subCount]);

  LocalPointer<UCharsTrie>    backwardsTrie; //  i.e. ".srM" for Mrs.
  LocalPointer<UCharsTrie>    forwardsPartialTrie; //  Has ".a" for "a.M."

  int n=0;
  for ( set<UnicodeString>::iterator i = fSet.begin();
        i != fSet.end();
        i++) {
    const UnicodeString &abbr = *i;
    ustrs[n] = abbr;
    partials[n] = 0; // default: not partial
    n++;
  }
  // first pass - find partials.
  for(int i=0;i<subCount;i++) {
    int nn = ustrs[i].indexOf(kFULLSTOP); // TODO: non-'.' abbreviations
    if(nn>-1 && (nn+1)!=ustrs[i].length()) {
      //if(true) u_printf("Is a partial: /%S/\n", ustrs[i].getTerminatedBuffer());
      // is partial.
      // is it unique?
      int sameAs = -1;
      for(int j=0;j<subCount;j++) {
        if(j==i) continue;
        if(ustrs[i].compare(0,nn+1,ustrs[j],0,nn+1)==0) {
          //if(true) u_printf("Prefix match: /%S/ to %d\n", ustrs[j].getTerminatedBuffer(), nn+1);
          //UBool otherIsPartial = ((nn+1)!=ustrs[j].length());  // true if ustrs[j] doesn't end at nn
          if(partials[j]==0) { // hasn't been processed yet
            partials[j] = kSuppressInReverse | kAddToForward;
            //if(true) u_printf("Suppressing: /%S/\n", ustrs[j].getTerminatedBuffer());
          } else if(partials[j] & kSuppressInReverse) {
            sameAs = j; // the other entry is already in the reverse table.
          }
        }
      }
      //if(debug2) u_printf("for partial /%S/ same=%d partials=%d\n",      ustrs[i].getTerminatedBuffer(), sameAs, partials[i]);
      UnicodeString prefix(ustrs[i], 0, nn+1);
      if(sameAs == -1 && partials[i] == 0) {
        // first one - add the prefix to the reverse table.
        prefix.reverse();
        builder->add(prefix, kPARTIAL, status);
        revCount++;
        //if(debug2) u_printf("Added Partial: /%S/ from /%S/ status=%s\n", prefix.getTerminatedBuffer(), ustrs[i].getTerminatedBuffer(), u_errorName(status));
        partials[i] = kSuppressInReverse | kAddToForward;
      } else {
        //if(debug2) u_printf(" // not adding partial for /%S/ from /%S/\n", prefix.getTerminatedBuffer(), ustrs[i].getTerminatedBuffer());
      }
    }
  }
  for(int i=0;i<subCount;i++) {
    if(partials[i]==0) {
      ustrs[i].reverse();
      builder->add(ustrs[i], kMATCH, status);
      revCount++;
      //if(debug2) u_printf("Added: /%S/ status=%s\n", ustrs[i].getTerminatedBuffer(), u_errorName(status));
    } else {
      //if(debug2) u_printf(" Adding fwd: /%S/\n", ustrs[i].getTerminatedBuffer());

      // an optimization would be to only add the portion after the '.'
      // for example, for "Ph.D." we store ".hP" in the reverse table. We could just store "D." in the forward,
      // instead of "Ph.D." since we already know the "Ph." part is a match.
      // would need the trie to be able to hold 0-length strings, though.
      builder2->add(ustrs[i], kMATCH, status); // forward
      fwdCount++;
      //ustrs[i].reverse();
      ////if(debug2) u_printf("SUPPRESS- not Added(%d):  /%S/ status=%s\n",partials[i], ustrs[i].getTerminatedBuffer(), u_errorName(status));
    }
  }
  //if(debug) u_printf(" %s has %d abbrs.\n", fJSONSource.c_str(), subCount);

  if(revCount>0) {
    backwardsTrie.adoptInstead(builder->build(USTRINGTRIE_BUILD_FAST, status));
    if(U_FAILURE(status)) {
      //printf("Error %s building backwards\n", u_errorName(status));
      return NULL;
    }
  }

  if(fwdCount>0) {
    forwardsPartialTrie.adoptInstead(builder2->build(USTRINGTRIE_BUILD_FAST, status));
    if(U_FAILURE(status)) {
      //printf("Error %s building forwards\n", u_errorName(status));
      return NULL;
    }
  }

  return new ULISentenceBreakIterator(adopt.orphan(), forwardsPartialTrie.orphan(), backwardsTrie.orphan(), status);
}


// -----------

FilteredBreakIteratorBuilder::FilteredBreakIteratorBuilder() {
}

FilteredBreakIteratorBuilder::~FilteredBreakIteratorBuilder() {
}

FilteredBreakIteratorBuilder *
FilteredBreakIteratorBuilder::createInstance(const Locale& where, UErrorCode& status) {
  if(U_FAILURE(status)) return NULL;

  LocalPointer<FilteredBreakIteratorBuilder> ret(new SimpleFilteredBreakIteratorBuilder(where, status));
  if(!ret.isValid()) status = U_MEMORY_ALLOCATION_ERROR;
  return ret.orphan();
}

FilteredBreakIteratorBuilder *
FilteredBreakIteratorBuilder::createInstance(UErrorCode& status) {
  if(U_FAILURE(status)) return NULL;

  LocalPointer<FilteredBreakIteratorBuilder> ret(new SimpleFilteredBreakIteratorBuilder());
  if(!ret.isValid()) status = U_MEMORY_ALLOCATION_ERROR;
  return ret.orphan();
}

U_NAMESPACE_END

#endif //#if !UCONFIG_NO_BREAK_ITERATION && U_HAVE_STD_STRING && !UCONFIG_NO_FILTERED_BREAK_ITERATION
