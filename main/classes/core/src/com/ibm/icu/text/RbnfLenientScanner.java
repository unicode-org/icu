/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

/**
 * Used by RBNF to leniently parse a string.
 *
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 */
public interface RbnfLenientScanner {
  /**
   * Return true if a string consists entirely of ignorable
   * characters.
   * @param s The string to test
   * @return true if the string is empty or consists entirely of
   * characters that are ignorable.
   * @draft ICU 4.4
   * @provisional This API might change or be removed in a future release.
   */
  boolean allIgnorable(String s);

  /**
   * Match characters in a string against a prefix and return
   * the number of chars that matched, or 0 if no match.  Only
   * primary-order differences are significant in determining
   * whether there's a match.  This means that the returned
   * value need not be the same as the length of the prefix.
   *
   * @param str The string being tested
   * @param prefix The text we're hoping to see at the beginning of "str"
   * @return the number of characters in "str" that were matched
   * @draft ICU 4.4
   * @provisional This API might change or be removed in a future release.
   */
  public int prefixLength(String str, String prefix);

  int[] findText(String str, String key, int startingAt);
}