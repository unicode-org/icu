/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/DerivedProperty.java,v $
* $Date: 2001/09/01 00:06:48 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;

public class DerivedProperty implements UCD_Types {
  
    UCD ucdData;
    
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
        
        DefaultIgnorable = 26,
        GraphemeExtend = 27,
        GraphemeBase = 28,

        LIMIT = 29;
    
    
    public DerivedProperty(UCD ucd) {
      ucdData = ucd;
    }
    
    public String getHeader(int propNumber) {
        DProp dp = dprops[propNumber];
        if (dp != null) return dp.getHeader();
        else return "Unimplemented!!";
    }

    public String getName(int propNumber) {
        DProp dp = dprops[propNumber];
        if (dp != null) return dp.getName();
        else return "Unimplemented!!";
    }

    public String getProperty(int cp, int propNumber) {
        DProp dp = dprops[propNumber];
        if (dp != null) return dp.getProperty(cp);
        else return "Unimplemented!!";
    }
    
    public boolean isDefined(int propNumber) {
        return dprops[propNumber] != null;
    }
    
    public boolean hasProperty(int cp, int propNumber) {
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
    private DProp[] dprops = new DProp[50];
    private Normalizer[] nf = new Normalizer[4];
    private Normalizer nfd, nfc, nfkd, nfkc;
    static final String[] NAME = {"NFD", "NFC", "NFKD", "NFKC"};
    static final String[] CaseNames = {
                "Uppercase", 
                "Lowercase", 
                "Mixedcase"};
                    
    private abstract class DProp {
        String name, header;
        String getName() { return name; }
        String getHeader() { return header; }
        abstract boolean hasProperty(int cp);
        public boolean propertyVaries() { return false; }
        public String getProperty(int cp) { return hasProperty(cp) ? name : ""; }
    }
    
    class ExDProp extends DProp {
        Normalizer nfx;
        ExDProp(int i) {
            nfx = nf[i-ExpandsOnNFD];
            name = "Expands_On_" + NAME[i-ExpandsOnNFD];
            header = "# Derived Property: " + name
                + "\r\n#   Generated according to UAX #15."
                + "\r\n#   Characters whose normalized length is not one."
                + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
                + "\r\n#            The length of a normalized string is not necessarily the sum of the lengths of the normalized characters!";
        }
        boolean hasProperty(int cp) {
            if (ucdData.getDecompositionType(cp) == NONE) return false;
            String cps = UTF32.valueOf32(cp);
            if (UTF32.length32(nfx.normalize(cps)) == UTF32.length32(cps)) return true;
            return false;
        }
    };
    
    class GenDProp extends DProp {
        Normalizer nfx;
        GenDProp (int i) {
            nfx = nf[i-GenNFD];
            name = NAME[i-GenNFD];
            header = "# Derived Property: " + name              
                + "\r\n#   Normalized forms, where different from the characters themselves."
                + "\r\n#   HANGUL SYLLABLES are algorithmically decomposed, and not listed explicitly."
                + "\r\n#   WARNING: Normalization of STRINGS must use the algorithm in UAX #15 because characters may interact."
                + "\r\n#            It is NOT sufficient to replace characters one-by-one with these results!";
        }
        public boolean propertyVaries() {return true;} // default
        public String getProperty(int cp) { 
            if (ucdData.getDecompositionType(cp) == NONE) return "";
            String cps = UTF32.valueOf32(cp);
            if (cps.equals(nfx.normalize(cps))) {
                return "";
            }
            String norm = Utility.hex(nfx.normalize(cp));
            String pad = Utility.repeat(" ", 14-norm.length());
            return name + "; " + norm + pad;
            //if (cp >= 0xAC00 && cp <= 0xD7A3) return true;
            //System.out.println(Utility.hex(cps) + " => " + Utility.hex(nf[i-4].normalize(cps)));
        } // default
        boolean hasProperty(int cp) { return getProperty(cp).length() != 0; }
    };
    
    class CaseDProp extends DProp {
        byte val;
        CaseDProp (int i) {
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
    
    class QuickDProp extends DProp {
        String NO;
        String MAYBE;
        Normalizer nfx;
        QuickDProp (int i) {
            nfx = nf[i - QuickNFD];
            NO = NAME[i-QuickNFD] + "_NO";
            MAYBE = NAME[i-QuickNFD] + "_MAYBE";
            name = NAME[i-QuickNFD] + "_QuickCheck";
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
            dprops[i] = new ExDProp(i);
        }
        
        for (int i = GenNFD; i <= GenNFKC; ++i) {
            dprops[i] = new GenDProp(i);
        }
        
        dprops[ID_Start] = new DProp() {
            {
                name = "ID_Start";
                header = "# Derived Property: " + name
                    + "\r\n#  Characters that can start an identifier."
                    + "\r\n#  Generated from Lu+Ll+Lt+Lm+Lo+Nl";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierStart(cp, false);
            }
        };
        
        dprops[ID_Continue_NO_Cf] = new DProp() {
            {
                name = "ID_Continue";
                header = "# Derived Property: " + name
                    + "\r\n#  Characters that can continue an identifier."
                    + "\r\n#  Generated from: ID_Start + Mn+Mc+Nd+Pc"
                    + "\r\n#  NOTE: Cf characters should be filtered out.";
            }
            boolean hasProperty(int cp) {
                return ucdData.isIdentifierContinue_NO_Cf(cp, false);
            }
        };
        
        dprops[Mod_ID_Start] = new DProp() {
            {
                name = "XID_Start";
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
        
        dprops[Mod_ID_Continue_NO_Cf] = new DProp() {
            {
                name = "XID_Continue";
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
        
        dprops[PropMath] = new DProp() {
            {
                name = "Math";
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
        
        dprops[PropAlphabetic] = new DProp() {
            {
                name = "Alphabetic";
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
        
        dprops[PropLowercase] = new DProp() {
            {
                name = "Lowercase";
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
        
        dprops[PropUppercase] = new DProp() {
            {
                name = "Uppercase";
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
        dprops[FullCompExclusion] = new DProp() {
            {
                name = "Comp_Ex";
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
        
        dprops[FullCompInclusion] = new DProp() {
            {
                name = "Comp_In";
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
        
        dprops[FC_NFKC_Closure] = new DProp() {
            {
                name = "FC_NFKC_Closure";
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
        
        for (int i = QuickNFD; i <= QuickNFKC; ++i) {
            dprops[i] = new QuickDProp(i);
        }        
        
        dprops[DefaultIgnorable] = new DProp() {
            {
                name = "Default_Ignorable_Code_Point";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from Other_Default_Ignorable_Code_Point + Cf + Cc + Cs - WhiteSpace";
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
        dprops[GraphemeExtend] = new DProp() {
            {
                name = "GraphemeExtend";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: Me + Mn + Mc + Other_GraphemeExtend - GraphemeLink";
            }
            boolean hasProperty(int cp) {
                if (ucdData.getBinaryProperty(cp, GraphemeExtend)) return false;
                byte cat = ucdData.getCategory(cp);
                if (cat == Me || cat == Mn || cat == Mc
                || ucdData.getBinaryProperty(cp,Other_GraphemeExtend)) return true;
                return false;
            }
        };

        dprops[GraphemeBase] = new DProp() {
            {
                name = "GraphemeBase";
                header = header = "# Derived Property: " + name
                    + "\r\n#  Generated from: [0..10FFFF] - Cc - Cf - Cs - Co - Cn - Zl - Zp - GraphemeLink - GraphemeExtend";
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
