/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.nio.ByteBuffer;
import java.text.CharacterIterator;
import java.text.ParseException;
import java.util.Arrays;

import com.ibm.icu.impl.BOCU;
import com.ibm.icu.impl.ICUDebug;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ImplicitCEGenerator;
import com.ibm.icu.impl.IntTrie;
import com.ibm.icu.impl.StringUCharacterIterator;
import com.ibm.icu.impl.Trie;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

/**
 * <p>RuleBasedCollator is a concrete subclass of Collator. It allows
 * customization of the Collator via user-specified rule sets.
 * RuleBasedCollator is designed to be fully compliant to the <a
 * href="http://www.unicode.org/unicode/reports/tr10/"> Unicode
 * Collation Algorithm (UCA)</a> and conforms to ISO 14651.</p>
 *
 * <p>Users are strongly encouraged to read <a
 * href="http://oss.software.ibm.com/icu/userguide/Collate_Intro.html">
 * the users guide</a> for more information about the collation
 * service before using this class.</p>
 *
 * <p>Create a RuleBasedCollator from a locale by calling the
 * getInstance(Locale) factory method in the base class Collator.
 * Collator.getInstance(Locale) creates a RuleBasedCollator object
 * based on the collation rules defined by the argument locale.  If a
 * customized collation ordering ar attributes is required, use the
 * RuleBasedCollator(String) constructor with the appropriate
 * rules. The customized RuleBasedCollator will base its ordering on
 * UCA, while re-adjusting the attributes and orders of the characters
 * in the specified rule accordingly.</p>
 *
 * <p>RuleBasedCollator provides correct collation orders for most
 * locales supported in ICU. If specific data for a locale is not
 * available, the orders eventually falls back to the <a
 * href="http://www.unicode.org/unicode/reports/tr10/">UCA collation
 * order </a>.</p>
 *
 * <p>For information about the collation rule syntax and details
 * about customization, please refer to the
 * <a href="http://oss.software.ibm.com/icu/userguide/Collate_Customization.html">
 * Collation customization</a> section of the user's guide.</p>
 *
 * <p><strong>Note</strong> that there are some differences between
 * the Collation rule syntax used in Java and ICU4J:
 *
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
 * <li>As mentioned in the documentation of the base class Collator,
 *     compatibility decomposition mode is not supported.
 * </ul>
 * <p>
 * <strong>Examples</strong>
 * </p>
 * <p>
 * Creating Customized RuleBasedCollators:
 * <blockquote>
 * <pre>
 * String simple = "&amp; a &lt; b &lt; c &lt; d";
 * RuleBasedCollator simpleCollator = new RuleBasedCollator(simple);
 *
 * String norwegian = "&amp; a , A &lt; b , B &lt; c , C &lt; d , D &lt; e , E "
 *                    + "&lt; f , F &lt; g , G &lt; h , H &lt; i , I &lt; j , "
 *                    + "J &lt; k , K &lt; l , L &lt; m , M &lt; n , N &lt; "
 *                    + "o , O &lt; p , P &lt; q , Q &lt r , R &lt s , S &lt; "
 *                    + "t , T &lt; u , U &lt; v , V &lt; w , W &lt; x , X "
 *                    + "&lt; y , Y &lt; z , Z &lt; &#92;u00E5 = a&#92;u030A "
 *                    + ", &#92;u00C5 = A&#92;u030A ; aa , AA &lt; &#92;u00E6 "
 *                    + ", &#92;u00C6 &lt; &#92;u00F8 , &#92;u00D8";
 * RuleBasedCollator norwegianCollator = new RuleBasedCollator(norwegian);
 * </pre>
 * </blockquote>
 *
 * Concatenating rules to combine <code>Collator</code>s:
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
 *
 * Making changes to an existing RuleBasedCollator to create a new
 * <code>Collator</code> object, by appending changes to the existing rule:
 * <blockquote>
 * <pre>
 * // Create a new Collator object with additional rules
 * String addRules = "&amp; C &lt; ch, cH, Ch, CH";
 * RuleBasedCollator myCollator =
 *     new RuleBasedCollator(en_USCollator + addRules);
 * // myCollator contains the new rules
 * </pre>
 * </blockquote>
 *
 * How to change the order of non-spacing accents:
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
 *
 * Putting in a new primary ordering before the default setting,
 * e.g. sort English characters before or after Japanese characters in the Japanese
 * <code>Collator</code>:
 * <blockquote>
 * <pre>
 * // get en_US Collator rules
 * RuleBasedCollator en_USCollator
 *                        = (RuleBasedCollator)Collator.getInstance(Locale.US);
 * // add a few Japanese characters to sort before English characters
 * // suppose the last character before the first base letter 'a' in
 * // the English collation rule is &#92;u2212
 * String jaString = "& &#92;u2212 &lt &#92;u3041, &#92;u3042 &lt &#92;u3043, "
 *                   + "&#92;u3044";
 * RuleBasedCollator myJapaneseCollator
 *              = new RuleBasedCollator(en_USCollator.getRules() + jaString);
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * This class is not subclassable
 * </p>
 * @author Syn Wee Quek
 * @stable ICU 2.8
 */
public final class RuleBasedCollator extends Collator
{   
    // public constructors ---------------------------------------------------

    /**
     * <p>
     * Constructor that takes the argument rules for
     * customization. The collator will be based on UCA,
     * with the attributes and re-ordering of the characters specified in the
     * argument rules.
     * </p>
     * <p>See the user guide's section on
     * <a href=http://oss.software.ibm.com/icu/userguide/Collate_Customization.html>
     * Collation Customization</a> for details on the rule syntax.
     * </p>
     * @param rules the collation rules to build the collation table from.
     * @exception ParseException and IOException thrown. ParseException thrown
     *            when argument rules have an invalid syntax. IOException
     *            thrown when an error occured while reading internal data.
     * @stable ICU 2.8
     */
    public RuleBasedCollator(String rules) throws Exception
    {
        if (rules == null) {
            throw new IllegalArgumentException(
                                            "Collation rules can not be null");
        }
        init(rules);
    }

    // public methods --------------------------------------------------------

    /**
     * Clones the RuleBasedCollator
     * @return a new instance of this RuleBasedCollator object
     * @stable ICU 2.8
     */
    public Object clone() throws CloneNotSupportedException
    {
        RuleBasedCollator result = (RuleBasedCollator)super.clone();
        // since all collation data in the RuleBasedCollator do not change
        // we can safely assign the result.fields to this collator
        result.initUtility(); // let the new clone have their own util
                                    // iterators
        return result;
    }

    /**
     * Return a CollationElementIterator for the given String.
     * @see CollationElementIterator
     * @stable ICU 2.8
     */
    public CollationElementIterator getCollationElementIterator(String source)
    {
        return new CollationElementIterator(source, this);
    }

    /**
     * Return a CollationElementIterator for the given CharacterIterator.
     * The source iterator's integrity will be preserved since a new copy
     * will be created for use.
     * @see CollationElementIterator
     * @stable ICU 2.8
     */
    public CollationElementIterator getCollationElementIterator(
                                                CharacterIterator source)
    {
        CharacterIterator newsource = (CharacterIterator)source.clone();
        return new CollationElementIterator(newsource, this);
    }
    
    /**
     * Return a CollationElementIterator for the given UCharacterIterator.
     * The source iterator's integrity will be preserved since a new copy
     * will be created for use.
     * @see CollationElementIterator
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public CollationElementIterator getCollationElementIterator(
                                                UCharacterIterator source)
    {
        return new CollationElementIterator(source, this);
    }

    // public setters --------------------------------------------------------

    /**
     * Sets the Hiragana Quaternary mode to be on or off.
     * When the Hiragana Quaternary mode is turned on, the collator
     * positions Hiragana characters before all non-ignorable characters in
     * QUATERNARY strength. This is to produce a correct JIS collation order,
     * distinguishing between Katakana  and Hiragana characters.
     * @param flag true if Hiragana Quaternary mode is to be on, false
     *        otherwise
     * @see #setHiraganaQuaternaryDefault
     * @see #isHiraganaQuaternary
     * @stable ICU 2.8
     */
    public void setHiraganaQuaternary(boolean flag)
    {
        m_isHiragana4_ = flag;
        updateInternalState();        
    }

    /**
     * Sets the Hiragana Quaternary mode to the initial mode set during
     * construction of the RuleBasedCollator.
     * See setHiraganaQuaternary(boolean) for more details.
     * @see #setHiraganaQuaternary(boolean)
     * @see #isHiraganaQuaternary
     * @stable ICU 2.8
     */
    public void setHiraganaQuaternaryDefault()
    {
        m_isHiragana4_ = m_defaultIsHiragana4_;
        updateInternalState();
    }

    /**
     * Sets whether uppercase characters sort before lowercase
     * characters or vice versa, in strength TERTIARY. The default
     * mode is false, and so lowercase characters sort before uppercase
     * characters.
     * If true, sort upper case characters first.
     * @param upperfirst true to sort uppercase characters before
     *                   lowercase characters, false to sort lowercase
     *                   characters before uppercase characters
     * @see #isLowerCaseFirst
     * @see #isUpperCaseFirst
     * @see #setLowerCaseFirst
     * @see #setCaseFirstDefault
     * @stable ICU 2.8
     */
    public void setUpperCaseFirst(boolean upperfirst)
    {
        if (upperfirst) {
            if(m_caseFirst_ != AttributeValue.UPPER_FIRST_) {
                latinOneRegenTable_ = true;
            }
            m_caseFirst_ = AttributeValue.UPPER_FIRST_;
        }
        else {
            if(m_caseFirst_ != AttributeValue.OFF_) {
                latinOneRegenTable_ = true;
            }
            m_caseFirst_ = AttributeValue.OFF_;
        }
        updateInternalState();
    }

    /**
     * Sets the orders of lower cased characters to sort before upper cased
     * characters, in strength TERTIARY. The default
     * mode is false.
     * If true is set, the RuleBasedCollator will sort lower cased characters
     * before the upper cased ones.
     * Otherwise, if false is set, the RuleBasedCollator will ignore case
     * preferences.
     * @param lowerfirst true for sorting lower cased characters before
     *                   upper cased characters, false to ignore case
     *                   preferences.
     * @see #isLowerCaseFirst
     * @see #isUpperCaseFirst
     * @see #setUpperCaseFirst
     * @see #setCaseFirstDefault
     * @stable ICU 2.8
     */
    public void setLowerCaseFirst(boolean lowerfirst)
    {
        if (lowerfirst) {
                if(m_caseFirst_ != AttributeValue.LOWER_FIRST_) {
                    latinOneRegenTable_ = true;
                }
                m_caseFirst_ = AttributeValue.LOWER_FIRST_;
        }
        else {
                if(m_caseFirst_ != AttributeValue.OFF_) {
                    latinOneRegenTable_ = true;
                }
            m_caseFirst_ = AttributeValue.OFF_;
            }
        updateInternalState();
    }

    /**
     * Sets the case first mode to the initial mode set during
     * construction of the RuleBasedCollator.
     * See setUpperCaseFirst(boolean) and setLowerCaseFirst(boolean) for more
     * details.
     * @see #isLowerCaseFirst
     * @see #isUpperCaseFirst
     * @see #setLowerCaseFirst(boolean)
     * @see #setUpperCaseFirst(boolean)
     * @stable ICU 2.8
     */
    public final void setCaseFirstDefault()
    {
        if(m_caseFirst_ != m_defaultCaseFirst_) {
            latinOneRegenTable_ = true;
        }
        m_caseFirst_ = m_defaultCaseFirst_;
        updateInternalState();
    }

    /**
     * Sets the alternate handling mode to the initial mode set during
     * construction of the RuleBasedCollator.
     * See setAlternateHandling(boolean) for more details.
     * @see #setAlternateHandlingShifted(boolean)
     * @see #isAlternateHandlingShifted()
     * @stable ICU 2.8
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
     * @stable ICU 2.8
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
     * @stable ICU 2.8
     */
    public void setDecompositionDefault()
    {
        setDecomposition(m_defaultDecomposition_);
        updateInternalState();        
    }

    /**
     * Sets the French collation mode to the initial mode set during
     * construction of the RuleBasedCollator.
     * See setFrenchCollation(boolean) for more details.
     * @see #isFrenchCollation
     * @see #setFrenchCollation(boolean)
     * @stable ICU 2.8
     */
    public void setFrenchCollationDefault()
    {
        if(m_isFrenchCollation_ != m_defaultIsFrenchCollation_) {
            latinOneRegenTable_ = true;
        }
        m_isFrenchCollation_ = m_defaultIsFrenchCollation_;
        updateInternalState();
    }

    /**
     * Sets the collation strength to the initial mode set during the
     * construction of the RuleBasedCollator.
     * See setStrength(int) for more details.
     * @see #setStrength(int)
     * @see #getStrength
     * @stable ICU 2.8
     */
    public void setStrengthDefault()
    {
        setStrength(m_defaultStrength_);
        updateInternalState();        
    }
    
    /**
     * Method to set numeric collation to its default value.
     * When numeric collation is turned on, this Collator generates a collation 
     * key for the numeric value of substrings of digits. This is a way to get 
     * '100' to sort AFTER '2'
     * @see #getNumericCollation
     * @see #setNumericCollation
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void setNumericCollationDefault()
    {
        setNumericCollation(m_defaultIsNumericCollation_);
        updateInternalState();        
    }

    /**
     * Sets the mode for the direction of SECONDARY weights to be used in
     * French collation.
     * The default value is false, which treats SECONDARY weights in the order
     * they appear.
     * If set to true, the SECONDARY weights will be sorted backwards.
     * See the section on
     * <a href=http://oss.software.ibm.com/icu/userguide/Collate_ServiceArchitecture.html>
     * French collation</a> for more information.
     * @param flag true to set the French collation on, false to set it off
     * @stable ICU 2.8
     * @see #isFrenchCollation
     * @see #setFrenchCollationDefault
     */
    public void setFrenchCollation(boolean flag)
    {
        if(m_isFrenchCollation_ != flag) {
            latinOneRegenTable_ = true;
        }
        m_isFrenchCollation_ = flag;
        updateInternalState();
    }

