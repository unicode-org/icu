package com.ibm.text.UCD;
import java.io.*;
import java.util.*;

import com.ibm.text.utility.*;

final class DerivedPropertyLister extends PropertyLister {
    static final boolean BRIDGE = false;
    
    static int enum = 0;
    static final int
        PropMath = 0,
        PropAlphabetic = 1,
        PropLowercase = 2,
        PropUppercase = 3,
        
        ID_Start = 4,
        ID_Continue_NO_Cf = 5,
        
        Mod_ID_Start = 6,
        Mod_ID_Continue_NO_Cf = 7,
        
        Missing_Uppercase = 8,
        Missing_Lowercase = 9,
        Missing_Mixedcase = 10,
        
        FC_NFKC_Closure = 11,
        
        FullCompExclusion = 12,
        FullCompInclusion = 13,
        
        QuickNFD = 14,
        QuickNFC = 15,
        QuickNFKD = 16,
        QuickNFKC = 17,
        
        ExpandsOnNFD = 18,
        ExpandsOnNFC = 19,
        ExpandsOnNFKD = 20,
        ExpandsOnNFKC = 21,
        
        GenNFD = 22,
        GenNFC = 23,
        GenNFKD = 24,
        GenNFKC = 25,
                
        LIMIT = 26;
   ;
    
    private int propMask;
    private Normalizer[] nf = new Normalizer[4];
    private Normalizer nfd, nfc, nfkd, nfkc;
    int width;
    
    public DerivedPropertyLister(UCD ucd, int propMask, PrintStream output) {
        this.propMask = propMask;
        this.output = output;
        this.ucdData = ucd;
        nfd = nf[0] = new Normalizer(Normalizer.NFD);
        nfc = nf[1] = new Normalizer(Normalizer.NFC);
        nfkd = nf[2] = new Normalizer(Normalizer.NFKD);
        nfkc = nf[3] = new Normalizer(Normalizer.NFKC);
        
        width = super.minPropertyWidth();
        switch (propMask) {
          case GenNFD: case GenNFC: case GenNFKD: case GenNFKC:
            alwaysBreaks = true;
            break;
          case FC_NFKC_Closure:
            alwaysBreaks = true;
            width = 21;
            break;
          case QuickNFC: case QuickNFKC:
            width = 11;
            break;
        }
    }
    
