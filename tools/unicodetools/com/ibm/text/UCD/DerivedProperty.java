/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/DerivedProperty.java,v $
* $Date: 2001/12/03 19:29:35 $
* $Revision: 1.7 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;
import com.ibm.text.*;
import java.util.*;

public class DerivedProperty implements UCD_Types {
  
    UCD ucdData;
    
    // ADD CONSTANT to UCD_TYPES
    
    static public UnicodeProperty getProperty(int derivedPropertyID, UCD ucd) {
        return new DerivedProperty(ucd).dprops[derivedPropertyID];
    }
    
    public DerivedProperty(UCD ucd) {
      ucdData = ucd;
    }
    
    public String getHeader(int propNumber) {
        UnicodeProperty dp = dprops[propNumber];
        if (dp != null) return dp.getHeader();
        else return "Unimplemented!!";
    }

    public String getName(int propNumber, byte style) {
        UnicodeProperty dp = dprops[propNumber];
        if (dp != null) return dp.getName(style);
        else return "Unimplemented!!";
    }

    public String getProperty(int cp, int propNumber) {
        UnicodeProperty dp = dprops[propNumber];
        if (dp != null) return dp.getProperty(cp);
        else return "Unimplemented!!";
    }
    
    public boolean isDefined(int propNumber) {
        if (propNumber < 0 || propNumber >= dprops.length) return false;
        return dprops[propNumber] != null;
    }
    
    public boolean isTest(int propNumber) {
        if (!isDefined(propNumber)) return false;
        return dprops[propNumber].isTest();
    }
    
    public boolean hasProperty(int cp, int propNumber) {
        if (!isDefined(propNumber)) return false;
        return dprops[propNumber].hasProperty(cp);
    }
    
    public boolean propertyVaries(int propNumber) {
        return dprops[propNumber].propertyVaries();
    }
    /*
    public String getProperty(int cp, int propNumber) {
        return dprops[propNumber].getProperty(int cp);
    }
    */
    private UnicodeProperty[] dprops = new UnicodeProperty[50];
    private Normalizer[] nf = new Normalizer[4];
    private Normalizer nfd, nfc, nfkd, nfkc;

    static final String[] CaseNames = {
                "Uppercase", 
                "Lowercase", 
                "Mixedcase"};
    
    /*         
    private abstract static class UnicodeProperty {
        boolean testStatus = false;
        byte defaultStyle = LONG;
        String name, shortName, header;
        String getName(byte style) { 
            if (style == NORMAL) style = defaultStyle;
            return style < LONG ? shortName : name;
        }
        String getHeader() { return header; }
        boolean isTest() { return testStatus; }
        abstract boolean hasProperty(int cp);
        public boolean propertyVaries() { return false; }
        public String getProperty(int cp) { return hasProperty(cp) ? name : ""; }
    }
    */
    
    class ExDProp extends UnicodeProperty {
        Normalizer nfx;
        ExDProp(int i) {
            nfx = nf[i];
            name = "Expands_On_" + nfx.getName();
            shortName = "XO_" + nfx.getName();
            header = "# Derived Property: " + name
                + "\r\n#   Generated according to UAX #15."
                + "\r\n#   Characters whose normalized length is not one."
                + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
                + "\r\n#            The length of a normalized string is not necessarily the sum of the lengths of the normalized characters!";
        }
        boolean hasProperty(int cp) {
            if (ucdData.getDecompositionType(cp) == NONE) return false;
            String norm = nfx.normalize(cp);
            if (UTF16.countCodePoint(norm) != 1) return true;
            return false;
        }
    };
    
    class NF_UnsafeStartProp extends UnicodeProperty {
        Normalizer nfx;
        //int prop;
        
