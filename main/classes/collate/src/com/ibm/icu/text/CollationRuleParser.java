/**
*******************************************************************************
* Copyright (C) 1996-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Hashtable;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;

/**
* Class for parsing collation rules, produces a list of tokens that will be
* turned into collation elements
* @author Syn Wee Quek
* @since release 2.2, June 7 2002
*/
final class CollationRuleParser
{
    // public data members ---------------------------------------------------

    // package private constructors ------------------------------------------

    /**
     * <p>RuleBasedCollator constructor that takes the rules.
     * Please see RuleBasedCollator class description for more details on the
     * collation rule syntax.</p>
     * @see java.util.Locale
     * @param rules the collation rules to build the collation table from.
     * @exception ParseException thrown when argument rules have an invalid
     *            syntax.
     */
    CollationRuleParser(String rules) throws ParseException
    {
        extractSetsFromRules(rules);
        m_source_ = new StringBuffer(Normalizer.decompose(rules, false).trim());
        m_rules_ = m_source_.toString();
        m_current_ = 0;
        m_extraCurrent_ = m_source_.length();
        m_variableTop_ = null;
        m_parsedToken_ = new ParsedToken();
        m_hashTable_ = new Hashtable<Token, Token>();
        m_options_ = new OptionSet(RuleBasedCollator.UCA_);
        m_listHeader_ = new TokenListHeader[512];
        m_resultLength_ = 0;
        // call assembleTokenList() manually, so that we can
        // init a parser and manually parse tokens
        //assembleTokenList();
    }

    // package private inner classes -----------------------------------------

    /**
     * Collation options set
     */
    static class OptionSet
    {
        // package private constructor ---------------------------------------

        /**
         * Initializes the option set with the argument collators
         * @param collator option to use
         */
        OptionSet(RuleBasedCollator collator)
        {
            m_variableTopValue_ = collator.m_variableTopValue_;
            m_isFrenchCollation_ = collator.isFrenchCollation();
            m_isAlternateHandlingShifted_
                                   = collator.isAlternateHandlingShifted();
            m_caseFirst_ = collator.m_caseFirst_;
            m_isCaseLevel_ = collator.isCaseLevel();
            m_decomposition_ = collator.getDecomposition();
            m_strength_ = collator.getStrength();
            m_isHiragana4_ = collator.m_isHiragana4_;
        }

        // package private data members --------------------------------------

        int m_variableTopValue_;
        boolean m_isFrenchCollation_;
        /**
         * Attribute for handling variable elements
         */
        boolean m_isAlternateHandlingShifted_;
        /**
         * who goes first, lower case or uppercase
         */
        int m_caseFirst_;
        /**
         * do we have an extra case level
         */
        boolean m_isCaseLevel_;
        /**
         * attribute for normalization
         */
        int m_decomposition_;
        /**
         * attribute for strength
         */
        int m_strength_;
        /**
         * attribute for special Hiragana
         */
        boolean m_isHiragana4_;
    }

    /**
     * List of tokens used by the collation rules
     */
    static class TokenListHeader
    {
        Token m_first_;
        Token m_last_;
        Token m_reset_;
        boolean m_indirect_;
        int m_baseCE_;
        int m_baseContCE_;
        int m_nextCE_;
        int m_nextContCE_;
        int m_previousCE_;
        int m_previousContCE_;
        int m_pos_[] = new int[Collator.IDENTICAL + 1];
        int m_gapsLo_[] = new int[3 * (Collator.TERTIARY + 1)];
        int m_gapsHi_[] = new int[3 * (Collator.TERTIARY + 1)];
        int m_numStr_[] = new int[3 * (Collator.TERTIARY + 1)];
        Token m_fStrToken_[] = new Token[Collator.TERTIARY + 1];
        Token m_lStrToken_[] = new Token[Collator.TERTIARY + 1];
    }

    /**
     * Token wrapper for collation rules
     */
    static class Token
    {
       // package private data members ---------------------------------------

       int m_CE_[];
       int m_CELength_;
       int m_expCE_[];
       int m_expCELength_;
       int m_source_;
       int m_expansion_;
       int m_prefix_;
       int m_strength_;
       int m_toInsert_;
       int m_polarity_; // 1 for <, <<, <<<, , ; and 0 for >, >>, >>>
       TokenListHeader m_listHeader_;
       Token m_previous_;
       Token m_next_;
       StringBuffer m_rules_;
       char m_flags_;

       // package private constructors ---------------------------------------

       Token()
       {
           m_CE_ = new int[128];
           m_expCE_ = new int[128];
           // TODO: this should also handle reverse
           m_polarity_ = TOKEN_POLARITY_POSITIVE_;
           m_next_ = null;
           m_previous_ = null;
           m_CELength_ = 0;
           m_expCELength_ = 0;
       }

       // package private methods --------------------------------------------

       /**
        * Hashcode calculation for token
        * @return the hashcode
        */
       public int hashCode()
       {
           int result = 0;
           int len = (m_source_ & 0xFF000000) >>> 24;
           int inc = ((len - 32) / 32) + 1;

           int start = m_source_ & 0x00FFFFFF;
           int limit = start + len;

           while (start < limit) {
               result = (result * 37) + m_rules_.charAt(start);
               start += inc;
           }
           return result;
       }

       /**
        * Equals calculation
        * @param target object to compare
        * @return true if target is the same as this object
        */
       public boolean equals(Object target)
       {
           if (target == this) {
               return true;
           }
           if (target instanceof Token) {
               Token t = (Token)target;
               int sstart = m_source_ & 0x00FFFFFF;
               int tstart = t.m_source_ & 0x00FFFFFF;
               int slimit = (m_source_ & 0xFF000000) >> 24;
               int tlimit = (m_source_ & 0xFF000000) >> 24;

               int end = sstart + slimit - 1;

               if (m_source_ == 0 || t.m_source_ == 0) {
                   return false;
               }
               if (slimit != tlimit) {
                   return false;
               }
               if (m_source_ == t.m_source_) {
                   return true;
               }

               while (sstart < end
                      && m_rules_.charAt(sstart) == t.m_rules_.charAt(tstart))
               {
                   ++ sstart;
                   ++ tstart;
               }
               if (m_rules_.charAt(sstart) == t.m_rules_.charAt(tstart)) {
                   return true;
               }
           }
           return false;
        }
    }

    // package private data member -------------------------------------------

    /**
     * Indicator that the token is resetted yet, ie & in the rules
     */
    static final int TOKEN_RESET_ = 0xDEADBEEF;

    /**
     * Size of the number of tokens
     */
    int m_resultLength_;
    /**
     * List of parsed tokens
     */
    TokenListHeader m_listHeader_[];
    /**
     * Variable top token
     */
    Token m_variableTop_;
    /**
     * Collation options
     */
    OptionSet m_options_;
    /**
     * Normalized collation rules with some extra characters
     */
    StringBuffer m_source_;
    /**
     * Hash table to keep all tokens
     */
    Hashtable<Token, Token> m_hashTable_;

    // package private method ------------------------------------------------

    void setDefaultOptionsInCollator(RuleBasedCollator collator)
    {
        collator.m_defaultStrength_ = m_options_.m_strength_;
        collator.m_defaultDecomposition_ = m_options_.m_decomposition_;
        collator.m_defaultIsFrenchCollation_ = m_options_.m_isFrenchCollation_;
        collator.m_defaultIsAlternateHandlingShifted_
                                    = m_options_.m_isAlternateHandlingShifted_;
        collator.m_defaultIsCaseLevel_ = m_options_.m_isCaseLevel_;
        collator.m_defaultCaseFirst_ = m_options_.m_caseFirst_;
        collator.m_defaultIsHiragana4_ = m_options_.m_isHiragana4_;
        collator.m_defaultVariableTopValue_ = m_options_.m_variableTopValue_;
    }

    // private inner classes -------------------------------------------------

    /**
     * This is a token that has been parsed but not yet processed. Used to
     * reduce the number of arguments in the parser
     */
    private static class ParsedToken
    {
        // private constructor ----------------------------------------------

        /**
         * Empty constructor
         */
        ParsedToken()
        {
            m_charsLen_ = 0;
            m_charsOffset_ = 0;
            m_extensionLen_ = 0;
            m_extensionOffset_ = 0;
            m_prefixLen_ = 0;
            m_prefixOffset_ = 0;
            m_flags_ = 0;
            m_strength_ = TOKEN_UNSET_;
        }

        // private data members ---------------------------------------------

        int m_strength_;
        int m_charsOffset_;
        int m_charsLen_;
        int m_extensionOffset_;
        int m_extensionLen_;
        int m_prefixOffset_;
        int m_prefixLen_;
        char m_flags_;
        char m_indirectIndex_;
    }

    /**
     * Boundary wrappers
     */
    private static class IndirectBoundaries
    {
        // package private constructor ---------------------------------------

        IndirectBoundaries(int startce[], int limitce[])
        {
            // Set values for the top - TODO: once we have values for all the
            // indirects, we are going to initalize here.
            m_startCE_ = startce[0];
            m_startContCE_ = startce[1];
            if (limitce != null) {
                m_limitCE_ = limitce[0];
                m_limitContCE_ = limitce[1];
            }
            else {
                m_limitCE_ = 0;
                m_limitContCE_ = 0;
            }
        }

        // package private data members --------------------------------------

        int m_startCE_;
        int m_startContCE_;
        int m_limitCE_;
        int m_limitContCE_;
    }

