/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RuleBasedCollator.java,v $ 
* $Date: 2002/05/14 16:48:49 $ 
* $Revision: 1.4 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.CharacterIterator;
import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.impl.Trie;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.UCharacterIterator;

/**
* <p>The RuleBasedCollator class is a concrete subclass of Collator that 
* provides a simple, data-driven, table collator. With this class you can 
* create a customized table-based Collator. RuleBasedCollator maps characters 
* to sort keys.</p>
* <p>RuleBasedCollator has the following restrictions for efficiency (other 
* subclasses may be used for more complex languages) : 
* <ol>
* <li>If a special collation rule controlled by a &lt;modifier&gt; is
*     specified it applies to the whole collator object.
* <li>All non-mentioned characters are at the end of the collation order.
* </ol>
* </p>
* <p>The collation table is composed of a list of collation rules, where each 
* rule is of three forms: 
*     <pre>
*    &lt;modifier&gt;
*    &lt;relation&gt; &lt;text-argument&gt;
*    &lt;reset&gt; &lt;text-argument&gt;
* </pre>
* </p>
* <p>The definitions of the rule elements is as follows:
* <UL Type=disc>
*    <LI><strong>Text-Argument</strong>: A text-argument is any sequence of
*        characters, excluding special characters (that is, common
*        whitespace characters [0009-000D, 0020] and rule syntax characters
*        [0021-002F, 003A-0040, 005B-0060, 007B-007E]). If those
*        characters are desired, you can put them in single quotes
*        (e.g. ampersand => '&'). Note that unquoted white space characters
*        are ignored; e.g. <code>b c</code> is treated as <code>bc</code>.
*    <LI><strong>Modifier</strong>: There are currently two modifiers that 
*        turn on special collation rules.
*        <UL Type=square>
*            <LI>'@' : Turns on backwards sorting of accents (secondary
*                      differences), as in French.
*            <LI>'!' : Turns on Thai/Lao vowel-consonant swapping.  If this
*                      rule is in force when a Thai vowel of the range
*                      &#92;U0E40-&#92;U0E44 precedes a Thai consonant of the 
*                      range &#92;U0E01-&#92;U0E2E OR a Lao vowel of the range 
*                      &#92;U0EC0-&#92;U0EC4 precedes a Lao consonant of the 
*                      range &#92;U0E81-&#92;U0EAE then the vowel is placed 
*                      after the consonant for collation purposes.
*        </UL>
*        <p>'@' : Indicates that accents are sorted backwards, as in French.
*    <LI><strong>Relation</strong>: The relations are the following:
*        <UL Type=square>
*            <LI>'&lt;' : Greater, as a letter difference (primary)
*            <LI>';' : Greater, as an accent difference (secondary)
*            <LI>',' : Greater, as a case difference (tertiary)
*            <LI>'=' : Equal
*        </UL>
*    <LI><strong>Reset</strong>: There is a single reset
*        which is used primarily for contractions and expansions, but which
*        can also be used to add a modification at the end of a set of rules.
*        <p>'&' : Indicates that the next rule follows the position to where
*            the reset text-argument would be sorted.
* </UL>
* </p>
* <p>
* This sounds more complicated than it is in practice. For example, the
* following are equivalent ways of expressing the same thing:
* <blockquote>
* <pre>
* a &lt; b &lt; c
* a &lt; b &amp; b &lt; c
* a &lt; c &amp; a &lt; b
* </pre>
* </blockquote>
* Notice that the order is important, as the subsequent item goes immediately
* after the text-argument. The following are not equivalent:
* <blockquote>
* <pre>
* a &lt; b &amp; a &lt; c
* a &lt; c &amp; a &lt; b
* </pre>
* </blockquote>
* Either the text-argument must already be present in the sequence, or some
* initial substring of the text-argument must be present. 
* (e.g. "a &lt; b &amp; ae &lt; e" is valid since "a" is present in the 
* sequence before "ae" is reset). In this latter case, "ae" is not entered and 
* treated as a single character; instead, "e" is sorted as if it were expanded 
* to two characters: "a" followed by an "e". This difference appears in 
* natural languages: in traditional Spanish "ch" is treated as though it 
* contracts to a single character (expressed as "c &lt; ch &lt; d"), while in 
* traditional German a-umlaut is treated as though it expanded to two 
* characters (expressed as 
* "a,A &lt; b,B ... &amp;ae;&#92;u00e3&amp;AE;&#92;u00c3").
* [&#92;u00e3 and &#92;u00c3 are, of course, the escape sequences for 
* a-umlaut.]
* </p>
* <p>
* <strong>Ignorable Characters</strong>
* <p>
* For ignorable characters, the first rule must start with a relation (the
* examples we have used above are really fragments; "a &lt; b" really should 
* be "&lt; a &lt; b"). If, however, the first relation is not "&lt;", then all 
* the all text-arguments up to the first "&lt;" are ignorable. For example, 
* ", - &lt; a &lt; b" makes "-" an ignorable character, as we saw earlier in 
* the word "black-birds". In the samples for different languages, you see that 
* most accents are ignorable.</p>
* <p><strong>Normalization and Accents</strong>
* <p><code>RuleBasedCollator</code> automatically processes its rule table to
* include both pre-composed and combining-character versions of accented 
* characters. Even if the provided rule string contains only base characters 
* and separate combining accent characters, the pre-composed accented 
* characters matching all canonical combinations of characters from the rule 
* string will be entered in the table.</p>
* <p>This allows you to use a RuleBasedCollator to compare accented strings
* even when the collator is set to NO_DECOMPOSITION. There are two caveats,
* however. First, if the strings to be collated contain combining sequences 
* that may not be in canonical order, you should set the collator to 
* CANONICAL_DECOMPOSITION or FULL_DECOMPOSITION to enable sorting of combining 
* sequences. Second, if the strings contain characters with compatibility 
* decompositions (such as full-width and half-width forms), you must use 
* FULL_DECOMPOSITION, since the rule tables only include canonical mappings.
* </p>
* <p><strong>Errors</strong></p>
* <p>The following are errors:</p>
* <UL Type=disc>
*     <LI>A text-argument contains unquoted punctuation symbols
*         (e.g. "a &lt; b-c &lt; d").
*     <LI>A relation or reset character not followed by a text-argument
*         (e.g. "a &lt; ,b").
*     <LI>A reset where the text-argument (or an initial substring of the
*         text-argument) is not already in the sequence.
*         (e.g. "a &lt; b &amp; e &lt; f")
* </UL>
* <p>If you produce one of these errors, a <code>RuleBasedCollator</code> 
* throws a <code>ParseException</code>.</p>
* <p><strong>Examples</strong></p>
* <p>Simple:     "&lt; a &lt; b &lt; c &lt; d"</p>
* <p>Norwegian:  "&lt; a,A&lt; b,B&lt; c,C&lt; d,D&lt; e,E&lt; f,F&lt; " +
*                "g,G&lt; h,H&lt; i,I&lt; j,J&lt; k,K&lt; l,L&lt; m,M&lt; " +
*                "n,N&lt; o,O&lt; p,P&lt; q,Q&lt; r,R&lt; s,S&lt; t,T&lt; " +
*                "u,U&lt; v,V&lt; w,W&lt; x,X&lt; y,Y&lt; z,Z&lt; " +
*                "&#92;u00E5=a&#92; u030A,&#92;u00C5=A&#92;u030A;aa,AA&lt; " +
*                "&#92;u00E6,&#92; u00C6&lt; &#92;u00F8,&#92;u00D8"</p>
* <p>Normally, to create a rule-based Collator object, you will use
* <code>Collator</code>'s factory method <code>getInstance</code>. However, to 
* create a rule-based Collator object with specialized rules tailored to your 
* needs, you construct the <code>RuleBasedCollator</code> with the rules 
* contained in a <code>String</code> object. For example:</p>
* <blockquote>
* <pre>
* String Simple = "&lt; a&lt; b&lt; c&lt; d";
* RuleBasedCollator mySimple = new RuleBasedCollator(Simple);
* </pre>
* </blockquote>
* Or:
* <blockquote>
* <pre>
* String Norwegian = "&lt; a,A&lt; b,B&lt; c,C&lt; d,D&lt; e,E&lt; f,F&lt;" +  
*                    "g,G&lt; h,H&lt; i,I&lt; j,J &lt; k,K&lt; l,L&lt; " +
*                    "m,M&lt; n,N&lt; o,O&lt; p,P&lt; q,Q&lt; r,R&lt; " +
*                    "s,S&lt; t,T &lt; u,U&lt; v,V&lt; w,W&lt; x,X&lt; " +
*                    "y,Y&lt; z,Z &lt; &#92;u00E5=a&#92;u030A," +
*                    "&#92;u00C5=A&#92;u030A;aa,AA&lt; &#92;u00E6," +
*                    "&#92;u00C6&lt; &#92;u00F8,&#92;u00D8";
* RuleBasedCollator myNorwegian = new RuleBasedCollator(Norwegian);
* </pre>
* </blockquote>
* <p>Combining <code>Collator</code>s is as simple as concatenating strings.
* Here's an example that combines two <code>Collator</code>s from two
* different locales:</p>
* <blockquote>
* <pre>
* // Create an en_US Collator object
* RuleBasedCollator en_USCollator = (RuleBasedCollator)
*     Collator.getInstance(new Locale("en", "US", ""));
* // Create a da_DK Collator object
* RuleBasedCollator da_DKCollator = (RuleBasedCollator)
*     Collator.getInstance(new Locale("da", "DK", ""));
* // Combine the two
* // First, get the collation rules from en_USCollator
* String en_USRules = en_USCollator.getRules();
* // Second, get the collation rules from da_DKCollator
* String da_DKRules = da_DKCollator.getRules();
* RuleBasedCollator newCollator =
*     new RuleBasedCollator(en_USRules + da_DKRules);
* // newCollator has the combined rules
* </pre>
* </blockquote>
* <p>Another more interesting example would be to make changes on an existing
* table to create a new <code>Collator</code> object. For example, add
* "&amp;C&lt; ch, cH, Ch, CH" to the <code>en_USCollator</code> object to 
* create your own:</p>
* <blockquote>
* <pre>
* // Create a new Collator object with additional rules
* String addRules = "&amp;C&lt; ch, cH, Ch, CH";
* RuleBasedCollator myCollator =
*     new RuleBasedCollator(en_USCollator + addRules);
* // myCollator contains the new rules
* </pre>
* </blockquote>
* <p>The following example demonstrates how to change the order of
* non-spacing accents,
* <blockquote>
* <pre>
* // old rule
* String oldRules = 
*     "=&#92;u0301;&#92;u0300;&#92;u0302;&#92;u0308"    // main accents
*     + ";&#92;u0327;&#92;u0303;&#92;u0304;&#92;u0305"    // main accents
*     + ";&#92;u0306;&#92;u0307;&#92;u0309;&#92;u030A"    // main accents
*     + ";&#92;u030B;&#92;u030C;&#92;u030D;&#92;u030E"    // main accents
*     + ";&#92;u030F;&#92;u0310;&#92;u0311;&#92;u0312"    // main accents
*     + "&lt; a , A ; ae, AE ; &#92;u00e6 , &#92;u00c6"
*     + "&lt; b , B &lt; c, C &lt; e, E & C &lt; d, D";
* // change the order of accent characters
* String addOn = "& &#92;u0300 ; &#92;u0308 ; &#92;u0302";
* RuleBasedCollator myCollator = new RuleBasedCollator(oldRules + addOn);
* </pre>
* </blockquote>
* <p>The last example shows how to put new primary ordering in before the
* default setting. For example, in Japanese <code>Collator</code>, you
* can either sort English characters before or after Japanese characters,
* <blockquote>
* <pre>
* // get en_US Collator rules
* RuleBasedCollator en_USCollator = (RuleBasedCollator)
*                                             Collator.getInstance(Locale.US);
* // add a few Japanese character to sort before English characters
* // suppose the last character before the first base letter 'a' in
* // the English collation rule is &#92;u2212
* String jaString = "& &#92;u2212 &lt; &#92;u3041, &#92;u3042 &lt; &#92;u3043, &#92;u3044";
* RuleBasedCollator myJapaneseCollator = new
*     RuleBasedCollator(en_USCollator.getRules() + jaString);
* </pre>
* @author Syn Wee Quek
* @since release 2.2, April 18 2002
* @draft 2.2
*/
public class RuleBasedCollator extends Collator implements Trie.DataManipulate
{     
	// public data members ---------------------------------------------------
	
	// public constructors ---------------------------------------------------
	
	/**
     * <p>RuleBasedCollator constructor that takes the rules. 
     * Please see RuleBasedCollator class description for more details on the 
     * collation rule syntax.</p>
     * <p>Note different from Java, does not throw a ParseException</p>
     * @see java.util.Locale
     * @param rules the collation rules to build the collation table from.
     * @exception Exception thrown when there's an error creating the collator
     * @draft 2.2
     */
    public RuleBasedCollator(String rules) throws Exception
    {
    	setStrength(Collator.TERTIARY);
        setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        m_rules_ = rules;
        // tables = new RBCollationTables(rules, decomp);
        // init();
    }
    
	// public methods --------------------------------------------------------
    
    /**
     * Return a CollationElementIterator for the given String.
     * @see CollationElementIterator
     * @draft 2.2
     */
    public CollationElementIterator getCollationElementIterator(String source) {
        return new CollationElementIterator(source, this);
    }

