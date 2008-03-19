/**
*******************************************************************************
* Copyright (C) 1996-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;
 
import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Arrays;
import java.util.Enumeration;

import com.ibm.icu.impl.TrieBuilder;
import com.ibm.icu.impl.IntTrieBuilder;
import com.ibm.icu.impl.TrieIterator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.VersionInfo;

/**
* Class for building a collator from a list of collation rules.
* This class is uses CollationRuleParser
* @author Syn Wee Quek
* @since release 2.2, June 11 2002
* @draft 2.2
*/
final class CollationParsedRuleBuilder
{     
    // package private constructors ------------------------------------------

    /**
     * Constructor
     * @param rules collation rules
     * @exception ParseException thrown when argument rules have an invalid 
     *            syntax 
     */
    CollationParsedRuleBuilder(String rules) throws ParseException
    {
        m_parser_ = new CollationRuleParser(rules);
        m_parser_.assembleTokenList();
        m_utilColEIter_ = RuleBasedCollator.UCA_.getCollationElementIterator(
                                         "");
    }
    
    // package private inner classes -----------------------------------------
    
    /** 
     * Inverse UCA wrapper
     */
    static class InverseUCA 
    {
        // package private constructor ---------------------------------------
        
        InverseUCA() 
        {
        }
        
        // package private data member ---------------------------------------
        
        /**
         * Array list of characters
         */
        int m_table_[];
        /**
         * Array list of continuation characters
         */
        char m_continuations_[];
        
        /**
         * UCA version of inverse UCA table
         */
        VersionInfo m_UCA_version_;
        
        // package private method --------------------------------------------
        
        /**
     * Returns the previous inverse ces of the argument ces
     * @param ce ce to test
     * @param contce continuation ce to test
     * @param strength collation strength
     * @param prevresult an array to store the return results previous 
         *                   inverse ce and previous inverse continuation ce
         * @return result of the inverse ce 
     */
    final int getInversePrevCE(int ce, int contce, int strength, 
                   int prevresult[]) 
    {
        int result = findInverseCE(ce, contce);
        
        if (result < 0) {
        prevresult[0] = CollationElementIterator.NULLORDER;
        return -1;
        }
        
        ce &= STRENGTH_MASK_[strength];
        contce &= STRENGTH_MASK_[strength];
        
            prevresult[0] = ce;
        prevresult[1] = contce;
        
        while ((prevresult[0]  & STRENGTH_MASK_[strength]) == ce 
           && (prevresult[1]  & STRENGTH_MASK_[strength])== contce
           && result > 0) { 
                        // this condition should prevent falling off the edge of the 
                        // world 
                // here, we end up in a singularity - zero
                prevresult[0] = m_table_[3 * (-- result)];
                prevresult[1] = m_table_[3 * result + 1];
           }
           return result;
        }
        
        final int getCEStrengthDifference(int CE, int contCE, 
                int prevCE, int prevContCE) {
            int strength = Collator.TERTIARY;
            while(
            ((prevCE & STRENGTH_MASK_[strength]) != (CE & STRENGTH_MASK_[strength]) 
            || (prevContCE & STRENGTH_MASK_[strength]) != (contCE & STRENGTH_MASK_[strength]))
            && (strength != 0)) {
                strength--;
            }
            return strength;                
        }

        private int compareCEs(int source0, int source1, int target0, int target1) {
            int s1 = source0, s2, t1 = target0, t2;
            if(RuleBasedCollator.isContinuation(source1)) {
                s2 = source1;
            } else {
                s2 = 0;
            }
            if(RuleBasedCollator.isContinuation(target1)) {
                t2 = target1;
            } else {
                t2 = 0;
            }
            
            int s = 0, t = 0;
            if(s1 == t1 && s2 == t2) {
                return 0;
            }
            s = (s1 & 0xFFFF0000)|((s2 & 0xFFFF0000)>>16); 
            t = (t1 & 0xFFFF0000)|((t2 & 0xFFFF0000)>>16);
            if(s == t) {
                s = (s1 & 0x0000FF00) | (s2 & 0x0000FF00)>>8;
                t = (t1 & 0x0000FF00) | (t2 & 0x0000FF00)>>8;
                if(s == t) {
                    s = (s1 & 0x000000FF)<<8 | (s2 & 0x000000FF);
                    t = (t1 & 0x000000FF)<<8 | (t2 & 0x000000FF);
                    return Utility.compareUnsigned(s, t);
                } else { 
                    return Utility.compareUnsigned(s, t);
                }
            } else {
                return Utility.compareUnsigned(s, t);                
            }
        }
        
        /**
         * Finding the inverse CE of the argument CEs
         * @param ce CE to be tested
         * @param contce continuation CE
         * @return inverse CE
         */
        int findInverseCE(int ce, int contce) 
        {
            int bottom = 0;
            int top = m_table_.length / 3;
            int result = 0;
            
            while (bottom < top - 1) {
                result = (top + bottom) >> 1;
                int first = m_table_[3 * result];
                int second = m_table_[3 * result + 1];
                int comparison = compareCEs(first, second, ce, contce);
                if (comparison > 0) {
                    top = result;
                } 
                else if (comparison < 0) {
                    bottom = result;
                } 
                else { 
                    break;
                }
            }
            
            return result;
        }
    
    /**
     * Getting gap offsets in the inverse UCA
     * @param listheader parsed token lists
     * @exception Exception thrown when error occurs while finding the 
     *            collation gaps
     */
    void getInverseGapPositions(CollationRuleParser.TokenListHeader 
                    listheader)
        throws Exception 
    {
        // reset all the gaps
        CollationRuleParser.Token token = listheader.m_first_;
        int tokenstrength = token.m_strength_;
    
        for (int i = 0; i < 3; i ++) {
        listheader.m_gapsHi_[3 * i] = 0;
        listheader.m_gapsHi_[3 * i + 1] = 0;
        listheader.m_gapsHi_[3 * i + 2] = 0;
        listheader.m_gapsLo_[3 * i] = 0;
        listheader.m_gapsLo_[3 * i + 1] = 0;
        listheader.m_gapsLo_[3 * i + 2] = 0;
        listheader.m_numStr_[i] = 0;
        listheader.m_fStrToken_[i] = null;
        listheader.m_lStrToken_[i] = null;
        listheader.m_pos_[i] = -1;
        }
    
        if ((listheader.m_baseCE_ >>> 24) 
                >= RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MIN_
        && (listheader.m_baseCE_ >>> 24)
                <= RuleBasedCollator.UCA_CONSTANTS_.PRIMARY_IMPLICIT_MAX_) 
        { 
                // implicits -
            listheader.m_pos_[0] = 0;
            int t1 = listheader.m_baseCE_;
            int t2 = listheader.m_baseContCE_;
            listheader.m_gapsLo_[0] = mergeCE(t1, t2, 
                                                  Collator.PRIMARY);
            listheader.m_gapsLo_[1] = mergeCE(t1, t2, 
                                                  Collator.SECONDARY);
            listheader.m_gapsLo_[2] = mergeCE(t1, t2, 
                                                  Collator.TERTIARY);
            int primaryCE = t1 & RuleBasedCollator.CE_PRIMARY_MASK_ | (t2 & RuleBasedCollator.CE_PRIMARY_MASK_) >>> 16;
            primaryCE = RuleBasedCollator.impCEGen_.getImplicitFromRaw(RuleBasedCollator.impCEGen_.getRawFromImplicit(primaryCE)+1);

            t1 = primaryCE & RuleBasedCollator.CE_PRIMARY_MASK_ | 0x0505;
            t2 = (primaryCE << 16) & RuleBasedCollator.CE_PRIMARY_MASK_ | RuleBasedCollator.CE_CONTINUATION_MARKER_;
                
            //                if (listheader.m_baseCE_ < 0xEF000000) {
            //                    // first implicits have three byte primaries, with a gap of
            //                    // one so we esentially need to add 2 to the top byte in 
            //                    // listheader.m_baseContCE_
            //                    t2 += 0x02000000;
            //                } 
            //                else {
            //                    // second implicits have four byte primaries, with a gap of
            //                    // IMPLICIT_LAST2_MULTIPLIER_
            //                    // Now, this guy is not really accessible here, so until we 
            //                    // find a better way to pass it around, assume that the gap is 1
            //                    t2 += 0x00020000;
            //                }
            listheader.m_gapsHi_[0] = mergeCE(t1, t2, 
                                                  Collator.PRIMARY);
            listheader.m_gapsHi_[1] = mergeCE(t1, t2, 
                                                  Collator.SECONDARY);
            listheader.m_gapsHi_[2] = mergeCE(t1, t2, 
                                                  Collator.TERTIARY);
        } 
        else if (listheader.m_indirect_ == true 
                     && listheader.m_nextCE_ != 0) {
        listheader.m_pos_[0] = 0;
        int t1 = listheader.m_baseCE_;
        int t2 = listheader.m_baseContCE_;
        listheader.m_gapsLo_[0] = mergeCE(t1, t2, 
                          Collator.PRIMARY);
        listheader.m_gapsLo_[1] = mergeCE(t1, t2, 
                          Collator.SECONDARY);
        listheader.m_gapsLo_[2] = mergeCE(t1, t2, 
                          Collator.TERTIARY);
        t1 = listheader.m_nextCE_;
        t2 = listheader.m_nextContCE_;
        listheader.m_gapsHi_[0] = mergeCE(t1, t2, 
                          Collator.PRIMARY);
        listheader.m_gapsHi_[1] = mergeCE(t1, t2, 
                          Collator.SECONDARY);
        listheader.m_gapsHi_[2] = mergeCE(t1, t2, 
                          Collator.TERTIARY);
        } 
        else {
        while (true) {
            if (tokenstrength < CE_BASIC_STRENGTH_LIMIT_) {
            listheader.m_pos_[tokenstrength] 
                = getInverseNext(listheader, 
                         tokenstrength);
            if (listheader.m_pos_[tokenstrength] >= 0) {
                listheader.m_fStrToken_[tokenstrength] = token;
            } 
            else { 
                            // The CE must be implicit, since it's not in the 
                            // table 
                // Error
                throw new Exception("Internal program error");
            }
            }
            
            while (token != null && token.m_strength_ >= tokenstrength) 
            {
                if (tokenstrength < CE_BASIC_STRENGTH_LIMIT_) {
                listheader.m_lStrToken_[tokenstrength] = token;
                }
                token = token.m_next_;
            }
            if (tokenstrength < CE_BASIC_STRENGTH_LIMIT_ - 1) {
            // check if previous interval is the same and merge the 
            // intervals if it is so
            if (listheader.m_pos_[tokenstrength] 
                == listheader.m_pos_[tokenstrength + 1]) {
                listheader.m_fStrToken_[tokenstrength] 
                = listheader.m_fStrToken_[tokenstrength 
                             + 1];
                listheader.m_fStrToken_[tokenstrength + 1] = null;
                listheader.m_lStrToken_[tokenstrength + 1] = null;
                listheader.m_pos_[tokenstrength + 1] = -1;
            }
            }
            if (token != null) {
            tokenstrength = token.m_strength_;
            } 
            else {
            break;
            }
        }
        for (int st = 0; st < 3; st ++) {
            int pos = listheader.m_pos_[st];
            if (pos >= 0) {
            int t1 = m_table_[3 * pos];
            int t2 = m_table_[3 * pos + 1];
            listheader.m_gapsHi_[3 * st] = mergeCE(t1, t2, 
                                   Collator.PRIMARY);
            listheader.m_gapsHi_[3 * st + 1] = mergeCE(t1, t2, 
                                   Collator.SECONDARY);
            listheader.m_gapsHi_[3 * st + 2] = (t1 & 0x3f) << 24 
                | (t2 & 0x3f) << 16;
            //pos --;
            //t1 = m_table_[3 * pos];
            //t2 = m_table_[3 * pos + 1];
            t1 = listheader.m_baseCE_;
            t2 = listheader.m_baseContCE_;
            
            listheader.m_gapsLo_[3 * st] = mergeCE(t1, t2, 
                                   Collator.PRIMARY);
            listheader.m_gapsLo_[3 * st + 1] = mergeCE(t1, t2, 
                                   Collator.SECONDARY);
            listheader.m_gapsLo_[3 * st + 2] = (t1 & 0x3f) << 24 
                | (t2 & 0x3f) << 16;
            }
        }
        }
        }
        
    /**
     * Gets the next CE in the inverse table
     * @param listheader token list header
     * @param strength collation strength
     * @return next ce
     */
    private final int getInverseNext(CollationRuleParser.TokenListHeader 
                     listheader, 
                     int strength) 
    {
        int ce = listheader.m_baseCE_;
        int secondce = listheader.m_baseContCE_; 
        int result = findInverseCE(ce, secondce);
            
        if (result < 0) {
        return -1;
        }
            
        ce &= STRENGTH_MASK_[strength];
        secondce &= STRENGTH_MASK_[strength];
        
        int nextce = ce;
        int nextcontce = secondce;
        
        while((nextce & STRENGTH_MASK_[strength]) == ce 
          && (nextcontce  & STRENGTH_MASK_[strength]) == secondce) {
        nextce = m_table_[3 * (++ result)];
        nextcontce = m_table_[3 * result + 1];
        }
            
        listheader.m_nextCE_ = nextce;
        listheader.m_nextContCE_ = nextcontce;
        
        return result;
    }
    }

    // package private data members ------------------------------------------
    
    /**
     * Inverse UCA, instantiate only when required
     */
    static final InverseUCA INVERSE_UCA_; 
    
    /**
     * UCA and Inverse UCA version do not match
     */
    private static final String INV_UCA_VERSION_MISMATCH_ =
    "UCA versions of UCA and inverse UCA should match";
           
    /**
     * UCA and Inverse UCA version do not match
     */
    private static final String UCA_NOT_INSTANTIATED_ =
    "UCA is not instantiated!";
    
    /**
     * Initializing the inverse UCA
     */
    static {
        InverseUCA temp = null;
        try {
        temp = CollatorReader.getInverseUCA();
    } catch (IOException e) {
    }
        /*
      try
      {
      String invdat = "/com/ibm/icu/impl/data/invuca.icu";
      InputStream i = CollationParsedRuleBuilder.class.getResourceAsStream(invdat);
      BufferedInputStream b = new BufferedInputStream(i, 110000);
      INVERSE_UCA_ = CollatorReader.readInverseUCA(b);
      b.close();
      i.close();
      }
      catch (Exception e)
      {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
      }
        */
        
        if(temp != null && RuleBasedCollator.UCA_ != null) {
            if(!temp.m_UCA_version_.equals(RuleBasedCollator.UCA_.m_UCA_version_)) {
                throw new RuntimeException(INV_UCA_VERSION_MISMATCH_);
            }
        } else {
            throw new RuntimeException(UCA_NOT_INSTANTIATED_);
        }
        
        INVERSE_UCA_ = temp;
    }
    
    // package private methods -----------------------------------------------
    
    /**
     * Parse and sets the collation rules in the argument collator
     * @param collator to set
     * @exception Exception thrown when internal program error occurs
     */
    void setRules(RuleBasedCollator collator) throws Exception
    {
        if (m_parser_.m_resultLength_ > 0 || m_parser_.m_removeSet_ != null) { 
        // we have a set of rules, let's make something of it 
        assembleTailoringTable(collator);
    } 
    else { // no rules, but no error either must be only options
        // We will init the collator from UCA   
        collator.setWithUCATables();
    }
        // And set only the options
        m_parser_.setDefaultOptionsInCollator(collator);
    }
    
    private void copyRangeFromUCA(BuildTable t, int start, int end) {
        int u = 0;
        for (u = start; u <= end; u ++) {
            // if ((CE = ucmpe32_get(t.m_mapping, u)) == UCOL_NOT_FOUND
            int CE = t.m_mapping_.getValue(u);
            if (CE == CE_NOT_FOUND_ 
                // this test is for contractions that are missing the starting 
                // element. Looks like latin-1 should be done before 
                // assembling the table, even if it results in more false 
                // closure elements
                || (isContractionTableElement(CE) 
            && getCE(t.m_contractions_, CE, 0) == CE_NOT_FOUND_)) {
                //m_utilElement_.m_uchars_ = str.toString();
                m_utilElement_.m_uchars_ = UCharacter.toString(u);
                m_utilElement_.m_cPoints_ = m_utilElement_.m_uchars_;
                m_utilElement_.m_prefix_ = 0;
                m_utilElement_.m_CELength_ = 0;
                m_utilColEIter_.setText(m_utilElement_.m_uchars_);
                while (CE != CollationElementIterator.NULLORDER) {
                    CE = m_utilColEIter_.next();
                    if (CE != CollationElementIterator.NULLORDER) {
                        m_utilElement_.m_CEs_[m_utilElement_.m_CELength_ ++] 
                = CE;
                    }
                }
                addAnElement(t, m_utilElement_);
            }
        }
    }
            
