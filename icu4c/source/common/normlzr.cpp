/*
 *************************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 *************************************************************************
 */

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/schriter.h"
#include "unicode/uchriter.h"
#include "unicode/normlzr.h"
#include "cmemory.h"
#include "unormimp.h"

U_CDECL_BEGIN

/*
 * This is wrapper code around a C++ CharacterIterator to
 * look like a C UCharIterator for the internal API
 * for incremental normalization.
 *
 * The UCharIterator.context field holds a pointer to the CharacterIterator.
 */

static int32_t U_CALLCONV
characterIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    return ((CharacterIterator *)(iter->context))->move(delta, (CharacterIterator::EOrigin)origin);
}

static UBool U_CALLCONV
characterIteratorHasNext(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasNext();
}

static UBool U_CALLCONV
characterIteratorHasPrevious(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasPrevious();
}

static UChar U_CALLCONV
characterIteratorCurrent(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->current();
}

static UChar U_CALLCONV
characterIteratorNext(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->nextPostInc();
}

static UChar U_CALLCONV
characterIteratorPrevious(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->previous();
}

static const UCharIterator characterIteratorWrapper={
    0, 0, 0,
    characterIteratorMove,
    characterIteratorHasNext,
    characterIteratorHasPrevious,
    characterIteratorCurrent,
    characterIteratorNext,
    characterIteratorPrevious
};

U_CDECL_END

U_NAMESPACE_BEGIN

//-------------------------------------------------------------------------
// Constructors and other boilerplate
//-------------------------------------------------------------------------

Normalizer::Normalizer(const UnicodeString& str, UNormalizationMode mode) :
    fUMode(mode), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(new StringCharacterIterator(str));
}

Normalizer::Normalizer(const UChar *str, int32_t length, UNormalizationMode mode) :
    fUMode(mode), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(new UCharCharacterIterator(str, length));
}

Normalizer::Normalizer(const CharacterIterator& iter, UNormalizationMode mode) :
    fUMode(mode), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(iter.clone());
}

// deprecated constructors

Normalizer::Normalizer(const UnicodeString& str, 
                       EMode mode) :
    fUMode(getUMode(mode)), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(new StringCharacterIterator(str));
}

Normalizer::Normalizer(const UnicodeString& str, 
                       EMode mode, 
                       int32_t options) :
    fUMode(getUMode(mode)), fOptions(options),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(new StringCharacterIterator(str));
}

Normalizer::Normalizer(const UChar *str, int32_t length, EMode mode) :
    fUMode(getUMode(mode)), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(new UCharCharacterIterator(str, length));
}

Normalizer::Normalizer(const CharacterIterator& iter, 
                       EMode mode) :
    fUMode(getUMode(mode)), fOptions(0),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(iter.clone());
}

Normalizer::Normalizer(const CharacterIterator& iter, 
                       EMode mode, 
                       int32_t options) :
    fUMode(getUMode(mode)), fOptions(options),
    currentIndex(0), nextIndex(0),
    buffer(), bufferPos(0)
{
    init(iter.clone());
}

Normalizer::Normalizer(const Normalizer &copy) :
    fUMode(copy.fUMode), fOptions(copy.fOptions),
    currentIndex(copy.nextIndex), nextIndex(copy.nextIndex),
    buffer(copy.buffer), bufferPos(copy.bufferPos)
{
    init(((CharacterIterator *)(copy.text->context))->clone());
}

static const UChar _NUL=0;

void
Normalizer::init(CharacterIterator *iter) {
    UErrorCode errorCode=U_ZERO_ERROR;

    text=new UCharIterator;
    uprv_memcpy(text, &characterIteratorWrapper, sizeof(UCharIterator));

    if(unorm_haveData(&errorCode)) {
        text->context=iter;
    } else {
        delete iter;
        text->context=new UCharCharacterIterator(&_NUL, 0);
    }
}

Normalizer::~Normalizer()
{
    delete text;
}

Normalizer* 
Normalizer::clone() const
{
    if(this!=0) {
        return new Normalizer(*this);
    } else {
        return 0;
    }
}