    /**
     * Return a CollationElementIterator for the given String.
     * @see CollationElementIterator
     * @draft 2.2
     */
    public CollationElementIterator getCollationElementIterator(
                                                CharacterIterator source) {
        return new CollationElementIterator(source, this);
    }
    
    // public setters --------------------------------------------------------
    
    /**
	 * Sets the Hiragana Quartenary sort to be on or off
	 * @param flag true if Hiragana Quartenary sort is to be on, false 
	 *        otherwise
	 * @draft 2.2
	 */
	public synchronized void setHiraganaQuartenary(boolean flag)
	{
		m_isHiragana4_ = flag;
	}
	
	/**
	 * Sets the Hiragana Quartenary sort to be on or off depending on the 
	 * Collator's locale specific default value.
	 * @draft 2.2
	 */
	public synchronized void setHiraganaQuartenaryDefault()
	{
		m_isHiragana4_ = m_defaultIsHiragana4_;
	}
	
	/**
   	 * Sets the Collator to sort with the indicated casing first
   	 * @param upper true for sorting uppercased characters before lowercased 
   	 *              characters, false for sorting lowercased characters before
   	 *              uppercased characters 
   	 * @draft 2.2
   	 */
   	public synchronized void setCaseFirst(boolean upper)
   	{
   		if (upper) {
   			m_caseFirst_ = AttributeValue.UPPER_FIRST_;
   		}
   		else {
   			m_caseFirst_ = AttributeValue.LOWER_FIRST_;
   		}
   		updateInternalState();
   	}
   	
   	/**
   	 * Sets the Collator to ignore any previous setCaseFirst(boolean) calls.
   	 * Ignores case preferences.
   	 * @draft 2.2
   	 */
   	public synchronized void setCaseFirstOff()
   	{
   		m_caseFirst_ = AttributeValue.OFF_;
   		updateInternalState();
   	}
   	
   	/**
   	 * Sets the case sorting preferences to the Collator's locale specific 
   	 * default value.
   	 * @see #setCaseFirst(boolean)
   	 * @see #setCaseFirstOff
   	 * @draft 2.2
   	 */
   	public synchronized final void setCaseFirstDefault()
   	{
   		m_caseFirst_ = m_defaultCaseFirst_;
   		updateInternalState();
   	}
   
   /**
     * Sets the alternate handling value for quartenary strength to the 
     * Collator's locale specific default value. 
     * @see #setAlternateHandling
     * @draft 2.2
     */
    public synchronized void setAlternateHandlingDefault()
    {
    	m_isAlternateHandlingShifted_ = m_defaultIsAlternateHandlingShifted_;
    }
    
    /**
     * Sets case level sorting to the Collator's locale specific default value.
     * @see #setCaseLevel
     * @draft 2.2
     */
    public synchronized void setCaseLevelDefault()
    {
    	m_isCaseLevel_ = m_defaultIsCaseLevel_;
    	updateInternalState();
    }
    
    /**
     * Set the decomposition mode to the Collator's locale specific default 
     * value. 
     * @see #getDecomposition
     * @draft 2.2
     */
    public synchronized void setDecompositionDefault()
    {
    	m_decomposition_ = m_defaultDecomposition_;
    }
    
    /**
     * Sets French collation to the Collator's locale specific default value.
     * @see #getFrenchCollation
     * @draft 2.2
     */
    public synchronized void setFrenchCollationDefault()
    {
    	m_isFrenchCollation_ = m_defaultIsFrenchCollation_;
    	updateInternalState();
    }
    
    /**
     * <p>Sets strength to the Collator's locale specific default value.</p>
     * @see #setStrength
     * @draft 2.2
     */
    public synchronized void setStrengthDefault()
    {
    	m_strength_ = m_defaultStrength_;
    	updateInternalState();
    }
    
    /**
     * Sets the French collation
     * @param flag true to set the French collation on, false to set it off
     * @draft 2.2
     */
    public synchronized void setFrenchCollation(boolean flag) 
    {
    	m_isFrenchCollation_ = flag;
    	updateInternalState();
    }
    
    /**
     * Sets the alternate handling for quartenary strength to be either 
     * shifted or non-ignorable. This attribute will only be effective with
     * a quartenary strength sort.
     * @param shifted true if shifted for alternate handling is desired, false 
     *        for the non-ignorable.
     * @draft 2.2
     */
    public synchronized void setAlternateHandling(boolean shifted)
    {
    	m_isAlternateHandlingShifted_ = shifted;
    	updateInternalState();
    }
    
    /**
     * Sets if case level sorting is required.
     * @param flag true if case level sorting is required, false otherwise
     * @draft 2.2
     */
    public synchronized void setCaseLevel(boolean flag) 
    {
    	m_isCaseLevel_ = flag;
    	updateInternalState();
    }


    // public getters --------------------------------------------------------
    
    /**
     * Internal method called to parse a lead surrogate's ce for the offset
     * to the next trail surrogate data.
     * @param ce collation element of the lead surrogate
     * @return data offset or 0 for the next trail surrogate
     * @draft 2.2
     */
    public int getFoldingOffset(int ce)
    {
    	if (isSpecial(ce) && getTag(ce) == CE_SURROGATE_TAG_) {
    		return (ce & 0xFFFFFF);
    	}
    	return 0;
    }
    	
	/**
     * Gets the collation rules for this RuleBasedCollator.     * @return returns the collation rules
     * @draft 2.2
     */
    public final String getRules()
    {
    	return m_rules_;
    }

	/**
     * <p>Transforms the String into a series of bits that can be compared 
     * bitwise to other CollationKeys. CollationKeys provide better 
     * performance than Collator.compare() when Strings are involved in 
     * multiple comparisons.</p> 
     * <p>Internally CollationKey stores its data in a null-terminated byte
     * array.</p>
     * <p>See the Collator class description for an example using 
     * CollationKeys.</p>
     * @param source the string to be transformed into a collation key.
     * @return the CollationKey for the given String based on this Collator's 
     *         collation rules. If the source String is null, a null 
     *         CollationKey is returned.
     * @see CollationKey
     * @see compare(String, String)
     * @draft 2.2
     */
    public CollationKey getCollationKey(String source)
    {
    	boolean compare[] = {m_isCaseLevel_,
    						 true,
    						 m_strength_ >= SECONDARY,
    						 m_strength_ >= TERTIARY,
    						 m_strength_ >= QUATERNARY,
							 m_strength_ == IDENTICAL
							};

		byte bytes[][] = {new byte[SORT_BUFFER_INIT_SIZE_CASE_], // case
    					new byte[SORT_BUFFER_INIT_SIZE_1_], // primary 
						new byte[SORT_BUFFER_INIT_SIZE_2_], // secondary
						new byte[SORT_BUFFER_INIT_SIZE_3_],	// tertiary	
						new byte[SORT_BUFFER_INIT_SIZE_4_]	// quartenary
    	};
    	int bytescount[] = {0, 0, 0, 0, 0};
    	int count[] = {0, 0, 0, 0, 0};
    	boolean doFrench = m_isFrenchCollation_ && compare[2];
    	// TODO: UCOL_COMMON_BOT4 should be a function of qShifted. 
	    // If we have no qShifted, we don't need to set UCOL_COMMON_BOT4 so 
	    // high.
   		int commonBottom4 = ((m_variableTopValue_ >> 8) & LAST_BYTE_MASK_) + 1;
    	byte hiragana4 = 0;
    	if (m_isHiragana4_ && compare[4]) {
    		// allocate one more space for hiragana, value for hiragana
      		hiragana4 = (byte)commonBottom4;
      		commonBottom4 ++;
    	}
    	
    	int bottomCount4 = 0xFF - commonBottom4;
    	// If we need to normalize, we'll do it all at once at the beginning!
    	if ((compare[5] || m_decomposition_ != NO_DECOMPOSITION)
    		/*&& UNORM_YES != unorm_quickCheck(source, len, normMode, status)*/
    		) {
        	/*
        	 * len = unorm_internalNormalize(normSource, normSourceLen,
                                      source, len,
                                      normMode, FALSE,
                                      status);
        	source = normSource;*/
        	String norm = source;
        	getSortKeyBytes(norm, compare, bytes, bytescount, count, 
        					doFrench, hiragana4, commonBottom4, bottomCount4);
    	}
		else {
			getSortKeyBytes(source, compare, bytes, bytescount, count, doFrench,
						hiragana4, commonBottom4, bottomCount4);
		}
		byte sortkey[] = getSortKey(source, compare, bytes, bytescount, count, 
									doFrench, commonBottom4, bottomCount4);
		return new CollationKey(source, sortkey);
    }
    		    
    /**
	 * Checks if uppercase is sorted before lowercase
	 * @return true if Collator sorts uppercase before lower, false otherwise
	 * @draft 2.2
	 */
	public boolean isUpperCaseFirst()
	{
		return (m_caseFirst_ == AttributeValue.UPPER_FIRST_);
	}
	
	/**
	 * Checks if lowercase is sorted before uppercase
	 * @return true if Collator sorts lowercase before upper, false otherwise
	 * @draft 2.2
	 */
	public boolean isLowerCaseFirst()
	{
		return (m_caseFirst_ == AttributeValue.LOWER_FIRST_);
	}
	
	/**
	 * Checks if case sorting is off.
	 * @return true if case sorting is off, false otherwise
	 * @draft 2.2
	 */
	public boolean isCaseFirstOff()
	{
		return (m_caseFirst_ == AttributeValue.OFF_);
	}
	
	/**
	 * Checks if the alternate handling attribute is shifted or non-ignorable.
	 * <ul>
	 * <li>If argument shifted is true and
	 *     <ul>
	 *     <li>return value is true, then the alternate handling attribute for 
	 *         the Collator is shifted. Or
	 *     <li>return value is false, then the alternate handling attribute for
	 *         the Collator is not shifted
	 *     </ul>
	 * <li> If argument shifted is false and 
	 *     <ul>
	 *     <li>return value is true, then the alternate handling attribute for 
	 *         the Collator is non-ignorable. Or
	 *     <li>return value is false, then the alternate handling attribute for
	 *         the Collator is not non-ignorable.
	 *     </ul>
	 * </ul>
	 * @param shifted true if checks are to be done on shifted, false if 
	 *        checks are to be done on non-ignorable
	 * @return true or false 
	 * @draft 2.2
	 */
	public boolean isAlternateHandling(boolean shifted)
	{
		if (shifted) {
			return m_isAlternateHandlingShifted_;
		}
		return !m_isAlternateHandlingShifted_;
	}
	
	/**
	 * Checks if case level sorting is on
	 * @return true if case level sorting is on
	 * @draft 2.2
	 */
	public boolean isCaseLevel()
	{
		return m_isCaseLevel_;
	}
	
	/**
	 * Checks if French Collation sorting is on
	 * @return true if French Collation sorting is on
	 * @draft 2.2
	 */
	public boolean isFrenchCollation()
	{
		return m_isFrenchCollation_;
	}
		
	// public other methods -------------------------------------------------

    /**
     * Compares the equality of two RuleBasedCollators.
     * @param obj the RuleBasedCollator to be compared with.
     * @return true if this RuleBasedCollator has exactly the same behaviour 
     *         as obj, false otherwise.
     * @draft 2.2
     */
    public boolean equals(Object obj) {
        if (obj == null || !super.equals(obj)) {
        	return false;  // super does class check
        }
        RuleBasedCollator other = (RuleBasedCollator)obj;
        // all other non-transient information is also contained in rules.
        return (m_rules_.equals(other.m_rules_));
    }
    
    /**
     * Standard override; no change in semantics.
     * @draft 2.2
     */
    public Object clone() {
    	// synwee todo: do after all implementation done
        return null;
    }
    
	/**
     * Generates the hash code for this RuleBasedCollator.
     * @return the unique hash code for this Collator
     * @draft 2.2
     */
    public final int hashCode() 
    {
    	return getRules().hashCode();
    }
    