    /**
     * 2.  Eliminate the negative lists by doing the following for each 
     * non-null negative list: 
     * o   if previousCE(baseCE, strongestN) != some ListHeader X's baseCE, 
     * create new ListHeader X 
     * o   reverse the list, add to the end of X's positive list. Reset the 
     * strength of the first item you add, based on the stronger strength 
     * levels of the two lists. 
     * 
     * 3.  For each ListHeader with a non-null positive list: 
     * o   Find all character strings with CEs between the baseCE and the 
     * next/previous CE, at the strength of the first token. Add these to the 
     * tailoring. 
     *     ? That is, if UCA has ...  x <<< X << x' <<< X' < y ..., and the 
     *       tailoring has & x < z... 
     *     ? Then we change the tailoring to & x  <<< X << x' <<< X' < z ... 
     * 
     * It is possible that this part should be done even while constructing list
     * The problem is that it is unknown what is going to be the strongest 
     * weight.
     * So we might as well do it here
     * o   Allocate CEs for each token in the list, based on the total number N 
     * of the largest level difference, and the gap G between baseCE and nextCE 
     * at that level. The relation * between the last item and nextCE is the 
     * same as the strongest strength. 
     * o   Example: baseCE < a << b <<< q << c < d < e * nextCE(X,1) 
     *     ? There are 3 primary items: a, d, e. Fit them into the primary gap. 
     *     Then fit b and c into the secondary gap between a and d, then fit q 
     *     into the tertiary gap between b and c. 
     * o   Example: baseCE << b <<< q << c * nextCE(X,2) 
     *     ? There are 2 secondary items: b, c. Fit them into the secondary gap. 
     *       Then fit q into the tertiary gap between b and c. 
     * o   When incrementing primary values, we will not cross high byte 
     *     boundaries except where there is only a single-byte primary. That is 
     *     to ensure that the script reordering will continue to work. 
     * @param collator the rule based collator to update
     * @exception Exception thrown when internal program error occurs
     */
    void assembleTailoringTable(RuleBasedCollator collator) throws Exception
    {
        
    for (int i = 0; i < m_parser_.m_resultLength_; i ++) {
        // now we need to generate the CEs  
        // We stuff the initial value in the buffers, and increase the 
            // appropriate buffer according to strength
            if  (m_parser_.m_listHeader_[i].m_first_ != null) { 
                // if there are any elements
                // due to the way parser works, subsequent tailorings
                // may remove all the elements from a sequence, therefore
                // leaving an empty tailoring sequence.
        initBuffers(m_parser_.m_listHeader_[i]);
            }
    }
        
        if (m_parser_.m_variableTop_ != null) { 
            // stuff the variable top value
        m_parser_.m_options_.m_variableTopValue_ 
        = m_parser_.m_variableTop_.m_CE_[0] >>> 16;
        // remove it from the list
        if (m_parser_.m_variableTop_.m_listHeader_.m_first_ 
                == m_parser_.m_variableTop_) { // first in list
        m_parser_.m_variableTop_.m_listHeader_.m_first_ 
            = m_parser_.m_variableTop_.m_next_;
        }
        if (m_parser_.m_variableTop_.m_listHeader_.m_last_ 
        == m_parser_.m_variableTop_) { 
                // first in list
        m_parser_.m_variableTop_.m_listHeader_.m_last_ 
            = m_parser_.m_variableTop_.m_previous_;    
        }
        if (m_parser_.m_variableTop_.m_next_ != null) {
        m_parser_.m_variableTop_.m_next_.m_previous_ 
            = m_parser_.m_variableTop_.m_previous_;
        }
        if (m_parser_.m_variableTop_.m_previous_ != null) {
        m_parser_.m_variableTop_.m_previous_.m_next_ 
            = m_parser_.m_variableTop_.m_next_;
        }
    }
        
    BuildTable t = new BuildTable(m_parser_);
        
        // After this, we have assigned CE values to all regular CEs now we 
    // will go through list once more and resolve expansions, make 
    // UCAElements structs and add them to table               
    for (int i = 0; i < m_parser_.m_resultLength_; i ++) {
        // now we need to generate the CEs 
        // We stuff the initial value in the buffers, and increase the 
        // appropriate buffer according to strength                                                          */
        createElements(t, m_parser_.m_listHeader_[i]);
    }
        
        m_utilElement_.clear();
        StringBuffer str = new StringBuffer();
        
    // add latin-1 stuff
        copyRangeFromUCA(t, 0, 0xFF);
    
        // add stuff for copying 
        if(m_parser_.m_copySet_ != null) {
            int i = 0;
            for(i = 0; i < m_parser_.m_copySet_.getRangeCount(); i++) {
                copyRangeFromUCA(t, m_parser_.m_copySet_.getRangeStart(i), 
                                 m_parser_.m_copySet_.getRangeEnd(i));
            }
        }
        
        // copy contractions from the UCA - this is felt mostly for cyrillic
    char conts[] = RuleBasedCollator.UCA_CONTRACTIONS_;
        int offset = 0;
    while (conts[offset] != 0) {
        // tailoredCE = ucmpe32_get(t.m_mapping, *conts);
            int tailoredCE = t.m_mapping_.getValue(conts[offset]);
        if (tailoredCE != CE_NOT_FOUND_) {         
        boolean needToAdd = true;
        if (isContractionTableElement(tailoredCE)) {
                    if (isTailored(t.m_contractions_, tailoredCE, 
                                   conts, offset + 1) == true) {
            needToAdd = false;
            }
        }
                if(m_parser_.m_removeSet_ != null && m_parser_.m_removeSet_.contains(conts[offset])) {
                    needToAdd = false;
                }

                
                if (needToAdd == true) { 
                    // we need to add if this contraction is not tailored.
            m_utilElement_.m_prefix_ = 0;
            m_utilElement_.m_prefixChars_ = null;
            m_utilElement_.m_cPoints_ = m_utilElement_.m_uchars_;
                    str.delete(0, str.length());
                    str.append(conts[offset]);
                    str.append(conts[offset + 1]);
            if (conts[offset + 2] != 0) {
            str.append(conts[offset + 2]);
            } 
                    m_utilElement_.m_uchars_ = str.toString();
                    m_utilElement_.m_CELength_ = 0;
                    m_utilColEIter_.setText(m_utilElement_.m_uchars_);
                    while (true) {
                        int CE = m_utilColEIter_.next();
                        if (CE != CollationElementIterator.NULLORDER) {
                            m_utilElement_.m_CEs_[m_utilElement_.m_CELength_ 
                         ++] = CE;
                        }
                        else {
                            break;
                        }
                    }
                    addAnElement(t, m_utilElement_);
                }
        } else if(m_parser_.m_removeSet_ != null && m_parser_.m_removeSet_.contains(conts[offset])) {
                copyRangeFromUCA(t, conts[offset], conts[offset]);
            }
            
        offset += 3;
    }
        
        // Add completely ignorable elements
        processUCACompleteIgnorables(t);
        
        // canonical closure 
        canonicalClosure(t);
  
    // still need to produce compatibility closure
    assembleTable(t, collator);  
    }
    
    // private inner classes -------------------------------------------------
    
    private static class CEGenerator 
    {
        // package private data members --------------------------------------
        
    WeightRange m_ranges_[];
    int m_rangesLength_;
    int m_byteSize_; 
        int m_start_; 
        int m_limit_;
    int m_maxCount_;
    int m_count_;
    int m_current_;
    int m_fLow_; // forbidden Low 
    int m_fHigh_; // forbidden High 
        
        // package private constructor ---------------------------------------
        
        CEGenerator() 
        {
            m_ranges_ = new WeightRange[7];      
            for (int i = 6; i >= 0; i --) {
                m_ranges_[i] = new WeightRange();
            }
        }
    }

    private static class WeightRange implements Comparable
    {
        // public methods ----------------------------------------------------
        
        /**
         * Compares this object with target
         * @param target object to compare with
         * @return 0 if equals, 1 if this is > target, -1 otherwise
         */
        public int compareTo(Object target) 
        {
            if (this == target) {
                return 0;
            }
            int tstart = ((WeightRange)target).m_start_;   
            if (m_start_ == tstart) {
                return 0;
            }
            if (m_start_ > tstart) {
                return 1;
            }
            return -1;
        }
        
        /**
         * Initialize 
         */
        public void clear()
        {
            m_start_ = 0;
            m_end_ = 0;
            m_length_ = 0; 
            m_count_ = 0;
            m_length2_ = 0;
            m_count2_ = 0;
        }
        
        // package private data members --------------------------------------
        
    int m_start_;
        int m_end_;
    int m_length_; 
        int m_count_;
    int m_length2_;
    int m_count2_;
        
        // package private constructor ---------------------------------------
        
        WeightRange()
        {
            clear();
        }
        
        /**
         * Copy constructor.
         * Cloneable is troublesome, needs to check for exception
         * @param source to clone
         */
        WeightRange(WeightRange source)
        {
            m_start_ = source.m_start_;
            m_end_ = source.m_end_;
            m_length_ = source.m_length_; 
            m_count_ = source.m_count_;
            m_length2_ = source.m_length2_;
            m_count2_ = source.m_count2_;
        }
    }
    
    private static class MaxJamoExpansionTable
    {
    // package private data members --------------------------------------
        
    Vector m_endExpansionCE_;
    // vector of booleans
    Vector m_isV_;
    byte m_maxLSize_;
    byte m_maxVSize_;
    byte m_maxTSize_;
        
    // package private constructor ---------------------------------------
        
    MaxJamoExpansionTable()
    {
        m_endExpansionCE_ = new Vector();
        m_isV_ = new Vector();
        m_endExpansionCE_.add(new Integer(0));
        m_isV_.add(Boolean.FALSE);
            m_maxLSize_ = 1;
            m_maxVSize_ = 1;
            m_maxTSize_ = 1;
    }
        
        MaxJamoExpansionTable(MaxJamoExpansionTable table)
        {
            m_endExpansionCE_ = (Vector)table.m_endExpansionCE_.clone();
            m_isV_ = (Vector)table.m_isV_.clone();
            m_maxLSize_ = table.m_maxLSize_;
            m_maxVSize_ = table.m_maxVSize_;
            m_maxTSize_ = table.m_maxTSize_;
        }
    }
    
    private static class MaxExpansionTable 
    {
    // package private constructor --------------------------------------
        
    MaxExpansionTable() 
    {
        m_endExpansionCE_ = new Vector();
        m_expansionCESize_ = new Vector();
        m_endExpansionCE_.add(new Integer(0));
        m_expansionCESize_.add(new Byte((byte)0));
    }
        
        MaxExpansionTable(MaxExpansionTable table) 
        {
            m_endExpansionCE_ = (Vector)table.m_endExpansionCE_.clone();
            m_expansionCESize_ = (Vector)table.m_expansionCESize_.clone();
        }
        
    // package private data member --------------------------------------
        
    Vector m_endExpansionCE_;
    Vector m_expansionCESize_;
    }
    
    private static class BasicContractionTable 
    {
    // package private constructors -------------------------------------
        
    BasicContractionTable()
    {
        m_CEs_ = new Vector();
        m_codePoints_ = new StringBuffer();
    }
        
    // package private data members -------------------------------------
        
    StringBuffer m_codePoints_;
    Vector m_CEs_;
    }
    
    private static class ContractionTable 
    {
    // package private constructor --------------------------------------
        
    /**
     * Builds a contraction table
     * @param mapping
     */
    ContractionTable(IntTrieBuilder mapping) 
    {
        m_mapping_ = mapping;
        m_elements_ = new Vector();
        m_CEs_ = new Vector();
        m_codePoints_ = new StringBuffer();
        m_offsets_ = new Vector();
        m_currentTag_ = CE_NOT_FOUND_TAG_;
    }
        
        /**
         * Copies a contraction table.
         * Not all data will be copied into their own object.
         * @param table
         */
        ContractionTable(ContractionTable table) 
        {
            m_mapping_ = table.m_mapping_;
            m_elements_ = (Vector)table.m_elements_.clone();
            m_codePoints_ = new StringBuffer(table.m_codePoints_.toString());
            m_CEs_ = (Vector)table.m_CEs_.clone();
            m_offsets_ = (Vector)table.m_offsets_.clone();
            m_currentTag_ = table.m_currentTag_;
        }
    
    // package private data members ------------------------------------
        
    /**
     * Vector of BasicContractionTable
     */
        Vector m_elements_;
        IntTrieBuilder m_mapping_;
        StringBuffer m_codePoints_;
    Vector m_CEs_;
    Vector m_offsets_;
    int m_currentTag_;
    }
    
    /**
     * Private class for combining mark table.
     * The table is indexed by the class value(0-255).
     */
    private static class CombinClassTable {
        /**
         * accumulated numbers of combining marks.
         */
        int[] index =  new int[256]; 
        
        /**
         * code point array for combining marks.
         */
        char[] cPoints; 
        
        /**
         * size of cPoints.
         */
        int    size;
        
        // constructor
        CombinClassTable() {
            cPoints = null;
            size = 0;
            pos = 0;
            curClass=1;
        }
        
        /**
         * Copy the combining mark table from ccc and index in compact
         * way.
         * 
         * @param cps : code point array
         * @param size : size of ccc
         * @param index : index of combining classes(0-255)
         */
        void generate(char[] cps, int numOfCM, int[] ccIndex) {
            int count =0;
            
            cPoints = new char[numOfCM];
            for (int i=0; i<256; i++) {
                for (int j=0; j<ccIndex[i]; j++) {
                    cPoints[count++] = cps[(i<<8)+j];
                }
                index[i] = count;
            }
            size = count;
        }
        
        /**
         * Get first CM(combining mark) with the combining class value cClass.
         * @param cClass : combining class value.
         * @return combining mark codepoint or 0 if no combining make with class value cClass
         */
        char GetFirstCM(int cClass) {
            curClass=cClass;
            if (cPoints==null || cClass==0 || index[cClass]==index[cClass-1]) {
                return 0;
            }
            pos =1;
            return cPoints[index[cClass-1]];
        }
        
        /**
         * Get next CM(combining mark) with the combining class value cClass.
         * Return combining mark codepoint or 0 if no next CM.
         */
        char GetNextCM() {
            if (cPoints==null || index[curClass]==(index[curClass-1]+pos))  {
                return 0;
            }
            return cPoints[index[curClass-1] + (pos++)];
        }
        
        // private data members
        int pos;
        int curClass;
    }

    private static final class BuildTable implements TrieBuilder.DataManipulate
    {
    // package private methods ------------------------------------------
        
        /**
     * For construction of the Trie tables.
     * Has to be labeled public
     * @param cp
     * @param offset
     * @return data offset or 0 
     * @draft 2.2
     */
    public int getFoldedValue(int cp, int offset)
    {
        int limit = cp + 0x400;
        while (cp < limit) {
        int value = m_mapping_.getValue(cp);
        boolean inBlockZero = m_mapping_.isInZeroBlock(cp);
        int tag = getCETag(value);
        if (inBlockZero == true) {
            cp += TrieBuilder.DATA_BLOCK_LENGTH;
        } 
        else if (!(isSpecial(value) && (tag == CE_IMPLICIT_TAG_ 
                        || tag == CE_NOT_FOUND_TAG_))) {
                    // These are values that are starting in either UCA 
                    // (IMPLICIT_TAG) or in the tailorings (NOT_FOUND_TAG). 
                    // Presence of these tags means that there is nothing in 
                    // this position and that it should be skipped.
            return RuleBasedCollator.CE_SPECIAL_FLAG_ 
            | (CE_SURROGATE_TAG_ << 24) | offset;
        } 
        else {
            ++ cp;
        }
        }
        return 0;
    }
    
    // package private constructor --------------------------------------
        
    /**
     * Returns a table
     */
    BuildTable(CollationRuleParser parser) 
    {
        m_collator_ = new RuleBasedCollator();
        m_collator_.setWithUCAData();
        MaxExpansionTable maxet = new MaxExpansionTable();
        MaxJamoExpansionTable maxjet = new MaxJamoExpansionTable();
        m_options_ = parser.m_options_;
        m_expansions_ = new Vector();
        // Do your own mallocs for the structure, array and have linear 
        // Latin 1
            int trieinitialvalue = RuleBasedCollator.CE_SPECIAL_FLAG_
        | (CE_NOT_FOUND_TAG_ << 24);
        // temporary fix for jb3822, 0x100000 -> 30000
        m_mapping_ = new IntTrieBuilder(null, 0x30000, trieinitialvalue, 
                                            trieinitialvalue, true); 
        m_prefixLookup_ = new Hashtable();
        // uhash_open(prefixLookupHash, prefixLookupComp);
        m_contractions_ = new ContractionTable(m_mapping_);
        // copy UCA's maxexpansion and merge as we go along
        m_maxExpansions_ = maxet;
        // adding an extra initial value for easier manipulation 
        for (int i = 0; 
         i < RuleBasedCollator.UCA_.m_expansionEndCE_.length; i ++) {
        maxet.m_endExpansionCE_.add(new Integer(
                            RuleBasedCollator.UCA_.m_expansionEndCE_[i]));
        maxet.m_expansionCESize_.add(new Byte(
                              RuleBasedCollator.UCA_.m_expansionEndCEMaxSize_[i]));
        }
        m_maxJamoExpansions_ = maxjet;
        
        m_unsafeCP_ = new byte[UNSAFECP_TABLE_SIZE_];
        m_contrEndCP_ = new byte[UNSAFECP_TABLE_SIZE_];
        Arrays.fill(m_unsafeCP_, (byte)0);
        Arrays.fill(m_contrEndCP_, (byte)0);
    }
    
        /**
         * Duplicating a BuildTable.
         * Not all data will be duplicated into their own object.
         * @param table to clone
         */
        BuildTable(BuildTable table) 
        {
            m_collator_ = table.m_collator_;
            m_mapping_ = new IntTrieBuilder(table.m_mapping_);
            m_expansions_ = (Vector)table.m_expansions_.clone();
            m_contractions_ = new ContractionTable(table.m_contractions_);
            m_contractions_.m_mapping_ = m_mapping_;
            m_options_ = table.m_options_;
            m_maxExpansions_ = new MaxExpansionTable(table.m_maxExpansions_);
            m_maxJamoExpansions_ 
        = new MaxJamoExpansionTable(table.m_maxJamoExpansions_);
            m_unsafeCP_ = new byte[table.m_unsafeCP_.length];
            System.arraycopy(table.m_unsafeCP_, 0, m_unsafeCP_, 0,
                             m_unsafeCP_.length);
            m_contrEndCP_ = new byte[table.m_contrEndCP_.length];
            System.arraycopy(table.m_contrEndCP_, 0, m_contrEndCP_, 0,
                             m_contrEndCP_.length);
        }
        
    // package private data members -------------------------------------
        
    RuleBasedCollator m_collator_;
        IntTrieBuilder m_mapping_; 
        Vector m_expansions_; 
        ContractionTable m_contractions_;
    // UCATableHeader image;
    CollationRuleParser.OptionSet m_options_;
    MaxExpansionTable m_maxExpansions_;
    MaxJamoExpansionTable m_maxJamoExpansions_;
    byte m_unsafeCP_[];
    byte m_contrEndCP_[];
    Hashtable m_prefixLookup_;
    CombinClassTable cmLookup = null;
    } 
    
    private static class Elements
    {
    // package private data members -------------------------------------
        
    String m_prefixChars_;
    int m_prefix_;
    String m_uchars_;
    /**
     * Working string
     */
    String m_cPoints_;    
    /**
     * Offset to the working string
     */
    int m_cPointsOffset_;
    /** 
     * These are collation elements - there could be more than one - in 
     * case of expansion 
     */    
    int m_CEs_[];      
        int m_CELength_;
    /** 
     * This is the value element maps in original table   
     */
    int m_mapCE_;         
    int m_sizePrim_[];
    int m_sizeSec_[];
    int m_sizeTer_[];
    boolean m_variableTop_;
    boolean m_caseBit_;
        
    // package private constructors -------------------------------------
        
    /**
     * Package private constructor
     */
    Elements()
    {
        m_sizePrim_ = new int[128];    
        m_sizeSec_ = new int[128];    
        m_sizeTer_ = new int[128];    
            m_CEs_ = new int[256];
            m_CELength_ = 0;
    }

