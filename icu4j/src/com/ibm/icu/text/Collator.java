/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Collator.java,v $ 
* $Date: 2002/05/20 23:43:01 $ 
* $Revision: 1.6 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.util.Locale;

/**
* <p>The Collator class performs locale-sensitive String comparison. 
* You use this class to build searching and sorting routines for natural 
* language text.</p> 
* <p>Collator is an abstract base class. Subclasses implement specific 
* collation strategies. One subclass, RuleBasedCollator, is currently 
* provided and is applicable to a wide set of languages. Other subclasses 
* may be created to handle more specialized needs.</p>
* <p>Like other locale-sensitive classes, you can use the static factory 
* method, getInstance, to obtain the appropriate Collator object for a given 
* locale. You will only need to look at the subclasses of Collator if you need 
* to understand the details of a particular collation strategy or if you need 
* to modify that strategy. </p>
* <p>The following example shows how to compare two strings using the Collator 
* for the default locale. 
* <pre>
* // Compare two strings in the default locale
* Collator myCollator = Collator.getInstance();
* if (myCollator.compare("abc", "ABC") < 0) {
*     System.out.println("abc is less than ABC");
* }
* else {
*     System.out.println("abc is greater than or equal to ABC");
* }
* </pre>
* <p>You can set a <code>Collator</code>'s <em>strength</em> property to 
* determine the level of difference considered significant in comparisons. 
* Four strengths are provided: <code>PRIMARY</code>, <code>SECONDARY</code>, 
* <code>TERTIARY</code>, and <code>IDENTICAL</code>. The exact assignment of 
* strengths to language features is locale dependant. For example, in Czech, 
* "e" and "f" are considered primary differences, while "e" and "\u00EA" are 
* secondary differences, "e" and "E" are tertiary differences and "e" and "e" 
* are identical. The following shows how both case and accents could be 
* ignored for US English.</p>
* <pre>
* //Get the Collator for US English and set its strength to PRIMARY
* Collator usCollator = Collator.getInstance(Locale.US);
* usCollator.setStrength(Collator.PRIMARY);
* if (usCollator.compare("abc", "ABC") == 0) {
*     System.out.println("Strings are equivalent");
* }
* </pre>
* <p>For comparing Strings exactly once, the compare method provides the best 
* performance. When sorting a list of Strings however, it is generally 
* necessary to compare each String multiple times. In this case, 
* CollationKeys provide better performance. The CollationKey class converts a 
* String to a series of bits that can be compared bitwise against other 
* CollationKeys. A CollationKey is created by a Collator object for a given 
* String.</p> 
* <p>Note: CollationKeys from different Collators can not be compared. See the 
* class description for CollationKey for an example using CollationKeys. 
* </p>
* @author Syn Wee Quek
* @since release 2.2, April 18 2002
* @draft 2.2
*/

public abstract class Collator
{     
	// public data members ---------------------------------------------------
	
	/**
     * Collator strength value. When set, only PRIMARY differences are
     * considered significant during comparison. The assignment of strengths
     * to language features is locale dependant. A common example is for
     * different base letters ("a" vs "b") to be considered a PRIMARY 
     * difference.
     * @see #setStrength
     * @see #getStrength
     * @draft 2.2
     */
    public final static int PRIMARY 
    							= RuleBasedCollator.AttributeValue.PRIMARY_;
    /**
     * Collator strength value. When set, only SECONDARY and above 
     * differences are considered significant during comparison. The 
     * assignment of strengths to language features is locale dependant. A 
     * common example is for different accented forms of the same base letter 
     * ("a" vs "\u00E4") to be considered a SECONDARY difference.
     * @see #setStrength
     * @see #getStrength
     * @draft 2.2
     */
    public final static int SECONDARY 
    							= RuleBasedCollator.AttributeValue.SECONDARY_;
    /**
     * Collator strength value. When set, only TERTIARY and above differences 
     * are considered significant during comparison. The assignment of 
     * strengths to language features is locale dependant. A common example is 
     * for case differences ("a" vs "A") to be considered a TERTIARY 
     * difference.
     * @see #setStrength
     * @see #getStrength
     * @draft 2.2
     */
    public final static int TERTIARY 
    							= RuleBasedCollator.AttributeValue.TERTIARY_;
                                   