    /**
     * <p>Compares the source string to the target string according to the
     * collation rules for this Collator. Returns an integer less than, equal 
     * to or greater than zero depending on whether the source String is less 
     * than, equal to or greater than the target string. See the Collator
     * class description for an example of use.</p>
     * <p>For a one time comparison, this method has the best performance. If 
     * a given String will be involved in multiple comparisons, 
     * CollationKey.compareTo() has the best performance. See the Collator
     * class description for an example using CollationKeys.</p>
     * @param source the source string.
     * @param target the target string.
     * @return Returns an integer value. Value is less than zero if source is 
     *         less than target, value is zero if source and target are equal, 
     *         value is greater than zero if source is greater than target.
     * @see CollationKey
     * @see Collator#getCollationKey
     * @draft 2.2
     */
    public final int compare(String source, String target)
    {
    	if (source == target) {
	        return 0;
	    }
	
		// Find the length of any leading portion that is equal
		int offset = getFirstUnmatchedOffset(source, target);
		if (source.charAt(offset) == 0) {
			if (target.charAt(offset) == 0) {
	        	return 0;
			}
			return 1;
	    }
	    else if (target.charAt(offset) == 0) {
	    	return -1;
	    }

		// setting up the collator parameters	
		boolean compare[] = {m_isCaseLevel_,
    						 true,
    						 m_strength_ >= SECONDARY,
    						 m_strength_ >= TERTIARY,
    						 m_strength_ >= QUATERNARY,
							 m_strength_ == IDENTICAL
							};
		boolean doFrench = m_isFrenchCollation_ && compare[2];
    	boolean doShift4 = m_isAlternateHandlingShifted_ && compare[4];
	    boolean doHiragana4 = m_isHiragana4_ && compare[4];
	
	    if (doHiragana4 && doShift4) {
	    	String sourcesub = source.substring(offset);
	    	String targetsub = target.substring(offset);
	      	return compareBySortKeys(sourcesub, targetsub);
	    }
	    
	    // Preparing the CE buffers. will be filled during the primary phase
		int cebuffer[][] = {new int[CE_BUFFER_SIZE_], new int[CE_BUFFER_SIZE_]};
		int cebuffersize[] = {0, 0};
		// This is the lowest primary value that will not be ignored if shifted
	    int lowestpvalue = m_isAlternateHandlingShifted_ 
	    									? m_variableTopValue_ << 16 : 0;
		int result = doPrimaryCompare(doHiragana4, lowestpvalue, source, 
										target, offset, cebuffer, cebuffersize);
		if (cebuffer[0] == null && cebuffer[1] == null) {
			// since the cebuffer is cleared when we have determined that
			// either source is greater than target or vice versa, the return
			// result is the comparison result and not the hiragana result
			return result;
		} 
		
		int hiraganaresult = result;
		
		if (compare[2]) {
			result = doSecondaryCompare(cebuffer, cebuffersize, doFrench);
			if (result != 0) {
				return result;
			}
		}
		// doing the case bit
	    if (compare[0]) {
	    	result = doCaseCompare(cebuffer);
			if (result != 0) {
				return result;
			}	
	    }
		// Tertiary level
	    if (compare[3]) {
	      	result = doTertiaryCompare(cebuffer);
	      	if (result != 0) {
	      		return result;
	      	}
	    }
	
		if (compare[4]) {  // checkQuad
	      	result = doQuaternaryCompare(cebuffer, lowestpvalue);
	      	if (result != 0) {
	      		return result;
	      	}
	    } 
	    else if (doHiragana4 && hiraganaresult != 0) {
	      	// If we're fine on quaternaries, we might be different
	      	// on Hiragana. This, however, might fail us in shifted.
	      	return hiraganaresult;
	    }
	
	    // For IDENTICAL comparisons, we use a bitwise character comparison 
	    // as a tiebreaker if all else is equal.                                
	    // Getting here  should be quite rare - strings are not identical -     
	    // that is checked first, but compared == through all other checks.  
	    if (compare[5]) {
	        return doIdenticalCompare(source, target, offset, true);
	    }
	    return 0;
    }
        
    // public abstract methods -----------------------------------------------

	// protected inner interfaces --------------------------------------------
    
    /**
	 * Attribute values to be used when setting the Collator options
	 */	
	protected static interface AttributeValue
	{
		/**
		 * Indicates that the default attribute value will be used. 
		 * See individual attribute for details on its default value. 
		 */
		static final int DEFAULT_ = -1;
		/** 
		 * Primary collation strength 
		 */
  		static final int PRIMARY_ = 0;
  		/** 
  		 * Secondary collation strength 
  		 */
  		static final int SECONDARY_ = 1;
  		/** 
  		 * Tertiary collation strength 
  		 */
  		static final int TERTIARY_ = 2;
  		/** 
  		 * Default collation strength 
  		 */
  		static final int DEFAULT_STRENGTH_ = TERTIARY;
  		/**
  		 * Internal use for strength checks in Collation elements
  		 */
  		static final int CE_STRENGTH_LIMIT_ = TERTIARY + 1;
  		/** 
  		 * Quaternary collation strength 
  		 */
  		static final int QUATERNARY_ = 3;
  		/** 
  		 * Identical collation strength 
  		 */
  		static final int IDENTICAL_ = 15;
  		/**
  		 * Internal use for strength checks
  		 */
  		static final int STRENGTH_LIMIT_ = IDENTICAL + 1;
  		/** 
  		 * Turn the feature off - works for FRENCH_COLLATION, CASE_LEVEL, 
  		 * HIRAGANA_QUATERNARY_MODE and DECOMPOSITION_MODE
  		 */
  		static final int OFF_ = 16;
  		/** 
  		 * Turn the feature on - works for FRENCH_COLLATION, CASE_LEVEL, 
  		 * HIRAGANA_QUATERNARY_MODE and DECOMPOSITION_MODE
  		 */
  		static final int ON_ = 17;
		/** 
		 * Valid for ALTERNATE_HANDLING. Alternate handling will be shifted 
		 */
  		static final int SHIFTED_ = 20;
  		/** 
  		 * Valid for ALTERNATE_HANDLING. Alternate handling will be non 
  		 * ignorable 
  		 */
  		static final int NON_IGNORABLE_ = 21;
  		/** 
  		 * Valid for CASE_FIRST - lower case sorts before upper case 
  		 */
  		static final int LOWER_FIRST_ = 24;
  		/** 
  		 * Upper case sorts before lower case 
  		 */
  		static final int UPPER_FIRST_ = 25;
  		/** 
  		 * Valid for NORMALIZATION_MODE ON and OFF are also allowed for this 
  		 * attribute 
  		 */
  		static final int ON_WITHOUT_HANGUL_ = 28;
  		/**
  		 * Number of attribute values
  		 */
  	    static final int LIMIT_ = 29;
	}
    
    /** 
     * Attributes that collation service understands. All the attributes can 
     * take DEFAULT value, as well as the values specific to each one. 
     */
	protected static interface Attribute {
     	/** 
     	 * Attribute for direction of secondary weights - used in French.
     	 * Acceptable values are ON, which results in secondary weights being 
     	 * considered backwards and OFF which treats secondary weights in the 
     	 * order they appear.
     	 */
     	static final int FRENCH_COLLATION_ = 0; 
     	/** 
     	 * Attribute for handling variable elements. Acceptable values are 
     	 * NON_IGNORABLE (default) which treats all the codepoints with 
     	 * non-ignorable primary weights in the same way, and SHIFTED which 
     	 * causes codepoints with primary weights that are equal or below the 
     	 * variable top value to be ignored on primary level and moved to the 
     	 * quaternary level.
     	 */
     	static final int ALTERNATE_HANDLING_ = 1;
     	/** 
     	 * Controls the ordering of upper and lower case letters. Acceptable 
     	 * values are OFF (default), which orders upper and lower case letters 
     	 * in accordance to their tertiary weights, UPPER_FIRST which forces 
     	 * upper case letters to sort before lower case letters, and 
     	 * LOWER_FIRST which does the opposite. 
     	 */
     	static final int CASE_FIRST_ = 2;
     	/** 
     	 * Controls whether an extra case level (positioned before the third 
     	 * level) is generated or not. Acceptable values are OFF (default),
     	 * when case level is not generated, and ON which causes the case
     	 * level to be generated. Contents of the case level are affected by
     	 * the value of CASE_FIRST attribute. A simple way to ignore accent 
     	 * differences in a string is to set the strength to PRIMARY and 
     	 * enable case level. 
     	 */
     	static final int CASE_LEVEL_ = 3;
     	/** 
     	 * Controls whether the normalization check and necessary 
     	 * normalizations are performed. When set to OFF (default) no 
     	 * normalization check is performed. The correctness of the result is 
     	 * guaranteed only if the input data is in so-called FCD form (see 
     	 * users manual for more info). When set to ON, an incremental check 
     	 * is performed to see whether the input data is in the FCD form. If 
     	 * the data is not in the FCD form, incremental NFD normalization is 
     	 * performed. 
     	 */
     	static final int NORMALIZATION_MODE_ = 4; 
     	/** 
     	 * The strength attribute. Can be either PRIMARY, SECONDARY, TERTIARY, 
     	 * QUATERNARY or IDENTICAL. The usual strength for most locales 
     	 * (except Japanese) is tertiary. Quaternary strength is useful when 
     	 * combined with shifted setting for alternate handling attribute and 
     	 * for JIS x 4061 collation, when it is used to distinguish between 
     	 * Katakana  and Hiragana (this is achieved by setting the 
     	 * HIRAGANA_QUATERNARY mode to on. Otherwise, quaternary level is 
     	 * affected only by the number of non ignorable code points in the 
     	 * string. Identical strength is rarely useful, as it amounts to 
     	 * codepoints of the NFD form of the string. 
     	 */
     	static final int STRENGTH_ = 5;
     	/** 
     	 * When turned on, this attribute positions Hiragana before all  
     	 * non-ignorables on quaternary level. This is a sneaky way to produce 
     	 * JIS sort order. 
     	 */     
     	static final int HIRAGANA_QUATERNARY_MODE_ = 6;
     	/**
     	 * Attribute count
     	 */
     	static final int LIMIT_ = 7;
	} 
	
    // protected data members ------------------------------------------------

	/**
	 * Size of collator raw data headers and options before the expansion
	 * data. This is used when expansion ces are to be retrieved. ICU4C uses
	 * the expansion offset starting from UCollator.UColHeader, hence ICU4J
	 * will have to minus that off to get the right expansion ce offset. In
	 * number of ints.
	 */
	protected int m_expansionOffset_;
	/**
	 * Size of collator raw data headers, options and expansions before
	 * contraction data. This is used when contraction ces are to be retrieved. 
	 * ICU4C uses contraction offset starting from UCollator.UColHeader, hence
	 * ICU4J will have to minus that off to get the right contraction ce 
	 * offset. In number of chars.
	 */
	protected int m_contractionOffset_;
    /**
     * Flag indicator if Jamo is special
     */
    protected boolean m_isJamoSpecial_;
 
 	// Collator options ------------------------------------------------------   
 	protected int m_defaultVariableTopValue_;
	protected boolean m_defaultIsFrenchCollation_;
	protected boolean m_defaultIsAlternateHandlingShifted_; 
    protected int m_defaultCaseFirst_;
    protected boolean m_defaultIsCaseLevel_;
    protected int m_defaultDecomposition_;
    protected int m_defaultStrength_;
    protected boolean m_defaultIsHiragana4_;
 	/**
 	 * Value of the variable top
 	 */
    protected int m_variableTopValue_;
    /** 
     * Attribute for special Hiragana 
     */
    protected boolean m_isHiragana4_;         
	/**
     * Case sorting customization
     */
    protected int m_caseFirst_;
    
    // end Collator options --------------------------------------------------
    
    /**
     * Expansion table
     */
    protected int m_expansion_[];
    /**
     * Contraction index table
     */
    protected char m_contractionIndex_[];
    /**
     * Contraction CE table
     */
    protected int m_contractionCE_[];
    /**
     * Data trie
     */
    protected IntTrie m_trie_;
    /**
     * Table to store all collation elements that are the last element of an
     * expansion. This is for use in StringSearch.
     */
    protected int m_expansionEndCE_[];
    /**
     * Table to store the maximum size of any expansions that end with the 
     * corresponding collation element in m_expansionEndCE_. For use in
     * StringSearch too
     */
    protected byte m_expansionEndCEMaxSize_[];
    /**
     * Heuristic table to store information on whether a char character is 
     * considered "unsafe". "Unsafe" character are combining marks or those 
     * belonging to some contraction sequence from the offset 1 onwards. 
     * E.g. if "ABC" is the only contraction, then 'B' and 'C' are considered 
     * unsafe. If we have another contraction "ZA" with the one above, then 
     * 'A', 'B', 'C' are "unsafe" but 'Z' is not. 
     */
    protected byte m_unsafe_[];
    /**
     * Table to store information on whether a codepoint can occur as the last
     * character in a contraction
     */
    protected byte m_contractionEnd_[];
    /**
     * Table for UCA use, may be removed
     */
    protected char m_UCAContraction_[];
	/**
	 * Original collation rules
	 */
	protected String m_rules_;
	/**
     * The smallest "unsafe" codepoint
     */
    protected char m_minUnsafe_;
    /**
	 * The smallest codepoint that could be the end of a contraction
	 */
	protected char m_minContractionEnd_;
	
	/**
     * UnicodeData.txt property object
     */
    protected static final RuleBasedCollator UCA_;  
    