    /**
     * Collation option rule tag
     */
    private static class TokenOption
    {
        // package private constructor ---------------------------------------

        TokenOption(String name, int attribute, String suboptions[],
                    int suboptionattributevalue[])
        {
            m_name_ = name;
            m_attribute_ = attribute;
            m_subOptions_ = suboptions;
            m_subOptionAttributeValues_ = suboptionattributevalue;
        }

        // package private data member ---------------------------------------

        private String m_name_;
        private int m_attribute_;
        private String m_subOptions_[];
        private int m_subOptionAttributeValues_[];
    }

    // private variables -----------------------------------------------------

    /**
     * Current parsed token
     */
    private ParsedToken m_parsedToken_;
    /**
     * Collation rule
     */
    private String m_rules_;
    private int m_current_;
    /**
     * End of the option while reading.
     * Need it for UnicodeSet reading support.
     */
    private int m_optionEnd_;
    /*
     * Current offset in m_source
     */
    //private int m_sourceLimit_;
    /**
     * Offset to m_source_ ofr the extra expansion characters
     */
    private int m_extraCurrent_;

    /**
     * UnicodeSet that contains code points to be copied from the UCA
     */
    UnicodeSet m_copySet_;

    /**
     * UnicodeSet that contains code points for which we want to remove
     * UCA contractions. It implies copying of these code points from
     * the UCA.
     */
    UnicodeSet m_removeSet_;
    /*
     * This is space for the extra strings that need to be unquoted during the
     * parsing of the rules
     */
    //private static final int TOKEN_EXTRA_RULE_SPACE_SIZE_ = 2048;
    /**
     * Indicator that the token is not set yet
     */
    private static final int TOKEN_UNSET_ = 0xFFFFFFFF;
    /*
     * Indicator that the rule is in the > polarity, ie everything on the
     * right of the rule is less than
     */
    //private static final int TOKEN_POLARITY_NEGATIVE_ = 0;
    /**
     * Indicator that the rule is in the < polarity, ie everything on the
     * right of the rule is greater than
     */
    private static final int TOKEN_POLARITY_POSITIVE_ = 1;
    /**
     * Flag mask to determine if top is set
     */
    private static final int TOKEN_TOP_MASK_ = 0x04;
    /**
     * Flag mask to determine if variable top is set
     */
    private static final int TOKEN_VARIABLE_TOP_MASK_ = 0x08;
    /**
     * Flag mask to determine if a before attribute is set
     */
    private static final int TOKEN_BEFORE_ = 0x03;
    /**
     * For use in parsing token options
     */
    private static final int TOKEN_SUCCESS_MASK_ = 0x10;

    /**
     * These values are used for finding CE values for indirect positioning.
     * Indirect positioning is a mechanism for allowing resets on symbolic
     * values. It only works for resets and you cannot tailor indirect names.
     * An indirect name can define either an anchor point or a range. An anchor
     * point behaves in exactly the same way as a code point in reset would,
     * except that it cannot be tailored. A range (we currently only know for
     * the [top] range will explicitly set the upper bound for generated CEs,
     * thus allowing for better control over how many CEs can be squeezed
     * between in the range without performance penalty. In that respect, we use
     * [top] for tailoring of locales that use CJK characters. Other indirect
     * values are currently a pure convenience, they can be used to assure that
     * the CEs will be always positioned in the same place relative to a point
     * with known properties (e.g. first primary ignorable).
     */
    private static final IndirectBoundaries INDIRECT_BOUNDARIES_[];

//    /**
//     * Inverse UCA constants
//     */
//    private static final int INVERSE_SIZE_MASK_ = 0xFFF00000;
//    private static final int INVERSE_OFFSET_MASK_ = 0x000FFFFF;
//    private static final int INVERSE_SHIFT_VALUE_ = 20;

    /**
     * Collation option tags
     * [last variable] last variable value
     * [last primary ignorable] largest CE for primary ignorable
     * [last secondary ignorable] largest CE for secondary ignorable
     * [last tertiary ignorable] largest CE for tertiary ignorable
     * [top] guaranteed to be above all implicit CEs, for now and in the future (in 1.8)
     */
    private static final TokenOption RULES_OPTIONS_[];