    /**
     * Collator strength value. When set, only QUARTENARY and above differences 
     * are considered significant during comparison. The assignment of 
     * strengths to language features is locale dependant.
     * difference.
     * @see #setStrength
     * @see #getStrength
     * @draft 2.2
     */
    public final static int QUATERNARY 
    							= RuleBasedCollator.AttributeValue.QUATERNARY_;

    /**
     * <p>Collator strength value. When set, all differences are considered 
     * significant during comparison. The assignment of strengths to language 
     * features is locale dependant. A common example is for control 
     * characters ("&#092;u0001" vs "&#092;u0002") to be considered equal at 
     * the PRIMARY, SECONDARY, and TERTIARY levels but different at the 
     * IDENTICAL level.  Additionally, differences between pre-composed 
     * accents such as "&#092;u00C0" (A-grave) and combining accents such as 
     * "A&#092;u0300" (A, combining-grave) will be considered significant at 
     * the tertiary level if decomposition is set to NO_DECOMPOSITION.
     * </p>
     * <p>Note this value is different from JDK's</p>
     * @draft 2.2
     */
    public final static int IDENTICAL 
    							= RuleBasedCollator.AttributeValue.IDENTICAL_;

    /**
     * <p>Decomposition mode value. With NO_DECOMPOSITION set, accented 
     * characters will not be decomposed for collation. This is the default 
     * setting and provides the fastest collation but will only produce 
     * correct results for languages that do not use accents.</p>
     * <p>Note this value is different from JDK's</p>
     * @see #getDecomposition
     * @see #setDecomposition
     * @draft 2.2
     */
    public final static int NO_DECOMPOSITION 
    							= RuleBasedCollator.AttributeValue.OFF_;

    /**
     * <p>Decomposition mode value. With CANONICAL_DECOMPOSITION set, 
     * characters that are canonical variants according to Unicode 2.0 will be 
     * decomposed for collation. This should be used to get correct collation 
     * of accented characters.</p>
     * <p>CANONICAL_DECOMPOSITION corresponds to Normalization Form D as
     * described in <a href="http://www.unicode.org/unicode/reports/tr15/">
     * Unicode Technical Report #15</a>.</p>
     * @see #getDecomposition
     * @see #setDecomposition
     * @draft 2.2
     */
    public final static int CANONICAL_DECOMPOSITION = 1;
    
    // public methods --------------------------------------------------------
    
    // public setters --------------------------------------------------------
    