        /**
     * Package private constructor
     */
    Elements(Elements element)
    {
            m_prefixChars_ = element.m_prefixChars_;
            m_prefix_ = element.m_prefix_;
            m_uchars_ = element.m_uchars_;
            m_cPoints_ = element.m_cPoints_;    
            m_cPointsOffset_ = element.m_cPointsOffset_;    
            m_CEs_ = element.m_CEs_;
            m_CELength_ = element.m_CELength_;
            m_mapCE_ = element.m_mapCE_;
        m_sizePrim_ = element.m_sizePrim_;
        m_sizeSec_ = element.m_sizeSec_;
        m_sizeTer_ = element.m_sizeTer_;
        m_variableTop_ = element.m_variableTop_;
        m_caseBit_ = element.m_caseBit_;
    }

        // package private methods -------------------------------------------
        
        /**
         * Initializing the elements
         */
        public void clear()
        {
            m_prefixChars_ = null;
            m_prefix_ = 0;
            m_uchars_ = null;
            m_cPoints_ = null;    
            m_cPointsOffset_ = 0;  
            m_CELength_ = 0;
            m_mapCE_ = 0;
            Arrays.fill(m_sizePrim_, 0);
            Arrays.fill(m_sizeSec_, 0);
            Arrays.fill(m_sizeTer_, 0);
            m_variableTop_ = false;
            m_caseBit_ = false;
        }

        
        /**
         * Hashcode calculation for token
         * @return the hashcode
         */
        public int hashCode()
        {
        String str = m_cPoints_.substring(m_cPointsOffset_);
        return str.hashCode();
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
        if (target instanceof Elements) {
        Elements t = (Elements)target;
        int size = m_cPoints_.length() - m_cPointsOffset_;
        if (size == t.m_cPoints_.length() - t.m_cPointsOffset_) {
            return t.m_cPoints_.regionMatches(t.m_cPointsOffset_, 
                              m_cPoints_, 
                              m_cPointsOffset_, size);
        }
            }
            return false;
    }
    }

    // private data member ---------------------------------------------------
    
    /**
     * Maximum strength used in CE building
     */
    private static final int CE_BASIC_STRENGTH_LIMIT_ = 3;
    /**
     * Maximum collation strength
     */
    private static final int CE_STRENGTH_LIMIT_ = 16;
    /**
     * Strength mask array, used in inverse UCA
     */
    private static final int STRENGTH_MASK_[] = {0xFFFF0000, 0xFFFFFF00, 
                                                 0xFFFFFFFF};
    /**
     * CE tag for not found
     */
    private static final int CE_NOT_FOUND_ = 0xF0000000;
    /**
     * CE tag for not found
     */
    private static final int CE_NOT_FOUND_TAG_ = 0;
    /**
     * This code point results in an expansion 
     */
    private static final int CE_EXPANSION_TAG_ = 1;
    /** 
     * Start of a contraction 
     */
    private static final int CE_CONTRACTION_TAG_ = 2;
    /* 
     * Thai character - do the reordering 
     */
    //private static final int CE_THAI_TAG_ = 3;            
    /* 
     * Charset processing, not yet implemented
     */
    //private static final int CE_CHARSET_TAG_ = 4;         
    /** 
     * Lead surrogate that is tailored and doesn't start a contraction 
     */
    private static final int CE_SURROGATE_TAG_ = 5;
    /* 
     * AC00-D7AF
     */
    //private static final int CE_HANGUL_SYLLABLE_TAG_ = 6;
    /* 
     * D800-DBFF
     */
    //private static final int CE_LEAD_SURROGATE_TAG_ = 7;
    /* 
     * DC00-DFFF
     */
    //private static final int CE_TRAIL_SURROGATE_TAG_ = 8; 
    /*
     * 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
     */    
    //private static final int CE_CJK_IMPLICIT_TAG_ = 9;
    private static final int CE_IMPLICIT_TAG_ = 10;
    private static final int CE_SPEC_PROC_TAG_ = 11;
    /** 
     * This is a three byte primary with starting secondaries and tertiaries.
     * It fits in a single 32 bit CE and is used instead of expansion to save
     * space without affecting the performance (hopefully) 
     */
    private static final int CE_LONG_PRIMARY_TAG_ = 12;  
    /** 
     * Unsafe UChar hash table table size. Size is 32 bytes for 1 bit for each 
     * latin 1 char + some power of two for hashing the rest of the chars. 
     * Size in bytes                               
     */
    private static final int UNSAFECP_TABLE_SIZE_ = 1056;
    /** 
     * Mask value down to "some power of two" -1. Number of bits, not num of 
     * bytes.       
     */
    private static final int UNSAFECP_TABLE_MASK_ = 0x1fff;
    /**
     * Case values
     */
    private static final int UPPER_CASE_ = 0x80;
    private static final int MIXED_CASE_ = 0x40;
    private static final int LOWER_CASE_ = 0x00;
    /*
     * Initial table size
     */
    //private static final int INIT_TABLE_SIZE_ = 1028;
    /*
     * Header size, copied from ICU4C, to be changed when that value changes
     */
    //private static final int HEADER_SIZE_ = 0xC4;
    /**
     * Contraction table new element indicator
     */
    private static final int CONTRACTION_TABLE_NEW_ELEMENT_ = 0xFFFFFF;
    /**
     * Parser for the rules
     */
    private CollationRuleParser m_parser_;
    /**
     * Utility UCA collation element iterator
     */
    private CollationElementIterator m_utilColEIter_;
    /**
     * Utility data members
     */
    private CEGenerator m_utilGens_[] = {new CEGenerator(), new CEGenerator(),
                                         new CEGenerator()};
    private int m_utilCEBuffer_[] = new int[CE_BASIC_STRENGTH_LIMIT_];
    private int m_utilIntBuffer_[] = new int[CE_STRENGTH_LIMIT_];
    private Elements m_utilElement_ = new Elements();
    private Elements m_utilElement2_ = new Elements();
    private CollationRuleParser.Token m_utilToken_ 
    = new CollationRuleParser.Token();
    private int m_utilCountBuffer_[] = new int[6];     
    private long m_utilLongBuffer_[] = new long[5];
    private WeightRange m_utilLowerWeightRange_[] = 
    {new WeightRange(), new WeightRange(), 
     new WeightRange(), new WeightRange(), 
     new WeightRange()}; 
    private WeightRange m_utilUpperWeightRange_[] = 
    {new WeightRange(), new WeightRange(), 
     new WeightRange(), new WeightRange(), 
     new WeightRange()}; 
    private WeightRange m_utilWeightRange_ = new WeightRange();
    private char m_utilCharBuffer_[] = new char[256];
    private CanonicalIterator m_utilCanIter_ = new CanonicalIterator("");
    private StringBuffer m_utilStringBuffer_ = new StringBuffer("");
    // Flag indicating a combining marks table is required or not.
    private static boolean buildCMTabFlag = false;
    
    
    // private methods -------------------------------------------------------
    
    /**
     * @param listheader parsed rule tokens
     * @exception Exception thrown when internal error occurs
     */
    private void initBuffers(CollationRuleParser.TokenListHeader listheader) 
    throws Exception
    {
        CollationRuleParser.Token token = listheader.m_last_;
        Arrays.fill(m_utilIntBuffer_, 0, CE_STRENGTH_LIMIT_, 0);
        
    token.m_toInsert_ = 1;
    m_utilIntBuffer_[token.m_strength_] = 1;
    while (token.m_previous_ != null) {
        if (token.m_previous_.m_strength_ < token.m_strength_) { 
                // going up
        m_utilIntBuffer_[token.m_strength_] = 0;
        m_utilIntBuffer_[token.m_previous_.m_strength_] ++;
        } 
            else if (token.m_previous_.m_strength_ > token.m_strength_) { 
                // going down
        m_utilIntBuffer_[token.m_previous_.m_strength_] = 1;
        } 
            else {
        m_utilIntBuffer_[token.m_strength_] ++;
        }
        token = token.m_previous_;
        token.m_toInsert_ = m_utilIntBuffer_[token.m_strength_];
    } 
        
    token.m_toInsert_ = m_utilIntBuffer_[token.m_strength_];
    INVERSE_UCA_.getInverseGapPositions(listheader);
        
    token = listheader.m_first_;
    int fstrength = Collator.IDENTICAL;
    int initstrength = Collator.IDENTICAL;
        
    m_utilCEBuffer_[Collator.PRIMARY] = mergeCE(listheader.m_baseCE_, 
                            listheader.m_baseContCE_,
                            Collator.PRIMARY);
    m_utilCEBuffer_[Collator.SECONDARY] = mergeCE(listheader.m_baseCE_, 
                              listheader.m_baseContCE_,
                              Collator.SECONDARY);
    m_utilCEBuffer_[Collator.TERTIARY] = mergeCE(listheader.m_baseCE_, 
                             listheader.m_baseContCE_,
                             Collator.TERTIARY);
    while (token != null) {
        fstrength = token.m_strength_;
        if (fstrength < initstrength) {
        initstrength = fstrength;
        if (listheader.m_pos_[fstrength] == -1) {
            while (listheader.m_pos_[fstrength] == -1 && fstrength > 0) 
            {
                fstrength--;
            }
            if (listheader.m_pos_[fstrength] == -1) {
            throw new Exception("Internal program error");
            }
        }
        if (initstrength == Collator.TERTIARY) { 
                    // starting with tertiary
            m_utilCEBuffer_[Collator.PRIMARY] 
            = listheader.m_gapsLo_[fstrength * 3];
            m_utilCEBuffer_[Collator.SECONDARY] 
            = listheader.m_gapsLo_[fstrength * 3 + 1];
            m_utilCEBuffer_[Collator.TERTIARY] = getCEGenerator(
                                    m_utilGens_[Collator.TERTIARY], 
                                    listheader.m_gapsLo_, 
                                    listheader.m_gapsHi_, 
                                    token, fstrength); 
        } 
                else if (initstrength == Collator.SECONDARY) { 
                    // secondaries
            m_utilCEBuffer_[Collator.PRIMARY] 
            = listheader.m_gapsLo_[fstrength * 3];
            m_utilCEBuffer_[Collator.SECONDARY] 
            = getCEGenerator(
                                         m_utilGens_[Collator.SECONDARY], 
                                         listheader.m_gapsLo_, 
                                         listheader.m_gapsHi_, 
                                         token, fstrength);
            m_utilCEBuffer_[Collator.TERTIARY] 
            = getSimpleCEGenerator(
                           m_utilGens_[Collator.TERTIARY], 
                           token, Collator.TERTIARY);
        } 
                else { 
                    // primaries 
            m_utilCEBuffer_[Collator.PRIMARY] 
            = getCEGenerator(
                     m_utilGens_[Collator.PRIMARY], 
                     listheader.m_gapsLo_, 
                     listheader.m_gapsHi_, 
                     token, fstrength);
            m_utilCEBuffer_[Collator.SECONDARY] 
            = getSimpleCEGenerator(
                                               m_utilGens_[Collator.SECONDARY], 
                                               token, Collator.SECONDARY);
            m_utilCEBuffer_[Collator.TERTIARY] 
            = getSimpleCEGenerator(
                                               m_utilGens_[Collator.TERTIARY], 
                                               token, Collator.TERTIARY);
        }
        } 
            else {
        if (token.m_strength_ == Collator.TERTIARY) {
            m_utilCEBuffer_[Collator.TERTIARY] 
            = getNextGenerated(m_utilGens_[Collator.TERTIARY]);
        } 
                else if (token.m_strength_ == Collator.SECONDARY) {
            m_utilCEBuffer_[Collator.SECONDARY] 
            = getNextGenerated(m_utilGens_[Collator.SECONDARY]);
            m_utilCEBuffer_[Collator.TERTIARY] 
            = getSimpleCEGenerator(
                           m_utilGens_[Collator.TERTIARY], 
                           token, Collator.TERTIARY);
        } 
                else if (token.m_strength_ == Collator.PRIMARY) {
            m_utilCEBuffer_[Collator.PRIMARY] 
            = getNextGenerated(
                       m_utilGens_[Collator.PRIMARY]);
            m_utilCEBuffer_[Collator.SECONDARY] 
            = getSimpleCEGenerator(
                           m_utilGens_[Collator.SECONDARY], 
                           token, Collator.SECONDARY);
            m_utilCEBuffer_[Collator.TERTIARY] 
            = getSimpleCEGenerator(
                           m_utilGens_[Collator.TERTIARY], 
                           token, Collator.TERTIARY);
        }
        }
        doCE(m_utilCEBuffer_, token);
        token = token.m_next_;
    }
    }

    /**
     * Get the next generated ce
     * @param g ce generator
     * @return next generated ce 
     */
    private int getNextGenerated(CEGenerator g) 
    {
        g.m_current_ = nextWeight(g);
        return g.m_current_;
    }

    /**
     * @param g CEGenerator
     * @param token rule token
     * @param strength 
     * @return ce generator
     * @exception Exception thrown when internal error occurs
     */
    private int getSimpleCEGenerator(CEGenerator g, 
                                     CollationRuleParser.Token token, 
                                     int strength) throws Exception
    {
        int high, low, count = 1;
        int maxbyte = (strength == Collator.TERTIARY) ? 0x3F : 0xFF;

    if (strength == Collator.SECONDARY) {
        low = RuleBasedCollator.COMMON_TOP_2_ << 24;
        high = 0xFFFFFFFF;
        count = 0xFF - RuleBasedCollator.COMMON_TOP_2_;
    } 
        else {
        low = RuleBasedCollator.BYTE_COMMON_ << 24; //0x05000000;
        high = 0x40000000;
        count = 0x40 - RuleBasedCollator.BYTE_COMMON_;
    }
    
    if (token.m_next_ != null && token.m_next_.m_strength_ == strength) {
        count = token.m_next_.m_toInsert_;
    } 
    
    g.m_rangesLength_ = allocateWeights(low, high, count, maxbyte, 
                                            g.m_ranges_);
    g.m_current_ = RuleBasedCollator.BYTE_COMMON_ << 24;
    
    if (g.m_rangesLength_ == 0) {
        throw new Exception("Internal program error");
    }
    return g.m_current_;
    }

    /**
     * Combines 2 ce into one with respect to the argument strength
     * @param ce1 first ce
     * @param ce2 second ce
     * @param strength strength to use
     * @return combined ce
     */
    private static int mergeCE(int ce1, int ce2, int strength) 
    {
        int mask = RuleBasedCollator.CE_TERTIARY_MASK_;
        if (strength == Collator.SECONDARY) {
            mask = RuleBasedCollator.CE_SECONDARY_MASK_;
        }
        else if (strength == Collator.PRIMARY) {
            mask = RuleBasedCollator.CE_PRIMARY_MASK_;
        }
        ce1 &= mask;
        ce2 &= mask;
        switch (strength) 
        {
            case Collator.PRIMARY:
                return ce1 | ce2 >>> 16;
            case Collator.SECONDARY:
                return ce1 << 16 | ce2 << 8;
            default:
                return ce1 << 24 | ce2 << 16;
        }
    }
    
    /**
     * @param g CEGenerator
     * @param lows low gap array
     * @param highs high gap array
     * @param token rule token
     * @param fstrength 
     * @exception Exception thrown when internal error occurs
     */
    private int getCEGenerator(CEGenerator g, int lows[], int highs[], 
                               CollationRuleParser.Token token, int fstrength) 
    throws Exception
    {
    int strength = token.m_strength_;
    int low = lows[fstrength * 3 + strength];
    int high = highs[fstrength * 3 + strength];
    int maxbyte = 0;
    if(strength == Collator.TERTIARY) {
        maxbyte = 0x3F;
    } else if(strength == Collator.PRIMARY) {
        maxbyte = 0xFE;
    } else {
        maxbyte = 0xFF;
    }
    
    int count = token.m_toInsert_;
    
    if (Utility.compareUnsigned(low, high) >= 0 
            && strength > Collator.PRIMARY) {
        int s = strength;
        while (true) {
        s --;
        if (lows[fstrength * 3 + s] != highs[fstrength * 3 + s]) {
            if (strength == Collator.SECONDARY) {
            low = RuleBasedCollator.COMMON_TOP_2_ << 24;
            high = 0xFFFFFFFF;
            } 
                    else {
                        // low = 0x02000000; 
                        // This needs to be checked - what if low is
                        // not good...
            high = 0x40000000;
            }
            break;
        }
        if (s < 0) {
            throw new Exception("Internal program error");
        }
        }
    } 
    if (low == 0) {
        low = 0x01000000;
    }
    if (strength == Collator.SECONDARY) { // similar as simple 
        if (Utility.compareUnsigned(low, 
                    RuleBasedCollator.COMMON_BOTTOM_2_ << 24) >= 0
                && Utility.compareUnsigned(low, 
                       RuleBasedCollator.COMMON_TOP_2_ << 24) < 0) {
        low = RuleBasedCollator.COMMON_TOP_2_ << 24;
            }
        if (Utility.compareUnsigned(high, 
                    RuleBasedCollator.COMMON_BOTTOM_2_ << 24) > 0 
                && Utility.compareUnsigned(high, 
                       RuleBasedCollator.COMMON_TOP_2_ << 24) < 0) {
        high = RuleBasedCollator.COMMON_TOP_2_ << 24;
        } 
        if (Utility.compareUnsigned(low, 
                    RuleBasedCollator.COMMON_BOTTOM_2_ << 24) < 0) {
        g.m_rangesLength_ = allocateWeights(
                            RuleBasedCollator.BYTE_UNSHIFTED_MIN_ << 24, 
                            high, count, maxbyte, g.m_ranges_);
        g.m_current_ = nextWeight(g);
        //g.m_current_ = RuleBasedCollator.COMMON_BOTTOM_2_ << 24;
        return g.m_current_;
        }
    } 
    
    g.m_rangesLength_ = allocateWeights(low, high, count, maxbyte, 
                                            g.m_ranges_);
    if (g.m_rangesLength_ == 0) {
        throw new Exception("Internal program error");
    }
    g.m_current_ = nextWeight(g);
    return g.m_current_;
    }

