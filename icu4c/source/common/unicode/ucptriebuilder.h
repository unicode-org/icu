// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// ucptriebuilder.h (split out of ucptrie.h)
// created: 2018jan24 Markus W. Scherer

#ifndef __UCPTRIEBUILDER_H__
#define __UCPTRIEBUILDER_H__

#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/ucptrie.h"
#include "unicode/utf8.h"
#include "putilimp.h"
#include "udataswp.h"

U_CDECL_BEGIN

/**
 * \file
 *
 * This file defines a builder for Unicode code point tries.
 *
 * @see UCPTrie
 * @see UCPTrieBuilder
 */

/**
 * Unicode code point trie builder.
 * Fast map from Unicode code points (U+0000..U+10FFFF) to 32-bit integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * Setting values (especially ranges) and lookup is fast.
 * The builder is only somewhat space-efficient.
 * It builds a compacted, immutable UCPTrie.
 *
 * @see UCPTrie
 * @see ucptriebld_build
 * @draft ICU 62
 */
struct UCPTrieBuilder;
typedef struct UCPTrieBuilder UCPTrieBuilder;

/**
 * Creates a trie builder that initially maps each Unicode code point to the same value.
 * At build time, 32-bit data values are used.
 * ucptriebld_build() takes a valueBits parameter which
 * determines the number of bits in the data value in the resulting UCPTrie.
 * You must ucptriebld_close() the trie builder once you are done using it.
 *
 * @param initialValue the initial value that is set for all code points
 * @param errorValue the value for out-of-range code points and ill-formed UTF-8/16
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the builder
 * @draft ICU 62
 */
U_CAPI UCPTrieBuilder * U_EXPORT2
ucptriebld_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode);

/**
 * Clones a trie builder.
 * You must ucptriebld_close() the clone once you are done using it.
 *
 * @param other the builder to clone
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the builder clone
 * @draft ICU 62
 */
U_CAPI UCPTrieBuilder * U_EXPORT2
ucptriebld_clone(const UCPTrieBuilder *other, UErrorCode *pErrorCode);

/**
 * Closes a trie builder and releases associated memory.
 *
 * @param builder the builder
 * @draft ICU 62
 */
U_CAPI void U_EXPORT2
ucptriebld_close(UCPTrieBuilder *builder);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalUCPTrieBuilderPointer
 * "Smart pointer" class, closes a UCPTrieBuilder via ucptriebld_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 62
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUCPTrieBuilderPointer, UCPTrieBuilder, ucptriebld_close);

U_NAMESPACE_END

#endif

/**
 * Creates a trie builder with the same contents as the input trie.
 * You must ucptriebld_close() the builder once you are done using it.
 *
 * @param trie the trie to clone
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the builder
 * @draft ICU 62
 */
U_CAPI UCPTrieBuilder * U_EXPORT2
ucptriebld_fromUCPTrie(const UCPTrie *trie, UErrorCode *pErrorCode);

/**
 * Returns the value for a code point as stored in the builder.
 *
 * @param builder the builder
 * @param c the code point
 * @return the value
 * @draft ICU 62
 */
U_CAPI uint32_t U_EXPORT2
ucptriebld_get(const UCPTrieBuilder *builder, UChar32 c);

/**
 * Returns the last code point such that all those from start to there have the same value.
 * Can be used to efficiently iterate over all same-value ranges in a trie builder.
 * The builder can be modified between calls to this function.
 *
 * If the UCPTrieHandleValue function pointer is not NULL, then
 * the value to be delivered is passed through that function, and the return value is the end
 * of the range where all values are modified to the same actual value.
 * The value is unchanged if that function pointer is NULL.
 *
 * See the same-signature ucptrie_getRange() for a code sample.
 *
 * @param builder the builder
 * @param start range start
 * @param handleValue a pointer to a function that may modify the builder data value,
 *     or NULL if the values from the builder are to be used unmodified
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     may have been modified by handleValue(context, builder value)
 *     if that function pointer is not NULL
 * @return the range end code point, or -1 if start is not a valid code point
 * @draft ICU 62
 */
U_CAPI UChar32 U_EXPORT2
ucptriebld_getRange(const UCPTrieBuilder *builder, UChar32 start,
                    UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue);

/**
 * Sets a value for a code point.
 *
 * @param builder the builder
 * @param c the code point
 * @param value the value
 * @param pErrorCode an in/out ICU UErrorCode
 * @draft ICU 62
 */
U_CAPI void U_EXPORT2
ucptriebld_set(UCPTrieBuilder *builder, UChar32 c, uint32_t value, UErrorCode *pErrorCode);

/**
 * Sets a value for each code point [start..end].
 * Faster and more space-efficient than setting the value for each code point separately.
 *
 * @param builder the builder
 * @param start the first code point to get the value
 * @param end the last code point to get the value (inclusive)
 * @param value the value
 * @param pErrorCode an in/out ICU UErrorCode
 * @draft ICU 62
 */
U_CAPI void U_EXPORT2
ucptriebld_setRange(UCPTrieBuilder *builder,
                    UChar32 start, UChar32 end,
                    uint32_t value, UErrorCode *pErrorCode);

/**
 * Compacts the data and builds an immutable UCPTrie according to the parameters.
 * After this, the builder will be empty.
 *
 * Not every possible set of mappings can be built into a UCPTrie,
 * because of limitations resulting from speed and space optimizations.
 * Every assigned character can be mapped to a unique value.
 * Typical data yields data structures far smaller than the limitations.
 *
 * It is possible to construct extremely unusual mappings that exceed the data structure limits.
 * In such a case this function will fail with a U_INDEX_OUTOFBOUNDS_ERROR.
 *
 * @param builder the trie builder
 * @param type selects the trie type
 * @param valueBits selects the number of bits in a trie data value; if smaller than 32 bits, then
 *                  the values stored in the builder will be truncated first
 * @param pErrorCode an in/out ICU UErrorCode
 *
 * @see ucptriebld_fromUCPTrie
 * @draft ICU 62
 */
U_CAPI UCPTrie * U_EXPORT2
ucptriebld_build(UCPTrieBuilder *builder, UCPTrieType type, UCPTrieValueBits valueBits,
                 UErrorCode *pErrorCode);

U_CDECL_END

#endif