    public String headerString() {
        String result = "# Derived Property: ";
        switch (propMask) {
          case ExpandsOnNFD: case ExpandsOnNFC: case ExpandsOnNFKD: case ExpandsOnNFKC:
            result += "Expands_On_" + NAME[propMask-ExpandsOnNFD] + "\r\n#   Generated according to UAX #15."
            + "\r\n#   Characters whose normalized length is not one."
            + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
            + "\r\n#            The length of a normalized string is not necessarily the sum of the lengths of the normalized characters!";
            break;
          case GenNFD: case GenNFC: case GenNFKD: case GenNFKC:
            result += NAME[propMask-GenNFD] + "\r\n#   Generated according to UAX #15."
            + "\r\n#   Normalized forms, where different from the characters themselves."
            + ((propMask == 5 || propMask == 3) 
              ? ""
              : "\r\n#   HANGUL SYLLABLES are algorithmically decomposed, and not listed explicitly.")
            + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
            + "\r\n#            It is NOT sufficient to replace characters one-by-one with these results!";
            break;
          case ID_Start: result += 
            "ID_Start"
            + "\r\n#  Characters that can start an identifier."
            + "\r\n#  Generated from Lu+Ll+Lt+Lm+Lo+Nl";
            break;
          case ID_Continue_NO_Cf: result += 
            "ID_Continue"
            + "\r\n#  Characters that can continue an identifier."
            + "\r\n#  Generated from: ID_Start + Mn+Mc+Nd+Pc"
            + "\r\n#  NOTE: Cf characters should be filtered out.";
            break;
          case Mod_ID_Start: result += 
            "XID_Start"
            + "\r\n#  ID_Start modified for closure under NFKx"
            + "\r\n#  Modified as described in UAX #15"
            + "\r\n#  NOTE: Does NOT remove the non-NFKx characters."
            + "\r\n#        Merely ensures that if isIdentifer(string) then isIdentifier(NFKx(string))";
            break;
          case Mod_ID_Continue_NO_Cf: result += 
            "XID_Continue"
            + "\r\n#  Mod_ID_Continue modified for closure under NFKx"
            + "\r\n#  Modified as described in UAX #15"
            + "\r\n#  NOTE: Cf characters should be filtered out."
            + "\r\n#  NOTE: Does NOT remove the non-NFKx characters."
            + "\r\n#        Merely ensures that if isIdentifer(string) then isIdentifier(NFKx(string))";
            break;
          case PropMath:
            result += "Math"
             + "\r\n#  Generated from: Sm + Other_Math";
            break;
          case PropAlphabetic: 
            result += "Alphabetic"
            + "\r\n#  Generated from: Lu+Ll+Lt+Lm+Lo+Nl + Other_Alphabetic";
            break;
          case PropLowercase:
            result += "Lowercase"
            + "\r\n#  Generated from: Ll + Other_Lowercase";
            break;
          case PropUppercase: result +=
            "Uppercase"
            + "\r\n#  Generated from: Lu + Other_Uppercase";
            break;
          case Missing_Uppercase: result +=
            "Missing_Uppercase"
            + "\r\n#  Generated from: NFKD has >0 Uppercase, no other cases";
            break;
          case Missing_Lowercase: result +=
            "Missing_Lowercase"
            + "\r\n#  Generated from: NFKD has >0 Lowercase, no other cases";
            break;
          case Missing_Mixedcase: result +=
            "Missing_Mixedcase"
            + "\r\n#  Generated from: NFKD has >0 Mixedcase, no other cases";
            break;
          case FullCompExclusion: result +=
            "Full Composition Exclusion"
            + "\r\n#  Generated from: Composition Exclusions + Singletons + Non-Starter Decompositions";
            break;
          case FullCompInclusion: result +=
            "Full Composition Inclusion"
            + "\r\n#  characters with Canonical Decompositions MINUS Full Composition Exclusion";
            break;
          case FC_NFKC_Closure: result +=
            "FC_NFKC_Closure"
            + "\r\n#  Generated from computing: b = NFKC(Fold(a)); c = NFKC(Fold(b));"
            + "\r\n#  Then if (c != b) add the mapping from a to c to the set of"
            + "\r\n#  mappings that constitute the FC_NFKC_Closure list";
            break;
          case QuickNFD: case QuickNFC: case QuickNFKD: case QuickNFKC:
            result += NAME[propMask-QuickNFD] + "_QuickCheck"
            + "\r\n#  Generated from computing decomposibles"
            + ((propMask == QuickNFC || propMask == QuickNFKC)
                ? " (and characters that may compose with previous ones)" : "");
            break;
          default: result += "Unimplemented!!";
        }
        return result;
    }

    public String propertyName(int cp) {
        switch (propMask) {
          case ExpandsOnNFD: case ExpandsOnNFC: case ExpandsOnNFKD: case ExpandsOnNFKC:
            return "Expands_On_" + NAME[propMask-ExpandsOnNFD];
          case GenNFD: case GenNFC: case GenNFKD: case GenNFKC:
            if (cp >= 0xAC00 && cp <= 0xD7A3) return NAME[propMask-GenNFD] + "; " + "<algorithmic normalization>";
            String norm = Utility.hex(nf[propMask-GenNFD].normalize(cp));
            String pad = Utility.repeat(" ", 14-norm.length());
            return NAME[propMask-GenNFD] + "; " + norm + pad;
          case ID_Start: return "ID_Start";
          case ID_Continue_NO_Cf: return "ID_Continue";
          case Mod_ID_Start: return "XID_Start";
          case Mod_ID_Continue_NO_Cf: return "XID_Continue";
          case PropMath: return "Math";
          case PropAlphabetic: return "Alphabetic";
          case PropLowercase: return "Lowercase";
          case PropUppercase: return "Uppercase";
          case Missing_Uppercase: return "Possible_Missing_Uppercase";
          case Missing_Lowercase: return "Possible_Missing_Lowercase";
          case Missing_Mixedcase: return "Possible_Missing_Titlecase";
          case FullCompExclusion: return "Comp_Ex";
          case FullCompInclusion: return "Comp_In";
          case FC_NFKC_Closure: return "FNC; " + Utility.hex(getComputedValue(cp));
          case QuickNFD: case QuickNFC: case QuickNFKD: case QuickNFKC:
            return NAME[propMask-QuickNFD] + "_" + getComputedValue(cp);
          default: return "Unimplemented!!";
        }
    }
    
