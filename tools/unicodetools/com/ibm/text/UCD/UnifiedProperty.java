/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UnifiedProperty.java,v $
* $Date: 2002/10/05 01:28:57 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import java.util.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UnicodeSet;

public final class UnifiedProperty extends UnicodeProperty {
    int majorProp;
    // DerivedProperty dp;
    
    public static UnicodeProperty make(int propMask) {
        return make(propMask, Default.ucd);
    }
    
    public static UnicodeProperty make(int propMask, UCD ucd) {
        if ((propMask & 0xFF00) == (BINARY_PROPERTIES & 0xFF00)) {
            return UnifiedBinaryProperty.make(propMask, ucd);
        }
        if ((propMask & 0xFF00) == DERIVED) {
            return DerivedProperty.make(propMask & 0xFF, ucd);
        }
        if (!isDefined(propMask, ucd)) return null;
        return getCached(propMask, ucd);
    }
    
    public static UnicodeProperty make(String propID, UCD ucd) {
        return make(getPropmask(propID, ucd), ucd);
    }
    
    public static UnicodeSet getSet(int propMask, UCD ucd) {
        UnicodeProperty up = make(propMask, ucd);
        return up.getSet();
    }
    
    public static UnicodeSet getSet(String propID, UCD ucd) {
        return getSet(getPropmask(propID, ucd), ucd);
    }
    
    private static Map propNameCache = null;
    
    public static int getPropmask(String propID, UCD ucd) {
        
        // cache the names
        if (propNameCache == null) {
            System.out.println("Caching Property Names");
            propNameCache = new HashMap();
        
            for (int i = 0; i < LIMIT_ENUM; ++i) {
                UnicodeProperty up = UnifiedProperty.make(i, ucd);
                if (up == null) continue;
                if (!up.isStandard()) continue;
                if (up.getValueType() < BINARY) continue;
                String shortName = Utility.getSkeleton(up.getProperty(SHORT));
                String longName = Utility.getSkeleton(up.getProperty(LONG));
                Integer result = new Integer(i);
                propNameCache.put(longName, result);
                propNameCache.put(shortName, result);
            }
            System.out.println("Done Caching");
        }
        
        propID = Utility.getSkeleton(propID);
        Integer indexObj = (Integer) propNameCache.get(propID);
        if (indexObj == null) {
            throw new IllegalArgumentException("No property found for " + propID);
        }
        return indexObj.intValue();
    }
    
    static Map cache = new HashMap();
    static UCD lastUCD = null;
    static int lastPropMask = -1;
    static UnifiedProperty lastValue = null;
    static Clump probeClump = new Clump();
    
    static class Clump {
        int prop;
        UCD ucd;
        public boolean equals(Object other) {
            Clump that = (Clump) other;
            return (that.prop != prop || !ucd.equals(that));
        }
    }
    
    private static UnifiedProperty getCached(int propMask, UCD ucd) {
        System.out.println(ucd);
        if (ucd.equals(lastUCD) && propMask == lastPropMask) return lastValue;
        probeClump.prop = propMask;
        probeClump.ucd = ucd;
        UnifiedProperty dp = (UnifiedProperty) cache.get(probeClump);
        if (dp == null) {
            dp = new UnifiedProperty(propMask, ucd);
            cache.put(probeClump, dp);
            probeClump = new Clump();
        }
        lastUCD = ucd;
        lastValue = dp;
        lastPropMask = propMask;
        return dp;
    }
    
    /////////////////////////////////
    
    private UnifiedProperty(int propMask, UCD ucdin) {
        ucd = ucdin;
        majorProp = propMask >> 8;
        
        //System.out.println("A: " + getValueType());
        if (majorProp <= (JOINING_GROUP>>8) || majorProp == SCRIPT>>8) setValueType(FLATTENED_BINARY);
        //System.out.println("B: " + getValueType());
        
        header = UCD_Names.UNIFIED_PROPERTY_HEADERS[majorProp];
        name = UCD_Names.UNIFIED_PROPERTIES[majorProp];
        shortName = UCD_Names.SHORT_UNIFIED_PROPERTIES[majorProp];
    }
        
    static private boolean isDefined(int propMask, UCD ucd) {
        int majorProp = propMask >> 8;
        switch (majorProp) {
          case CATEGORY>>8: 
          case COMBINING_CLASS>>8:
          case BIDI_CLASS>>8:
          case DECOMPOSITION_TYPE>>8:
          case NUMERIC_TYPE>>8:
          case EAST_ASIAN_WIDTH>>8:
          case LINE_BREAK>>8: 
          case JOINING_TYPE>>8:
          case JOINING_GROUP>>8:
          case SCRIPT>>8:
          case AGE>>8:
            return true;
            /*
          case DERIVED>>8:
            UnicodeProperty up = DerivedProperty.make(propValue, ucd);
            if (up == null) break;
            return up.hasValue(cp);
            */
        }
        return false;
    }
    
    public boolean hasValue(int cp) {
        throw new ChainException("Can't call 'hasValue' on non-binary property {0}", new Object[]{
                new Integer(majorProp)});
    }

    public String getFullName(byte style) {
        String pre = "";
        String preShort = getProperty(SHORT);
        String preLong = getProperty(LONG);
        if (style < LONG) pre = preShort;
        else if (style == LONG || preShort.equals(preLong)) pre = preLong;
        else pre = preShort + "(" + preLong + ")";
        return pre;
    }

    public String getValue(int cp, byte style) {
        switch (majorProp) {
        case CATEGORY>>8: return ucd.getCategoryID_fromIndex(ucd.getCategory(cp), style);
        case COMBINING_CLASS>>8: return ucd.getCombiningClassID_fromIndex(ucd.getCombiningClass(cp), style);
        case BIDI_CLASS>>8: return ucd.getBidiClassID_fromIndex(ucd.getBidiClass(cp), style);
        case DECOMPOSITION_TYPE>>8: return ucd.getDecompositionTypeID_fromIndex(ucd.getDecompositionType(cp), style);
        case NUMERIC_TYPE>>8: return ucd.getNumericTypeID_fromIndex(ucd.getNumericType(cp), style);
        case EAST_ASIAN_WIDTH>>8: return ucd.getEastAsianWidthID_fromIndex(ucd.getEastAsianWidth(cp), style);
        case LINE_BREAK>>8:  return ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp), style);
        case JOINING_TYPE>>8: return ucd.getJoiningTypeID_fromIndex(ucd.getJoiningType(cp), style);
        case JOINING_GROUP>>8: return ucd.getJoiningGroupID_fromIndex(ucd.getJoiningGroup(cp), style);
        case SCRIPT>>8: return ucd.getScriptID_fromIndex(ucd.getScript(cp), style);
        case AGE>>8: return ucd.getAgeID_fromIndex(ucd.getAge(cp), style);
        default: throw new IllegalArgumentException("Internal Error");
        }
    }
}