    /**
     * @param ceparts list of collation elements parts
     * @param token rule token
     * @exception Exception thrown when forming case bits for expansions fails
     */
    private void doCE(int ceparts[], CollationRuleParser.Token token) 
    throws Exception
    {
        // this one makes the table and stuff
    // int noofbytes[] = new int[3];
    for (int i = 0; i < 3; i ++) {
        // noofbytes[i] = countBytes(ceparts[i]);
            m_utilIntBuffer_[i] = countBytes(ceparts[i]);
    }
    
    // Here we have to pack CEs from parts
    int cei = 0;
    int value = 0;
    
    while ((cei << 1) < m_utilIntBuffer_[0] || cei < m_utilIntBuffer_[1] 
               || cei < m_utilIntBuffer_[2]) {
        if (cei > 0) {
        value = RuleBasedCollator.CE_CONTINUATION_MARKER_;
        } else {
        value = 0;
        }
        
        if ((cei << 1) < m_utilIntBuffer_[0]) {
        value |= ((ceparts[0] >> (32 - ((cei + 1) << 4))) & 0xFFFF) 
                      << 16;
        }
        if (cei < m_utilIntBuffer_[1]) {
        value |= ((ceparts[1] >> (32 - ((cei + 1) << 3))) & 0xFF) << 8;
        }
            
        if (cei < m_utilIntBuffer_[2]) {
        value |= ((ceparts[2] >> (32 - ((cei+1) << 3))) & 0x3F);
        }
        token.m_CE_[cei] = value;
        cei ++;
    }
    if (cei == 0) { // totally ignorable
        token.m_CELength_ = 1;
        token.m_CE_[0] = 0;
    } 
    else { // there is at least something
        token.m_CELength_ = cei;
    }
          
    // Case bits handling for expansion
    if(token.m_CE_[0] != 0) { // case bits should be set only for non-ignorables
        int startoftokenrule = token.m_source_ & 0xFF;
        if ((token.m_source_ >>> 24) > 1) {
            // Do it manually
            int length = token.m_source_ >>> 24;
            String tokenstr = token.m_rules_.substring(startoftokenrule, 
                                   startoftokenrule + length);
            token.m_CE_[0] |= getCaseBits(tokenstr);
        } 
        else {
            // Copy it from the UCA
            int caseCE 
            = getFirstCE(token.m_rules_.charAt(startoftokenrule));
            token.m_CE_[0] |= (caseCE & 0xC0);
        }
    }
    }

    /**
     * Count the number of non-zero bytes used in the ce
     * @param ce 
     * @return number of non-zero bytes used in ce
     */
    private static final int countBytes(int ce)   
    {                               
    int mask = 0xFFFFFFFF;   
    int result = 0;              
    while (mask != 0) {            
        if ((ce & mask) != 0) { 
        result ++;            
        }                           
        mask >>>= 8;                 
    }   
        return result;                          
    }
    
    /**
     * We are ready to create collation elements
     * @param t build table to insert
     * @param lh rule token list header
     */
    private void createElements(BuildTable t, 
                CollationRuleParser.TokenListHeader lh)
    {
    CollationRuleParser.Token tok = lh.m_first_;
    m_utilElement_.clear();
    while (tok != null) {
        // first, check if there are any expansions
        // if there are expansions, we need to do a little bit more 
        // processing since parts of expansion can be tailored, while 
        // others are not
        if (tok.m_expansion_ != 0) {
        int len = tok.m_expansion_ >>> 24;
        int currentSequenceLen = len;
        int expOffset = tok.m_expansion_ & 0x00FFFFFF;
        m_utilToken_.m_source_ = currentSequenceLen | expOffset;
        m_utilToken_.m_rules_ = m_parser_.m_source_;
    
        while (len > 0) {
            currentSequenceLen = len;
            while (currentSequenceLen > 0) {
            m_utilToken_.m_source_ = (currentSequenceLen << 24) 
                | expOffset;
            CollationRuleParser.Token expt = 
                (CollationRuleParser.Token)
                m_parser_.m_hashTable_.get(m_utilToken_);
            if (expt != null 
                && expt.m_strength_ 
                != CollationRuleParser.TOKEN_RESET_) { 
                // expansion is tailored
                int noOfCEsToCopy = expt.m_CELength_;
                for (int j = 0; j < noOfCEsToCopy; j ++) {
                tok.m_expCE_[tok.m_expCELength_ + j] 
                    = expt.m_CE_[j];
                }
                tok.m_expCELength_ += noOfCEsToCopy;
                // never try to add codepoints and CEs.
                // For some odd reason, it won't work.
                expOffset += currentSequenceLen; //noOfCEsToCopy;
                len -= currentSequenceLen; //noOfCEsToCopy;
                break;
            } 
            else {
                currentSequenceLen --;
            }
            }
            if (currentSequenceLen == 0) { 
            // couldn't find any tailored subsequence, will have to 
            // get one from UCA. first, get the UChars from the 
            // rules then pick CEs out until there is no more and 
            // stuff them into expansion
            m_utilColEIter_.setText(m_parser_.m_source_.substring(
                                          expOffset, expOffset + 1));
            while (true) {
                int order = m_utilColEIter_.next();
                if (order == CollationElementIterator.NULLORDER) {
                break;
                }
                tok.m_expCE_[tok.m_expCELength_ ++] = order;
            }
            expOffset ++;
            len --;
            }
        }
        } 
        else {
        tok.m_expCELength_ = 0;
        }
    
        // set the ucaelement with obtained values
            m_utilElement_.m_CELength_ = tok.m_CELength_ + tok.m_expCELength_;
            
        // copy CEs
        System.arraycopy(tok.m_CE_, 0, m_utilElement_.m_CEs_, 0, 
                             tok.m_CELength_);
        System.arraycopy(tok.m_expCE_, 0, m_utilElement_.m_CEs_, 
                             tok.m_CELength_, tok.m_expCELength_);
    
        // copy UChars 
        // We kept prefix and source kind of together, as it is a kind of a 
        // contraction. 
        // However, now we have to slice the prefix off the main thing - 
        m_utilElement_.m_prefix_ = 0;// el.m_prefixChars_;
        m_utilElement_.m_cPointsOffset_ = 0; //el.m_uchars_;
        if (tok.m_prefix_ != 0) { 
        // we will just copy the prefix here, and adjust accordingly in 
        // the addPrefix function in ucol_elm. The reason is that we 
        // need to add both composed AND decomposed elements to the 
        // unsafe table.
        int size = tok.m_prefix_ >> 24;
        int offset = tok.m_prefix_ & 0x00FFFFFF;
        m_utilElement_.m_prefixChars_ 
            = m_parser_.m_source_.substring(offset, offset + size);
        size = (tok.m_source_ >> 24) - (tok.m_prefix_ >> 24); 
        offset = (tok.m_source_ & 0x00FFFFFF) + (tok.m_prefix_ >> 24);
        m_utilElement_.m_uchars_ 
            = m_parser_.m_source_.substring(offset, offset + size);
        } 
        else {
        m_utilElement_.m_prefixChars_ = null;
        int offset = tok.m_source_ & 0x00FFFFFF;
        int size = tok.m_source_ >>> 24;
        m_utilElement_.m_uchars_ = m_parser_.m_source_.substring(offset, 
                                     offset + size);
        }
        m_utilElement_.m_cPoints_ = m_utilElement_.m_uchars_;
        
        boolean containCombinMarks = false;
        for (int i = 0; i < m_utilElement_.m_cPoints_.length() 
             - m_utilElement_.m_cPointsOffset_; i ++) {
        if (isJamo(m_utilElement_.m_cPoints_.charAt(i))) {
            t.m_collator_.m_isJamoSpecial_ = true;
            break;
        }
        if ( !buildCMTabFlag ) {
            // check combining class
            char fcd = NormalizerImpl.getFCD16(m_utilElement_.m_cPoints_.charAt(i));
            if ( (fcd & 0xff) == 0 ) {
                // reset flag when current char is not combining mark.
                containCombinMarks = false;  
            }
            else {
                containCombinMarks = true;
            }
        }
        }
        
        if ( !buildCMTabFlag && containCombinMarks ) {
            buildCMTabFlag = true;
        }
            
            /***
    
            // Case bits handling 
            m_utilElement_.m_CEs_[0] &= 0xFFFFFF3F; 
        // Clean the case bits field
            if (m_utilElement_.m_cPoints_.length() 
                - m_utilElement_.m_cPointsOffset_ > 1) {
        // Do it manually
        m_utilElement_.m_CEs_[0] 
        |= getCaseBits(m_utilElement_.m_cPoints_);
            } 
            else {
        // Copy it from the UCA
        int caseCE = getFirstCE(m_utilElement_.m_cPoints_.charAt(0));
        m_utilElement_.m_CEs_[0] |= (caseCE & 0xC0);
            }
    
            ***/
        // and then, add it
        addAnElement(t, m_utilElement_);
        tok = tok.m_next_;
    }   
    }
    
    /**
     * Testing if the string argument has case
     * @param src string
     * @return the case for this char array
     * @exception Exception thrown when internal program error occurs
     */
    private final int getCaseBits(String src) throws Exception
    {
    int uCount = 0; 
    int lCount = 0;
    src = Normalizer.decompose(src, true);
    m_utilColEIter_.setText(src);
    for (int i = 0; i < src.length(); i++) {
        m_utilColEIter_.setText(src.substring(i, i + 1));
        int order = m_utilColEIter_.next();
        if (RuleBasedCollator.isContinuation(order)) {
        throw new Exception("Internal program error");
        }
        if ((order & RuleBasedCollator.CE_CASE_BIT_MASK_)
        == UPPER_CASE_) {
        uCount ++;
        } 
        else {
        char ch = src.charAt(i);
        if (UCharacter.isLowerCase(ch)) {
            lCount ++;
        } 
        else {
            if (toSmallKana(ch) == ch && toLargeKana(ch) != ch) {
            lCount ++;
            }
        }
        }
    }
        
    if (uCount != 0 && lCount != 0) {
        return MIXED_CASE_;
    } 
    else if (uCount != 0) {
        return UPPER_CASE_;
    } 
    else {
        return LOWER_CASE_;
    }
    }
    
    /**
     * Converts a char to the uppercase Kana
     * @param ch character to convert
     * @return the converted Kana character
     */
    private static final char toLargeKana(char ch) 
    {
    if (0x3042 < ch && ch < 0x30ef) { // Kana range 
        switch (ch - 0x3000) {
        case 0x41: 
        case 0x43: 
        case 0x45: 
        case 0x47: 
        case 0x49: 
        case 0x63: 
        case 0x83: 
        case 0x85: 
        case 0x8E:
        case 0xA1: 
        case 0xA3: 
        case 0xA5: 
        case 0xA7: 
        case 0xA9: 
        case 0xC3: 
        case 0xE3: 
        case 0xE5: 
        case 0xEE:
        ch ++;
        break;
        case 0xF5:
        ch = 0x30AB;
        break;
        case 0xF6:
        ch = 0x30B1;
        break;
        }
    }
    return ch;
    }
    
    /**
     * Converts a char to the lowercase Kana
     * @param ch character to convert
     * @return the converted Kana character
     */
    private static final char toSmallKana(char ch) 
    {
    if (0x3042 < ch && ch < 0x30ef) { // Kana range
        switch (ch - 0x3000) {
        case 0x42: 
        case 0x44: 
        case 0x46: 
        case 0x48: 
        case 0x4A: 
        case 0x64: 
        case 0x84: 
        case 0x86: 
        case 0x8F:
        case 0xA2: 
        case 0xA4: 
        case 0xA6: 
        case 0xA8: 
        case 0xAA: 
        case 0xC4: 
        case 0xE4: 
        case 0xE6: 
        case 0xEF:
        ch --;
        break;
        case 0xAB:
        ch = 0x30F5;
        break;
        case 0xB1:
        ch = 0x30F6;
        break;
        }
    }
    return ch;
    }

    /**
     * This should be connected to special Jamo handling.
     */
    private int getFirstCE(char ch) 
    {
        m_utilColEIter_.setText(UCharacter.toString(ch));
    return m_utilColEIter_.next();
    }
    
    /** 
     * This adds a read element, while testing for existence 
     * @param t build table
     * @param element 
     * @return ce
     */
    private int addAnElement(BuildTable t, Elements element) 
    {
        Vector expansions = t.m_expansions_;
        element.m_mapCE_ = 0;
        
        if (element.m_CELength_ == 1) {
            element.m_mapCE_ = element.m_CEs_[0];

        } else {     
        // unfortunately, it looks like we have to look for a long primary 
        // here since in canonical closure we are going to hit some long 
        // primaries from the first phase, and they will come back as 
        // continuations/expansions destroying the effect of the previous 
        // opitimization. A long primary is a three byte primary with 
        // starting secondaries and tertiaries. It can appear in long runs 
        // of only primary differences (like east Asian tailorings) also, 
        // it should not be an expansion, as expansions would break with 
        // this
        if (element.m_CELength_ == 2 // a two CE expansion 
        && RuleBasedCollator.isContinuation(element.m_CEs_[1]) 
        && (element.m_CEs_[1] 
            & (~(0xFF << 24 | RuleBasedCollator.CE_CONTINUATION_MARKER_))) 
        == 0 // that has only primaries in continuation
        && (((element.m_CEs_[0] >> 8) & 0xFF) 
            == RuleBasedCollator.BYTE_COMMON_) 
        // a common secondary
        && ((element.m_CEs_[0] & 0xFF) 
            == RuleBasedCollator.BYTE_COMMON_) // and a common tertiary
        ) {
        element.m_mapCE_ = RuleBasedCollator.CE_SPECIAL_FLAG_ 
            // a long primary special
            | (CE_LONG_PRIMARY_TAG_ << 24) 
            // first and second byte of primary
            | ((element.m_CEs_[0] >> 8) & 0xFFFF00) 
            // third byte of primary
            | ((element.m_CEs_[1] >> 24) & 0xFF);   
        } 
        else {
                // omitting expansion offset in builder
                // (HEADER_SIZE_ >> 2)
        int expansion = RuleBasedCollator.CE_SPECIAL_FLAG_ 
            | (CE_EXPANSION_TAG_ 
               << RuleBasedCollator.CE_TAG_SHIFT_) 
            | (addExpansion(expansions, element.m_CEs_[0])
               << 4) & 0xFFFFF0;
    
        for (int i = 1; i < element.m_CELength_; i ++) {
            addExpansion(expansions, element.m_CEs_[i]);
        }
        if (element.m_CELength_ <= 0xF) {
            expansion |= element.m_CELength_;
        } 
        else {
            addExpansion(expansions, 0);
        }
        element.m_mapCE_ = expansion;
        setMaxExpansion(element.m_CEs_[element.m_CELength_ - 1],
                (byte)element.m_CELength_, 
                t.m_maxExpansions_);
        if (isJamo(element.m_cPoints_.charAt(0))){
            t.m_collator_.m_isJamoSpecial_ = true;
            setMaxJamoExpansion(element.m_cPoints_.charAt(0),
                    element.m_CEs_[element.m_CELength_ 
                              - 1],
                    (byte)element.m_CELength_,
                    t.m_maxJamoExpansions_);
        }
        }
    }
        
        // We treat digits differently - they are "uber special" and should be
        // processed differently if numeric collation is on. 
        int uniChar = 0;
        if ((element.m_uchars_.length() == 2) 
            && UTF16.isLeadSurrogate(element.m_uchars_.charAt(0))) {
            uniChar = UCharacterProperty.getRawSupplementary(
                                 element.m_uchars_.charAt(0), 
                                 element.m_uchars_.charAt(1));      
        } 
        else if (element.m_uchars_.length() == 1) {
            uniChar = element.m_uchars_.charAt(0);
        }
        
        // Here, we either have one normal CE OR mapCE is set. Therefore, we 
        // stuff only one element to the expansion buffer. When we encounter a 
        // digit and we don't do numeric collation, we will just pick the CE 
        // we have and break out of case (see ucol.cpp ucol_prv_getSpecialCE 
        // && ucol_prv_getSpecialPrevCE). If we picked a special, further 
        // processing will occur. If it's a simple CE, we'll return due
        // to how the loop is constructed.
        if (uniChar != 0 && UCharacter.isDigit(uniChar)) {
            // prepare the element
            int expansion = RuleBasedCollator.CE_SPECIAL_FLAG_ 
        | (CollationElementIterator.CE_DIGIT_TAG_
           << RuleBasedCollator.CE_TAG_SHIFT_) | 1; 
            if (element.m_mapCE_ != 0) { 
                // if there is an expansion, we'll pick it here
                expansion |= (addExpansion(expansions, element.m_mapCE_) << 4);
            } 
            else {
                expansion |= (addExpansion(expansions, element.m_CEs_[0]) << 4);
            }
            element.m_mapCE_ = expansion;
        }
    
    // here we want to add the prefix structure.
    // I will try to process it as a reverse contraction, if possible.
    // prefix buffer is already reversed.
    
    if (element.m_prefixChars_ != null &&
            element.m_prefixChars_.length() - element.m_prefix_ > 0) {
        // We keep the seen prefix starter elements in a hashtable we need 
            // it to be able to distinguish between the simple codepoints and 
            // prefix starters. Also, we need to use it for canonical closure.
        m_utilElement2_.m_caseBit_ = element.m_caseBit_;
            m_utilElement2_.m_CELength_ = element.m_CELength_;
            m_utilElement2_.m_CEs_ = element.m_CEs_;
            m_utilElement2_.m_mapCE_ = element.m_mapCE_;
            //m_utilElement2_.m_prefixChars_ = element.m_prefixChars_;
            m_utilElement2_.m_sizePrim_ = element.m_sizePrim_;
            m_utilElement2_.m_sizeSec_ = element.m_sizeSec_;
            m_utilElement2_.m_sizeTer_ = element.m_sizeTer_;
            m_utilElement2_.m_variableTop_ = element.m_variableTop_;
            m_utilElement2_.m_prefix_ = element.m_prefix_;
            m_utilElement2_.m_prefixChars_ = Normalizer.compose(element.m_prefixChars_, false);
            m_utilElement2_.m_uchars_ = element.m_uchars_;
            m_utilElement2_.m_cPoints_ = element.m_cPoints_;
            m_utilElement2_.m_cPointsOffset_ = 0;
            
        if (t.m_prefixLookup_ != null) {
        Elements uCE = (Elements)t.m_prefixLookup_.get(element);
        if (uCE != null) { 
                    // there is already a set of code points here
            element.m_mapCE_ = addPrefix(t, uCE.m_mapCE_, element);
        } 
                else { // no code points, so this spot is clean
            element.m_mapCE_ = addPrefix(t, CE_NOT_FOUND_, element);
            uCE = new Elements(element);
            uCE.m_cPoints_ = uCE.m_uchars_;
            t.m_prefixLookup_.put(uCE, uCE);
        }
        if (m_utilElement2_.m_prefixChars_.length() 
            != element.m_prefixChars_.length() - element.m_prefix_
                    || !m_utilElement2_.m_prefixChars_.regionMatches(0,
                                     element.m_prefixChars_, element.m_prefix_,
                                     m_utilElement2_.m_prefixChars_.length())) {
            // do it!
                    m_utilElement2_.m_mapCE_ = addPrefix(t, element.m_mapCE_, 
                                                         m_utilElement2_);
        }
        }
    }
    
    // We need to use the canonical iterator here
    // the way we do it is to generate the canonically equivalent strings 
    // for the contraction and then add the sequences that pass FCD check
    if (element.m_cPoints_.length() - element.m_cPointsOffset_ > 1 
        && !(element.m_cPoints_.length() - element.m_cPointsOffset_ == 2 
         && UTF16.isLeadSurrogate(element.m_cPoints_.charAt(0)) 
         && UTF16.isTrailSurrogate(element.m_cPoints_.charAt(1)))) { 
            // this is a contraction, we should check whether a composed form 
            // should also be included
        m_utilCanIter_.setSource(element.m_cPoints_);
        String source = m_utilCanIter_.next();
        while (source != null && source.length() > 0) {
        if (Normalizer.quickCheck(source, Normalizer.FCD,0) 
                    != Normalizer.NO) {
            element.m_uchars_ = source;
            element.m_cPoints_ = element.m_uchars_;
            finalizeAddition(t, element);
        }
        source = m_utilCanIter_.next();
        }
        
        return element.m_mapCE_;
    } 
        else {
        return finalizeAddition(t, element);  
    }
    }
    
