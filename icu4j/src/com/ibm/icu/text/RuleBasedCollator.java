/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RuleBasedCollator.java,v $ 
* $Date: 2002/06/21 23:56:47 $ 
* $Revision: 1.8 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.text.ParseException;
import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.impl.Trie;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.ICULocaleData;

/**
 * <p>
 * The RuleBasedCollator class is a concrete subclass of Collator. It allows
 * customization of the Collator via user specified rule sets. 
 * RuleBasedCollator is designed to be fully compliant to the 
 * <a href = http://www.unicode.org/unicode/reports/tr10/>
 * Unicode Collation Algorithm (UCA)</a> and conforms to ISO 14651. 
 * </p>
 * <p>
 * Users are strongly encouraged to read  
 * <a href="http://oss.software.ibm.com/icu/userguide/Collate_Intro.html">
 * the users guide</a> for more information about the collation service before
 * using this class.
 * </p>
 * <p>
 * Create a RuleBasedCollator from a locale by calling the getInstance(Locale) 
 * factory method in the base class Collator. 
 * Collator.getInstance(Locale) creates a RuleBasedCollator object based on the 
 * collation rules defined by the argument locale.
 * If a customized collation ordering ar attributes is required, use the 
 * RuleBasedCollator(String) constructor with the appropriate rules. The 
 * customized RuleBasedCollator will base its ordering on UCA, while 
 * re-adjusting the attributes and orders of the characters in the specified 
 * rule accordingly. 
 * <p>
 * RuleBasedCollator provides correct collation orders for most locales 
 * supported in ICU. If specific data for a locale is not available, the orders 
 * eventually falls back to the 
 * <a href="http://www.unicode.org/unicode/reports/tr10/">UCA collation order
 * </a>. 
 * </p>
 * <p>
 * For information about the collation rule syntax to use and details about
 * customization, please refer to the 
 * <a href="http://oss.software.ibm.com/icu/userguide/Collate_Customization.html">
 * Collation customization</a> section of the users guide.
 * </p>
 * <p>
 * Note that there are some differences between the Collation rule syntax
 * used in Java and ICU4J
 * <ul>
 * <li>According to the JDK documentation:
 * <i>
 * <p>
 * Modifier '!' : Turns on Thai/Lao vowel-consonant swapping. If this rule 
 * is in force when a Thai vowel of the range &#92;U0E40-&#92;U0E44 precedes a 
 * Thai consonant of the range &#92;U0E01-&#92;U0E2E OR a Lao vowel of the 
 * range &#92;U0EC0-&#92;U0EC4 precedes a Lao consonant of the range 
 * &#92;U0E81-&#92;U0EAE then the 
 * vowel is placed after the consonant for collation purposes. 
 * </p>
 * <p>
 * If a rule is without the modifier '!', the Thai/Lao vowel-consonant 
 * swapping is not turned on.
 * </p>
 * </i>
 * <p>
 * ICU4J's RuleBasedCollator does not support turning off the Thai/Lao 
 * vowel-consonant swapping, since the UCA clearly states that it has to be 
 * supported to ensure a correct sorting order. If a '!' is encountered, it is
 * ignored.
 * </p>
 * <li>According to the JDK documentation:
 * <i>
 * <p>
 * If, however, the first relation is not "&lt;", then all the all 
 * text-arguments up to the first "&lt;" are ignorable. For example, 
 * ", - &lt; a &lt; b" makes "-" an ignorable character, as we saw earlier in 
 * the word "black-birds".
 * </p>
 * </i>
 * <p>
 * The above allows random characters before the first '&lt;' not in any 
 * specific sequence to be ignorable. ICU4J does not support this feature.
 * To define ignorable characters in PRIMARY to TERTIARY strength, users can 
 * use the rule "& X &lt; [variable top]" to set the variable top to the 
 * PRIMARY strength of "X". Once alternate handling is set to shifted 
 * (setAlternateHandling(true)), the Collator using strengths PRIMARY, 
 * SECONDARY or TERTIARY will ignore all code points with PRIMARY strengths 
 * less than variable top.    
 * See the user guide's section on 
 * <a href=http://www-124.ibm.com/icu/userguide/Collate_Customization.html>
 * Collation Customization</a> for details.
 * </p>
 * <li>As mentioned in the documentation of the base class Collator, 
 *     compatibility decomposition mode is not supported.
 * </ul>
 * </p>
 * <p>
 * <strong>Examples</strong>
 * </p>
 * <p>
 * Creating Customized RuleBasedCollators
 * <blockquote>
 * <pre>
 * String Simple = "&amp; a &lt; b &lt; c &lt; d";
 * RuleBasedCollator mySimple = new RuleBasedCollator(Simple);
 * 
 * String Norwegian = "&amp; a , A &lt; b , B &lt; c , C &lt; d , D &lt; e , E "
 *                    + "&lt; f , F &lt; g , G &lt; h , H &lt; i , I &lt; j , "
 *                    + "J &lt; k , K &lt; l , L &lt; m , M &lt; n , N &lt; "
 *                    + "o , O &lt; p , P &lt; q , Q &lt r , R &lt s , S &lt; "
 *                    + "t , T &lt; u , U &lt; v , V &lt; w , W &lt; x , X "
 *                    + "&lt; y , Y &lt; z , Z &lt; &#92;u00E5 = a&#92;u030A "
 *                    + ", &#92;u00C5 = A&#92;u030A ; aa , AA &lt; &#92;u00E6 "
 *                    + ", &#92;u00C6 &lt; &#92;u00F8 , &#92;u00D8";
 * RuleBasedCollator myNorwegian = new RuleBasedCollator(Norwegian);
 * </pre>
 * </blockquote>
 * Concatenating rules to combining <code>Collator</code>s
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
 *                             new RuleBasedCollator(en_USRules + da_DKRules);
 * // newCollator has the combined rules
 * </pre>
 * </blockquote>
 * Making changes on an existing RuleBasedCollator to create a new 
 * <code>Collator</code> object, by appending the existing rule with the 
 * changes.
 * <blockquote>
 * <pre>
 * // Create a new Collator object with additional rules
 * String addRules = "&amp; C &lt; ch, cH, Ch, CH";
 * RuleBasedCollator myCollator =
 *     new RuleBasedCollator(en_USCollator + addRules);
 * // myCollator contains the new rules
 * </pre>
 * </blockquote>
 * The following example demonstrates how to change the order of
 * non-spacing accents,
 * <blockquote>
 * <pre>
 * // old rule with main accents
 * String oldRules = "= &#92;u0301 ; &#92;u0300 ; &#92;u0302 ; &#92;u0308 "    
 *                 + "; &#92;u0327 ; &#92;u0303 ; &#92;u0304 ; &#92;u0305 "
 *                 + "; &#92;u0306 ; &#92;u0307 ; &#92;u0309 ; &#92;u030A " 
 *                 + "; &#92;u030B ; &#92;u030C ; &#92;u030D ; &#92;u030E "
 *                 + "; &#92;u030F ; &#92;u0310 ; &#92;u0311 ; &#92;u0312 "
 *                 + "&lt; a , A ; ae, AE ; &#92;u00e6 , &#92;u00c6 "
 *                 + "&lt; b , B &lt; c, C &lt; e, E &amp; C &lt; d , D";
 * // change the order of accent characters
 * String addOn = "&amp; &#92;u0300 ; &#92;u0308 ; &#92;u0302";
 * RuleBasedCollator myCollator = new RuleBasedCollator(oldRules + addOn);
 * </pre>
 * </blockquote>
 * Putting new primary ordering in before the default setting, 
 * e.g. Sort English characters before or after Japanese characters in Japanese 
 * <code>Collator</code>.
 * <blockquote>
 * <pre>
 * // get en_US Collator rules
 * RuleBasedCollator en_USCollator 
 *                        = (RuleBasedCollator)Collator.getInstance(Locale.US);
 * // add a few Japanese character to sort before English characters
 * // suppose the last character before the first base letter 'a' in
 * // the English collation rule is &#92;u2212
 * String jaString = "& &#92;u2212 &lt &#92;u3041, &#92;u3042 &lt &#92;u3043, "
 *                   + "&#92;u3044";
 * RuleBasedCollator myJapaneseCollator 
 *              = new RuleBasedCollator(en_USCollator.getRules() + jaString);
 * </pre>
 * </blockquote>
 * </p>
 * @author Syn Wee Quek
 * @since release 2.2, April 18 2002
 * @draft 2.2
 */