        NF_UnsafeStartProp(int i) {
            isStandard = false;
            nfx = nf[i];
            name = nfx.getName() + "_UnsafeStart";
            shortName = nfx.getName() + "_SS";
            header = "# Derived Property: " + name
                + "\r\n#   Generated according to UAX #15."
                + "\r\n#   Characters that are cc==0, BUT which may interact with previous characters."
                ;
        }
        boolean hasProperty(int cp) {
            if (ucdData.getCombiningClass(cp) != 0) return false;
            String norm = nfx.normalize(cp);
            int first = UTF16.charAt(norm, 0);
            if (ucdData.getCombiningClass(first) != 0) return true;
            if (nfx.isComposition()
                && dprops[NFC_TrailingZero].hasProperty(first)) return true; // 1,3 == composing
            return false;
        }
    };
    
    
    class NFC_Prop extends UnicodeProperty {
        BitSet bitset;
        boolean filter = false;
        boolean keepNonZero = true;
        
        NFC_Prop(int i) {
            isStandard = false;
            BitSet[] bitsets = new BitSet[3];
            switch(i) {
                case NFC_Leading: bitsets[0] = bitset = new BitSet(); break;
                case NFC_Resulting: bitsets[2] = bitset = new BitSet(); break;
                case NFC_TrailingZero: keepNonZero = false; // FALL THRU
                case NFC_TrailingNonZero: bitsets[1] = bitset = new BitSet(); break;
            }
            filter = bitsets[1] != null;
            nfc.getCompositionStatus(bitsets[0], bitsets[1], bitsets[2]);
            
            name = Names[i-NFC_Leading];
            shortName = SNames[i-NFC_Leading];
            header = "# Derived Property: " + name
                + "\r\n#   " + Description[i-NFC_Leading]
                + "\r\n#   NFKC characters are the same, after subtracting the NFKD = NO values."
                + "\r\n#   Generated according to UAX #15."
                + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
                + "\r\n#            The length of a normalized string is not necessarily the sum of the lengths of the normalized characters!";
        }
        boolean hasProperty(int cp) {
            boolean result = bitset.get(cp);
            if (result && filter) {
                result = (ucdData.getCombiningClass(cp) != 0) == keepNonZero;
            }
            return result;
        }
        final String[] Names = {"NFC_Leading", "NFC_TrailingNonZero", "NFC_TrailingZero", "NFC_Resulting"};
        final String[] SNames = {"NFC_L", "NFC_TNZ", "NFC_TZ", "NFC_R"};
        final String[] Description = {
            "Characters that can combine with following characters in NFC",
            "Characters that can combine with previous characters in NFC, and have non-zero combining class",
            "Characters that can combine with previous characters in NFC, and have zero combining class",
            "Characters that can result from a combination of other characters in NFC",
        };
    };
    
    class GenDProp extends UnicodeProperty {
        Normalizer nfx;
        Normalizer nfComp = null;
        
        GenDProp (int i) {
                isStandard = false;
            nfx = nf[i];
            name = nfx.getName();
            String compName = "the character itself";
            
            if (i == NFKC || i == NFD) {
                name += "-NFC";
                nfComp = nfc;
                compName = "NFC for the character";
            } else if (i == NFKD) {
                name += "-NFD";
                nfComp = nfd;
                compName = "NFD for the character";
            }
            header = "# Derived Property: " + name              
                + "\r\n#   Lists characters in normalized form " + nfx.getName() + "."
                + "\r\n#   Only those characters whith normalized forms are DIFFERENT from " + compName + " are listed!"
                + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
                + "\r\n#            It is NOT sufficient to replace characters one-by-one with these results!";
        }
        public boolean propertyVaries() {return true;} // default
        
        int cacheCp = 0;
        String cacheStr = "";
        
        public String getProperty(int cp) {
            if (cacheCp == cp) return cacheStr;
            cacheCp = cp;
            cacheStr = "";
            
            if (ucdData.getDecompositionType(cp) != NONE) {
                String cps = UTF32.valueOf32(cp);
                String comp = cps;
                if (nfComp != null) {
                    comp = nfComp.normalize(comp);
                }
                String normal = nfx.normalize(cps);
                if (!comp.equals(normal)) {
                    String norm = Utility.hex(normal);
                    String pad = Utility.repeat(" ", 14-norm.length());
                    cacheStr = name + "; " + norm + pad;
                }
            }
            
            return cacheStr;
            //if (cp >= 0xAC00 && cp <= 0xD7A3) return true;
            //System.out.println(Utility.hex(cps) + " => " + Utility.hex(nf[i-4].normalize(cps)));
        } // default
        boolean hasProperty(int cp) { return getProperty(cp).length() != 0; }
    };
    