    /**
     * Adds an expansion ce to the expansion vector
     * @param expansions vector to add to
     * @param value of the expansion
     * @return the current position of the new element
     */
    private static final int addExpansion(Vector expansions, int value) 
    {
    expansions.add(new Integer(value));
    return expansions.size() - 1;
    }
    
    /**
     * Looks for the maximum length of all expansion sequences ending with the 
     * same collation element. The size required for maxexpansion and maxsize 
     * is returned if the arrays are too small.
     * @param endexpansion the last expansion collation element to be added
     * @param expansionsize size of the expansion
     * @param maxexpansion data structure to store the maximum expansion data.
     * @returns size of the maxexpansion and maxsize used.
     */
    private static int setMaxExpansion(int endexpansion, byte expansionsize,
                       MaxExpansionTable maxexpansion)
    {
    int start = 0;
    int limit = maxexpansion.m_endExpansionCE_.size();
        long unsigned = (long)endexpansion;
        unsigned &= 0xFFFFFFFFl;
    
    // using binary search to determine if last expansion element is 
    // already in the array 
    int result = -1;
    while (start < limit - 1) {                                                
        int mid = start + ((limit - start) >> 1);                                    
            long unsignedce = ((Integer)maxexpansion.m_endExpansionCE_.get(
                                       mid)).intValue(); 
            unsignedce &= 0xFFFFFFFFl;
        if (unsigned <= unsignedce) {                                                   
        limit = mid;                                                           
        }                                                                        
        else {                                                                   
        start = mid;                                                           
        }                                                                        
    } 
          
    if (((Integer)maxexpansion.m_endExpansionCE_.get(start)).intValue() 
        == endexpansion) {                                                     
        result = start;  
    }                                                                          
    else if (((Integer)maxexpansion.m_endExpansionCE_.get(limit)).intValue() 
         == endexpansion) {                                                     
        result = limit;      
    }                                            
    if (result > -1) {
        // found the ce in expansion, we'll just modify the size if it 
        // is smaller
        Object currentsize = maxexpansion.m_expansionCESize_.get(result);
        if (((Byte)currentsize).byteValue() < expansionsize) {
        maxexpansion.m_expansionCESize_.set(result, 
                            new Byte(expansionsize));
        }
    }
    else {
        // we'll need to squeeze the value into the array. initial 
        // implementation. shifting the subarray down by 1
        maxexpansion.m_endExpansionCE_.insertElementAt(
                                                   new Integer(endexpansion),
                                                   start + 1);
        maxexpansion.m_expansionCESize_.insertElementAt(
                                new Byte(expansionsize),
                                start + 1);
    }
    return maxexpansion.m_endExpansionCE_.size();
    }
    
    /**
     * Sets the maximum length of all jamo expansion sequences ending with the 
     * same collation element. The size required for maxexpansion and maxsize 
     * is returned if the arrays are too small.
     * @param ch the jamo codepoint
     * @param endexpansion the last expansion collation element to be added
     * @param expansionsize size of the expansion
     * @param maxexpansion data structure to store the maximum expansion data.
     * @returns size of the maxexpansion and maxsize used.
     */
    private static int setMaxJamoExpansion(char ch, int endexpansion,
                       byte expansionsize,
                       MaxJamoExpansionTable maxexpansion)
    {
    boolean isV = true;
    if (ch >= 0x1100 && ch <= 0x1112) {
        // determines L for Jamo, doesn't need to store this since it is 
        // never at the end of a expansion
        if (maxexpansion.m_maxLSize_ < expansionsize) {
        maxexpansion.m_maxLSize_ = expansionsize;
        }
        return maxexpansion.m_endExpansionCE_.size();
    }
    
    if (ch >= 0x1161 && ch <= 0x1175) {
        // determines V for Jamo
        if (maxexpansion.m_maxVSize_ < expansionsize) {
        maxexpansion.m_maxVSize_ = expansionsize;
        }
    }
    
    if (ch >= 0x11A8 && ch <= 0x11C2) {
        isV = false;
        // determines T for Jamo
        if (maxexpansion.m_maxTSize_ < expansionsize) {
        maxexpansion.m_maxTSize_ = expansionsize;
        }
    }

        int pos = maxexpansion.m_endExpansionCE_.size();    
    while (pos > 0) {
        pos --;
        if (((Integer)maxexpansion.m_endExpansionCE_.get(pos)).intValue() 
        == endexpansion) {
        return maxexpansion.m_endExpansionCE_.size();
        }
    }
    maxexpansion.m_endExpansionCE_.add(new Integer(endexpansion));
    maxexpansion.m_isV_.add(isV ? Boolean.TRUE : Boolean.FALSE);
          
    return maxexpansion.m_endExpansionCE_.size();
    }
    
    /**
     * Adds a prefix to the table
     * @param t build table to update
     * @param CE collation element to add
     * @param element rule element to add
     * @return modified ce
     */
    private int addPrefix(BuildTable t, int CE, Elements element) 
    {
    // currently the longest prefix we're supporting in Japanese is two 
    // characters long. Although this table could quite easily mimic 
    // complete contraction stuff there is no good reason to make a general 
    // solution, as it would require some error prone messing.
    ContractionTable contractions = t.m_contractions_;
    String oldCP = element.m_cPoints_;
    int oldCPOffset = element.m_cPointsOffset_;
        
    contractions.m_currentTag_ = CE_SPEC_PROC_TAG_;
    // here, we will normalize & add prefix to the table.
    int size = element.m_prefixChars_.length() - element.m_prefix_;
    for (int j = 1; j < size; j ++) {   
        // First add NFD prefix chars to unsafe CP hash table 
        // Unless it is a trail surrogate, which is handled algoritmically 
        // and shouldn't take up space in the table.
        char ch = element.m_prefixChars_.charAt(j + element.m_prefix_);
        if (!UTF16.isTrailSurrogate(ch)) {
        unsafeCPSet(t.m_unsafeCP_, ch);
        }
    }
        
    // StringBuffer reversed = new StringBuffer();
        m_utilStringBuffer_.delete(0, m_utilStringBuffer_.length());
    for (int j = 0; j < size; j ++) { 
        // prefixes are going to be looked up backwards
        // therefore, we will promptly reverse the prefix buffer...
        int offset = element.m_prefixChars_.length() - j - 1;
        m_utilStringBuffer_.append(element.m_prefixChars_.charAt(offset));
    }
    element.m_prefixChars_ = m_utilStringBuffer_.toString();
    element.m_prefix_ = 0;
    
    // the first codepoint is also unsafe, as it forms a 'contraction' with 
    // the prefix
    if (!UTF16.isTrailSurrogate(element.m_cPoints_.charAt(0))) {
        unsafeCPSet(t.m_unsafeCP_, element.m_cPoints_.charAt(0));
    }
        
    element.m_cPoints_ = element.m_prefixChars_;
    element.m_cPointsOffset_ = element.m_prefix_;
    
    // Add the last char of the contraction to the contraction-end hash 
    // table. unless it is a trail surrogate, which is handled 
    // algorithmically and shouldn't be in the table
    if (!UTF16.isTrailSurrogate(
                    element.m_cPoints_.charAt(element.m_cPoints_.length() - 1))) {
        ContrEndCPSet(t.m_contrEndCP_, element.m_cPoints_.charAt(
                                     element.m_cPoints_.length() - 1));
    }
    // First we need to check if contractions starts with a surrogate
    // int cp = UTF16.charAt(element.m_cPoints_, element.m_cPointsOffset_);
    
    // If there are any Jamos in the contraction, we should turn on special 
    // processing for Jamos
    if (isJamo(element.m_prefixChars_.charAt(element.m_prefix_))) {
        t.m_collator_.m_isJamoSpecial_ = true;
    }
    // then we need to deal with it 
    // we could aready have something in table - or we might not 
    if (!isPrefix(CE)) { 
        // if it wasn't contraction, we wouldn't end up here
        int firstContractionOffset = addContraction(contractions, 
                            CONTRACTION_TABLE_NEW_ELEMENT_, 
                            (char)0, CE);
        int newCE = processContraction(contractions, element, 
                       CE_NOT_FOUND_);
        addContraction(contractions, firstContractionOffset, 
                           element.m_prefixChars_.charAt(element.m_prefix_), 
                           newCE);
        addContraction(contractions, firstContractionOffset, (char)0xFFFF, 
                           CE);
        CE = constructSpecialCE(CE_SPEC_PROC_TAG_, firstContractionOffset);
    } 
    else { 
        // we are adding to existing contraction 
        // there were already some elements in the table, so we need to add 
        // a new contraction 
        // Two things can happen here: either the codepoint is already in 
        // the table, or it is not
        char ch = element.m_prefixChars_.charAt(element.m_prefix_);
        int position = findCP(contractions, CE, ch);
        if (position > 0) {       
        // if it is we just continue down the chain 
        int eCE = getCE(contractions, CE, position);
        int newCE = processContraction(contractions, element, eCE);
        setContraction(contractions, CE, position, ch, newCE);
        } 
        else {                  
        // if it isn't, we will have to create a new sequence 
        processContraction(contractions, element, CE_NOT_FOUND_);
        insertContraction(contractions, CE, ch, element.m_mapCE_);
        }
    }
    
    element.m_cPoints_ = oldCP;
    element.m_cPointsOffset_ = oldCPOffset;
    
    return CE;
    }
    
    /**
     * Checks if the argument ce is a contraction
     * @param CE collation element
     * @return true if argument ce is a contraction
     */
    private static final boolean isContraction(int CE) 
    {
    return isSpecial(CE) && (getCETag(CE) == CE_CONTRACTION_TAG_);
    }
    
    /**
     * Checks if the argument ce has a prefix
     * @param CE collation element
     * @return true if argument ce has a prefix
     */
    private static final boolean isPrefix(int CE) 
    {
    return isSpecial(CE) && (getCETag(CE) == CE_SPEC_PROC_TAG_);
    }
    
    /**
     * Checks if the argument ce is special
     * @param CE collation element
     * @return true if argument ce is special
     */
    private static final boolean isSpecial(int CE) 
    {
    return (CE & RuleBasedCollator.CE_SPECIAL_FLAG_) == 0xF0000000;
    }
    
    /**
     * Checks if the argument ce has a prefix
     * @param CE collation element
     * @return true if argument ce has a prefix
     */
    private static final int getCETag(int CE) 
    {
    return (CE & RuleBasedCollator.CE_TAG_MASK_) >>> 
        RuleBasedCollator.CE_TAG_SHIFT_;
    }
    
    /**
     * Gets the ce at position in contraction table
     * @param table contraction table
     * @param position offset to the contraction table
     * @return ce
     */
    private static final int getCE(ContractionTable table, int element, 
                   int position) 
    {
    element &= 0xFFFFFF;
        BasicContractionTable tbl = getBasicContractionTable(table, element);
        
        if (tbl == null) {
            return CE_NOT_FOUND_;
        }
    if (position > tbl.m_CEs_.size() || position == -1) {
        return CE_NOT_FOUND_;
    } 
    else {
        return ((Integer)tbl.m_CEs_.get(position)).intValue();
    }
    }
    
    /**
     * Sets the unsafe character
     * @param table unsafe table
     * @param c character to be added
     */
    private static final void unsafeCPSet(byte table[], char c) 
    {
    int hash = c;
    if (hash >= (UNSAFECP_TABLE_SIZE_ << 3)) {
        if (hash >= 0xd800 && hash <= 0xf8ff) {
        // Part of a surrogate, or in private use area. 
        // These don't go in the table                            
        return;
        }
        hash = (hash & UNSAFECP_TABLE_MASK_) + 256;
    }
    table[hash >> 3] |= (1 << (hash & 7));
    }
    
    /**
     * Sets the contraction end character
     * @param table contraction end table
     * @param c character to be added
     */
    private static final void ContrEndCPSet(byte table[], char c) 
    {
    int hash = c;
    if (hash >= (UNSAFECP_TABLE_SIZE_ << 3)) {
        hash = (hash & UNSAFECP_TABLE_MASK_) + 256;
    }
    table[hash >> 3] |= (1 << (hash & 7));
    }
    
    /** 
     * Adds more contractions in table. If element is non existant, it creates 
     * on. Returns element handle 
     * @param table contraction table
     * @param element offset to the contraction table
     * @param codePoint codepoint to add
     * @param value
     * @return collation element
     */
    private static int addContraction(ContractionTable table, int element, 
                                      char codePoint, int value) 
    {
    BasicContractionTable tbl = getBasicContractionTable(table, element);
    if (tbl == null) {
        tbl = addAContractionElement(table);
        element = table.m_elements_.size() - 1;
    } 
    
    tbl.m_CEs_.add(new Integer(value));
    tbl.m_codePoints_.append(codePoint);
    return constructSpecialCE(table.m_currentTag_, element);
    }

    /**
     * Adds a contraction element to the table
     * @param table contraction table to update
     * @return contraction 
     */
    private static BasicContractionTable addAContractionElement(
                                ContractionTable table) 
    {
    BasicContractionTable result = new BasicContractionTable();
    table.m_elements_.add(result);
    return result;
    }

    /**
     * Constructs a special ce
     * @param tag special tag
     * @param CE collation element 
     * @return a contraction ce
     */
    private static final int constructSpecialCE(int tag, int CE) 
    {
    return RuleBasedCollator.CE_SPECIAL_FLAG_ 
        | (tag << RuleBasedCollator.CE_TAG_SHIFT_) | (CE & 0xFFFFFF);
    }
    
    /**
     * Sets and inserts the element that has a contraction
     * @param contractions contraction table 
     * @param element contracting element
     * @param existingCE
     * @return contraction ce
     */
    private static int processContraction(ContractionTable contractions, 
                      Elements element, 
                      int existingCE) 
    {
    int firstContractionOffset = 0;
    // end of recursion 
    if (element.m_cPoints_.length() - element.m_cPointsOffset_ == 1) {
        if (isContractionTableElement(existingCE) 
        && getCETag(existingCE) == contractions.m_currentTag_) {
        changeContraction(contractions, existingCE, (char)0, 
                  element.m_mapCE_);
        changeContraction(contractions, existingCE, (char)0xFFFF,
                                  element.m_mapCE_);
        return existingCE;
        } 
        else {
        // can't do just that. existingCe might be a contraction, 
        // meaning that we need to do another step
        return element.m_mapCE_; 
        }
    }
    
    // this recursion currently feeds on the only element we have... 
    // We will have to copy it in order to accomodate for both backward 
    // and forward cycles
    // we encountered either an empty space or a non-contraction element 
    // this means we are constructing a new contraction sequence 
    element.m_cPointsOffset_ ++;
    if (!isContractionTableElement(existingCE)) { 
        // if it wasn't contraction, we wouldn't end up here
        firstContractionOffset = addContraction(contractions, 
                            CONTRACTION_TABLE_NEW_ELEMENT_, 
                            (char)0, existingCE);
        int newCE = processContraction(contractions, element, 
                       CE_NOT_FOUND_);
        addContraction(contractions, firstContractionOffset, 
               element.m_cPoints_.charAt(element.m_cPointsOffset_), 
               newCE);
        addContraction(contractions, firstContractionOffset, 
               (char)0xFFFF, existingCE);
        existingCE = constructSpecialCE(contractions.m_currentTag_, 
                        firstContractionOffset);
    } 
    else { 
        // we are adding to existing contraction
        // there were already some elements in the table, so we need to add 
        // a new contraction 
        // Two things can happen here: either the codepoint is already in 
        // the table, or it is not
        int position = findCP(contractions, existingCE, 
                  element.m_cPoints_.charAt(element.m_cPointsOffset_));
        if (position > 0) {       
        // if it is we just continue down the chain 
        int eCE = getCE(contractions, existingCE, position);
        int newCE = processContraction(contractions, element, eCE);
        setContraction(contractions, existingCE, position, 
                           element.m_cPoints_.charAt(element.m_cPointsOffset_), 
                   newCE);
        } 
        else {  
        // if it isn't, we will have to create a new sequence 
        int newCE = processContraction(contractions, element, 
                           CE_NOT_FOUND_);
        insertContraction(contractions, existingCE, 
                  element.m_cPoints_.charAt(element.m_cPointsOffset_), 
                  newCE);
        }
    }
    element.m_cPointsOffset_ --;
    return existingCE;
    }
    
    /**
     * Checks if CE belongs to the contraction table
     * @param CE collation element to test
     * @return true if CE belongs to the contraction table
     */
    private static final boolean isContractionTableElement(int CE) 
    { 
    return isSpecial(CE) 
        && (getCETag(CE) == CE_CONTRACTION_TAG_
        || getCETag(CE) == CE_SPEC_PROC_TAG_);
    }
    
    /**
     * Gets the codepoint 
     * @param table contraction table
     * @param element offset to the contraction element in the table
     * @param codePoint code point to look for
     * @return the offset to the code point
     */
    private static int findCP(ContractionTable table, int element, 
                  char codePoint) 
    {
    BasicContractionTable tbl = getBasicContractionTable(table, element);
    if (tbl == null) {
        return -1;
    }
    
    int position = 0;
    while (codePoint > tbl.m_codePoints_.charAt(position)) {
        position ++;
        if (position > tbl.m_codePoints_.length()) {
        return -1;
        }
    }
    if (codePoint == tbl.m_codePoints_.charAt(position)) {
        return position;
    } 
    else {
        return -1;
    }
    }

