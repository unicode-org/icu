/**************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 **************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/LocaleElements_el.java,v $ 
 * $Date: 2001/11/30 22:27:01 $ 
 * $Revision: 1.8 $
 **************************************************************************
 */

package com.ibm.text.resources;

import java.util.ListResourceBundle;

public class LocaleElements_el extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Transliterate_LATIN",
              new String[] { "UNGEGN",

"# Rules are predicated on running NFD first, and NFC afterwards\n"+
// MINIMAL FILTER GENERATED FOR: Greek-Latin/UNGEGN
":: [;\u00B5\u00B7\u00C0\u00C2\u00C8\u00CA\u00CC\u00CE\u00D2\u00D4\u00D9\u00DB\u00E0\u00E2\u00E8\u00EA\u00EC\u00EE\u00F2\u00F4\u00F9\u00FB\u0108-\u0109\u011C-\u011D\u0124-\u0125\u0134-\u0135\u015C-\u015D\u0174-\u0177\u01DB-\u01DC\u01F8-\u01F9\u0300\u0302\u0313-\u0314\u0340\u0342-\u0343\u0345\u037A\u037E\u0386-\u038A\u038C\u038E-\u03A1\u03A3-\u03CE\u03D0-\u03D6\u03F0-\u03F5\u0400\u040D\u0450\u045D\u1E14-\u1E15\u1E50-\u1E51\u1E80-\u1E81\u1E90-\u1E91\u1EA4-\u1EAD\u1EB0-\u1EB1\u1EBE-\u1EC7\u1ED0-\u1ED9\u1EDC-\u1EDD\u1EEA-\u1EEB\u1EF2-\u1EF3\u1F00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC1-\u1FC4\u1FC6-\u1FCD\u1FCF-\u1FD3\u1FD6-\u1FDB\u1FDD\u1FDF-\u1FED\u1FF2-\u1FF4\u1FF6-\u1FFC\u2126] ;"+

// ":: [\\u0000-\\u007F \\u00B7 [:Greek:] [:nonspacing mark:]] ;"+
"::NFD (NFC) ; "+

"# For modern Greek.\n"+

"# Useful variables\n"+

"$lower = [[:latin:][:greek:] & [:Ll:]] ; "+
"$upper = [[:latin:][:greek:] & [:Lu:]] ; "+
"$accent = [:M:] ; "+

"$macron = \u0304 ;"+
"$ddot = \u0308 ;"+

"$lcgvowel = [\u03b1\u03b5\u03b7\u03b9\u03bf\u03c5\u03c9] ; "+
"$ucgvowel = [\u0391\u0395\u0397\u0399\u039f\u03a5\u03a9] ; "+
"$gvowel = [$lcgvowel $ucgvowel] ; "+
"$lcgvowelC = [$lcgvowel $accent] ; "+

"$evowel = [aeiouyAEIOUY];"+
"$vowel = [ $evowel $gvowel] ; "+

"$beforeLower = $accent * $lower ; "+

"$gammaLike = [\u0393\u039a\u039e\u03a7\u03b3\u03ba\u03be\u03c7\u03f0] ; "+
"$egammaLike = [GKXCgkxc] ; "+
"$smooth = \u0313 ; "+
"$rough = \u0314 ; "+
"$iotasub = \u0345 ; "+

"$softener = [\u03b2\u0392\u03b3\u0393\u03b4\u0394\u03b6\u0396\u03bb\u039b\u03bc\u039c\u03bd\u039d\u03c1\u03a1$gvowel] ;"+

"$under = \u0331;"+

"$caron = \u030C;"+

"$afterLetter = [:L:] [\\'[:M:]]* ;"+
"$beforeLetter = [\\'[:M:]]* [:L:] ;"+


"# Fix punctuation\n"+

"\\; <> \\? ;"+
"\u00b7 <> \\: ;"+

"# Fix any ancient characters that creep in\n"+

"\u0342 > \u0301 ;"+
"\u0302 > \u0301 ;"+
"\u0300 > \u0301 ;"+
"$smooth > ;"+
"$rough > ;"+
"$iotasub > ;"+
"\u037A > ;"+

"# need to have these up here so the rules don't mask\n"+

"\u03b7 <> i $under ;"+
"\u0397 <> I $under ;"+

"\u03a8 } $beforeLower <> Ps ; "+
"\u03a8 <> PS ; "+
"\u03c8 <> ps ; "+

"\u03c9 <> o $under ;"+
"\u03a9 <>  O $under;"+

"# at begining or end of word, convert mp to b\n"+

"[^[:L:][:M:]] { \u03bc\u03c0 > b ; "+
"\u03bc\u03c0 } [^[:L:][:M:]] > b ; "+
"[^[:L:][:M:]] { [\u039c\u03bc][\u03a0\u03c0] > B ; "+
"[\u039c\u03bc][\u03a0\u03c0] } [^[:L:][:M:]] > B ;"+

"\u03bc\u03c0 < b ; "+
"\u039c\u03c0 < B } $beforeLower ; "+
"\u039c\u03a0 < B ; "+

"# handle diphthongs ending with upsilon\n"+

"$vowel { \u03c5 } $softener <> v $under ; "+
"$vowel { \u03c5 } <> f $under; "+
"\u03c5 <> y ; "+
"$vowel { \u03a5 } $softener <> V $under ; "+
"$vowel { \u03a5 <> U $under ; "+
"\u03a5 <> Y ; "+

"# NORMAL\n"+

"\u03b1 <> a ; "+
"\u0391 <> A ; "+

"\u03b2 <> v ; "+
"\u0392 <> V ; "+

"\u03b3 } $gammaLike <> n } $egammaLike ; "+
"\u03b3 <> g ; "+
"\u0393 } $gammaLike <> N } $egammaLike ; "+
"\u0393 <> G ; "+

"\u03b4 <> d ; "+
"\u0394 <> D ; "+

"\u03b5 <> e ; "+
"\u0395 <> E ; "+

"\u03b6 <> z ; "+
"\u0396 <> Z ; "+

"\u03b8 <> th ; "+
"\u0398 } $beforeLower <> Th ; "+
"\u0398 <> TH ; "+

"\u03b9 <> i ; "+
"\u0399 <> I ; "+

"\u03ba <> k ;"+
"\u039a <> K ; "+

"\u03bb <> l ; "+
"\u039b <> L ; "+

"\u03bc <> m ; "+
"\u039c <> M ; "+

"\u03bd } $gammaLike > n\\' ; "+
"\u03bd <> n ; "+
"\u039d } $gammaLike <> N\\' ; "+
"\u039d <> N ; "+

"\u03be <> x ; "+
"\u039e <> X ; "+

"\u03bf <> o ; "+
"\u039f <> O ; "+

"\u03c0 <> p ; "+
"\u03a0 <> P ; "+

"\u03c1 <> r ; "+
"\u03a1 <> R ; "+

"[Pp] { } \u03c2 > \\' ; "+
"[Pp] { } \u03c3 > \\' ;"+

"# Caron means exception\n"+

"# before a letter, initial\n"+
"\u03c2 } $beforeLetter <> s $under } $beforeLetter;"+
"\u03c3 } $beforeLetter <> s } $beforeLetter;"+

"# otherwise, after a letter = final\n"+
"$afterLetter { \u03c3 <> $afterLetter { s $under;"+
"$afterLetter { \u03c2 <> $afterLetter { s ;"+

"# otherwise (isolated) = initial\n"+
"\u03c2 <> s $under;"+
"\u03c3 <> s ;"+

"[Pp] { \u03a3 <> \\'S ; "+
"\u03a3 <> S ; "+

"\u03c4 <> t ; "+
"\u03a4 <> T ; "+

"\u03c6 <> f ; "+
"\u03a6 <> F ;"+

"\u03c7 <> ch ; "+
"\u03a7 } $beforeLower <> Ch ; "+
"\u03a7 <> CH ; "+

"# Completeness for ASCII\n"+

// "$ignore = [[:Mark:]''] * ;"+

"| ch < h ;"+
"| k  < c ;"+
"| i  < j ;"+
"| k < q ;"+
"| b < u } $vowel ;"+
"| b < w } $vowel ;"+
"| y < u ;"+
"| y < w ;"+

"| Ch < H ;"+
"| K < C ;"+
"| I < J ;"+
"| K < Q ;"+
"| B < W } $vowel ;"+
"| B < U } $vowel ;"+
"| Y < W ;"+
"| Y < U ;"+

"# Completeness for Greek\n"+

"\u03d0 > | \u03b2 ;"+
"\u03d1 > | \u03b8 ;"+
"\u03d2 > | \u03a5 ;"+
"\u03d5 > | \u03c6 ;"+
"\u03d6 > | \u03c0 ;"+

"\u03f0 > | \u03ba ;"+
"\u03f1 > | \u03c1 ;"+
"\u03f2 > | \u03c3 ;"+
"\u03f3 > j ;"+
"\u03f4 > | \u0398 ;"+
"\u03f5 > | \u03b5 ;"+
"\u00B5 > | \u03BC ; " +

"# delete any trailing ' marks used for roundtripping\n"+

" < [\u03a0\u03c0] { \\' } [Ss] ;"+
" < [\u039d\u03bd] { \\' } $egammaLike ;"+

"::NFC (NFD) ; "+

// MINIMAL FILTER GENERATED FOR: Latin-Greek/UNGEGN BACKWARD
":: ( [':?A-Za-z\u00C0-\u00C5\u00C7-\u00CF\u00D1-\u00D6\u00D9-\u00DD\u00E0-\u00E5\u00E7-\u00EF\u00F1-\u00F6\u00F9-\u00FD\u00FF-\u010F\u0112-\u0125\u0128-\u0130\u0134-\u0137\u0139-\u013E\u0143-\u0148\u014C-\u0151\u0154-\u0165\u0168-\u017E\u01A0-\u01A1\u01AF-\u01B0\u01CD-\u01DC\u01DE-\u01E1\u01E6-\u01ED\u01F0\u01F4-\u01F5\u01F8-\u01FB\u0200-\u021B\u021E-\u021F\u0226-\u0233\u0331\u1E00-\u1E99\u1EA0-\u1EF9\u212A-\u212B] ) ;"

// ":: ([\\u0000-\\u007F [:Latin:] [:nonspacing mark:]]) ;"

              }
            }
        };
    }
}
