/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UnifiedBinaryProperty.java,v $
* $Date: 2002/07/03 02:15:47 $
* $Revision: 1.8 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import java.util.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UnicodeSet;

public final class UnifiedBinaryProperty extends UnicodeProperty {
    int majorProp;
    int propValue;
    // DerivedProperty dp;
    
    public static UnicodeProperty make(int propMask) {
        return make(propMask, Default.ucd);
    }
    
    public static UnicodeProperty make(int propMask, UCD ucd) {
        if ((propMask & 0xFF00) == DERIVED) {
            return DerivedProperty.make(propMask & 0xFF, ucd);
        }
        if (!isDefined(propMask, ucd)) return null;
        return getCached(propMask, ucd);
    }
    
    public static UnicodeProperty make(String propAndValue, UCD ucd) {
        return make(getPropmask(propAndValue, ucd), ucd);
    }
    
    public static UnicodeSet getSet(int propMask, UCD ucd) {
        UnicodeProperty up = make(propMask, ucd);
        return up.getSet();
    }
    
    public static UnicodeSet getSet(String propAndValue, UCD ucd) {
        return getSet(getPropmask(propAndValue, ucd), ucd);
    }
    
    private static Map propNameCache = null;
    
    public static int getPropmask(String propAndValue, UCD ucd) {
        
        // cache the names
        if (propNameCache == null) {
            System.out.println("Caching Property Names");
            propNameCache = new HashMap();
        
            for (int i = 0; i < LIMIT_ENUM; ++i) {
                UnicodeProperty up = UnifiedBinaryProperty.make(i, ucd);
                if (up == null) continue;
                if (!up.isStandard()) continue;
                if (up.getValueType() < BINARY) continue;
                String shortValue = Utility.getSkeleton(up.getValue(SHORT));
                String shortName = Utility.getSkeleton(up.getProperty(SHORT));
                String longValue = Utility.getSkeleton(up.getValue(LONG));
                String longName = Utility.getSkeleton(up.getProperty(LONG));
                Integer result = new Integer(i);
                propNameCache.put(longName + "=" + longValue, result);
                propNameCache.put(longName + "=" + shortValue, result);
                propNameCache.put(shortName + "=" + longValue, result);
                propNameCache.put(shortName + "=" + shortValue, result);
            }
            System.out.println("Done Caching");
        }
        
        propAndValue = Utility.getSkeleton(propAndValue);
        Integer indexObj = (Integer) propNameCache.get(propAndValue);
        if (indexObj == null) {
            throw new IllegalArgumentException("No property found for " + propAndValue);
        }
        return indexObj.intValue();
    }
    
    static Map cache = new HashMap();
    static UCD lastUCD = null;
    static int lastPropMask = -1;
    static UnifiedBinaryProperty lastValue = null;
    static Clump probeClump = new Clump();
    
    static class Clump {
        int prop;
        UCD ucd;
        public boolean equals(Object other) {
            Clump that = (Clump) other;
            return (that.prop != prop || !ucd.equals(that));
        }
    }
    
    private static UnifiedBinaryProperty getCached(int propMask, UCD ucd) {
        if (ucd.equals(lastUCD) && propMask == lastPropMask) return lastValue;
        probeClump.prop = propMask;
        probeClump.ucd = ucd;
        UnifiedBinaryProperty dp = (UnifiedBinaryProperty) cache.get(probeClump);
        if (dp == null) {
            dp = new UnifiedBinaryProperty(propMask, ucd);
            cache.put(probeClump, dp);
            probeClump = new Clump();
        }
        lastUCD = ucd;
        lastValue = dp;
        lastPropMask = propMask;
        return dp;
    }
    
    /////////////////////////////////
    
    private UnifiedBinaryProperty(int propMask, UCD ucdin) {
        ucd = ucdin;
        majorProp = propMask >> 8;
        propValue = propMask & 0xFF;
        
        //System.out.println("A: " + getValueType());
        if (majorProp <= (JOINING_GROUP>>8) || majorProp == SCRIPT>>8) setValueType(FLATTENED_BINARY);
        //System.out.println("B: " + getValueType());
        
        header = UCD_Names.UNIFIED_PROPERTY_HEADERS[majorProp];
        name = UCD_Names.UNIFIED_PROPERTIES[majorProp];
        shortName = UCD_Names.SHORT_UNIFIED_PROPERTIES[majorProp];
        
        valueName = _getValue(LONG);
        shortValueName = _getValue(SHORT);
        numberValueName = _getValue(NUMBER);
        defaultValueStyle = _getDefaultStyle();
        
        if (majorProp == (BINARY_PROPERTIES>>8)) {
            name = valueName;
            shortName = shortValueName;
            defaultPropertyStyle = defaultValueStyle;
            valueName = "YES";
            shortValueName = "Y";
        }
        
        // System.out.println("Value = " + getValue(defaultValueStyle));
        // System.out.println(majorProp + ", " + propValue + ", " + name);
        // dp = new DerivedProperty(ucd);
    }
    