    /**
     * Gets the contraction element out of the contraction table
     * @param table contraction table
     * @param offset to the element in the contraction table
     * @return basic contraction element at offset in the contraction table
     */
    private static final BasicContractionTable getBasicContractionTable(
                                    ContractionTable table,
                                    int offset) 
    {
        offset &= 0xFFFFFF;
        if (offset == 0xFFFFFF) {
        return null;
        }
    return (BasicContractionTable)table.m_elements_.get(offset);
    }
    
    /**
     * Changes the contraction element
     * @param table contraction table
     * @param element offset to the element in the contraction table
     * @param codePoint codepoint 
     * @param newCE new collation element
     * @return basic contraction element at offset in the contraction table
     */
    private static final int changeContraction(ContractionTable table, 
                                               int element, char codePoint, 
                                               int newCE) 
    {
    BasicContractionTable tbl = getBasicContractionTable(table, element);    
    if (tbl == null) {
        return 0;
    }
    int position = 0;
    while (codePoint > tbl.m_codePoints_.charAt(position)) {
        position ++;
        if (position > tbl.m_codePoints_.length()) {
        return CE_NOT_FOUND_;
        }
    }
    if (codePoint == tbl.m_codePoints_.charAt(position)) {
        tbl.m_CEs_.set(position, new Integer(newCE));
        return element & 0xFFFFFF;
    } 
    else {
        return CE_NOT_FOUND_;
    }
    }
    
    /** 
     * Sets a part of contraction sequence in table. If element is non 
     * existant, it creates on. Returns element handle.
     * @param table contraction table
     * @param element offset to the contraction table
     * @param offset
     * @param codePoint contraction character
     * @param value ce value
     * @return new contraction ce
     */
    private static final int setContraction(ContractionTable table, 
                                            int element, int offset, 
                                            char codePoint, int value) 
    {
        element &= 0xFFFFFF;
    BasicContractionTable tbl = getBasicContractionTable(table, element);    
    if (tbl == null) {
        tbl = addAContractionElement(table);
        element = table.m_elements_.size() - 1;
    }
    
    tbl.m_CEs_.set(offset, new Integer(value));
    tbl.m_codePoints_.setCharAt(offset, codePoint);
    return constructSpecialCE(table.m_currentTag_, element);
    }
    
    /** 
     * Inserts a part of contraction sequence in table. Sequences behind the 
     * offset are moved back. If element is non existent, it creates on. 
     * @param table contraction
     * @param element offset to the table contraction
     * @param codePoint code point
     * @param value collation element value
     * @return contraction collation element
     */
    private static final int insertContraction(ContractionTable table, 
                                               int element, char codePoint, 
                                               int value) 
    {
    element &= 0xFFFFFF;
    BasicContractionTable tbl = getBasicContractionTable(table, element);
    if (tbl == null) {
        tbl = addAContractionElement(table);
        element = table.m_elements_.size() - 1;
    }
    
    int offset = 0;
    while (tbl.m_codePoints_.charAt(offset) < codePoint 
           && offset < tbl.m_codePoints_.length()) {
        offset ++;
    }
    
    tbl.m_CEs_.insertElementAt(new Integer(value), offset);
    tbl.m_codePoints_.insert(offset, codePoint);
    
    return constructSpecialCE(table.m_currentTag_, element);
    }
    
    /**
     * Finalize addition
     * @param t build table
     * @param element to add
     */
    private final static int finalizeAddition(BuildTable t, Elements element) 
    {
    int CE = CE_NOT_FOUND_;
        // This should add a completely ignorable element to the  
        // unsafe table, so that backward iteration will skip 
        // over it when treating contractions. 
        if (element.m_mapCE_ == 0) { 
            for (int i = 0; i < element.m_cPoints_.length(); i ++) { 
                char ch = element.m_cPoints_.charAt(i);
                if (!UTF16.isTrailSurrogate(ch)) { 
                    unsafeCPSet(t.m_unsafeCP_, ch); 
                } 
            } 
        } 

    if (element.m_cPoints_.length() - element.m_cPointsOffset_ > 1) { 
        // we're adding a contraction
        int cp = UTF16.charAt(element.m_cPoints_, element.m_cPointsOffset_);
        CE = t.m_mapping_.getValue(cp);
        CE = addContraction(t, CE, element);
    } 
    else { 
        // easy case
        CE = t.m_mapping_.getValue(element.m_cPoints_.charAt(
                                 element.m_cPointsOffset_));
        
        if (CE != CE_NOT_FOUND_) {
        if(isContractionTableElement(CE)) { 
            // adding a non contraction element (thai, expansion, 
            // single) to already existing contraction 
            if (!isPrefix(element.m_mapCE_)) { 
            // we cannot reenter prefix elements - as we are going 
            // to create a dead loop
            // Only expansions and regular CEs can go here... 
            // Contractions will never happen in this place
            setContraction(t.m_contractions_, CE, 0, (char)0, 
                       element.m_mapCE_);
            // This loop has to change the CE at the end of 
            // contraction REDO!
            changeLastCE(t.m_contractions_, CE, element.m_mapCE_);
            }
        } 
        else {
            t.m_mapping_.setValue(element.m_cPoints_.charAt(
                                    element.m_cPointsOffset_), 
                      element.m_mapCE_);
        }
        } 
        else {
        t.m_mapping_.setValue(element.m_cPoints_.charAt(
                                element.m_cPointsOffset_), 
                                      element.m_mapCE_);
        }
    }
    return CE;
    }
    
    /** 
     * Note regarding surrogate handling: We are interested only in the single
     * or leading surrogates in a contraction. If a surrogate is somewhere else
     * in the contraction, it is going to be handled as a pair of code units,
     * as it doesn't affect the performance AND handling surrogates specially
     * would complicate code way too much.
     */
    private static int addContraction(BuildTable t, int CE, Elements element) 
    {
    ContractionTable contractions = t.m_contractions_;
    contractions.m_currentTag_ = CE_CONTRACTION_TAG_;
    
    // First we need to check if contractions starts with a surrogate
    int cp = UTF16.charAt(element.m_cPoints_, 0);
    int cpsize = 1;
    if (UCharacter.isSupplementary(cp)) {
        cpsize = 2;
    }
    if (cpsize < element.m_cPoints_.length()) { 
        // This is a real contraction, if there are other characters after 
        // the first
        int size = element.m_cPoints_.length() - element.m_cPointsOffset_;
        for (int j = 1; j < size; j ++) {   
        // First add contraction chars to unsafe CP hash table 
        // Unless it is a trail surrogate, which is handled 
        // algoritmically and shouldn't take up space in the table.
        if (!UTF16.isTrailSurrogate(element.m_cPoints_.charAt(
                                      element.m_cPointsOffset_ + j))) {
            unsafeCPSet(t.m_unsafeCP_, 
                element.m_cPoints_.charAt(
                              element.m_cPointsOffset_ + j));
        }
        }
        // Add the last char of the contraction to the contraction-end 
        // hash table. unless it is a trail surrogate, which is handled 
        // algorithmically and shouldn't be in the table
        if (!UTF16.isTrailSurrogate(element.m_cPoints_.charAt(
                                  element.m_cPoints_.length() -1))) {
        ContrEndCPSet(t.m_contrEndCP_, 
                  element.m_cPoints_.charAt(
                            element.m_cPoints_.length() -1));
        }
    
        // If there are any Jamos in the contraction, we should turn on 
        // special processing for Jamos
        if (isJamo(element.m_cPoints_.charAt(element.m_cPointsOffset_))) {
        t.m_collator_.m_isJamoSpecial_ = true;
        }
        // then we need to deal with it 
        // we could aready have something in table - or we might not 
        element.m_cPointsOffset_ += cpsize;
        if (!isContraction(CE)) { 
        // if it wasn't contraction, we wouldn't end up here
        int firstContractionOffset = addContraction(contractions, 
                                CONTRACTION_TABLE_NEW_ELEMENT_, (char)0, CE);
        int newCE = processContraction(contractions, element, 
                           CE_NOT_FOUND_);
        addContraction(contractions, firstContractionOffset, 
                   element.m_cPoints_.charAt(element.m_cPointsOffset_), 
                   newCE);
        addContraction(contractions, firstContractionOffset, 
                               (char)0xFFFF, CE);
        CE = constructSpecialCE(CE_CONTRACTION_TAG_, 
                    firstContractionOffset);
        } 
        else { 
        // we are adding to existing contraction 
        // there were already some elements in the table, so we need to 
        // add a new contraction
        // Two things can happen here: either the codepoint is already 
        // in the table, or it is not 
        int position = findCP(contractions, CE, 
                      element.m_cPoints_.charAt(element.m_cPointsOffset_));
        if (position > 0) {       
            // if it is we just continue down the chain
            int eCE = getCE(contractions, CE, position);
            int newCE = processContraction(contractions, element, eCE);
            setContraction(contractions, CE, position, 
                   element.m_cPoints_.charAt(element.m_cPointsOffset_), 
                   newCE);
        } 
        else {                  
            // if it isn't, we will have to create a new sequence 
            int newCE = processContraction(contractions, element, 
                           CE_NOT_FOUND_);
            insertContraction(contractions, CE, 
                      element.m_cPoints_.charAt(element.m_cPointsOffset_), 
                                      newCE);
        }
        }
        element.m_cPointsOffset_ -= cpsize;
        t.m_mapping_.setValue(cp, CE);
    } 
    else if (!isContraction(CE)) { 
        // this is just a surrogate, and there is no contraction 
        t.m_mapping_.setValue(cp, element.m_mapCE_);
    } 
    else { 
        // fill out the first stage of the contraction with the surrogate 
        // CE 
        changeContraction(contractions, CE, (char)0, element.m_mapCE_);
        changeContraction(contractions, CE, (char)0xFFFF, element.m_mapCE_);
    }
    return CE;
    }
    
    /** 
     * this is for adding non contractions 
     * @param table contraction table
     * @param element offset to the contraction table
     * @param value collation element value
     * @return new collation element 
     */
    private static final int changeLastCE(ContractionTable table, int element, 
                                          int value) 
    {
    BasicContractionTable tbl = getBasicContractionTable(table, element);
    if (tbl == null) {
        return 0;
    }
    
    tbl.m_CEs_.set(tbl.m_CEs_.size() - 1, new Integer(value));
    return constructSpecialCE(table.m_currentTag_, element & 0xFFFFFF);
    }
    
    /**
     * Given a set of ranges calculated by allocWeights(), iterate through the 
     * weights. Sets the next weight in cegenerator.m_current_.
     * @param cegenerator object that contains ranges weight range array and
     *        its rangeCount
     * @return the next weight
     */
    private static int nextWeight(CEGenerator cegenerator) 
    {
        if (cegenerator.m_rangesLength_ > 0) {
            // get maxByte from the .count field
            int maxByte = cegenerator.m_ranges_[0].m_count_;
            // get the next weight 
            int weight = cegenerator.m_ranges_[0].m_start_;
            if (weight == cegenerator.m_ranges_[0].m_end_) {
                // this range is finished, remove it and move the following 
                // ones up 
                cegenerator.m_rangesLength_ --;
                if (cegenerator.m_rangesLength_ > 0) {
                    System.arraycopy(cegenerator.m_ranges_, 1, 
                                     cegenerator.m_ranges_, 0, 
                                     cegenerator.m_rangesLength_);
                    cegenerator.m_ranges_[0].m_count_ = maxByte; 
                    // keep maxByte in ranges[0]
                }
            } 
            else {
                // increment the weight for the next value
                cegenerator.m_ranges_[0].m_start_ 
            = incWeight(weight, cegenerator.m_ranges_[0].m_length2_, 
                maxByte);
            }
            return weight;
        }
        return -1;
    }
    
    /**
     * Increment the collation weight
     * @param weight to increment
     * @param length
     * @param maxByte
     * @return new incremented weight
     */
    private static final int incWeight(int weight, int length, int maxByte) 
    {
        while (true) {
            int b = getWeightByte(weight, length);
            if (b < maxByte) {
                return setWeightByte(weight, length, b + 1);
            } 
            else {
                // roll over, set this byte to BYTE_FIRST_TAILORED_ and 
                // increment the previous one
                weight = setWeightByte(weight, length, 
                                       RuleBasedCollator.BYTE_FIRST_TAILORED_);
                -- length;
            }
        }
    }
    
    /**
     * Gets the weight byte
     * @param weight
     * @param index
     * @return byte
     */
    private static final int getWeightByte(int weight, int index) 
    {
        return (weight >> ((4 - index) << 3)) & 0xff;
    }
    
    /**
     * Set the weight byte in table
     * @param weight 
     * @param index
     * @param b byte
     */
    private static final int setWeightByte(int weight, int index, int b) 
    {
        index <<= 3;
        // 0xffffffff except a 00 "hole" for the index-th byte
        int mask = 0xffffffff >>> index;
        index = 32 - index;
        mask |= 0xffffff00 << index;
        return (weight & mask) | (b << index);
    }
    
    /**
     * Call getWeightRanges and then determine heuristically which ranges to 
     * use for a given number of weights between (excluding) two limits
     * @param lowerLimit
     * @param upperLimit
     * @param n
     * @param maxByte
     * @param ranges
     * @return
     */
    private int allocateWeights(int lowerLimit, int upperLimit, int n,
                                int maxByte, WeightRange ranges[]) 
    {
        // number of usable byte values 3..maxByte
        int countBytes = maxByte - RuleBasedCollator.BYTE_FIRST_TAILORED_ + 1;
        // [0] unused, [5] to make index checks unnecessary, m_utilCountBuffer_
        // countBytes to the power of index, m_utilLongBuffer_ for unsignedness
        // gcc requires explicit initialization 
        m_utilLongBuffer_[0] = 1;
        m_utilLongBuffer_[1] = countBytes;
        m_utilLongBuffer_[2] = m_utilLongBuffer_[1] * countBytes;
        m_utilLongBuffer_[3] = m_utilLongBuffer_[2] * countBytes;
        m_utilLongBuffer_[4] = m_utilLongBuffer_[3] * countBytes;
        int rangeCount = getWeightRanges(lowerLimit, upperLimit, maxByte, 
                                         countBytes, ranges);
        if (rangeCount <= 0) {
            return 0;
        }
        // what is the maximum number of weights with these ranges?
        long maxCount = 0;
        for (int i = 0; i < rangeCount; ++ i) {
            maxCount += (long)ranges[i].m_count_ 
        * m_utilLongBuffer_[4 - ranges[i].m_length_];
        }
        if (maxCount < n) {
            return 0;
        }
        // set the length2 and count2 fields
        for (int i = 0; i < rangeCount; ++ i) {
            ranges[i].m_length2_ = ranges[i].m_length_;
            ranges[i].m_count2_ = ranges[i].m_count_;
        }
        // try until we find suitably large ranges
        while (true) {
            // get the smallest number of bytes in a range
            int minLength = ranges[0].m_length2_;
            // sum up the number of elements that fit into ranges of each byte 
            // length
            Arrays.fill(m_utilCountBuffer_, 0);
            for (int i = 0; i < rangeCount; ++ i) {
                m_utilCountBuffer_[ranges[i].m_length2_] += ranges[i].m_count2_;
            }
            // now try to allocate n elements in the available short ranges 
            if (n <= m_utilCountBuffer_[minLength] 
        + m_utilCountBuffer_[minLength + 1]) {
                // trivial cases, use the first few ranges
                maxCount = 0;
                rangeCount = 0;
                do {
                    maxCount += ranges[rangeCount].m_count2_;
                    ++ rangeCount;
                } while (n > maxCount);
                break;
            } 
            else if (n <= ranges[0].m_count2_ * countBytes) {
                // easy case, just make this one range large enough by 
                // lengthening it once more, possibly split it
                rangeCount = 1;
                // calculate how to split the range between maxLength-1 
                // (count1) and maxLength (count2) 
                long power_1 
            = m_utilLongBuffer_[minLength - ranges[0].m_length_];
                long power = power_1 * countBytes;
                int count2 = (int)((n + power - 1) / power);
                int count1 = ranges[0].m_count_ - count2;
                // split the range
                if (count1 < 1) {
                    // lengthen the entire range to maxLength 
                    lengthenRange(ranges, 0, maxByte, countBytes);
                } 
                else {
                    // really split the range
                    // create a new range with the end and initial and current 
                    // length of the old one
                    rangeCount = 2;
                    ranges[1].m_end_ = ranges[0].m_end_;
                    ranges[1].m_length_ = ranges[0].m_length_;
                    ranges[1].m_length2_ = minLength;
                    // set the end of the first range according to count1
                    int i = ranges[0].m_length_;
                    int b = getWeightByte(ranges[0].m_start_, i) + count1 - 1;
                    // ranges[0].count and count1 may be >countBytes from 
                    // merging adjacent ranges; b > maxByte is possible
                    if (b <= maxByte) {
                        ranges[0].m_end_ = setWeightByte(ranges[0].m_start_, i, 
                                                         b);
                    } 
                    else {
                        ranges[0].m_end_ = setWeightByte(
                             incWeight(ranges[0].m_start_, i - 1, 
                                   maxByte), 
                             i, b - countBytes);
                    }
                    // set the bytes in the end weight at length + 1..length2 
                    // to maxByte
                    b = (maxByte << 24) | (maxByte << 16) | (maxByte << 8)
                        | maxByte; // this used to be 0xffffffff 
                    ranges[0].m_end_ = truncateWeight(ranges[0].m_end_, i) 
            | (b >>> (i << 3)) 
            & (b << ((4 - minLength) << 3));
                    // set the start of the second range to immediately follow 
                    // the end of the first one
                    ranges[1].m_start_ = incWeight(ranges[0].m_end_, minLength, 
                                                   maxByte);
                    // set the count values (informational)
                    ranges[0].m_count_ = count1;
                    ranges[1].m_count_ = count2;
    
                    ranges[0].m_count2_ = (int)(count1 * power_1);
                    // will be *countBytes when lengthened 
                    ranges[1].m_count2_ = (int)(count2 * power_1); 
    
                    // lengthen the second range to maxLength
                    lengthenRange(ranges, 1, maxByte, countBytes);
                }
                break;
            }
            // no good match, lengthen all minLength ranges and iterate 
            for (int i=0; ranges[i].m_length2_ == minLength; ++ i) {
                lengthenRange(ranges, i, maxByte, countBytes);
            }
        }
    
        if (rangeCount > 1) {
            // sort the ranges by weight values 
            Arrays.sort(ranges, 0, rangeCount);
        }
    
        // set maxByte in ranges[0] for ucol_nextWeight()
        ranges[0].m_count_ = maxByte;
    
        return rangeCount;
    }
    
