/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UnifiedBinaryProperty.java,v $
* $Date: 2001/10/25 20:37:09 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;

import com.ibm.text.utility.*;

final class UnifiedBinaryProperty implements UCD_Types {
    UCD ucd;
    DerivedProperty dp;
    
    UnifiedBinaryProperty(UCD ucdin) {
        ucd = ucdin;
        dp = new DerivedProperty(ucd);
    }
    
    public String getPropertyName(int propMask, byte style) {
        if (style < LONG) return UCD_Names.ABB_UNIFIED_PROPERTIES[propMask>>8];
        else return UCD_Names.SHORT_UNIFIED_PROPERTIES[propMask>>8];
    }
    
    public boolean isTest(int propMask) {
        int enum = propMask >> 8;
        propMask &= 0xFF;
        if (enum != (DERIVED>>8)) return false;
        return dp.isTest(propMask);
    }
    
    /**
     * @return unified property number
     */
    public boolean isDefined(int propMask) {
        int enum = propMask >> 8;
        propMask &= 0xFF;
        switch (enum) {
          case CATEGORY>>8: return propMask != UNUSED_CATEGORY && propMask < LIMIT_CATEGORY;
          case COMBINING_CLASS>>8: return true;
          // ucd.isCombiningClassUsed((byte)propMask) 
          //  || !ucd.getCombiningID_fromIndex ((byte)propMask, SHORT).startsWith("Fixed");
          case BIDI_CLASS>>8: return propMask != BIDI_UNUSED && propMask < LIMIT_BIDI_CLASS;
          case DECOMPOSITION_TYPE>>8: return propMask < LIMIT_DECOMPOSITION_TYPE;
          case NUMERIC_TYPE>>8: return propMask < LIMIT_NUMERIC_TYPE;
          case EAST_ASIAN_WIDTH>>8: return propMask < LIMIT_EAST_ASIAN_WIDTH;
          case LINE_BREAK>>8: return propMask < LIMIT_LINE_BREAK;
          case JOINING_TYPE>>8: return propMask < LIMIT_JOINING_TYPE;
          case JOINING_GROUP>>8: return propMask < LIMIT_JOINING_GROUP;
          case BINARY_PROPERTIES>>8: return propMask < LIMIT_BINARY_PROPERTIES;
          case SCRIPT>>8: return propMask != UNUSED_SCRIPT && propMask < LIMIT_SCRIPT;
          case AGE>>8: return propMask < LIMIT_AGE;
          case DERIVED>>8: return dp.isDefined(propMask);
          default: return false;
        }
    }

    public boolean get(int cp, int propMask) {
        int enum = propMask >> 8;
        propMask &= 0xFF;
        switch (enum) {
          case CATEGORY>>8: if (propMask >= LIMIT_CATEGORY) break;
            return ucd.getCategory(cp) == propMask;
          case COMBINING_CLASS>>8: if (propMask >= LIMIT_COMBINING_CLASS) break;
            return ucd.getCombiningClass(cp) == propMask;
          case BIDI_CLASS>>8: if (propMask >= LIMIT_BIDI_CLASS) break;
            return ucd.getBidiClass(cp) == propMask;
          case DECOMPOSITION_TYPE>>8: if (propMask >= LIMIT_DECOMPOSITION_TYPE) break;
            return ucd.getDecompositionType(cp) == propMask;
          case NUMERIC_TYPE>>8: if (propMask >= LIMIT_NUMERIC_TYPE) break;
            return ucd.getNumericType(cp) == propMask;
          case EAST_ASIAN_WIDTH>>8: if (propMask >= LIMIT_EAST_ASIAN_WIDTH) break;
            return ucd.getEastAsianWidth(cp) == propMask;
          case LINE_BREAK>>8:  if (propMask >= LIMIT_LINE_BREAK) break;
            return ucd.getLineBreak(cp) == propMask;
          case JOINING_TYPE>>8: if (propMask >= LIMIT_JOINING_TYPE) break;
            return ucd.getJoiningType(cp) == propMask;
          case JOINING_GROUP>>8: if (propMask >= LIMIT_JOINING_GROUP) break;
            return ucd.getJoiningGroup(cp) == propMask;
          case BINARY_PROPERTIES>>8: if (propMask >= LIMIT_BINARY_PROPERTIES) break;
            return ucd.getBinaryProperty(cp, propMask);
          case SCRIPT>>8: if (propMask >= LIMIT_SCRIPT) break;
            return ucd.getScript(cp) == propMask;
          case AGE>>8: if (propMask >= LIMIT_AGE) break;
            return ucd.getAge(cp) == propMask;
          case DERIVED>>8: if (!dp.isDefined(propMask)) break;
            return dp.hasProperty(cp, propMask);
        }
        throw new ChainException("Illegal property Number {0}", new Object[]{new Integer(propMask)});
    }

