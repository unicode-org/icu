package com.ibm.text.UCD;
import java.io.*;

import com.ibm.text.utility.*;

final class MyPropertyLister extends PropertyLister {
    
    static final boolean BRIDGE = false;
    
    private int propMask;
        
    public MyPropertyLister(UCD ucd, int propMask, PrintStream output) {
        this.propMask = propMask;
        this.output = output;
        this.ucdData = ucd;
        if (propMask < COMBINING_CLASS) usePropertyComment = false; // skip gen cat
    }
    
    static String getCombiningName (int propMask) {
        String s = "";
        switch (propMask & 0xFF) {
            case 0: s = "NotReordered"; break;
            case 1: s = "Overlay"; break;
            case 7: s = "Nukta"; break;
            case 8: s = "KanaVoicing"; break;
            case 9: s = "Virama"; break;
            case 202: s = "AttachedBelowLeft"; break;
            case 204: s = "AttachedBelow"; break;
            case 206: s = "AttachedBelowRight"; break;
            case 208: s = "AttachedLeft"; break;
            case 210: s = "AttachedRight"; break;
            case 212: s = "AttachedAboveLeft"; break;
            case 214: s = "AttachedAbove"; break;
            case 216: s =  "AttachedAboveRight"; break;
            case 218: s =  "BelowLeft"; break;
            case 220: s =  "Below"; break;
            case 222: s =  "BelowRight"; break;
            case 224: s =  "Left"; break;
            case 226: s =  "Right"; break;
            case 228: s =  "AboveLeft"; break;
            case 230: s =  "Above"; break;
            case 232: s =  "AboveRight"; break;
            case 233: s =  "DoubleBelow"; break;
            case 234: s =  "DoubleAbove"; break;
            case 240: s =  "IotaSubscript"; break;
        }
        return s;
    }
    
    public String headerString() {
        int main = (propMask & 0xFF00);
        if (main == COMBINING_CLASS) {
            String s = getCombiningName(propMask);
            if (s.length() == 0) s = "Other Combining Class";
            return "# " + s;
        } else if (main == BINARY_PROPERTIES) {
            return "# Binary Property";
        } else if (main == JOINING_GROUP) {
            return "";
        } else {
            String shortID = getUnifiedBinaryPropertyID(ucdData, propMask, SHORT);
            String longID = getUnifiedBinaryPropertyID(ucdData, propMask, LONG);
            return "# " + shortID + (shortID.equals(longID) ? "" : "\t(" + longID + ")");
        }
    }
        
    public String propertyName(int cp) {
        return getUnifiedBinaryPropertyID(propMask);
    }
    
    public String optionalComment(int cp) {
        if (propMask < COMBINING_CLASS) return ""; // skip gen cat
        int cat = ucdData.getCategory(cp);
        if (cat == Lt || cat == Ll || cat == Lu) return "L&";
        return ucdData.getCategoryID(cp);
    }
    
    /*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
    */
        
    public byte status(int cp) {
        //if (cp == 0xFFFF) {
        //    System.out.println("# " + Utility.hex(cp));
        //}
        byte cat = ucdData.getCategory(cp);
        //if (cp == 0x0385) {
        //    System.out.println(Utility.hex(firstRealCp));
        //}
        
        if (cat == Cn
            && propMask != (BINARY_PROPERTIES | Noncharacter_Code_Point)
            && propMask != (BINARY_PROPERTIES | Reserved_Cf_Code_Point)
            && propMask != (CATEGORY | Cn)) {
            if (BRIDGE) return CONTINUE;
            else return EXCLUDE;
        }
        
        boolean inSet = getUnifiedBinaryProperty(cp, propMask);
        /*
        if (cp >= 0x1D400 && cp <= 0x1D7C9 && cat != Cn) {
            if (propMask == (SCRIPT | LATIN_SCRIPT)) inSet = cp <= 0x1D6A3;
            else if (propMask == (SCRIPT | GREEK_SCRIPT)) inSet = cp > 0x1D6A3;
        }
        */
/* HACK
1D400;MATHEMATICAL BOLD CAPITAL A;Lu;0;L;<font> 0041;;;;N;;;;;
1D6A3;MATHEMATICAL MONOSPACE SMALL Z;Ll;0;L;<font> 007A;;;;N;;;;;
1D6A8;MATHEMATICAL BOLD CAPITAL ALPHA;Lu;0;L;<font> 0391;;;;N;;;;;
1D7C9;MATHEMATICAL SANS-SERIF BOLD ITALIC PI SYMBOL;Ll;0;L;<font> 03D6;;;;N;;;;;
*/

        if (!inSet) return EXCLUDE;
        return INCLUDE;
    }
    
