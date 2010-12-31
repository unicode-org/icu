/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  udicttrie.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010dec17
*   created by: Markus W. Scherer
*/

#ifndef __UDICTTRIE_H__
#define __UDICTTRIE_H__

/**
 * \file
 * \brief C API: Helper definitions for dictionary trie APIs.
 */

#include "unicode/utypes.h"

/**
 * Return values for ByteTrie::next(), UCharTrie::next() and similar methods.
 * @see UDICTTRIE_RESULT_MATCHES
 * @see UDICTTRIE_RESULT_HAS_VALUE
 * @see UDICTTRIE_RESULT_HAS_NEXT
 */
enum UDictTrieResult {
    /**
     * The input unit(s) did not continue a matching string.
     */
    UDICTTRIE_NO_MATCH,
    /**
     * The input unit(s) continued a matching string
     * but there is no value for the string so far.
     * (It is a prefix of a longer string.)
     */
    UDICTTRIE_NO_VALUE,
    /**
     * The input unit(s) continued a matching string
     * and there is a value for the string so far.
     * This value will be returned by getValue().
     * No further input byte/unit can continue a matching string.
     */
    UDICTTRIE_HAS_FINAL_VALUE,
    /**
     * The input unit(s) continued a matching string
     * and there is a value for the string so far.
     * This value will be returned by getValue().
     * Another input byte/unit can continue a matching string.
     */
    UDICTTRIE_HAS_VALUE
};

/**
 * Same as (result!=UDICTTRIE_NO_MATCH).
 * @param result A result from ByteTrie::first(), UCharTrie::next() etc.
 * @return true if the input bytes/units so far are part of a matching string/byte sequence.
 */
#define UDICTTRIE_RESULT_MATCHES(result) ((result)!=UDICTTRIE_NO_MATCH)

/**
 * Equivalent to (result==UDICTTRIE_HAS_VALUE || result==UDICTTRIE_HAS_FINAL_VALUE) but
 * this macro evaluates result exactly once.
 * @param result A result from ByteTrie::first(), UCharTrie::next() etc.
 * @return true if there is a value for the input bytes/units so far.
 * @see ByteTrie::getValue
 * @see UCharTrie::getValue
 */
#define UDICTTRIE_RESULT_HAS_VALUE(result) ((result)>=UDICTTRIE_HAS_FINAL_VALUE)

/**
 * Equivalent to (result==UDICTTRIE_NO_VALUE || result==UDICTTRIE_HAS_VALUE) but
 * this macro evaluates result exactly once.
 * @param result A result from ByteTrie::first(), UCharTrie::next() etc.
 * @return true if another input byte/unit can continue a matching string.
 */
#define UDICTTRIE_RESULT_HAS_NEXT(result) ((result)&1)

#endif  /* __UDICTTRIE_H__ */