    class CaseDProp extends UnicodeProperty {
        byte val;
        CaseDProp (int i) {
                isStandard = false;
            val = (i == Missing_Uppercase ? Lu : i == Missing_Lowercase ? Ll : Lt);
            name = "Possible_Missing_" + CaseNames[i-Missing_Uppercase];
            header = "# Derived Property: " + name
            + "\r\n#  Generated from: NFKD has >0 " + CaseNames[i-Missing_Uppercase] + ", no other cases";
        }
        boolean hasProperty(int cp) {
            byte cat = ucdData.getCategory(cp);
            if (cat == val
            || val != Lt && ucdData.getBinaryProperty(cp, Other_Uppercase)) return false;
            byte xCat = getDecompCat(cp);
            if (xCat == val) return true;
            return false;
        }
    };
    
    class QuickDProp extends UnicodeProperty {
        String NO;
        String MAYBE;
        Normalizer nfx;
        QuickDProp (int i) {
            nfx = nf[i];
            NO = nfx.getName() + "_NO";
            MAYBE = nfx.getName() + "_MAYBE";
            name = nfx.getName() + "_QuickCheck";
            shortName = nfx.getName() + "_QC";
            header = "# Derived Property: " + name
            + "\r\n#  Generated from computing decomposibles"
            + ((i == QuickNFC || i == QuickNFKC)
                ? " (and characters that may compose with previous ones)" : "");
        }
                
        public boolean propertyVaries() {return true;}
        public String getProperty(int cp) { 
            if (nfx.normalizationDiffers(cp)) return NO;
            else if (nfx.isTrailing(cp)) return MAYBE;
            else return "";
        }
        boolean hasProperty(int cp) { return getProperty(cp).length() != 0; }
    };

    {
        nfd = nf[0] = new Normalizer(Normalizer.NFD);
        nfc = nf[1] = new Normalizer(Normalizer.NFC);
        nfkd = nf[2] = new Normalizer(Normalizer.NFKD);
        nfkc = nf[3] = new Normalizer(Normalizer.NFKC);

        for (int i = ExpandsOnNFD; i <= ExpandsOnNFKC; ++i) {
            dprops[i] = new ExDProp(i-ExpandsOnNFD);
        }
        
        for (int i = GenNFD; i <= GenNFKC; ++i) {
            dprops[i] = new GenDProp(i-GenNFD);
        }
        
        for (int i = NFC_Leading; i <= NFC_Resulting; ++i) {
            dprops[i] = new NFC_Prop(i);
        }
        
        for (int i = NFD_UnsafeStart; i <= NFKC_UnsafeStart; ++i) {
            dprops[i] = new NF_UnsafeStartProp(i-NFD_UnsafeStart);
        }
        
        dprops[ID_Start] = new UnicodeProperty() {
            {
                name = "ID_Start";
                shortName = "IDS";
                header = "# Derived Property: " + name
                    + "\r\n#  Characters that can start an identifier."
                    + "\r\n#  Generated from Lu+Ll+Lt+Lm+Lo+Nl";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierStart(cp, false);
            }
        };
        
        dprops[ID_Continue_NO_Cf] = new UnicodeProperty() {
            {
                name = "ID_Continue";
                shortName = "IDC";
                header = "# Derived Property: " + name
                    + "\r\n#  Characters that can continue an identifier."
                    + "\r\n#  Generated from: ID_Start + Mn+Mc+Nd+Pc"
                    + "\r\n#  NOTE: Cf characters should be filtered out.";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierContinue_NO_Cf(cp, false);
            }
        };
        
        dprops[Mod_ID_Start] = new UnicodeProperty() {
            {
                name = "XID_Start";
                shortName = "XIDS";
                header = "# Derived Property: " + name
                    + "\r\n#  ID_Start modified for closure under NFKx"
                    + "\r\n#  Modified as described in UAX #15"
                    + "\r\n#  NOTE: Does NOT remove the non-NFKx characters."
                    + "\r\n#        Merely ensures that if isIdentifer(string) then isIdentifier(NFKx(string))";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierStart(cp, true);
            }
        };
        
        dprops[Mod_ID_Continue_NO_Cf] = new UnicodeProperty() {
            {
                name = "XID_Continue";
                shortName = "XIDC";
                header = "# Derived Property: " + name
                    + "\r\n#  Mod_ID_Continue modified for closure under NFKx"
                    + "\r\n#  Modified as described in UAX #15"
                    + "\r\n#  NOTE: Cf characters should be filtered out."
                    + "\r\n#  NOTE: Does NOT remove the non-NFKx characters."
                    + "\r\n#        Merely ensures that if isIdentifer(string) then isIdentifier(NFKx(string))";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierContinue_NO_Cf(cp, true);
            }
        };
        
        dprops[PropMath] = new UnicodeProperty() {
            {
                name = "Math";
                shortName = name;
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Sm + Other_Math";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Sm
                || ucdData.getBinaryProperty(cp,Math_Property)) return true;
                return false;
            }
        };
        