    /**
     * Sets the alternate handling for QUATERNARY strength to be either
     * shifted or non-ignorable.
     * See the UCA definition on
     * <a href="http://www.unicode.org/unicode/reports/tr10/#Variable_Weighting">
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
     * @see #isAlternateHandlingShifted
     * @see #setAlternateHandlingDefault
     * @stable ICU 2.8
     */
    public void setAlternateHandlingShifted(boolean shifted)
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
     * The default value is false, which means the case level is not generated.
     * The contents of the case level are affected by the case first
     * mode. A simple way to ignore accent differences in a string is to set
     * the strength to PRIMARY and enable case level.
     * </p>
     * <p>
     * See the section on
     * <a href=http://oss.software.ibm.com/icu/userguide/Collate_ServiceArchitecture.html>
     * case level</a> for more information.
     * </p>
     * @param flag true if case level sorting is required, false otherwise
     * @stable ICU 2.8
     * @see #setCaseLevelDefault
     * @see #isCaseLevel
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
     * @param newStrength the new strength value.
     * @see #getStrength
     * @see #setStrengthDefault
     * @see #PRIMARY
     * @see #SECONDARY
     * @see #TERTIARY
     * @see #QUATERNARY
     * @see #IDENTICAL
     * @exception IllegalArgumentException If the new strength value is not one
     *              of PRIMARY, SECONDARY, TERTIARY, QUATERNARY or IDENTICAL.
     * @stable ICU 2.8
     */
    public void setStrength(int newStrength)
    {
        super.setStrength(newStrength);
        updateInternalState();
    }
    
    /** 
     * <p>
     * Variable top is a two byte primary value which causes all the codepoints 
     * with primary values that are less or equal than the variable top to be 
     * shifted when alternate handling is set to SHIFTED.
     * </p>
     * <p>
     * Sets the variable top to a collation element value of a string supplied.
     * </p> 
     * @param varTop one or more (if contraction) characters to which the 
     *               variable top should be set
     * @return a int value containing the value of the variable top in upper 16
     *         bits. Lower 16 bits are undefined.
     * @exception IllegalArgumentException is thrown if varTop argument is not 
     *            a valid variable top element. A variable top element is 
     *            invalid when 
     *            <ul>
     *            <li>it is a contraction that does not exist in the
     *                Collation order
     *            <li>when the PRIMARY strength collation element for the 
     *                variable top has more than two bytes
     *            <li>when the varTop argument is null or zero in length.
     *            </ul>
     * @see #getVariableTop
     * @see RuleBasedCollator#setAlternateHandlingShifted
     * @stable ICU 2.6
     */
    public int setVariableTop(String varTop)
    {
        if (varTop == null || varTop.length() == 0) {
            throw new IllegalArgumentException(
            "Variable top argument string can not be null or zero in length.");
        }
        
        m_srcUtilColEIter_.setText(varTop);
        int ce = m_srcUtilColEIter_.next();
        
        // here we check if we have consumed all characters 
        // you can put in either one character or a contraction
        // you shouldn't put more... 
        if (m_srcUtilColEIter_.getOffset() != varTop.length() 
            || ce == CollationElementIterator.NULLORDER) {
            throw new IllegalArgumentException(
            "Variable top argument string is a contraction that does not exist "
            + "in the Collation order");
        }
        
        int nextCE = m_srcUtilColEIter_.next();
        
        if ((nextCE != CollationElementIterator.NULLORDER) 
            && (!isContinuation(nextCE) || (nextCE & CE_PRIMARY_MASK_) != 0)) {
                throw new IllegalArgumentException(
                "Variable top argument string can only have a single collation "
                + "element that has less than or equal to two PRIMARY strength "
                + "bytes");
        }
        
        m_variableTopValue_ = (ce & CE_PRIMARY_MASK_) >> 16;
        
        return ce & CE_PRIMARY_MASK_;
    }
    
    /** 
     * Sets the variable top to a collation element value supplied.
     * Variable top is set to the upper 16 bits. 
     * Lower 16 bits are ignored.
     * @param varTop Collation element value, as returned by setVariableTop or 
     *               getVariableTop
     * @see #getVariableTop
     * @see #setVariableTop
     * @stable ICU 2.6
     */
    public void setVariableTop(int varTop)
    {
        m_variableTopValue_ = (varTop & CE_PRIMARY_MASK_) >> 16;
    }
    
    /**
     * When numeric collation is turned on, this Collator generates a collation 
     * key for the numeric value of substrings of digits. This is a way to get 
     * '100' to sort AFTER '2'
     * @param flag true to turn numeric collation on and false to turn it off
     * @see #getNumericCollation
     * @see #setNumericCollationDefault
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void setNumericCollation(boolean flag)
    {
        // sort substrings of digits as numbers
        m_isNumericCollation_ = flag;
        updateInternalState();
    }

    // public getters --------------------------------------------------------

    /**
     * Gets the collation rules for this RuleBasedCollator.
     * Equivalent to String getRules(RuleOption.FULL_RULES).
     * @return returns the collation rules
     * @see #getRules(boolean)
     * @stable ICU 2.8
     */
    public String getRules()
    {
        return m_rules_;
    }
    
    /**
     * Returns current rules. The argument defines whether full rules 
     * (UCA + tailored) rules are returned or just the tailoring. 
     * @param fullrules true if the rules that defines the full set of 
     *        collation order is required, otherwise false for returning only 
     *        the tailored rules
     * @return the current rules that defines this Collator.
     * @see #getRules
     * @stable ICU 2.6
     */
    public String getRules(boolean fullrules)
    {
        if (!fullrules) {
            return m_rules_;
        }
        // take the UCA rules and append real rules at the end 
        return UCA_.m_rules_.concat(m_rules_);
    }

