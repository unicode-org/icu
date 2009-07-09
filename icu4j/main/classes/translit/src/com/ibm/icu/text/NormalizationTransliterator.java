/*
**********************************************************************
*   Copyright (C) 2001-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/08/01    aliu        Creation.
**********************************************************************
*/

package com.ibm.icu.text;
import com.ibm.icu.lang.*;

/**
 * @author Alan Liu
 */
final class NormalizationTransliterator extends Transliterator {
    
    static final boolean DEBUG = false;

    /**
     * The normalization mode of this transliterator.
     */
    private Normalizer.Mode mode;

    /**
     * Normalization options for this transliterator.
     */
    private int options;

    /**
     * The set of "unsafe start" characters.  These are characters
     * with cc==0 but which may interact with previous characters.  We
     * effectively consider these to be cc!=0, for our purposes.
     *
     * From http://www.macchiato.com/utc/NFUnsafeStart-3.1.1dX.txt
     * Generated in unicodetools, NFSkippable
     *
     */
     
    static final UnicodeSet[] UNSAFE_STARTS = new UnicodeSet[4];
    static final UnicodeSet[] SKIPPABLES = new UnicodeSet[4];
    
    static final int
        D = 0, C = 1, KD= 2, KC = 3;
    
    // Instance data, simply pointer to one of the sets below
    final UnicodeSet unsafeStart;
    final UnicodeSet skippable;
    
    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory("Any-NFC", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.NFC);
            }
        });
        Transliterator.registerFactory("Any-NFD", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.NFD);
            }
        });
        Transliterator.registerFactory("Any-NFKC", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.NFKC);
            }
        });
        Transliterator.registerFactory("Any-NFKD", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.NFKD);
            }
        });
        Transliterator.registerSpecialInverse("NFC", "NFD", true);
        Transliterator.registerSpecialInverse("NFKC", "NFKD", true);
    }

    /**
     * Factory method.
     */
    public static NormalizationTransliterator getInstance(Normalizer.Mode m,
                                                          int opt) {
        StringBuffer id = new StringBuffer("NF");
        int choice = 0;
        if(m==Normalizer.NFC){
            id.append("C");
            choice |= C;
        }else if(m==Normalizer.NFKC){
            id.append("KC");
            choice |= KC;
        }else if(m==Normalizer.NFD){
            id.append("D");
            choice |= D;
        }else if(m==Normalizer.NFKD){
            id.append("KD");
            choice |= KD;
        }
        
        /*if (m.compat()) {
            id.append('K');
            choice |= KD;
        }
        if (m.compose()) {
            id.append('C');
            choice |= C;
        } else {
            id.append('D');
        }*/
        return new NormalizationTransliterator(id.toString(), m, choice, opt);
    }

    /**
     * Factory method.
     */
    public static NormalizationTransliterator getInstance(Normalizer.Mode m) {
        return getInstance(m, 0);
    }

    /**
     * Constructs a transliterator.
     */
    private NormalizationTransliterator(String id, Normalizer.Mode m, int startChoice,
                                        int opt) {
        super(id, null);
        mode = m;
        options = opt;
        if (UNSAFE_STARTS[startChoice] == null) {
            initStatics(startChoice);
        }
        // TODO: use built-in properties data rather than hardcoded sets; see ICU4C
        unsafeStart = UNSAFE_STARTS[startChoice];
        skippable = SKIPPABLES[startChoice];
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int start = offsets.start;
        int limit = offsets.limit;
        if (start >= limit) return;

        int overallDelta = 0;
            
        // Walk through the string looking for safe characters.
        // Whenever you hit one normalize from the start of the last
        // safe character up to just before the next safe character
        // Also, if you hit the end and we are not in incremental mode,
        // do to end.
            
        // TODO: fix for surrogates
        // TODO: add QuickCheck, so we rarely convert OK stuff
            
        int lastSafe = start; // go back to start in any event
        int cp;
        for (int i = start+1; i < limit; i += UTF16.getCharCount(cp)) {
            cp = text.char32At(i);
            if (UCharacter.getCombiningClass(cp) == 0 && !unsafeStart.contains(cp)) {
                int delta = convert(text, lastSafe, i, null);
                i += delta;
                limit += delta;
                overallDelta += delta;
                lastSafe = i;
            }
        }
        if (!isIncremental) {
            int delta = convert(text, lastSafe, limit, null);
            overallDelta += delta;
            lastSafe = limit + delta;
        } else {
            // We are incremental, so accept the last characters IF they turn into skippables
            int delta = convert(text, lastSafe, limit, skippable);
            if (delta != Integer.MIN_VALUE) {
                overallDelta += delta;
                lastSafe = limit + delta;
            }
        }
        offsets.contextLimit += overallDelta;
        offsets.limit += overallDelta;
        offsets.start = lastSafe;
    }
    
    /**
     * Converts the range from lastSafe to limit.
     * @param verify If non-null, check to see that all replacement characters are in it. If not,
     * abort the conversion and return Integer.MIN_VALUE.
     * @return return the delta in length (new - old), or Integer.MIN_VALUE if the verify aborted.
     */
    int convert(Replaceable text, int lastSafe, int limit, UnicodeSet verify) {        
        //System.out.println("t: " + com.ibm.icu.impl.Utility.hex(text.toString()) + ", s: " + lastSafe + ", l: " + limit);

        int len = limit - lastSafe;
        String input = null;
        synchronized (this) {
            if (buffer.length < len) {
                buffer = new char[len]; // rare, and we don't care if we grow too large
            }
            text.getChars(lastSafe, limit, buffer, 0);
            input = new String(buffer, 0, len); // TODO: fix normalizer to take char[]
        }
        String output = Normalizer.normalize(input, mode, options);
        
        // verify OK, if specified
        if (verify != null) {
            boolean skip = !skippable.containsAll(output);
            if (DEBUG) {
                System.out.println((skip ? "  SKIP: " : "NOSKIP: ") 
                    + com.ibm.icu.impl.Utility.escape(input) 
                    + " => " + com.ibm.icu.impl.Utility.escape(output));
            }
            if (skip) return Integer.MIN_VALUE;
        }
        
        if (output.equals(input)) {
            return 0;
        }
        text.replace(lastSafe, limit, output);
        return output.length() - len;
    }
    
    private char buffer[] = new char[30];
    
    /**
     * Initialize statics for the given mode.  This is slow, so we
     * defer it.
     */
    private static final void initStatics(int startChoice) {
        switch (startChoice) {
        case D:
          UNSAFE_STARTS[D] = new UnicodeSet("[\u0F73\u0F75\u0F81]", false);
          SKIPPABLES[D] = new UnicodeSet(
              "[^\\u00C0-\\u00C5\\u00C7-\\u00CF\\u00D1-\\u00D6\\u00D9-\\u00DD"
            + "\\u00E0-\\u00E5\\u00E7-\\u00EF\\u00F1-\\u00F6\\u00F9-\\u00FD"
            + "\\u00FF-\\u010F\\u0112-\\u0125\\u0128-\\u0130\\u0134-\\u0137"
            + "\\u0139-\\u013E\\u0143-\\u0148\\u014C-\\u0151\\u0154-\\u0165"
            + "\\u0168-\\u017E\\u01A0\\u01A1\\u01AF\\u01B0\\u01CD-\\u01DC"
            + "\\u01DE-\\u01E3\\u01E6-\\u01F0\\u01F4\\u01F5\\u01F8-\\u021B"
            + "\\u021E\\u021F\\u0226-\\u0233\\u0300-\\u034E\\u0350-\\u036F"
            + "\\u0374\\u037E\\u0385-\\u038A\\u038C\\u038E-\\u0390\\u03AA-"
            + "\\u03B0\\u03CA-\\u03CE\\u03D3\\u03D4\\u0400\\u0401\\u0403\\u0407"
            + "\\u040C-\\u040E\\u0419\\u0439\\u0450\\u0451\\u0453\\u0457\\u045C"
            + "-\\u045E\\u0476\\u0477\\u0483-\\u0486\\u04C1\\u04C2\\u04D0-"
            + "\\u04D3\\u04D6\\u04D7\\u04DA-\\u04DF\\u04E2-\\u04E7\\u04EA-"
            + "\\u04F5\\u04F8\\u04F9\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4"
            + "\\u05C5\\u05C7\\u0610-\\u0615\\u0622-\\u0626\\u064B-\\u065E"
            + "\\u0670\\u06C0\\u06C2\\u06D3\\u06D6-\\u06DC\\u06DF-\\u06E4"
            + "\\u06E7\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-"
            + "\\u07F3\\u0929\\u0931\\u0934\\u093C\\u094D\\u0951-\\u0954\\u0958"
            + "-\\u095F\\u09BC\\u09CB-\\u09CD\\u09DC\\u09DD\\u09DF\\u0A33"
            + "\\u0A36\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C"
            + "\\u0B48\\u0B4B-\\u0B4D\\u0B5C\\u0B5D\\u0B94\\u0BCA-\\u0BCD"
            + "\\u0C48\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CC0\\u0CC7\\u0CC8\\u0CCA"
            + "\\u0CCB\\u0CCD\\u0D4A-\\u0D4D\\u0DCA\\u0DDA\\u0DDC-\\u0DDE"
            + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB8\\u0EB9\\u0EC8-\\u0ECB"
            + "\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D\\u0F52\\u0F57"
            + "\\u0F5C\\u0F69\\u0F71-\\u0F76\\u0F78\\u0F7A-\\u0F7D\\u0F80-"
            + "\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
            + "\\u0FC6\\u1026\\u1037\\u1039\\u135F\\u1714\\u1734\\u17D2\\u17DD"
            + "\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1B34\\u1B44\\u1B6B-"
            + "\\u1B73\\u1DC0-\\u1DCA\\u1DFE-\\u1E99\\u1E9B\\u1EA0-\\u1EF9"
            + "\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D"
            + "\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-"
            + "\\u1FB4\\u1FB6-\\u1FBC\\u1FBE\\u1FC1-\\u1FC4\\u1FC6-\\u1FD3"
            + "\\u1FD6-\\u1FDB\\u1FDD-\\u1FEF\\u1FF2-\\u1FF4\\u1FF6-\\u1FFD"
            + "\\u2000\\u2001\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20EF\\u2126"
            + "\\u212A\\u212B\\u219A\\u219B\\u21AE\\u21CD-\\u21CF\\u2204\\u2209"
            + "\\u220C\\u2224\\u2226\\u2241\\u2244\\u2247\\u2249\\u2260\\u2262"
            + "\\u226D-\\u2271\\u2274\\u2275\\u2278\\u2279\\u2280\\u2281\\u2284"
            + "\\u2285\\u2288\\u2289\\u22AC-\\u22AF\\u22E0-\\u22E3\\u22EA-"
            + "\\u22ED\\u2329\\u232A\\u2ADC\\u302A-\\u302F\\u304C\\u304E\\u3050"
            + "\\u3052\\u3054\\u3056\\u3058\\u305A\\u305C\\u305E\\u3060\\u3062"
            + "\\u3065\\u3067\\u3069\\u3070\\u3071\\u3073\\u3074\\u3076\\u3077"
            + "\\u3079\\u307A\\u307C\\u307D\\u3094\\u3099\\u309A\\u309E\\u30AC"
            + "\\u30AE\\u30B0\\u30B2\\u30B4\\u30B6\\u30B8\\u30BA\\u30BC\\u30BE"
            + "\\u30C0\\u30C2\\u30C5\\u30C7\\u30C9\\u30D0\\u30D1\\u30D3\\u30D4"
            + "\\u30D6\\u30D7\\u30D9\\u30DA\\u30DC\\u30DD\\u30F4\\u30F7-\\u30FA"
            + "\\u30FE\\uA806\\uAC00-\\uD7A3\\uF900-\\uFA0D\\uFA10\\uFA12"
            + "\\uFA15-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A-\\uFA2D"
            + "\\uFA30-\\uFA6A\\uFA70-\\uFAD9\\uFB1D-\\uFB1F\\uFB2A-\\uFB36"
            + "\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-"
            + "\\uFB4E\\uFE20-\\uFE23\\U00010A0D\\U00010A0F\\U00010A38-\\U00010"
            + "A3A\\U00010A3F\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001D172"
            + "\\U0001D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-"
            + "\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0002"
            + "F800-\\U0002FA1D]", false);
          break;
        case C:
          UNSAFE_STARTS[C] = new UnicodeSet(
              "[\\u09BE\\u09D7\\u0B3E\\u0B56\\u0B57\\u0BBE\\u0BD7\\u0CC2\\u0CD5"
            + "\\u0CD6\\u0D3E\\u0D57\\u0DCF\\u0DDF\\u0F73\\u0F75\\u0F81\\u102E"
            + "\\u1161-\\u1175\\u11A7-\\u11C2]", false);
          SKIPPABLES[C] = new UnicodeSet(
              "[^<->A-PR-Za-pr-z\\u00A8\\u00C0-\\u00CF\\u00D1-\\u00D6\\u00D8-"
            + "\\u00DD\\u00E0-\\u00EF\\u00F1-\\u00F6\\u00F8-\\u00FD\\u00FF-"
            + "\\u0103\\u0106-\\u010F\\u0112-\\u0117\\u011A-\\u0121\\u0124"
            + "\\u0125\\u0128-\\u012D\\u0130\\u0139\\u013A\\u013D\\u013E\\u0143"
            + "\\u0144\\u0147\\u0148\\u014C-\\u0151\\u0154\\u0155\\u0158-"
            + "\\u015D\\u0160\\u0161\\u0164\\u0165\\u0168-\\u0171\\u0174-"
            + "\\u017F\\u01A0\\u01A1\\u01AF\\u01B0\\u01B7\\u01CD-\\u01DC\\u01DE"
            + "-\\u01E1\\u01E6-\\u01EB\\u01F4\\u01F5\\u01F8-\\u01FB\\u0200-"
            + "\\u021B\\u021E\\u021F\\u0226-\\u0233\\u0292\\u0300-\\u034E"
            + "\\u0350-\\u036F\\u0374\\u037E\\u0387\\u0391\\u0395\\u0397\\u0399"
            + "\\u039F\\u03A1\\u03A5\\u03A9\\u03AC\\u03AE\\u03B1\\u03B5\\u03B7"
            + "\\u03B9\\u03BF\\u03C1\\u03C5\\u03C9-\\u03CB\\u03CE\\u03D2\\u0406"
            + "\\u0410\\u0413\\u0415-\\u0418\\u041A\\u041E\\u0423\\u0427\\u042B"
            + "\\u042D\\u0430\\u0433\\u0435-\\u0438\\u043A\\u043E\\u0443\\u0447"
            + "\\u044B\\u044D\\u0456\\u0474\\u0475\\u0483-\\u0486\\u04D8\\u04D9"
            + "\\u04E8\\u04E9\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5"
            + "\\u05C7\\u0610-\\u0615\\u0622\\u0623\\u0627\\u0648\\u064A-"
            + "\\u065E\\u0670\\u06C1\\u06D2\\u06D5-\\u06DC\\u06DF-\\u06E4"
            + "\\u06E7\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-"
            + "\\u07F3\\u0928\\u0930\\u0933\\u093C\\u094D\\u0951-\\u0954\\u0958"
            + "-\\u095F\\u09BC\\u09BE\\u09C7\\u09CD\\u09D7\\u09DC\\u09DD\\u09DF"
            + "\\u0A33\\u0A36\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD"
            + "\\u0B3C\\u0B3E\\u0B47\\u0B4D\\u0B56\\u0B57\\u0B5C\\u0B5D\\u0B92"
            + "\\u0BBE\\u0BC6\\u0BC7\\u0BCD\\u0BD7\\u0C46\\u0C4D\\u0C55\\u0C56"
            + "\\u0CBC\\u0CBF\\u0CC2\\u0CC6\\u0CCA\\u0CCD\\u0CD5\\u0CD6\\u0D3E"
            + "\\u0D46\\u0D47\\u0D4D\\u0D57\\u0DCA\\u0DCF\\u0DD9\\u0DDC\\u0DDF"
            + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB8\\u0EB9\\u0EC8-\\u0ECB"
            + "\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D\\u0F52\\u0F57"
            + "\\u0F5C\\u0F69\\u0F71-\\u0F76\\u0F78\\u0F7A-\\u0F7D\\u0F80-"
            + "\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
            + "\\u0FC6\\u1025\\u102E\\u1037\\u1039\\u1100-\\u1112\\u1161-"
            + "\\u1175\\u11A8-\\u11C2\\u135F\\u1714\\u1734\\u17D2\\u17DD\\u18A9"
            + "\\u1939-\\u193B\\u1A17\\u1A18\\u1B34\\u1B44\\u1B6B-\\u1B73"
            + "\\u1DC0-\\u1DCA\\u1DFE-\\u1E03\\u1E0A-\\u1E0F\\u1E12-\\u1E1B"
            + "\\u1E20-\\u1E27\\u1E2A-\\u1E41\\u1E44-\\u1E53\\u1E58-\\u1E7D"
            + "\\u1E80-\\u1E87\\u1E8E-\\u1E91\\u1E96-\\u1E99\\u1EA0-\\u1EF3"
            + "\\u1EF6-\\u1EF9\\u1F00-\\u1F11\\u1F18\\u1F19\\u1F20-\\u1F31"
            + "\\u1F38\\u1F39\\u1F40\\u1F41\\u1F48\\u1F49\\u1F50\\u1F51\\u1F59"
            + "\\u1F60-\\u1F71\\u1F73-\\u1F75\\u1F77\\u1F79\\u1F7B-\\u1F7D"
            + "\\u1F80\\u1F81\\u1F88\\u1F89\\u1F90\\u1F91\\u1F98\\u1F99\\u1FA0"
            + "\\u1FA1\\u1FA8\\u1FA9\\u1FB3\\u1FB6\\u1FBB\\u1FBC\\u1FBE\\u1FBF"
            + "\\u1FC3\\u1FC6\\u1FC9\\u1FCB\\u1FCC\\u1FD3\\u1FDB\\u1FE3\\u1FEB"
            + "\\u1FEE\\u1FEF\\u1FF3\\u1FF6\\u1FF9\\u1FFB-\\u1FFE\\u2000\\u2001"
            + "\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20EF\\u2126\\u212A\\u212B"
            + "\\u2190\\u2192\\u2194\\u21D0\\u21D2\\u21D4\\u2203\\u2208\\u220B"
            + "\\u2223\\u2225\\u223C\\u2243\\u2245\\u2248\\u224D\\u2261\\u2264"
            + "\\u2265\\u2272\\u2273\\u2276\\u2277\\u227A-\\u227D\\u2282\\u2283"
            + "\\u2286\\u2287\\u2291\\u2292\\u22A2\\u22A8\\u22A9\\u22AB\\u22B2-"
            + "\\u22B5\\u2329\\u232A\\u2ADC\\u302A-\\u302F\\u3046\\u304B\\u304D"
            + "\\u304F\\u3051\\u3053\\u3055\\u3057\\u3059\\u305B\\u305D\\u305F"
            + "\\u3061\\u3064\\u3066\\u3068\\u306F\\u3072\\u3075\\u3078\\u307B"
            + "\\u3099\\u309A\\u309D\\u30A6\\u30AB\\u30AD\\u30AF\\u30B1\\u30B3"
            + "\\u30B5\\u30B7\\u30B9\\u30BB\\u30BD\\u30BF\\u30C1\\u30C4\\u30C6"
            + "\\u30C8\\u30CF\\u30D2\\u30D5\\u30D8\\u30DB\\u30EF-\\u30F2\\u30FD"
            + "\\uA806\\uAC00\\uAC1C\\uAC38\\uAC54\\uAC70\\uAC8C\\uACA8\\uACC4"
            + "\\uACE0\\uACFC\\uAD18\\uAD34\\uAD50\\uAD6C\\uAD88\\uADA4\\uADC0"
            + "\\uADDC\\uADF8\\uAE14\\uAE30\\uAE4C\\uAE68\\uAE84\\uAEA0\\uAEBC"
            + "\\uAED8\\uAEF4\\uAF10\\uAF2C\\uAF48\\uAF64\\uAF80\\uAF9C\\uAFB8"
            + "\\uAFD4\\uAFF0\\uB00C\\uB028\\uB044\\uB060\\uB07C\\uB098\\uB0B4"
            + "\\uB0D0\\uB0EC\\uB108\\uB124\\uB140\\uB15C\\uB178\\uB194\\uB1B0"
            + "\\uB1CC\\uB1E8\\uB204\\uB220\\uB23C\\uB258\\uB274\\uB290\\uB2AC"
            + "\\uB2C8\\uB2E4\\uB300\\uB31C\\uB338\\uB354\\uB370\\uB38C\\uB3A8"
            + "\\uB3C4\\uB3E0\\uB3FC\\uB418\\uB434\\uB450\\uB46C\\uB488\\uB4A4"
            + "\\uB4C0\\uB4DC\\uB4F8\\uB514\\uB530\\uB54C\\uB568\\uB584\\uB5A0"
            + "\\uB5BC\\uB5D8\\uB5F4\\uB610\\uB62C\\uB648\\uB664\\uB680\\uB69C"
            + "\\uB6B8\\uB6D4\\uB6F0\\uB70C\\uB728\\uB744\\uB760\\uB77C\\uB798"
            + "\\uB7B4\\uB7D0\\uB7EC\\uB808\\uB824\\uB840\\uB85C\\uB878\\uB894"
            + "\\uB8B0\\uB8CC\\uB8E8\\uB904\\uB920\\uB93C\\uB958\\uB974\\uB990"
            + "\\uB9AC\\uB9C8\\uB9E4\\uBA00\\uBA1C\\uBA38\\uBA54\\uBA70\\uBA8C"
            + "\\uBAA8\\uBAC4\\uBAE0\\uBAFC\\uBB18\\uBB34\\uBB50\\uBB6C\\uBB88"
            + "\\uBBA4\\uBBC0\\uBBDC\\uBBF8\\uBC14\\uBC30\\uBC4C\\uBC68\\uBC84"
            + "\\uBCA0\\uBCBC\\uBCD8\\uBCF4\\uBD10\\uBD2C\\uBD48\\uBD64\\uBD80"
            + "\\uBD9C\\uBDB8\\uBDD4\\uBDF0\\uBE0C\\uBE28\\uBE44\\uBE60\\uBE7C"
            + "\\uBE98\\uBEB4\\uBED0\\uBEEC\\uBF08\\uBF24\\uBF40\\uBF5C\\uBF78"
            + "\\uBF94\\uBFB0\\uBFCC\\uBFE8\\uC004\\uC020\\uC03C\\uC058\\uC074"
            + "\\uC090\\uC0AC\\uC0C8\\uC0E4\\uC100\\uC11C\\uC138\\uC154\\uC170"
            + "\\uC18C\\uC1A8\\uC1C4\\uC1E0\\uC1FC\\uC218\\uC234\\uC250\\uC26C"
            + "\\uC288\\uC2A4\\uC2C0\\uC2DC\\uC2F8\\uC314\\uC330\\uC34C\\uC368"
            + "\\uC384\\uC3A0\\uC3BC\\uC3D8\\uC3F4\\uC410\\uC42C\\uC448\\uC464"
            + "\\uC480\\uC49C\\uC4B8\\uC4D4\\uC4F0\\uC50C\\uC528\\uC544\\uC560"
            + "\\uC57C\\uC598\\uC5B4\\uC5D0\\uC5EC\\uC608\\uC624\\uC640\\uC65C"
            + "\\uC678\\uC694\\uC6B0\\uC6CC\\uC6E8\\uC704\\uC720\\uC73C\\uC758"
            + "\\uC774\\uC790\\uC7AC\\uC7C8\\uC7E4\\uC800\\uC81C\\uC838\\uC854"
            + "\\uC870\\uC88C\\uC8A8\\uC8C4\\uC8E0\\uC8FC\\uC918\\uC934\\uC950"
            + "\\uC96C\\uC988\\uC9A4\\uC9C0\\uC9DC\\uC9F8\\uCA14\\uCA30\\uCA4C"
            + "\\uCA68\\uCA84\\uCAA0\\uCABC\\uCAD8\\uCAF4\\uCB10\\uCB2C\\uCB48"
            + "\\uCB64\\uCB80\\uCB9C\\uCBB8\\uCBD4\\uCBF0\\uCC0C\\uCC28\\uCC44"
            + "\\uCC60\\uCC7C\\uCC98\\uCCB4\\uCCD0\\uCCEC\\uCD08\\uCD24\\uCD40"
            + "\\uCD5C\\uCD78\\uCD94\\uCDB0\\uCDCC\\uCDE8\\uCE04\\uCE20\\uCE3C"
            + "\\uCE58\\uCE74\\uCE90\\uCEAC\\uCEC8\\uCEE4\\uCF00\\uCF1C\\uCF38"
            + "\\uCF54\\uCF70\\uCF8C\\uCFA8\\uCFC4\\uCFE0\\uCFFC\\uD018\\uD034"
            + "\\uD050\\uD06C\\uD088\\uD0A4\\uD0C0\\uD0DC\\uD0F8\\uD114\\uD130"
            + "\\uD14C\\uD168\\uD184\\uD1A0\\uD1BC\\uD1D8\\uD1F4\\uD210\\uD22C"
            + "\\uD248\\uD264\\uD280\\uD29C\\uD2B8\\uD2D4\\uD2F0\\uD30C\\uD328"
            + "\\uD344\\uD360\\uD37C\\uD398\\uD3B4\\uD3D0\\uD3EC\\uD408\\uD424"
            + "\\uD440\\uD45C\\uD478\\uD494\\uD4B0\\uD4CC\\uD4E8\\uD504\\uD520"
            + "\\uD53C\\uD558\\uD574\\uD590\\uD5AC\\uD5C8\\uD5E4\\uD600\\uD61C"
            + "\\uD638\\uD654\\uD670\\uD68C\\uD6A8\\uD6C4\\uD6E0\\uD6FC\\uD718"
            + "\\uD734\\uD750\\uD76C\\uD788\\uF900-\\uFA0D\\uFA10\\uFA12\\uFA15"
            + "-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A-\\uFA2D\\uFA30-"
            + "\\uFA6A\\uFA70-\\uFAD9\\uFB1D-\\uFB1F\\uFB2A-\\uFB36\\uFB38-"
            + "\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-\\uFB4E\\uFE20"
            + "-\\uFE23\\U00010A0D\\U00010A0F\\U00010A38-\\U00010A3A\\U00010A3F"
            + "\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001D172\\U0001D17B-"
            + "\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-\\U0001D1AD\\U0001"
            + "D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0002F800-\\U0002FA1D]", false);
          break;
        case KD:
          UNSAFE_STARTS[KD] = new UnicodeSet("[\u0F73\u0F75\u0F81\uFF9E-\uFF9F]", false);
          SKIPPABLES[KD] = new UnicodeSet(
              "[^\\u00A0\\u00A8\\u00AA\\u00AF\\u00B2-\\u00B5\\u00B8-\\u00BA"
            + "\\u00BC-\\u00BE\\u00C0-\\u00C5\\u00C7-\\u00CF\\u00D1-\\u00D6"
            + "\\u00D9-\\u00DD\\u00E0-\\u00E5\\u00E7-\\u00EF\\u00F1-\\u00F6"
            + "\\u00F9-\\u00FD\\u00FF-\\u010F\\u0112-\\u0125\\u0128-\\u0130"
            + "\\u0132-\\u0137\\u0139-\\u0140\\u0143-\\u0149\\u014C-\\u0151"
            + "\\u0154-\\u0165\\u0168-\\u017F\\u01A0\\u01A1\\u01AF\\u01B0"
            + "\\u01C4-\\u01DC\\u01DE-\\u01E3\\u01E6-\\u01F5\\u01F8-\\u021B"
            + "\\u021E\\u021F\\u0226-\\u0233\\u02B0-\\u02B8\\u02D8-\\u02DD"
            + "\\u02E0-\\u02E4\\u0300-\\u034E\\u0350-\\u036F\\u0374\\u037A"
            + "\\u037E\\u0384-\\u038A\\u038C\\u038E-\\u0390\\u03AA-\\u03B0"
            + "\\u03CA-\\u03CE\\u03D0-\\u03D6\\u03F0-\\u03F2\\u03F4\\u03F5"
            + "\\u03F9\\u0400\\u0401\\u0403\\u0407\\u040C-\\u040E\\u0419\\u0439"
            + "\\u0450\\u0451\\u0453\\u0457\\u045C-\\u045E\\u0476\\u0477\\u0483"
            + "-\\u0486\\u04C1\\u04C2\\u04D0-\\u04D3\\u04D6\\u04D7\\u04DA-"
            + "\\u04DF\\u04E2-\\u04E7\\u04EA-\\u04F5\\u04F8\\u04F9\\u0587"
            + "\\u0591-\\u05BD\\u05BF\\u05C1\\u05C2\\u05C4\\u05C5\\u05C7\\u0610"
            + "-\\u0615\\u0622-\\u0626\\u064B-\\u065E\\u0670\\u0675-\\u0678"
            + "\\u06C0\\u06C2\\u06D3\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7"
            + "\\u06E8\\u06EA-\\u06ED\\u0711\\u0730-\\u074A\\u07EB-\\u07F3"
            + "\\u0929\\u0931\\u0934\\u093C\\u094D\\u0951-\\u0954\\u0958-"
            + "\\u095F\\u09BC\\u09CB-\\u09CD\\u09DC\\u09DD\\u09DF\\u0A33\\u0A36"
            + "\\u0A3C\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C\\u0B48"
            + "\\u0B4B-\\u0B4D\\u0B5C\\u0B5D\\u0B94\\u0BCA-\\u0BCD\\u0C48"
            + "\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CC0\\u0CC7\\u0CC8\\u0CCA\\u0CCB"
            + "\\u0CCD\\u0D4A-\\u0D4D\\u0DCA\\u0DDA\\u0DDC-\\u0DDE\\u0E33"
            + "\\u0E38-\\u0E3A\\u0E48-\\u0E4B\\u0EB3\\u0EB8\\u0EB9\\u0EC8-"
            + "\\u0ECB\\u0EDC\\u0EDD\\u0F0C\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39"
            + "\\u0F43\\u0F4D\\u0F52\\u0F57\\u0F5C\\u0F69\\u0F71-\\u0F7D\\u0F80"
            + "-\\u0F84\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9"
            + "\\u0FC6\\u1026\\u1037\\u1039\\u10FC\\u135F\\u1714\\u1734\\u17D2"
            + "\\u17DD\\u18A9\\u1939-\\u193B\\u1A17\\u1A18\\u1B34\\u1B44\\u1B6B"
            + "-\\u1B73\\u1D2C-\\u1D2E\\u1D30-\\u1D3A\\u1D3C-\\u1D4D\\u1D4F-"
            + "\\u1D6A\\u1D78\\u1D9B-\\u1DCA\\u1DFE-\\u1E9B\\u1EA0-\\u1EF9"
            + "\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D"
            + "\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-"
            + "\\u1FB4\\u1FB6-\\u1FC4\\u1FC6-\\u1FD3\\u1FD6-\\u1FDB\\u1FDD-"
            + "\\u1FEF\\u1FF2-\\u1FF4\\u1FF6-\\u1FFE\\u2000-\\u200A\\u2011"
            + "\\u2017\\u2024-\\u2026\\u202F\\u2033\\u2034\\u2036\\u2037\\u203C"
            + "\\u203E\\u2047-\\u2049\\u2057\\u205F\\u2070\\u2071\\u2074-"
            + "\\u208E\\u2090-\\u2094\\u20A8\\u20D0-\\u20DC\\u20E1\\u20E5-"
            + "\\u20EF\\u2100-\\u2103\\u2105-\\u2107\\u2109-\\u2113\\u2115"
            + "\\u2116\\u2119-\\u211D\\u2120-\\u2122\\u2124\\u2126\\u2128"
            + "\\u212A-\\u212D\\u212F-\\u2131\\u2133-\\u2139\\u213B-\\u2140"
            + "\\u2145-\\u2149\\u2153-\\u217F\\u219A\\u219B\\u21AE\\u21CD-"
            + "\\u21CF\\u2204\\u2209\\u220C\\u2224\\u2226\\u222C\\u222D\\u222F"
            + "\\u2230\\u2241\\u2244\\u2247\\u2249\\u2260\\u2262\\u226D-\\u2271"
            + "\\u2274\\u2275\\u2278\\u2279\\u2280\\u2281\\u2284\\u2285\\u2288"
            + "\\u2289\\u22AC-\\u22AF\\u22E0-\\u22E3\\u22EA-\\u22ED\\u2329"
            + "\\u232A\\u2460-\\u24EA\\u2A0C\\u2A74-\\u2A76\\u2ADC\\u2D6F"
            + "\\u2E9F\\u2EF3\\u2F00-\\u2FD5\\u3000\\u302A-\\u302F\\u3036"
            + "\\u3038-\\u303A\\u304C\\u304E\\u3050\\u3052\\u3054\\u3056\\u3058"
            + "\\u305A\\u305C\\u305E\\u3060\\u3062\\u3065\\u3067\\u3069\\u3070"
            + "\\u3071\\u3073\\u3074\\u3076\\u3077\\u3079\\u307A\\u307C\\u307D"
            + "\\u3094\\u3099-\\u309C\\u309E\\u309F\\u30AC\\u30AE\\u30B0\\u30B2"
            + "\\u30B4\\u30B6\\u30B8\\u30BA\\u30BC\\u30BE\\u30C0\\u30C2\\u30C5"
            + "\\u30C7\\u30C9\\u30D0\\u30D1\\u30D3\\u30D4\\u30D6\\u30D7\\u30D9"
            + "\\u30DA\\u30DC\\u30DD\\u30F4\\u30F7-\\u30FA\\u30FE\\u30FF\\u3131"
            + "-\\u318E\\u3192-\\u319F\\u3200-\\u321E\\u3220-\\u3243\\u3250-"
            + "\\u327E\\u3280-\\u32FE\\u3300-\\u33FF\\uA806\\uAC00-\\uD7A3"
            + "\\uF900-\\uFA0D\\uFA10\\uFA12\\uFA15-\\uFA1E\\uFA20\\uFA22"
            + "\\uFA25\\uFA26\\uFA2A-\\uFA2D\\uFA30-\\uFA6A\\uFA70-\\uFAD9"
            + "\\uFB00-\\uFB06\\uFB13-\\uFB17\\uFB1D-\\uFB36\\uFB38-\\uFB3C"
            + "\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-\\uFBB1\\uFBD3-"
            + "\\uFD3D\\uFD50-\\uFD8F\\uFD92-\\uFDC7\\uFDF0-\\uFDFC\\uFE10-"
            + "\\uFE19\\uFE20-\\uFE23\\uFE30-\\uFE44\\uFE47-\\uFE52\\uFE54-"
            + "\\uFE66\\uFE68-\\uFE6B\\uFE70-\\uFE72\\uFE74\\uFE76-\\uFEFC"
            + "\\uFF01-\\uFFBE\\uFFC2-\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7"
            + "\\uFFDA-\\uFFDC\\uFFE0-\\uFFE6\\uFFE8-\\uFFEE\\U00010A0D\\U00010"
            + "A0F\\U00010A38-\\U00010A3A\\U00010A3F\\U0001D15E-\\U0001D169"
            + "\\U0001D16D-\\U0001D172\\U0001D17B-\\U0001D182\\U0001D185-"
            + "\\U0001D18B\\U0001D1AA-\\U0001D1AD\\U0001D1BB-\\U0001D1C0\\U0001"
            + "D242-\\U0001D244\\U0001D400-\\U0001D454\\U0001D456-\\U0001D49C"
            + "\\U0001D49E\\U0001D49F\\U0001D4A2\\U0001D4A5\\U0001D4A6\\U0001D4"
            + "A9-\\U0001D4AC\\U0001D4AE-\\U0001D4B9\\U0001D4BB\\U0001D4BD-"
            + "\\U0001D4C3\\U0001D4C5-\\U0001D505\\U0001D507-\\U0001D50A\\U0001"
            + "D50D-\\U0001D514\\U0001D516-\\U0001D51C\\U0001D51E-\\U0001D539"
            + "\\U0001D53B-\\U0001D53E\\U0001D540-\\U0001D544\\U0001D546\\U0001"
            + "D54A-\\U0001D550\\U0001D552-\\U0001D6A5\\U0001D6A8-\\U0001D7CB"
            + "\\U0001D7CE-\\U0001D7FF\\U0002F800-\\U0002FA1D]", false);
          break;
        case KC:
          UNSAFE_STARTS[KC] = new UnicodeSet(
              "[\\u09BE\\u09D7\\u0B3E\\u0B56\\u0B57\\u0BBE\\u0BD7\\u0CC2\\u0CD5"
            + "\\u0CD6\\u0D3E\\u0D57\\u0DCF\\u0DDF\\u0F73\\u0F75\\u0F81\\u102E"
            + "\\u1161-\\u1175\\u11A7-\\u11C2\\u3133\\u3135\\u3136\\u313A-"
            + "\\u313F\\u314F-\\u3163\\uFF9E\\uFF9F\\uFFA3\\uFFA5\\uFFA6\\uFFAA"
            + "-\\uFFAF\\uFFC2-\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7\\uFFDA-"
            + "\\uFFDC]", false);
          SKIPPABLES[KC] = new UnicodeSet(
              "[^<->A-PR-Za-pr-z\\u00A0\\u00A8\\u00AA\\u00AF\\u00B2-\\u00B5"
            + "\\u00B8-\\u00BA\\u00BC-\\u00BE\\u00C0-\\u00CF\\u00D1-\\u00D6"
            + "\\u00D8-\\u00DD\\u00E0-\\u00EF\\u00F1-\\u00F6\\u00F8-\\u00FD"
            + "\\u00FF-\\u0103\\u0106-\\u010F\\u0112-\\u0117\\u011A-\\u0121"
            + "\\u0124\\u0125\\u0128-\\u012D\\u0130\\u0132\\u0133\\u0139\\u013A"
            + "\\u013D-\\u0140\\u0143\\u0144\\u0147-\\u0149\\u014C-\\u0151"
            + "\\u0154\\u0155\\u0158-\\u015D\\u0160\\u0161\\u0164\\u0165\\u0168"
            + "-\\u0171\\u0174-\\u017F\\u01A0\\u01A1\\u01AF\\u01B0\\u01B7"
            + "\\u01C4-\\u01DC\\u01DE-\\u01E1\\u01E6-\\u01EB\\u01F1-\\u01F5"
            + "\\u01F8-\\u01FB\\u0200-\\u021B\\u021E\\u021F\\u0226-\\u0233"
            + "\\u0292\\u02B0-\\u02B8\\u02D8-\\u02DD\\u02E0-\\u02E4\\u0300-"
            + "\\u034E\\u0350-\\u036F\\u0374\\u037A\\u037E\\u0384\\u0385\\u0387"
            + "\\u0391\\u0395\\u0397\\u0399\\u039F\\u03A1\\u03A5\\u03A9\\u03AC"
            + "\\u03AE\\u03B1\\u03B5\\u03B7\\u03B9\\u03BF\\u03C1\\u03C5\\u03C9-"
            + "\\u03CB\\u03CE\\u03D0-\\u03D6\\u03F0-\\u03F2\\u03F4\\u03F5"
            + "\\u03F9\\u0406\\u0410\\u0413\\u0415-\\u0418\\u041A\\u041E\\u0423"
            + "\\u0427\\u042B\\u042D\\u0430\\u0433\\u0435-\\u0438\\u043A\\u043E"
            + "\\u0443\\u0447\\u044B\\u044D\\u0456\\u0474\\u0475\\u0483-\\u0486"
            + "\\u04D8\\u04D9\\u04E8\\u04E9\\u0587\\u0591-\\u05BD\\u05BF\\u05C1"
            + "\\u05C2\\u05C4\\u05C5\\u05C7\\u0610-\\u0615\\u0622\\u0623\\u0627"
            + "\\u0648\\u064A-\\u065E\\u0670\\u0675-\\u0678\\u06C1\\u06D2"
            + "\\u06D5-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED"
            + "\\u0711\\u0730-\\u074A\\u07EB-\\u07F3\\u0928\\u0930\\u0933"
            + "\\u093C\\u094D\\u0951-\\u0954\\u0958-\\u095F\\u09BC\\u09BE"
            + "\\u09C7\\u09CD\\u09D7\\u09DC\\u09DD\\u09DF\\u0A33\\u0A36\\u0A3C"
            + "\\u0A4D\\u0A59-\\u0A5B\\u0A5E\\u0ABC\\u0ACD\\u0B3C\\u0B3E\\u0B47"
            + "\\u0B4D\\u0B56\\u0B57\\u0B5C\\u0B5D\\u0B92\\u0BBE\\u0BC6\\u0BC7"
            + "\\u0BCD\\u0BD7\\u0C46\\u0C4D\\u0C55\\u0C56\\u0CBC\\u0CBF\\u0CC2"
            + "\\u0CC6\\u0CCA\\u0CCD\\u0CD5\\u0CD6\\u0D3E\\u0D46\\u0D47\\u0D4D"
            + "\\u0D57\\u0DCA\\u0DCF\\u0DD9\\u0DDC\\u0DDF\\u0E33\\u0E38-\\u0E3A"
            + "\\u0E48-\\u0E4B\\u0EB3\\u0EB8\\u0EB9\\u0EC8-\\u0ECB\\u0EDC"
            + "\\u0EDD\\u0F0C\\u0F18\\u0F19\\u0F35\\u0F37\\u0F39\\u0F43\\u0F4D"
            + "\\u0F52\\u0F57\\u0F5C\\u0F69\\u0F71-\\u0F7D\\u0F80-\\u0F84"
            + "\\u0F86\\u0F87\\u0F93\\u0F9D\\u0FA2\\u0FA7\\u0FAC\\u0FB9\\u0FC6"
            + "\\u1025\\u102E\\u1037\\u1039\\u10FC\\u1100-\\u1112\\u1161-"
            + "\\u1175\\u11A8-\\u11C2\\u135F\\u1714\\u1734\\u17D2\\u17DD\\u18A9"
            + "\\u1939-\\u193B\\u1A17\\u1A18\\u1B34\\u1B44\\u1B6B-\\u1B73"
            + "\\u1D2C-\\u1D2E\\u1D30-\\u1D3A\\u1D3C-\\u1D4D\\u1D4F-\\u1D6A"
            + "\\u1D78\\u1D9B-\\u1DCA\\u1DFE-\\u1E03\\u1E0A-\\u1E0F\\u1E12-"
            + "\\u1E1B\\u1E20-\\u1E27\\u1E2A-\\u1E41\\u1E44-\\u1E53\\u1E58-"
            + "\\u1E7D\\u1E80-\\u1E87\\u1E8E-\\u1E91\\u1E96-\\u1E9B\\u1EA0-"
            + "\\u1EF3\\u1EF6-\\u1EF9\\u1F00-\\u1F11\\u1F18\\u1F19\\u1F20-"
            + "\\u1F31\\u1F38\\u1F39\\u1F40\\u1F41\\u1F48\\u1F49\\u1F50\\u1F51"
            + "\\u1F59\\u1F60-\\u1F71\\u1F73-\\u1F75\\u1F77\\u1F79\\u1F7B-"
            + "\\u1F7D\\u1F80\\u1F81\\u1F88\\u1F89\\u1F90\\u1F91\\u1F98\\u1F99"
            + "\\u1FA0\\u1FA1\\u1FA8\\u1FA9\\u1FB3\\u1FB6\\u1FBB-\\u1FC1\\u1FC3"
            + "\\u1FC6\\u1FC9\\u1FCB-\\u1FCF\\u1FD3\\u1FDB\\u1FDD-\\u1FDF"
            + "\\u1FE3\\u1FEB\\u1FED-\\u1FEF\\u1FF3\\u1FF6\\u1FF9\\u1FFB-"
            + "\\u1FFE\\u2000-\\u200A\\u2011\\u2017\\u2024-\\u2026\\u202F"
            + "\\u2033\\u2034\\u2036\\u2037\\u203C\\u203E\\u2047-\\u2049\\u2057"
            + "\\u205F\\u2070\\u2071\\u2074-\\u208E\\u2090-\\u2094\\u20A8"
            + "\\u20D0-\\u20DC\\u20E1\\u20E5-\\u20EF\\u2100-\\u2103\\u2105-"
            + "\\u2107\\u2109-\\u2113\\u2115\\u2116\\u2119-\\u211D\\u2120-"
            + "\\u2122\\u2124\\u2126\\u2128\\u212A-\\u212D\\u212F-\\u2131"
            + "\\u2133-\\u2139\\u213B-\\u2140\\u2145-\\u2149\\u2153-\\u217F"
            + "\\u2190\\u2192\\u2194\\u21D0\\u21D2\\u21D4\\u2203\\u2208\\u220B"
            + "\\u2223\\u2225\\u222C\\u222D\\u222F\\u2230\\u223C\\u2243\\u2245"
            + "\\u2248\\u224D\\u2261\\u2264\\u2265\\u2272\\u2273\\u2276\\u2277"
            + "\\u227A-\\u227D\\u2282\\u2283\\u2286\\u2287\\u2291\\u2292\\u22A2"
            + "\\u22A8\\u22A9\\u22AB\\u22B2-\\u22B5\\u2329\\u232A\\u2460-"
            + "\\u24EA\\u2A0C\\u2A74-\\u2A76\\u2ADC\\u2D6F\\u2E9F\\u2EF3\\u2F00"
            + "-\\u2FD5\\u3000\\u302A-\\u302F\\u3036\\u3038-\\u303A\\u3046"
            + "\\u304B\\u304D\\u304F\\u3051\\u3053\\u3055\\u3057\\u3059\\u305B"
            + "\\u305D\\u305F\\u3061\\u3064\\u3066\\u3068\\u306F\\u3072\\u3075"
            + "\\u3078\\u307B\\u3099-\\u309D\\u309F\\u30A6\\u30AB\\u30AD\\u30AF"
            + "\\u30B1\\u30B3\\u30B5\\u30B7\\u30B9\\u30BB\\u30BD\\u30BF\\u30C1"
            + "\\u30C4\\u30C6\\u30C8\\u30CF\\u30D2\\u30D5\\u30D8\\u30DB\\u30EF-"
            + "\\u30F2\\u30FD\\u30FF\\u3131-\\u318E\\u3192-\\u319F\\u3200-"
            + "\\u321E\\u3220-\\u3243\\u3250-\\u327E\\u3280-\\u32FE\\u3300-"
            + "\\u33FF\\uA806\\uAC00\\uAC1C\\uAC38\\uAC54\\uAC70\\uAC8C\\uACA8"
            + "\\uACC4\\uACE0\\uACFC\\uAD18\\uAD34\\uAD50\\uAD6C\\uAD88\\uADA4"
            + "\\uADC0\\uADDC\\uADF8\\uAE14\\uAE30\\uAE4C\\uAE68\\uAE84\\uAEA0"
            + "\\uAEBC\\uAED8\\uAEF4\\uAF10\\uAF2C\\uAF48\\uAF64\\uAF80\\uAF9C"
            + "\\uAFB8\\uAFD4\\uAFF0\\uB00C\\uB028\\uB044\\uB060\\uB07C\\uB098"
            + "\\uB0B4\\uB0D0\\uB0EC\\uB108\\uB124\\uB140\\uB15C\\uB178\\uB194"
            + "\\uB1B0\\uB1CC\\uB1E8\\uB204\\uB220\\uB23C\\uB258\\uB274\\uB290"
            + "\\uB2AC\\uB2C8\\uB2E4\\uB300\\uB31C\\uB338\\uB354\\uB370\\uB38C"
            + "\\uB3A8\\uB3C4\\uB3E0\\uB3FC\\uB418\\uB434\\uB450\\uB46C\\uB488"
            + "\\uB4A4\\uB4C0\\uB4DC\\uB4F8\\uB514\\uB530\\uB54C\\uB568\\uB584"
            + "\\uB5A0\\uB5BC\\uB5D8\\uB5F4\\uB610\\uB62C\\uB648\\uB664\\uB680"
            + "\\uB69C\\uB6B8\\uB6D4\\uB6F0\\uB70C\\uB728\\uB744\\uB760\\uB77C"
            + "\\uB798\\uB7B4\\uB7D0\\uB7EC\\uB808\\uB824\\uB840\\uB85C\\uB878"
            + "\\uB894\\uB8B0\\uB8CC\\uB8E8\\uB904\\uB920\\uB93C\\uB958\\uB974"
            + "\\uB990\\uB9AC\\uB9C8\\uB9E4\\uBA00\\uBA1C\\uBA38\\uBA54\\uBA70"
            + "\\uBA8C\\uBAA8\\uBAC4\\uBAE0\\uBAFC\\uBB18\\uBB34\\uBB50\\uBB6C"
            + "\\uBB88\\uBBA4\\uBBC0\\uBBDC\\uBBF8\\uBC14\\uBC30\\uBC4C\\uBC68"
            + "\\uBC84\\uBCA0\\uBCBC\\uBCD8\\uBCF4\\uBD10\\uBD2C\\uBD48\\uBD64"
            + "\\uBD80\\uBD9C\\uBDB8\\uBDD4\\uBDF0\\uBE0C\\uBE28\\uBE44\\uBE60"
            + "\\uBE7C\\uBE98\\uBEB4\\uBED0\\uBEEC\\uBF08\\uBF24\\uBF40\\uBF5C"
            + "\\uBF78\\uBF94\\uBFB0\\uBFCC\\uBFE8\\uC004\\uC020\\uC03C\\uC058"
            + "\\uC074\\uC090\\uC0AC\\uC0C8\\uC0E4\\uC100\\uC11C\\uC138\\uC154"
            + "\\uC170\\uC18C\\uC1A8\\uC1C4\\uC1E0\\uC1FC\\uC218\\uC234\\uC250"
            + "\\uC26C\\uC288\\uC2A4\\uC2C0\\uC2DC\\uC2F8\\uC314\\uC330\\uC34C"
            + "\\uC368\\uC384\\uC3A0\\uC3BC\\uC3D8\\uC3F4\\uC410\\uC42C\\uC448"
            + "\\uC464\\uC480\\uC49C\\uC4B8\\uC4D4\\uC4F0\\uC50C\\uC528\\uC544"
            + "\\uC560\\uC57C\\uC598\\uC5B4\\uC5D0\\uC5EC\\uC608\\uC624\\uC640"
            + "\\uC65C\\uC678\\uC694\\uC6B0\\uC6CC\\uC6E8\\uC704\\uC720\\uC73C"
            + "\\uC758\\uC774\\uC790\\uC7AC\\uC7C8\\uC7E4\\uC800\\uC81C\\uC838"
            + "\\uC854\\uC870\\uC88C\\uC8A8\\uC8C4\\uC8E0\\uC8FC\\uC918\\uC934"
            + "\\uC950\\uC96C\\uC988\\uC9A4\\uC9C0\\uC9DC\\uC9F8\\uCA14\\uCA30"
            + "\\uCA4C\\uCA68\\uCA84\\uCAA0\\uCABC\\uCAD8\\uCAF4\\uCB10\\uCB2C"
            + "\\uCB48\\uCB64\\uCB80\\uCB9C\\uCBB8\\uCBD4\\uCBF0\\uCC0C\\uCC28"
            + "\\uCC44\\uCC60\\uCC7C\\uCC98\\uCCB4\\uCCD0\\uCCEC\\uCD08\\uCD24"
            + "\\uCD40\\uCD5C\\uCD78\\uCD94\\uCDB0\\uCDCC\\uCDE8\\uCE04\\uCE20"
            + "\\uCE3C\\uCE58\\uCE74\\uCE90\\uCEAC\\uCEC8\\uCEE4\\uCF00\\uCF1C"
            + "\\uCF38\\uCF54\\uCF70\\uCF8C\\uCFA8\\uCFC4\\uCFE0\\uCFFC\\uD018"
            + "\\uD034\\uD050\\uD06C\\uD088\\uD0A4\\uD0C0\\uD0DC\\uD0F8\\uD114"
            + "\\uD130\\uD14C\\uD168\\uD184\\uD1A0\\uD1BC\\uD1D8\\uD1F4\\uD210"
            + "\\uD22C\\uD248\\uD264\\uD280\\uD29C\\uD2B8\\uD2D4\\uD2F0\\uD30C"
            + "\\uD328\\uD344\\uD360\\uD37C\\uD398\\uD3B4\\uD3D0\\uD3EC\\uD408"
            + "\\uD424\\uD440\\uD45C\\uD478\\uD494\\uD4B0\\uD4CC\\uD4E8\\uD504"
            + "\\uD520\\uD53C\\uD558\\uD574\\uD590\\uD5AC\\uD5C8\\uD5E4\\uD600"
            + "\\uD61C\\uD638\\uD654\\uD670\\uD68C\\uD6A8\\uD6C4\\uD6E0\\uD6FC"
            + "\\uD718\\uD734\\uD750\\uD76C\\uD788\\uF900-\\uFA0D\\uFA10\\uFA12"
            + "\\uFA15-\\uFA1E\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A-\\uFA2D"
            + "\\uFA30-\\uFA6A\\uFA70-\\uFAD9\\uFB00-\\uFB06\\uFB13-\\uFB17"
            + "\\uFB1D-\\uFB36\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43"
            + "\\uFB44\\uFB46-\\uFBB1\\uFBD3-\\uFD3D\\uFD50-\\uFD8F\\uFD92-"
            + "\\uFDC7\\uFDF0-\\uFDFC\\uFE10-\\uFE19\\uFE20-\\uFE23\\uFE30-"
            + "\\uFE44\\uFE47-\\uFE52\\uFE54-\\uFE66\\uFE68-\\uFE6B\\uFE70-"
            + "\\uFE72\\uFE74\\uFE76-\\uFEFC\\uFF01-\\uFFBE\\uFFC2-\\uFFC7"
            + "\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7\\uFFDA-\\uFFDC\\uFFE0-\\uFFE6"
            + "\\uFFE8-\\uFFEE\\U00010A0D\\U00010A0F\\U00010A38-\\U00010A3A"
            + "\\U00010A3F\\U0001D15E-\\U0001D169\\U0001D16D-\\U0001D172\\U0001"
            + "D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-\\U0001D1AD"
            + "\\U0001D1BB-\\U0001D1C0\\U0001D242-\\U0001D244\\U0001D400-"
            + "\\U0001D454\\U0001D456-\\U0001D49C\\U0001D49E\\U0001D49F\\U0001D"
            + "4A2\\U0001D4A5\\U0001D4A6\\U0001D4A9-\\U0001D4AC\\U0001D4AE-"
            + "\\U0001D4B9\\U0001D4BB\\U0001D4BD-\\U0001D4C3\\U0001D4C5-\\U0001"
            + "D505\\U0001D507-\\U0001D50A\\U0001D50D-\\U0001D514\\U0001D516-"
            + "\\U0001D51C\\U0001D51E-\\U0001D539\\U0001D53B-\\U0001D53E\\U0001"
            + "D540-\\U0001D544\\U0001D546\\U0001D54A-\\U0001D550\\U0001D552-"
            + "\\U0001D6A5\\U0001D6A8-\\U0001D7CB\\U0001D7CE-\\U0001D7FF\\U0002"
            + "F800-\\U0002FA1D]", false);
          break;
        }
    }    
}