        dprops[PropAlphabetic] = new UnicodeProperty() {
            {
                name = "Alphabetic";
                shortName = "Alpha";
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Lu+Ll+Lt+Lm+Lo+Nl + Other_Alphabetic";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Lu || cat == Ll || cat == Lt || cat == Lm || cat == Lo || cat == Nl
                || ucdData.getBinaryProperty(cp, Alphabetic)) return true;
                return false;
            }
        };
        
        dprops[PropLowercase] = new UnicodeProperty() {
            {
                name = "Lowercase";
                shortName = "Lower";
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Ll + Other_Lowercase";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Ll
                || ucdData.getBinaryProperty(cp, Other_Lowercase)) return true;
                return false;
            }
        };
        
        dprops[PropUppercase] = new UnicodeProperty() {
            {
                name = "Uppercase";
                shortName = "Upper";
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Lu + Other_Uppercase";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Lu
                || ucdData.getBinaryProperty(cp, Other_Uppercase)) return true;
                return false;
            }
        };
        
        for (int i = Missing_Uppercase; i <= Missing_Mixedcase; ++i) {
            dprops[i] = new CaseDProp(i);
        }
        
/*
(3) Singleton Decompositions: characters that  can be derived from the UnicodeData file by
including all characters whose canonical decomposition consists of a single character.
(4) Non-Starter Decompositions: characters that  can be derived from the UnicodeData
file by including all characters whose canonical decomposition consists of a sequence
of characters, the first of which has a non-zero combining class.
*/
        dprops[FullCompExclusion] = new UnicodeProperty() {
            {
                name = "Full_Composition_Exclusion";
                shortName = "Comp_Ex";
                defaultStyle = SHORT;
                header = "# Derived Property: " + name
                    + ": Full Composition Exclusion"
                    + "\r\n#  Generated from: Composition Exclusions + Singletons + Non-Starter Decompositions";
            }
            boolean hasProperty(int cp) {
                if (!ucdData.isRepresented(cp)) return false;
                byte dtype = ucdData.getDecompositionType(cp);
                if (dtype != CANONICAL) return false;

                if (isCompEx(cp)) return true;
                return false;
            }
        };
        
        dprops[FullCompInclusion] = new UnicodeProperty() {
            {
                isStandard = false;
                name = "Full_Composition_Inclusion";
                shortName = "Comp_In";
                defaultStyle = SHORT;
                header = "# Derived Property: " + name
                    + ": Full Composition Inclusion"
                    + "\r\n#  characters with Canonical Decompositions MINUS Full Composition Exclusion";
            }
            boolean hasProperty(int cp) {
                if (!ucdData.isRepresented(cp)) return false;
                byte dtype = ucdData.getDecompositionType(cp);
                if (dtype != CANONICAL) return false;

                if (isCompEx(cp)) return true;
                return false;
            }
        };
        
        dprops[FC_NFKC_Closure] = new UnicodeProperty() {
            {
                name = "FC_NFKC_Closure";
                shortName = "FC_NFKC";
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from computing: b = NFKC(Fold(a)); c = NFKC(Fold(b));"
                    + "\r\n#  Then if (c != b) add the mapping from a to c to the set of"
                    + "\r\n#  mappings that constitute the FC_NFKC_Closure list";
            }
            public boolean propertyVaries() {return true;} // default
            public String getProperty(int cp) { 
                if (!ucdData.isRepresented(cp)) return "";
                String b = nfkc.normalize(fold(cp));
                String c = nfkc.normalize(fold(b));
                if (c.equals(b)) return "";
                return "FNC; " + Utility.hex(c);
            } // default
            boolean hasProperty(int cp) { return getProperty(cp).length() != 0; }
        };
        
        dprops[FC_NFC_Closure] = new UnicodeProperty() {
            {
                name = "FC_NFC_Closure";
                shortName = "FC_NFC";
                header = "# Derived Property: " + name
                    + "\r\n#  Generated from computing: b = NFC(Fold(a)); c = NFC(Fold(b));"
                    + "\r\n#  Then if (c != b) add the mapping from a to c to the set of"
                    + "\r\n#  mappings that constitute the FC_NFC_Closure list";
            }
            public boolean propertyVaries() {return true;} // default
            public String getProperty(int cp) { 
                if (!ucdData.isRepresented(cp)) return "";
                String b = nfc.normalize(fold(cp));
                String c = nfc.normalize(fold(b));
                if (c.equals(b)) return "";
                return "FN; " + Utility.hex(c);
            } // default
            boolean hasProperty(int cp) { return getProperty(cp).length() != 0; }
        };
        
        for (int i = QuickNFD; i <= QuickNFKC; ++i) {
            dprops[i] = new QuickDProp(i - QuickNFD);
        }        
        
        dprops[DefaultIgnorable] = new UnicodeProperty() {
            {
                name = "Default_Ignorable_Code_Point";
                shortName = "DI";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from Other_Default_Ignorable_Code_Point + Cf + Cc + Cs - White_Space";
            }
            boolean hasProperty(int cp) {
                if (ucdData.getBinaryProperty(cp, White_space)) return false;
                byte cat = ucdData.getCategory(cp);
                if (cat == Cf || cat == Cs || cat == Cc
                || ucdData.getBinaryProperty(cp,Reserved_Cf_Code_Point)) return true;
                return false;
            }
        };