public final class RuleBasedCollator extends Collator 
{   
	// public data members ---------------------------------------------------
	
	// public constructors ---------------------------------------------------
	
	/**
     * <p>
     * RuleBasedCollator constructor that takes the argument rules for 
     * customization. RuleBasedCollator constructed will be based on UCA, 
     * with the attributes and re-ordering of the characters specified in the 
     * argument rules.
     * </p>
     * <p>See the user guide's section on 
     * <a href=http://www-124.ibm.com/icu/userguide/Collate_Customization.html>
     * Collation Customization</a> for details on the rule syntax.
     * </p>
     * @param rules the collation rules to build the collation table from.
     * @exception ParseException and IOException thrown. ParseException thrown 
     *            when argument rules have an invalid syntax. IOException 
     *            thrown when an error occured while reading internal data.
     * @draft 2.2
     */
    public RuleBasedCollator(String rules) throws Exception
    {
        if (rules == null) {
            throw new IllegalArgumentException(
                                            "Collation rules can not be null");
        }
    	setWithUCAData();
        CollationParsedRuleBuilder builder 
                                       = new CollationParsedRuleBuilder(rules);
	    
		builder.setRules(this);
        m_rules_ = rules;
        init();
    }
    
	// public methods --------------------------------------------------------
    
    /**
     * Return a CollationElementIterator for the given String.
     * @see CollationElementIterator
     * @draft 2.2
     */
    public CollationElementIterator getCollationElementIterator(String source) 
    {
        return new CollationElementIterator(source, this);
    }

    /**
     * Return a CollationElementIterator for the given CharacterIterator.
     * Argument source's integrity will be preserved since a new copy of source 
     * will be created for use instead.
     * @see CollationElementIterator
     * @draft 2.2
     */
    public CollationElementIterator getCollationElementIterator(
                                                CharacterIterator source) 
    {	
     	CharacterIterator newsource = (CharacterIterator)source.clone();   
        return new CollationElementIterator(source, this);
    }
    
    // public setters --------------------------------------------------------
    
    /**
	 * Sets the Hiragana Quaternary mode to be on or off.
	 * When the Hiragana Quaternary mode turned on, the RuleBasedCollator 
	 * positions Hiragana characters before all non-ignorable characters in 
	 * QUATERNARY strength. This is to produce a correct JIS collation order,
	 * distinguishing between Katakana  and Hiragana characters. 
	 * @param flag true if Hiragana Quaternary mode is to be on, false 
	 *        otherwise
	 * @see #setHiraganaQuaternaryDefault
	 * @see #isHiraganaQuaternary
	 * @draft 2.2
	 */
	public void setHiraganaQuaternary(boolean flag)
	{
		m_isHiragana4_ = flag;
	}
	
	/**
	 * Sets the Hiragana Quaternary mode to the initial mode set during 
	 * construction of the RuleBasedCollator.
	 * See setHiraganaQuaternary(boolean) for more details.
	 * @see #setHiraganaQuaternary(boolean)
	 * @see #isHiraganaQuaternary
	 * @draft 2.2
	 */
	public void setHiraganaQuaternaryDefault()
	{
		m_isHiragana4_ = m_defaultIsHiragana4_;
	}
	