    /**
     * Get an UnicodeSet that contains all the characters and sequences
     * tailored in this collator.
     * @return a pointer to a UnicodeSet object containing all the
     *         code points and sequences that may sort differently than
     *         in the UCA.
     * @exception ParseException thrown when argument rules have an
     *            invalid syntax. IOException
     * @stable ICU 2.4
     */
    public UnicodeSet getTailoredSet()
    {
        try {
           CollationRuleParser src = new CollationRuleParser(getRules());
           return src.getTailoredSet();
        } catch(Exception e) {
            throw new InternalError("A tailoring rule should not have errors. Something is quite wrong!");
        }
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
     * @see #getRawCollationKey
     * @stable ICU 2.8
     */
    public CollationKey getCollationKey(String source) {
        if (source == null) {
            return null;
        }
        m_utilRawCollationKey_ = getRawCollationKey(source, 
                                                    m_utilRawCollationKey_);
        return new CollationKey(source, m_utilRawCollationKey_);
    }
    
    /**
     * Gets the simpler form of a CollationKey for the String source following
     * the rules of this Collator and stores the result into the user provided 
     * argument key. 
     * If key has a internal byte array of length that's too small for the 
     * result, the internal byte array will be grown to the exact required 
     * size.
     * @param source the text String to be transformed into a RawCollationKey  
     * @param key output RawCollationKey to store results
     * @return If key is null, a new instance of RawCollationKey will be 
     *         created and returned, otherwise the user provided key will be 
     *         returned.
     * @see #getCollationKey 
     * @see #compare(String, String)
     * @see RawCollationKey
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public RawCollationKey getRawCollationKey(String source, 
                                              RawCollationKey key)
    {
        if (source == null) {
            return null;
        }
        int strength = getStrength();
        m_utilCompare0_ = m_isCaseLevel_;
        m_utilCompare1_ = true;
        m_utilCompare2_ = strength >= SECONDARY;
        m_utilCompare3_ = strength >= TERTIARY;
        m_utilCompare4_ = strength >= QUATERNARY;
        m_utilCompare5_ = strength == IDENTICAL;

        m_utilBytesCount0_ = 0;
        m_utilBytesCount1_ = 0;
        m_utilBytesCount2_ = 0;
        m_utilBytesCount3_ = 0;
        m_utilBytesCount4_ = 0;
        m_utilBytesCount5_ = 0;
        m_utilCount0_ = 0;
        m_utilCount1_ = 0;
        m_utilCount2_ = 0;
        m_utilCount3_ = 0;
        m_utilCount4_ = 0;
        m_utilCount5_ = 0;
        boolean doFrench = m_isFrenchCollation_ && m_utilCompare2_;
        // TODO: UCOL_COMMON_BOT4 should be a function of qShifted.
        // If we have no qShifted, we don't need to set UCOL_COMMON_BOT4 so
        // high.
        int commonBottom4 = ((m_variableTopValue_ >>> 8) + 1) & LAST_BYTE_MASK_;
        byte hiragana4 = 0;
        if (m_isHiragana4_ && m_utilCompare4_) {
            // allocate one more space for hiragana, value for hiragana
            hiragana4 = (byte)commonBottom4;
            commonBottom4 ++;
        }

        int bottomCount4 = 0xFF - commonBottom4;
        // If we need to normalize, we'll do it all at once at the beginning!
        if (m_utilCompare5_ && Normalizer.quickCheck(source, Normalizer.NFD,0)
                                                    != Normalizer.YES) {
            // if it is identical strength, we have to normalize the string to
            // NFD so that it will be appended correctly to the end of the sort
            // key
            source = Normalizer.decompose(source, false);
        }
        else if (getDecomposition() != NO_DECOMPOSITION
            && Normalizer.quickCheck(source, Normalizer.FCD,0)
                                                    != Normalizer.YES) {
            // for the rest of the strength, if decomposition is on, FCD is
            // enough for us to work on.
            source = Normalizer.normalize(source,Normalizer.FCD);
        }
        getSortKeyBytes(source, doFrench, hiragana4, commonBottom4,
                        bottomCount4);
        if (key == null) {
            key = new RawCollationKey();
        }
        getSortKey(source, doFrench, commonBottom4, bottomCount4, key);
        return key;
    }

    /**
     * Return true if an uppercase character is sorted before the corresponding lowercase character.
     * See setCaseFirst(boolean) for details.
     * @see #setUpperCaseFirst
     * @see #setLowerCaseFirst
     * @see #isLowerCaseFirst
     * @see #setCaseFirstDefault
     * @return true if upper cased characters are sorted before lower cased
     *         characters, false otherwise
     * @stable ICU 2.8
     */
     public boolean isUpperCaseFirst()
     {
        return (m_caseFirst_ == AttributeValue.UPPER_FIRST_);
     }
     
    /**
     * Return true if a lowercase character is sorted before the corresponding uppercase character.
     * See setCaseFirst(boolean) for details.
     * @see #setUpperCaseFirst
     * @see #setLowerCaseFirst
     * @see #isUpperCaseFirst
     * @see #setCaseFirstDefault
     * @return true lower cased characters are sorted before upper cased
     *         characters, false otherwise
     * @stable ICU 2.8
     */
    public boolean isLowerCaseFirst()
    {
        return (m_caseFirst_ == AttributeValue.LOWER_FIRST_);
    }

    /**
     * Checks if the alternate handling behaviour is the UCA defined SHIFTED or
     * NON_IGNORABLE.
     * If return value is true, then the alternate handling attribute for the
     * Collator is SHIFTED. Otherwise if return value is false, then the
     * alternate handling attribute for the Collator is NON_IGNORABLE
     * See setAlternateHandlingShifted(boolean) for more details.
     * @return true or false
     * @see #setAlternateHandlingShifted(boolean)
     * @see #setAlternateHandlingDefault
     * @stable ICU 2.8
     */
    public boolean isAlternateHandlingShifted()
    {
        return m_isAlternateHandlingShifted_;
    }

    /**
     * Checks if case level is set to true.
     * See setCaseLevel(boolean) for details.
     * @return the case level mode
     * @see #setCaseLevelDefault
     * @see #isCaseLevel
     * @see #setCaseLevel(boolean)
     * @stable ICU 2.8
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
     * @stable ICU 2.8
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
     * @stable ICU 2.8
     */
    public boolean isHiraganaQuaternary()
    {
        return m_isHiragana4_;
    }

    /** 
     * Gets the variable top value of a Collator. 
     * Lower 16 bits are undefined and should be ignored.
     * @return the variable top value of a Collator.
     * @see #setVariableTop
     * @stable ICU 2.6
     */
    public int getVariableTop()
    {
          return m_variableTopValue_ << 16;
    }
    
    /** 
     * Method to retrieve the numeric collation value.
     * When numeric collation is turned on, this Collator generates a collation 
     * key for the numeric value of substrings of digits. This is a way to get 
     * '100' to sort AFTER '2'
     * @see #setNumericCollation
     * @see #setNumericCollationDefault
     * @return true if numeric collation is turned on, false otherwise
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public boolean getNumericCollation()
    {
        return m_isNumericCollation_;
    }
    
    // public other methods -------------------------------------------------

    /**
     * Compares the equality of two RuleBasedCollator objects.
     * RuleBasedCollator objects are equal if they have the same collation
     * rules and the same attributes.
     * @param obj the RuleBasedCollator to be compared to.
     * @return true if this RuleBasedCollator has exactly the same
     *         collation behaviour as obj, false otherwise.
     * @stable ICU 2.8
     */
    public boolean equals(Object obj)
    {
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
        if (getStrength() != other.getStrength()
               || getDecomposition() != other.getDecomposition()
               || other.m_caseFirst_ != m_caseFirst_
               || other.m_caseSwitch_ != m_caseSwitch_
               || other.m_isAlternateHandlingShifted_
                                             != m_isAlternateHandlingShifted_
               || other.m_isCaseLevel_ != m_isCaseLevel_
               || other.m_isFrenchCollation_ != m_isFrenchCollation_
               || other.m_isHiragana4_ != m_isHiragana4_) {
            return false;
        }
        boolean rules = m_rules_ == other.m_rules_;
        if (!rules && (m_rules_ != null && other.m_rules_ != null)) {
            rules = m_rules_.equals(other.m_rules_);
        }
        if (!rules || !ICUDebug.enabled("collation")) {
            return rules;
        }
        if (m_addition3_ != other.m_addition3_
                  || m_bottom3_ != other.m_bottom3_
                  || m_bottomCount3_ != other.m_bottomCount3_
                  || m_common3_ != other.m_common3_
                  || m_isSimple3_ != other.m_isSimple3_
                  || m_mask3_ != other.m_mask3_
                  || m_minContractionEnd_ != other.m_minContractionEnd_
                  || m_minUnsafe_ != other.m_minUnsafe_
                  || m_top3_ != other.m_top3_
                  || m_topCount3_ != other.m_topCount3_
                  || !Arrays.equals(m_unsafe_, other.m_unsafe_)) {
            return false;
        }
        if (!m_trie_.equals(other.m_trie_)) {
            // we should use the trie iterator here, but then this part is
            // only used in the test.
            for (int i = UCharacter.MAX_VALUE; i >= UCharacter.MIN_VALUE; i --)
            {
                int v = m_trie_.getCodePointValue(i);
                int otherv = other.m_trie_.getCodePointValue(i);
                if (v != otherv) {
                    int mask = v & (CE_TAG_MASK_ | CE_SPECIAL_FLAG_);
                    if (mask == (otherv & 0xff000000)) {
                        v &= 0xffffff;
                        otherv &= 0xffffff;
                        if (mask == 0xf1000000) {
                            v -= (m_expansionOffset_ << 4);
                            otherv -= (other.m_expansionOffset_ << 4);
                        }
                        else if (mask == 0xf2000000) {
                            v -= m_contractionOffset_;
                            otherv -= other.m_contractionOffset_;
                        }
                        if (v == otherv) {
                            continue;
                        }
                    }
                    return false;
                }
            }
        }
        if (Arrays.equals(m_contractionCE_, other.m_contractionCE_)
            && Arrays.equals(m_contractionEnd_, other.m_contractionEnd_)
            && Arrays.equals(m_contractionIndex_, other.m_contractionIndex_)
            && Arrays.equals(m_expansion_, other.m_expansion_)
            && Arrays.equals(m_expansionEndCE_, other.m_expansionEndCE_)) {
            // not comparing paddings
            for (int i = 0; i < m_expansionEndCE_.length; i ++) {
                 if (m_expansionEndCEMaxSize_[i]
                     != other.m_expansionEndCEMaxSize_[i]) {
                     return false;
                 }
                 return true;
            }
        }
        return false;
    }

    /**
     * Generates a unique hash code for this RuleBasedCollator.
     * @return the unique hash code for this Collator
     * @stable ICU 2.8
     */
    public int hashCode()
    {
        String rules = getRules();
        if (rules == null) {
            rules = "";
        }
        return rules.hashCode();
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
     * If speed performance is critical and object instantiation is to be 
     * reduced, further optimization may be achieved by generating a simpler 
     * key of the form RawCollationKey and reusing this RawCollationKey 
     * object with the method RuleBasedCollator.getRawCollationKey. Internal 
     * byte representation can be directly accessed via RawCollationKey and
     * stored for future use. Like CollationKey, RawCollationKey provides a
     * method RawCollationKey.compareTo for key comparisons.
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
     * @stable ICU 2.8
     */
    public int compare(String source, String target)
    {
        if (source == target) {
            return 0;
        }

        // Find the length of any leading portion that is equal
        int offset = getFirstUnmatchedOffset(source, target);
        //return compareRegular(source, target, offset);
        if(latinOneUse_) {
          if ((offset < source.length() 
               && source.charAt(offset) > ENDOFLATINONERANGE_) 
              || (offset < target.length() 
                  && target.charAt(offset) > ENDOFLATINONERANGE_)) { 
              // source or target start with non-latin-1
            return compareRegular(source, target, offset);
          } else {
            return compareUseLatin1(source, target, offset);
          }
        } else {
          return compareRegular(source, target, offset);
        }
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
         * @stable ICU 2.8
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

    /**
     * UCAConstants
     */
    static final class UCAConstants
    {
         int FIRST_TERTIARY_IGNORABLE_[] = new int[2];       // 0x00000000
         int LAST_TERTIARY_IGNORABLE_[] = new int[2];        // 0x00000000
         int FIRST_PRIMARY_IGNORABLE_[] = new int[2];        // 0x00008705
         int FIRST_SECONDARY_IGNORABLE_[] = new int[2];      // 0x00000000
         int LAST_SECONDARY_IGNORABLE_[] = new int[2];       // 0x00000500
         int LAST_PRIMARY_IGNORABLE_[] = new int[2];         // 0x0000DD05
         int FIRST_VARIABLE_[] = new int[2];                 // 0x05070505
         int LAST_VARIABLE_[] = new int[2];                  // 0x13CF0505
         int FIRST_NON_VARIABLE_[] = new int[2];             // 0x16200505
         int LAST_NON_VARIABLE_[] = new int[2];              // 0x767C0505
         int RESET_TOP_VALUE_[] = new int[2];                // 0x9F000303
         int FIRST_IMPLICIT_[] = new int[2];
         int LAST_IMPLICIT_[] = new int[2];
         int FIRST_TRAILING_[] = new int[2];
         int LAST_TRAILING_[] = new int[2];
         int PRIMARY_TOP_MIN_;
         int PRIMARY_IMPLICIT_MIN_; // 0xE8000000
         int PRIMARY_IMPLICIT_MAX_; // 0xF0000000
         int PRIMARY_TRAILING_MIN_; // 0xE8000000
         int PRIMARY_TRAILING_MAX_; // 0xF0000000
         int PRIMARY_SPECIAL_MIN_; // 0xE8000000
         int PRIMARY_SPECIAL_MAX_; // 0xF0000000
    }

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
    boolean m_defaultIsNumericCollation_;
    
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
    /**
     * Numeric collation option
     */
    boolean m_isNumericCollation_;

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
     * General version of the collator
     */
    VersionInfo m_version_;
    /**
     * UCA version
     */
    VersionInfo m_UCA_version_;
    /**
     * UCD version
     */
    VersionInfo m_UCD_version_;

    /**
     * UnicodeData.txt property object
     */
    static final RuleBasedCollator UCA_;
    /**
     * UCA Constants
     */
    static final UCAConstants UCA_CONSTANTS_;
    /**
     * Table for UCA and builder use
     */
    static final char UCA_CONTRACTIONS_[];
    /**
     * Implicit generator
     */
    static final ImplicitCEGenerator impCEGen_;
//    /**
//     * Implicit constants
//     */
//    static final int IMPLICIT_BASE_BYTE_;
//    static final int IMPLICIT_LIMIT_BYTE_;
//    static final int IMPLICIT_4BYTE_BOUNDARY_;
//    static final int LAST_MULTIPLIER_;
//    static final int LAST2_MULTIPLIER_;
//    static final int IMPLICIT_BASE_3BYTE_;
//    static final int IMPLICIT_BASE_4BYTE_;
//    static final int BYTES_TO_AVOID_ = 3;
//    static final int OTHER_COUNT_ = 256 - BYTES_TO_AVOID_;
//    static final int LAST_COUNT_ = OTHER_COUNT_ / 2;
//    /**
//     * Room for intervening, without expanding to 5 bytes
//     */
//    static final int LAST_COUNT2_ = OTHER_COUNT_ / 21;
//    static final int IMPLICIT_3BYTE_COUNT_ = 1;
//    
    static final byte SORT_LEVEL_TERMINATOR_ = 1;


    // block to initialise character property database
    static
    {
        try
        {
            UCA_ = new RuleBasedCollator();
            UCA_CONSTANTS_ = new UCAConstants();
            UCA_CONTRACTIONS_ = CollatorReader.read(UCA_, UCA_CONSTANTS_);
            /*
            InputStream i = UCA_.getClass().getResourceAsStream(
                                        "/com/ibm/icu/impl/data/ucadata.icu");

            BufferedInputStream b = new BufferedInputStream(i, 90000);
            CollatorReader reader = new CollatorReader(b);
            UCA_CONTRACTIONS_ = reader.read(UCA_, UCA_CONSTANTS_);
            b.close();
            i.close();
            */
            // called before doing canonical closure for the UCA.
            impCEGen_ = new ImplicitCEGenerator(UCA_CONSTANTS_.PRIMARY_IMPLICIT_MIN_, UCA_CONSTANTS_.PRIMARY_IMPLICIT_MAX_);
//            IMPLICIT_BASE_BYTE_ = UCA_CONSTANTS_.PRIMARY_IMPLICIT_MIN_;
//            // leave room for 1 3-byte and 2 4-byte forms
//            IMPLICIT_LIMIT_BYTE_ = IMPLICIT_BASE_BYTE_ + 4;
//            IMPLICIT_4BYTE_BOUNDARY_ = IMPLICIT_3BYTE_COUNT_ * OTHER_COUNT_
//                                       * LAST_COUNT_;
//            LAST_MULTIPLIER_ = OTHER_COUNT_ / LAST_COUNT_;
//            LAST2_MULTIPLIER_ = OTHER_COUNT_ / LAST_COUNT2_;
//            IMPLICIT_BASE_3BYTE_ = (IMPLICIT_BASE_BYTE_ << 24) + 0x030300;
//            IMPLICIT_BASE_4BYTE_ = ((IMPLICIT_BASE_BYTE_
//                                     + IMPLICIT_3BYTE_COUNT_) << 24) + 0x030303;
            UCA_.init();
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME, ULocale.ENGLISH);
            UCA_.m_rules_ = (String)rb.getObject("%%UCARULES");
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
    */
    RuleBasedCollator()
    {
        initUtility();
    }

    /**
     * Constructors a RuleBasedCollator from the argument locale.
     * If no resource bundle is associated with the locale, UCA is used
     * instead.
     * @param locale
     */
    RuleBasedCollator(ULocale locale)
    {
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME, locale);
        initUtility();
        if (rb != null) {
            try {
                // Use keywords, if supplied for lookup
                String collkey = locale.getKeywordValue("collation");
                  if(collkey == null) {
                      collkey = rb.getStringWithFallback("collations/default");
                }
                       
                // collations/default will always give a string back
                // keyword for the real collation data
                // if "collations/collkey" will return null if collkey == null 
                ICUResourceBundle elements = rb.getWithFallback("collations/" + collkey);
                if (elements != null) {
                    // TODO: Determine actual & valid locale correctly
                    ULocale uloc = rb.getULocale();
                    setLocale(uloc, uloc);

                    m_rules_ = elements.getString("Sequence");
                    ByteBuffer buf = elements.get("%%CollationBin").getBinary();
                    // %%CollationBin
                    if(buf!=null){
                    //     m_rules_ = (String)rules[1][1];
                        byte map[] = buf.array();
                        CollatorReader.initRBC(this, map);
                        /*
                        BufferedInputStream input =
                                                 new BufferedInputStream(
                                                    new ByteArrayInputStream(map));
                        /*
                        CollatorReader reader = new CollatorReader(input, false);
                        if (map.length > MIN_BINARY_DATA_SIZE_) {
                            reader.read(this, null);
                        }
                        else {
                            reader.readHeader(this);
                            reader.readOptions(this);
                            // duplicating UCA_'s data
                            setWithUCATables();
                        }
                        */
                        // at this point, we have read in the collator
                        // now we need to check whether the binary image has
                        // the right UCA and other versions
                        if(!m_UCA_version_.equals(UCA_.m_UCA_version_) ||
                        !m_UCD_version_.equals(UCA_.m_UCD_version_)) {
                            init(m_rules_);
                            return;
                        }
                        init();
                        return;
                    }
                    else {
                        // due to resource redirection ICUListResourceBundle does not
                        // raise missing resource error
                        //throw new MissingResourceException("Could not get resource for constructing RuleBasedCollator","com.ibm.icu.impl.data.LocaleElements_"+locale.toString(), "%%CollationBin");
                        
                        init(m_rules_);
                        return;
                    }
                }
            }
            catch (Exception e) {
                // e.printStackTrace();
                // if failed use UCA.
            }
        }
        setWithUCAData();
    }

    // package private methods -----------------------------------------------

    /**
     * Sets this collator to use the tables in UCA. Note options not taken
     * care of here.
     */
    final void setWithUCATables()
    {
        m_contractionOffset_ = UCA_.m_contractionOffset_;
        m_expansionOffset_ = UCA_.m_expansionOffset_;
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
        latinOneFailed_ = true;

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
        m_defaultIsNumericCollation_ = UCA_.m_defaultIsNumericCollation_;
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
        m_isNumericCollation_ = UCA_.m_isNumericCollation_;
        setWithUCATables();
        latinOneFailed_ = false;
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
            if (UTF16.isLeadSurrogate(ch) 
                || UTF16.isTrailSurrogate(ch)) {
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

    private int m_caseSwitch_;
    private int m_common3_;
    private int m_mask3_;
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
    /*private*/ static final byte BYTE_UNSHIFTED_MIN_ = BYTE_SHIFT_PREFIX_;
    private static final byte BYTE_FIRST_UCA_ = BYTE_COMMON_;
    static final byte CODAN_PLACEHOLDER = 0x24;
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
    //private static final int MIN_BINARY_DATA_SIZE_ = (42 + 25) << 2;

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

    /**
     * CE buffer size
     */
    private static final int CE_BUFFER_SIZE_ = 512;

    // variables for Latin-1 processing
    boolean latinOneUse_        = false;
    boolean latinOneRegenTable_ = false;
    boolean latinOneFailed_     = false;

    int latinOneTableLen_ = 0;
    int latinOneCEs_[] = null;
    /**
     * Bunch of utility iterators
     */
    private StringUCharacterIterator m_srcUtilIter_;
    private CollationElementIterator m_srcUtilColEIter_;
    private StringUCharacterIterator m_tgtUtilIter_;
    private CollationElementIterator m_tgtUtilColEIter_;
    /**
     * Utility comparison flags
     */
    private boolean m_utilCompare0_;
    private boolean m_utilCompare1_;
    private boolean m_utilCompare2_;
    private boolean m_utilCompare3_;
    private boolean m_utilCompare4_;
    private boolean m_utilCompare5_;
    /**
     * Utility byte buffer
     */
    private byte m_utilBytes0_[];
    private byte m_utilBytes1_[];
    private byte m_utilBytes2_[];
    private byte m_utilBytes3_[];
    private byte m_utilBytes4_[];
    private byte m_utilBytes5_[];
    private RawCollationKey m_utilRawCollationKey_;

    private int m_utilBytesCount0_;
    private int m_utilBytesCount1_;
    private int m_utilBytesCount2_;
    private int m_utilBytesCount3_;
    private int m_utilBytesCount4_;
    private int m_utilBytesCount5_;
    private int m_utilCount0_;
    private int m_utilCount1_;
    private int m_utilCount2_;
    private int m_utilCount3_;
    private int m_utilCount4_;
    private int m_utilCount5_;

    private int m_utilFrenchStart_;
    private int m_utilFrenchEnd_;

    /**
     * Preparing the CE buffers. will be filled during the primary phase
     */
    private int m_srcUtilCEBuffer_[];
    private int m_tgtUtilCEBuffer_[];
    private int m_srcUtilCEBufferSize_;
    private int m_tgtUtilCEBufferSize_;

    private int m_srcUtilContOffset_;
    private int m_tgtUtilContOffset_;

    private int m_srcUtilOffset_;
    private int m_tgtUtilOffset_;

    // private methods -------------------------------------------------------

    private void init(String rules) throws Exception
    {
        setWithUCAData();
        CollationParsedRuleBuilder builder
                                       = new CollationParsedRuleBuilder(rules);
        builder.setRules(this);
        m_rules_ = rules;
        init();
        initUtility();
    }
    
    private final int compareRegular(String source, String target, int offset) {
        int strength = getStrength();
        // setting up the collator parameters
        m_utilCompare0_ = m_isCaseLevel_;
        m_utilCompare1_ = true;
        m_utilCompare2_ = strength >= SECONDARY;
        m_utilCompare3_ = strength >= TERTIARY;
        m_utilCompare4_ = strength >= QUATERNARY;
        m_utilCompare5_ = strength == IDENTICAL;
        boolean doFrench = m_isFrenchCollation_ && m_utilCompare2_;
        boolean doShift4 = m_isAlternateHandlingShifted_ && m_utilCompare4_;
        boolean doHiragana4 = m_isHiragana4_ && m_utilCompare4_;

        if (doHiragana4 && doShift4) {
            String sourcesub = source.substring(offset);
            String targetsub = target.substring(offset);
            return compareBySortKeys(sourcesub, targetsub);
        }

        // This is the lowest primary value that will not be ignored if shifted
        int lowestpvalue = m_isAlternateHandlingShifted_
                                            ? m_variableTopValue_ << 16 : 0;
        m_srcUtilCEBufferSize_ = 0;
        m_tgtUtilCEBufferSize_ = 0;
        int result = doPrimaryCompare(doHiragana4, lowestpvalue, source,
                                      target, offset);
        if (m_srcUtilCEBufferSize_ == -1
            && m_tgtUtilCEBufferSize_ == -1) {
            // since the cebuffer is cleared when we have determined that
            // either source is greater than target or vice versa, the return
            // result is the comparison result and not the hiragana result
            return result;
        }

        int hiraganaresult = result;

        if (m_utilCompare2_) {
            result = doSecondaryCompare(doFrench);
            if (result != 0) {
                return result;
            }
        }
        // doing the case bit
        if (m_utilCompare0_) {
            result = doCaseCompare();
            if (result != 0) {
                return result;
            }
        }
        // Tertiary level
        if (m_utilCompare3_) {
            result = doTertiaryCompare();
            if (result != 0) {
                return result;
            }
        }

        if (doShift4) {  // checkQuad
            result = doQuaternaryCompare(lowestpvalue);
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
        if (m_utilCompare5_) {
            return doIdenticalCompare(source, target, offset, true);
        }
        return 0;
    }

    /**
     * Gets the 2 bytes of primary order and adds it to the primary byte array
     * @param ce current ce
     * @param notIsContinuation flag indicating if the current bytes belong to
     *          a continuation ce
     * @param doShift flag indicating if ce is to be shifted
     * @param leadPrimary lead primary used for compression
     * @param commonBottom4 common byte value for Quaternary
     * @param bottomCount4 smallest byte value for Quaternary
     * @return the new lead primary for compression
     */
    private final int doPrimaryBytes(int ce, boolean notIsContinuation,
                                  boolean doShift, int leadPrimary,
                                  int commonBottom4, int bottomCount4)
    {

        int p2 = (ce >>= 16) & LAST_BYTE_MASK_; // in ints for unsigned
        int p1 = ce >>> 8;  // comparison
        if (doShift) {
            if (m_utilCount4_ > 0) {
                while (m_utilCount4_ > bottomCount4) {
                    m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                         (byte)(commonBottom4 + bottomCount4));
                    m_utilBytesCount4_ ++;
                    m_utilCount4_ -= bottomCount4;
                }
                m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                       (byte)(commonBottom4
                                              + (m_utilCount4_ - 1)));
                m_utilBytesCount4_ ++;
                m_utilCount4_ = 0;
            }
            // dealing with a variable and we're treating them as shifted
            // This is a shifted ignorable
            if (p1 != 0) {
                // we need to check this since we could be in continuation
                m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                       (byte)p1);
                m_utilBytesCount4_ ++;
            }
            if (p2 != 0) {
                m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                       (byte)p2);
                m_utilBytesCount4_ ++;
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
                        m_utilBytes1_ = append(m_utilBytes1_,
                                               m_utilBytesCount1_, (byte)p2);
                        m_utilBytesCount1_ ++;
                    }
                    else {
                        if (leadPrimary != 0) {
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                   m_utilBytesCount1_,
                                    ((p1 > leadPrimary)
                                            ? BYTE_UNSHIFTED_MAX_
                                            : BYTE_UNSHIFTED_MIN_));
                            m_utilBytesCount1_ ++;
                        }
                        if (p2 == CollationElementIterator.IGNORABLE) {
                            // one byter, not compressed
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                   m_utilBytesCount1_,
                                                   (byte)p1);
                            m_utilBytesCount1_ ++;
                            leadPrimary = 0;
                        }
                        else if (p1 < BYTE_FIRST_NON_LATIN_PRIMARY_
                              || (p1
                    > (RuleBasedCollator.UCA_CONSTANTS_.LAST_NON_VARIABLE_[0]
                                                                 >>> 24)
                                && p1
                    < (RuleBasedCollator.UCA_CONSTANTS_.FIRST_IMPLICIT_[0]
                                                                    >>> 24))) {
                                // not compressible
                                leadPrimary = 0;
                                m_utilBytes1_ = append(m_utilBytes1_,
                                                       m_utilBytesCount1_,
                                                       (byte)p1);
                                m_utilBytesCount1_ ++;
                                m_utilBytes1_ = append(m_utilBytes1_,
                                                       m_utilBytesCount1_,
                                                       (byte)p2);
                                m_utilBytesCount1_ ++;
                        }
                        else { // compress
                            leadPrimary = p1;
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                   m_utilBytesCount1_,
                                                   (byte)p1);
                            m_utilBytesCount1_ ++;
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                  m_utilBytesCount1_, (byte)p2);
                            m_utilBytesCount1_ ++;
                        }
                    }
                }
                else {
                    // continuation, add primary to the key, no compression
                    m_utilBytes1_ = append(m_utilBytes1_,
                                           m_utilBytesCount1_, (byte)p1);
                    m_utilBytesCount1_ ++;
                    if (p2 != CollationElementIterator.IGNORABLE) {
                        m_utilBytes1_ = append(m_utilBytes1_,
                                           m_utilBytesCount1_, (byte)p2);
                        // second part
                        m_utilBytesCount1_ ++;
                    }
                }
            }
        }
        return leadPrimary;
    }

    /**
     * Gets the secondary byte and adds it to the secondary byte array
     * @param ce current ce
     * @param notIsContinuation flag indicating if the current bytes belong to
     *          a continuation ce
     * @param doFrench flag indicator if french sort is to be performed
     */
    private final void doSecondaryBytes(int ce, boolean notIsContinuation,
                                        boolean doFrench)
    {
        int s = (ce >>= 8) & LAST_BYTE_MASK_; // int for comparison
        if (s != 0) {
            if (!doFrench) {
                // This is compression code.
                if (s == COMMON_2_ && notIsContinuation) {
                   m_utilCount2_ ++;
                }
                else {
                    if (m_utilCount2_ > 0) {
                        if (s > COMMON_2_) { // not necessary for 4th level.
                            while (m_utilCount2_ > TOP_COUNT_2_) {
                                m_utilBytes2_ = append(m_utilBytes2_,
                                        m_utilBytesCount2_,
                                        (byte)(COMMON_TOP_2_ - TOP_COUNT_2_));
                                m_utilBytesCount2_ ++;
                                m_utilCount2_ -= TOP_COUNT_2_;
                            }
                            m_utilBytes2_ = append(m_utilBytes2_,
                                                   m_utilBytesCount2_,
                                                   (byte)(COMMON_TOP_2_
                                                       - (m_utilCount2_ - 1)));
                            m_utilBytesCount2_ ++;
                        }
                        else {
                            while (m_utilCount2_ > BOTTOM_COUNT_2_) {
                                m_utilBytes2_ = append(m_utilBytes2_,
                                                       m_utilBytesCount2_,
                                    (byte)(COMMON_BOTTOM_2_ + BOTTOM_COUNT_2_));
                                m_utilBytesCount2_ ++;
                                m_utilCount2_ -= BOTTOM_COUNT_2_;
                            }
                            m_utilBytes2_ = append(m_utilBytes2_,
                                                   m_utilBytesCount2_,
                                                   (byte)(COMMON_BOTTOM_2_
                                                       + (m_utilCount2_ - 1)));
                            m_utilBytesCount2_ ++;
                        }
                        m_utilCount2_ = 0;
                    }
                    m_utilBytes2_ = append(m_utilBytes2_, m_utilBytesCount2_,
                                           (byte)s);
                    m_utilBytesCount2_ ++;
                }
            }
            else {
                  m_utilBytes2_ = append(m_utilBytes2_, m_utilBytesCount2_,
                                         (byte)s);
                  m_utilBytesCount2_ ++;
                  // Do the special handling for French secondaries
                  // We need to get continuation elements and do intermediate
                  // restore
                  // abc1c2c3de with french secondaries need to be edc1c2c3ba
                  // NOT edc3c2c1ba
                  if (notIsContinuation) {
                        if (m_utilFrenchStart_ != -1) {
                            // reverse secondaries from frenchStartPtr up to
                            // frenchEndPtr
                            reverseBuffer(m_utilBytes2_);
                            m_utilFrenchStart_ = -1;
                        }
                  }
                  else {
                        if (m_utilFrenchStart_ == -1) {
                            m_utilFrenchStart_  = m_utilBytesCount2_ - 2;
                        }
                        m_utilFrenchEnd_ = m_utilBytesCount2_ - 1;
                  }
            }
        }
    }

    /**
     * Reverse the argument buffer
     * @param buffer to reverse
     */
    private void reverseBuffer(byte buffer[])
    {
        int start = m_utilFrenchStart_;
        int end = m_utilFrenchEnd_;
        while (start < end) {
            byte b = buffer[start];
            buffer[start ++] = buffer[end];
            buffer[end --] = b;
        }
    }

    /**
     * Insert the case shifting byte if required
     * @param caseshift value
     * @return new caseshift value
     */
    private final int doCaseShift(int caseshift)
    {
        if (caseshift  == 0) {
            m_utilBytes0_ = append(m_utilBytes0_, m_utilBytesCount0_,
                                   SORT_CASE_BYTE_START_);
            m_utilBytesCount0_ ++;
            caseshift = SORT_CASE_SHIFT_START_;
        }
        return caseshift;
    }

    /**
     * Performs the casing sort
     * @param tertiary byte in ints for easy comparison
     * @param notIsContinuation flag indicating if the current bytes belong to
     *          a continuation ce
     * @param caseshift
     * @return the new value of case shift
     */
    private final int doCaseBytes(int tertiary, boolean notIsContinuation,
                                  int caseshift)
    {
        caseshift = doCaseShift(caseshift);

        if (notIsContinuation && tertiary != 0) {
            byte casebits = (byte)(tertiary & 0xC0);
            if (m_caseFirst_ == AttributeValue.UPPER_FIRST_) {
                if (casebits == 0) {
                    m_utilBytes0_[m_utilBytesCount0_ - 1]
                                                      |= (1 << (-- caseshift));
                }
                else {
                     // second bit
                     caseshift = doCaseShift(caseshift - 1);
                     m_utilBytes0_[m_utilBytesCount0_ - 1]
                                    |= ((casebits >> 6) & 1) << (-- caseshift);
                }
            }
            else {
                if (casebits != 0) {
                    m_utilBytes0_[m_utilBytesCount0_ - 1]
                                                        |= 1 << (-- caseshift);
                    // second bit
                    caseshift = doCaseShift(caseshift);
                    m_utilBytes0_[m_utilBytesCount0_ - 1]
                                  |= ((casebits >> 7) & 1) << (-- caseshift);
                }
                else {
                    caseshift --;
                }
            }
        }

        return caseshift;
    }

    /**
     * Gets the tertiary byte and adds it to the tertiary byte array
     * @param tertiary byte in int for easy comparison
     * @param notIsContinuation flag indicating if the current bytes belong to
     *          a continuation ce
     */
    private final void doTertiaryBytes(int tertiary, boolean notIsContinuation)
    {
        if (tertiary != 0) {
            // This is compression code.
            // sequence size check is included in the if clause
            if (tertiary == m_common3_ && notIsContinuation) {
                 m_utilCount3_ ++;
            }
            else {
                int common3 = m_common3_ & LAST_BYTE_MASK_;
                if (tertiary > common3 && m_common3_ == COMMON_NORMAL_3_) {
                    tertiary += m_addition3_;
                }
                else if (tertiary <= common3
                         && m_common3_ == COMMON_UPPER_FIRST_3_) {
                    tertiary -= m_addition3_;
                }
                if (m_utilCount3_ > 0) {
                    if (tertiary > common3) {
                        while (m_utilCount3_ > m_topCount3_) {
                            m_utilBytes3_ = append(m_utilBytes3_,
                                                   m_utilBytesCount3_,
                                            (byte)(m_top3_ - m_topCount3_));
                            m_utilBytesCount3_ ++;
                            m_utilCount3_ -= m_topCount3_;
                        }
                        m_utilBytes3_ = append(m_utilBytes3_,
                                               m_utilBytesCount3_,
                                               (byte)(m_top3_
                                                      - (m_utilCount3_ - 1)));
                        m_utilBytesCount3_ ++;
                    }
                    else {
                        while (m_utilCount3_ > m_bottomCount3_) {
                            m_utilBytes3_ = append(m_utilBytes3_,
                                                   m_utilBytesCount3_,
                                         (byte)(m_bottom3_ + m_bottomCount3_));
                            m_utilBytesCount3_ ++;
                            m_utilCount3_ -= m_bottomCount3_;
                        }
                        m_utilBytes3_ = append(m_utilBytes3_,
                                               m_utilBytesCount3_,
                                               (byte)(m_bottom3_
                                                      + (m_utilCount3_ - 1)));
                        m_utilBytesCount3_ ++;
                    }
                    m_utilCount3_ = 0;
                }
                m_utilBytes3_ = append(m_utilBytes3_, m_utilBytesCount3_,
                                       (byte)tertiary);
                m_utilBytesCount3_ ++;
            }
        }
    }

    /**
     * Gets the Quaternary byte and adds it to the Quaternary byte array
     * @param isCodePointHiragana flag indicator if the previous codepoint
     *          we dealt with was Hiragana
     * @param commonBottom4 smallest common Quaternary byte
     * @param bottomCount4 smallest Quaternary byte
     * @param hiragana4 hiragana Quaternary byte
     */
    private final void doQuaternaryBytes(boolean isCodePointHiragana,
                                      int commonBottom4, int bottomCount4,
                                      byte hiragana4)
    {
        if (isCodePointHiragana) { // This was Hiragana, need to note it
            if (m_utilCount4_ > 0) { // Close this part
                while (m_utilCount4_ > bottomCount4) {
                    m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                           (byte)(commonBottom4
                                                        + bottomCount4));
                    m_utilBytesCount4_ ++;
                    m_utilCount4_ -= bottomCount4;
                }
                m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                      (byte)(commonBottom4
                                             + (m_utilCount4_ - 1)));
                m_utilBytesCount4_ ++;
                m_utilCount4_ = 0;
            }
            m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                   hiragana4); // Add the Hiragana
            m_utilBytesCount4_ ++;
        }
        else { // This wasn't Hiragana, so we can continue adding stuff
            m_utilCount4_ ++;
        }
    }

    /**
     * Iterates through the argument string for all ces.
     * Split the ces into their relevant primaries, secondaries etc.
     * @param source normalized string
     * @param doFrench flag indicator if special handling of French has to be
     *                  done
     * @param hiragana4 offset for Hiragana quaternary
     * @param commonBottom4 smallest common quaternary byte
     * @param bottomCount4 smallest quaternary byte
     */
    private final void getSortKeyBytes(String source, boolean doFrench,
                                       byte hiragana4, int commonBottom4,
                                       int bottomCount4)

    {
        int backupDecomposition = getDecomposition();
        setDecomposition(NO_DECOMPOSITION); // have to revert to backup later
        m_srcUtilIter_.setText(source);
        m_srcUtilColEIter_.setText(m_srcUtilIter_);
        m_utilFrenchStart_ = -1;
        m_utilFrenchEnd_ = -1;

        // scriptorder not implemented yet
        // const uint8_t *scriptOrder = coll->scriptOrder;

        boolean doShift = false;
        boolean notIsContinuation = false;

        int leadPrimary = 0; // int for easier comparison
        int caseShift = 0;

        while (true) {
            int ce = m_srcUtilColEIter_.next();
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
            boolean isPrimaryByteIgnorable = (ce & CE_PRIMARY_MASK_) == 0;
            // actually we can just check that the first byte is 0
            // generation stuffs the order left first
            boolean isSmallerThanVariableTop = (ce >>> CE_PRIMARY_SHIFT_)
                                               <= m_variableTopValue_;
            doShift = (m_isAlternateHandlingShifted_
                        && ((notIsContinuation && isSmallerThanVariableTop
                            && !isPrimaryByteIgnorable) // primary byte not 0
                        || (!notIsContinuation && doShift))
                        || (doShift && isPrimaryByteIgnorable));
            if (doShift && isPrimaryByteIgnorable) {
                // amendment to the UCA says that primary ignorables and other
                // ignorables should be removed if following a shifted code
                // point
                // if we were shifted and we got an ignorable code point
                // we should just completely ignore it
                continue;
            }
            leadPrimary = doPrimaryBytes(ce, notIsContinuation, doShift,
                                         leadPrimary, commonBottom4,
                                         bottomCount4);
            if (doShift) {
                continue;
            }
            if (m_utilCompare2_) {
                doSecondaryBytes(ce, notIsContinuation, doFrench);
            }

            int t = ce & LAST_BYTE_MASK_;
            if (!notIsContinuation) {
                t = ce & CE_REMOVE_CONTINUATION_MASK_;
            }

            if (m_utilCompare0_) {
                caseShift = doCaseBytes(t, notIsContinuation, caseShift);
            }
            else if (notIsContinuation) {
                 t ^= m_caseSwitch_;
            }

            t &= m_mask3_;

            if (m_utilCompare3_) {
                doTertiaryBytes(t, notIsContinuation);
            }

            if (m_utilCompare4_ && notIsContinuation) { // compare quad
                doQuaternaryBytes(m_srcUtilColEIter_.m_isCodePointHiragana_,
                                  commonBottom4, bottomCount4, hiragana4);
            }
        }
        setDecomposition(backupDecomposition); // reverts to original
        if (m_utilFrenchStart_ != -1) {
            // one last round of checks
            reverseBuffer(m_utilBytes2_);
        }
    }

    /**
     * From the individual strength byte results the final compact sortkey
     * will be calculated.
     * @param source text string
     * @param doFrench flag indicating that special handling of French has to
     *                  be done
     * @param commonBottom4 smallest common quaternary byte
     * @param bottomCount4 smallest quaternary byte
     * @param key output RawCollationKey to store results, key cannot be null
     */
    private final void getSortKey(String source, boolean doFrench,
                                             int commonBottom4, 
                                             int bottomCount4,
                                             RawCollationKey key)
    {
        // we have done all the CE's, now let's put them together to form
        // a key
        if (m_utilCompare2_) {
            doSecondary(doFrench);
            if (m_utilCompare0_) {
                doCase();
            }
            if (m_utilCompare3_) {
                doTertiary();
                if (m_utilCompare4_) {
                    doQuaternary(commonBottom4, bottomCount4);
                    if (m_utilCompare5_) {
                        doIdentical(source);
                    }

                }
            }
        }
        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_, (byte)0);
        m_utilBytesCount1_ ++;

        key.set(m_utilBytes1_, 0, m_utilBytesCount1_);
    }

    /**
     * Packs the French bytes
     * @param count array of compression counts
     */
    private final void doFrench()
    {
        for (int i = 0; i < m_utilBytesCount2_; i ++) {
            byte s = m_utilBytes2_[m_utilBytesCount2_ - i - 1];
            // This is compression code.
            if (s == COMMON_2_) {
                ++ m_utilCount2_;
            }
            else {
                if (m_utilCount2_ > 0) {
                    // getting the unsigned value
                    if ((s & LAST_BYTE_MASK_) > COMMON_2_) {
                        // not necessary for 4th level.
                        while (m_utilCount2_ > TOP_COUNT_2_) {
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                   m_utilBytesCount1_,
                                        (byte)(COMMON_TOP_2_ - TOP_COUNT_2_));
                            m_utilBytesCount1_ ++;
                            m_utilCount2_ -= TOP_COUNT_2_;
                        }
                        m_utilBytes1_ = append(m_utilBytes1_,
                                               m_utilBytesCount1_,
                                               (byte)(COMMON_TOP_2_
                                                      - (m_utilCount2_ - 1)));
                        m_utilBytesCount1_ ++;
                    }
                    else {
                        while (m_utilCount2_ > BOTTOM_COUNT_2_) {
                            m_utilBytes1_ = append(m_utilBytes1_,
                                                   m_utilBytesCount1_,
                                (byte)(COMMON_BOTTOM_2_ + BOTTOM_COUNT_2_));
                            m_utilBytesCount1_ ++;
                            m_utilCount2_ -= BOTTOM_COUNT_2_;
                        }
                        m_utilBytes1_ = append(m_utilBytes1_,
                                               m_utilBytesCount1_,
                                               (byte)(COMMON_BOTTOM_2_
                                                      + (m_utilCount2_ - 1)));
                        m_utilBytesCount1_ ++;
                    }
                    m_utilCount2_ = 0;
                }
                m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_, s);
                m_utilBytesCount1_ ++;
            }
        }
        if (m_utilCount2_ > 0) {
            while (m_utilCount2_ > BOTTOM_COUNT_2_) {
                m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                                       (byte)(COMMON_BOTTOM_2_
                                                    + BOTTOM_COUNT_2_));
                m_utilBytesCount1_ ++;
                m_utilCount2_ -= BOTTOM_COUNT_2_;
            }
            m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                                   (byte)(COMMON_BOTTOM_2_
                                                    + (m_utilCount2_ - 1)));
            m_utilBytesCount1_ ++;
        }
    }

    /**
     * Compacts the secondary bytes and stores them into the primary array
     * @param doFrench flag indicator that French has to be handled specially
     */
    private final void doSecondary(boolean doFrench)
    {
        if (m_utilCount2_ > 0) {
            while (m_utilCount2_ > BOTTOM_COUNT_2_) {
                m_utilBytes2_ = append(m_utilBytes2_, m_utilBytesCount2_,
                                       (byte)(COMMON_BOTTOM_2_
                                                        + BOTTOM_COUNT_2_));
                m_utilBytesCount2_ ++;
                m_utilCount2_ -= BOTTOM_COUNT_2_;
            }
            m_utilBytes2_ = append(m_utilBytes2_, m_utilBytesCount2_,
                                   (byte)(COMMON_BOTTOM_2_ +
                                                    (m_utilCount2_ - 1)));
            m_utilBytesCount2_ ++;
        }

        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                               SORT_LEVEL_TERMINATOR_);
        m_utilBytesCount1_ ++;

        if (doFrench) { // do the reverse copy
            doFrench();
        }
        else {
            if (m_utilBytes1_.length <= m_utilBytesCount1_
                                        + m_utilBytesCount2_) {
                m_utilBytes1_ = increase(m_utilBytes1_, m_utilBytesCount1_,
                                         m_utilBytesCount2_);
            }
            System.arraycopy(m_utilBytes2_, 0, m_utilBytes1_,
                             m_utilBytesCount1_, m_utilBytesCount2_);
            m_utilBytesCount1_ += m_utilBytesCount2_;
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
     */
    private final void doCase()
    {
        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                               SORT_LEVEL_TERMINATOR_);
        m_utilBytesCount1_ ++;
        if (m_utilBytes1_.length <= m_utilBytesCount1_ + m_utilBytesCount0_) {
            m_utilBytes1_ = increase(m_utilBytes1_, m_utilBytesCount1_,
                                     m_utilBytesCount0_);
        }
        System.arraycopy(m_utilBytes0_, 0, m_utilBytes1_, m_utilBytesCount1_,
                         m_utilBytesCount0_);
        m_utilBytesCount1_ += m_utilBytesCount0_;
    }

    /**
     * Compacts the tertiary bytes and stores them into the primary array
     */
    private final void doTertiary()
    {
        if (m_utilCount3_ > 0) {
            if (m_common3_ != COMMON_BOTTOM_3_) {
                while (m_utilCount3_ >= m_topCount3_) {
                    m_utilBytes3_ = append(m_utilBytes3_, m_utilBytesCount3_,
                                           (byte)(m_top3_ - m_topCount3_));
                    m_utilBytesCount3_ ++;
                    m_utilCount3_ -= m_topCount3_;
                }
                m_utilBytes3_ = append(m_utilBytes3_, m_utilBytesCount3_,
                                       (byte)(m_top3_ - m_utilCount3_));
                m_utilBytesCount3_ ++;
            }
            else {
                while (m_utilCount3_ > m_bottomCount3_) {
                    m_utilBytes3_ = append(m_utilBytes3_, m_utilBytesCount3_,
                                           (byte)(m_bottom3_
                                                        + m_bottomCount3_));
                    m_utilBytesCount3_ ++;
                    m_utilCount3_ -= m_bottomCount3_;
                }
                m_utilBytes3_ = append(m_utilBytes3_, m_utilBytesCount3_,
                                       (byte)(m_bottom3_
                                              + (m_utilCount3_ - 1)));
                m_utilBytesCount3_ ++;
            }
        }
        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                               SORT_LEVEL_TERMINATOR_);
        m_utilBytesCount1_ ++;
        if (m_utilBytes1_.length <= m_utilBytesCount1_ + m_utilBytesCount3_) {
            m_utilBytes1_ = increase(m_utilBytes1_, m_utilBytesCount1_,
                                     m_utilBytesCount3_);
        }
        System.arraycopy(m_utilBytes3_, 0, m_utilBytes1_, m_utilBytesCount1_,
                         m_utilBytesCount3_);
        m_utilBytesCount1_ += m_utilBytesCount3_;
    }

    /**
     * Compacts the quaternary bytes and stores them into the primary array
     */
    private final void doQuaternary(int commonbottom4, int bottomcount4)
    {
        if (m_utilCount4_ > 0) {
            while (m_utilCount4_ > bottomcount4) {
                m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                       (byte)(commonbottom4 + bottomcount4));
                m_utilBytesCount4_ ++;
                m_utilCount4_ -= bottomcount4;
            }
            m_utilBytes4_ = append(m_utilBytes4_, m_utilBytesCount4_,
                                   (byte)(commonbottom4
                                                + (m_utilCount4_ - 1)));
            m_utilBytesCount4_ ++;
        }
        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                               SORT_LEVEL_TERMINATOR_);
        m_utilBytesCount1_ ++;
        if (m_utilBytes1_.length <= m_utilBytesCount1_ + m_utilBytesCount4_) {
            m_utilBytes1_ = increase(m_utilBytes1_, m_utilBytesCount1_,
                                     m_utilBytesCount4_);
        }
        System.arraycopy(m_utilBytes4_, 0, m_utilBytes1_, m_utilBytesCount1_,
                         m_utilBytesCount4_);
        m_utilBytesCount1_ += m_utilBytesCount4_;
    }

    /**
     * Deals with the identical sort.
     * Appends the BOCSU version of the source string to the ends of the
     * byte buffer.
     * @param source text string
     */
    private final void doIdentical(String source)
    {
        int isize = BOCU.getCompressionLength(source);
        m_utilBytes1_ = append(m_utilBytes1_, m_utilBytesCount1_,
                               SORT_LEVEL_TERMINATOR_);
        m_utilBytesCount1_ ++;
        if (m_utilBytes1_.length <= m_utilBytesCount1_ + isize) {
            m_utilBytes1_ = increase(m_utilBytes1_, m_utilBytesCount1_,
                                     1 + isize);
        }
        m_utilBytesCount1_ = BOCU.compress(source, m_utilBytes1_,
                                           m_utilBytesCount1_);
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
                && source.charAt(result) == target.charAt(result)
                && !CollationElementIterator.isThaiPreVowel(source.charAt(result))) {
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
                schar = source.charAt(minlength - 1);
                if (isUnsafe(schar)) {
                    tchar = schar;
                }
                else if (slength == tlength) {
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
     * @param appendindex index in the byte array to append
     * @param value to append
     * @return array if array size can accomodate the new value, otherwise
     *         a bigger array will be created and returned
     */
    private static final byte[] append(byte array[], int appendindex,
                                       byte value)
    {
        try {
            array[appendindex] = value;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            array = increase(array, appendindex, SORT_BUFFER_INIT_SIZE_);
            array[appendindex] = value;
        }
        return array;
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
        m_utilRawCollationKey_ = getRawCollationKey(source, 
                                                    m_utilRawCollationKey_);
        // this method is very seldom called
        RawCollationKey targetkey = getRawCollationKey(target, null);
        return m_utilRawCollationKey_.compareTo(targetkey);
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
     *                  observed
     * @param lowestpvalue the lowest primary value that will not be ignored if
     *                      alternate handling is shifted
     * @param source text string
     * @param target text string
     * @param textoffset offset in text to start the comparison
     * @return comparion result if a primary difference is found, otherwise
     *                      hiragana result
     */
    private final int doPrimaryCompare(boolean doHiragana4, int lowestpvalue,
                                        String source, String target,
                                        int textoffset)

    {
        // Preparing the context objects for iterating over strings
        m_srcUtilIter_.setText(source);
        m_srcUtilColEIter_.setText(m_srcUtilIter_, textoffset);
        m_tgtUtilIter_.setText(target);
        m_tgtUtilColEIter_.setText(m_tgtUtilIter_, textoffset);

        // Non shifted primary processing is quite simple
        if (!m_isAlternateHandlingShifted_) {
            int hiraganaresult = 0;
            while (true) {
                int sorder = 0;
                // We fetch CEs until we hit a non ignorable primary or end.
                do {
                    sorder = m_srcUtilColEIter_.next();
                    m_srcUtilCEBuffer_ = append(m_srcUtilCEBuffer_,
                                                m_srcUtilCEBufferSize_, sorder);
                    m_srcUtilCEBufferSize_ ++;
                    sorder &= CE_PRIMARY_MASK_;
                } while (sorder == CollationElementIterator.IGNORABLE);

                int torder = 0;
                do {
                    torder = m_tgtUtilColEIter_.next();
                    m_tgtUtilCEBuffer_ = append(m_tgtUtilCEBuffer_,
                                                m_tgtUtilCEBufferSize_, torder);
                    m_tgtUtilCEBufferSize_ ++;
                    torder &= CE_PRIMARY_MASK_;
                } while (torder == CollationElementIterator.IGNORABLE);

                // if both primaries are the same
                if (sorder == torder) {
                    // and there are no more CEs, we advance to the next level
                    // see if we are at the end of either string
                    if (m_srcUtilCEBuffer_[m_srcUtilCEBufferSize_ - 1]
                                        == CollationElementIterator.NULLORDER) {
                        if (m_tgtUtilCEBuffer_[m_tgtUtilCEBufferSize_ - 1] 
                            != CollationElementIterator.NULLORDER) {
                            return -1;
                        }
                        break;
                    }
                    else if (m_tgtUtilCEBuffer_[m_tgtUtilCEBufferSize_ - 1]
                             == CollationElementIterator.NULLORDER) {
                        return 1;
                    }
                    if (doHiragana4 && hiraganaresult == 0
                        && m_srcUtilColEIter_.m_isCodePointHiragana_ !=
                                        m_tgtUtilColEIter_.m_isCodePointHiragana_) {
                        if (m_srcUtilColEIter_.m_isCodePointHiragana_) {
                            hiraganaresult = -1;
                        }
                        else {
                            hiraganaresult = 1;
                        }
                    }
                }
                else {
                    // if two primaries are different, we are done
                    return endPrimaryCompare(sorder, torder);
                }
            }
            // no primary difference... do the rest from the buffers
            return hiraganaresult;
        }
        else { // shifted - do a slightly more complicated processing :)
            while (true) {
                int sorder = getPrimaryShiftedCompareCE(m_srcUtilColEIter_,
                                                        lowestpvalue, true);
                int torder = getPrimaryShiftedCompareCE(m_tgtUtilColEIter_,
                                                        lowestpvalue, false);
                if (sorder == torder) {
                    if (m_srcUtilCEBuffer_[m_srcUtilCEBufferSize_ - 1]
                            == CollationElementIterator.NULLORDER) {
                        break;
                    }
                    else {
                        continue;
                    }
                }
                else {
                    return endPrimaryCompare(sorder, torder);
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
     * @return the comparison result of sorder and torder
     */
    private final int endPrimaryCompare(int sorder, int torder)
    {
        // if we reach here, the ce offset accessed is the last ce
        // appended to the buffer
        boolean isSourceNullOrder = (m_srcUtilCEBuffer_[
                                                    m_srcUtilCEBufferSize_ - 1]
                                        == CollationElementIterator.NULLORDER);
        boolean isTargetNullOrder = (m_tgtUtilCEBuffer_[
                                                    m_tgtUtilCEBufferSize_ - 1]
                                        == CollationElementIterator.NULLORDER);
        m_srcUtilCEBufferSize_ = -1;
        m_tgtUtilCEBufferSize_ = -1;
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
     *                      handled
     * @param lowestpvalue lowest primary shifted value that will not be
     *                      ignored
     * @return result next modified ce
     */
    private final int getPrimaryShiftedCompareCE(
                                        CollationElementIterator coleiter,
                                        int lowestpvalue, boolean isSrc)

    {
        boolean shifted = false;
        int result = CollationElementIterator.IGNORABLE;
        int cebuffer[] = m_srcUtilCEBuffer_;
        int cebuffersize = m_srcUtilCEBufferSize_;
        if (!isSrc) {
            cebuffer = m_tgtUtilCEBuffer_;
            cebuffersize = m_tgtUtilCEBufferSize_;
        }
        while (true) {
            result = coleiter.next();
            if (result == CollationElementIterator.NULLORDER) {
                cebuffer = append(cebuffer, cebuffersize, result);
                cebuffersize ++;
                break;
            }
            else if (result == CollationElementIterator.IGNORABLE
                     || (shifted
                         && (result & CE_PRIMARY_MASK_)
                                      == CollationElementIterator.IGNORABLE)) {
                // UCA amendment - ignore ignorables that follow shifted code
                // points
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
                        cebuffer = append(cebuffer, cebuffersize, result);
                        cebuffersize ++;
                        continue;
                    }
                    else {
                        cebuffer = append(cebuffer, cebuffersize, result);
                        cebuffersize ++;
                        break;
                    }
                }
                else { // Just lower level values
                    if (!shifted) {
                        cebuffer = append(cebuffer, cebuffersize, result);
                        cebuffersize ++;
                    }
                }
            }
            else { // regular
                if (Utility.compareUnsigned(result & CE_PRIMARY_MASK_,
                                            lowestpvalue) > 0) {
                    cebuffer = append(cebuffer, cebuffersize, result);
                    cebuffersize ++;
                    break;
                }
                else {
                    if ((result & CE_PRIMARY_MASK_) != 0) {
                        shifted = true;
                        result &= CE_PRIMARY_MASK_;
                        cebuffer = append(cebuffer, cebuffersize, result);
                        cebuffersize ++;
                        continue;
                    }
                    else {
                        cebuffer = append(cebuffer, cebuffersize, result);
                        cebuffersize ++;
                        shifted = false;
                        continue;
                    }
                }
            }
        }
        if (isSrc) {
            m_srcUtilCEBuffer_ = cebuffer;
            m_srcUtilCEBufferSize_ = cebuffersize;
        }
        else {
            m_tgtUtilCEBuffer_ = cebuffer;
            m_tgtUtilCEBufferSize_ = cebuffersize;
        }
        result &= CE_PRIMARY_MASK_;
        return result;
    }

    /**
     * Appending an int to an array of ints and increases it if we run out of
     * space
     * @param array of int arrays
     * @param appendindex index at which value will be appended
     * @param value to append
     * @return array if size is not increased, otherwise a new array will be
     *         returned
     */
    private static final int[] append(int array[], int appendindex, int value)
    {
        if (appendindex + 1 >= array.length) {
            array = increase(array, appendindex, CE_BUFFER_SIZE_);
        }
        array[appendindex] = value;
        return array;
    }

    /**
     * Does secondary strength comparison based on the collected ces.
     * @param doFrench flag indicates if French ordering is to be done
     * @return the secondary strength comparison result
     */
    private final int doSecondaryCompare(boolean doFrench)
    {
        // now, we're gonna reexamine collected CEs
        if (!doFrench) { // normal
            int soffset = 0;
            int toffset = 0;
            while (true) {
                int sorder = CollationElementIterator.IGNORABLE;
                while (sorder == CollationElementIterator.IGNORABLE) {
                    sorder = m_srcUtilCEBuffer_[soffset ++]
                             & CE_SECONDARY_MASK_;
                }
                int torder = CollationElementIterator.IGNORABLE;
                while (torder == CollationElementIterator.IGNORABLE) {
                    torder = m_tgtUtilCEBuffer_[toffset ++]
                             & CE_SECONDARY_MASK_;
                }

                if (sorder == torder) {
                    if (m_srcUtilCEBuffer_[soffset - 1]
                                    == CollationElementIterator.NULLORDER) {
                        if (m_tgtUtilCEBuffer_[toffset - 1] 
                            != CollationElementIterator.NULLORDER) {
                            return -1;
                        }
                        break;
                    }
                    else if (m_tgtUtilCEBuffer_[toffset - 1]
                             == CollationElementIterator.NULLORDER) {
                        return 1;
                    }
                }
                else {
                    if (m_srcUtilCEBuffer_[soffset - 1] ==
                            CollationElementIterator.NULLORDER) {
                        return -1;
                    }
                    if (m_tgtUtilCEBuffer_[toffset - 1] ==
                            CollationElementIterator.NULLORDER) {
                        return 1;
                    }
                    return (sorder < torder) ? -1 : 1;
                }
            }
        }
        else { // do the French
            m_srcUtilContOffset_ = 0;
            m_tgtUtilContOffset_ = 0;
            m_srcUtilOffset_ = m_srcUtilCEBufferSize_ - 2;
            m_tgtUtilOffset_ = m_tgtUtilCEBufferSize_ - 2;
            while (true) {
                int sorder = getSecondaryFrenchCE(true);
                int torder = getSecondaryFrenchCE(false);
                if (sorder == torder) {
                    if ((m_srcUtilOffset_ < 0 && m_tgtUtilOffset_ < 0)
                        || (m_srcUtilOffset_ >= 0 
                            && m_srcUtilCEBuffer_[m_srcUtilOffset_]
                                    == CollationElementIterator.NULLORDER)) {
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
     * @param isSrc flag indicator if we are calculating the src ces
     * @return result next modified ce
     */
    private final int getSecondaryFrenchCE(boolean isSrc)
    {
        int result = CollationElementIterator.IGNORABLE;
        int offset = m_srcUtilOffset_;
        int continuationoffset = m_srcUtilContOffset_;
        int cebuffer[] = m_srcUtilCEBuffer_;
        if (!isSrc) {
            offset = m_tgtUtilOffset_;
            continuationoffset = m_tgtUtilContOffset_;
            cebuffer = m_tgtUtilCEBuffer_;
        }

        while (result == CollationElementIterator.IGNORABLE
                && offset >= 0) {
            if (continuationoffset == 0) {
                result = cebuffer[offset];
                while (isContinuation(cebuffer[offset --]));
                // after this, sorder is at the start of continuation,
                // and offset points before that
                if (isContinuation(cebuffer[offset + 1])) {
                    // save offset for later
                    continuationoffset = offset;
                    offset += 2;
                }
            }
            else {
                result = cebuffer[offset ++];
                if (!isContinuation(result)) {
                    // we have finished with this continuation
                    offset = continuationoffset;
                    // reset the pointer to before continuation
                    continuationoffset = 0;
                    continue;
                }
            }
            result &= CE_SECONDARY_MASK_; // remove continuation bit
        }
        if (isSrc) {
            m_srcUtilOffset_ = offset;
            m_srcUtilContOffset_ = continuationoffset;
        }
        else {
            m_tgtUtilOffset_ = offset;
            m_tgtUtilContOffset_ = continuationoffset;
        }
        return result;
    }

    /**
     * Does case strength comparison based on the collected ces.
     * @return the case strength comparison result
     */
    private final int doCaseCompare()
    {
        int soffset = 0;
        int toffset = 0;
        while (true) {
            int sorder = CollationElementIterator.IGNORABLE;
            int torder = CollationElementIterator.IGNORABLE;
            while ((sorder & CE_REMOVE_CASE_)
                                    == CollationElementIterator.IGNORABLE) {
                sorder = m_srcUtilCEBuffer_[soffset ++];
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
                torder = m_tgtUtilCEBuffer_[toffset ++];
                if (!isContinuation(torder)) {
                    torder &= CE_CASE_MASK_3_;
                    torder ^= m_caseSwitch_;
                }
                else {
                    torder = CollationElementIterator.IGNORABLE;
                }
            }

            sorder &= CE_CASE_BIT_MASK_;
            torder &= CE_CASE_BIT_MASK_;
            if (sorder == torder) {
                // checking end of strings
                if (m_srcUtilCEBuffer_[soffset - 1]
                                        == CollationElementIterator.NULLORDER) {
                    if (m_tgtUtilCEBuffer_[toffset - 1] 
                        != CollationElementIterator.NULLORDER) {
                        return -1;
                    }
                    break;
                }
                else if (m_tgtUtilCEBuffer_[toffset - 1]
                            == CollationElementIterator.NULLORDER) {
                    return 1;
                }
            }
            else {
                if (m_srcUtilCEBuffer_[soffset - 1]
                                    == CollationElementIterator.NULLORDER) {
                    return -1;
                }
                if (m_tgtUtilCEBuffer_[soffset - 1]
                                    == CollationElementIterator.NULLORDER) {
                    return 1;
                }
                return (sorder < torder) ? -1 : 1;
            }
        }
        return 0;
    }

    /**
     * Does tertiary strength comparison based on the collected ces.
     * @return the tertiary strength comparison result
     */
    private final int doTertiaryCompare()
    {
        int soffset = 0;
        int toffset = 0;
        while (true) {
            int sorder = CollationElementIterator.IGNORABLE;
            int torder = CollationElementIterator.IGNORABLE;
            while ((sorder & CE_REMOVE_CASE_)
                                == CollationElementIterator.IGNORABLE) {
                sorder = m_srcUtilCEBuffer_[soffset ++] & m_mask3_;
                if (!isContinuation(sorder)) {
                    sorder ^= m_caseSwitch_;
                }
                else {
                    sorder &= CE_REMOVE_CASE_;
                }
            }

            while ((torder & CE_REMOVE_CASE_)
                                == CollationElementIterator.IGNORABLE) {
                torder = m_tgtUtilCEBuffer_[toffset ++] & m_mask3_;
                if (!isContinuation(torder)) {
                    torder ^= m_caseSwitch_;
                }
                else {
                    torder &= CE_REMOVE_CASE_;
                }
            }

            if (sorder == torder) {
                if (m_srcUtilCEBuffer_[soffset - 1]
                                    == CollationElementIterator.NULLORDER) {
                    if (m_tgtUtilCEBuffer_[toffset - 1]
                        != CollationElementIterator.NULLORDER) {
                        return -1;
                    }
                    break;
                }
                else if (m_tgtUtilCEBuffer_[toffset - 1]
                            == CollationElementIterator.NULLORDER) {
                    return 1;
                }
            }
            else {
                if (m_srcUtilCEBuffer_[soffset - 1] ==
                                        CollationElementIterator.NULLORDER) {
                    return -1;
                }
                if (m_tgtUtilCEBuffer_[toffset - 1] ==
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
     * @param lowestpvalue the lowest primary value that will not be ignored if
     *                      alternate handling is shifted
     * @return the quaternary strength comparison result
     */
    private final int doQuaternaryCompare(int lowestpvalue)
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
                sorder = m_srcUtilCEBuffer_[soffset ++];
                if (isContinuation(sorder)) {
                    if (!sShifted) {
                        continue;
                    }
                }
                else if (Utility.compareUnsigned(sorder, lowestpvalue) > 0
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
            sorder >>>= CE_PRIMARY_SHIFT_;
            while (torder == CollationElementIterator.IGNORABLE
                    || (isContinuation(torder) && !tShifted)) {
                torder = m_tgtUtilCEBuffer_[toffset ++];
                if (isContinuation(torder)) {
                    if (!tShifted) {
                        continue;
                    }
                }
                else if (Utility.compareUnsigned(torder, lowestpvalue) > 0
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
            torder >>>= CE_PRIMARY_SHIFT_;

            if (sorder == torder) {
                if (m_srcUtilCEBuffer_[soffset - 1]
                    == CollationElementIterator.NULLORDER) {
                    if (m_tgtUtilCEBuffer_[toffset - 1]
                        != CollationElementIterator.NULLORDER) {
                        return -1;
                    }
                    break;
                }
                else if (m_tgtUtilCEBuffer_[toffset - 1]
                            == CollationElementIterator.NULLORDER) {
                    return 1;
                }
            }
            else {
                if (m_srcUtilCEBuffer_[soffset - 1] ==
                    CollationElementIterator.NULLORDER) {
                    return -1;
                }
                if (m_tgtUtilCEBuffer_[toffset - 1] ==
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
     *              comparison
     * @return 1 if source is greater than target, -1 less than and 0 if equals
     */
    private static final int doIdenticalCompare(String source, String target,
                                                int offset, boolean normalize)

    {
        if (normalize) {
            if (Normalizer.quickCheck(source, Normalizer.NFD,0)
                                                    != Normalizer.YES) {
                source = Normalizer.decompose(source, false);
            }

            if (Normalizer.quickCheck(target, Normalizer.NFD,0)
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
     * Resets the internal case data members and compression values.
     */
    private void updateInternalState()
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

        if (!m_isCaseLevel_ && getStrength() == AttributeValue.TERTIARY_
            && !m_isFrenchCollation_ && !m_isAlternateHandlingShifted_) {
            m_isSimple3_ = true;
        }
        else {
            m_isSimple3_ = false;
        }
        if(!m_isCaseLevel_ && getStrength() <= AttributeValue.TERTIARY_ && !m_isNumericCollation_
          && !m_isAlternateHandlingShifted_ && !latinOneFailed_) {
          if(latinOneCEs_ == null || latinOneRegenTable_) {
            if(setUpLatinOne()) { // if we succeed in building latin1 table, we'll use it
              latinOneUse_ = true;
            } else {
              latinOneUse_ = false;
              latinOneFailed_ = true;
            }
            latinOneRegenTable_ = false;
          } else { // latin1Table exists and it doesn't need to be regenerated, just use it
            latinOneUse_ = true;
          }
        } else {
          latinOneUse_ = false;
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
        latinOneFailed_ = true;
        setStrength(m_defaultStrength_);
        setDecomposition(m_defaultDecomposition_);
        m_variableTopValue_ = m_defaultVariableTopValue_;
        m_isFrenchCollation_ = m_defaultIsFrenchCollation_;
        m_isAlternateHandlingShifted_ = m_defaultIsAlternateHandlingShifted_;
        m_isCaseLevel_ = m_defaultIsCaseLevel_;
        m_caseFirst_ = m_defaultCaseFirst_;
        m_isHiragana4_ = m_defaultIsHiragana4_;
        m_isNumericCollation_ = m_defaultIsNumericCollation_;
        latinOneFailed_ = false;
        updateInternalState();
    }

    /**
     *  Initializes utility iterators and byte buffer used by compare
     */
    private final void initUtility() {
       m_srcUtilIter_ = new StringUCharacterIterator();
       m_srcUtilColEIter_ = new CollationElementIterator(m_srcUtilIter_, this);
       m_tgtUtilIter_ = new StringUCharacterIterator();
       m_tgtUtilColEIter_ = new CollationElementIterator(m_tgtUtilIter_, this);
       m_utilBytes0_ = new byte[SORT_BUFFER_INIT_SIZE_CASE_]; // case
       m_utilBytes1_ = new byte[SORT_BUFFER_INIT_SIZE_1_]; // primary
       m_utilBytes2_ = new byte[SORT_BUFFER_INIT_SIZE_2_]; // secondary
       m_utilBytes3_ = new byte[SORT_BUFFER_INIT_SIZE_3_]; // tertiary
       m_utilBytes4_ = new byte[SORT_BUFFER_INIT_SIZE_4_];  // Quaternary
       m_srcUtilCEBuffer_ = new int[CE_BUFFER_SIZE_];
       m_tgtUtilCEBuffer_ = new int[CE_BUFFER_SIZE_];
    }



    // Consts for Latin-1 special processing
    private static final int ENDOFLATINONERANGE_ = 0xFF;
    private static final int LATINONETABLELEN_   = (ENDOFLATINONERANGE_+50);
    private static final int BAIL_OUT_CE_        = 0xFF000000;

     /**
     * Generate latin-1 tables
     */

    private class shiftValues {
        int primShift = 24;
        int secShift = 24;
        int terShift = 24;
    };

    private final void
    addLatinOneEntry(char ch, int CE, shiftValues sh) {
      int primary1 = 0, primary2 = 0, secondary = 0, tertiary = 0;
      boolean reverseSecondary = false;
      if(!isContinuation(CE)) {
        tertiary = ((CE & m_mask3_));
        tertiary ^= m_caseSwitch_;
        reverseSecondary = true;
      } else {
        tertiary = (byte)((CE & CE_REMOVE_CONTINUATION_MASK_));
        tertiary &= CE_REMOVE_CASE_;
        reverseSecondary = false;
      }

      secondary = ((CE >>>= 8) & LAST_BYTE_MASK_);
      primary2 =  ((CE >>>= 8) & LAST_BYTE_MASK_);
      primary1 =  (CE >>> 8);

      if(primary1 != 0) {
        latinOneCEs_[ch] |= (primary1 << sh.primShift);
        sh.primShift -= 8;
      }
      if(primary2 != 0) {
        if(sh.primShift < 0) {
          latinOneCEs_[ch] = BAIL_OUT_CE_;
          latinOneCEs_[latinOneTableLen_+ch] = BAIL_OUT_CE_;
          latinOneCEs_[2*latinOneTableLen_+ch] = BAIL_OUT_CE_;
          return;
        }
        latinOneCEs_[ch] |= (primary2 << sh.primShift);
        sh.primShift -= 8;
      }
      if(secondary != 0) {
        if(reverseSecondary && m_isFrenchCollation_) { // reverse secondary
          latinOneCEs_[latinOneTableLen_+ch] >>>= 8; // make space for secondary
          latinOneCEs_[latinOneTableLen_+ch] |= (secondary << 24);
        } else { // normal case
          latinOneCEs_[latinOneTableLen_+ch] |= (secondary << sh.secShift);
        }
        sh.secShift -= 8;
      }
      if(tertiary != 0) {
        latinOneCEs_[2*latinOneTableLen_+ch] |= (tertiary << sh.terShift);
        sh.terShift -= 8;
      }
    }

    private final void
    resizeLatinOneTable(int newSize) {
        int newTable[] = new int[3*newSize];
        int sizeToCopy = ((newSize<latinOneTableLen_)?newSize:latinOneTableLen_);
        //uprv_memset(newTable, 0, newSize*sizeof(uint32_t)*3); // automatically cleared.
        System.arraycopy(latinOneCEs_, 0, newTable, 0, sizeToCopy);
        System.arraycopy(latinOneCEs_, latinOneTableLen_, newTable, newSize, sizeToCopy);
        System.arraycopy(latinOneCEs_, 2*latinOneTableLen_, newTable, 2*newSize, sizeToCopy);
        latinOneTableLen_ = newSize;
        latinOneCEs_ = newTable;
    }

    private final boolean setUpLatinOne() {
      if(latinOneCEs_ == null) {
        latinOneCEs_ = new int[3*LATINONETABLELEN_];
        latinOneTableLen_ = LATINONETABLELEN_;
      } else {
        Arrays.fill(latinOneCEs_, 0);
      }
      if(m_ContInfo_ == null) {
        m_ContInfo_ = new ContractionInfo();
      }
      char ch = 0;
      //StringBuffer sCh = new StringBuffer();
      //CollationElementIterator it = getCollationElementIterator(sCh.toString());
      CollationElementIterator it = getCollationElementIterator("");

      shiftValues s = new shiftValues();
      int CE = 0;
      char contractionOffset = ENDOFLATINONERANGE_+1;

      for(ch = 0; ch <= ENDOFLATINONERANGE_; ch++) {
        s.primShift = 24; s.secShift = 24; s.terShift = 24;
        if(ch < 0x100) {
          CE = m_trie_.getLatin1LinearValue(ch);
        } else {
          CE = m_trie_.getLeadValue(ch);
          if(CE == CollationElementIterator.CE_NOT_FOUND_) {
            CE = UCA_.m_trie_.getLeadValue(ch);
          }
        }
        if(!isSpecial(CE)) {
          addLatinOneEntry(ch, CE, s);
        } else {
          switch (RuleBasedCollator.getTag(CE)) {
          case CollationElementIterator.CE_EXPANSION_TAG_:
          case CollationElementIterator.CE_DIGIT_TAG_:
            //sCh.delete(0, sCh.length());
            //sCh.append(ch);
            //it.setText(sCh.toString());
            it.setText(UCharacter.toString(ch));
            while((CE = it.next()) != CollationElementIterator.NULLORDER) {
              if(s.primShift < 0 || s.secShift < 0 || s.terShift < 0) {
                latinOneCEs_[ch] = BAIL_OUT_CE_;
                latinOneCEs_[latinOneTableLen_+ch] = BAIL_OUT_CE_;
                latinOneCEs_[2*latinOneTableLen_+ch] = BAIL_OUT_CE_;
                break;
              }
              addLatinOneEntry(ch, CE, s);
            }
            break;
          case CollationElementIterator.CE_CONTRACTION_TAG_:
            // here is the trick
            // F2 is contraction. We do something very similar to contractions
            // but have two indices, one in the real contraction table and the
            // other to where we stuffed things. This hopes that we don't have
            // many contractions (this should work for latin-1 tables).
            {
              if((CE & 0x00FFF000) != 0) {
                latinOneFailed_ = true;
                return false;
              }

              int UCharOffset = (CE & 0xFFFFFF) - m_contractionOffset_; //getContractionOffset(CE)]

              CE |= (contractionOffset & 0xFFF) << 12; // insert the offset in latin-1 table

              latinOneCEs_[ch] = CE;
              latinOneCEs_[latinOneTableLen_+ch] = CE;
              latinOneCEs_[2*latinOneTableLen_+ch] = CE;

              // We're going to jump into contraction table, pick the elements
              // and use them
              do {
                  //CE = *(contractionCEs + (UCharOffset - contractionIndex));
                  CE = m_contractionCE_[UCharOffset];
                  if(isSpecial(CE) 
                     && getTag(CE) 
                               == CollationElementIterator.CE_EXPANSION_TAG_) {
                    int i;    /* general counter */
                    //uint32_t *CEOffset = (uint32_t *)image+getExpansionOffset(CE); /* find the offset to expansion table */
                    int offset = ((CE & 0xFFFFF0) >> 4) - m_expansionOffset_; //it.getExpansionOffset(this, CE);
                    int size = CE & 0xF; // getExpansionCount(CE);
                    //CE = *CEOffset++;
                    if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
                      for(i = 0; i<size; i++) {
                        if(s.primShift < 0 || s.secShift < 0 || s.terShift < 0) {
                          latinOneCEs_[contractionOffset] = BAIL_OUT_CE_;
                          latinOneCEs_[latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                          latinOneCEs_[2*latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                          break;
                        }
                        addLatinOneEntry(contractionOffset, m_expansion_[offset+i], s);
                      }
                    } else { /* else, we do */
                      while(m_expansion_[offset] != 0) {
                        if(s.primShift < 0 || s.secShift < 0 || s.terShift < 0) {
                          latinOneCEs_[contractionOffset] = BAIL_OUT_CE_;
                          latinOneCEs_[latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                          latinOneCEs_[2*latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                          break;
                        }
                        addLatinOneEntry(contractionOffset, m_expansion_[offset++], s);
                      }
                    }
                    contractionOffset++;
                  } else if(!isSpecial(CE)) {
                    addLatinOneEntry(contractionOffset++, CE, s);
                  } else {
                      latinOneCEs_[contractionOffset] = BAIL_OUT_CE_;
                      latinOneCEs_[latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                      latinOneCEs_[2*latinOneTableLen_+contractionOffset] = BAIL_OUT_CE_;
                      contractionOffset++;
                  }
                  UCharOffset++;
                  s.primShift = 24; s.secShift = 24; s.terShift = 24;
                  if(contractionOffset == latinOneTableLen_) { // we need to reallocate
                   resizeLatinOneTable(2*latinOneTableLen_);
                  }
              } while(m_contractionIndex_[UCharOffset] != 0xFFFF);
            }
            break;
          default:
            latinOneFailed_ = true;
            return false;
          }
        }
      }
      // compact table
      if(contractionOffset < latinOneTableLen_) {
        resizeLatinOneTable(contractionOffset);
      }
      return true;
    }

    private class
    ContractionInfo {
        int index;
    };

    ContractionInfo m_ContInfo_;

    private int
    getLatinOneContraction(int strength, int CE, String s) {
    //int strength, int CE, String s, Integer ind) {
      int len = s.length();
      //const UChar *UCharOffset = (UChar *)coll->image+getContractOffset(CE&0xFFF);
      int UCharOffset = (CE & 0xFFF) - m_contractionOffset_;
      int offset = 1;
      int latinOneOffset = (CE & 0x00FFF000) >>> 12;
      char schar = 0, tchar = 0;

      for(;;) {
        /*
        if(len == -1) {
          if(s[*index] == 0) { // end of string
            return(coll->latinOneCEs[strength*coll->latinOneTableLen+latinOneOffset]);
          } else {
            schar = s[*index];
          }
        } else {
        */
          if(m_ContInfo_.index == len) {
            return(latinOneCEs_[strength*latinOneTableLen_+latinOneOffset]);
          } else {
            schar = s.charAt(m_ContInfo_.index);
          }
        //}

        while(schar > (tchar = m_contractionIndex_[UCharOffset+offset]/**(UCharOffset+offset)*/)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          offset++;
        }

        if (schar == tchar) {
          m_ContInfo_.index++;
          return(latinOneCEs_[strength*latinOneTableLen_+latinOneOffset+offset]);
        }
        else
        {
          if(schar  > ENDOFLATINONERANGE_ /*& 0xFF00*/) {
            return BAIL_OUT_CE_;
          }
          // skip completely ignorables
          int isZeroCE = m_trie_.getLeadValue(schar); //UTRIE_GET32_FROM_LEAD(coll->mapping, schar);
          if(isZeroCE == 0) { // we have to ignore completely ignorables
            m_ContInfo_.index++;
            continue;
          }

          return(latinOneCEs_[strength*latinOneTableLen_+latinOneOffset]);
        }
      }
    }


    /**
     * This is a fast strcoll, geared towards text in Latin-1.
     * It supports contractions of size two, French secondaries
     * and case switching. You can use it with strengths primary
     * to tertiary. It does not support shifted and case level.
     * It relies on the table build by setupLatin1Table. If it
     * doesn't understand something, it will go to the regular
     * strcoll.
     */
    private final int
    compareUseLatin1(String source, String target, int startOffset)
    {
        int sLen = source.length();
        int tLen = target.length();

        int strength = getStrength();

        int sIndex = startOffset, tIndex = startOffset;
        char sChar = 0, tChar = 0;
        int sOrder=0, tOrder=0;

        boolean endOfSource = false;

        //uint32_t *elements = coll->latinOneCEs;

        boolean haveContractions = false; // if we have contractions in our string
                                        // we cannot do French secondary

        int offset = latinOneTableLen_;

        // Do the primary level
    primLoop:
        for(;;) {
          while(sOrder==0) { // this loop skips primary ignorables
            // sOrder=getNextlatinOneCE(source);
              if(sIndex==sLen) {
                endOfSource = true;
                break;
              }
              sChar=source.charAt(sIndex++); //[sIndex++];
            //}
            if(sChar > ENDOFLATINONERANGE_) { // if we encounter non-latin-1, we bail out
              //fprintf(stderr, "R");
              return compareRegular(source, target, startOffset);
            }
            sOrder = latinOneCEs_[sChar];
            if(isSpecial(sOrder)) { // if we got a special
              // specials can basically be either contractions or bail-out signs. If we get anything
              // else, we'll bail out anywasy
              if(getTag(sOrder) == CollationElementIterator.CE_CONTRACTION_TAG_) {
                m_ContInfo_.index = sIndex;
                sOrder = getLatinOneContraction(0, sOrder, source);
                sIndex = m_ContInfo_.index;
                haveContractions = true; // if there are contractions, we cannot do French secondary
                // However, if there are contractions in the table, but we always use just one char,
                // we might be able to do French. This should be checked out.
              }
              if(isSpecial(sOrder) /*== UCOL_BAIL_OUT_CE*/) {
                //fprintf(stderr, "S");
                return compareRegular(source, target, startOffset);
              }
            }
          }

          while(tOrder==0) {  // this loop skips primary ignorables
            // tOrder=getNextlatinOneCE(target);
            if(tIndex==tLen) {
              if(endOfSource) {
                break primLoop;
              } else {
                return 1;
              }
            }
            tChar=target.charAt(tIndex++); //[tIndex++];
            if(tChar > ENDOFLATINONERANGE_) { // if we encounter non-latin-1, we bail out
              //fprintf(stderr, "R");
              return compareRegular(source, target, startOffset);
            }
            tOrder = latinOneCEs_[tChar];
            if(isSpecial(tOrder)) {
              // Handling specials, see the comments for source
              if(getTag(tOrder) == CollationElementIterator.CE_CONTRACTION_TAG_) {
                m_ContInfo_.index = tIndex;
                tOrder = getLatinOneContraction(0, tOrder, target);
                tIndex = m_ContInfo_.index;
                haveContractions = true;
              }
              if(isSpecial(tOrder)/*== UCOL_BAIL_OUT_CE*/) {
                //fprintf(stderr, "S");
                return compareRegular(source, target, startOffset);
              }
            }
          }
          if(endOfSource) { // source is finished, but target is not, say the result.
              return -1;
          }

          if(sOrder == tOrder) { // if we have same CEs, we continue the loop
            sOrder = 0; tOrder = 0;
            continue;
          } else {
            // compare current top bytes
            if(((sOrder^tOrder)&0xFF000000)!=0) {
              // top bytes differ, return difference
              if(sOrder >>> 8 < tOrder >>> 8) {
                return -1;
              } else {
                return 1;
              }
              // instead of return (int32_t)(sOrder>>24)-(int32_t)(tOrder>>24);
              // since we must return enum value
            }

            // top bytes match, continue with following bytes
            sOrder<<=8;
            tOrder<<=8;
          }
        }

        // after primary loop, we definitely know the sizes of strings,
        // so we set it and use simpler loop for secondaries and tertiaries
        //sLen = sIndex; tLen = tIndex;
        if(strength >= SECONDARY) {
          // adjust the table beggining
          //latinOneCEs_ += coll->latinOneTableLen;
          endOfSource = false;

          if(!m_isFrenchCollation_) { // non French
            // This loop is a simplified copy of primary loop
            // at this point we know that whole strings are latin-1, so we don't
            // check for that. We also know that we only have contractions as
            // specials.
            //sIndex = 0; tIndex = 0;
            sIndex = startOffset; tIndex = startOffset;
    secLoop:
            for(;;) {
              while(sOrder==0) {
                if(sIndex==sLen) {
                  endOfSource = true;
                  break;
                }
                sChar=source.charAt(sIndex++); //[sIndex++];
                sOrder = latinOneCEs_[offset+sChar];
                if(isSpecial(sOrder)) {
                    m_ContInfo_.index = sIndex;
                    sOrder = getLatinOneContraction(1, sOrder, source);
                    sIndex = m_ContInfo_.index;
                }
              }

              while(tOrder==0) {
                if(tIndex==tLen) {
                  if(endOfSource) {
                    break secLoop;
                  } else {
                    return 1;
                  }
                }
                tChar=target.charAt(tIndex++); //[tIndex++];
                tOrder = latinOneCEs_[offset+tChar];
                if(isSpecial(tOrder)) {
                    m_ContInfo_.index = tIndex;
                    tOrder = getLatinOneContraction(1, tOrder, target);
                    tIndex = m_ContInfo_.index;
                }
              }
              if(endOfSource) {
                  return -1;
              }

              if(sOrder == tOrder) {
                sOrder = 0; tOrder = 0;
                continue;
              } else {
                // see primary loop for comments on this
                if(((sOrder^tOrder)&0xFF000000)!=0) {
                  if(sOrder >>> 8 < tOrder >>> 8) {
                    return -1;
                  } else {
                    return 1;
                  }
                }
                sOrder<<=8;
                tOrder<<=8;
              }
            }
          } else { // French
            if(haveContractions) { // if we have contractions, we have to bail out
              // since we don't really know how to handle them here
              return compareRegular(source, target, startOffset);
            }
            // For French, we go backwards
            sIndex = sLen; tIndex = tLen;
    secFLoop:
            for(;;) {
              while(sOrder==0) {
                if(sIndex==startOffset) {
                  endOfSource = true;
                  break;
                }
                sChar=source.charAt(--sIndex); //[--sIndex];
                sOrder = latinOneCEs_[offset+sChar];
                // don't even look for contractions
              }

              while(tOrder==0) {
                if(tIndex==startOffset) {
                  if(endOfSource) {
                    break secFLoop;
                  } else {
                    return 1;
                  }
                }
                tChar=target.charAt(--tIndex); //[--tIndex];
                tOrder = latinOneCEs_[offset+tChar];
                // don't even look for contractions
              }
              if(endOfSource) {
                  return -1;
              }

              if(sOrder == tOrder) {
                sOrder = 0; tOrder = 0;
                continue;
              } else {
                // see the primary loop for comments
                if(((sOrder^tOrder)&0xFF000000)!=0) {
                  if(sOrder >>> 8 < tOrder >>> 8) {
                    return -1;
                  } else {
                    return 1;
                  }
                }
                sOrder<<=8;
                tOrder<<=8;
              }
            }
          }
        }

        if(strength >= TERTIARY) {
          // tertiary loop is the same as secondary (except no French)
          offset += latinOneTableLen_;
          //sIndex = 0; tIndex = 0;
          sIndex = startOffset; tIndex = startOffset;
          endOfSource = false;
          for(;;) {
            while(sOrder==0) {
              if(sIndex==sLen) {
                endOfSource = true;
                break;
              }
              sChar=source.charAt(sIndex++); //[sIndex++];
              sOrder = latinOneCEs_[offset+sChar];
              if(isSpecial(sOrder)) {
                m_ContInfo_.index = sIndex;
                sOrder = getLatinOneContraction(2, sOrder, source);
                sIndex = m_ContInfo_.index;
              }
            }
            while(tOrder==0) {
              if(tIndex==tLen) {
                if(endOfSource) {
                  return 0; // if both strings are at the end, they are equal
                } else {
                  return 1;
                }
              }
              tChar=target.charAt(tIndex++); //[tIndex++];
              tOrder = latinOneCEs_[offset+tChar];
              if(isSpecial(tOrder)) {
                m_ContInfo_.index = tIndex;
                tOrder = getLatinOneContraction(2, tOrder, target);
                tIndex = m_ContInfo_.index;
              }
            }
            if(endOfSource) {
                return -1;
            }
            if(sOrder == tOrder) {
              sOrder = 0; tOrder = 0;
              continue;
            } else {
              if(((sOrder^tOrder)&0xff000000)!=0) {
                if(sOrder >>> 8 < tOrder >>> 8) {
                  return -1;
                } else {
                  return 1;
                }
              }
              sOrder<<=8;
              tOrder<<=8;
            }
          }
        }
        return 0;
    }
    /** Get the version of this collator object.
     *  @return the version object associated with this collator
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public VersionInfo getVersion() {
        /* RunTime version  */
        int rtVersion = VersionInfo.UCOL_RUNTIME_VERSION.getMajor();
        /* Builder version*/
        int bdVersion = m_version_.getMajor();

        /* Charset Version. Need to get the version from cnv files
         * makeconv should populate cnv files with version and
         * an api has to be provided in ucnv.h to obtain this version
         */
        int csVersion = 0;

        /* combine the version info */
        int cmbVersion = ((rtVersion<<11) | (bdVersion<<6) | (csVersion)) & 0xFFFF;
        
        /* Tailoring rules */
        return VersionInfo.getInstance(cmbVersion>>8, 
                cmbVersion & 0xFF, 
                m_version_.getMinor(), 
                UCA_.m_UCA_version_.getMajor());

//        versionInfo[0] = (uint8_t)(cmbVersion>>8);
//        versionInfo[1] = (uint8_t)cmbVersion;
//        versionInfo[2] = coll->image->version[1];
//        versionInfo[3] = coll->UCA->image->UCAVersion[0];
    }
    
    /** 
     * Get the UCA version of this collator object.
     * @return the version object associated with this collator
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public VersionInfo getUCAVersion() {
        return UCA_.m_UCA_version_;
    }
}