    /**
     * Updates the range length
     * @param range weight range array
     * @param offset to weight range array
     * @param maxByte
     * @param countBytes
     * @return new length
     */
    private static final int lengthenRange(WeightRange range[], int offset, 
                                           int maxByte, int countBytes) 
    {
        int length = range[offset].m_length2_ + 1;
        range[offset].m_start_ = setWeightTrail(range[offset].m_start_, length, 
                        RuleBasedCollator.BYTE_FIRST_TAILORED_);
        range[offset].m_end_ = setWeightTrail(range[offset].m_end_, length, 
                                              maxByte);
        range[offset].m_count2_ *= countBytes;
        range[offset].m_length2_ = length;
        return length;
    }
    
    /**
     * Gets the weight 
     * @param weight
     * @param length
     * @param trail
     * @return new weight
     */
    private static final int setWeightTrail(int weight, int length, int trail) 
    {
        length = (4 - length) << 3;
        return (weight & (0xffffff00 << length)) | (trail << length);
    }
    
    /**
     * take two CE weights and calculate the
     * possible ranges of weights between the two limits, excluding them
     * for weights with up to 4 bytes there are up to 2*4-1=7 ranges
     * @param lowerLimit
     * @param upperLimit
     * @param maxByte
     * @param countBytes
     * @param ranges
     * @return weight ranges
     */
    private int getWeightRanges(int lowerLimit, int upperLimit, int maxByte, 
                                int countBytes, WeightRange ranges[]) 
    {
        // assume that both lowerLimit & upperLimit are not 0 
        // get the lengths of the limits 
        int lowerLength = lengthOfWeight(lowerLimit);
        int upperLength = lengthOfWeight(upperLimit);
        if (Utility.compareUnsigned(lowerLimit, upperLimit) >= 0) {
            return 0;
        }
        // check that neither is a prefix of the other
        if (lowerLength < upperLength) {
            if (lowerLimit == truncateWeight(upperLimit, lowerLength)) {
                return 0;
            }
        }
        // if the upper limit is a prefix of the lower limit then the earlier 
        // test lowerLimit >= upperLimit has caught it
        // reset local variables
        // With the limit lengths of 1..4, there are up to 7 ranges for 
        // allocation:
        // range     minimum length
        // lower[4]  4
        // lower[3]  3
        // lower[2]  2
        // middle    1
        // upper[2]  2
        // upper[3]  3
        // upper[4]  4
        // We are now going to calculate up to 7 ranges.
        // Some of them will typically overlap, so we will then have to merge 
        // and eliminate ranges.
        
        // We have to clean cruft from previous invocations
        // before doing anything. C++ already does that
        for(int length = 0; length < 5; length++) {
            m_utilLowerWeightRange_[length].clear();
            m_utilUpperWeightRange_[length].clear();
        }
        m_utilWeightRange_.clear();
        
        int weight = lowerLimit;
        for (int length = lowerLength; length >= 2; -- length) {
            m_utilLowerWeightRange_[length].clear();
            int trail = getWeightByte(weight, length);
            if (trail < maxByte) {
                m_utilLowerWeightRange_[length].m_start_ 
            = incWeightTrail(weight, length);
                m_utilLowerWeightRange_[length].m_end_ 
            = setWeightTrail(weight, length, maxByte);
                m_utilLowerWeightRange_[length].m_length_ = length;
                m_utilLowerWeightRange_[length].m_count_ = maxByte - trail;
            }
            weight = truncateWeight(weight, length - 1);
        }
        m_utilWeightRange_.m_start_ = incWeightTrail(weight, 1);
    
        weight = upperLimit;
        // [0] and [1] are not used - this simplifies indexing, 
        // m_utilUpperWeightRange_
        
        for (int length = upperLength; length >= 2; length --) {
            int trail = getWeightByte(weight, length);
            if (trail > RuleBasedCollator.BYTE_FIRST_TAILORED_) {
                m_utilUpperWeightRange_[length].m_start_ 
            = setWeightTrail(weight, length, 
                     RuleBasedCollator.BYTE_FIRST_TAILORED_);
                m_utilUpperWeightRange_[length].m_end_ 
            = decWeightTrail(weight, length);
                m_utilUpperWeightRange_[length].m_length_ = length;
                m_utilUpperWeightRange_[length].m_count_ = trail
            - RuleBasedCollator.BYTE_FIRST_TAILORED_;
            }
            weight = truncateWeight(weight, length - 1);
        }
        m_utilWeightRange_.m_end_ = decWeightTrail(weight, 1);
    
        // set the middle range
        m_utilWeightRange_.m_length_ = 1;
        if (Utility.compareUnsigned(m_utilWeightRange_.m_end_, m_utilWeightRange_.m_start_) >= 0) {
        //if (m_utilWeightRange_.m_end_ >= m_utilWeightRange_.m_start_) {
            m_utilWeightRange_.m_count_ 
        = ((m_utilWeightRange_.m_end_ - m_utilWeightRange_.m_start_) 
           >>> 24) + 1;
        } 
        else {
            // eliminate overlaps
            // remove the middle range
            m_utilWeightRange_.m_count_ = 0;
            // reduce or remove the lower ranges that go beyond upperLimit
            for (int length = 4; length >= 2; -- length) {
                if (m_utilLowerWeightRange_[length].m_count_ > 0 
                    && m_utilUpperWeightRange_[length].m_count_ > 0) {
                    int start = m_utilUpperWeightRange_[length].m_start_;
                    int end = m_utilLowerWeightRange_[length].m_end_;
                    if (end >= start || incWeight(end, length, maxByte) 
            == start) {
                        // lower and upper ranges collide or are directly 
                        // adjacent: merge these two and remove all shorter 
                        // ranges
                        start = m_utilLowerWeightRange_[length].m_start_;
                        end = m_utilLowerWeightRange_[length].m_end_ 
                            = m_utilUpperWeightRange_[length].m_end_;
                        // merging directly adjacent ranges needs to subtract 
                        // the 0/1 gaps in between;
                        // it may result in a range with count>countBytes
                        m_utilLowerWeightRange_[length].m_count_ 
                = getWeightByte(end, length)
                - getWeightByte(start, length) + 1 
                + countBytes * (getWeightByte(end, length - 1)
                        - getWeightByte(start, 
                                length - 1));
                        m_utilUpperWeightRange_[length].m_count_ = 0;
                        while (-- length >= 2) {
                            m_utilLowerWeightRange_[length].m_count_ 
                                = m_utilUpperWeightRange_[length].m_count_ = 0;
                        }
                        break;
                    }
                }
            }
        }
    
        // copy the ranges, shortest first, into the result array 
        int rangeCount = 0;
        if (m_utilWeightRange_.m_count_ > 0) {
            ranges[0] = new WeightRange(m_utilWeightRange_);
            rangeCount = 1;
        }
        for (int length = 2; length <= 4; ++ length) {
            // copy upper first so that later the middle range is more likely 
            // the first one to use
            if (m_utilUpperWeightRange_[length].m_count_ > 0) {
                ranges[rangeCount] 
            = new WeightRange(m_utilUpperWeightRange_[length]);
                ++ rangeCount;
            }
            if (m_utilLowerWeightRange_[length].m_count_ > 0) {
                ranges[rangeCount] 
            = new WeightRange(m_utilLowerWeightRange_[length]);
                ++ rangeCount;
            }
        }
        return rangeCount;
    }
    
    /**
     * Truncates the weight with length
     * @param weight
     * @param length
     * @return truncated weight
     */
    private static final int truncateWeight(int weight, int length) 
    {
        return weight & (0xffffffff << ((4 - length) << 3));
    }
    
    /**
     * Length of the weight
     * @param weight
     * @return length of the weight
     */
    private static final int lengthOfWeight(int weight) 
    {
        if ((weight & 0xffffff) == 0) {
            return 1;
        } 
        else if ((weight & 0xffff) == 0) {
            return 2;
        } 
        else if ((weight & 0xff) == 0) {
            return 3;
        } 
        return 4;
    }
    
    /**
     * Increment the weight trail
     * @param weight 
     * @param length
     * @return new weight
     */
    private static final int incWeightTrail(int weight, int length) 
    {
        return weight + (1 << ((4-length) << 3));
    }

    /**
     * Decrement the weight trail
     * @param weight 
     * @param length
     * @return new weight
     */
    private static int decWeightTrail(int weight, int length) 
    {
        return weight - (1 << ((4 - length) << 3));
    }
    
    /**
     * Gets the codepoint 
     * @param tbl contraction table
     * @param codePoint code point to look for
     * @return the offset to the code point
     */
    private static int findCP(BasicContractionTable tbl, char codePoint) 
    {
        int position = 0;
        while (codePoint > tbl.m_codePoints_.charAt(position)) {
            position ++;
            if (position > tbl.m_codePoints_.length()) {
                return -1;
            }
        }
        if (codePoint == tbl.m_codePoints_.charAt(position)) {
            return position;
        } 
        else {
            return -1;
        }
    }

    /**
     * Finds a contraction ce
     * @param table
     * @param element
     * @param ch
     * @return ce
     */
    private static int findCE(ContractionTable table, int element, char ch) 
    {
        if (table == null) {
            return CE_NOT_FOUND_;
        }
        BasicContractionTable tbl = getBasicContractionTable(table, element);
        if (tbl == null) {
            return CE_NOT_FOUND_;
        }
        int position = findCP(tbl, ch);
        if (position > tbl.m_CEs_.size() || position < 0) {
            return CE_NOT_FOUND_;
        } 
        return ((Integer)tbl.m_CEs_.get(position)).intValue();
    }    
    
    /**
     * Checks if the string is tailored in the contraction
     * @param table contraction table
     * @param element 
     * @param array character array to check
     * @param offset array offset
     * @return true if it is tailored
     */
    private static boolean isTailored(ContractionTable table, int element, 
                                      char array[], int offset) 
    {
        while (array[offset] != 0) {
            element = findCE(table, element, array[offset]);
            if (element == CE_NOT_FOUND_) {
                return false;
            }
            if (!isContractionTableElement(element)) {
                return true;
            }
            offset ++;
        }
        if (getCE(table, element, 0) != CE_NOT_FOUND_) {
            return true;
        } 
        else {
            return false; 
        }
    }
    
    /**
     * Assemble RuleBasedCollator
     * @param t build table
     * @param collator to update
     */
    private void assembleTable(BuildTable t, RuleBasedCollator collator) 
    {
        IntTrieBuilder mapping = t.m_mapping_;
        Vector expansions = t.m_expansions_;
        ContractionTable contractions = t.m_contractions_;
        MaxExpansionTable maxexpansion = t.m_maxExpansions_;
        
        // contraction offset has to be in since we are building on the 
        // UCA contractions 
        // int beforeContractions = (HEADER_SIZE_ 
        //                         + paddedsize(expansions.size() << 2)) >>> 1;
        collator.m_contractionOffset_ = 0;
        int contractionsSize = constructTable(contractions);
        
        // the following operation depends on the trie data. Therefore, we have 
        // to do it before the trie is compacted 
        // sets jamo expansions
        getMaxExpansionJamo(mapping, maxexpansion, t.m_maxJamoExpansions_,
                            collator.m_isJamoSpecial_);
        
        // TODO: LATIN1 array is now in the utrie - it should be removed from 
        // the calculation
        setAttributes(collator, t.m_options_);
        // copy expansions
        int size = expansions.size();
        collator.m_expansion_ = new int[size];
        for (int i = 0; i < size; i ++) {
            collator.m_expansion_[i] = ((Integer)expansions.get(i)).intValue();
        }
        // contractions block 
        if (contractionsSize != 0) {
            // copy contraction index 
            collator.m_contractionIndex_ = new char[contractionsSize];
            contractions.m_codePoints_.getChars(0, contractionsSize, 
                                                collator.m_contractionIndex_, 
                                                0);
            // copy contraction collation elements
            collator.m_contractionCE_ = new int[contractionsSize];
            for (int i = 0; i < contractionsSize; i ++) {
                collator.m_contractionCE_[i] = ((Integer)
                        contractions.m_CEs_.get(i)).intValue();
            }
        }
        // copy mapping table
        collator.m_trie_ = mapping.serialize(t, 
                         RuleBasedCollator.DataManipulate.getInstance());
        // copy max expansion table
        // not copying the first element which is a dummy
        // to be in synch with icu4c's builder, we continue to use the 
        // expansion offset
        // omitting expansion offset in builder
        collator.m_expansionOffset_ = 0; 
        size = maxexpansion.m_endExpansionCE_.size();
        collator.m_expansionEndCE_ = new int[size - 1];
        for (int i = 1; i < size; i ++) {
            collator.m_expansionEndCE_[i - 1] = ((Integer)
                         maxexpansion.m_endExpansionCE_.get(i)).intValue();
        }
        collator.m_expansionEndCEMaxSize_ = new byte[size - 1];
        for (int i = 1; i < size; i ++) {
            collator.m_expansionEndCEMaxSize_[i - 1] 
        = ((Byte)maxexpansion.m_expansionCESize_.get(i)).byteValue();
        }
        // Unsafe chars table.  Finish it off, then copy it.
        unsafeCPAddCCNZ(t);
        // Or in unsafebits from UCA, making a combined table.
        for (int i = 0; i < UNSAFECP_TABLE_SIZE_; i ++) {    
        t.m_unsafeCP_[i] |= RuleBasedCollator.UCA_.m_unsafe_[i];
        }
        collator.m_unsafe_ = t.m_unsafeCP_;
    
        // Finish building Contraction Ending chars hash table and then copy it 
        // out.
        // Or in unsafebits from UCA, making a combined table
        for (int i = 0; i < UNSAFECP_TABLE_SIZE_; i ++) {    
        t.m_contrEndCP_[i] |= RuleBasedCollator.UCA_.m_contractionEnd_[i];
        }
        collator.m_contractionEnd_ = t.m_contrEndCP_;
    }
    
    /**
     * Sets this collator to use the all options and tables in UCA. 
     * @param collator which attribute is to be set 
     * @param option to set with
     */
    private static final void setAttributes(RuleBasedCollator collator,
                        CollationRuleParser.OptionSet option)
    {
        collator.latinOneFailed_ = true;
        collator.m_caseFirst_ = option.m_caseFirst_;
        collator.setDecomposition(option.m_decomposition_);
        collator.setAlternateHandlingShifted(
                         option.m_isAlternateHandlingShifted_);
        collator.setCaseLevel(option.m_isCaseLevel_);
        collator.setFrenchCollation(option.m_isFrenchCollation_);
        collator.m_isHiragana4_ = option.m_isHiragana4_;
        collator.setStrength(option.m_strength_);
        collator.m_variableTopValue_ = option.m_variableTopValue_;    
        collator.latinOneFailed_ = false;
    }
    
    /**
     * Constructing the contraction table
     * @param table contraction table
     * @return 
     */
    private int constructTable(ContractionTable table) 
    {
        // See how much memory we need 
        int tsize = table.m_elements_.size();
        if (tsize == 0) {
            return 0;
        }
        table.m_offsets_.clear();
        int position = 0;
        for (int i = 0; i < tsize; i ++) {
            table.m_offsets_.add(new Integer(position));
            position += ((BasicContractionTable)
             table.m_elements_.get(i)).m_CEs_.size();
        }
        table.m_CEs_.clear();
        table.m_codePoints_.delete(0, table.m_codePoints_.length());
        // Now stuff the things in
        StringBuffer cpPointer = table.m_codePoints_;
        Vector CEPointer = table.m_CEs_;
        for (int i = 0; i < tsize; i ++) {
            BasicContractionTable bct = (BasicContractionTable)
        table.m_elements_.get(i);
            int size = bct.m_CEs_.size();
            char ccMax = 0;
            char ccMin = 255;
            int offset = CEPointer.size();
            CEPointer.add(bct.m_CEs_.get(0));
            for (int j = 1; j < size; j ++) {
                char ch = bct.m_codePoints_.charAt(j);
                char cc = (char)(UCharacter.getCombiningClass(ch) & 0xFF);
                if (cc > ccMax) {
                    ccMax = cc;
                }
                if (cc < ccMin) {
                    ccMin = cc;
                }
                cpPointer.append(ch);
                CEPointer.add(bct.m_CEs_.get(j));
            }
            cpPointer.insert(offset, 
                             (char)(((ccMin == ccMax) ? 1 : 0 << 8) | ccMax));
            for (int j = 0; j < size; j ++) {
                if (isContractionTableElement(((Integer)
                           CEPointer.get(offset + j)).intValue())) {
                    int ce = ((Integer)CEPointer.get(offset + j)).intValue();
                    CEPointer.set(offset + j, 
                  new Integer(constructSpecialCE(getCETag(ce), 
                                 ((Integer)table.m_offsets_.get(
                                                getContractionOffset(ce))).intValue())));
                }
            }
        }
    
        for (int i = 0; i <= 0x10FFFF; i ++) {
            int CE = table.m_mapping_.getValue(i);
            if (isContractionTableElement(CE)) {
                CE = constructSpecialCE(getCETag(CE), 
                                        ((Integer)table.m_offsets_.get(
                                       getContractionOffset(CE))).intValue());
                table.m_mapping_.setValue(i, CE);
            }
        }
        return position;
    }
    
    /**
     * Get contraction offset
     * @param ce collation element 
     * @return contraction offset
     */
    private static final int getContractionOffset(int ce)
    {
        return ce & 0xFFFFFF;
    }
    
    /**
     * Gets the maximum Jamo expansion
     * @param mapping trie table
     * @param maxexpansion maximum expansion table
     * @param maxjamoexpansion maximum jamo expansion table
     * @param jamospecial is jamo special?
     */
    private static void getMaxExpansionJamo(IntTrieBuilder mapping, 
                                            MaxExpansionTable maxexpansion,
                                            MaxJamoExpansionTable 
                        maxjamoexpansion,
                                            boolean jamospecial)
    {
        int VBASE  = 0x1161;
        int TBASE  = 0x11A8;
        int VCOUNT = 21;
        int TCOUNT = 28;
        int v = VBASE + VCOUNT - 1;
        int t = TBASE + TCOUNT - 1;
        
        while (v >= VBASE) {
            int ce = mapping.getValue(v);
            if ((ce & RuleBasedCollator.CE_SPECIAL_FLAG_) 
        != RuleBasedCollator.CE_SPECIAL_FLAG_) {
                setMaxExpansion(ce, (byte)2, maxexpansion);
            }
            v --;
        }
        
        while (t >= TBASE)
        {
        int ce = mapping.getValue(t);
        if ((ce & RuleBasedCollator.CE_SPECIAL_FLAG_) 
            != RuleBasedCollator.CE_SPECIAL_FLAG_) {
            setMaxExpansion(ce, (byte)3, maxexpansion);
        }
        t --;
        }
        // According to the docs, 99% of the time, the Jamo will not be special 
        if (jamospecial) {
            // gets the max expansion in all unicode characters
            int count = maxjamoexpansion.m_endExpansionCE_.size();
            byte maxTSize = (byte)(maxjamoexpansion.m_maxLSize_ + 
                                   maxjamoexpansion.m_maxVSize_ +
                                   maxjamoexpansion.m_maxTSize_);
            byte maxVSize = (byte)(maxjamoexpansion.m_maxLSize_ + 
                                   maxjamoexpansion.m_maxVSize_);
        
            while (count > 0) {
                count --;
                if (((Boolean)maxjamoexpansion.m_isV_.get(count)).booleanValue()
            == true) {
                    setMaxExpansion(((Integer)
                     maxjamoexpansion.m_endExpansionCE_.get(count)).intValue(), 
                    maxVSize, maxexpansion);
                }
                else {
                    setMaxExpansion(((Integer)
                     maxjamoexpansion.m_endExpansionCE_.get(count)).intValue(), 
                    maxTSize, maxexpansion);
                }
            }
        }
    }
    
