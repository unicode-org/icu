/**
 *******************************************************************************
 * Copyright (C) 1996-2005, international Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.CollectionUtilities.MultiComparator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/** Provides more flexible formatting of UnicodeSet patterns.
 */
public class PrettyPrinter {
	private static UnicodeSet patternWhitespace = new UnicodeSet("[[:Cn:][:Default_Ignorable_Code_Point:][:patternwhitespace:]]");
	
	private boolean first = true;
	private StringBuffer target = new StringBuffer();
	private int firstCodePoint = -2;
	private int lastCodePoint = -2;
	private boolean compressRanges = true;
	private String lastString = "";
	private UnicodeSet toQuote = new UnicodeSet(patternWhitespace);
	private Transliterator quoter = null;
	
	private Comparator ordering;
	private Comparator spaceComp = Collator.getInstance(ULocale.ROOT);
	{
		setOrdering(Collator.getInstance(ULocale.ROOT));
		((RuleBasedCollator)spaceComp).setStrength(RuleBasedCollator.PRIMARY);
	}
	
	public Transliterator getQuoter() {
		return quoter;
	}

	public PrettyPrinter setQuoter(Transliterator quoter) {
		this.quoter = quoter;
		return this; // for chaining
	}

	public boolean isCompressRanges() {
		return compressRanges;
	}
	
	/**
	 * @param compressRanges if you want abcde instead of a-e, make this false
	 * @return
	 */
	public PrettyPrinter setCompressRanges(boolean compressRanges) {
		this.compressRanges = compressRanges;
		return this;
	}
	
	public Comparator getOrdering() {
		return ordering;
	}
	
	/**
	 * @param ordering the resulting  ordering of the list of characters in the pattern
	 * @return
	 */
	public PrettyPrinter setOrdering(Comparator ordering) {
		this.ordering = new MultiComparator(new Comparator[] {ordering, new UTF16.StringComparator(true,false,0)});
		return this;
	}
	
	public Comparator getSpaceComparator() {
		return spaceComp;
	}
	
	/**
	 * @param spaceComp if the comparison returns non-zero, then a space will be inserted between characters
	 * @return this, for chaining
	 */
	public PrettyPrinter setSpaceComparator(Comparator spaceComp) {
		this.spaceComp = spaceComp;
		return this;
	}
	
	public UnicodeSet getToQuote() {
		return toQuote;
	}
	
	/**
	 * a UnicodeSet of extra characters to quote with \\uXXXX-style escaping (will automatically quote pattern whitespace)
	 * @param toQuote
	 */
	public PrettyPrinter setToQuote(UnicodeSet toQuote) {
		toQuote = (UnicodeSet)toQuote.clone();
		toQuote.addAll(patternWhitespace);
		this.toQuote = toQuote;
		return this;
	}
		
	/**
	 * Get the pattern for a particular set.
	 * @param uset
	 * @return formatted UnicodeSet
	 */
	public String toPattern(UnicodeSet uset) {
		first = true;
		// make sure that comparison separates all strings, even canonically equivalent ones
		Set orderedStrings = new TreeSet(ordering);
		for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
			orderedStrings.add(it.getString());
		}
		target.setLength(0);
		target.append("[");
		for (Iterator it = orderedStrings.iterator(); it.hasNext();) {
			appendUnicodeSetItem((String) it.next());
		}
		flushLast();
		target.append("]");
		String sresult = target.toString();
		UnicodeSet doubleCheck = new UnicodeSet(sresult);
		if (!uset.equals(doubleCheck)) {
			throw new InternalError("Failure to round-trip in pretty-print");
		}
		return sresult;
	}
	
	PrettyPrinter appendUnicodeSetItem(String s) {
		int cp;
		if (UTF16.hasMoreCodePointsThan(s, 1)) {
			flushLast();
			addSpace(s);
			target.append("{");
			for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
				appendQuoted(cp = UTF16.charAt(s, i));
			}
			target.append("}");
			lastString = s;
		} else {
			if (!compressRanges)
				flushLast();
			cp = UTF16.charAt(s, 0);
			if (cp == lastCodePoint + 1) {
				lastCodePoint = cp; // continue range
			} else { // start range
				flushLast();
				firstCodePoint = lastCodePoint = cp;
			}
		}
		return this;
	}
	/**
	 * 
	 */
	private void addSpace(String s) {
		if (first) {
			first = false;
		} else if (spaceComp.compare(s, lastString) != 0) {
			target.append(' ');
		} else {
			int type = UCharacter.getType(UTF16.charAt(s,0));
			if (type == UCharacter.NON_SPACING_MARK || type == UCharacter.ENCLOSING_MARK) {
				target.append(' ');
			}
		}
	}
	
	private void flushLast() {
		if (lastCodePoint >= 0) {
			addSpace(UTF16.valueOf(firstCodePoint));
			if (firstCodePoint != lastCodePoint) {
				appendQuoted(firstCodePoint);
				target.append(firstCodePoint + 1 == lastCodePoint ? ' ' : '-');
			}
			appendQuoted(lastCodePoint);
			lastString = UTF16.valueOf(lastCodePoint);
			firstCodePoint = lastCodePoint = -2;
		}
	}
	PrettyPrinter appendQuoted(int codePoint) {
		if (toQuote.contains(codePoint)) {
			if (quoter != null) {
				target.append(quoter.transliterate(UTF16.valueOf(codePoint)));
				return this;
			}
			if (codePoint > 0xFFFF) {
				target.append("\\U");
				target.append(Utility.hex(codePoint,8));
			} else {
				target.append("\\u");
				target.append(Utility.hex(codePoint,4));        			
			}
			return this;
		}
		switch (codePoint) {
		case '[': // SET_OPEN:
		case ']': // SET_CLOSE:
		case '-': // HYPHEN:
		case '^': // COMPLEMENT:
		case '&': // INTERSECTION:
		case '\\': //BACKSLASH:
		case '{':
		case '}':
		case '$':
		case ':':
			target.append('\\');
			break;
		default:
			// Escape whitespace
			if (patternWhitespace.contains(codePoint)) {
				target.append('\\');
			}
		break;
		}
		UTF16.append(target, codePoint);
		return this;
	}        
//	Appender append(String s) {
//	target.append(s);
//	return this;
//	}
//	public String toString() {
//	return target.toString();
//	}
}