	/**
   	 * Sets the orders of upper cased characters to sort before lower cased 
   	 * characters or vice versa, in strength TERTIARY. The default 
   	 * mode is false, and that sorts lower cased characters before upper cased 
   	 * characters. 
   	 * If true is set, the RuleBasedCollator will sort upper cased characters 
   	 * before the lower cased ones.
   	 * @param upperfirst true for sorting upper cased characters before 
   	 *                   lower cased characters, false for sorting lower cased 
   	 *                   characters before upper cased characters 
   	 * @see #setCaseFirstOff
   	 * @see #isCaseFirstOff
   	 * @see #isLowerCaseFirst
   	 * @see #isUpperCaseFirst
   	 * @see #setCaseFirstDefault
   	 * @draft 2.2
   	 */
   	public void setCaseFirst(boolean upperfirst)
   	{
   		if (upperfirst) {
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
   	 * @see #setCaseFirst(boolean)
   	 * @see #isCaseFirstOff
   	 * @see #isLowerCaseFirst
   	 * @see #isUpperCaseFirst
   	 * @see #setCaseFirstDefault
   	 */
   	public void setCaseFirstOff()
   	{
   		m_caseFirst_ = AttributeValue.OFF_;
   		updateInternalState();
   	}
   	
   	/**
   	 * Sets the case first mode to the initial mode set during 
	 * construction of the RuleBasedCollator.
	 * See setCaseFirst(boolean) for more details.
   	 * @see #setCaseFirstOff
   	 * @see #isCaseFirstOff
   	 * @see #isUpperCaseFirst
   	 * @see #setCaseFirst
   	 * @draft 2.2
   	 */
   	public final void setCaseFirstDefault()
   	{
   		m_caseFirst_ = m_defaultCaseFirst_;
   		updateInternalState();
   	}
   
    /**
     * Sets the alternate handling mode to the initial mode set during 
	 * construction of the RuleBasedCollator.
	 * See setAlternateHandling(boolean) for more details.
	 * @see #setAlternateHandling(boolean)
	 * @see #isAlternateHandling(boolean)
     * @draft 2.2
     */
    public void setAlternateHandlingDefault()
    {
    	m_isAlternateHandlingShifted_ = m_defaultIsAlternateHandlingShifted_;
    	updateInternalState();
    }
    
    /**
     * Sets the case level mode to the initial mode set during 
	 * construction of the RuleBasedCollator.
	 * See setCaseLevel(boolean) for more details.
	 * @see #setCaseLevel(boolean)
	 * @see #isCaseLevel
     * @draft 2.2
     */
    public void setCaseLevelDefault()
    {
    	m_isCaseLevel_ = m_defaultIsCaseLevel_;
    	updateInternalState();
    }
    
    /**
     * Sets the decomposition mode to the initial mode set during construction
     * of the RuleBasedCollator.
     * See setDecomposition(int) for more details.
     * @see #getDecomposition
     * @see #setDecomposition(int)
     * @draft 2.2
     */
    public void setDecompositionDefault()
    {
    	setDecomposition(m_defaultDecomposition_);
    }
    
    /**
     * Sets the French collation mode to the initial mode set during 
     * construction of the RuleBasedCollator.
     * See setFrenchCollation(boolean) for more details.
     * @see #isFrenchCollation
     * @see #setFrenchCollation(boolean)
     * @draft 2.2
     */
    public void setFrenchCollationDefault()
    {
    	m_isFrenchCollation_ = m_defaultIsFrenchCollation_;
    	updateInternalState();
    }
    
    /**
     * Sets the collation strength to the initial mode set during the 
     * construction of the RuleBasedCollator.
     * See setStrength(int) for more details.
     * @see #setStrength(int)
     * @see #getStrength
     * @draft 2.2
     */
    public void setStrengthDefault()
    {
    	setStrength(m_defaultStrength_);
    }
    
    /**
     * Sets the mode for the direction of SECONDARY weights to be used in 
     * French collation.
     * The default value is false which treats SECONDARY weights in the order 
     * they appear.
     * If true is set, the SECONDARY weights will be sorted backwards.
     * See the section on 
     * <a href=http://www-124.ibm.com/icu/userguide/Collate_ServiceArchitecture.html>
     * French collation</a> for more information.
     * @param flag true to set the French collation on, false to set it off
     * @draft 2.2
     * @see #isFrenchCollation
     * @see #setFrenchCollationDefault
     */
    public void setFrenchCollation(boolean flag) 
    {
    	m_isFrenchCollation_ = flag;
    	updateInternalState();
    }
    
    /**
     * Sets the alternate handling for Quaternary strength to be either 
     * shifted or non-ignorable. 
     * See the UCA definition on 
     * <a href=http://www.unicode.org/unicode/reports/tr10/#§3.2.2 Variable Collation Elements>
     * Alternate Weighting</a>.
     * This attribute will only be effective when QUATERNARY strength is set.
     * The default value for this mode is false, corresponding to the 
     * NON_IGNORABLE mode in UCA. In the NON-IGNORABLE mode, the 
     * RuleBasedCollator will treats all the codepoints with non-ignorable 
     * primary weights in the same way. 
     * If the mode is set to true, the behaviour corresponds to SHIFTED defined
     * in UCA, this causes codepoints with PRIMARY orders that are equal or 
     * below the variable top value to be ignored in PRIMARY order and 
     * moved to the QUATERNARY order.
     * @param shifted true if SHIFTED behaviour for alternate handling is 
     *        desired, false for the NON_IGNORABLE behaviour.
     * @see #isAlternateHandling(boolean)
     * @see #setAlternateHandlingDefault
     * @draft 2.2
     */
    public void setAlternateHandling(boolean shifted)
    {
    	m_isAlternateHandlingShifted_ = shifted;
    	updateInternalState();
    }
    
    /**
     * <p>
     * When case level is set to true, an additional weight is formed 
     * between the SECONDARY and TERTIARY weight, known as the case level. 
     * The case level is used to distinguish large and small Japanese Kana 
     * characters. Case level could also be used in other situations. 
     * For example to distinguish certain Pinyin characters. 
     * The default value is false, where the case level is not generated.
     * If the case level is set to true, which causes the case level to be 
     * generated. Contents of the case level are affected by the case first
     * mode. A simple way to ignore accent differences in a string is to set 
     * the strength to PRIMARY and enable case level.
     * </p>
     * <p>
     * See the section on 
     * <a href=http://www-124.ibm.com/icu/userguide/Collate_ServiceArchitecture.html>
     * case level</a> for more information.
     * </p>
     * @param flag true if case level sorting is required, false otherwise
     * @draft 2.2
     * @see #setCaseLevelDefault
	 * @see #isCaseLevel
	 * @see #setCaseFirst(boolean)
     */
    public void setCaseLevel(boolean flag) 
    {
    	m_isCaseLevel_ = flag;
    	updateInternalState();
    }

	/**
     * <p>
     * Sets this Collator's strength property. The strength property 
     * determines the minimum level of difference considered significant 
     * during comparison.
     * </p>
     * <p>See the Collator class description for an example of use.</p>
     * @param the new strength value.
     * @see #getStrength
     * @see #setStrengthDefault
     * @see #PRIMARY
     * @see #SECONDARY
     * @see #TERTIARY
     * @see #QUATERNARY
     * @see #IDENTICAL
     * @exception IllegalArgumentException If the new strength value is not one 
     * 				of PRIMARY, SECONDARY, TERTIARY, QUATERNARY or IDENTICAL.
     * @draft 2.2
     */
    public void setStrength(int newStrength) {
        super.setStrength(newStrength);
        updateInternalState();
    }

    // public getters --------------------------------------------------------
    
    /**
     * Gets the collation rules for this RuleBasedCollator.     
     * @return returns the collation rules
     * @draft 2.2
     */
    public String getRules()
    {
    	return m_rules_;
    }

	/**
     * <p>
     * Get a Collation key for the argument String source from this 
     * RuleBasedCollator. 
     * </p>
     * <p>
     * General recommendation: <br>
     * If comparison are to be done to the same String multiple times, it would
     * be more efficient to generate CollationKeys for the Strings and use 
     * CollationKey.compareTo(CollationKey) for the comparisons.
     * If the each Strings are compared to only once, using the method
     * RuleBasedCollator.compare(String, String) will have a better performance.
     * </p>
     * <p>
     * See the class documentation for an explanation about CollationKeys.
     * </p>
     * @param source the text String to be transformed into a collation key.
     * @return the CollationKey for the given String based on this 
     *         RuleBasedCollator's collation rules. If the source String is 
     *         null, a null CollationKey is returned.
     * @see CollationKey
     * @see #compare(String, String)
     * @draft 2.2
     */
    public CollationKey getCollationKey(String source)
    {
    	if (source == null) {
    		return null;
    	}
    	int strength = getStrength();
    	boolean compare[] = {m_isCaseLevel_,
    						 true,
    						 strength >= SECONDARY,
    						 strength >= TERTIARY,
    						 strength >= QUATERNARY,
							 strength == IDENTICAL
							};

		byte bytes[][] = {new byte[SORT_BUFFER_INIT_SIZE_CASE_], // case
    					new byte[SORT_BUFFER_INIT_SIZE_1_], // primary 
						new byte[SORT_BUFFER_INIT_SIZE_2_], // secondary
						new byte[SORT_BUFFER_INIT_SIZE_3_],	// tertiary	
						new byte[SORT_BUFFER_INIT_SIZE_4_]	// Quaternary
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
    	if ((compare[5] || getDecomposition() != NO_DECOMPOSITION)
    		&& Normalizer.quickCheck(source, Normalizer.NFD) 
    												!= Normalizer.YES) {
        	source = Normalizer.decompose(source, false);
    	}
		getSortKeyBytes(source, compare, bytes, bytescount, count, doFrench,
						hiragana4, commonBottom4, bottomCount4);
		byte sortkey[] = getSortKey(source, compare, bytes, bytescount, count, 
									doFrench, commonBottom4, bottomCount4);
		return new CollationKey(source, sortkey);
    }
    		    
    /**
	 * Checks if upper cased character is sorted before lower cased character.
	 * See setCaseFirst(boolean) for details.
	 * @see #setCaseFirstOff
   	 * @see #setCaseFirst(boolean)
   	 * @see #isLowerCaseFirst
   	 * @see #setCaseFirstDefault
	 * @return true if upper cased characters are sorted before lower cased 
	 *         characters, false otherwise
	 * @draft 2.2
	 */
	public boolean isUpperCaseFirst()
	{
		return (m_caseFirst_ == AttributeValue.UPPER_FIRST_);
	}
	
	/**
	 * Checks if lower cased character is sorted before upper cased character.
	 * See setCaseFirst(boolean) for details.
	 * @see #setCaseFirstOff
   	 * @see #setCaseFirst(boolean)
   	 * @see #isUpperCaseFirst
   	 * @see #setCaseFirstDefault
	 * @return true lower cased characters are sorted before upper cased 
	 *         characters, false otherwise
	 * @draft 2.2
	 */
	public boolean isLowerCaseFirst()
	{
		return (m_caseFirst_ == AttributeValue.LOWER_FIRST_);
	}
	
	/**
	 * Checks if a previous call to setCaseFirst(boolean) is turned off
	 * by setCaseFirstOff().
	 * See setCaseFirst(boolean) for details.
	 * @return true if the customized case sorting is turned off, false 
	 *         otherwise
	 * @see #setCaseFirstOff
   	 * @see #setCaseFirst(boolean)
   	 * @see #isUpperCaseFirst
   	 * @see #isLowerCaseFirst
   	 * @see #setCaseFirstDefault
	 * @draft 2.2
	 */
	public boolean isCaseFirstOff()
	{
		return (m_caseFirst_ == AttributeValue.OFF_);
	}
	
	/**
	 * Checks if the alternate handling behaviour is the UCA defined SHIFTED or 
	 * NON_IGNORABLE.
	 * <ul>
	 * <li>If argument shifted is true and
	 *     <ul>
	 *     <li>return value is true, then the alternate handling attribute for 
	 *         the Collator is SHIFTED. Or
	 *     <li>return value is false, then the alternate handling attribute for
	 *         the Collator is NON_IGNORABLE
	 *     </ul>
	 * <li> If argument shifted is false and 
	 *     <ul>
	 *     <li>return value is true, then the alternate handling attribute for 
	 *         the Collator is NON_IGNORABLE. Or
	 *     <li>return value is false, then the alternate handling attribute for
	 *         the Collator is SHIFTED.
	 *     </ul>
	 * </ul>
	 * See setAlternateHandling(boolean) for more details.
	 * @param shifted true if checks are to be done to see if the SHIFTED 
	 *        behaviour is on, false if checks are to be done to see if the
	 *        NON_IGNORABLE behaviour is on.
	 * @return true or false 
	 * @see #setAlternateHandling(boolean)
	 * @see #setAlternateHandlingDefault
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
	 * Checks if case level is set to true.
	 * See setCaseLevel(boolean) for details.
	 * @return the case level mode
	 * @see #setCaseLevelDefault
	 * @see #isCaseLevel
	 * @see #setCaseLevel(boolean)
	 * @draft 2.2
	 */
	public boolean isCaseLevel()
	{
		return m_isCaseLevel_;
	}
	
	/**
	 * Checks if French Collation is set to true.
	 * See setFrenchCollation(boolean) for details.
	 * @return true if French Collation is set to true, false otherwise
	 * @see #setFrenchCollation(boolean)
	 * @see #setFrenchCollationDefault
	 * @draft 2.2
	 */
	public boolean isFrenchCollation()
	{
		return m_isFrenchCollation_;
	}
	
	/**
	 * Checks if the Hiragana Quaternary mode is set on.
	 * See setHiraganaQuaternary(boolean) for more details.
	 * @return flag true if Hiragana Quaternary mode is on, false otherwise
	 * @see #setHiraganaQuaternaryDefault
	 * @see #setHiraganaQuaternary(boolean)
	 * @draft 2.2
	 */
	public boolean isHiraganaQuaternary()
	{
		return m_isHiragana4_;
	}
		
	// public other methods -------------------------------------------------

    /**
     * Compares the equality of two RuleBasedCollator objects.
     * RuleBasedCollator objects are equivalent if they have the same collation
     * rules and the same attributes.
     * @param obj the RuleBasedCollator to be compared with.
     * @return true if this RuleBasedCollator has exactly the same 
     *         collation behaviour as obj, false otherwise.
     * @draft 2.2
     */
    public boolean equals(Object obj) {
        if (obj == null) {
        	return false;  // super does class check
        }
        if (this == obj) {
        	return true;
        }
        if (getClass() != obj.getClass()) {
        	return false;
        }
        RuleBasedCollator other = (RuleBasedCollator)obj;
        // all other non-transient information is also contained in rules.
        return getStrength() == other.getStrength() 
               && getDecomposition() == other.getDecomposition() 
               && other.m_caseFirst_ == m_caseFirst_
               && other.m_caseSwitch_ == m_caseSwitch_
               && other.m_isAlternateHandlingShifted_ 
                                             == m_isAlternateHandlingShifted_
               && other.m_isCaseLevel_ == m_isCaseLevel_
               && other.m_isFrenchCollation_ == m_isFrenchCollation_
               && other.m_isHiragana4_ == m_isHiragana4_
               && m_rules_.equals(other.m_rules_);
    }
    
	/**
     * Generates a unique hash code for this RuleBasedCollator.
     * @return the unique hash code for this Collator
     * @draft 2.2
     */
    public int hashCode() 
    {
    	return getRules().hashCode();
    }
    
    /**
     * Compares the source text String to the target text String according to 
     * the collation rules, strength and decomposition mode for this 
     * RuleBasedCollator. 
     * Returns an integer less than, 
     * equal to or greater than zero depending on whether the source String is 
     * less than, equal to or greater than the target String. See the Collator
     * class description for an example of use.
     * </p>
     * <p>
     * General recommendation: <br>
     * If comparison are to be done to the same String multiple times, it would
     * be more efficient to generate CollationKeys for the Strings and use 
     * CollationKey.compareTo(CollationKey) for the comparisons.
     * If the each Strings are compared to only once, using the method
     * RuleBasedCollator.compare(String, String) will have a better performance.
     * </p>
     * @param source the source text String.
     * @param target the target text String.
     * @return Returns an integer value. Value is less than zero if source is 
     *         less than target, value is zero if source and target are equal, 
     *         value is greater than zero if source is greater than target.
     * @see CollationKey
     * @see #getCollationKey
     * @draft 2.2
     */
    public int compare(String source, String target)
    {
    	if (source == target) {
	        return 0;
	    }
	
		// Find the length of any leading portion that is equal
		int offset = getFirstUnmatchedOffset(source, target);
		if (offset == source.length()) {
			if (offset == target.length() || checkIgnorable(target, offset)) {
				return 0;
			}
			return -1;
	    }
	    else if (target.length() == offset) {
	    	if (checkIgnorable(source, offset)) {
	    		return 0;
	    	}
	    	return 1;
	    }

        int strength = getStrength();
		// setting up the collator parameters	
		boolean compare[] = {m_isCaseLevel_,
    						 true,
    						 strength >= SECONDARY,
    						 strength >= TERTIARY,
    						 strength >= QUATERNARY,
							 strength == IDENTICAL
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
	
		if (doShift4) {  // checkQuad
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

    // package private inner interfaces --------------------------------------
    
    /**
	 * Attribute values to be used when setting the Collator options
	 */
	static interface AttributeValue
	{
		/**
		 * Indicates that the default attribute value will be used. 
		 * See individual attribute for details on its default value. 
		 */
		static final int DEFAULT_ = -1;
		/** 
		 * Primary collation strength 
		 */
		static final int PRIMARY_ = Collator.PRIMARY;
		/** 
		 * Secondary collation strength 
		 */
		static final int SECONDARY_ = Collator.SECONDARY;
		/** 
		 * Tertiary collation strength 
		 */
		static final int TERTIARY_ = Collator.TERTIARY;
		/** 
		 * Default collation strength 
		 */
		static final int DEFAULT_STRENGTH_ = Collator.TERTIARY;
		/**
		 * Internal use for strength checks in Collation elements
		 */
		static final int CE_STRENGTH_LIMIT_ = Collator.TERTIARY + 1;
		/** 
		 * Quaternary collation strength 
		 */
		static final int QUATERNARY_ = 3;
		/** 
		 * Identical collation strength 
		 */
		static final int IDENTICAL_ = Collator.IDENTICAL;
		/**
		 * Internal use for strength checks
		 */
		static final int STRENGTH_LIMIT_ = Collator.IDENTICAL + 1;
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
	};
	    
	/** 
	 * Attributes that collation service understands. All the attributes can 
	 * take DEFAULT value, as well as the values specific to each one. 
	 */
	static interface Attribute 
	{
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
	};
	
	/**
     * DataManipulate singleton
     */
    static class DataManipulate implements Trie.DataManipulate
    {
    	// public methods ----------------------------------------------------
    	
    	/**
	     * Internal method called to parse a lead surrogate's ce for the offset
	     * to the next trail surrogate data.
	     * @param ce collation element of the lead surrogate
	     * @return data offset or 0 for the next trail surrogate
	     * @draft 2.2
	     */
	    public final int getFoldingOffset(int ce)
	    {
	    	if (isSpecial(ce) && getTag(ce) == CE_SURROGATE_TAG_) {
	    		return (ce & 0xFFFFFF);
	    	}
	    	return 0;
	    } 
	    
	    /**
	     * Get singleton object
	     */
	    public static final DataManipulate getInstance()
	    {
	    	if (m_instance_ == null) {
	    		m_instance_ =  new DataManipulate();
	    	}
	    	return m_instance_;
	    }
	    
	    // private data member ----------------------------------------------
	    
	    /**
	     * Singleton instance
	     */
	    private static DataManipulate m_instance_;
	    
	    // private constructor ----------------------------------------------
	    
	    /**
	     * private to prevent initialization
	     */
	    private DataManipulate()
	    {
	    }
    };
    
    // package private data member -------------------------------------------
    
    static final byte BYTE_FIRST_TAILORED_ = (byte)0x04;
    static final byte BYTE_COMMON_ = (byte)0x05;
    static final int COMMON_TOP_2_ = 0x86; // int for unsigness
    static final int COMMON_BOTTOM_2_ = BYTE_COMMON_;
    /**
	 * Case strength mask
	 */
	static final int CE_CASE_BIT_MASK_ = 0xC0;
	static final int CE_TAG_SHIFT_ = 24;
	static final int CE_TAG_MASK_ = 0x0F000000;
	
	static final int CE_SPECIAL_FLAG_ = 0xF0000000;
    /** 
     * Lead surrogate that is tailored and doesn't start a contraction 
     */
    static final int CE_SURROGATE_TAG_ = 5;  
	/**
  	 * Mask to get the primary strength of the collation element
  	 */
  	static final int CE_PRIMARY_MASK_ = 0xFFFF0000;
  	/**
  	 * Mask to get the secondary strength of the collation element
  	 */
   	static final int CE_SECONDARY_MASK_ = 0xFF00;
   	/**
  	 * Mask to get the tertiary strength of the collation element
  	 */
   	static final int CE_TERTIARY_MASK_ = 0xFF;
   	/**
   	 * Primary strength shift 
   	 */
	static final int CE_PRIMARY_SHIFT_ = 16;
	/** 
	 * Secondary strength shift 
	 */
	static final int CE_SECONDARY_SHIFT_ = 8;
   	/**
   	 * Continuation marker
   	 */
   	static final int CE_CONTINUATION_MARKER_ = 0xC0;
   	
   	/**
	 * Size of collator raw data headers and options before the expansion
	 * data. This is used when expansion ces are to be retrieved. ICU4C uses
	 * the expansion offset starting from UCollator.UColHeader, hence ICU4J
	 * will have to minus that off to get the right expansion ce offset. In
	 * number of ints.
	 */
	int m_expansionOffset_;
	/**
	 * Size of collator raw data headers, options and expansions before
	 * contraction data. This is used when contraction ces are to be retrieved. 
	 * ICU4C uses contraction offset starting from UCollator.UColHeader, hence
	 * ICU4J will have to minus that off to get the right contraction ce 
	 * offset. In number of chars.
	 */
	int m_contractionOffset_;
    /**
     * Flag indicator if Jamo is special
     */
    boolean m_isJamoSpecial_;
 
 	// Collator options ------------------------------------------------------   
 	int m_defaultVariableTopValue_;
	boolean m_defaultIsFrenchCollation_;
	boolean m_defaultIsAlternateHandlingShifted_; 
    int m_defaultCaseFirst_;
    boolean m_defaultIsCaseLevel_;
    int m_defaultDecomposition_;
    int m_defaultStrength_;
    boolean m_defaultIsHiragana4_;
 	/**
 	 * Value of the variable top
 	 */
    int m_variableTopValue_;
    /** 
     * Attribute for special Hiragana 
     */
    boolean m_isHiragana4_;         
	/**
     * Case sorting customization
     */
    int m_caseFirst_;
    
    // end Collator options --------------------------------------------------
       
    /**
     * Expansion table
     */
    int m_expansion_[];
    /**
     * Contraction index table
     */
    char m_contractionIndex_[];
    /**
     * Contraction CE table
     */
    int m_contractionCE_[];
    /**
     * Data trie
     */
    IntTrie m_trie_;
    /**
     * Table to store all collation elements that are the last element of an
     * expansion. This is for use in StringSearch.
     */
    int m_expansionEndCE_[];
    /**
     * Table to store the maximum size of any expansions that end with the 
     * corresponding collation element in m_expansionEndCE_. For use in
     * StringSearch too
     */
    byte m_expansionEndCEMaxSize_[];
    /**
     * Heuristic table to store information on whether a char character is 
     * considered "unsafe". "Unsafe" character are combining marks or those 
     * belonging to some contraction sequence from the offset 1 onwards. 
     * E.g. if "ABC" is the only contraction, then 'B' and 'C' are considered 
     * unsafe. If we have another contraction "ZA" with the one above, then 
     * 'A', 'B', 'C' are "unsafe" but 'Z' is not. 
     */
    byte m_unsafe_[];
    /**
     * Table to store information on whether a codepoint can occur as the last
     * character in a contraction
     */
    byte m_contractionEnd_[];
    /**
     * Table for UCA and builder use
     */
    char m_UCAContraction_[];
	/**
	 * Original collation rules
	 */
	String m_rules_;
	/**
     * The smallest "unsafe" codepoint
     */
    char m_minUnsafe_;
    /**
	 * The smallest codepoint that could be the end of a contraction
	 */
	char m_minContractionEnd_;
	
	/**
     * UnicodeData.txt property object
     */
    static final RuleBasedCollator UCA_;  
    
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
    
    // package private constructors ------------------------------------------
    
    /**
    * <p>Private contructor for use by subclasses. 
    * Public access to creating Collators is handled by the API 
    * Collator.getInstance() or RuleBasedCollator(String rules).
    * </p>
    * <p>
    * This constructor constructs the UCA collator internally
    * </p>
    * @draft 2.2
    */
    RuleBasedCollator() 
    {
    }
    
    // package private methods -----------------------------------------------
    
    /**
     * Sets this collator to use the tables in UCA. Note options not taken
     * care of here.
     */
    final void setWithUCATables()
    {
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
    }
    
    /**
     * Sets this collator to use the all options and tables in UCA. 
     */
    final void setWithUCAData()
    {
    	m_addition3_ = UCA_.m_addition3_;
    	m_bottom3_ = UCA_.m_bottom3_;
    	m_bottomCount3_ = UCA_.m_bottomCount3_;
    	m_caseFirst_ = UCA_.m_caseFirst_;
    	m_caseSwitch_ = UCA_.m_caseSwitch_;
    	m_common3_ = UCA_.m_common3_;
    	m_contractionOffset_ = UCA_.m_contractionOffset_;
    	setDecomposition(UCA_.getDecomposition());
    	m_defaultCaseFirst_ = UCA_.m_defaultCaseFirst_;
    	m_defaultDecomposition_ = UCA_.m_defaultDecomposition_;
    	m_defaultIsAlternateHandlingShifted_ 
    	                           = UCA_.m_defaultIsAlternateHandlingShifted_;
    	m_defaultIsCaseLevel_ = UCA_.m_defaultIsCaseLevel_;
    	m_defaultIsFrenchCollation_ = UCA_.m_defaultIsFrenchCollation_;
    	m_defaultIsHiragana4_ = UCA_.m_defaultIsHiragana4_;
    	m_defaultStrength_ = UCA_.m_defaultStrength_;
    	m_defaultVariableTopValue_ = UCA_.m_defaultVariableTopValue_;
    	m_expansionOffset_ = UCA_.m_expansionOffset_;
    	m_isAlternateHandlingShifted_ = UCA_.m_isAlternateHandlingShifted_;
    	m_isCaseLevel_ = UCA_.m_isCaseLevel_;
    	m_isFrenchCollation_ = UCA_.m_isFrenchCollation_;
    	m_isHiragana4_ = UCA_.m_isHiragana4_;
    	m_isJamoSpecial_ = UCA_.m_isJamoSpecial_;
    	m_isSimple3_ = UCA_.m_isSimple3_;
    	m_mask3_ = UCA_.m_mask3_;
    	m_minContractionEnd_ = UCA_.m_minContractionEnd_;
    	m_minUnsafe_ = UCA_.m_minUnsafe_;
    	m_rules_ = UCA_.m_rules_;
    	setStrength(UCA_.getStrength());
    	m_top3_ = UCA_.m_top3_;
    	m_topCount3_ = UCA_.m_topCount3_;
    	m_variableTopValue_ = UCA_.m_variableTopValue_;
    	setWithUCATables();
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
	final boolean isUnsafe(char ch) 
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
	final boolean isContractionEnd(char ch) 
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
	 * Retrieve the tag of a special ce
	 * @param ce ce to test
	 * @return tag of ce
	 */
	static int getTag(int ce) 
	{
		return (ce & CE_TAG_MASK_) >> CE_TAG_SHIFT_;
	}
    
    /** 
	 * Checking if ce is special
	 * @param ce to check
	 * @return true if ce is special
	 */
	static boolean isSpecial(int ce)
	{
		return (ce & CE_SPECIAL_FLAG_) == CE_SPECIAL_FLAG_; 
	}

    /**
     * Checks if the argument ce is a continuation
     * @param ce collation element to test
     * @return true if ce is a continuation
     */
    static final boolean isContinuation(int ce) 
    {
        return ce != CollationElementIterator.NULLORDER 
                       && (ce & CE_CONTINUATION_TAG_) == CE_CONTINUATION_TAG_;
    }
    
    // protected constructor -------------------------------------------------
  
    /**
     * Constructors a RuleBasedCollator from the argument locale.
     * If no resource bundle is associated with the locale, UCA is used 
     * instead.
     * @param locale
     * @exception Exception thrown when there's an error creating the Collator
     */
    RuleBasedCollator(Locale locale) throws Exception
    {
	    ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
	 
	    if (rb != null) {
		    byte map[] = (byte [])rb.getObject("%%CollationBin");
		    BufferedInputStream input = 
			            new BufferedInputStream(new ByteArrayInputStream(map));
			CollatorReader reader = new CollatorReader(input, false);
			if (map.length > MIN_BINARY_DATA_SIZE_) {
				reader.read(this);
		    } 
		    else {
			    reader.readHeader(this);
			    reader.readOptions(this);
			    // duplicating UCA_'s data
			    setWithUCATables();
		    }
		    Object rules = rb.getObject("CollationElements");
		    if (rules != null) {
		        m_rules_ = (String)((Object[][])rules)[0][1];
		    }
		    init();
	    }
	    else {
	    	setWithUCAData();
	    }
    } 
    
    // private inner classes ------------------------------------------------
    
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
	private static final int CASE_SWITCH_ = 0xC0;
	private static final int NO_CASE_SWITCH_ = 0;
	/**
	 * Case level constants
	 */
	private static final int CE_REMOVE_CASE_ = 0x3F;
	private static final int CE_KEEP_CASE_ = 0xFF;
	/**
	 * Case strength mask
	 */
	private static final int CE_CASE_MASK_3_ = 0xFF;
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
    private static final byte BYTE_FIRST_UCA_ = BYTE_COMMON_;
    private static final byte BYTE_LAST_LATIN_PRIMARY_ = (byte)0x4C;
    private static final byte BYTE_FIRST_NON_LATIN_PRIMARY_ = (byte)0x4D;
    private static final byte BYTE_UNSHIFTED_MAX_ = (byte)0xFF;
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
	 * Minimum size required for the binary collation data in bytes.
	 * Size of UCA header + size of options to 4 bytes
	 */
	private static final int MIN_BINARY_DATA_SIZE_ = (41 + 8) << 2;     
	
	/**
	 * If this collator is to generate only simple tertiaries for fast path
	 */
	private boolean m_isSimple3_;
	
	/**
     * French collation sorting flag
     */
    private boolean m_isFrenchCollation_;
    /**
     * Flag indicating if shifted is requested for Quaternary alternate
     * handling. If this is not true, the default for alternate handling will
     * be non-ignorable.
     */
    private boolean m_isAlternateHandlingShifted_; 
    /** 
     * Extra case level for sorting
     */
    private boolean m_isCaseLevel_;
	
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
     * Gets the 2 bytes of primary order and adds it to the primary byte array
     * @param ce current ce
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param notIsContinuation flag indicating if the current bytes belong to 
     * 			a continuation ce
     * @param doShift flag indicating if ce is to be shifted
     * @param leadPrimary lead primary used for compression
     * @param commonBottom4 common byte value for Quaternary
     * @param bottomCount4 smallest byte value for Quaternary
     * @return the new lead primary for compression
     */
    private final int doPrimaryBytes(int ce, byte bytes[][], int bytescount[],
    						      int count[], boolean notIsContinuation, 
    						      boolean doShift, int leadPrimary,
    						      int commonBottom4, int bottomCount4)
    {
    	
    	int p2 = (ce >>= 16) & LAST_BYTE_MASK_; // in ints for unsigned 
        int p1 = ce >>> 8;  // comparison
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
	 * Gets the Quaternary byte and adds it to the Quaternary byte array
     * @param bytes array of byte arrays for each strength
     * @param bytescount array of the size of each strength byte arrays 
     * @param count array of counters for each of the strength
     * @param isCodePointHiragana flag indicator if the previous codepoint 
     * 			we dealt with was Hiragana
     * @param commonBottom4 smallest common Quaternary byte 
     * @param bottomCount4 smallest Quaternary byte 
     * @param hiragana4 hiragana Quaternary byte
	 */
	private final void doQuaternaryBytes(byte bytes[][], int bytescount[], 
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
	private final void getSortKeyBytes(String source, boolean compare[], 
									   byte bytes[][], int bytescount[], 
									   int count[], boolean doFrench,
									   byte hiragana4, int commonBottom4, 
									   int bottomCount4)
									   
	{
		int backupDecomposition = getDecomposition();
		setDecomposition(NO_DECOMPOSITION); // have to revert to backup later
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
                doQuaternaryBytes(bytes, bytescount, count, 
                			 	coleiter.m_isCodePointHiragana_, 
                			 	commonBottom4, bottomCount4, hiragana4);
            }
        }
        setDecomposition(backupDecomposition); // reverts to original	
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
		      		// getting the unsigned value
		        	if ((s & LAST_BYTE_MASK_) > COMMON_2_) { 
		        		// not necessary for 4th level.
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
        bytescount[1] = BOSCU.writeIdenticalLevelRun(source, bytes[1], 
        											 bytescount[1]); 
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
		int slength = source.length();
		int tlength = target.length();
		int minlength = slength;
		if (minlength > tlength) {
			minlength = tlength;
		}
		while (result < minlength 
				&& source.charAt(result) == target.charAt(result)) {
			result ++;
	    }
	    if (result > 0) {
	        // There is an identical portion at the beginning of the two 
	        // strings. If the identical portion ends within a contraction or a 
	        // combining character sequence, back up to the start of that 
	        // sequence.
	        char schar = 0;
	        char tchar = 0;
	        if (result < minlength) {              
	        	schar = source.charAt(result); // first differing chars   
	        	tchar = target.charAt(result);
	        }
	        else {
	        	if (slength == tlength) {
	        		return result;
	        	}
	        	else if (slength < tlength) {
	        		tchar = target.charAt(result);
	        	}
	        	else {
	        		schar = source.charAt(result);
	        	}
	        }
	        if (isUnsafe(schar) || isUnsafe(tchar))
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
	    StringCharacterIterator siter = new StringCharacterIterator(source, 
	    													textoffset, 
	    													source.length(), 
	    													textoffset);
	    CollationElementIterator scoleiter = new CollationElementIterator(
	    														siter, this);
	    StringCharacterIterator titer = new StringCharacterIterator(target, 
	    												    textoffset, 
	    													target.length(),
	    													textoffset);
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
	            	return endPrimaryCompare(sorder, torder, cebuffer, 
	            								cebuffersize);
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
	    			return endPrimaryCompare(sorder, torder, cebuffer, 
	    														cebuffersize);
	        	}
	      	} // no primary difference... do the rest from the buffers
	    }
		return 0;
	}
	
	/**
	 * This is used only for primary strength when we know that sorder is 
	 * already different from torder.
	 * Compares sorder and torder, returns -1 if sorder is less than torder.
	 * Clears the cebuffer at the same time.
	 * @param sorder source strength order
	 * @param torder target strength order
	 * @param cebuffer array of buffers containing the ce values
	 * @param cebuffersize array of cebuffer offsets
	 * @return the comparison result of sorder and torder
	 */
	private static final int endPrimaryCompare(int sorder, int torder, 
										       int cebuffer[][], 
										       int cebuffersize[])
	{
		// if we reach here, the ce offset accessed is the last ce
		// appended to the buffer
		boolean isSourceNullOrder = (cebuffer[0][cebuffersize[0] - 1] 
			 							== CollationElementIterator.NULLORDER);
		boolean isTargetNullOrder = (cebuffer[1][cebuffersize[1] - 1] 
			 							== CollationElementIterator.NULLORDER);	 					
		cebuffer[0] = null;
	    cebuffer[1] = null;
	    cebuffersize[0] = 0;
	    cebuffersize[1] = 0;
	    if (isSourceNullOrder) {
	    	return -1;
	    }
	    if (isTargetNullOrder) {
	    	return 1;
	    }
	    // getting rid of the sign
	    sorder >>>= CE_PRIMARY_SHIFT_;
	    torder >>>= CE_PRIMARY_SHIFT_;
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
	    	int soffset = 0;
	    	int toffset = 0;
	        while (true) {
	        	int sorder = CollationElementIterator.IGNORABLE;
	          	while (sorder == CollationElementIterator.IGNORABLE) {
	            	sorder = cebuffer[0][soffset ++] & CE_SECONDARY_MASK_;
	          	}
				int torder = CollationElementIterator.IGNORABLE;
	          	while (torder == CollationElementIterator.IGNORABLE) {
	          		torder = cebuffer[1][toffset ++] & CE_SECONDARY_MASK_;
	          	}
	
	          	if (sorder == torder) {
	            	if (cebuffer[0][soffset - 1]  
	            					== CollationElementIterator.NULLORDER) {
	              		break;
	            	}
	          	} 
	          	else {
	          		if (cebuffer[0][soffset - 1] == 
	          				CollationElementIterator.NULLORDER) {
	          			return -1;
	          		}
	          		if (cebuffer[1][toffset - 1] == 
	          				CollationElementIterator.NULLORDER) {
	          			return 1;
	          		}
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
	        										continuationoffset, 1);
	          	if (sorder == torder) {
	            	if ((offset[0] < 0 && offset[1] < 0) 
	            		|| cebuffer[0][offset[0]] 
	            					== CollationElementIterator.NULLORDER) {
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
	        	result = cebuffer[index][offset[index]];
		        while (isContinuation(cebuffer[index][offset[index] --]));
		            // after this, sorder is at the start of continuation, 
		            // and offset points before that 
		            if (isContinuation(cebuffer[index][offset[index] + 1])) {
		            	// save offset for later
		            	continuationoffset[index] = offset[index]; 
		            	offset[index] += 2;  
		           	}
	        	//}
	        }
	        else {
	        	result = cebuffer[index][offset[index] ++];
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
		int soffset = 0;
		int toffset = 0;
	    while (true) {
	    	int sorder = CollationElementIterator.IGNORABLE;
	        int torder = CollationElementIterator.IGNORABLE;
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
	          	if (!isContinuation(torder)) {
	            	torder &= CE_CASE_MASK_3_;
	            	torder ^= m_caseSwitch_;
	          	} 
	          	else {
	            	torder = CollationElementIterator.IGNORABLE;
	          	}
	        }
	
			if (sorder == torder) {
				if (cebuffer[0][soffset - 1] 
										== CollationElementIterator.NULLORDER) {
	          		break;
	        	} 
			}
			else {
	        	if (cebuffer[0][soffset - 1] 
	        						== CollationElementIterator.NULLORDER) {
	          		return -1;
	        	}
	        
	        	return ((sorder & CE_CASE_BIT_MASK_) 
	        			< (torder & CE_CASE_BIT_MASK_)) ? -1 : 1;
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
	          						== CollationElementIterator.NULLORDER) {
	            	break;
	          	} 
	        } 
	        else {
	        	if (cebuffer[0][soffset - 1] == 
	          							CollationElementIterator.NULLORDER) {
	          		return -1;
	          	}
	          	if (cebuffer[1][toffset - 1] == 
	          				CollationElementIterator.NULLORDER) {
	          		return 1;
	          	}
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
	          	if (cebuffer[0][soffset - 1] 
	          		== CollationElementIterator.NULLORDER) {
	            	break;
	          	}
	        } 
	        else {
	        	if (cebuffer[0][soffset - 1] == 
	          		CollationElementIterator.NULLORDER) {
	          		return -1;
	          	}
	          	if (cebuffer[1][toffset - 1] == 
	          		CollationElementIterator.NULLORDER) {
	          		return 1;
	          	}
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
	        if (Normalizer.quickCheck(source, Normalizer.NFD) 
	        										!= Normalizer.YES) {
	            source = Normalizer.decompose(source, false);
	        }
	
	        if (Normalizer.quickCheck(target, Normalizer.NFD) 
	        											!= Normalizer.YES) {
	            target = Normalizer.decompose(target, false);
	        }
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
    	int slength = source.length();
    	int tlength = target.length();
    	int minlength = Math.min(slength, tlength);
    	while (offset < minlength) {
        	schar = source.charAt(offset);
        	tchar = target.charAt(offset ++);
        	if (schar != tchar) {
            	break;
        	}
    	}
    	
    	if (schar == tchar && offset == minlength) {
    		if (slength > minlength) {
    			return 1;
    		}
    		if (tlength > minlength) {
    			return -1;
    		}
    		return 0;
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
	
	/**
	 * Checks that the source after offset is ignorable
	 * @param source text string to check
	 * @param offset 
	 * @return true if source after offset is ignorable. false otherwise
	 */
	private final boolean checkIgnorable(String source, int offset)
													
	{
		StringCharacterIterator siter = new StringCharacterIterator(source,
											offset, source.length(), offset);
		CollationElementIterator coleiter = new CollationElementIterator(
	    														siter, this);
	    int ce = coleiter.next();
	    while (ce != CollationElementIterator.NULLORDER) {
	    	if (ce != CollationElementIterator.IGNORABLE) {
	    		return false;
	    	}
	    	ce = coleiter.next();
	    }
	    return true; 
	}
	
	/**
	 * Resets the internal case data members and compression values.
	 */
	private void updateInternalState() 
	{
      	if (m_caseFirst_ == AttributeValue.UPPER_FIRST_) {
        	m_caseSwitch_ = (byte)CASE_SWITCH_;
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
        	m_mask3_ = (byte)CE_KEEP_CASE_;
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

      	if (!m_isCaseLevel_ && getStrength() == AttributeValue.TERTIARY_ 
          	&& !m_isFrenchCollation_ && !m_isAlternateHandlingShifted_) {
        	m_isSimple3_ = true;
      	} 
      	else {
        	m_isSimple3_ = false;
      	}
	}
	
	/**
     * Initializes the RuleBasedCollator
     */
    private final void init()
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
    	setStrength(m_defaultStrength_);
    	setDecomposition(m_defaultDecomposition_);
    	m_isFrenchCollation_ = m_defaultIsFrenchCollation_;
    	m_isAlternateHandlingShifted_ = m_defaultIsAlternateHandlingShifted_;
    	m_isCaseLevel_ = m_defaultIsCaseLevel_;
    	m_caseFirst_ = m_defaultCaseFirst_;
    	m_isHiragana4_ = m_defaultIsHiragana4_;
    	updateInternalState();
    }
}