    /**
     * <p>Sets this Collator's strength property. The strength property 
     * determines the minimum level of difference considered significant 
     * during comparison.</p>
     * <p>See the Collator class description for an example of use.</p>
     * @param the new strength value.
     * @see #getStrength
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
        if ((newStrength != PRIMARY) &&
            (newStrength != SECONDARY) &&
            (newStrength != TERTIARY) &&
            (newStrength != QUATERNARY) &&
            (newStrength != IDENTICAL)) {
            throw new IllegalArgumentException("Incorrect comparison level.");
        }
        m_strength_ = newStrength;
    }
    
    /**
     * Set the decomposition mode of this Collator. See getDecomposition
     * for a description of decomposition mode.
     * @param decomposition the new decomposition mode
     * @see #getDecomposition
     * @see #NO_DECOMPOSITION
     * @see #CANONICAL_DECOMPOSITION
     * @see #FULL_DECOMPOSITION
     * @exception IllegalArgumentException If the given value is not a valid decomposition
     * mode.
     * @draft 2.2
     */
    public void setDecomposition(int decomposition) {
        if ((decomposition != NO_DECOMPOSITION) &&
            (decomposition != CANONICAL_DECOMPOSITION)) {
            throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        m_decomposition_ = decomposition;
    }
    
    // public getters --------------------------------------------------------
    
    /**
     * Gets the Collator for the current default locale.
     * The default locale is determined by java.util.Locale.getDefault().
     * @return the Collator for the default locale (for example, en_US) if it
     *         is created successfully, otherwise if there is a failure,
     *         null will be returned.
     * @see java.util.Locale#getDefault
     * @draft 2.2
     */
    public static final Collator getInstance() 
    {
        return getInstance(Locale.getDefault());
    }
    
    /**
     * Gets the Collator for the desired locale.
     * @param locale the desired locale.
     * @return Collator for the desired locale if it is created successfully,
     *         otherwise if there is a failure, the default UCA collator will 
     * 		   be returned.
     * @see java.util.Locale
     * @see java.util.ResourceBundle
     * @draft 2.2
     */
    public static final Collator getInstance(Locale locale)
    {
    	try {
    		return new RuleBasedCollator(locale);
    	} 
    	catch(Exception e) {
    		return RuleBasedCollator.UCA_;
    	}
    }
    
    /**
     * <p>Returns this Collator's strength property. The strength property 
     * determines the minimum level of difference considered significant 
     * during comparison.</p>
     * <p>See the Collator class description for an example of use.</p>
     * @return this Collator's current strength property.
     * @see #setStrength
     * @see #PRIMARY
     * @see #SECONDARY
     * @see #TERTIARY
     * @see #IDENTICAL
     * @draft 2.2
     */
    public int getStrength()
    {
        return m_strength_;
    }
    
    /**
     * <p>Get the decomposition mode of this Collator. Decomposition mode
     * determines how Unicode composed characters are handled. Adjusting
     * decomposition mode allows the user to select between faster and more
     * complete collation behavior.
     * <p>The three values for decomposition mode are:
     * <UL>
     * <LI>NO_DECOMPOSITION,
     * <LI>CANONICAL_DECOMPOSITION
     * <LI>FULL_DECOMPOSITION.
     * </UL>
     * See the documentation for these three constants for a description
     * of their meaning.
     * </p>
     * @return the decomposition mode
     * @see #setDecomposition
     * @see #NO_DECOMPOSITION
     * @see #CANONICAL_DECOMPOSITION
     * @see #FULL_DECOMPOSITION
     * @draft 2.2
     */
    public int getDecomposition()
    {
        return m_decomposition_;
    }
    
    // public other methods -------------------------------------------------

    /**
     * Convenience method for comparing the equality of two strings based on
     * this Collator's collation rules.
     * @param source the source string to be compared with.
     * @param target the target string to be compared with.
     * @return true if the strings are equal according to the collation
     *         rules. false, otherwise.
     * @see #compare
     * @draft 2.2
     */
    public boolean equals(String source, String target)
    {
        return (compare(source, target) == 0);
    }
	    
    /**
     * Cloning this Collator.
     * @return a cloned Collator of this object
     * @draft 2.2
     */
    public Object clone()
    {
        try {
            return (Collator)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Compares the equality of two Collators.
     * @param that the Collator to be compared with this.
     * @return true if this Collator is the same as that Collator;
     * false otherwise.
     * @draft 2.2
     */
    public boolean equals(Object that)
    {
        if (this == that) {
        	return true;
        }
        if (that == null || getClass() != that.getClass()) {
        	return false;
        }
        Collator other = (Collator) that;
        return ((m_strength_ == other.m_strength_) &&
                (m_decomposition_ == other.m_decomposition_));
    }
    
    // public abstract methods -----------------------------------------------

    /**
     * Generates the hash code for this Collator.
     * @draft 2.2
     */
    public abstract int hashCode();
    
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
     * @see #getCollationKey
     * @draft 2.2
     */
    public abstract int compare(String source, String target);

    /**
     * <p>Transforms the String into a series of bits that can be compared 
     * bitwise to other CollationKeys. CollationKeys provide better 
     * performance than Collator.compare() when Strings are involved in 
     * multiple comparisons.</p> 
     * <p>See the Collator class description for an example using 
     * CollationKeys.</p>
     * @param source the string to be transformed into a collation key.
     * @return the CollationKey for the given String based on this Collator's 
     *         collation rules. If the source String is null, a null 
     *         CollationKey is returned.
     * @see CollationKey
     * @see #compare(String, String)
     * @draft 2.2
     */
    public abstract CollationKey getCollationKey(String source);
    
    // protected data members ------------------------------------------------
    
    /**
     * Collation strength
     */
    protected int m_strength_;
    /**
     * Decomposition mode
     */ 
    protected int m_decomposition_;
    
    // protected constructor -------------------------------------------------
    
    /**
    * <p>Protected constructor for use by subclasses. 
    * Public access to creating Collators is handled by the API getInstance().
    * </p>
    * @draft 2.2
    */
    protected Collator() throws Exception
    {
    	m_strength_ = TERTIARY;
    	m_decomposition_ = CANONICAL_DECOMPOSITION;
    }
  
    // protected methods -----------------------------------------------------
    
    // private variables -----------------------------------------------------

    // private methods -------------------------------------------------------
}