    static
    {
        INDIRECT_BOUNDARIES_ = new IndirectBoundaries[15];
        // UCOL_RESET_TOP_VALUE
        INDIRECT_BOUNDARIES_[0] = new IndirectBoundaries(
                        RuleBasedCollator.UCA_CONSTANTS_.LAST_NON_VARIABLE_,
                        RuleBasedCollator.UCA_CONSTANTS_.FIRST_IMPLICIT_);
        // UCOL_FIRST_PRIMARY_IGNORABLE
        INDIRECT_BOUNDARIES_[1] = new IndirectBoundaries(
                    RuleBasedCollator.UCA_CONSTANTS_.FIRST_PRIMARY_IGNORABLE_,
                    null);
        // UCOL_LAST_PRIMARY_IGNORABLE
        INDIRECT_BOUNDARIES_[2] = new IndirectBoundaries(
                    RuleBasedCollator.UCA_CONSTANTS_.LAST_PRIMARY_IGNORABLE_,
                    null);

        // UCOL_FIRST_SECONDARY_IGNORABLE
        INDIRECT_BOUNDARIES_[3] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_SECONDARY_IGNORABLE_,
                   null);
        // UCOL_LAST_SECONDARY_IGNORABLE
        INDIRECT_BOUNDARIES_[4] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_SECONDARY_IGNORABLE_,
                   null);
        // UCOL_FIRST_TERTIARY_IGNORABLE
        INDIRECT_BOUNDARIES_[5] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_TERTIARY_IGNORABLE_,
                   null);
        // UCOL_LAST_TERTIARY_IGNORABLE
        INDIRECT_BOUNDARIES_[6] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_TERTIARY_IGNORABLE_,
                   null);
        // UCOL_FIRST_VARIABLE;
        INDIRECT_BOUNDARIES_[7] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_VARIABLE_,
                   null);
        // UCOL_LAST_VARIABLE
        INDIRECT_BOUNDARIES_[8] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_VARIABLE_,
                   null);
        // UCOL_FIRST_NON_VARIABLE
        INDIRECT_BOUNDARIES_[9] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_NON_VARIABLE_,
                   null);
        // UCOL_LAST_NON_VARIABLE
        INDIRECT_BOUNDARIES_[10] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_NON_VARIABLE_,
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_IMPLICIT_);
        // UCOL_FIRST_IMPLICIT
        INDIRECT_BOUNDARIES_[11] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_IMPLICIT_,
                   null);
        // UCOL_LAST_IMPLICIT
        INDIRECT_BOUNDARIES_[12] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_IMPLICIT_,
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_TRAILING_);
        // UCOL_FIRST_TRAILING
        INDIRECT_BOUNDARIES_[13] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.FIRST_TRAILING_,
                   null);
        // UCOL_LAST_TRAILING
        INDIRECT_BOUNDARIES_[14] = new IndirectBoundaries(
                   RuleBasedCollator.UCA_CONSTANTS_.LAST_TRAILING_,
                   null);
        INDIRECT_BOUNDARIES_[14].m_limitCE_
                 = RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_SPECIAL_MIN_ << 24;

        RULES_OPTIONS_ = new TokenOption[19];
        String option[] = {"non-ignorable", "shifted"};
        int value[] = {RuleBasedCollator.AttributeValue.NON_IGNORABLE_,
                       RuleBasedCollator.AttributeValue.SHIFTED_};
        RULES_OPTIONS_[0] = new TokenOption("alternate",
                              RuleBasedCollator.Attribute.ALTERNATE_HANDLING_,
                              option, value);
        option = new String[1];
        option[0] = "2";
        value = new int[1];
        value[0] = RuleBasedCollator.AttributeValue.ON_;
        RULES_OPTIONS_[1] = new TokenOption("backwards",
                                 RuleBasedCollator.Attribute.FRENCH_COLLATION_,
                                 option, value);
        String offonoption[] = new String[2];
        offonoption[0] = "off";
        offonoption[1] = "on";
        int offonvalue[] = new int[2];
        offonvalue[0] = RuleBasedCollator.AttributeValue.OFF_;
        offonvalue[1] = RuleBasedCollator.AttributeValue.ON_;
        RULES_OPTIONS_[2] = new TokenOption("caseLevel",
                                       RuleBasedCollator.Attribute.CASE_LEVEL_,
                                       offonoption, offonvalue);
        option = new String[3];
        option[0] = "lower";
        option[1] = "upper";
        option[2] = "off";
        value = new int[3];
        value[0] = RuleBasedCollator.AttributeValue.LOWER_FIRST_;
        value[1] = RuleBasedCollator.AttributeValue.UPPER_FIRST_;
        value[2] = RuleBasedCollator.AttributeValue.OFF_;
        RULES_OPTIONS_[3] = new TokenOption("caseFirst",
                                       RuleBasedCollator.Attribute.CASE_FIRST_,
                                       option, value);
        RULES_OPTIONS_[4] = new TokenOption("normalization",
                               RuleBasedCollator.Attribute.NORMALIZATION_MODE_,
                               offonoption, offonvalue);
        RULES_OPTIONS_[5] = new TokenOption("hiraganaQ",
                         RuleBasedCollator.Attribute.HIRAGANA_QUATERNARY_MODE_,
                         offonoption, offonvalue);
        option = new String[5];
        option[0] = "1";
        option[1] = "2";
        option[2] = "3";
        option[3] = "4";
        option[4] = "I";
        value = new int[5];
        value[0] = RuleBasedCollator.AttributeValue.PRIMARY_;
        value[1] = RuleBasedCollator.AttributeValue.SECONDARY_;
        value[2] = RuleBasedCollator.AttributeValue.TERTIARY_;
        value[3] = RuleBasedCollator.AttributeValue.QUATERNARY_;
        value[4] = RuleBasedCollator.AttributeValue.IDENTICAL_;
        RULES_OPTIONS_[6] = new TokenOption("strength",
                                         RuleBasedCollator.Attribute.STRENGTH_,
                                         option, value);
        RULES_OPTIONS_[7] = new TokenOption("variable top",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[8] = new TokenOption("rearrange",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        option = new String[3];
        option[0] = "1";
        option[1] = "2";
        option[2] = "3";
        value = new int[3];
        value[0] = RuleBasedCollator.AttributeValue.PRIMARY_;
        value[1] = RuleBasedCollator.AttributeValue.SECONDARY_;
        value[2] = RuleBasedCollator.AttributeValue.TERTIARY_;
        RULES_OPTIONS_[9] = new TokenOption("before",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  option, value);
        RULES_OPTIONS_[10] = new TokenOption("top",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        String firstlastoption[] = new String[7];
        firstlastoption[0] = "primary";
        firstlastoption[1] = "secondary";
        firstlastoption[2] = "tertiary";
        firstlastoption[3] = "variable";
        firstlastoption[4] = "regular";
        firstlastoption[5] = "implicit";
        firstlastoption[6] = "trailing";

        int firstlastvalue[] = new int[7];
        Arrays.fill(firstlastvalue, RuleBasedCollator.AttributeValue.PRIMARY_);

        RULES_OPTIONS_[11] = new TokenOption("first",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  firstlastoption, firstlastvalue);
        RULES_OPTIONS_[12] = new TokenOption("last",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  firstlastoption, firstlastvalue);
        RULES_OPTIONS_[13] = new TokenOption("optimize",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[14] = new TokenOption("suppressContractions",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[15] = new TokenOption("undefined",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[16] = new TokenOption("scriptOrder",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[17] = new TokenOption("charsetname",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
        RULES_OPTIONS_[18] = new TokenOption("charset",
                                  RuleBasedCollator.Attribute.LIMIT_,
                                  null, null);
    }

    /**
     * Utility data members
     */
    private Token m_utilToken_ = new Token();
    private CollationElementIterator m_UCAColEIter_
                      = RuleBasedCollator.UCA_.getCollationElementIterator("");
    private int m_utilCEBuffer_[] = new int[2];

    // private methods -------------------------------------------------------

    /**
     * Assembles the token list
     * @exception ParseException thrown when rules syntax fails
     */
    int assembleTokenList() throws ParseException
    {
        Token lastToken = null;
        m_parsedToken_.m_strength_ = TOKEN_UNSET_;
        int sourcelimit = m_source_.length();
        int expandNext = 0;

        while (m_current_ < sourcelimit) {
            m_parsedToken_.m_prefixOffset_ = 0;
            if (parseNextToken(lastToken == null) < 0) {
                // we have reached the end
                continue;
            }
            char specs = m_parsedToken_.m_flags_;
            boolean variableTop = ((specs & TOKEN_VARIABLE_TOP_MASK_) != 0);
            boolean top = ((specs & TOKEN_TOP_MASK_) != 0);
            int lastStrength = TOKEN_UNSET_;
            if (lastToken != null) {
                lastStrength = lastToken.m_strength_;
            }
            m_utilToken_.m_source_ = m_parsedToken_.m_charsLen_ << 24
                                             | m_parsedToken_.m_charsOffset_;
            m_utilToken_.m_rules_ = m_source_;
            // 4 Lookup each source in the CharsToToken map, and find a
            // sourcetoken
            Token sourceToken = m_hashTable_.get(m_utilToken_);
            if (m_parsedToken_.m_strength_ != TOKEN_RESET_) {
                if (lastToken == null) {
                    // this means that rules haven't started properly
                    throwParseException(m_source_.toString(), 0);
                }
                //  6 Otherwise (when relation != reset)
                if (sourceToken == null) {
                    // If sourceToken is null, create new one
                    sourceToken = new Token();
                     sourceToken.m_rules_ = m_source_;
                    sourceToken.m_source_ = m_parsedToken_.m_charsLen_ << 24
                                           | m_parsedToken_.m_charsOffset_;
                    sourceToken.m_prefix_ = m_parsedToken_.m_prefixLen_ << 24
                                           | m_parsedToken_.m_prefixOffset_;
                    // TODO: this should also handle reverse
                    sourceToken.m_polarity_ = TOKEN_POLARITY_POSITIVE_;
                    sourceToken.m_next_ = null;
                     sourceToken.m_previous_ = null;
                    sourceToken.m_CELength_ = 0;
                    sourceToken.m_expCELength_ = 0;
                    m_hashTable_.put(sourceToken, sourceToken);
                }
                else {
                    // we could have fished out a reset here
                    if (sourceToken.m_strength_ != TOKEN_RESET_
                        && lastToken != sourceToken) {
                        // otherwise remove sourceToken from where it was.
                        if (sourceToken.m_next_ != null) {
                            if (sourceToken.m_next_.m_strength_
                                                   > sourceToken.m_strength_) {
                                sourceToken.m_next_.m_strength_
                                                   = sourceToken.m_strength_;
                            }
                            sourceToken.m_next_.m_previous_
                                                    = sourceToken.m_previous_;
                        }
                        else {
                            sourceToken.m_listHeader_.m_last_
                                                    = sourceToken.m_previous_;
                        }
                        if (sourceToken.m_previous_ != null) {
                            sourceToken.m_previous_.m_next_
                                                        = sourceToken.m_next_;
                        }
                        else {
                            sourceToken.m_listHeader_.m_first_
                                                        = sourceToken.m_next_;
                        }
                        sourceToken.m_next_ = null;
                        sourceToken.m_previous_ = null;
                    }
                }
                sourceToken.m_strength_ = m_parsedToken_.m_strength_;
                sourceToken.m_listHeader_ = lastToken.m_listHeader_;

                // 1.  Find the strongest strength in each list, and set
                // strongestP and strongestN accordingly in the headers.
                if (lastStrength == TOKEN_RESET_
                    || sourceToken.m_listHeader_.m_first_ == null) {
                    // If LAST is a reset insert sourceToken in the list.
                    if (sourceToken.m_listHeader_.m_first_ == null) {
                        sourceToken.m_listHeader_.m_first_ = sourceToken;
                        sourceToken.m_listHeader_.m_last_ = sourceToken;
                    }
                    else { // we need to find a place for us
                           // and we'll get in front of the same strength
                        if (sourceToken.m_listHeader_.m_first_.m_strength_
                                                 <= sourceToken.m_strength_) {
                            sourceToken.m_next_
                                          = sourceToken.m_listHeader_.m_first_;
                            sourceToken.m_next_.m_previous_ = sourceToken;
                            sourceToken.m_listHeader_.m_first_ = sourceToken;
                            sourceToken.m_previous_ = null;
                        }
                        else {
                            lastToken = sourceToken.m_listHeader_.m_first_;
                            while (lastToken.m_next_ != null
                                   && lastToken.m_next_.m_strength_
                                                 > sourceToken.m_strength_) {
                                lastToken = lastToken.m_next_;
                            }
                            if (lastToken.m_next_ != null) {
                                lastToken.m_next_.m_previous_ = sourceToken;
                            }
                            else {
                                sourceToken.m_listHeader_.m_last_
                                                               = sourceToken;
                            }
                            sourceToken.m_previous_ = lastToken;
                            sourceToken.m_next_ = lastToken.m_next_;
                            lastToken.m_next_ = sourceToken;
                        }
                    }
                }
                else {
                    // Otherwise (when LAST is not a reset)
                    // if polarity (LAST) == polarity(relation), insert
                    // sourceToken after LAST, otherwise insert before.
                    // when inserting after or before, search to the next
                    // position with the same strength in that direction.
                    // (This is called postpone insertion).
                    if (sourceToken != lastToken) {
                        if (lastToken.m_polarity_ == sourceToken.m_polarity_) {
                            while (lastToken.m_next_ != null
                                   && lastToken.m_next_.m_strength_
                                                   > sourceToken.m_strength_) {
                                lastToken = lastToken.m_next_;
                            }
                            sourceToken.m_previous_ = lastToken;
                            if (lastToken.m_next_ != null) {
                                lastToken.m_next_.m_previous_ = sourceToken;
                            }
                            else {
                                sourceToken.m_listHeader_.m_last_ = sourceToken;
                            }
                            sourceToken.m_next_ = lastToken.m_next_;
                            lastToken.m_next_ = sourceToken;
                        }
                        else {
                            while (lastToken.m_previous_ != null
                                   && lastToken.m_previous_.m_strength_
                                                > sourceToken.m_strength_) {
                                lastToken = lastToken.m_previous_;
                            }
                            sourceToken.m_next_ = lastToken;
                            if (lastToken.m_previous_ != null) {
                                lastToken.m_previous_.m_next_ = sourceToken;
                            }
                            else {
                                sourceToken.m_listHeader_.m_first_
                                                                 = sourceToken;
                            }
                            sourceToken.m_previous_ = lastToken.m_previous_;
                            lastToken.m_previous_ = sourceToken;
                        }
                    }
                    else { // repeated one thing twice in rules, stay with the
                           // stronger strength
                        if (lastStrength < sourceToken.m_strength_) {
                            sourceToken.m_strength_ = lastStrength;
                        }
                    }
                }
                // if the token was a variable top, we're gonna put it in
                if (variableTop == true && m_variableTop_ == null) {
                    variableTop = false;
                    m_variableTop_ = sourceToken;
                }
                // Treat the expansions.
                // There are two types of expansions: explicit (x / y) and
                // reset based propagating expansions
                // (&abc * d * e <=> &ab * d / c * e / c)
                // if both of them are in effect for a token, they are combined.
               sourceToken.m_expansion_ = m_parsedToken_.m_extensionLen_ << 24
                                          | m_parsedToken_.m_extensionOffset_;
               if (expandNext != 0) {
                   if (sourceToken.m_strength_ == RuleBasedCollator.PRIMARY) {
                       // primary strength kills off the implicit expansion
                       expandNext = 0;
                   }
                   else if (sourceToken.m_expansion_ == 0) {
                       // if there is no expansion, implicit is just added to
                       // the token
                       sourceToken.m_expansion_ = expandNext;
                   }
                   else {
                       // there is both explicit and implicit expansion.
                       // We need to make a combination
                       int start = expandNext & 0xFFFFFF;
                       int size = expandNext >>> 24;
                       if (size > 0) {
                          m_source_.append(m_source_.substring(start,
                                                               start + size));
                       }
                          start = m_parsedToken_.m_extensionOffset_;
                       m_source_.append(m_source_.substring(start,
                                      start + m_parsedToken_.m_extensionLen_));
                       sourceToken.m_expansion_ = (size
                                       + m_parsedToken_.m_extensionLen_) << 24
                                       | m_extraCurrent_;
                       m_extraCurrent_ += size + m_parsedToken_.m_extensionLen_;
                   }
                }
               // if the previous token was a reset before, the strength of this
               // token must match the strength of before. Otherwise we have an
               // undefined situation.
               // In other words, we currently have a cludge which we use to
               // represent &a >> x. This is written as &[before 2]a << x.
               if((lastToken.m_flags_ & TOKEN_BEFORE_) != 0) {
                   int beforeStrength = (lastToken.m_flags_ & TOKEN_BEFORE_) - 1;
                   if(beforeStrength != sourceToken.m_strength_) {
                          throwParseException(m_source_.toString(), m_current_);
                   }
               }

            }
            else {
                if (lastToken != null && lastStrength == TOKEN_RESET_) {
                    // if the previous token was also a reset, this means that
                    // we have two consecutive resets and we want to remove the
                    // previous one if empty
                    if (m_resultLength_ > 0 && m_listHeader_[m_resultLength_ - 1].m_first_ == null) {
                        m_resultLength_ --;
                    }
                }
                if (sourceToken == null) {
                    // this is a reset, but it might still be somewhere in the
                    // tailoring, in shorter form
                    int searchCharsLen = m_parsedToken_.m_charsLen_;
                    while (searchCharsLen > 1 && sourceToken == null) {
                        searchCharsLen --;
                        // key = searchCharsLen << 24 | charsOffset;
                        m_utilToken_.m_source_ = searchCharsLen << 24
                                             | m_parsedToken_.m_charsOffset_;
                        m_utilToken_.m_rules_ = m_source_;
                        sourceToken = m_hashTable_.get(m_utilToken_);
                    }
                    if (sourceToken != null) {
                        expandNext = (m_parsedToken_.m_charsLen_
                                                      - searchCharsLen) << 24
                                        | (m_parsedToken_.m_charsOffset_
                                           + searchCharsLen);
                    }
                }
                if ((specs & TOKEN_BEFORE_) != 0) {
                    if (top == false) {
                        // we're doing before & there is no indirection
                        int strength = (specs & TOKEN_BEFORE_) - 1;
                        if (sourceToken != null
                            && sourceToken.m_strength_ != TOKEN_RESET_) {
                            // this is a before that is already ordered in the UCA
                            // - so we need to get the previous with good strength
                            while (sourceToken.m_strength_ > strength
                                   && sourceToken.m_previous_ != null) {
                                sourceToken = sourceToken.m_previous_;
                            }
                            // here, either we hit the strength or NULL
                            if (sourceToken.m_strength_ == strength) {
                                if (sourceToken.m_previous_ != null) {
                                    sourceToken = sourceToken.m_previous_;
                                }
                                else { // start of list
                                    sourceToken
                                         = sourceToken.m_listHeader_.m_reset_;
                                }
                            }
                            else { // we hit NULL, we should be doing the else part
                                sourceToken
                                         = sourceToken.m_listHeader_.m_reset_;
                                sourceToken = getVirginBefore(sourceToken,
                                                              strength);
                            }
                        }
                        else {
                            sourceToken
                                      = getVirginBefore(sourceToken, strength);
                        }
                    }
                    else {
                        // this is both before and indirection
                        top = false;
                        m_listHeader_[m_resultLength_] = new TokenListHeader();
                        m_listHeader_[m_resultLength_].m_previousCE_ = 0;
                        m_listHeader_[m_resultLength_].m_previousContCE_ = 0;
                        m_listHeader_[m_resultLength_].m_indirect_ = true;
                        // we need to do slightly more work. we need to get the
                        // baseCE using the inverse UCA & getPrevious. The next
                        // bound is not set, and will be decided in ucol_bld
                        int strength = (specs & TOKEN_BEFORE_) - 1;
                        int baseCE = INDIRECT_BOUNDARIES_[
                                   m_parsedToken_.m_indirectIndex_].m_startCE_;
                        int baseContCE = INDIRECT_BOUNDARIES_[
                               m_parsedToken_.m_indirectIndex_].m_startContCE_;
                        int ce[] = new int[2];
                        if((baseCE >>> 24 >= RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MIN_)
                        && (baseCE >>> 24 <=  RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MAX_)) { /* implicits - */
                            int primary = baseCE & RuleBasedCollator.CE_PRIMARY_MASK_ | (baseContCE & RuleBasedCollator.CE_PRIMARY_MASK_) >> 16;
                            int raw = RuleBasedCollator.impCEGen_.getRawFromImplicit(primary);
                            int primaryCE = RuleBasedCollator.impCEGen_.getImplicitFromRaw(raw-1);
                            ce[0] = primaryCE & RuleBasedCollator.CE_PRIMARY_MASK_ | 0x0505;
                            ce[1] = (primaryCE << 16) & RuleBasedCollator.CE_PRIMARY_MASK_ | RuleBasedCollator.CE_CONTINUATION_MARKER_;
                        } else {
                            CollationParsedRuleBuilder.InverseUCA invuca
                                = CollationParsedRuleBuilder.INVERSE_UCA_;
                            invuca.getInversePrevCE(baseCE, baseContCE, strength,
                                    ce);
                        }
                        m_listHeader_[m_resultLength_].m_baseCE_ = ce[0];
                        m_listHeader_[m_resultLength_].m_baseContCE_ = ce[1];
                        m_listHeader_[m_resultLength_].m_nextCE_ = 0;
                        m_listHeader_[m_resultLength_].m_nextContCE_ = 0;

                        sourceToken = new Token();
                        expandNext = initAReset(0, sourceToken);
                    }
                }
                // 5 If the relation is a reset:
                // If sourceToken is null
                // Create new list, create new sourceToken, make the baseCE
                // from source, put the sourceToken in ListHeader of the new
                // list
                if (sourceToken == null) {
                    if (m_listHeader_[m_resultLength_] == null) {
                        m_listHeader_[m_resultLength_] = new TokenListHeader();
                    }
                    // 3 Consider each item: relation, source, and expansion:
                    // e.g. ...< x / y ...
                    // First convert all expansions into normal form.
                    // Examples:
                    // If "xy" doesn't occur earlier in the list or in the UCA,
                    // convert &xy * c * d * ... into &x * c/y * d * ...
                    // Note: reset values can never have expansions, although
                    // they can cause the very next item to have one. They may
                    // be contractions, if they are found earlier in the list.
                    if (top == false) {
                        CollationElementIterator coleiter
                        = RuleBasedCollator.UCA_.getCollationElementIterator(
                            m_source_.substring(m_parsedToken_.m_charsOffset_,
                                                m_parsedToken_.m_charsOffset_
                                                + m_parsedToken_.m_charsLen_));

                        int CE = coleiter.next();
                        // offset to the character in the full rule string
                        int expand = coleiter.getOffset()
                                     + m_parsedToken_.m_charsOffset_;
                        int SecondCE = coleiter.next();

                        m_listHeader_[m_resultLength_].m_baseCE_
                                                             = CE & 0xFFFFFF3F;
                        if (RuleBasedCollator.isContinuation(SecondCE)) {
                            m_listHeader_[m_resultLength_].m_baseContCE_
                                                                    = SecondCE;
                        }
                        else {
                            m_listHeader_[m_resultLength_].m_baseContCE_ = 0;
                        }
                        m_listHeader_[m_resultLength_].m_nextCE_ = 0;
                        m_listHeader_[m_resultLength_].m_nextContCE_ = 0;
                        m_listHeader_[m_resultLength_].m_previousCE_ = 0;
                        m_listHeader_[m_resultLength_].m_previousContCE_ = 0;
                        m_listHeader_[m_resultLength_].m_indirect_ = false;
                        sourceToken = new Token();
                        expandNext = initAReset(expand, sourceToken);
                    }
                    else { // top == TRUE
                        top = false;
                        m_listHeader_[m_resultLength_].m_previousCE_ = 0;
                        m_listHeader_[m_resultLength_].m_previousContCE_ = 0;
                        m_listHeader_[m_resultLength_].m_indirect_ = true;
                        IndirectBoundaries ib = INDIRECT_BOUNDARIES_[
                                              m_parsedToken_.m_indirectIndex_];
                        m_listHeader_[m_resultLength_].m_baseCE_
                                                               = ib.m_startCE_;
                        m_listHeader_[m_resultLength_].m_baseContCE_
                                                           = ib.m_startContCE_;
                        m_listHeader_[m_resultLength_].m_nextCE_
                                                               = ib.m_limitCE_;
                        m_listHeader_[m_resultLength_].m_nextContCE_
                                                           = ib.m_limitContCE_;
                        sourceToken = new Token();
                        expandNext = initAReset(0, sourceToken);
                    }
                }
                else { // reset to something already in rules
                    top = false;
                }
            }
            // 7 After all this, set LAST to point to sourceToken, and goto
            // step 3.
            lastToken = sourceToken;
        }

        if (m_resultLength_ > 0
            && m_listHeader_[m_resultLength_ - 1].m_first_ == null) {
            m_resultLength_ --;
        }
        return m_resultLength_;
    }

    /**
     * Formats and throws a ParseException
     * @param rules collation rule that failed
     * @param offset failed offset in rules
     * @throws ParseException with failure information
     */
    private static final void throwParseException(String rules, int offset)
                                                          throws ParseException
    {
        // for pre-context
        String precontext = rules.substring(0, offset);
        String postcontext = rules.substring(offset, rules.length());
        StringBuffer error = new StringBuffer(
                                    "Parse error occurred in rule at offset ");
        error.append(offset);
        error.append("\n after the prefix \"");
        error.append(precontext);
        error.append("\" before the suffix \"");
        error.append(postcontext);
        throw new ParseException(error.toString(), offset);
    }

    private final boolean doSetTop() {
        m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
        m_source_.append((char)0xFFFE);
        IndirectBoundaries ib =
                  INDIRECT_BOUNDARIES_[m_parsedToken_.m_indirectIndex_];
        m_source_.append((char)(ib.m_startCE_ >> 16));
        m_source_.append((char)(ib.m_startCE_ & 0xFFFF));
        m_extraCurrent_ += 3;
        if (INDIRECT_BOUNDARIES_[m_parsedToken_.m_indirectIndex_
                                                       ].m_startContCE_ == 0) {
            m_parsedToken_.m_charsLen_ = 3;
        }
        else {
            m_source_.append((char)(INDIRECT_BOUNDARIES_[
                                        m_parsedToken_.m_indirectIndex_
                                    ].m_startContCE_ >> 16));
            m_source_.append((char)(INDIRECT_BOUNDARIES_[
                                        m_parsedToken_.m_indirectIndex_
                                    ].m_startContCE_ & 0xFFFF));
            m_extraCurrent_ += 2;
            m_parsedToken_.m_charsLen_ = 5;
        }
        return true;
    }

    private static boolean isCharNewLine(char c) {
        switch (c) {
        case 0x000A: /* LF */
        case 0x000D: /* CR */
        case 0x000C: /* FF */
        case 0x0085: /* NEL */
        case 0x2028: /* LS */
        case 0x2029: /* PS */
            return true;
        default:
            return false;
        }
    }

    /**
     * Getting the next token
     *
     * @param startofrules
     *            flag indicating if we are at the start of rules
     * @return the offset of the rules
     * @exception ParseException
     *                thrown when rule parsing fails
     */
    @SuppressWarnings("fallthrough")
    private int parseNextToken(boolean startofrules) throws ParseException
    {
        // parsing part
        boolean variabletop = false;
        boolean top = false;
        boolean inchars = true;
        boolean inquote = false;
        boolean wasinquote = false;
        byte before = 0;
        boolean isescaped = false;
        int /*newcharslen = 0,*/ newextensionlen = 0;
        int /*charsoffset = 0,*/ extensionoffset = 0;
        int newstrength = TOKEN_UNSET_;

        m_parsedToken_.m_charsLen_ = 0;
        m_parsedToken_.m_charsOffset_ = 0;
        m_parsedToken_.m_prefixOffset_ = 0;
        m_parsedToken_.m_prefixLen_ = 0;
        m_parsedToken_.m_indirectIndex_ = 0;

        int limit = m_rules_.length();
        while (m_current_ < limit) {
            char ch = m_source_.charAt(m_current_);
            if (inquote) {
                if (ch == 0x0027) { // '\''
                    inquote = false;
                }
                else {
                    if ((m_parsedToken_.m_charsLen_ == 0) || inchars) {
                         if (m_parsedToken_.m_charsLen_ == 0) {
                             m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                         }
                         m_parsedToken_.m_charsLen_ ++;
                    }
                    else {
                        if (newextensionlen == 0) {
                            extensionoffset = m_extraCurrent_;
                        }
                        newextensionlen ++;
                    }
                }
            }
            else if (isescaped) {
                isescaped = false;
                if (newstrength == TOKEN_UNSET_) {
                    throwParseException(m_rules_, m_current_);
                }
                if (ch != 0 && m_current_ != limit) {
                    if (inchars) {
                        if (m_parsedToken_.m_charsLen_ == 0) {
                            m_parsedToken_.m_charsOffset_ = m_current_;
                        }
                        m_parsedToken_.m_charsLen_ ++;
                    }
                    else {
                        if (newextensionlen == 0) {
                            extensionoffset = m_current_;
                        }
                        newextensionlen ++;
                    }
                }
            }
            else {
                if (!UCharacterProperty.isRuleWhiteSpace(ch)) {
                    // Sets the strength for this entry
                    switch (ch) {
                    case 0x003D : // '='
                        if (newstrength != TOKEN_UNSET_) {
                            return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        // if we start with strength, we'll reset to top
                        if (startofrules == true) {
                            m_parsedToken_.m_indirectIndex_ = 5;
                            top = doSetTop();
                            return doEndParseNextToken(TOKEN_RESET_,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        newstrength = Collator.IDENTICAL;
                        break;
                    case 0x002C : // ','
                        if (newstrength != TOKEN_UNSET_) {
                            return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        // if we start with strength, we'll reset to top
                        if (startofrules == true) {
                            m_parsedToken_.m_indirectIndex_ = 5;
                            top = doSetTop();
                            return doEndParseNextToken(TOKEN_RESET_,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        newstrength = Collator.TERTIARY;
                        break;
                    case 0x003B : // ';'
                        if (newstrength != TOKEN_UNSET_) {
                            return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        // if we start with strength, we'll reset to top
                        if (startofrules == true) {
                            m_parsedToken_.m_indirectIndex_ = 5;
                            top = doSetTop();
                            return doEndParseNextToken(TOKEN_RESET_,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        newstrength = Collator.SECONDARY;
                        break;
                    case 0x003C : // '<'
                        if (newstrength != TOKEN_UNSET_) {
                            return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        // if we start with strength, we'll reset to top
                        if (startofrules == true) {
                            m_parsedToken_.m_indirectIndex_ = 5;
                            top = doSetTop();
                            return doEndParseNextToken(TOKEN_RESET_,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        // before this, do a scan to verify whether this is
                        // another strength
                        if (m_source_.charAt(m_current_ + 1) == 0x003C) {
                            m_current_ ++;
                            if (m_source_.charAt(m_current_ + 1) == 0x003C) {
                                m_current_ ++; // three in a row!
                                newstrength = Collator.TERTIARY;
                            }
                            else { // two in a row
                                newstrength = Collator.SECONDARY;
                            }
                        }
                        else { // just one
                            newstrength = Collator.PRIMARY;
                        }
                        break;
                    case 0x0026 : // '&'
                        if (newstrength != TOKEN_UNSET_) {
                            return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                        }
                        newstrength = TOKEN_RESET_; // PatternEntry::RESET = 0
                        break;
                    case 0x005b : // '['
                        // options - read an option, analyze it
                        m_optionEnd_ = m_rules_.indexOf(0x005d, m_current_);
                        if (m_optionEnd_ != -1) { // ']'
                            byte result = readAndSetOption();
                            m_current_ = m_optionEnd_;
                            if ((result & TOKEN_TOP_MASK_) != 0) {
                                if (newstrength == TOKEN_RESET_) {
                                    top = doSetTop();
                                    if (before != 0) {
                                        // This is a combination of before and
                                        // indirection like
                                        // '&[before 2][first regular]<b'
                                        m_source_.append((char)0x002d);
                                        m_source_.append((char)before);
                                        m_extraCurrent_ += 2;
                                        m_parsedToken_.m_charsLen_ += 2;
                                    }
                                    m_current_ ++;
                                    return doEndParseNextToken(newstrength,
                                                       true,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                                }
                                else {
                                    throwParseException(m_rules_, m_current_);
                                }
                            }
                            else if ((result & TOKEN_VARIABLE_TOP_MASK_) != 0) {
                                if (newstrength != TOKEN_RESET_
                                    && newstrength != TOKEN_UNSET_) {
                                    variabletop = true;
                                    m_parsedToken_.m_charsOffset_
                                                             = m_extraCurrent_;
                                    m_source_.append((char)0xFFFF);
                                    m_extraCurrent_ ++;
                                    m_current_ ++;
                                    m_parsedToken_.m_charsLen_ = 1;
                                    return doEndParseNextToken(newstrength,
                                                       top,
                                                       extensionoffset,
                                                       newextensionlen,
                                                       variabletop, before);
                                }
                                else {
                                    throwParseException(m_rules_, m_current_);
                                }
                            }
                            else if ((result & TOKEN_BEFORE_) != 0){
                                if (newstrength == TOKEN_RESET_) {
                                    before = (byte)(result & TOKEN_BEFORE_);
                                }
                                else {
                                    throwParseException(m_rules_, m_current_);
                                }
                            }
                        }
                        break;
                    case 0x002F : // '/'
                        wasinquote = false; // if we were copying source
                                            // characters, we want to stop now
                        inchars = false; // we're now processing expansion
                        break;
                    case 0x005C : // back slash for escaped chars
                        isescaped = true;
                        break;
                    // found a quote, we're gonna start copying
                    case 0x0027 : //'\''
                        if (newstrength == TOKEN_UNSET_) {
                            // quote is illegal until we have a strength
                            throwParseException(m_rules_, m_current_);
                        }
                        inquote = true;
                        if (inchars) { // we're doing characters
                            if (wasinquote == false) {
                                m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                            }
                            if (m_parsedToken_.m_charsLen_ != 0) {
                                m_source_.append(m_source_.substring(
                                       m_current_ - m_parsedToken_.m_charsLen_,
                                       m_current_));
                                m_extraCurrent_ += m_parsedToken_.m_charsLen_;
                            }
                            m_parsedToken_.m_charsLen_ ++;
                        }
                        else { // we're doing an expansion
                            if (wasinquote == false) {
                                extensionoffset = m_extraCurrent_;
                            }
                            if (newextensionlen != 0) {
                                m_source_.append(m_source_.substring(
                                                   m_current_ - newextensionlen,
                                                   m_current_));
                                m_extraCurrent_ += newextensionlen;
                            }
                            newextensionlen ++;
                        }
                        wasinquote = true;
                        m_current_ ++;
                        ch = m_source_.charAt(m_current_);
                        if (ch == 0x0027) { // copy the double quote
                            m_source_.append(ch);
                            m_extraCurrent_ ++;
                            inquote = false;
                        }
                        break;
                    // '@' is french only if the strength is not currently set
                    // if it is, it's just a regular character in collation
                    case 0x0040 : // '@'
                        if (newstrength == TOKEN_UNSET_) {
                            m_options_.m_isFrenchCollation_ = true;
                            break;
                        }
                        // fall through
                    case 0x007C : //|
                        // this means we have actually been reading prefix part
                        // we want to store read characters to the prefix part
                        // and continue reading the characters (proper way
                        // would be to restart reading the chars, but in that
                        // case we would have to complicate the token hasher,
                        // which I do not intend to play with. Instead, we will
                        // do prefixes when prefixes are due (before adding the
                        // elements).
                        m_parsedToken_.m_prefixOffset_
                                                = m_parsedToken_.m_charsOffset_;
                        m_parsedToken_.m_prefixLen_
                                                = m_parsedToken_.m_charsLen_;
                        if (inchars) { // we're doing characters
                            if (wasinquote == false) {
                                m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
                            }
                            if (m_parsedToken_.m_charsLen_ != 0) {
                                String prefix = m_source_.substring(
                                       m_current_ - m_parsedToken_.m_charsLen_,
                                       m_current_);
                                m_source_.append(prefix);
                                m_extraCurrent_ += m_parsedToken_.m_charsLen_;
                            }
                            m_parsedToken_.m_charsLen_ ++;
                        }
                        wasinquote = true;
                        do {
                            m_current_ ++;
                            ch = m_source_.charAt(m_current_);
                            // skip whitespace between '|' and the character
                        } while (UCharacterProperty.isRuleWhiteSpace(ch));
                        break;
                    case 0x0023: // '#' // this is a comment, skip everything through the end of line
                        do {
                            m_current_ ++;
                            ch = m_source_.charAt(m_current_);
                        } while (!isCharNewLine(ch));
                        break;
                    case 0x0021: // '!' // ignoring java set thai reordering
                        break;
                    default :
                        if (newstrength == TOKEN_UNSET_) {
                            throwParseException(m_rules_, m_current_);
                        }
                        if (isSpecialChar(ch) && (inquote == false)) {
                            throwParseException(m_rules_, m_current_);
                        }
                        if (ch == 0x0000 && m_current_ + 1 == limit) {
                            break;
                        }
                        if (inchars) {
                            if (m_parsedToken_.m_charsLen_ == 0) {
                                m_parsedToken_.m_charsOffset_ = m_current_;
                            }
                            m_parsedToken_.m_charsLen_++;
                        }
                        else {
                            if (newextensionlen == 0) {
                                extensionoffset = m_current_;
                            }
                            newextensionlen ++;
                        }
                        break;
                    }
                }
            }
            if (wasinquote) {
                if (ch != 0x27) {
                      m_source_.append(ch);
                    m_extraCurrent_ ++;
                }
            }
            m_current_ ++;
        }
        return doEndParseNextToken(newstrength, top,
                                   extensionoffset, newextensionlen,
                                   variabletop, before);
    }

    /**
     * End the next parse token
     * @param newstrength new strength
     * @return offset in rules, -1 for end of rules
     */
    private int doEndParseNextToken(int newstrength, /*int newcharslen,*/
                                    boolean top, /*int charsoffset,*/
                                    int extensionoffset, int newextensionlen,
                                    boolean variabletop, int before)
                                    throws ParseException
    {
        if (newstrength == TOKEN_UNSET_) {
            return -1;
        }
        if (m_parsedToken_.m_charsLen_ == 0 && top == false) {
            throwParseException(m_rules_, m_current_);
        }

        m_parsedToken_.m_strength_ = newstrength;
        //m_parsedToken_.m_charsOffset_ = charsoffset;
        //m_parsedToken_.m_charsLen_ = newcharslen;
        m_parsedToken_.m_extensionOffset_ = extensionoffset;
        m_parsedToken_.m_extensionLen_ = newextensionlen;
        m_parsedToken_.m_flags_ = (char)
                                  ((variabletop ? TOKEN_VARIABLE_TOP_MASK_ : 0)
                                  | (top ? TOKEN_TOP_MASK_ : 0) | before);
        return m_current_;
    }

    /**
     * Token before this element
     * @param sourcetoken
     * @param strength collation strength
     * @return the token before source token
     * @exception ParseException thrown when rules have the wrong syntax
     */
    private Token getVirginBefore(Token sourcetoken, int strength)
                                                          throws ParseException
    {
        // this is a virgin before - we need to fish the anchor from the UCA
        if (sourcetoken != null) {
            int offset = sourcetoken.m_source_ & 0xFFFFFF;
            m_UCAColEIter_.setText(m_source_.substring(offset, offset + 1));
        }
        else {
            m_UCAColEIter_.setText(
                             m_source_.substring(m_parsedToken_.m_charsOffset_,
                             m_parsedToken_.m_charsOffset_ + 1));
        }

        int basece = m_UCAColEIter_.next() & 0xFFFFFF3F;
        int basecontce = m_UCAColEIter_.next();
        if (basecontce == CollationElementIterator.NULLORDER) {
            basecontce = 0;
        }

        int ch = 0;


        if((basece >>> 24 >= RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MIN_)
                && (basece >>> 24 <=  RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MAX_)) { /* implicits - */

            int primary = basece & RuleBasedCollator.CE_PRIMARY_MASK_ | (basecontce & RuleBasedCollator.CE_PRIMARY_MASK_) >> 16;
            int raw = RuleBasedCollator.impCEGen_.getRawFromImplicit(primary);
            ch = RuleBasedCollator.impCEGen_.getCodePointFromRaw(raw-1);
            int primaryCE = RuleBasedCollator.impCEGen_.getImplicitFromRaw(raw-1);
            m_utilCEBuffer_[0] = primaryCE & RuleBasedCollator.CE_PRIMARY_MASK_ | 0x0505;
            m_utilCEBuffer_[1] = (primaryCE << 16) & RuleBasedCollator.CE_PRIMARY_MASK_ | RuleBasedCollator.CE_CONTINUATION_MARKER_;

            m_parsedToken_.m_charsOffset_ = m_extraCurrent_;
            m_source_.append('\uFFFE');
            m_source_.append((char)ch);
            m_extraCurrent_ += 2;
            m_parsedToken_.m_charsLen_++;

            m_utilToken_.m_source_ = (m_parsedToken_.m_charsLen_ << 24)
            | m_parsedToken_.m_charsOffset_;
            m_utilToken_.m_rules_ = m_source_;
            sourcetoken = m_hashTable_.get(m_utilToken_);

            if(sourcetoken == null) {
                m_listHeader_[m_resultLength_] = new TokenListHeader();
                m_listHeader_[m_resultLength_].m_baseCE_
                    = m_utilCEBuffer_[0] & 0xFFFFFF3F;
                if (RuleBasedCollator.isContinuation(m_utilCEBuffer_[1])) {
                    m_listHeader_[m_resultLength_].m_baseContCE_
                    = m_utilCEBuffer_[1];
                }
                else {
                    m_listHeader_[m_resultLength_].m_baseContCE_ = 0;
                }
                m_listHeader_[m_resultLength_].m_nextCE_ = 0;
                m_listHeader_[m_resultLength_].m_nextContCE_ = 0;
                m_listHeader_[m_resultLength_].m_previousCE_ = 0;
                m_listHeader_[m_resultLength_].m_previousContCE_ = 0;
                m_listHeader_[m_resultLength_].m_indirect_ = false;

                sourcetoken = new Token();
                initAReset(-1, sourcetoken);
            }

        } else {

            // first ce and second ce m_utilCEBuffer_
            /*int invpos = */CollationParsedRuleBuilder.INVERSE_UCA_.getInversePrevCE(
                                                         basece, basecontce,
                                                         strength, m_utilCEBuffer_);
            // we got the previous CE. Now we need to see if the difference between
            // the two CEs is really of the requested strength.
            // if it's a bigger difference (we asked for secondary and got primary), we
            // need to modify the CE.
            if(CollationParsedRuleBuilder.INVERSE_UCA_.getCEStrengthDifference(basece, basecontce, m_utilCEBuffer_[0], m_utilCEBuffer_[1]) < strength) {
                // adjust the strength
                // now we are in the situation where our baseCE should actually be modified in
                // order to get the CE in the right position.
                if(strength == Collator.SECONDARY) {
                    m_utilCEBuffer_[0] = basece - 0x0200;
                } else { // strength == UCOL_TERTIARY
                    m_utilCEBuffer_[0] = basece - 0x02;
                }
                if(RuleBasedCollator.isContinuation(basecontce)) {
                    if(strength == Collator.SECONDARY) {
                        m_utilCEBuffer_[1] = basecontce - 0x0200;
                    } else { // strength == UCOL_TERTIARY
                        m_utilCEBuffer_[1] = basecontce - 0x02;
                    }
                }
            }

/*
            // the code below relies on getting a code point from the inverse table, in order to be
            // able to merge the situations like &x < 9 &[before 1]a < d. This won't work:
            // 1. There are many code points that have the same CE
            // 2. The CE to codepoint table (things pointed to by CETable[3*invPos+2] are broken.
            // Also, in case when there is no equivalent strength before an element, we have to actually
            // construct one. For example, &[before 2]a << x won't result in x << a, because the element
            // before a is a primary difference.
            ch = CollationParsedRuleBuilder.INVERSE_UCA_.m_table_[3 * invpos
                                                                      + 2];
            if ((ch &  INVERSE_SIZE_MASK_) != 0) {
                int offset = ch & INVERSE_OFFSET_MASK_;
                ch = CollationParsedRuleBuilder.INVERSE_UCA_.m_continuations_[
                                                                           offset];
            }
            m_source_.append((char)ch);
            m_extraCurrent_ ++;
            m_parsedToken_.m_charsOffset_ = m_extraCurrent_ - 1;
            m_parsedToken_.m_charsLen_ = 1;

            // We got an UCA before. However, this might have been tailored.
            // example:
            // &\u30ca = \u306a
            // &[before 3]\u306a<<<\u306a|\u309d

            m_utilToken_.m_source_ = (m_parsedToken_.m_charsLen_ << 24)
                                                 | m_parsedToken_.m_charsOffset_;
            m_utilToken_.m_rules_ = m_source_;
            sourcetoken = (Token)m_hashTable_.get(m_utilToken_);
*/

            // here is how it should be. The situation such as &[before 1]a < x, should be
            // resolved exactly as if we wrote &a > x.
            // therefore, I don't really care if the UCA value before a has been changed.
            // However, I do care if the strength between my element and the previous element
            // is bigger then I wanted. So, if CE < baseCE and I wanted &[before 2], then i'll
            // have to construct the base CE.

            // if we found a tailored thing, we have to use the UCA value and
            // construct a new reset token with constructed name
            //if (sourcetoken != null && sourcetoken.m_strength_ != TOKEN_RESET_) {
                // character to which we want to anchor is already tailored.
                // We need to construct a new token which will be the anchor point
                //m_source_.setCharAt(m_extraCurrent_ - 1, '\uFFFE');
                //m_source_.append(ch);
                //m_extraCurrent_ ++;
                //m_parsedToken_.m_charsLen_ ++;
                // grab before
                m_parsedToken_.m_charsOffset_ -= 10;
                m_parsedToken_.m_charsLen_ += 10;
                m_listHeader_[m_resultLength_] = new TokenListHeader();
                m_listHeader_[m_resultLength_].m_baseCE_
                                                 = m_utilCEBuffer_[0] & 0xFFFFFF3F;
                if (RuleBasedCollator.isContinuation(m_utilCEBuffer_[1])) {
                    m_listHeader_[m_resultLength_].m_baseContCE_
                                                              = m_utilCEBuffer_[1];
                }
                else {
                    m_listHeader_[m_resultLength_].m_baseContCE_ = 0;
                }
                m_listHeader_[m_resultLength_].m_nextCE_ = 0;
                m_listHeader_[m_resultLength_].m_nextContCE_ = 0;
                m_listHeader_[m_resultLength_].m_previousCE_ = 0;
                m_listHeader_[m_resultLength_].m_previousContCE_ = 0;
                m_listHeader_[m_resultLength_].m_indirect_ = false;
                sourcetoken = new Token();
                initAReset(-1, sourcetoken);
            //}
        }
        return sourcetoken;
    }

    /**
     * Processing Description.
     * 1. Build a m_listHeader_. Each list has a header, which contains two lists
     * (positive and negative), a reset token, a baseCE, nextCE, and
     * previousCE. The lists and reset may be null.
     * 2. As you process, you keep a LAST pointer that points to the last token
     * you handled.
     * @param expand string offset, -1 for null strings
     * @param targetToken token to update
     * @return expandnext offset
     * @throws ParseException thrown when rules syntax failed
     */
    private int initAReset(int expand, Token targetToken) throws ParseException
    {
        if (m_resultLength_ == m_listHeader_.length - 1) {
            // Unfortunately, this won't work, as we store addresses of lhs in
            // token
            TokenListHeader temp[] = new TokenListHeader[m_resultLength_ << 1];
            System.arraycopy(m_listHeader_, 0, temp, 0, m_resultLength_ + 1);
            m_listHeader_ = temp;
        }
        // do the reset thing
        targetToken.m_rules_ = m_source_;
        targetToken.m_source_ = m_parsedToken_.m_charsLen_ << 24
                                | m_parsedToken_.m_charsOffset_;
        targetToken.m_expansion_ = m_parsedToken_.m_extensionLen_ << 24
                                   | m_parsedToken_.m_extensionOffset_;
        // keep the flags around so that we know about before
        targetToken.m_flags_ = m_parsedToken_.m_flags_;

        if (m_parsedToken_.m_prefixOffset_ != 0) {
            throwParseException(m_rules_, m_parsedToken_.m_charsOffset_ - 1);
        }

        targetToken.m_prefix_ = 0;
        // TODO: this should also handle reverse
        targetToken.m_polarity_ = TOKEN_POLARITY_POSITIVE_;
        targetToken.m_strength_ = TOKEN_RESET_;
        targetToken.m_next_ = null;
        targetToken.m_previous_ = null;
        targetToken.m_CELength_ = 0;
        targetToken.m_expCELength_ = 0;
        targetToken.m_listHeader_ = m_listHeader_[m_resultLength_];
        m_listHeader_[m_resultLength_].m_first_ = null;
        m_listHeader_[m_resultLength_].m_last_ = null;
        m_listHeader_[m_resultLength_].m_first_ = null;
        m_listHeader_[m_resultLength_].m_last_ = null;
        m_listHeader_[m_resultLength_].m_reset_ = targetToken;

        /* 3 Consider each item: relation, source, and expansion:
         * e.g. ...< x / y ...
         * First convert all expansions into normal form. Examples:
         * If "xy" doesn't occur earlier in the list or in the UCA, convert
         * &xy * c * d * ... into &x * c/y * d * ...
         * Note: reset values can never have expansions, although they can
         * cause the very next item to have one. They may be contractions, if
         * they are found earlier in the list.
         */
        int result = 0;
        if (expand > 0) {
            // check to see if there is an expansion
            if (m_parsedToken_.m_charsLen_ > 1) {
                targetToken.m_source_ = ((expand
                                          - m_parsedToken_.m_charsOffset_ )
                                          << 24)
                                          | m_parsedToken_.m_charsOffset_;
                result = ((m_parsedToken_.m_charsLen_
                               + m_parsedToken_.m_charsOffset_ - expand) << 24)
                               | expand;
            }
        }

        m_resultLength_ ++;
        m_hashTable_.put(targetToken, targetToken);
        return result;
    }

    /**
     * Checks if an character is special
     * @param ch character to test
     * @return true if the character is special
     */
    private static final boolean isSpecialChar(char ch)
    {
        return (ch <= 0x002F && ch >= 0x0020) || (ch <= 0x003F && ch >= 0x003A)
               || (ch <= 0x0060 && ch >= 0x005B)
               || (ch <= 0x007E && ch >= 0x007D) || ch == 0x007B;
    }

    private
    UnicodeSet readAndSetUnicodeSet(String source, int start) throws ParseException
    {
      while(source.charAt(start) != '[') { /* advance while we find the first '[' */
        start++;
      }
      // now we need to get a balanced set of '[]'. The problem is that a set can have
      // many, and *end point to the first closing '['
      int noOpenBraces = 1;
      int current = 1; // skip the opening brace
      while(start+current < source.length() && noOpenBraces != 0) {
        if(source.charAt(start+current) == '[') {
          noOpenBraces++;
        } else if(source.charAt(start+current) == ']') { // closing brace
          noOpenBraces--;
        }
        current++;
      }
      //int nextBrace = -1;

      if(noOpenBraces != 0 || (/*nextBrace =*/ source.indexOf("]", start+current) /*']'*/) == -1) {
        throwParseException(m_rules_, start);
      }
      return new UnicodeSet(source.substring(start, start+current)); //uset_openPattern(start, current);
    }


    /** in C, optionarg is passed by reference to function.
     *  We use a private int to simulate this.
     */
    private int m_optionarg_ = 0;

    private int readOption(String rules, int start, int optionend)
    {
        m_optionarg_ = 0;
        int i = 0;
        while (i < RULES_OPTIONS_.length) {
            String option = RULES_OPTIONS_[i].m_name_;
            int optionlength = option.length();
            if (rules.length() > start + optionlength
                && option.equalsIgnoreCase(rules.substring(start,
                                                      start + optionlength))) {
                if (optionend - start > optionlength) {
                    m_optionarg_ = start + optionlength;
                    // start of the options, skip space
                    while (m_optionarg_ < optionend && (UCharacter.isWhitespace(rules.charAt(m_optionarg_)) || UCharacterProperty.isRuleWhiteSpace(rules.charAt(m_optionarg_))))
                    {   // eat whitespace
                        m_optionarg_ ++;
                    }
                }
                break;
            }
            i ++;
        }
        if(i == RULES_OPTIONS_.length) {
            i = -1;
        }
        return i;
    }
    /**
     * Reads and set collation options
     * @return TOKEN_SUCCESS if option is set correct, 0 otherwise
     * @exception ParseException thrown when options in rules are wrong
     */
    private byte readAndSetOption() throws ParseException
    {
        int start = m_current_ + 1; // skip opening '['
        int i = readOption(m_rules_, start, m_optionEnd_);

        int optionarg = m_optionarg_;

        if (i < 0) {
            throwParseException(m_rules_, start);
        }

        if (i < 7) {
            if (optionarg != 0) {
                for (int j = 0; j < RULES_OPTIONS_[i].m_subOptions_.length;
                                                                        j ++) {
                     String subname = RULES_OPTIONS_[i].m_subOptions_[j];
                     int size = optionarg + subname.length();
                     if (m_rules_.length() > size
                         && subname.equalsIgnoreCase(m_rules_.substring(
                                                           optionarg, size))) {
                         setOptions(m_options_, RULES_OPTIONS_[i].m_attribute_,
                             RULES_OPTIONS_[i].m_subOptionAttributeValues_[j]);
                         return TOKEN_SUCCESS_MASK_;
                     }
                }
            }
            throwParseException(m_rules_, optionarg);
        }
        else if (i == 7) { // variable top
            return TOKEN_SUCCESS_MASK_ | TOKEN_VARIABLE_TOP_MASK_;
        }
        else if (i == 8) { // rearange
            return TOKEN_SUCCESS_MASK_;
        }
        else if (i == 9) { // before
            if (optionarg != 0) {
                for (int j = 0; j < RULES_OPTIONS_[i].m_subOptions_.length;
                                                                        j ++) {
                     String subname = RULES_OPTIONS_[i].m_subOptions_[j];
                     int size = optionarg + subname.length();
                     if (m_rules_.length() > size
                         && subname.equalsIgnoreCase(
                                               m_rules_.substring(optionarg,
                                              optionarg + subname.length()))) {
                         return (byte)(TOKEN_SUCCESS_MASK_
                            | RULES_OPTIONS_[i].m_subOptionAttributeValues_[j]
                            + 1);
                     }
                }
            }
            throwParseException(m_rules_, optionarg);
        }
        else if (i == 10) {  // top, we are going to have an array with
            // structures of limit CEs index to this array will be
            // src->parsedToken.indirectIndex
            m_parsedToken_.m_indirectIndex_ = 0;
            return TOKEN_SUCCESS_MASK_ | TOKEN_TOP_MASK_;
        }
        else if (i < 13) { // first, last
            for (int j = 0; j < RULES_OPTIONS_[i].m_subOptions_.length; j ++) {
                String subname = RULES_OPTIONS_[i].m_subOptions_[j];
                int size = optionarg + subname.length();
                if (m_rules_.length() > size
                    && subname.equalsIgnoreCase(m_rules_.substring(optionarg,
                                                                   size))) {
                    m_parsedToken_.m_indirectIndex_ = (char)(i - 10 + (j << 1));
                    return TOKEN_SUCCESS_MASK_ | TOKEN_TOP_MASK_;
                }
            }
            throwParseException(m_rules_, optionarg);
        }
        else if(i == 13 || i == 14) { // copy and remove are handled before normalization
            // we need to move end here
            int noOpenBraces = 1;
            m_current_++; // skip opening brace
            while(m_current_ < m_source_.length() && noOpenBraces != 0) {
                if(m_source_.charAt(m_current_) == '[') {
                  noOpenBraces++;
                } else if(m_source_.charAt(m_current_) == ']') { // closing brace
                  noOpenBraces--;
                }
                m_current_++;
            }
            m_optionEnd_ = m_current_-1;
            return TOKEN_SUCCESS_MASK_;
        }
        else {
            throwParseException(m_rules_, optionarg);
        }
        return TOKEN_SUCCESS_MASK_; // we will never reach here.
    }

    /**
     * Set collation option
     * @param optionset option set to set
     * @param attribute type to set
     * @param value attribute value
     */
    private void setOptions(OptionSet optionset, int attribute, int value)
    {
        switch (attribute) {
            case RuleBasedCollator.Attribute.HIRAGANA_QUATERNARY_MODE_ :
                optionset.m_isHiragana4_
                            = (value == RuleBasedCollator.AttributeValue.ON_);
                break;
            case RuleBasedCollator.Attribute.FRENCH_COLLATION_ :
                optionset.m_isFrenchCollation_
                             = (value == RuleBasedCollator.AttributeValue.ON_);
                break;
            case RuleBasedCollator.Attribute.ALTERNATE_HANDLING_ :
                optionset.m_isAlternateHandlingShifted_
                             = (value
                                == RuleBasedCollator.AttributeValue.SHIFTED_);
                break;
            case RuleBasedCollator.Attribute.CASE_FIRST_ :
                optionset.m_caseFirst_ = value;
                break;
            case RuleBasedCollator.Attribute.CASE_LEVEL_ :
                optionset.m_isCaseLevel_
                             = (value == RuleBasedCollator.AttributeValue.ON_);
                break;
            case RuleBasedCollator.Attribute.NORMALIZATION_MODE_ :
                if (value == RuleBasedCollator.AttributeValue.ON_) {
                    value = Collator.CANONICAL_DECOMPOSITION;
                }
                optionset.m_decomposition_ = value;
                break;
            case RuleBasedCollator.Attribute.STRENGTH_ :
                optionset.m_strength_ = value;
                break;
            default :
                break;
        }
      }

    UnicodeSet getTailoredSet() throws ParseException
    {
        boolean startOfRules = true;
        UnicodeSet tailored = new UnicodeSet();
        String pattern;
        CanonicalIterator it = new CanonicalIterator("");

        m_parsedToken_.m_strength_ = TOKEN_UNSET_;
        int sourcelimit = m_source_.length();
        //int expandNext = 0;

        while (m_current_ < sourcelimit) {
        m_parsedToken_.m_prefixOffset_ = 0;
        if (parseNextToken(startOfRules) < 0) {
            // we have reached the end
            continue;
        }
        startOfRules = false;
        // The idea is to tokenize the rule set. For each non-reset token,
        // we add all the canonicaly equivalent FCD sequences
            if(m_parsedToken_.m_strength_ != TOKEN_RESET_) {
                it.setSource(m_source_.substring(
                      m_parsedToken_.m_charsOffset_,
                      m_parsedToken_.m_charsOffset_+m_parsedToken_.m_charsLen_));
                pattern = it.next();
                while(pattern != null) {
                      if(Normalizer.quickCheck(pattern, Normalizer.FCD,0) != Normalizer.NO) {
                        tailored.add(pattern);
                    }
                    pattern = it.next();
                }
            }
        }
        return tailored;
    }

    final private void extractSetsFromRules(String rules) throws ParseException {
      int optionNumber = -1;
      int setStart = 0;
      int i = 0;
      while(i < rules.length()) {
        if(rules.charAt(i) == 0x005B) {
          optionNumber = readOption(rules, i+1, rules.length());
          setStart = m_optionarg_;
          if(optionNumber == 13) { /* copy - parts of UCA to tailoring */
            UnicodeSet newSet = readAndSetUnicodeSet(rules, setStart);
              if(m_copySet_ == null) {
                m_copySet_ = newSet;
              } else {
                m_copySet_.addAll(newSet);
              }
          } else if(optionNumber == 14) {
            UnicodeSet newSet = readAndSetUnicodeSet(rules, setStart);
              if(m_removeSet_ == null) {
                m_removeSet_ = newSet;
              } else {
                m_removeSet_.addAll(newSet);
              }
          }
        }
        i++;
      }
    }
}