    /**
     * @return unified property number
     */
    public static boolean isUnifiedBinaryPropertyDefined(UCD ucd, int propMask) {
        int enum = propMask >> 8;
        propMask &= 0xFF;
        switch (enum) {
          case CATEGORY>>8: return propMask != UNUSED_CATEGORY && propMask < LIMIT_CATEGORY;
          case COMBINING_CLASS>>8: return ucd.isCombiningClassUsed((byte)propMask);
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
          default: return false;
        }
    }    
    
    public boolean getUnifiedBinaryProperty(int cp, int propMask) {
        return getUnifiedBinaryProperty(ucdData, cp, propMask);
    }
        
    static public boolean getUnifiedBinaryProperty(UCD ucd, int cp, int propMask) {
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
        }
        throw new ChainException("Illegal property Number {0}", new Object[]{new Integer(propMask)});
    }    
    
    static final int SHORT = -1, NORMAL = 0, LONG = 1, BOTH = 2;
    
    public String getUnifiedBinaryPropertyID(int unifiedPropMask) {
        return getUnifiedBinaryPropertyID(ucdData, unifiedPropMask, NORMAL);
    }
    
    public static String getUnifiedBinaryPropertyID(UCD ucd, int unifiedPropMask) {
        String longOne = getUnifiedBinaryPropertyID(ucd, unifiedPropMask, LONG);
        String shortOne = getUnifiedBinaryPropertyID(ucd, unifiedPropMask, SHORT);
        if (longOne.equals(shortOne)) return longOne;
        return shortOne + "(" + longOne + ")";
    }
    
    public static String getFullUnifiedBinaryPropertyID(UCD ucd, int unifiedPropMask, int style) {
        String pre = "";
        if ((unifiedPropMask & 0xFF00) != BINARY_PROPERTIES) {
            String preShort = UCD_Names.ABB_UNIFIED_PROPERTIES[unifiedPropMask>>8] + "=";
            String preLong = UCD_Names.SHORT_UNIFIED_PROPERTIES[unifiedPropMask>>8] + "=";
            if (style < LONG) pre = preShort;
            else if (style == LONG || preShort.equals(preLong)) pre = preLong;
            else pre = preShort + "(" + preLong + ")";
        }
        String shortOne = getUnifiedBinaryPropertyID(ucd, unifiedPropMask, SHORT);
        if (shortOne.length() == 0) shortOne = "xx";
        String longOne = getUnifiedBinaryPropertyID(ucd, unifiedPropMask, LONG);
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
    
    public static String getUnifiedBinaryPropertyID(UCD ucd, int unifiedPropMask, int style) {
        int enum = unifiedPropMask >> 8;
        byte propMask = (byte)unifiedPropMask;
        switch (enum) {
          case CATEGORY>>8: if (propMask >= LIMIT_CATEGORY) break;
            if (style != LONG) return ucd.getCategoryID_fromIndex(propMask);
            return UCD_Names.LONG_GC[propMask];
          case COMBINING_CLASS>>8: if (propMask >= LIMIT_COMBINING_CLASS) break;
            String s = "";
            if (style == LONG) {
                s = getCombiningName(unifiedPropMask);
                if (s.length() != 0) return s;
                s = "fixed_";
            }
            return s + ucd.getCombiningClassID_fromIndex((short)(0xFF & propMask));
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
        }
        throw new ChainException("Illegal property Number {0}", new Object[]{new Integer(propMask)});
    }    
    
}
    