/*
        GraphemeExtend = 27,
        GraphemeBase = 28,
# GraphemeExtend := Me + Mn + Mc + Other_GraphemeExtend - GraphemeLink
# GraphemeBase := 

*/
        dprops[GraphemeExtend] = new UnicodeProperty() {
            {
                name = "Grapheme_Extend";
                shortName = "GrExt";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Me + Mn + Mc + Other_Grapheme_Extend - Grapheme_Link"
                    + "\r\n#  Used in the definition of GraphemeCluster: "
                    + "\r\n#    GraphemeCluster ::= GraphameBase? ( Grapheme_Extend | Grapheme_Link Join_Control? Grapheme_Base? )*";
            }
            boolean hasProperty(int cp) {
                if (ucdData.getBinaryProperty(cp, GraphemeExtend)) return false;
                byte cat = ucdData.getCategory(cp);
                if (cat == Me || cat == Mn || cat == Mc
                || ucdData.getBinaryProperty(cp,Other_GraphemeExtend)) return true;
                return false;
            }
        };

        dprops[Other_Case_Ignorable] = new UnicodeProperty() {
            {
                name = "Other_Case_Ignorable";
                shortName = "OCI";
                
                header = header = "# Binary Property";
            }
            boolean hasProperty(int cp) {
                switch(cp) {
                    case 0x27: case 0x2019: case 0xAD: return true;
                    //  case 0x2d: case 0x2010: case 0x2011: 
/*
0027          ; Other_Case_Ignorable # Po       APOSTROPHE
00AD          ; Other_Case_Ignorable # Pd       SOFT HYPHEN
2019          ; Other_Case_Ignorable # Pf       RIGHT SINGLE QUOTATION MARK
*/
                }
                return false;
            }
        };
        
        dprops[Type_i] = new UnicodeProperty() {
            {
                name = "Special_Dotted";
                shortName = "SDot";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: all characters whose canonical decompositions end with a combining character sequence that"
                    + "\r\n# - starts with i or j"
                    + "\r\n# - has no combining marks above"
                    + "\r\n# - has no combining marks with zero canonical combining class"
                ;
            }
            boolean hasProperty(int cp) {
                if (cp == 'i' || cp == 'j') return true;
                if (!nfkd.hasDecomposition(cp)) return false;
                String decomp = nfd.normalize(cp);
                boolean ok = false;
                for (int i = decomp.length()-1; i >= 0; --i) {
                    char ch = decomp.charAt(i);
                    int cc = ucdData.getCombiningClass(ch);
                    if (cc == 230) return false;
                    if (cc == 0) {
                        if (ch == 'i' || ch == 'j') ok = true;
                        else return false;
                    }
                }
                return ok;
            }
        };
        
        dprops[Case_Ignorable] = new UnicodeProperty() {
            {
                name = "Case_Ignorable";
                shortName = "CI";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Other_Case_Ignorable + Lm + Mn + Me + Cf";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Lm || cat == Cf || cat == Mn || cat == Me) return true;
                if (dprops[Other_Case_Ignorable].hasProperty(cp)) return true;
                return false;
            }
        };
        
        dprops[GraphemeBase] = new UnicodeProperty() {
            {
                name = "Grapheme_Base";
                shortName = "GrBase";
                
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: [0..10FFFF] - Cc - Cf - Cs - Co - Cn - Zl - Zp - Grapheme_Link - Grapheme_Extend"
                    + "\r\n#  Used in the definition of GraphemeCluster: "
                    + "\r\n#    GraphemeCluster ::= GraphameBase? ( Grapheme_Extend | Grapheme_Link Join_Control? Grapheme_Base? )*";
            }
            boolean hasProperty(int cp) {
                byte cat = ucdData.getCategory(cp);
                if (cat == Cc || cat == Cf || cat == Cs || cat == Co || cat == Cn || cat == Zl || cat == Zp
                || ucdData.getBinaryProperty(cp,GraphemeLink)) return false;
                if (dprops[GraphemeExtend].hasProperty(cp)) return false;
                return true;
            }
        };
    }
    
    byte getDecompCat(int cp) {
        byte cat = ucdData.getCategory(cp);
        if (cat == Lu
            || ucdData.getBinaryProperty(cp, Other_Uppercase)) return Lu;
        if (cat == Ll
            || ucdData.getBinaryProperty(cp, Other_Lowercase)) return Ll;
        if (cat == Lt || cat == Lo || cat == Lm || cat == Nl) return cat;
        
        if (true) throw new IllegalArgumentException("FIX nf[2]");
        
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
    
    boolean isCompEx(int cp) {
        if (ucdData.getBinaryProperty(cp, CompositionExclusion)) return true;
        String decomp = ucdData.getDecompositionMapping(cp);
        if (UTF32.length32(decomp) == 1) return true;
        int first = UTF32.char32At(decomp,0);
        if (ucdData.getCombiningClass(first) != 0) return true;
        return false;
    }
    
    String fold(int cp) {
        return ucdData.getCase(cp, FULL, FOLD);
    }

    String fold(String s) {
        return ucdData.getCase(s, FULL, FOLD);
    }
    
    public static void test() {
        UCD ucd = UCD.make();
        DerivedProperty dprop = new DerivedProperty(ucd);
        /*
        for (int j = 0; j < LIMIT; ++j) {
            System.out.println();
            System.out.println(j + "\t" + dprop.getName(j));
            System.out.println(dprop.getHeader(j));
        }
        */
        
        for (int cp = 0xA0; cp < 0xFF; ++cp) {
            System.out.println();
            System.out.println(ucd.getCodeAndName(cp));
            for (int j = 0; j < LIMIT; ++j) {
                String prop = dprop.getProperty(cp, j);
                if (prop.length() != 0) System.out.println("\t" + prop);
            }
        }
    }
}