    public String getID(int unifiedPropMask) {
        return getID(unifiedPropMask, NORMAL);
    }
/*
    public static String getID(UCD ucd, int unifiedPropMask) {
        String longOne = getID(ucd, unifiedPropMask, LONG);
        String shortOne = getID(ucd, unifiedPropMask, SHORT);
        if (longOne.equals(shortOne)) return longOne;
        return shortOne + "(" + longOne + ")";
    }
*/
    public String getFullID(int unifiedPropMask, byte style) {
        String pre = "";
        if ((unifiedPropMask & 0xFF00) != BINARY_PROPERTIES) {
            String preShort = UCD_Names.ABB_UNIFIED_PROPERTIES[unifiedPropMask>>8] + "=";
            String preLong = UCD_Names.SHORT_UNIFIED_PROPERTIES[unifiedPropMask>>8] + "=";
            if (style < LONG) pre = preShort;
            else if (style == LONG || preShort.equals(preLong)) pre = preLong;
            else pre = preShort + "(" + preLong + ")";
        }
        String shortOne = getID(unifiedPropMask, SHORT);
        if (shortOne.length() == 0) shortOne = "xx";
        String longOne = getID(unifiedPropMask, LONG);
        if (longOne.length() == 0) longOne = "none";

        String post;
        if (style < LONG) post = shortOne;
        else if (style == LONG || shortOne.equals(longOne)) post = longOne;
        else post = shortOne + "(" + longOne + ")";

        if (pre.length() == 0) {
            pre = post + "=";
            post = "T";
        }

        return pre + post;
    }

    public String getID(int unifiedPropMask, byte style) {
        int enum = unifiedPropMask >> 8;
        byte propMask = (byte)unifiedPropMask;
        switch (enum) {
          case CATEGORY>>8: if (propMask >= LIMIT_CATEGORY) break;
            if (style != LONG) return ucd.getCategoryID_fromIndex(propMask);
            return UCD_Names.LONG_GC[propMask];
          case COMBINING_CLASS>>8: if (propMask >= LIMIT_COMBINING_CLASS) break;
            return UCD.getCombiningID_fromIndex((short)(unifiedPropMask & 0xFF), style);
          case BIDI_CLASS>>8: if (propMask >= LIMIT_BIDI_CLASS) break;
            if (style != LONG) return ucd.getBidiClassID_fromIndex(propMask);
            return UCD_Names.LONG_BC[propMask];
          case DECOMPOSITION_TYPE>>8: if (propMask >= LIMIT_DECOMPOSITION_TYPE) break;
            if (style != SHORT) return ucd.getDecompositionTypeID_fromIndex(propMask);
            return UCD_Names.SHORT_DT[propMask];
          case NUMERIC_TYPE>>8: if (propMask >= LIMIT_NUMERIC_TYPE) break;
            if (style != SHORT) return ucd.getNumericTypeID_fromIndex(propMask);
            return UCD_Names.SHORT_NT[propMask];
          case EAST_ASIAN_WIDTH>>8: if (propMask >= LIMIT_EAST_ASIAN_WIDTH) break;
            if (style != LONG) return ucd.getEastAsianWidthID_fromIndex(propMask);
            return UCD_Names.SHORT_EA[propMask];
          case LINE_BREAK>>8:  if (propMask >= LIMIT_LINE_BREAK) break;
            if (style != LONG) return ucd.getLineBreakID_fromIndex(propMask);
            return UCD_Names.LONG_LB[propMask];
          case JOINING_TYPE>>8: if (propMask >= LIMIT_JOINING_TYPE) break;
            if (style != LONG) return ucd.getJoiningTypeID_fromIndex(propMask);
            return UCD_Names.LONG_JOINING_TYPE[propMask];
          case JOINING_GROUP>>8: if (propMask >= LIMIT_JOINING_GROUP) break;
            return ucd.getJoiningGroupID_fromIndex(propMask);
          case BINARY_PROPERTIES>>8: if (propMask >= LIMIT_BINARY_PROPERTIES) break;
            if (style != SHORT) return ucd.getBinaryPropertiesID_fromIndex(propMask);
            return UCD_Names.SHORT_BP[propMask];
          case SCRIPT>>8: if (propMask >= LIMIT_SCRIPT) break;
            if (style != SHORT) return ucd.getScriptID_fromIndex(propMask);
            return UCD_Names.ABB_SCRIPT[propMask];
          case AGE>>8: if (propMask >= LIMIT_AGE) break;
            return ucd.getAgeID_fromIndex(propMask);
          case DERIVED>>8: if (!dp.isDefined(propMask)) break;
            return dp.getName(propMask, style);
        }
        throw new ChainException("Illegal property Number {0}", new Object[]{new Integer(propMask)});
    }
}