    //public String optionalComment(int cp) {
    //    return super.optionalComment(cp) + " [" + ucdData.getCodeAndName(computedValue) + "]";
    //}
    
    
    public int minPropertyWidth() {
        return width;
    }
    
    
    static final String[] NAME = {"NFD", "NFC", "NFKD", "NFKC"};
    /*
    public String optionalComment(int cp) {
        String id = ucdData.getCategoryID(cp);
        if (UCD.mainCategoryMask(ucdData.getCategory(cp)) == LETTER_MASK) return id.substring(0,1) + "*";
        return id;
    }
    */
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
        if (!ucdData.isAssigned(cp)) return EXCLUDE;
        //if (cp == 0xFFFF) {
        //    System.out.println("# " + Utility.hex(cp));
        //}
        byte cat = ucdData.getCategory(cp);
        //if (cp == 0x0385) {
        //    System.out.println(Utility.hex(firstRealCp));
        //}
        
        String cps;
        byte xCat;
        
        switch (propMask) {
          default: return EXCLUDE;
            
          case ExpandsOnNFD: case ExpandsOnNFC: case ExpandsOnNFKD: case ExpandsOnNFKC:
            if (ucdData.getDecompositionType(cp) == NONE) return EXCLUDE;
            cps = UTF32.valueOf32(cp);
            if (UTF32.length32(nf[propMask-ExpandsOnNFD].normalize(cps)) == UTF32.length32(cps)) return EXCLUDE;
            break;
          case GenNFD: case GenNFC: case GenNFKD: case GenNFKC:
            if (ucdData.getDecompositionType(cp) == NONE) return EXCLUDE;
            cps = UTF32.valueOf32(cp);
            if (cps.equals(nf[propMask-GenNFD].normalize(cps))) {
                return EXCLUDE;
            }
            if (cp >= 0xAC00 && cp <= 0xD7A3) return INCLUDE;
            //System.out.println(Utility.hex(cps) + " => " + Utility.hex(nf[propMask-4].normalize(cps)));
            return BREAK;
          case ID_Start:
            if (ucdData.isIdentifierStart(cp, false)) return INCLUDE;
            return EXCLUDE;
          case ID_Continue_NO_Cf:
            if (ucdData.isIdentifierContinue_NO_Cf(cp, false)) return INCLUDE;
            return EXCLUDE;
          case Mod_ID_Start:
            if (ucdData.isIdentifierStart(cp, true)) return INCLUDE;
            return EXCLUDE;
          case Mod_ID_Continue_NO_Cf:
            if (ucdData.isIdentifierContinue_NO_Cf(cp, true)) return INCLUDE;
            return EXCLUDE;
          case PropMath:
            if (cat == Sm
             || ucdData.getBinaryProperty(cp,Math_Property)) return INCLUDE;
            return EXCLUDE;
          case PropAlphabetic:
            if (cat == Lu || cat == Ll || cat == Lt || cat == Lm || cat == Lo || cat == Nl
             || ucdData.getBinaryProperty(cp, Alphabetic)) return INCLUDE;
          case PropLowercase:
            if (cat == Ll
             || ucdData.getBinaryProperty(cp, Other_Lowercase)) return INCLUDE;
            return EXCLUDE;
          case PropUppercase:
            if (cat == Lu
             || ucdData.getBinaryProperty(cp, Other_Uppercase)) return INCLUDE;
            return EXCLUDE;
          case Missing_Uppercase:
            if (cat == Lu
             || ucdData.getBinaryProperty(cp, Other_Uppercase)) return EXCLUDE;
            xCat = getDecompCat(cp);
            if (xCat == Lu) return INCLUDE;
            return EXCLUDE;
          case Missing_Lowercase:
            if (cat == Ll
             || ucdData.getBinaryProperty(cp, Other_Lowercase)) return EXCLUDE;
            xCat = getDecompCat(cp);
            if (xCat == Ll) return INCLUDE;
            return EXCLUDE;
          case Missing_Mixedcase:
            if (cat == Lt) return EXCLUDE;
            xCat = getDecompCat(cp);
            if (xCat == Lt) return INCLUDE;
            return EXCLUDE;
          case FullCompExclusion:
            /*
(3) Singleton Decompositions: characters that  can be derived from the UnicodeData file by 
including all characters whose canonical decomposition consists of a single character.
(4) Non-Starter Decompositions: characters that  can be derived from the UnicodeData
file by including all characters whose canonical decomposition consists of a sequence
of characters, the first of which has a non-zero combining class.
*/          
            {
                if (!ucdData.isRepresented(cp)) return EXCLUDE;
                byte dtype = ucdData.getDecompositionType(cp);
                if (dtype != CANONICAL) return EXCLUDE;
                
                if (isCompEx(cp)) return INCLUDE;
                return EXCLUDE;
            }
          case FullCompInclusion:
            {
                if (!ucdData.isRepresented(cp)) return EXCLUDE;
                byte dtype = ucdData.getDecompositionType(cp);
                if (dtype != CANONICAL) return EXCLUDE;
                
                if (isCompEx(cp)) return EXCLUDE;
                return INCLUDE;
            }
          case FC_NFKC_Closure:
            if (!ucdData.isRepresented(cp)) return EXCLUDE;
          
          /*
            b = Normalize(Fold(a));
            c = Normalize(Fold(b));
            if (c != b) add a => c
          */
            {
                String b = nfkc.normalize(fold(cp));
                String c = nfkc.normalize(fold(b));
                if (c.equals(b)) return EXCLUDE;
                setComputedValue(cp, c);
                if (cp == 0x1F88) {
                    System.out.println(ucdData.toString(cp));
                    System.out.println("cp: " + ucdData.getCodeAndName(cp));
                    System.out.println("fold(cp): " + ucdData.getCodeAndName(fold(cp)));
                    System.out.println("b: " + ucdData.getCodeAndName(b));
                    System.out.println("fold(b): " + ucdData.getCodeAndName(fold(b)));
                    System.out.println("c: " + ucdData.getCodeAndName(c));
                }
                return BREAK;
            }
            
         case QuickNFD: case QuickNFC: case QuickNFKD: case QuickNFKC:
            lastValue = currentValue;
            Normalizer nfx = nf[propMask - QuickNFD];
            if (nfx.normalizationDiffers(cp)) currentValue = "NO";
            else if (nfx.isTrailing(cp)) currentValue = "MAYBE";
            else return EXCLUDE;
            setComputedValue(cp, currentValue);
            if (currentValue != lastValue) return BREAK;
            return INCLUDE;
        }
        
        
        // handle script stuff
        /*
        if (firstRealCp == -1) return INCLUDE;
        byte cat2 = ucdData.getCategory(firstRealCp);
        if (cat == cat2) return INCLUDE;
        int mc = UCD.mainCategoryMask(cat);
        if (LETTER_MASK == mc && mc == UCD.mainCategoryMask(cat2)) return INCLUDE;
        
        return BREAK;
        */
        return INCLUDE;
    }
    
    static Map computedValue = new HashMap();
    static String getComputedValue(int cp) {
        return (String) computedValue.get(new Integer(cp));
    }
    static void setComputedValue(int cp, String value) {
        computedValue.put(new Integer(cp), value);
    }
    static String lastValue = "";
    static String currentValue = "";
    
    boolean isCompEx(int cp) {     
        if (ucdData.getBinaryProperty(cp, CompositionExclusion)) return true;
        String decomp = ucdData.getDecompositionMapping(cp);
        if (UTF32.length32(decomp) == 1) return true;
        int first = UTF32.char32At(decomp,0);
        if (ucdData.getCombiningClass(first) != 0) return true;
        return false;
    }
    
    StringBuffer foldBuffer = new StringBuffer();
    
    String fold(int cp) {
        return ucdData.getCase(cp, FULL, FOLD);
    }
    
    String fold(String s) {
        return ucdData.getCase(s, FULL, FOLD);
    }
    
    byte getDecompCat(int cp) {
        byte cat = ucdData.getCategory(cp);
        if (cat == Lu
            || ucdData.getBinaryProperty(cp, Other_Uppercase)) return Lu;
        if (cat == Ll
            || ucdData.getBinaryProperty(cp, Other_Lowercase)) return Ll;
        if (cat == Lt || cat == Lo || cat == Lm || cat == Nl) return cat;
        if (!nf[2].normalizationDiffers(cp)) return Lo;
        
        String norm = nf[2].normalize(cp);
        int cp2;
        boolean gotUpper = false;
        boolean gotLower = false;
        boolean gotTitle = false;
        for (int i = 0; i < norm.length(); i += UTF32.count16(cp2)) {
            cp2 = UTF32.char32At(norm, i);
            byte catx = ucdData.getCategory(cp2);
            boolean upx = ucdData.getBinaryProperty(cp, Other_Uppercase);
            boolean lowx = ucdData.getBinaryProperty(cp, Other_Lowercase);
            if (catx == Ll || lowx || cp2 == 0x345) gotLower = true;
            if (catx == Lu || upx) gotUpper = true;
            if (catx == Lt) gotTitle = true;
        }
        if (gotLower && !gotUpper && !gotTitle) return Ll;
        if (!gotLower && gotUpper && !gotTitle) return Lu;
        if (gotLower || gotUpper || gotTitle) return Lt;
        return cat;
    }
}
    
