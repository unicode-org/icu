/**************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 **************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/resources/Attic/LocaleElements_el.java,v $ 
 * $Date: 2001/11/21 00:51:58 $ 
 * $Revision: 1.5 $
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

":: [\\u0000-\\u007F \\u00B7 [:Greek:] [:nonspacing mark:]] ;"+
"::NFKD (NFC) ; "+

"# For modern Greek.\n"+

"# Useful variables\n"+

"$lower = [:Ll:] ; "+
"$upper = [:Lu:] ; "+
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

"[^[:L:][:M:]] } \u03bc\u03c0 > b ; "+
"\u03bc\u03c0 } [^[:L:][:M:]] > b ; "+
"[^[:L:][:M:]] } [\u039c\u03bc][\u03a0\u03c0] > B ; "+
"[\u039c\u03bc][\u03a0\u03c0] } [^[:L:][:M:]] > B ;"+

"\u03bc\u03c0 < b ; "+
"\u039c\u03c0 < B { $beforeLower ; "+
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

"$ignore = [[:Mark:]''] * ;"+

"| ch < h ;"+
"| k  < c ;"+
"| i  < j ;"+
"| k < q ;"+
"| y < u ;"+
"| y < w ;"+

"| Ch < H ;"+
"| K < C ;"+
"| I < J ;"+
"| K < Q ;"+
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

"::NFC (NFKD) ; "+

":: ([\\u0000-\\u007F [:Latin:] [:nonspacing mark:]]) ;"

              }
            }
        };
    }
}