    /*
    public boolean isTest(int propMask) {
        int enum = propMask >> 8;
        propMask &= 0xFF;
        if (enum != (DERIVED>>8)) return false;
        return DerivedProperty.make(propMask, ucd).isTest();
    }
    */
    
    /**
     * @return unified property number
     */
     /*
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
          case DERIVED>>8: 
            UnicodeProperty up = DerivedProperty.make(propMask, ucd);
            return (up != null);
          default: return false;
        }
    }
    */
    
    static private boolean isDefined(int propMask, UCD ucd) {
        int majorProp = propMask >> 8;
        int propValue = propMask & 0xFF;
        switch (majorProp) {
          case CATEGORY>>8: if (propValue >= LIMIT_CATEGORY) break;
            if (propValue == UNUSED_CATEGORY) break;
            return true;
          case COMBINING_CLASS>>8: if (propValue >= LIMIT_COMBINING_CLASS) break;
            return ucd.isCombiningClassUsed((byte)propValue);
          case BIDI_CLASS>>8: if (propValue >= LIMIT_BIDI_CLASS) break;
            if (propValue == BIDI_UNUSED) break;
            return true;
          case DECOMPOSITION_TYPE>>8: if (propValue >= LIMIT_DECOMPOSITION_TYPE) break;
            return true;
          case NUMERIC_TYPE>>8: if (propValue >= LIMIT_NUMERIC_TYPE) break;
            return true;
          case EAST_ASIAN_WIDTH>>8: if (propValue >= LIMIT_EAST_ASIAN_WIDTH) break;
            return true;
          case LINE_BREAK>>8:  if (propValue >= LIMIT_LINE_BREAK) break;
            return true;
          case JOINING_TYPE>>8: if (propValue >= LIMIT_JOINING_TYPE) break;
            return true;
          case JOINING_GROUP>>8: if (propValue >= LIMIT_JOINING_GROUP) break;
            return true;
          case BINARY_PROPERTIES>>8: if (propValue >= LIMIT_BINARY_PROPERTIES) break;
            return true;
          case SCRIPT>>8: if (propValue >= LIMIT_SCRIPT) break;
            if (propValue == UNUSED_SCRIPT) break;
            return true;
          case AGE>>8: if (propValue >= LIMIT_AGE) break;
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
        try {
            switch (majorProp) {
            case CATEGORY>>8: return ucd.getCategory(cp) == propValue;
            case COMBINING_CLASS>>8: return ucd.getCombiningClass(cp) == propValue;
            case BIDI_CLASS>>8: return ucd.getBidiClass(cp) == propValue;
            case DECOMPOSITION_TYPE>>8: return ucd.getDecompositionType(cp) == propValue;
            case NUMERIC_TYPE>>8: return ucd.getNumericType(cp) == propValue;
            case EAST_ASIAN_WIDTH>>8: return ucd.getEastAsianWidth(cp) == propValue;
            case LINE_BREAK>>8: return ucd.getLineBreak(cp) == propValue;
            case JOINING_TYPE>>8: return ucd.getJoiningType(cp) == propValue;
            case JOINING_GROUP>>8: return ucd.getJoiningGroup(cp) == propValue;
            case BINARY_PROPERTIES>>8: return ucd.getBinaryProperty(cp, propValue);
            case SCRIPT>>8: return ucd.getScript(cp) == propValue;
            case AGE>>8: return ucd.getAge(cp) == propValue;
                /*
            case DERIVED>>8:
                UnicodeProperty up = DerivedProperty.make(propValue, ucd);
                if (up == null) break;
                return up.hasValue(cp);
                */
            }
            throw new ChainException("Illegal property Number {0}, {1}", new Object[]{
                    new Integer(majorProp), new Integer(propValue)});
        } catch (RuntimeException e) {
            throw new ChainException("Illegal property Number {0}, {1}", new Object[]{
                 new Integer(majorProp), new Integer(propValue)}, e);
        }
    }

/*
    public static String getID(UCD ucd, int unifiedPropMask) {
        String longOne = getID(ucd, unifiedPropMask, LONG);
        String shortOne = getID(ucd, unifiedPropMask, SHORT);
        if (longOne.equals(shortOne)) return longOne;
        return shortOne + "(" + longOne + ")";
    }
*/
    public String getFullName(byte style) {
        String pre = "";
        if ((majorProp) != BINARY_PROPERTIES>>8) {
            String preShort = getProperty(SHORT) + "=";
            String preLong = getProperty(LONG) + "=";
            if (style < LONG) pre = preShort;
            else if (style == LONG || preShort.equals(preLong)) pre = preLong;
            else pre = preShort + "(" + preLong + ")";
        }
        String shortOne = getValue(SHORT);
        if (shortOne.length() == 0) shortOne = "xx";
        String longOne = getValue(LONG);
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

    public String _getValue(byte style) {
        try {
            switch (majorProp) {
            case CATEGORY>>8: return ucd.getCategoryID_fromIndex((byte)propValue, style);
            case COMBINING_CLASS>>8: return ucd.getCombiningClassID_fromIndex((byte)propValue, style);
            case BIDI_CLASS>>8: return ucd.getBidiClassID_fromIndex((byte)propValue, style);
            case DECOMPOSITION_TYPE>>8: return ucd.getDecompositionTypeID_fromIndex((byte)propValue, style);
            case NUMERIC_TYPE>>8: if (propValue >= LIMIT_NUMERIC_TYPE) break;
                if (style != SHORT) return ucd.getNumericTypeID_fromIndex((byte)propValue);
                return UCD_Names.SHORT_NT[propValue];
            case EAST_ASIAN_WIDTH>>8: if (propValue >= LIMIT_EAST_ASIAN_WIDTH) break;
                if (style != LONG) return ucd.getEastAsianWidthID_fromIndex((byte)propValue);
                return UCD_Names.SHORT_EA[propValue];
            case LINE_BREAK>>8:  if (propValue >= LIMIT_LINE_BREAK) break;
                if (style != LONG) return ucd.getLineBreakID_fromIndex((byte)propValue);
                return UCD_Names.LONG_LB[propValue];
            case JOINING_TYPE>>8: if (propValue >= LIMIT_JOINING_TYPE) break;
                if (style != LONG) return ucd.getJoiningTypeID_fromIndex((byte)propValue);
                return UCD_Names.LONG_JOINING_TYPE[propValue];
            case JOINING_GROUP>>8: if (propValue >= LIMIT_JOINING_GROUP) break;
                return ucd.getJoiningGroupID_fromIndex((byte)propValue);
            case BINARY_PROPERTIES>>8: return ucd.getBinaryPropertiesID_fromIndex((byte)propValue, style);
            case SCRIPT>>8: if (propValue >= LIMIT_SCRIPT) break;
                if (style != SHORT) return ucd.getScriptID_fromIndex((byte)propValue);
                return UCD_Names.ABB_SCRIPT[propValue];
            case AGE>>8: if (propValue >= LIMIT_AGE) break;
                return ucd.getAgeID_fromIndex((byte)propValue);
                /*
            case DERIVED>>8:
                UnicodeProperty up = DerivedProperty.make(propValue, ucd);
                if (up == null) break;
                return up.getName(style);
                */
            }
        } catch (RuntimeException e) {
            throw new ChainException("Illegal property Number* {0}, {1}", new Object[]{
                 new Integer(majorProp), new Integer(propValue)}, e);
        }
        throw new ChainException("Illegal property Number {0}, {1}", new Object[]{
                new Integer(majorProp), new Integer(propValue)});
    }
    
    public byte _getDefaultStyle() {
        try {
            switch (majorProp) {
            case CATEGORY>>8: return SHORT;
            case COMBINING_CLASS>>8: return NUMBER;
            case BIDI_CLASS>>8: return SHORT;
            case DECOMPOSITION_TYPE>>8: return LONG;
            case NUMERIC_TYPE>>8: return LONG;
            case EAST_ASIAN_WIDTH>>8: return SHORT;
            case LINE_BREAK>>8:  return SHORT;
            case JOINING_TYPE>>8: return SHORT;
            case JOINING_GROUP>>8: return LONG;
            case BINARY_PROPERTIES>>8: return LONG;
            case SCRIPT>>8: return LONG;
            case AGE>>8: return LONG;
            }
        } catch (RuntimeException e) {
            throw new ChainException("Illegal property Number {0}, {1}", new Object[]{
                 new Integer(majorProp), new Integer(propValue)}, e);
        }
        throw new ChainException("Illegal property Number {0}, {1}", new Object[]{
                new Integer(majorProp), new Integer(propValue)});
    }
    
}