    // block to initialise character property database
    static
    {
        try
        {
        	UCA_ = new RuleBasedCollator();
        	InputStream i = UCA_.getClass().getResourceAsStream(
                                        "/com/ibm/icu/impl/data/ucadata.dat");
        	
       		BufferedInputStream b = new BufferedInputStream(i, 90000);
        	CollatorReader reader = new CollatorReader(b);
        	reader.read(UCA_);
        	b.close();
        	i.close();
        	ResourceBundle rb = 
        	                  ICULocaleData.getLocaleElements(Locale.ENGLISH);
        	UCA_.m_rules_ = rb.getString("%%UCARULES");
        	UCA_.init();
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }    
    
    // protected constants ---------------------------------------------------
    
    protected static final int CE_SPECIAL_FLAG_ = 0xF0000000;
    /** 
     * Lead surrogate that is tailored and doesn't start a contraction 
     */
    protected static final int CE_SURROGATE_TAG_ = 5;  
  
  	/**
	 * Minimum size required for the binary collation data in bytes.
	 * Size of UCA header + size of options to 4 bytes
	 */
	private static final int MIN_BINARY_DATA_SIZE_ = (41 + 8) << 2;     
	/**
  	 * Mask to get the primary strength of the collation element
  	 */
  	protected static final int CE_PRIMARY_MASK_ = 0xFFFF0000;
  	/**
  	 * Mask to get the secondary strength of the collation element
  	 */
   	protected static final int CE_SECONDARY_MASK_ = 0xFF00;
   	/**
  	 * Mask to get the tertiary strength of the collation element
  	 */
   	protected static final int CE_TERTIARY_MASK_ = 0xFF;
   	/**
   	 * Primary strength shift 
   	 */
	protected static final int CE_PRIMARY_SHIFT_ = 16;
	/** 
	 * Secondary strength shift 
	 */
	protected static final int CE_SECONDARY_SHIFT_ = 8;

   	/**
   	 * Continuation marker
   	 */
   	protected static final int CE_CONTINUATION_MARKER_ = 0xC0;
    
    // end protected constants -----------------------------------------------
    
    // protected constructor -------------------------------------------------
  
  	/**
     * Constructors a RuleBasedCollator from the argument locale.
     * If no resource bundle is associated with the locale, UCA is used 
     * instead.
     * @param locale
     * @exception Exception thrown when there's an error creating the Collator
     */
    protected RuleBasedCollator(Locale locale) throws Exception
    {
    	ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
 
    	if (rb != null) {
    		byte map[] = (byte [])rb.getObject("%%CollationBin");
			// synwee todo: problem, data in little endian and
			// ICUListResourceBundle should not calculate size by
			// using .available() that only gives the buffer size
			BufferedInputStream input = 
						new BufferedInputStream(new ByteArrayInputStream(map));
			CollatorReader reader = new CollatorReader(input, false);
			if (map.length > MIN_BINARY_DATA_SIZE_) {
				// synwee todo: undo when problem solved
				reader.read(this);
    		} 
    		else {
    			reader.readHeader(this);
    			reader.readOptions(this);
    			// duplicating UCA_'s data
    			m_expansion_ = UCA_.m_expansion_;
    			m_contractionIndex_ = UCA_.m_contractionIndex_;
    			m_contractionCE_ = UCA_.m_contractionCE_;
    			m_trie_ = UCA_.m_trie_;
				m_expansionEndCE_ = UCA_.m_expansionEndCE_;
    			m_expansionEndCEMaxSize_ = UCA_.m_expansionEndCEMaxSize_;
    			m_unsafe_ = UCA_.m_unsafe_;
    			m_contractionEnd_ = UCA_.m_contractionEnd_;
    			m_minUnsafe_ = UCA_.m_minUnsafe_; 
    	     	m_minContractionEnd_ = UCA_.m_minContractionEnd_;
    	     	setStrengthDefault();
    	     	setDecompositionDefault();
    	     	setFrenchCollationDefault();
    			setAlternateHandlingDefault();
    			setCaseLevelDefault();
    			setCaseFirstDefault();
    			setHiraganaQuartenaryDefault();
    			updateInternalState();
    		}
    		Object rules = rb.getObject("CollationElements");
    		if (rules != null) {
     			m_rules_ = (String)((Object[][])rules)[0][1];
    		}
    		init();
    	}
    }
    
  	/**
    * <p>Protected constructor for use by subclasses. 
    * Public access to creating Collators is handled by the API 
    * Collator.getInstance() or RuleBasedCollator(String rules).
    * </p>
    * <p>
    * This constructor constructs the UCA collator internally
    * </p>
    * @draft 2.2
    */
    protected RuleBasedCollator() throws Exception
    {
    }
  	
    // protected methods -----------------------------------------------------
    
    /**
     * Initializes the RuleBasedCollator
     */
    protected synchronized final void init()
    {
    	for (m_minUnsafe_ = 0; m_minUnsafe_ < DEFAULT_MIN_HEURISTIC_; 
    	     m_minUnsafe_ ++) {  
    		// Find the smallest unsafe char.
        	if (isUnsafe(m_minUnsafe_)) {
        		break;
        	}
    	}
    	
    	for (m_minContractionEnd_ = 0; 
    	     m_minContractionEnd_ < DEFAULT_MIN_HEURISTIC_; 
    	     m_minContractionEnd_ ++) {  
    	    // Find the smallest contraction-ending char.
        	if (isContractionEnd(m_minContractionEnd_)) {
        		break;
        	}
    	}
    	setStrengthDefault();
    	setDecompositionDefault();
    	setFrenchCollationDefault();
    	setAlternateHandlingDefault();
    	setCaseLevelDefault();
    	setCaseFirstDefault();
    	setHiraganaQuartenaryDefault();
    	updateInternalState();
    }
    
    /**
     * Test whether a char character is potentially "unsafe" for use as a 
     * collation starting point. "Unsafe" characters are combining marks or 
     * those belonging to some contraction sequence from the offset 1 onwards.
     * E.g. if "ABC" is the only contraction, then 'B' and 
     * 'C' are considered unsafe. If we have another contraction "ZA" with 
     * the one above, then 'A', 'B', 'C' are "unsafe" but 'Z' is not. 
     * @param ch character to determin
     * @return true if ch is unsafe, false otherwise
     */
	protected final boolean isUnsafe(char ch) 
	{
    	if (ch < m_minUnsafe_) {
	        return false;
	    }
	
	    if (ch >= (HEURISTIC_SIZE_ << HEURISTIC_SHIFT_)) {
	      	if (UTF16.isTrailSurrogate(ch)) {
	            //  Trail surrogate are always considered unsafe.
	            return true;
	        }
	        ch &= HEURISTIC_OVERFLOW_MASK_;
	        ch += HEURISTIC_OVERFLOW_OFFSET_;
	    }
	    int value = m_unsafe_[ch >> HEURISTIC_SHIFT_];
	    return ((value >> (ch & HEURISTIC_MASK_)) & 1) != 0;
	}
	
	/**
	 * Approximate determination if a char character is at a contraction end.
	 * Guaranteed to be true if a character is at the end of a contraction,
	 * otherwise it is not deterministic.
	 * @param ch character to be determined
	 */
	protected final boolean isContractionEnd(char ch) 
	{
		if (UTF16.isTrailSurrogate(ch)) {
      		return true;
		}

    	if (ch < m_minContractionEnd_) {
        	return false;
    	}

   		if (ch >= (HEURISTIC_SIZE_ << HEURISTIC_SHIFT_)) {
        	ch &= HEURISTIC_OVERFLOW_MASK_;
        	ch += HEURISTIC_OVERFLOW_OFFSET_;
    	}
    	int value = m_contractionEnd_[ch >> HEURISTIC_SHIFT_];
	    return ((value >> (ch & HEURISTIC_MASK_)) & 1) != 0;
	}
	
	/**
	 * Resets the internal case data members and compression values.
	 */
	protected synchronized void updateInternalState() 
	{
      	if (m_caseFirst_ == AttributeValue.UPPER_FIRST_) {
        	m_caseSwitch_ = CASE_SWITCH_;
      	} 
      	else {
        	m_caseSwitch_ = NO_CASE_SWITCH_;
      	}

      	if (m_isCaseLevel_ || m_caseFirst_ == AttributeValue.OFF_) {
        	m_mask3_ = CE_REMOVE_CASE_;
        	m_common3_ = COMMON_NORMAL_3_;
        	m_addition3_ = FLAG_BIT_MASK_CASE_SWITCH_OFF_;
        	m_top3_ = COMMON_TOP_CASE_SWITCH_OFF_3_;
        	m_bottom3_ = COMMON_BOTTOM_3_;
      	} 
      	else {
        	m_mask3_ = CE_KEEP_CASE_;
        	m_addition3_ = FLAG_BIT_MASK_CASE_SWITCH_ON_;
        	if (m_caseFirst_ == AttributeValue.UPPER_FIRST_) {
          		m_common3_ = COMMON_UPPER_FIRST_3_;
          		m_top3_ = COMMON_TOP_CASE_SWITCH_UPPER_3_;
          		m_bottom3_ = COMMON_BOTTOM_CASE_SWITCH_UPPER_3_;
        	} else {
          		m_common3_ = COMMON_NORMAL_3_;
          		m_top3_ = COMMON_TOP_CASE_SWITCH_LOWER_3_;
          		m_bottom3_ = COMMON_BOTTOM_CASE_SWITCH_LOWER_3_;
        	}
      	}

      	// Set the compression values
      	int total3 = m_top3_ - COMMON_BOTTOM_3_ - 1;
      	// we multilply double with int, but need only int
      	m_topCount3_ = (int)(PROPORTION_3_ * total3); 
      	m_bottomCount3_ = total3 - m_topCount3_;

      	if (!m_isCaseLevel_ && m_strength_ == AttributeValue.TERTIARY_ 
          	&& !m_isFrenchCollation_ && !m_isAlternateHandlingShifted_) {
        	m_isSimple3_ = true;
      	} 
      	else {
        	m_isSimple3_ = false;
      	}
	}
	
	/**
 	 * <p>Converts the C attribute index and values for use and stores it into 
 	 * the relevant default attribute variable.</p>
 	 * <p>Note internal use, no sanity checks done on arguments</p>
 	 */
    protected void setAttributeDefault(int attribute, int value)
    {
    	switch (attribute) {
    		case Attribute.FRENCH_COLLATION_:
    			m_defaultIsFrenchCollation_ = (value == AttributeValue.ON_);
    			break;
    		case Attribute.ALTERNATE_HANDLING_:
    			m_defaultIsAlternateHandlingShifted_ = 
    			                            (value == AttributeValue.SHIFTED_);
    			break;
    		case Attribute.CASE_FIRST_:
    			m_defaultCaseFirst_ = value;
        		break;
    		case Attribute.CASE_LEVEL_:
    			m_defaultIsCaseLevel_ = (value == AttributeValue.ON_);
    			break;
    		case Attribute.NORMALIZATION_MODE_:
    			m_defaultDecomposition_ = value;
    			break;
    		case Attribute.STRENGTH_:
    			m_defaultStrength_ = value;
    		case Attribute.HIRAGANA_QUATERNARY_MODE_:
    			m_defaultIsHiragana4_ = (value == AttributeValue.ON_);
    	}
    }
    
    /**
	 * Retrieve the tag of a special ce
	 * @param ce ce to test
	 * @return tag of ce
	 */
	protected static int getTag(int ce) 
	{
		return (ce & CE_TAG_MASK_) >> CE_TAG_SHIFT_;
	}
    
    /** 
	 * Checking if ce is special
	 * @param ce to check
	 * @return true if ce is special
	 */
	protected static boolean isSpecial(int ce)
	{
		return (ce & CE_SPECIAL_FLAG_) == CE_SPECIAL_FLAG_; 
	}
	
	/**
	 * Getting the mask for collation strength
	 * @param strength collation strength
 	 * @return collation element mask
	 */
	protected static final int getMask(int strength) 
	{
	    switch (strength) 
	    {
	    	case Collator.PRIMARY:
	        	return CE_PRIMARY_MASK_;
	    	case Collator.SECONDARY:
	        	return CE_SECONDARY_MASK_ | CE_PRIMARY_MASK_;
	    	default:
	        	return CE_TERTIARY_MASK_ | CE_SECONDARY_MASK_ 
	        											| CE_PRIMARY_MASK_;
	    }
	}

	/** 
	 * Gets the primary weights from a CE 
	 * @param ce collation element
	 * @return the primary weight of the collation element
	 */
	protected static final int getPrimaryWeight(int ce)
	{
		return ((ce) & CE_PRIMARY_MASK_) >> CE_PRIMARY_SHIFT_;
	}
	
	/** 
	 * Gets the secondary weights from a CE 
	 * @param ce collation element
	 * @return the secondary weight of the collation element
	 */
	protected static final int getSecondaryWeight(int ce)
	{
		return (ce & CE_SECONDARY_MASK_) >> CE_SECONDARY_SHIFT_;
	}
	
	/** 
	 * Gets the tertiary weights from a CE 
	 * @param ce collation element
	 * @return the tertiary weight of the collation element
	 */
	protected static final int getTertiaryWeight(int ce)
	{
		return ce & CE_TERTIARY_MASK_;
	}
	
    // private variables -----------------------------------------------------

    /**
     * The smallest natural unsafe or contraction end char character before 
     * tailoring.
     * This is a combining mark.
     */
    private static final int DEFAULT_MIN_HEURISTIC_ = 0x300;
    /** 
     * Heuristic table table size. Size is 32 bytes, 1 bit for each 
     * latin 1 char, and some power of two for hashing the rest of the chars.   
     * Size in bytes.                               
     */
	private static final char HEURISTIC_SIZE_ = 1056;
    /** 
     * Mask value down to "some power of two" - 1,
     * number of bits, not num of bytes.
     */
	private static final char HEURISTIC_OVERFLOW_MASK_ = 0x1fff;
	/**
	 * Unsafe character shift
	 */
	private static final int HEURISTIC_SHIFT_ = 3;
	/**
	 * Unsafe character addition for character too large, it has to be folded
	 * then incremented.
	 */
	private static final char HEURISTIC_OVERFLOW_OFFSET_ = 256;
	/** 
     * Mask value to get offset in heuristic table.
     */
	private static final char HEURISTIC_MASK_ = 7;
	
	private byte m_caseSwitch_;
    private int m_common3_;
    private byte m_mask3_;
    /** 
     * When switching case, we need to add or subtract different values.
     */
    private int m_addition3_; 
    /** 
     * Upper range when compressing 
     */
    private int m_top3_;
    /** 
     * Upper range when compressing 
     */ 
    private int m_bottom3_; 
    private int m_topCount3_;
    private int m_bottomCount3_;	
	/**
	 * Case first constants
	 */
	private static final byte CASE_SWITCH_ = (byte)0xC0;
	private static final byte NO_CASE_SWITCH_ = 0;
	/**
	 * Case level constants
	 */
	private static final byte CE_REMOVE_CASE_ = (byte)0x3F;
	private static final byte CE_KEEP_CASE_ = (byte)0xFF;
	/**
	 * Case strength mask
	 */
	private static final byte CE_CASE_BIT_MASK_ = (byte)0xC0;
	private static final byte CE_CASE_MASK_3_ = (byte)0xFF;
	/** 
	 * Sortkey size factor. Values can be changed.
	 */
	private static final double PROPORTION_2_ = 0.5;
	private static final double PROPORTION_3_ = 0.667;

	// These values come from the UCA ----------------------------------------
	
	/** 
	 * This is an enum that lists magic special byte values from the 
	 * fractional UCA 
	 */
	private static final byte BYTE_ZERO_ = 0x0;
    private static final byte BYTE_LEVEL_SEPARATOR_ = (byte)0x01;
    private static final byte BYTE_SORTKEY_GLUE_ = (byte)0x02;
    private static final byte BYTE_SHIFT_PREFIX_ = (byte)0x03;
    private static final byte BYTE_UNSHIFTED_MIN_ = BYTE_SHIFT_PREFIX_;
    private static final byte BYTE_FIRST_TAILORED_ = (byte)0x04;
    private static final byte BYTE_COMMON_ = (byte)0x05;
    private static final byte BYTE_FIRST_UCA_ = BYTE_COMMON_;
    private static final byte BYTE_LAST_LATIN_PRIMARY_ = (byte)0x4C;
    private static final byte BYTE_FIRST_NON_LATIN_PRIMARY_ = (byte)0x4D;
    private static final byte BYTE_UNSHIFTED_MAX_ = (byte)0xFF;
	private static final int COMMON_BOTTOM_2_ = BYTE_COMMON_;
	private static final int COMMON_TOP_2_ = 0x86; // int for unsigness
	private static final int TOTAL_2_ = COMMON_TOP_2_ - COMMON_BOTTOM_2_ - 1; 
	private static final int FLAG_BIT_MASK_CASE_SWITCH_OFF_ = 0x80;
	private static final int FLAG_BIT_MASK_CASE_SWITCH_ON_ = 0x40;
	private static final int COMMON_TOP_CASE_SWITCH_OFF_3_ = 0x85;
	private static final int COMMON_TOP_CASE_SWITCH_LOWER_3_ = 0x45;
	private static final int COMMON_TOP_CASE_SWITCH_UPPER_3_ = 0xC5;
	private static final int COMMON_BOTTOM_3_ = 0x05;
	private static final int COMMON_BOTTOM_CASE_SWITCH_UPPER_3_ = 0x86;
	private static final int COMMON_BOTTOM_CASE_SWITCH_LOWER_3_ = 
                                                              COMMON_BOTTOM_3_;
	private static final int TOP_COUNT_2_ = (int)(PROPORTION_2_ * TOTAL_2_);
	private static final int BOTTOM_COUNT_2_ = TOTAL_2_ - TOP_COUNT_2_;
	private static final int COMMON_2_ = COMMON_BOTTOM_2_;
	private static final int COMMON_UPPER_FIRST_3_ = 0xC5;
	private static final int COMMON_NORMAL_3_ = COMMON_BOTTOM_3_;
	private static final int COMMON_4_ = (byte)0xFF;
	
	/**
	 * If this collator is to generate only simple tertiaries for fast path
	 */
	private boolean m_isSimple3_;
	
	/**
     * French collation sorting flag
     */
    private boolean m_isFrenchCollation_;
    /**
     * Flag indicating if shifted is requested for quartenary alternate
     * handling. If this is not true, the default for alternate handling will
     * be non-ignorable.
     */
    private boolean m_isAlternateHandlingShifted_; 
    /** 
     * Extra case level for sorting
     */
    private boolean m_isCaseLevel_;
    
    private static final int CE_TAG_SHIFT_ = 24;
	private static final int CE_TAG_MASK_ = 0x0F000000;
	
	private static final int SORT_BUFFER_INIT_SIZE_ = 128;
	private static final int SORT_BUFFER_INIT_SIZE_1_ = 
													SORT_BUFFER_INIT_SIZE_ << 3;
	private static final int SORT_BUFFER_INIT_SIZE_2_ = SORT_BUFFER_INIT_SIZE_;
	private static final int SORT_BUFFER_INIT_SIZE_3_ = SORT_BUFFER_INIT_SIZE_;
	private static final int SORT_BUFFER_INIT_SIZE_CASE_ = 
												SORT_BUFFER_INIT_SIZE_ >> 2;
	private static final int SORT_BUFFER_INIT_SIZE_4_ = SORT_BUFFER_INIT_SIZE_;
    
    private static final int CE_CONTINUATION_TAG_ = 0xC0;
	private static final int CE_REMOVE_CONTINUATION_MASK_ = 0xFFFFFF3F;

	private static final int LAST_BYTE_MASK_ = 0xFF;
	
	private static final int CE_RESET_TOP_VALUE_ = 0x9F000303;
	private static final int CE_NEXT_TOP_VALUE_ = 0xE8960303;

	private static final byte SORT_CASE_BYTE_START_ = (byte)0x80;
	private static final byte SORT_CASE_SHIFT_START_ = (byte)7;
	
	private static final byte SORT_LEVEL_TERMINATOR_ = 1;
	
	/**
	 * CE buffer size
	 */
	private static final int CE_BUFFER_SIZE_ = 512;

    // private methods -------------------------------------------------------
    
    /**
     * Checks if the argument ce is a continuation
     * @param ce collation element to test
     * @return true if ce is a continuation
     */
    private static final boolean isContinuation(int ce) 
    {
    	return (ce & CE_CONTINUATION_TAG_) == CE_CONTINUATION_TAG_;
    }
    
    /**
     * Gets the 2 bytes of primary order and adds it to the primary byte array
     * @param ce current ce
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param notIsContinuation flag indicating if the current bytes belong to 
     * 			a continuation ce
     * @param doShift flag indicating if ce is to be shifted
     * @param leadPrimary lead primary used for compression
     * @param commonBottom4 common byte value for quartenary
     * @param bottomCount4 smallest byte value for quartenary
     * @return the new lead primary for compression
     */
    private final int doPrimaryBytes(int ce, byte bytes[][], int bytescount[],
    						      int count[], boolean notIsContinuation, 
    						      boolean doShift, int leadPrimary,
    						      int commonBottom4, int bottomCount4)
    {
    	
    	int p2 = (ce >>= 16) & LAST_BYTE_MASK_; // in ints for unsigned 
        int p1 = (ce >> 8) & LAST_BYTE_MASK_;  // comparison
    	if (doShift) {
    		if (count[4] > 0) {
            	while (count[4] > bottomCount4) {
            		append(bytes, bytescount, 4,
            				(byte)(commonBottom4 + bottomCount4));
           			count[4] -= bottomCount4;
           		}
           		append(bytes, bytescount, 4,
							(byte)(commonBottom4 + (count[4] - 1)));
           		count[4] = 0;
        	}
        	// dealing with a variable and we're treating them as shifted
            // This is a shifted ignorable
            if (p1 != 0) { 
             	// we need to check this since we could be in continuation
             	append(bytes, bytescount, 4, (byte)p1);
            }
            if (p2 != 0) {
            	append(bytes, bytescount, 4, (byte)p2);
            }
        } 
        else {
        	// Note: This code assumes that the table is well built 
        	// i.e. not having 0 bytes where they are not supposed to be.
       		// Usually, we'll have non-zero primary1 & primary2, except 
      		// in cases of LatinOne and friends, when primary2 will be
       		// regular and simple sortkey calc 
       		if (p1 != CollationElementIterator.IGNORABLE) {
           		if (notIsContinuation) {
           			if (leadPrimary == p1) {
               			append(bytes, bytescount, 1, (byte)p2);
           			} 
                  	else {
                    	if (leadPrimary != 0) {
                    		append(bytes, bytescount, 1, 
                    				(byte)((p1 > leadPrimary) 
                    						? BYTE_UNSHIFTED_MAX_ 
                      						: BYTE_UNSHIFTED_MIN_));
                    	}
                    	if (p2 == CollationElementIterator.IGNORABLE) {
                    		// one byter, not compressed
                        	append(bytes, bytescount, 1, (byte)p1);
                        	leadPrimary = 0;
                    	} 
                    	else if (p1 < BYTE_FIRST_NON_LATIN_PRIMARY_
                    			|| (p1 > ((CE_RESET_TOP_VALUE_ >> 24) & 0xFF)
                    			&& p1 < ((CE_NEXT_TOP_VALUE_ >> 24) & 0xFF))) {
                    			// not compressible
                        		leadPrimary = 0;
                        		append(bytes, bytescount, 1, (byte)p1);
                        		append(bytes, bytescount, 1, (byte)p2);
                    	} 
                    	else { // compress
                    		leadPrimary = p1;
                        	append(bytes, bytescount, 1, (byte)p1);
                        	append(bytes, bytescount, 1, (byte)p2);
                    	}
                  	}
                } 
                else { 
                	// continuation, add primary to the key, no compression
                  	append(bytes, bytescount, 1, (byte)p1);
                  	if (p2 != CollationElementIterator.IGNORABLE) {
                    	append(bytes, bytescount, 1, (byte)p2); // second part
                  	}
                }
            }
       	}
       	return leadPrimary;
    }
    
    /**
     * Gets the secondary byte and adds it to the secondary byte array
     * @param ce current ce
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param notIsContinuation flag indicating if the current bytes belong to 
     * 			a continuation ce
     * @param doFrench flag indicator if french sort is to be performed
     * @param frenchOffset start and end offsets to source string for reversing
     */
    private final void doSecondaryBytes(int ce, byte bytes[][], 
    								 int bytescount[], int count[], 
    								 boolean notIsContinuation,
    						 		 boolean doFrench, int frenchOffset[])
    {
    	int s = (ce >>= 8) & LAST_BYTE_MASK_; // int for comparison
    	if (s != 0) {
    		if (!doFrench) {
                // This is compression code.
                if (s == COMMON_2_ && notIsContinuation) {
                   count[2] ++;
                } 
                else {
                  	if (count[2] > 0) {
                    	if (s > COMMON_2_) { // not necessary for 4th level.
                    		while (count[2] > TOP_COUNT_2_) {
                        		append(bytes, bytescount, 2,
                        				(byte)(COMMON_TOP_2_ - TOP_COUNT_2_));
                        		count[2] -= TOP_COUNT_2_;
                      		}
                      		append(bytes, bytescount, 2,
                      				(byte)(COMMON_TOP_2_ - (count[2] - 1)));
                    	} 
                    	else {
                    		while (count[2] > BOTTOM_COUNT_2_) {
                        		append(bytes, bytescount, 2,
                        			(byte)(COMMON_BOTTOM_2_ + BOTTOM_COUNT_2_));
                        		count[2] -= BOTTOM_COUNT_2_;
                      		}
                      		append(bytes, bytescount, 2,
                      				(byte)(COMMON_BOTTOM_2_ + (count[2] - 1)));
                    	}
                    	count[2] = 0;
                  	}
                  	append(bytes, bytescount, 2, (byte)s);
                }
            } 
            else {
                  append(bytes, bytescount, 2, (byte)s);
                  // Do the special handling for French secondaries
                  // We need to get continuation elements and do intermediate 
                  // restore 
                  // abc1c2c3de with french secondaries need to be edc1c2c3ba 
                  // NOT edc3c2c1ba
                  if (notIsContinuation) {
                    	if (frenchOffset[0] != -1) {
                        	// reverse secondaries from frenchStartPtr up to 
                        	// frenchEndPtr
                      		reverseBuffer(bytes[2], frenchOffset);
                      		frenchOffset[0] = -1;
                    	}
                  } 
                  else {
                    	if (frenchOffset[0] == -1) {
                      		frenchOffset[0] = bytescount[2] - 2;
                    	}
                    	frenchOffset[1] = bytescount[2] - 1;
                  }
        	}
    	}
    }
    
    /**
     * Reverse the argument buffer 
     * @param buffer to reverse
     * @param offset start and end offsets to reverse
     */
    private void reverseBuffer(byte buffer[], int offset[]) 
    { 
    	int start = offset[0];
    	int end = offset[1];
    	while (start < end) { 
    		byte b = buffer[start]; 
    		buffer[start ++] = buffer[end]; 
    		buffer[end --] = b; 
    	}
	}

	/**
	 * Insert the case shifting byte if required
	 * @param bytes array of byte arrays corresponding to each strength
	 * @param bytescount array of the size of the byte arrays
	 * @param caseshift value
	 * @return new caseshift value
	 */
	private static final int doCaseShift(byte bytes[][], int bytescount[],
											 int caseshift) 
	{
  		if (caseshift  == 0) {
    		append(bytes, bytescount, 0, SORT_CASE_BYTE_START_);
    		caseshift = SORT_CASE_SHIFT_START_;
  		}
  		return caseshift;
	}

	/**
	 * Performs the casing sort
	 * @param tertiary byte in ints for easy comparison
	 * @param bytes of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param notIsContinuation flag indicating if the current bytes belong to 
     * 			a continuation ce
	 * @param caseshift
	 * @return the new value of case shift
	 */
	private final int doCaseBytes(int tertiary, byte bytes[][], 
							   int bytescount[], boolean notIsContinuation, 
							   int caseshift)
	{
		caseshift = doCaseShift(bytes, bytescount, caseshift);
              		
        if (notIsContinuation && tertiary != 0) {
        	byte casebits = (byte)(tertiary & 0xC0);
            if (m_caseFirst_ == AttributeValue.UPPER_FIRST_) {
                if (casebits == 0) {
                    bytes[0][bytescount[0] - 1] |= (1 << (-- caseshift));
                } 
                else {
                     // second bit
                     caseshift = doCaseShift(bytes, bytescount, caseshift);
                     bytes[0][bytescount[0] - 1] |= ((casebits >> 6) & 1) 
                     										<< (-- caseshift);
                } 
            }
            else {
                if (casebits != 0) {
                    bytes[0][bytescount[0] - 1] |= 1 << (-- caseshift);
                    // second bit
                    caseshift = doCaseShift(bytes, bytescount, caseshift);
                    bytes[0][bytescount[0] - 1] |= ((casebits >> 7) & 1) 
                          									<< (-- caseshift);
                }
            }
        }
             
		return caseshift;
	}
	
	/**
	 * Gets the tertiary byte and adds it to the tertiary byte array
     * @param tertiary byte in int for easy comparison
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param notIsContinuation flag indicating if the current bytes belong to 
     * 			a continuation ce
	 */
	private final void doTertiaryBytes(int tertiary, byte bytes[][], 
									int bytescount[], int count[], 
									boolean notIsContinuation)
	{
		if (tertiary != 0) {
			// This is compression code.
            // sequence size check is included in the if clause
            if (tertiary == m_common3_ && notIsContinuation) {
                 count[3] ++;
            } 
            else {
            	int common3 = m_common3_ & LAST_BYTE_MASK_;
                if ((tertiary > common3 
                	&& m_common3_ == COMMON_NORMAL_3_)
                    || (tertiary <= common3 
                    	&& m_common3_ == COMMON_UPPER_FIRST_3_)) {
                    tertiary += m_addition3_;
                }
                if (count[3] > 0) {
                	if (tertiary > common3) {
                		while (count[3] > m_topCount3_) {
                        	append(bytes, bytescount, 3, 
                        					(byte)(m_top3_ - m_topCount3_));
                        	count[3] -= m_topCount3_;
                      	}
                      	append(bytes, bytescount, 3,
                        		             (byte)(m_top3_ - (count[3] - 1)));
                 	} 
                 	else {
                 		while (count[3] > m_bottomCount3_) {
                        	append(bytes, bytescount, 3,
                       		          (byte)(m_bottom3_ + m_bottomCount3_));
                        	count[3] -= m_bottomCount3_;
                      	}
                      	append(bytes, bytescount, 3,
                        		          (byte)(m_bottom3_ + (count[3] - 1)));
                    }
                    count[3] = 0;
                }
                append(bytes, bytescount, 3, (byte)tertiary);
            }
        }
	}
	
	/**
	 * Gets the quartenary byte and adds it to the quartenary byte array
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param isCodePointHiragana flag indicator if the previous codepoint 
     * 			we dealt with was Hiragana
     * @param commonBottom4 smallest common quartenary byte 
     * @param bottomCount4 smallest quartenary byte 
     * @param hiragana4 hiragana quartenary byte
	 */
	private final void doQuartenaryBytes(byte bytes[][], int bytescount[], 
									  int count[],	
									  boolean isCodePointHiragana,
									  int commonBottom4, int bottomCount4,
									  byte hiragana4)
	{
		if (isCodePointHiragana) { // This was Hiragana, need to note it
			if (count[4] > 0) { // Close this part
            	while (count[4] > bottomCount4) {
                    append(bytes, bytescount, 4, (byte)(commonBottom4 
                    									+ bottomCount4));
                    count[4] -= bottomCount4;
                }
                append(bytes, bytescount, 4, (byte)(commonBottom4 
                										+ (count[4] - 1)));
                count[4] = 0;
            }
            append(bytes, bytescount, 4, hiragana4); // Add the Hiragana
        } 
        else { // This wasn't Hiragana, so we can continue adding stuff
            count[4] ++;
        }
	}
	
	/**
	 * Iterates through the argument string for all ces.
	 * Split the ces into their relevant primaries, secondaries etc.
	 * @param source normalized string
	 * @param compare array of flags indicating if a particular strength is 
	 * 			to be processed
	 * @param bytes an array of byte arrays corresponding to the strengths
	 * @param bytescount an array of the size of the byte arrays
	 * @param count array of compression counters for each strength
	 * @param doFrench flag indicator if special handling of French has to be
	 * 					done
	 * @param hiragana4 offset for Hiragana quaternary
	 * @param commonBottom4 smallest common quaternary byte
	 * @param bottomCount4 smallest quaternary byte
	 */
	private synchronized final void getSortKeyBytes(String source, 
													boolean compare[], 
													byte bytes[][], 
													int bytescount[], 
													int count[],
													boolean doFrench,
													byte hiragana4, 
													int commonBottom4, 
													int bottomCount4)
	{
		int backupDecomposition = m_decomposition_;
		m_decomposition_ = NO_DECOMPOSITION; // have to revert to backup later
    	CollationElementIterator coleiter = 
    							new CollationElementIterator(source, this);
    	
		int frenchOffset[] = {-1, -1};
    	
    	// scriptorder not implemented yet 
    	// const uint8_t *scriptOrder = coll->scriptOrder;

		boolean doShift = false;
    	boolean notIsContinuation = false;

    	int leadPrimary = 0; // int for easier comparison
    	int caseShift = 0;
	    
    	while (true) {
        	int ce = coleiter.next();
            if (ce == CollationElementIterator.NULLORDER) {
            	break;
            }

            if (ce == CollationElementIterator.IGNORABLE) {
            	continue;
            }

            notIsContinuation = !isContinuation(ce);

            /*
             * if (notIsContinuation) {
            		if (scriptOrder != NULL) {
                		primary1 = scriptOrder[primary1];
              		}
            	}*/
            doShift = (m_isAlternateHandlingShifted_ 
            			&& ((notIsContinuation && ce <= m_variableTopValue_ 
    						 && (ce >> 24) != 0)) // primary byte not 0
    					|| (!notIsContinuation && doShift));
			leadPrimary = doPrimaryBytes(ce, bytes, bytescount, count, 
									notIsContinuation, doShift, leadPrimary, 
									commonBottom4, bottomCount4);
			if (compare[2]) {
        		doSecondaryBytes(ce, bytes, bytescount, count, 
        						 notIsContinuation,	doFrench, frenchOffset);
			}

			int t = ce & LAST_BYTE_MASK_;
			if (!notIsContinuation) {
              	t = ce & CE_REMOVE_CONTINUATION_MASK_;
            }
            	
            if (compare[0]) {
              	caseShift = doCaseBytes(t, bytes, bytescount, 
              							notIsContinuation, caseShift);
            }
            else if (notIsContinuation) {
                 t ^= m_caseSwitch_;
            }

            t &= m_mask3_;
              	
            if (compare[3]) {
            	doTertiaryBytes(t, bytes, bytescount, count, 
            					notIsContinuation);
            }
                
            if (compare[4] && notIsContinuation) { // compare quad
                doQuartenaryBytes(bytes, bytescount, count, 
                			 	coleiter.m_isCodePointHiragana_, 
                			 	commonBottom4, bottomCount4, hiragana4);
            }
        }
        m_decomposition_ = backupDecomposition; // reverts to original	
        if (frenchOffset[0] != -1) {
        	// one last round of checks
    		reverseBuffer(bytes[2], frenchOffset);
  		}
	}
	
	/**
	 * From the individual strength byte results the final compact sortkey 
	 * will be calculated.
	 * @param source text string
	 * @param compare array of flags indicating if a particular strength is 
	 * 			to be processed
	 * @param bytes an array of byte arrays corresponding to the strengths
	 * @param bytescount an array of the size of the byte arrays
	 * @param count array of compression counters for each strength
	 * @param doFrench flag indicating that special handling of French has to 
	 * 					be done
	 * @param commonBottom4 smallest common quaternary byte
	 * @param bottomCount4 smallest quaternary byte
	 * @return the compact sortkey
	 */
	private final byte[] getSortKey(String source, boolean compare[], 
									byte bytes[][], int bytescount[], 
									int count[], boolean doFrench, 
									int commonBottom4, int bottomCount4)
	{
		// we have done all the CE's, now let's put them together to form 
      	// a key 
      	if (compare[2]) {
        	doSecondary(bytes, bytescount, count, doFrench);
      		if (compare[0]) {
				doCase(bytes, bytescount);        
      		}
      		if (compare[3]) {
      			doTertiary(bytes, bytescount, count);
      			if (compare[4]) {
      				doQuaternary(bytes, bytescount, count, commonBottom4,
      							 bottomCount4);
        			if (compare[5]) {
          				doIdentical(source, bytes, bytescount);
        			}

      			}
      		}
      	}
      	append(bytes, bytescount, 1, (byte)0);
    	return bytes[1];
	}
	
	/**
	 * Packs the French bytes
	 * @param bytes array of byte arrays corresponding to strenghts
	 * @param bytescount array of the size of byte arrays
	 * @param count array of compression counts
	 */
	private final void doFrench(byte bytes[][], int bytescount[], int count[]) 
	{
		for (int i = 0; i < bytescount[2]; i ++) {
		    byte s = bytes[2][bytescount[2] - i - 1];
		    // This is compression code.
		    if (s == COMMON_2_) {
		      ++ count[2];
		    } 
		    else {
		      	if (count[2] > 0) {
		        	if (s > COMMON_2_) { // not necessary for 4th level.
		          		while (count[2] > TOP_COUNT_2_) {
		            		append(bytes, bytescount, 1, 
		            					(byte)(COMMON_TOP_2_ - TOP_COUNT_2_));
		            		count[2] -= TOP_COUNT_2_;
		          		}
		          		append(bytes, bytescount, 1, (byte)(COMMON_TOP_2_ 
		          											- (count[2] - 1)));
		        	} 
		        	else {
		          		while (count[2] > BOTTOM_COUNT_2_) {
		            		append(bytes, bytescount, 1,
		            			(byte)(COMMON_BOTTOM_2_ + BOTTOM_COUNT_2_));
		            		count[2] -= BOTTOM_COUNT_2_;
		          		}
		          		append(bytes, bytescount, 1, (byte)(COMMON_BOTTOM_2_ 
		          											+ (count[2] - 1)));
		        	}
		        	count[2] = 0;
		      	}
		      	append(bytes, bytescount, 1, s);
		    }
		}
		if (count[2] > 0) {
		    while (count[2] > BOTTOM_COUNT_2_) {
		      	append(bytes, bytescount, 1, (byte)(COMMON_BOTTOM_2_ 
		      										+ BOTTOM_COUNT_2_));
		      	count[2] -= BOTTOM_COUNT_2_;
		    }
		    append(bytes, bytescount, 1, (byte)(COMMON_BOTTOM_2_ 
		    											+ (count[2] - 1)));
		}
	}

	/**
	 * Compacts the secondary bytes and stores them into the primary array
	 * @param bytes array of byte arrays corresponding to the strengths
	 * @param bytecount array of the size of the byte arrays
	 * @param count array of the number of compression counts
	 * @param doFrench flag indicator that French has to be handled specially
	 */
	private final void doSecondary(byte bytes[][], int bytescount[], 
									int count[], boolean doFrench)
	{
		if (count[2] > 0) {
          	while (count[2] > BOTTOM_COUNT_2_) {
            	append(bytes, bytescount, 2, (byte)(COMMON_BOTTOM_2_ 
            											+ BOTTOM_COUNT_2_));
            	count[2] -= BOTTOM_COUNT_2_;
          	}
          	append(bytes, bytescount, 2, (byte)(COMMON_BOTTOM_2_ + 
          											(count[2] - 1)));
        }
        
        append(bytes, bytescount, 1, SORT_LEVEL_TERMINATOR_);
        
        if (doFrench) { // do the reverse copy
           	doFrench(bytes, bytescount, count);
        } 
        else {
        	if (bytes[1].length <= bytescount[1] + bytescount[2]) {
        		bytes[1] = increase(bytes[1], bytescount[1], bytescount[2]);
        	}
           	System.arraycopy(bytes[2], 0, bytes[1], bytescount[1], 
           					 bytescount[2]);
            bytescount[1] += bytescount[2];
        } 
	}
	
	/**
	 * Increase buffer size
	 * @param array array of bytes
	 * @param size of the byte array
	 * @param incrementsize size to increase
	 * @return the new buffer
	 */
	private static final byte[] increase(byte buffer[], int size, 
										 int incrementsize)
	{
		byte result[] = new byte[buffer.length + incrementsize];
		System.arraycopy(buffer, 0, result, 0, size);
		return result;
	}
	
	/**
	 * Increase buffer size
	 * @param array array of bytes
	 * @param size of the byte array
	 * @param incrementsize size to increase
	 * @return the new buffer
	 */
	private static final int[] increase(int buffer[], int size, 
										int incrementsize)
	{
		int result[] = new int[buffer.length + incrementsize];
		System.arraycopy(buffer, 0, result, 0, size);
		return result;
	}
	
	/**
	 * Compacts the case bytes and stores them into the primary array
	 * @param bytes array of byte arrays corresponding to the strengths
	 * @param bytecount array of the size of the byte arrays
	 */
	private final void doCase(byte bytes[][], int bytescount[])
	{
		append(bytes, bytescount, 1, SORT_LEVEL_TERMINATOR_);
		if (bytes[1].length <= bytescount[1] + bytescount[0]) {
			bytes[1] = increase(bytes[1], bytescount[1], bytescount[0]);
		}
		if (bytes[1].length <= bytescount[1] + bytescount[0]) {
        	bytes[1] = increase(bytes[1], bytescount[1], bytescount[0]);
        }
		System.arraycopy(bytes[0], 0, bytes[1], bytescount[1], bytescount[0]);
        bytescount[1] += bytescount[0];
	}
	
	/**
	 * Compacts the tertiary bytes and stores them into the primary array
	 * @param bytes array of byte arrays corresponding to the strengths
	 * @param bytecount array of the size of the byte arrays
	 * @param count array of the number of compression counts
	 */
	private final void doTertiary(byte bytes[][], int bytescount[], 
									int count[])
	{
		if (count[3] > 0) {
          	if (m_common3_ != COMMON_BOTTOM_3_) {
          		while (count[3] >= m_topCount3_) {
              		append(bytes, bytescount, 3, (byte)(m_top3_	
              												- m_topCount3_));
              		count[3] -= m_topCount3_;
            	}
            	append(bytes, bytescount, 3, (byte)(m_top3_ - count[3]));
          	} 
          	else {
          		while (count[3] > m_bottomCount3_) {
              		append(bytes, bytescount, 3, (byte)(m_bottom3_ 
              											+ m_bottomCount3_));
              		count[3] -= m_bottomCount3_;
            	}
            	append(bytes, bytescount, 3, (byte)(m_bottom3_ 
            											+ (count[3] - 1)));
          	}
        }
        append(bytes, bytescount, 1, SORT_LEVEL_TERMINATOR_);
        if (bytes[1].length <= bytescount[1] + bytescount[3]) {
        	bytes[1] = increase(bytes[1], bytescount[1], bytescount[3]);
        }
        System.arraycopy(bytes[3], 0, bytes[1], bytescount[1], bytescount[3]);
        bytescount[1] += bytescount[3];
	}
	
	/**
	 * Compacts the quaternary bytes and stores them into the primary array
	 * @param bytes array of byte arrays corresponding to the strengths
	 * @param bytecount array of the size of the byte arrays
	 * @param count array of compression counts
	 */
	private final void doQuaternary(byte bytes[][], int bytescount[], 
									int count[], int commonbottom4, 
									int bottomcount4)
	{
		if (count[4] > 0) {
            while (count[4] > bottomcount4) {
                append(bytes, bytescount, 4, (byte)(commonbottom4 
                									+ bottomcount4));
                count[4] -= bottomcount4;
            }
            append(bytes, bytescount, 4, (byte)(commonbottom4
            									+ (count[4] - 1)));
        }
        append(bytes, bytescount, 1, SORT_LEVEL_TERMINATOR_);
        if (bytes[1].length <= bytescount[1] + bytescount[4]) {
        	bytes[1] = increase(bytes[1], bytescount[1], bytescount[4]);
        }
        System.arraycopy(bytes[4], 0, bytes[1], bytescount[1], bytescount[4]);
        bytescount[1] += bytescount[4];
	}
	
	/**
	 * Deals with the identical sort.
	 * Appends the BOCSU version of the source string to the ends of the
	 * byte buffer.
	 * @param source text string
	 * @param bytes array of a byte array corresponding to the strengths
	 * @param bytescount array of the byte array size
	 */
	private final void doIdentical(String source, byte bytes[][], 
								   int bytescount[])
	{
		int isize = BOSCU.lengthOfIdenticalLevelRun(source);
		append(bytes, bytescount, 1, SORT_LEVEL_TERMINATOR_);
		if (bytes[1].length <= bytescount[1] + isize) {
        	bytes[1] = increase(bytes[1], bytescount[1], 1 + isize);
        }
        BOSCU.writeIdenticalLevelRun(source, bytes[1], bytescount[1]); 
	}
	
	/**
	 * Gets the offset of the first unmatched characters in source and target.
	 * This method returns the offset of the start of a contraction or a 
	 * combining sequence, if the first difference is in the middle of such a 
	 * sequence.
	 * @param source string
	 * @param target string
	 * @return offset of the first unmatched characters in source and target.
	 */
	private final int getFirstUnmatchedOffset(String source, String target)
	{
		int result = 0;
		while (source.charAt(result) == target.charAt(result) 
				&& source.charAt(result) != 0) {
			result ++;
	    }
	    if (result > 0) {
	        // There is an identical portion at the beginning of the two 
	        // strings. If the identical portion ends within a contraction or a 
	        // combining character sequence, back up to the start of that 
	        // sequence.              
	        char schar = source.charAt(result); // first differing chars   
	        char tchar = target.charAt(result);
	        if (schar != 0 && isUnsafe(schar) || tchar != 0 && isUnsafe(tchar))
	        {
	            // We are stopped in the middle of a contraction or combining
	            // sequence.
	            // Look backwards for the part of the string for the start of 
	            // the sequence
	            // It doesn't matter which string we scan, since they are the 
	            // same in this region.
	            do {
	                result --;
	            }
	            while (result > 0 && isUnsafe(source.charAt(result)));
	        }
	    }
	    return result;
	}
	
	/**
	 * Appending an byte to an array of bytes and increases it if we run out of 
	 * space
	 * @param array of byte arrays
	 * @param array of the end offsets corresponding to array
	 * @param appendarrayindex of the int array to append
	 * @param value to append
	 */
	private static final void append(byte array[][], int arrayoffset[], 
										int appendarrayindex, byte value)
	{
		if (arrayoffset[appendarrayindex] + 1 
			>= array[appendarrayindex].length) {
			array[appendarrayindex] = increase(array[appendarrayindex],	
											   arrayoffset[appendarrayindex],
											   SORT_BUFFER_INIT_SIZE_);
		}			
		array[appendarrayindex][arrayoffset[appendarrayindex]] = value;
		arrayoffset[appendarrayindex] ++;
	}
	
	/** 
	 * This is a trick string compare function that goes in and uses sortkeys 
	 * to compare. It is used when compare gets in trouble and needs to bail 
	 * out.
	 * @param source text string
	 * @param target text string          
	 */
	private final int compareBySortKeys(String source, String target)
	{
	    CollationKey sourcekey = getCollationKey(source);
	    CollationKey targetkey = getCollationKey(target);	
	    return sourcekey.compareTo(targetkey);
	}
	
	/**
	 * Performs the primary comparisons, and fills up the CE buffer at the
	 * same time. 
	 * The return value toggles between the comparison result and the hiragana
	 * result. If either the source is greater than target or vice versa, the 
	 * return result is the comparison result, ie 1 or -1, furthermore the
	 * cebuffers will be cleared when that happens. If the primary comparisons
	 * are equal, we'll have to continue with secondary comparison. In this case
	 * the cebuffer will not be cleared and the return result will be the 
	 * hiragana result.
	 * @param doHiragana4 flag indicator that Hiragana Quaternary has to be 
	 * 					observed
	 * @param lowestpvalue the lowest primary value that will not be ignored if 
	 * 						alternate handling is shifted
	 * @param source text string
	 * @param target text string
	 * @param textoffset offset in text to start the comparison
	 * @param cebuffer array of CE buffers to populate, offset 0 for source, 
	 * 					1 for target, cleared when a primary difference is 
	 * 					found.
	 * @param cebuffersize array of CE buffer size corresponding to the 
	 * 						cebuffer, 0 when a primary difference is found.
	 * @return comparion result if a primary difference is found, otherwise
	 * 						hiragana result
	 */
	private final int doPrimaryCompare(boolean doHiragana4, int lowestpvalue,
										String source, String target, 
										int textoffset, int cebuffer[][], 
									   	int cebuffersize[])
	{
		// Preparing the context objects for iterating over strings
	    UCharacterIterator siter = new UCharacterIterator(source, textoffset, 
	    													source.length());
	    CollationElementIterator scoleiter = new CollationElementIterator(
	    														siter, this);
	    UCharacterIterator titer = new UCharacterIterator(target, textoffset, 
	    													target.length());
	    CollationElementIterator tcoleiter = new CollationElementIterator(
	    														titer, this);
		
		// Non shifted primary processing is quite simple
	    if (!m_isAlternateHandlingShifted_) {
	    	int hiraganaresult = 0;    														
	      	while (true) {
	      		int sorder = 0;
				// We fetch CEs until we hit a non ignorable primary or end.
	        	do {
	          		sorder = scoleiter.next();
	          		append(cebuffer, cebuffersize, 0, sorder);
	          		sorder &= CE_PRIMARY_MASK_;
	        	} while (sorder == CollationElementIterator.IGNORABLE);
	
				int torder = 0;
	        	do {
	          		torder = tcoleiter.next();
	          		append(cebuffer, cebuffersize, 1, torder);
	          		torder &= CE_PRIMARY_MASK_;
	        	} while (torder == CollationElementIterator.IGNORABLE);
	
	        	// if both primaries are the same
	        	if (sorder == torder) {
	            	// and there are no more CEs, we advance to the next level
	            	if (cebuffer[0][cebuffersize[0] - 1] 
	            					== CollationElementIterator.NULLORDER) {
	              		break;
	            	}
	            	if (doHiragana4 && hiraganaresult == 0 
	            		&& scoleiter.m_isCodePointHiragana_ !=
	              						tcoleiter.m_isCodePointHiragana_) {
	              		if (scoleiter.m_isCodePointHiragana_) {
	                		hiraganaresult = -1;
	              		}
	              		else {
	              			hiraganaresult = 1;
	              		}
	            	}
	        	} 
	        	else {
	            	// if two primaries are different, we are done
	            	return endCompare(sorder, torder, cebuffer, cebuffersize);
	        	}
	      	} 
	      	// no primary difference... do the rest from the buffers
	      	return hiraganaresult;
	    } 
	    else { // shifted - do a slightly more complicated processing :)
	      	while (true) {
	        	int sorder = getPrimaryShiftedCompareCE(scoleiter, lowestpvalue, 
	        										cebuffer, cebuffersize, 0);
				int torder = getPrimaryShiftedCompareCE(tcoleiter, lowestpvalue, 
													cebuffer, cebuffersize, 1);
	        	if (sorder == torder) {
	            	if (cebuffer[0][cebuffersize[0] - 1] 
	            			== CollationElementIterator.NULLORDER) {
	              		break;
	            	} 
	            	else {
	              		continue;
	            	}
	        	} 
	        	else {
	    			return endCompare(sorder, torder, cebuffer, cebuffersize);
	        	}
	      	} // no primary difference... do the rest from the buffers
	    }
		return 0;
	}
	
	/**
	 * This is used only when we know that sorder is already different from
	 * torder.
	 * Compares sorder and torder, returns -1 if sorder is less than torder.
	 * Clears the cebuffer at the same time.
	 * @param sorder source strength order
	 * @param torder target strength order
	 * @param cebuffer array of buffers containing the ce values
	 * @param cebuffersize array of cebuffer offsets
	 * @return the comparison result of sorder and torder
	 */
	private static final int endCompare(int sorder, int torder, 
										int cebuffer[][], int cebuffersize[])
	{
		cebuffer[0] = null;
	    cebuffer[1] = null;
	    cebuffersize[0] = 0;
	    cebuffersize[1] = 0;
	    if (sorder < torder) {
	    	return -1;
	    }
	    return 1;
	}
	
	/**
	 * Calculates the next primary shifted value and fills up cebuffer with the 
	 * next non-ignorable ce.
	 * @param coleiter collation element iterator
	 * @param doHiragana4 flag indicator if hiragana quaternary is to be 
	 * 						handled
	 * @param lowestpvalue lowest primary shifted value that will not be 
	 * 						ignored
	 * @param cebuffer array of buffers to append with the next ce
	 * @param cebuffersize array of offsets corresponding to the cebuffer
	 * @param cebufferindex index of the buffer to append to
	 * @return result next modified ce 
	 */
	private final static int getPrimaryShiftedCompareCE(
										CollationElementIterator coleiter,
										int lowestpvalue, int cebuffer[][], 
										int cebuffersize[],	int cebufferindex)
	{
		boolean shifted = false;
		int result = CollationElementIterator.IGNORABLE;
	    while (true) {
	        result = coleiter.next();
	        if (result == CollationElementIterator.NULLORDER) {
	            append(cebuffer, cebuffersize, cebufferindex, result);
	            break;
	        } 
	        else if (result == CollationElementIterator.IGNORABLE) {
	            continue;
	        } 
	        else if (isContinuation(result)) {
	        	if ((result & CE_PRIMARY_MASK_) 
	            					!= CollationElementIterator.IGNORABLE) { 
	            	// There is primary value
	              	if (shifted) {
	                	result = (result & CE_PRIMARY_MASK_) 
	                						| CE_CONTINUATION_MARKER_; 
	                	// preserve interesting continuation
	                	append(cebuffer, cebuffersize, cebufferindex, result);
	           			continue;
	        		} 
	        		else {
	           			append(cebuffer, cebuffersize, cebufferindex, result);
	                	break;
	              	}
	            } 
	            else { // Just lower level values
	            	if (!shifted) {
	            		append(cebuffer, cebuffersize, cebufferindex, result);
	              	}
	            }
	        } 
	        else { // regular
	        	if ((result & CE_PRIMARY_MASK_) > lowestpvalue) {
	             	append(cebuffer, cebuffersize, cebufferindex, result);
	             	break;
	            } 
	            else {
	            	if ((result & CE_PRIMARY_MASK_) > 0) {
	                	shifted = true;
	                	result &= CE_PRIMARY_MASK_;
	                	append(cebuffer, cebuffersize, cebufferindex, result);
	                	continue;
	              	} 
	              	else {
	                	append(cebuffer, cebuffersize, cebufferindex, result);
	                	shifted = false;
	                	continue;
	              	}
	            }
	        }
	    }
	    result &= CE_PRIMARY_MASK_;
	    return result;
	}
							
	/**
	 * Appending an int to an array of ints and increases it if we run out of 
	 * space
	 * @param array of int arrays
	 * @param array of the end offsets corresponding to array
	 * @param appendarrayindex of the int array to append
	 * @param value to append
	 */
	private static final void append(int array[][], int arrayoffset[], 
										int appendarrayindex, int value)
	{
		if (arrayoffset[appendarrayindex] + 1 
			>= array[appendarrayindex].length) {
			array[appendarrayindex] = increase(array[appendarrayindex],
												arrayoffset[appendarrayindex],		
												CE_BUFFER_SIZE_);
		}			
		array[appendarrayindex][arrayoffset[appendarrayindex]] = value;
		arrayoffset[appendarrayindex] ++;
	}
	
	/**
	 * Does secondary strength comparison based on the collected ces.
	 * @param cebuffer array of int arrays that contains the collected ces
	 * @param cebuffersize array of offsets corresponding to the cebuffer,
	 *							indicates the offset of the last ce in buffer
	 * @param doFrench flag indicates if French ordering is to be done
	 * @return the secondary strength comparison result
	 */
	private static final int doSecondaryCompare(int cebuffer[][], 
												int cebuffersize[],
												boolean doFrench)
	{
		// now, we're gonna reexamine collected CEs
	    if (!doFrench) { // normal
	    	int offset = 0;
	        while (true) {
	        	int sorder = CollationElementIterator.IGNORABLE;
	          	while (sorder == CollationElementIterator.IGNORABLE) {
	            	sorder = cebuffer[0][offset ++] & CE_SECONDARY_MASK_;
	          	}
				int torder = CollationElementIterator.IGNORABLE;
	          	while (torder == CollationElementIterator.IGNORABLE) {
	          		torder = cebuffer[1][offset ++] & CE_SECONDARY_MASK_;
	          	}
	
	          	if (sorder == torder) {
	            	if (cebuffer[0][offset - 1]  
	            					== CollationElementIterator.NULLORDER) {
	              		break;
	            	}
	          	} 
	          	else {
	               	return (sorder < torder) ? -1 : 1;
	          	}
	        }
	    } 
	    else { // do the French 
	    	int continuationoffset[] = {0, 0};
	    	int offset[] = {cebuffersize[0] - 2, cebuffersize[1] - 2} ; 
	        while (true) {
	        	int sorder = getSecondaryFrenchCE(cebuffer, offset, 
	        										continuationoffset, 0);
	        	int torder = getSecondaryFrenchCE(cebuffer, offset, 
	        										continuationoffset,1);
	          	if (sorder == torder) {
	            	if (cebuffer[0][offset[0] - 1] 
	            						== CollationElementIterator.NULLORDER	            					 
	            		|| (offset[0] < 0 && offset[1] < 0)) {
	              		break;
	            	} 
	          	} 
	          	else {
	              	return (sorder < torder) ? -1 : 1;
	          	}
	        }
	    }
	    return 0;
	}
	
	/**
	 * Calculates the next secondary french CE.
	 * @param cebuffer array of buffers to append with the next ce
	 * @param offset array of offsets corresponding to the cebuffer
	 * @param continuationoffset index of the start of a continuation
	 * @param index of cebuffer to use
	 * @return result next modified ce
	 */
	private static final int getSecondaryFrenchCE(int cebuffer[][], 
												  int offset[], 
												  int continuationoffset[],
												  int index)
	{
		int result = CollationElementIterator.IGNORABLE;
	    while (result == CollationElementIterator.IGNORABLE 
	    		&& offset[index] >= 0) {
	        if (continuationoffset[index] == 0) {
	        	while (isContinuation(cebuffer[0][offset[index] --]));
	            // after this, sorder is at the start of continuation, 
	            // and offset points before that 
	            if (isContinuation(cebuffer[0][offset[index] + 1])) {
	            	// save offset for later
	            	continuationoffset[index] = offset[index]; 
	            	offset[index] += 2;  
	           	}
	        }
	        else {
	        	result = cebuffer[0][offset[index] ++];
	        	if (!isContinuation(result)) { 
	        		// we have finished with this continuation
	           		offset[index] = continuationoffset[index];
	           		// reset the pointer to before continuation 
	           		continuationoffset[index] = 0;
	           		continue;
	        	}
	        }
	        result &= CE_SECONDARY_MASK_; // remove continuation bit        
	    }    
	    return result;
	}
	
	/**
	 * Does case strength comparison based on the collected ces.
	 * @param cebuffer array of int arrays that contains the collected ces
	 * @return the case strength comparison result
	 */
	private final int doCaseCompare(int cebuffer[][])
	{
		int sorder = CollationElementIterator.IGNORABLE;
		int torder = CollationElementIterator.IGNORABLE;
		int soffset = 0;
		int toffset = 0;
	    while (true) {
	        while ((sorder & CE_REMOVE_CASE_) 
	        						== CollationElementIterator.IGNORABLE) {
	        	sorder = cebuffer[0][soffset ++];
	          	if (!isContinuation(sorder)) {
	            	sorder &= CE_CASE_MASK_3_;
	            	sorder ^= m_caseSwitch_;
	          	} 
	          	else {
	            	sorder = CollationElementIterator.IGNORABLE;
	          	}
	        }
	
	        while ((torder & CE_REMOVE_CASE_) 
	        						== CollationElementIterator.IGNORABLE) {
	        	torder = cebuffer[1][toffset ++];
	          	if (!isContinuation(sorder)) {
	            	torder &= CE_CASE_MASK_3_;
	            	torder ^= m_caseSwitch_;
	          	} 
	          	else {
	            	torder = CollationElementIterator.IGNORABLE;
	          	}
	        }
	
	        if ((sorder & CE_CASE_BIT_MASK_) < (torder & CE_CASE_BIT_MASK_)) {
	          	return -1;
	        } 
	        else if ((sorder & CE_CASE_BIT_MASK_) 
	        							> (torder & CE_CASE_BIT_MASK_)) {
	          	return 1;
	        }
	
	        if (cebuffer[0][soffset - 1] == CollationElementIterator.NULLORDER) 
	        {
	          	break;
	        } 
	        else {
	          	sorder = CollationElementIterator.IGNORABLE;
	          	torder = CollationElementIterator.IGNORABLE;
	        }
	    }
	    return 0;
	}
	
	/**
	 * Does tertiary strength comparison based on the collected ces.
	 * @param cebuffer array of int arrays that contains the collected ces
	 * @return the tertiary strength comparison result
	 */
	private final int doTertiaryCompare(int cebuffer[][])
	{
		int soffset = 0;
	    int toffset = 0;
	    while (true) {
	    	int sorder = CollationElementIterator.IGNORABLE;
	    	int torder = CollationElementIterator.IGNORABLE;
	        while ((sorder & CE_REMOVE_CASE_) 
	        					== CollationElementIterator.IGNORABLE) {
	          	sorder = cebuffer[0][soffset ++] & m_mask3_;
	          	if (!isContinuation(sorder)) {
	            	sorder ^= m_caseSwitch_;
	          	} 
	          	else {
	            	sorder &= CE_REMOVE_CASE_;
	          	}
	        }
	
			while ((torder & CE_REMOVE_CASE_) 
	        					== CollationElementIterator.IGNORABLE) {
	          	torder = cebuffer[1][toffset ++] & m_mask3_;
	          	if (!isContinuation(torder)) {
	            	torder ^= m_caseSwitch_;
	          	} 
	          	else {
	            	torder &= CE_REMOVE_CASE_;
	          	}
	        }        
	
	        if (sorder == torder) {
	          	if (cebuffer[0][soffset - 1] 
	          		== (CollationElementIterator.NULLORDER & CE_REMOVE_CASE_)) {
	            	break;
	          	} 
	        } 
	        else {
	            return (sorder < torder) ? -1 : 1;
	        }
	    }
	    return 0;
	}
	
	/**
	 * Does quaternary strength comparison based on the collected ces.
	 * @param cebuffer array of int arrays that contains the collected ces
	 * @param lowestpvalue the lowest primary value that will not be ignored if 
	 * 						alternate handling is shifted
	 * @return the quaternary strength comparison result
	 */
	private final int doQuaternaryCompare(int cebuffer[][], int lowestpvalue)
	{
		boolean sShifted = true;
	    boolean tShifted = true;
	    int soffset = 0;
	    int toffset = 0;
	    while (true) {
	    	int sorder = CollationElementIterator.IGNORABLE;
	    	int torder = CollationElementIterator.IGNORABLE;
	        while (sorder == CollationElementIterator.IGNORABLE 
	        		&& sorder != CollationElementIterator.NULLORDER 
	        		|| (isContinuation(sorder) && !sShifted)) {
	          	sorder = cebuffer[0][soffset ++];
	          	if (isContinuation(sorder)) {
	            	if (!sShifted) {
	              		continue;
	            	}
	          	} 
	          	else if (sorder > lowestpvalue 
	          				|| (sorder & CE_PRIMARY_MASK_) 
	          						== CollationElementIterator.IGNORABLE) { 
	          		// non continuation
	            	sorder = CE_PRIMARY_MASK_;
	            	sShifted = false;
	          	} 
	          	else {
	            	sShifted = true;
	          	}
	        }
	        sorder &= CE_PRIMARY_MASK_;
			while (torder == CollationElementIterator.IGNORABLE 
	        		&& torder != CollationElementIterator.NULLORDER 
	        		|| (isContinuation(torder) && !tShifted)) {
	          	torder = cebuffer[0][toffset ++];
	          	if (isContinuation(torder)) {
	            	if (!tShifted) {
	              		continue;
	            	}
	          	} 
	          	else if (torder > lowestpvalue 
	          				|| (torder & CE_PRIMARY_MASK_) 
	          						== CollationElementIterator.IGNORABLE) { 
	          		// non continuation
	            	torder = CE_PRIMARY_MASK_;
	            	tShifted = false;
	          	} 
	          	else {
	            	tShifted = true;
	          	}
	        }
	        torder &= CE_PRIMARY_MASK_;
	
	        if (sorder == torder) {
	          	if (cebuffer[0][soffset -1] 
	          		== CollationElementIterator.NULLORDER) {
	            	break;
	          	}
	        } 
	        else {
	            return (sorder < torder) ? -1 : 1;
	        }
	    }
	    return 0;
	}
	
	/**  
	 * Internal function. Does byte level string compare. Used by strcoll if 
	 * strength == identical and strings are otherwise equal. This is a rare 
	 * case. Comparison must be done on NFD normalized strings. FCD is not good 
	 * enough.
	 * @param source text
	 * @param target text
	 * @param offset of the first difference in the text strings
	 * @param normalize flag indicating if we are to normalize the text before
	 * 				comparison
	 * @return 1 if source is greater than target, -1 less than and 0 if equals
	 */
	private static final int doIdenticalCompare(String source, String target, 
												int offset, boolean normalize)
	{
	    if (normalize) {
	        /*
	        if (unorm_quickCheck(sColl->string, sLen, UNORM_NFD) != UNORM_YES) {
	            source = unorm_decompose(sColl->writableBuffer, 
	            							sColl->writableBufSize,
	                                   		sBuf, sLen, FALSE, FALSE);
	        }
	
	        if (unorm_quickCheck(tColl->string, tLen, UNORM_NFD) != UNORM_YES) {
	            target = unorm_decompose(tColl->writableBuffer, 
	            							tColl->writableBufSize,
	                                   		tBuf, tLen, FALSE, FALSE);
	        }
	        */
	        offset = 0;
	    }
	
	    return doStringCompare(source, target, offset);
	}
	
	/**
	 * Compares string for their codepoint order.
	 * This comparison handles surrogate characters and place them after the 
	 * all non surrogate characters.
	 * @param source text
	 * @param target text
	 * @param offset start offset for comparison
	 * @return 1 if source is greater than target, -1 less than and 0 if equals
	 */
	private static final int doStringCompare(String source, 
											 String target,
											 int offset) 
	{
    	// compare identical prefixes - they do not need to be fixed up
    	char schar = 0;
    	char tchar = 0;
    	while (true) {
        	schar = source.charAt(offset);
        	tchar = target.charAt(offset ++);
        	if (schar != tchar) {
            	break;
        	}
        	if (schar == 0) {
            	return 0;
        	}
    	}

   		//  if both values are in or above the surrogate range, Fix them up.
   		if (schar >= UTF16.LEAD_SURROGATE_MIN_VALUE 
   			&& tchar >= UTF16.LEAD_SURROGATE_MIN_VALUE) {
        	schar = fixupUTF16(schar);
        	tchar = fixupUTF16(tchar);
    	}

    	// now c1 and c2 are in UTF-32-compatible order
    	return (schar < tchar) ? -1 : 1; // schar and tchar has to be different
	}
	
	/** 
	 * Rotate surrogates to the top to get code point order
	 */
	private static final char fixupUTF16(char ch) 
	{                  
    	if (ch >= 0xe000) {                 
        	ch -= 0x800;                    
    	} 
    	else {                             
        	ch += 0x2000;                 
    	}     
    	return ch;                             
	}
}