/**
 * Generates a hash code for this iterator.
 */
int32_t Normalizer::hashCode() const
{
    return ((CharacterIterator *)(text->context))->hashCode() + fUMode + fOptions + buffer.hashCode() + bufferPos + currentIndex + nextIndex;
}
    
UBool Normalizer::operator==(const Normalizer& that) const
{
    return
        this==&that ||
        fUMode==that.fUMode &&
        fOptions==that.fOptions &&
        *((CharacterIterator *)(text->context))==*((CharacterIterator *)(that.text->context)) &&
        buffer==that.buffer &&
        bufferPos==that.bufferPos &&
        nextIndex==that.nextIndex;
}

//-------------------------------------------------------------------------
// Static utility methods
//-------------------------------------------------------------------------

void 
Normalizer::normalize(const UnicodeString& source, 
                      UNormalizationMode mode, int32_t options,
                      UnicodeString& result, 
                      UErrorCode &status) {
    if(source.isBogus() || U_FAILURE(status)) {
        result.setToBogus();
    } else {
        UChar *buffer=result.getBuffer(source.length());
        int32_t length=unorm_internalNormalize(buffer, result.getCapacity(),
                                               source.getBuffer(), source.length(),
                                               mode, (options&IGNORE_HANGUL)!=0,
                                               &status);
        result.releaseBuffer(length);
        if(status==U_BUFFER_OVERFLOW_ERROR) {
            status=U_ZERO_ERROR;
            buffer=result.getBuffer(length);
            length=unorm_internalNormalize(buffer, result.getCapacity(),
                                           source.getBuffer(), source.length(),
                                           mode, (options&IGNORE_HANGUL)!=0,
                                           &status);
            result.releaseBuffer(length);
        }

        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

UNormalizationCheckResult
Normalizer::quickCheck(const UnicodeString& source,
                       UNormalizationMode mode, 
                       UErrorCode &status) {
    if(U_FAILURE(status)) {
        return UNORM_MAYBE;
    }

    return unorm_quickCheck(source.getBuffer(), source.length(),
                            mode, &status);
}

void
Normalizer::compose(const UnicodeString& source, 
                    UBool compat, int32_t options,
                    UnicodeString& result, 
                    UErrorCode &status) {
    if(source.isBogus() || U_FAILURE(status)) {
        result.setToBogus();
    } else {
        UChar *buffer=result.getBuffer(source.length());
        int32_t length=unorm_compose(buffer, result.getCapacity(),
                                     source.getBuffer(), source.length(),
                                     compat, (options&IGNORE_HANGUL)!=0,
                                     &status);
        result.releaseBuffer(length);
        if(status==U_BUFFER_OVERFLOW_ERROR) {
            status=U_ZERO_ERROR;
            buffer=result.getBuffer(length);
            length=unorm_compose(buffer, result.getCapacity(),
                                 source.getBuffer(), source.length(),
                                 compat, (options&IGNORE_HANGUL)!=0,
                                 &status);
            result.releaseBuffer(length);
        }

        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

void
Normalizer::decompose(const UnicodeString& source, 
                      UBool compat, int32_t options,
                      UnicodeString& result, 
                      UErrorCode &status) {
    if(source.isBogus() || U_FAILURE(status)) {
        result.setToBogus();
    } else {
        UChar *buffer=result.getBuffer(source.length());
        int32_t length=unorm_compose(buffer, result.getCapacity(),
                                     source.getBuffer(), source.length(),
                                     compat, (options&IGNORE_HANGUL)!=0,
                                     &status);
        result.releaseBuffer(length);
        if(status==U_BUFFER_OVERFLOW_ERROR) {
            status=U_ZERO_ERROR;
            buffer=result.getBuffer(length);
            length=unorm_decompose(buffer, result.getCapacity(),
                                   source.getBuffer(), source.length(),
                                   compat, (options&IGNORE_HANGUL)!=0,
                                   &status);
            result.releaseBuffer(length);
        }

        if(U_FAILURE(status)) {
            result.setToBogus();
        }
    }
}

//-------------------------------------------------------------------------
// Iteration API
//-------------------------------------------------------------------------

/**
 * Return the current character in the normalized text->
 */
UChar32 Normalizer::current() {
    if(bufferPos<buffer.length() || nextNormalize()) {
        return buffer.char32At(bufferPos);
    } else {
        return DONE;
    }
}

/**
 * Return the next character in the normalized text and advance
 * the iteration position by one.  If the end
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar32 Normalizer::next() {
    if(bufferPos<buffer.length() ||  nextNormalize()) {
        UChar32 c=buffer.char32At(bufferPos);
        bufferPos+=UTF_CHAR_LENGTH(c);
        return c;
    } else {
        return DONE;
    }
}

/**
 * Return the previous character in the normalized text and decrement
 * the iteration position by one.  If the beginning
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar32 Normalizer::previous() {
    if(bufferPos>0 || previousNormalize()) {
        UChar32 c=buffer.char32At(bufferPos-1);
        bufferPos-=UTF_CHAR_LENGTH(c);
        return c;
    } else {
        return DONE;
    }
}

void Normalizer::reset() {
    currentIndex=nextIndex=text->move(text, 0, UITERATOR_START);
    clearBuffer();
}

void
Normalizer::setIndexOnly(UTextOffset index) {
    currentIndex=nextIndex=text->move(text, index, UITERATOR_START); // validates index
    clearBuffer();
}

/**
 * Set the iteration position in the input text that is being normalized
 * and return the first normalized character at that position.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em> text,
 * while {@link #next} and {@link #previous} iterate through characters
 * in the normalized <em>output</em>.  This means that there is not
 * necessarily a one-to-one correspondence between characters returned
 * by <tt>next</tt> and <tt>previous</tt> and the indices passed to and
 * returned from <tt>setIndex</tt> and {@link #getIndex}.
 * <p>
 * @param index the desired index in the input text->
 *
 * @return      the first normalized character that is the result of iterating
 *              forward starting at the given index.
 *
 * @throws IllegalArgumentException if the given index is less than
 *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
 */
UChar32 Normalizer::setIndex(UTextOffset index) {
    setIndexOnly(index);
    return current();
}

/**
 * Return the first character in the normalized text->  This resets
 * the <tt>Normalizer's</tt> position to the beginning of the text->
 */
UChar32 Normalizer::first() {
    reset();
    return next();
}

/**
 * Return the last character in the normalized text->  This resets
 * the <tt>Normalizer's</tt> position to be just before the
 * the input text corresponding to that normalized character.
 */
UChar32 Normalizer::last() {
    currentIndex=nextIndex=text->move(text, 0, UITERATOR_END);
    clearBuffer();
    return previous();
}

/**
 * Retrieve the current iteration position in the input text that is
 * being normalized.  This method is useful in applications such as
 * searching, where you need to be able to determine the position in
 * the input text that corresponds to a given normalized output character.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em>, while
 * {@link #next} and {@link #previous} iterate through characters in the
 * <em>output</em>.  This means that there is not necessarily a one-to-one
 * correspondence between characters returned by <tt>next</tt> and
 * <tt>previous</tt> and the indices passed to and returned from
 * <tt>setIndex</tt> and {@link #getIndex}.
 *
 */
UTextOffset Normalizer::getIndex() const {
    if(bufferPos<buffer.length()) {
        return currentIndex;
    } else {
        return nextIndex;
    }
}

/**
 * Retrieve the index of the start of the input text->  This is the begin index
 * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::startIndex() const {
    return text->move(text, 0, UITERATOR_START);
}

/**
 * Retrieve the index of the end of the input text->  This is the end index
 * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::endIndex() const {
    return text->move(text, 0, UITERATOR_END);
}

//-------------------------------------------------------------------------
// Property access methods
//-------------------------------------------------------------------------

void
Normalizer::setMode(UNormalizationMode newMode) 
{
    fUMode = newMode;
}

UNormalizationMode
Normalizer::getUMode() const
{
    return fUMode;
}

void
Normalizer::setOption(int32_t option, 
                      UBool value) 
{
    if (value) {
        fOptions |= option;
    } else {
        fOptions &= (~option);
    }
}

UBool
Normalizer::getOption(int32_t option) const
{
    return (fOptions & option) != 0;
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the input text->
 */
void
Normalizer::setText(const UnicodeString& newText, 
                    UErrorCode &status)
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = new StringCharacterIterator(newText);
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete (CharacterIterator *)(text->context);
    text->context = newIter;
    reset();
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the string.
 */
void
Normalizer::setText(const CharacterIterator& newText, 
                    UErrorCode &status) 
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = newText.clone();
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete (CharacterIterator *)(text->context);
    text->context = newIter;
    reset();
}

void
Normalizer::setText(const UChar* newText,
                    int32_t length,
                    UErrorCode &status)
{
    if (U_FAILURE(status)) {
        return;
    }
    CharacterIterator *newIter = new UCharCharacterIterator(newText, length);
    if (newIter == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    delete (CharacterIterator *)(text->context);
    text->context = newIter;
    reset();
}

/**
 * Copies the text under iteration into the UnicodeString referred to by "result".
 * @param result Receives a copy of the text under iteration.
 */
void
Normalizer::getText(UnicodeString&  result) 
{
    ((CharacterIterator *)(text->context))->getText(result);
}

//-------------------------------------------------------------------------
// Private utility methods
//-------------------------------------------------------------------------

void Normalizer::clearBuffer() {
    buffer.remove();
    bufferPos=0;
}

UBool
Normalizer::nextNormalize() {
    UChar *p;
    int32_t length;
    UErrorCode errorCode;

    clearBuffer();
    currentIndex=nextIndex;
    text->move(text, nextIndex, UITERATOR_START);
    if(!text->hasNext(text)) {
        return FALSE;
    }

    errorCode=U_ZERO_ERROR;
    p=buffer.getBuffer(-1);
    length=unorm_nextNormalize(p, buffer.getCapacity(), text,
                               fUMode, (fOptions&IGNORE_HANGUL)!=0,
                               &errorCode);
    buffer.releaseBuffer(length);
    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        errorCode=U_ZERO_ERROR;
        text->move(text, nextIndex, UITERATOR_START);
        p=buffer.getBuffer(length);
        length=unorm_nextNormalize(p, buffer.getCapacity(), text,
                                   fUMode, (fOptions&IGNORE_HANGUL)!=0,
                                   &errorCode);
        buffer.releaseBuffer(length);
    }

    nextIndex=text->move(text, 0, UITERATOR_CURRENT);
    return U_SUCCESS(errorCode) && !buffer.isEmpty();
}

UBool
Normalizer::previousNormalize() {
    UChar *p;
    int32_t length;
    UErrorCode errorCode;

    clearBuffer();
    nextIndex=currentIndex;
    text->move(text, currentIndex, UITERATOR_START);
    if(!text->hasPrevious(text)) {
        return FALSE;
    }

    errorCode=U_ZERO_ERROR;
    p=buffer.getBuffer(-1);
    length=unorm_previousNormalize(p, buffer.getCapacity(), text,
                                   fUMode, (fOptions&IGNORE_HANGUL)!=0,
                                   &errorCode);
    buffer.releaseBuffer(length);
    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        errorCode=U_ZERO_ERROR;
        text->move(text, currentIndex, UITERATOR_START);
        p=buffer.getBuffer(length);
        length=unorm_previousNormalize(p, buffer.getCapacity(), text,
                                       fUMode, (fOptions&IGNORE_HANGUL)!=0,
                                       &errorCode);
        buffer.releaseBuffer(length);
    }

    bufferPos=buffer.length();
    currentIndex=text->move(text, 0, UITERATOR_CURRENT);
    return U_SUCCESS(errorCode) && !buffer.isEmpty();
}

U_NAMESPACE_END