    /**  
     * To the UnsafeCP hash table, add all chars with combining class != 0     
     * @param t build table
     */
    private static final void unsafeCPAddCCNZ(BuildTable t) 
    {
        boolean buildCMTable = ( buildCMTabFlag & (t.cmLookup==null) );
        char[]  cm = null;  // combining mark array
        int[]   index= new int[256];
        int     count=0;

        if (buildCMTable) {
            cm = new char[0x10000];
        }
        for (char c = 0; c < 0xffff; c ++) {
            char fcd = NormalizerImpl.getFCD16(c);
            if (fcd >= 0x100 || // if the leading combining class(c) > 0 ||
                (UTF16.isLeadSurrogate(c) && fcd != 0)) {
                // c is a leading surrogate with some FCD data
                unsafeCPSet(t.m_unsafeCP_, c);
                if ( buildCMTable && (fcd!=0)) {
                    int cc = (fcd& 0xff);
                    int pos = (cc<<8)+index[cc];
                    cm[pos] = c;
                    index[cc]++;
                    count++;
                }
            }
        }
    
        if (t.m_prefixLookup_ != null) {
            Enumeration els = t.m_prefixLookup_.elements();
            while (els.hasMoreElements()) {
                Elements e = (Elements)els.nextElement();
                // codepoints here are in the NFD form. We need to add the
                // first code point of the NFC form to unsafe, because 
                // strcoll needs to backup over them.
                // weiv: This is wrong! See the comment above.
                //String decomp = Normalizer.decompose(e.m_cPoints_, true);
                //unsafeCPSet(t.m_unsafeCP_, decomp.charAt(0));
                // it should be:
                String comp = Normalizer.compose(e.m_cPoints_, false);
                unsafeCPSet(t.m_unsafeCP_, comp.charAt(0));
            } 
        }
        
        if (buildCMTable) {
            t.cmLookup = new CombinClassTable();
            t.cmLookup.generate(cm, count, index);
        }
    }
    
    /**
     * Create closure
     * @param t build table
     * @param collator RuleBasedCollator
     * @param colEl collation element iterator
     * @param start 
     * @param limit
     * @param type character type
     * @return 
     */
    private boolean enumCategoryRangeClosureCategory(BuildTable t, 
                             RuleBasedCollator collator, 
                             CollationElementIterator colEl, 
                             int start, int limit, int type) 
    {
        if (type != UCharacterCategory.UNASSIGNED 
            && type != UCharacterCategory.PRIVATE_USE) { 
            // if the range is assigned - we might ommit more categories later
            
            for (int u32 = start; u32 < limit; u32 ++) {
                int noOfDec = NormalizerImpl.getDecomposition(u32, false,
                                                              m_utilCharBuffer_, 
                                                              0, 256);
                if (noOfDec > 0) {
                    // if we're positive, that means there is no decomposition
                    String comp = UCharacter.toString(u32);
                    String decomp = new String(m_utilCharBuffer_, 0, noOfDec);
                    if (!collator.equals(comp, decomp)) {
                        m_utilElement_.m_cPoints_ = decomp;
                        m_utilElement_.m_prefix_ = 0;
                        Elements prefix 
                = (Elements)t.m_prefixLookup_.get(m_utilElement_);
                        if (prefix == null) {
                            m_utilElement_.m_cPoints_ = comp;
                            m_utilElement_.m_prefix_ = 0;
                            m_utilElement_.m_prefixChars_ = null;
                            colEl.setText(decomp);
                            int ce = colEl.next();
                            m_utilElement_.m_CELength_ = 0;
                            while (ce != CollationElementIterator.NULLORDER) {
                                m_utilElement_.m_CEs_[
                              m_utilElement_.m_CELength_ ++] 
                    = ce;
                                ce = colEl.next();
                            }
                        } 
                        else {
                            m_utilElement_.m_cPoints_ = comp;
                            m_utilElement_.m_prefix_ = 0;
                            m_utilElement_.m_prefixChars_ = null;
                            m_utilElement_.m_CELength_ = 1;
                            m_utilElement_.m_CEs_[0] = prefix.m_mapCE_;
                            // This character uses a prefix. We have to add it 
                            // to the unsafe table, as it decomposed form is 
                            // already in. In Japanese, this happens for \u309e 
                            // & \u30fe
                            // Since unsafeCPSet is static in ucol_elm, we are 
                            // going to wrap it up in the unsafeCPAddCCNZ 
                            // function
                        }
                        addAnElement(t, m_utilElement_);
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Determine if a character is a Jamo
     * @param ch character to test
     * @return true if ch is a Jamo, false otherwise
     */
    private static final boolean isJamo(char ch)
    { 
    return (ch >= 0x1100 && ch <= 0x1112) 
        || (ch >= 0x1175 && ch <= 0x1161) 
        || (ch >= 0x11A8 && ch <= 0x11C2);
    }
    
    /**
     * Produces canonical closure
     */
    private void canonicalClosure(BuildTable t) 
    {
        BuildTable temp = new BuildTable(t);
        assembleTable(temp, temp.m_collator_);
        // produce canonical closure 
        CollationElementIterator coleiter 
        = temp.m_collator_.getCollationElementIterator("");
        RangeValueIterator typeiter = UCharacter.getTypeIterator();
        RangeValueIterator.Element element = new RangeValueIterator.Element();
        while (typeiter.next(element)) {
            enumCategoryRangeClosureCategory(t, temp.m_collator_, coleiter, 
                         element.start, element.limit, 
                         element.value);
        }
        
        t.cmLookup = temp.cmLookup;
        temp.cmLookup = null;
        
        for (int i = 0; i < m_parser_.m_resultLength_; i ++) {
            char baseChar, firstCM;
            // now we need to generate the CEs 
            // We stuff the initial value in the buffers, and increase the 
            // appropriate buffer according to strength                                                          */
            // createElements(t, m_parser_.m_listHeader_[i]);
            CollationRuleParser.Token tok = m_parser_.m_listHeader_[i].m_first_;
            m_utilElement_.clear();
            while ( tok!=null ) {
                m_utilElement_.m_prefix_ = 0;// el.m_prefixChars_;
                m_utilElement_.m_cPointsOffset_ = 0; //el.m_uchars_;
                if (tok.m_prefix_ != 0) { 
                // we will just copy the prefix here, and adjust accordingly in 
                // the addPrefix function in ucol_elm. The reason is that we 
                // need to add both composed AND decomposed elements to the 
                // unsafe table.
                int size = tok.m_prefix_ >> 24;
                int offset = tok.m_prefix_ & 0x00FFFFFF;
                m_utilElement_.m_prefixChars_ 
                    = m_parser_.m_source_.substring(offset, offset + size);
                size = (tok.m_source_ >> 24) - (tok.m_prefix_ >> 24); 
                offset = (tok.m_source_ & 0x00FFFFFF) + (tok.m_prefix_ >> 24);
                m_utilElement_.m_uchars_ 
                    = m_parser_.m_source_.substring(offset, offset + size);
                } 
                else {
                m_utilElement_.m_prefixChars_ = null;
                int offset = tok.m_source_ & 0x00FFFFFF;
                int size = tok.m_source_ >>> 24;
                m_utilElement_.m_uchars_ = m_parser_.m_source_.substring(offset, 
                                             offset + size);
                }
                m_utilElement_.m_cPoints_ = m_utilElement_.m_uchars_;
                
                baseChar = firstCM = 0;  //reset
                for (int j = 0; j < m_utilElement_.m_cPoints_.length() 
                     - m_utilElement_.m_cPointsOffset_; j ++) {

                    char fcd = NormalizerImpl.getFCD16(m_utilElement_.m_cPoints_.charAt(j));
                    if ( (fcd & 0xff) == 0 ) {
                        baseChar = m_utilElement_.m_cPoints_.charAt(j);
                    }
                    else {
                        if ( (baseChar!=0) && (firstCM==0) ) {
                            firstCM = m_utilElement_.m_cPoints_.charAt(j); // first combining mark
                        }
                    }
                }
                
                if ( (baseChar!=0) && (firstCM!=0)) {
                      addTailCanonicalClosures(t, temp.m_collator_, coleiter, baseChar, firstCM);
                }
                tok = tok.m_next_;
            }
        }
    }
    
    private void addTailCanonicalClosures(BuildTable t,
            RuleBasedCollator m_collator, 
            CollationElementIterator colEl,
            char baseChar, 
            char cMark) {
        if ( t.cmLookup == null ) {
            return;
        }
        CombinClassTable cmLookup = t.cmLookup;  
        int[] index = cmLookup.index;
        int cClass =  NormalizerImpl.getFCD16(cMark) & 0xff;
        int maxIndex=0;
        char[] precompCh = new char[256];  
        int[] precompClass = new int[256];
        int precompLen=0;
        Elements element = new Elements();
        
        if ( cClass>0 ) {
            maxIndex = index[cClass-1];
        }
        for (int i=0; i<maxIndex; i++) {
            StringBuffer decompBuf = new StringBuffer();
            decompBuf.append(baseChar).append(cmLookup.cPoints[i]);
            String comp = Normalizer.compose(decompBuf.toString(), false);
            if ( comp.length() == 1 ) {
                precompCh[precompLen] = comp.charAt(0);
                precompClass[precompLen] = 
                    (NormalizerImpl.getFCD16(cmLookup.cPoints[i]) & 0xff);
                precompLen++;
                StringBuffer decomp = new StringBuffer();
                for (int j=0; j < m_utilElement_.m_cPoints_.length(); j++) {
                    if ( m_utilElement_.m_cPoints_.charAt(j) == cMark ) {
                        decomp.append(cmLookup.cPoints[i]);
                    }
                    else {
                        decomp.append(m_utilElement_.m_cPoints_.charAt(j));
                    }
                }
                comp = Normalizer.compose(decomp.toString(), false);
                StringBuffer buf= new StringBuffer(comp);
                buf.append(cMark);
                decomp.append(cMark);
                comp = buf.toString();
                
                element.m_cPoints_ = decomp.toString();
                element.m_CELength_ = 0;
                element.m_prefix_ = 0;
                Elements prefix = (Elements)t.m_prefixLookup_.get(element);
                element.m_cPoints_ = comp;
                element.m_uchars_ = comp;
                
                if (prefix == null) {
                    element.m_prefix_ = 0;
                    element.m_prefixChars_ = null;
                    colEl.setText(decomp.toString());
                    int ce = colEl.next();
                    element.m_CELength_ = 0;
                    while (ce != CollationElementIterator.NULLORDER) {
                        element.m_CEs_[element.m_CELength_ ++] = ce;
                        ce = colEl.next();
                    }
                } 
                else {
                    element.m_cPoints_ = comp;
                    element.m_prefix_ = 0;
                    element.m_prefixChars_ = null;
                    element.m_CELength_ = 1;
                    element.m_CEs_[0] = prefix.m_mapCE_;
                }
                setMapCE(t, element);
                finalizeAddition(t, element);
                
                if (comp.length()>2) {
                    // This is a fix for tailoring contractions with accented
                    // character at the end of contraction string.
                    addFCD4AccentedContractions(t, colEl, comp, element);
                }
                if ( precompLen > 1 ) {
                    precompLen = addMultiCMontractions(t, colEl, element, precompCh, 
                                 precompClass, precompLen, cMark, i, decomp.toString());
                }
            }
        }
        
    }
    
    private void setMapCE(BuildTable t, Elements element) {
        Vector expansions = t.m_expansions_;
        element.m_mapCE_ = 0;
        
        if (element.m_CELength_ == 2 // a two CE expansion 
        && RuleBasedCollator.isContinuation(element.m_CEs_[1]) 
        && (element.m_CEs_[1] 
            & (~(0xFF << 24 | RuleBasedCollator.CE_CONTINUATION_MARKER_))) == 0 // that has only primaries in continuation
        && (((element.m_CEs_[0] >> 8) & 0xFF) == RuleBasedCollator.BYTE_COMMON_) 
        // a common secondary
        && ((element.m_CEs_[0] & 0xFF) == RuleBasedCollator.BYTE_COMMON_)) { // and a common tertiary
        
            element.m_mapCE_ = RuleBasedCollator.CE_SPECIAL_FLAG_ 
                // a long primary special
                | (CE_LONG_PRIMARY_TAG_ << 24) 
                // first and second byte of primary
                | ((element.m_CEs_[0] >> 8) & 0xFFFF00) 
                // third byte of primary
                | ((element.m_CEs_[1] >> 24) & 0xFF);   
        } 
        else {
                // omitting expansion offset in builder
                // (HEADER_SIZE_ >> 2)
            int expansion = RuleBasedCollator.CE_SPECIAL_FLAG_ 
                | (CE_EXPANSION_TAG_ << RuleBasedCollator.CE_TAG_SHIFT_) 
                | (addExpansion(expansions, element.m_CEs_[0]) << 4) & 0xFFFFF0;
    
            for (int i = 1; i < element.m_CELength_; i ++) {
                addExpansion(expansions, element.m_CEs_[i]);
            }
            if (element.m_CELength_ <= 0xF) {
                expansion |= element.m_CELength_;
            } 
            else {
                addExpansion(expansions, 0);
            }
            element.m_mapCE_ = expansion;
            setMaxExpansion(element.m_CEs_[element.m_CELength_ - 1],
                    (byte)element.m_CELength_, t.m_maxExpansions_);
        }
    }
    
    private int addMultiCMontractions(BuildTable t,
            CollationElementIterator colEl,
            Elements element,
            char[] precompCh,
            int[] precompClass,
            int maxComp,
            char cMark,
            int cmPos,
            String decomp) {
        
        CombinClassTable cmLookup = t.cmLookup;
        char[] combiningMarks = {cMark};
        int cMarkClass = (int)(UCharacter.getCombiningClass(cMark) & 0xFF);
        String comMark = new String(combiningMarks);
        int  noOfPrecomposedChs = maxComp; 
        
        for (int j=0; j<maxComp; j++) {
            int count = 0;
            StringBuffer temp;
           
            do {
                String newDecomp, comp;
                
                if ( count==0 ) { // Decompose the saved precomposed char.
                    newDecomp = Normalizer.decompose( new String(precompCh, j ,1), false);
                    temp = new StringBuffer(newDecomp);
                    temp.append(cmLookup.cPoints[cmPos]);
                    newDecomp = temp.toString();
                }
                else {
                    temp = new StringBuffer(decomp);
                    temp.append(precompCh[j]);
                    newDecomp = temp.toString();
                }
                comp = Normalizer.compose(newDecomp, false);
                if (comp.length()==1) {
                    temp.append(cMark);
                    element.m_cPoints_ = temp.toString();
                    element.m_CELength_ = 0;
                    element.m_prefix_ = 0;
                    Elements prefix = (Elements)t.m_prefixLookup_.get(element);
                    element.m_cPoints_ = comp+comMark;
                    if (prefix == null) {
                        element.m_prefix_ = 0;
                        element.m_prefixChars_ = null;
                        colEl.setText(temp.toString());
                        int ce = colEl.next();
                        element.m_CELength_ = 0;
                        while (ce != CollationElementIterator.NULLORDER) {
                            element.m_CEs_[element.m_CELength_ ++] = ce;
                            ce = colEl.next();
                        }
                    } 
                    else {
                        element.m_cPoints_ = comp;
                        element.m_prefix_ = 0;
                        element.m_prefixChars_ = null;
                        element.m_CELength_ = 1;
                        element.m_CEs_[0] = prefix.m_mapCE_;
                    }
                    setMapCE(t, element);
                    finalizeAddition(t, element);
                    precompCh[noOfPrecomposedChs] = comp.charAt(0);
                    precompClass[noOfPrecomposedChs] = cMarkClass;  
                    noOfPrecomposedChs++;
                }
            }while(++count<2 && (precompClass[j]==cMarkClass)); 
        }
        return noOfPrecomposedChs;
    }

    private void addFCD4AccentedContractions(BuildTable t, 
            CollationElementIterator colEl,
            String data,
            Elements element) {
        String decomp = Normalizer.decompose(data, false);
        String comp = Normalizer.compose(data, false);
        
        element.m_cPoints_ = decomp;
        element.m_CELength_ = 0;
        element.m_prefix_ = 0;
        Elements prefix = (Elements)t.m_prefixLookup_.get(element);
        if (prefix == null) {
            element.m_cPoints_ = comp;
            element.m_prefix_ = 0;
            element.m_prefixChars_ = null;
            element.m_CELength_ = 0;
            colEl.setText(decomp);
            int ce = colEl.next();
            element.m_CELength_ = 0;
            while (ce != CollationElementIterator.NULLORDER) {
                element.m_CEs_[element.m_CELength_ ++] = ce;
                ce = colEl.next();
            }
            addAnElement(t, element);
        } 
    }
    
    private void processUCACompleteIgnorables(BuildTable t) 
    {
        TrieIterator trieiterator 
        = new TrieIterator(RuleBasedCollator.UCA_.m_trie_);
        RangeValueIterator.Element element = new RangeValueIterator.Element();
        while (trieiterator.next(element)) {
            int start = element.start;
            int limit = element.limit;
            if (element.value == 0) {
                while (start < limit) {
                    int CE = t.m_mapping_.getValue(start);
                    if (CE == CE_NOT_FOUND_) {
                        m_utilElement_.m_prefix_ = 0;
                        m_utilElement_.m_uchars_ = UCharacter.toString(start);
                        m_utilElement_.m_cPoints_ = m_utilElement_.m_uchars_;
                        m_utilElement_.m_cPointsOffset_ = 0;
                        m_utilElement_.m_CELength_ = 1;
                        m_utilElement_.m_CEs_[0] = 0;
                        addAnElement(t, m_utilElement_);
                    }
                    start ++;
                }
            }
        }
    }
}
