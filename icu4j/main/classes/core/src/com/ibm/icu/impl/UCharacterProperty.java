// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.MissingResourceException;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacter.HangulSyllableType;
import com.ibm.icu.lang.UCharacter.NumericType;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.CodePointMap;
import com.ibm.icu.util.CodePointTrie;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;

/**
* <p>Internal class used for Unicode character property database.</p>
* <p>This classes store binary data read from uprops.icu.
* It does not have the capability to parse the data into more high-level
* information. It only returns bytes of information when required.</p>
* <p>Due to the form most commonly used for retrieval, array of char is used
* to store the binary data.</p>
* <p>UCharacterPropertyDB also contains information on accessing indexes to
* significant points in the binary data.</p>
* <p>Responsibility for molding the binary data into more meaning form lies on
* <a href=UCharacter.html>UCharacter</a>.</p>
* @author Syn Wee Quek
* @since release 2.1, february 1st 2002
*/

public final class UCharacterProperty
{
    // public data members -----------------------------------------------

    /*
     * public singleton instance
     */
    public static final UCharacterProperty INSTANCE;

    /**
    * Trie data
    */
    public Trie2_16 m_trie_;
    /**
    * Unicode version
    */
    public VersionInfo m_unicodeVersion_;
    /**
    * Latin capital letter i with dot above
    */
    public static final char LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_ = 0x130;
    /**
    * Latin small letter i with dot above
    */
    public static final char LATIN_SMALL_LETTER_DOTLESS_I_ = 0x131;
    /**
    * Latin lowercase i
    */
    public static final char LATIN_SMALL_LETTER_I_ = 0x69;
    /**
    * Character type mask
    */
    public static final int TYPE_MASK = 0x1F;

    // uprops.h enum UPropertySource --------------------------------------- ***

    /** No source, not a supported property. */
    public static final int SRC_NONE=0;
    /** From uchar.c/uprops.icu main trie */
    public static final int SRC_CHAR=1;
    /** From uchar.c/uprops.icu properties vectors trie */
    public static final int SRC_PROPSVEC=2;
    /** From unames.c/unames.icu */
    public static final int SRC_NAMES=3;
    /** From ucase.c/ucase.icu */
    public static final int SRC_CASE=4;
    /** From ubidi_props.c/ubidi.icu */
    public static final int SRC_BIDI=5;
    /** From uchar.c/uprops.icu main trie as well as properties vectors trie */
    public static final int SRC_CHAR_AND_PROPSVEC=6;
    /** From ucase.c/ucase.icu as well as unorm.cpp/unorm.icu */
    public static final int SRC_CASE_AND_NORM=7;
    /** From normalizer2impl.cpp/nfc.nrm */
    public static final int SRC_NFC=8;
    /** From normalizer2impl.cpp/nfkc.nrm */
    public static final int SRC_NFKC=9;
    /** From normalizer2impl.cpp/nfkc_cf.nrm */
    public static final int SRC_NFKC_CF=10;
    /** From normalizer2impl.cpp/nfc.nrm canonical iterator data */
    public static final int SRC_NFC_CANON_ITER=11;
    // Text layout properties.
    public static final int SRC_INPC=12;
    public static final int SRC_INSC=13;
    public static final int SRC_VO=14;
    /** One more than the highest UPropertySource (SRC_) constant. */
    public static final int SRC_COUNT=15;

    // hardcoded text layout properties ----------------------------------
    // TODO(ICU-20111): move to a data file and load on demand

    private static final CodePointTrie makeTrie(String data) {
        // One char == one byte.
        // U+0000 and U+007A='z' are swapped because
        // Java class String literals encode U+0000 and U+0080..U+07FF in two bytes.
        byte[] bytes = new byte[data.length()];
        for (int i = 0; i < bytes.length; ++i) {
            char c = data.charAt(i);
            if (c == 0) {
                c = 'z';
            } else if (c == 'z') {
                c = 0;
            }
            assert 0 <= c && c <= 0xff;
            bytes[i] = (byte)c;
        }
        return CodePointTrie.fromBinary(null, null, ByteBuffer.wrap(bytes));
    }

    // Do not store the data in static String variables because
    // those would not be garbage-collected.

    private static final class InPCTrie {
        static final CodePointTrie INSTANCE = makeTrie(
                "\63\151\162\124\102z\37\3\226\13\2zzz\220z" +
                "zz\100zzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\200z\300z\377z\77\1" +
                "\176\1\276\1\176\1\376\1\76\2\176\2\274\2\374\2" +
                "\74\3\173\3\76\2\273\3\373\3\71\4\167\4\255\4" +
                "\341\4\41\5\61\5\161\5\231\5\331\5\31\6\126\6" +
                "\327\2\350\2\364\2\350\2\17\3zz\20z\40z" +
                "\60z\100z\120z\140z\160zzz\20z\40z" +
                "\60zzz\20z\40z\60zzz\20z\40z" +
                "\60zzz\20z\40z\60zzz\20z\40z" +
                "\60zzz\20z\40z\60zzz\20z\40z" +
                "\60zzz\20z\40z\60z\200z\220z\240z" +
                "\260z\300z\320z\340z\360z\377z\17\1\37\1" +
                "\57\1\77\1\117\1\137\1\157\1\176\1\216\1\236\1" +
                "\256\1\276\1\316\1\336\1\356\1\176\1\216\1\236\1" +
                "\256\1\376\1\16\2\36\2\56\2\76\2\116\2\136\2" +
                "\156\2\176\2\216\2\236\2\256\2\274\2\314\2\334\2" +
                "\354\2\374\2\14\3\34\3\54\3\74\3\114\3\134\3" +
                "\154\3\173\3\213\3\233\3\253\3\76\2\116\2\136\2" +
                "\156\2\273\3\313\3\333\3\353\3\373\3\13\4\33\4" +
                "\53\4\71\4\111\4\131\4\151\4\167\4\207\4\227\4" +
                "\247\4\255\4\275\4\315\4\335\4\341\4\361\4\1\5" +
                "\21\5\41\5\61\5\101\5\121\5\61\5\101\5\121\5" +
                "\141\5\161\5\201\5\221\5\241\5\231\5\251\5\271\5" +
                "\311\5\331\5\351\5\371\5\11\6\31\6\51\6\71\6" +
                "\111\6\126\6\146\6\166\6\206\6zzzz\213\6" +
                "\232\6zz\251\6\270\6\307\6\325\6\345\6zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzz\363\6zz\363\6" +
                "zz\1\7zz\1\7zzzzzz\13\7" +
                "\33\7\51\7zzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\71\7\111\7zzzz" +
                "zzzzzzzzzz\131\7\150\7zz" +
                "zzzz\162\7zzzzzz\176\7\215\7" +
                "\233\7zzzzzzzzzzzzzz" +
                "zz\253\7zzzz\267\7\307\7zz\314\7" +
                "\54\5\201zzz\334\7zzzzzz\352\7" +
                "\373\3zzzz\372\7\7\10zzzzzz" +
                "zzzzzzzzzzzz\27\10\47\10" +
                "\65\10zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\263\2zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\276\2zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zz\103\10zz\120\10zzzzzzzz" +
                "zz\1\1zzzz\134\10\150\10zz\170\10" +
                "\206\10zzzz\226\10zz\244\10\373\3zz" +
                "zz\200zzzzz\264\10\304\10zz\271\2" +
                "zzzz\313\10\332\10\347\10zzzz\365\10" +
                "zzzzzz\5\11\275\2zz\25\11\121\1" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\45\11zz" +
                "\64\11zzzz\104\11zzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\124\11zzzz\134\11\152\11zzzzzz" +
                "\201zzzzz\0\11zzzzzzzz" +
                "\55\5zz\205\11\225\11\313\3zzzz\314\1" +
                "\201zzzzz\242\11\262\11zzzzzz" +
                "\277\11\317\11zzzzzzzzzzzz" +
                "zzzzzz\161z\337\11zz\377zzz" +
                "zz\352\11\372\11\117\1\10\12\53\5zzzz" +
                "zzzzzzzzzzzz\240\11\30\12" +
                "\157\1zzzzzzzzzz\50\12\67\12" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzz\353\2\107\12\343z" +
                "\24\2zzzzzz\127\12\276\2zzzz" +
                "zzzzzz\147\12\167\12zzzzzz" +
                "zzzz\177\12\217\12zzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zz\233\12\252\12zzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\271\12\311\12zz\330\12" +
                "zzzz\345\12zz\364\12zzzzz\13" +
                "\12\13zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzz\353\2" +
                "\32\13zzzzzzzzzz\52\13\62\13" +
                "\101\13zzzzzzzzzzzzzz" +
                "\120\13\137\13zzzzzz\147\13\167\13zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\204\13zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzz\105z" +
                "\115z\115z\115z\135z\175z\235z\275z\335z" +
                "\2z\2z\354z\12\1\51\1\111\1\2z\151\1" +
                "\2z\2z\2z\2z\2z\2z\2z\2z" +
                "\2z\2z\2z\2z\2z\2z\2z\2z" +
                "\2z\2z\2z\2z\2z\2z\2z\2z" +
                "\2z\2z\2z\2z\2z\2z\2z\2z" +
                "\211\1\250\1\2z\2z\2z\2z\2z\2z" +
                "\2z\2z\2z\2z\310\1\2z\2z\350\1" +
                "\6\2\43\2\101\2\137\2\177\2\235\2\267\2zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzz\10\10" +
                "\10\7zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\10\7\1z\7\4\7\1" +
                "\1\1\1\10\10\10\10\7\7\7\7\1\4\7z\10" +
                "\1\10\10\10\1\1zzzzzzzzzz" +
                "\1\1zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzz\10\7" +
                "\7zzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzz\1z\7\4\7\1\1" +
                "\1\1zz\4\4zz\5\5\1zzzzz" +
                "zzzz\7zzzzzzzzzz\1" +
                "\1zzzzzzzzzzzzzzz" +
                "zzzzzzzzzzz\10z\10\10\7" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\1z\7\4\7\1\1z" +
                "zzz\10\10zz\10\10\1zzz\1zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\10\10zz" +
                "z\1zzzzzzzzzz\7\1\1\1" +
                "\1\10z\10\10\15z\7\7\1zzzzzz" +
                "zzzzzzzzzzzzzz\1\1" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzz\10\10\10\10\10\10z\10\7\7" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\1z\7\10\7\1\1\1" +
                "\1zz\4\13zz\5\14\1zzzzzz" +
                "zz\10\15zzzzzzzzzz\1\1" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\10zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\7\7\10\7\7zzz" +
                "\4\4\4z\5\5\5\10zzzzzzzz" +
                "z\7zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzz\10\7\7\7\10z" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\10\10\7\7\7\7z\10" +
                "\10\11z\10\10\10\10zzzzzzz\10\1" +
                "zzzzzzzzzzz\1\1zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzz\15\7\7\7\7z\10" +
                "\15\15z\15\15\10\10zzzzzzz\7\7" +
                "zzzzzzzzzzz\1\1zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzz\10\10\7\7zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzz\10\10z\7\7\7\1\1z\4\4\4" +
                "z\5\5\5\10zzzzzzzzz\7z" +
                "zzzzzzzzz\1\1zzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzz\7\7zzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzz\10zzzz\7\7\7\10\10\1" +
                "z\1z\7\4\13\4\5\14\5\7zzzzz" +
                "zzzzzzzzzzzzz\7\7z" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzz\7" +
                "\10\7\7\10\10\10\10\1\1\1zzzzz\16" +
                "\16\16\16\16\7z\10\10\10\10\10\10\10\10zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzz\7" +
                "\10\7\7\10\10\10\10\1\1\1\10\1zzz\16" +
                "\16\16\16\16zzz\10\10\10\10\10\10zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzz\1" +
                "\1zzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\1z\1z" +
                "\10zzzz\7\4zzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\1\10\11\1\1\11\11\11" +
                "\11\10\10\10\10\10\7\10\11\10\10\1z\10\10z" +
                "zzzz\1\1\1\1\1\1\1\1\1\1\1z" +
                "\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1" +
                "\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1\1" +
                "\1\1\1\1zzzzzz\1zzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzz\7\7\10\10\1\4\10\10\10\10\10\1" +
                "\7z\10\7z\1\1zzzzzz\7\7\1" +
                "\1zzzz\1\1z\7\7\7zz\7\7\7" +
                "\7\7\7\7zz\10\10\10\10zzzzzz" +
                "zzzzz\1\7\4\10\10\7\7\7\7\7\7" +
                "\1z\7zzzzzzzzzz\7\7\7" +
                "\10zz\10\1\1zzzzzzzzzz" +
                "z\10\1zzzzzzzzzzzz\7" +
                "\10\10\10\10\1\1\1\13\14\5\4\4\4\5\5\10" +
                "\7\7\10\10\10\10\10\10\10z\10zzzzz" +
                "zzzz\10zz\10\10\1\7\7\15\15\10\10" +
                "\7\7\7zzzz\7\7\1\7\7\7\7\7\7" +
                "\1\10\1zzzz\7\7\7\7\7\16\16\16\7" +
                "\7\16\7\7\7\7\7zzzzzzz\7\7" +
                "zzzzzzz\10\1\4\7\10zzzz" +
                "z\4\1\7\10\10\10\1\1\1\1z\7\10\7\7" +
                "\10\10\10\10\1\1\10\1\7\4\4\4\10\10\10\10" +
                "\10\10\10\10\10\10zz\1\10\10\10\10\7zz" +
                "zzzzzzzzz\10\7\10\10\1\1\1" +
                "\3\11\12\4\4\5\5\10\15\7zzzzzz" +
                "zzzzz\10\1\10\10\10z\7\1\1\10\1" +
                "\4\7\10\10\7z\1\1zzzzzz\10\7" +
                "\10\10\7\7\7\10\7\10zzzz\7\7\7\4" +
                "\4\13\7\7\1\10\10\10\10\4\4\10\1zzz" +
                "zzzzz\10\10\10z\6\1\1\1\1\1\10" +
                "\10\1\1\1\1\10\7\6\6\6\6\6\6\6zz" +
                "zz\1zzzz\10zz\7zzzzz" +
                "zzz\10zzz\10zzzz\10zzz" +
                "z\7\7\1\10\7zzzzzzzz\7\7" +
                "\7\7\7\7\7\7\7\7\7\7\1\10zzzz" +
                "zzzzzz\10\10\10\10\10\10\10\10\10\10" +
                "\10\10\10\10\10\10zzzzzzzzzz" +
                "zzz\10zzzzzzzzzzz\1" +
                "\1\1zzzzzzz\1\1\1\10\1\1\1" +
                "\1\10zzz\10\7\7\10\10\1\1\4\4\10\7" +
                "\7\2\3zzzzzzzzzzzzz" +
                "zz\10\10\10\10\1\10\4\10\1\7\4\1\1z" +
                "zzzzzzzz\10zzzzzzz" +
                "z\10\7zzzzzzzzzzz\7\10" +
                "\7zz\10\7\10\10\1\16\16\10\10\16\7\16\16" +
                "\7\10\10zzzzzzzzzzz\4\1" +
                "\10\4\7zzz\7\7\10\7\7\1\7\7z\7" +
                "\1zz\6\1\1z\10\6zzzzz\1\1" +
                "\1\10zzzzzzzz\10\1\1zzz" +
                "zz\7\10\7zzzzzzzzzzz" +
                "zz\10\10\10\10\1\1\1\1\10\10\10\10\10z" +
                "zzzzzzzz\7\4\7\1\1\10\10\7" +
                "\7\1\1zzzzzzz\10\10\10\1\1\4" +
                "\10\11\11\10\1\1z\10zzzzzzzz" +
                "zzz\7\4\7\1\1\1\1\1\1\10\10\10\15" +
                "\7zzzzzzzz\1z\10\1zzz" +
                "zzzzzzzzz\7\7\7\1\10\10\15" +
                "\15\10\7\10\10zzzzzz\10z\7\4\7" +
                "\1\1\10\10\10\10\1\1zzzzzzzz" +
                "zzz\1\1z\7\7\10\7\7\7\7zz\4" +
                "\4zz\5\5\7zz\7\7zz\10\10\10\10" +
                "\10\10\10zzz\7\7\1\10\10\7\1zzz" +
                "zzzzzz\7\4\7\1\1\1\1\1\1\4" +
                "\10\13\5\7\5\10\7\1\1zzzzzzz" +
                "zzzzz\4\7\1\1\1\1zz\4\13\5" +
                "\14\10\10\7\1\7\7\7\1\1\1\1\1\1\10\10" +
                "\7\7\10\7\1zzzzzzzzzzz" +
                "\10\7\10\4\7\1\1\10\10\10\10\7\1zzz" +
                "zzzzzzzzzz\1z\10\7\7\10" +
                "\10\1\1\4\10\1\10\10\10zzzzzzz" +
                "zzzzz\7\4\7\1\1\1\10\10\10\10\10" +
                "\7\1\1zzzzz\7\10\7\1\1\1\1z" +
                "z\10\10\7\7\7\7\1zzz\4zzzz" +
                "zzzzzzz\10\1\1\10\10\10\10\10\10" +
                "\1zzzzz\1\1\10\10\10\10\7z\1\1" +
                "\1\1z\10\1\1\10\10\10\7\7\1\1\1zz" +
                "zzzzzzzz\1\1\1\1\1\1\10\7" +
                "\10zzzzzzz\10\10\1\1\1\1\1z" +
                "\10\10\10\10\10\10\7\1zz\1\1\1\1\1\1" +
                "\1\1\1\1\1\1\1\1z\7\1\1\1\1\1\1" +
                "\4\1\10\7\10\10zzzzzzzzz\10" +
                "\10\10\10\10\1zzz\10z\10\10z\10\10\1" +
                "\10\1zz\1zzzzzzzzzz\7" +
                "\7\7\7\7z\10\10z\7\7\10\7zzzz" +
                "zzzzz\10\1\4\7zzzzzzz" +
                "zzzz");
    }

    private static final class InSCTrie {
        static final CodePointTrie INSTANCE = makeTrie(
                "\63\151\162\124\102z\111\3\272\17\4z\100z\220z" +
                "zz\100z\140z\224z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\324z\22\1\122\1\220\1" +
                "\317\1\15\2\114\2\212\2\312\2\10\3\106\3\204\3" +
                "\304\3\2\4\102\4\200\4\300\4\376\4\76\5\176\5" +
                "\275\5\375\5\74\6\174\6\234\6\334\6\34\7\131\7" +
                "\377\2\22\3\36\3\22\3\71\3zz\20z\40z" +
                "\60z\100z\120z\140z\160z\140z\160z\200z" +
                "\220z\224z\244z\264z\304z\100z\120z\140z" +
                "\160z\100z\120z\140z\160z\100z\120z\140z" +
                "\160z\100z\120z\140z\160z\100z\120z\140z" +
                "\160z\100z\120z\140z\160z\100z\120z\140z" +
                "\160z\100z\120z\140z\160z\324z\344z\364z" +
                "\4\1\22\1\42\1\62\1\102\1\122\1\142\1\162\1" +
                "\202\1\220\1\240\1\260\1\300\1\317\1\337\1\357\1" +
                "\377\1\15\2\35\2\55\2\75\2\114\2\134\2\154\2" +
                "\174\2\212\2\232\2\252\2\272\2\312\2\332\2\352\2" +
                "\372\2\10\3\30\3\50\3\70\3\106\3\126\3\146\3" +
                "\166\3\204\3\224\3\244\3\264\3\304\3\324\3\344\3" +
                "\364\3\2\4\22\4\42\4\62\4\102\4\122\4\142\4" +
                "\162\4\200\4\220\4\240\4\260\4\300\4\320\4\340\4" +
                "\360\4\376\4\16\5\36\5\56\5\76\5\116\5\136\5" +
                "\156\5\176\5\216\5\236\5\256\5\275\5\315\5\335\5" +
                "\355\5\375\5\15\6\35\6\55\6\74\6\114\6\134\6" +
                "\154\6\174\6\214\6\234\6\254\6\234\6\254\6\274\6" +
                "\314\6\334\6\354\6\374\6\14\7\34\7\54\7\74\7" +
                "\114\7\131\7\151\7\171\7\211\7\351z\351z\231\7" +
                "\244\7\264\7\304\7\323\7\342\7\360\7z\10\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\20\10\36\10\346z" +
                "\36\10\346z\56\10\20\10\76\10\351z\351z\116\10" +
                "\132\10\144\10\163\10\60z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\203\10\154\1\223\10\243\10" +
                "\55\2\351z\263\10\303\10\351z\351z\164\3\323\10" +
                "\342\10\60z\100z\100z\351z\362\10\351z\351z" +
                "\2\11\17\11\37\11\53\11\60z\60z\100z\100z" +
                "\100z\100z\100z\100z\73\11\346z\351z\113\11" +
                "\127\11\60z\100z\100z\147\11\351z\166\11\206\11" +
                "\351z\351z\226\11\246\11\351z\351z\266\11\303\11" +
                "\323\11\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\343\11\361\11\377\11\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\12\12\26\12\46\12\100z\100z" +
                "\100z\100z\100z\133\7\64\12\100z\100z\100z" +
                "\100z\100z\100z\104\12\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\164z\100z\100z\100z\124\12\351z\141\12\100z" +
                "\351z\161\12\177\12\216\12\326z\347z\351z\236\12" +
                "\252\12\60z\272\12\310\12\330\12\351z\346\12\351z" +
                "\366\12\5\13\100z\100z\25\13\351z\351z\44\13" +
                "\227\2\60z\64\13\104\13\343z\351z\212\10\124\13" +
                "\144\13\60z\351z\163\13\351z\351z\351z\203\13" +
                "\223\13\100z\243\13\263\13\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\303\13\323\13\340\13\60z\360\13z\14\351z\12\14" +
                "\61z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\32\14\346z\351z\213\10" +
                "\52\14\70\14\102\14\122\14\142\14\351z\351z\162\14" +
                "\100z\100z\100z\100z\202\14\351z\214\10\222\14" +
                "\242\14\262\14\351z\277\14\325z\350z\351z\317\14" +
                "\337\14\60z\273\6\65z\341z\353\3\207\10\357\14" +
                "\100z\100z\100z\100z\377\14\155\1\16\15\337z" +
                "\351z\36\15\56\15\60z\76\15\142\1\162\1\116\15" +
                "\10\3\136\15\156\15\356\11\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\333z\351z\351z\176\15" +
                "\214\15\234\15\100z\100z\254\15\351z\351z\40\11" +
                "\274\15\60z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\333z\351z\377z\314\15" +
                "\334\15\344\15\100z\100z\333z\351z\351z\364\15" +
                "\4\16\60z\100z\100z\337z\351z\24\16\41\16" +
                "\60z\100z\100z\100z\351z\61\16\101\16\121\16" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\337z\351z\207\10\141\16" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\161\16\351z" +
                "\351z\200\16\220\16\100z\240\16\351z\351z\255\16" +
                "\275\16\315\16\351z\351z\331\16\343\16\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\363\16\351z\377z\3\17" +
                "\23\17\274\6\43\17\126\5\351z\61\17\54\7\101\17" +
                "\100z\100z\100z\100z\121\17\351z\351z\140\17" +
                "\160\17\60z\200\17\351z\214\17\231\17\60z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\351z\251\17\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\100z" +
                "\100z\100z\100z\100z\100z\100z\100z\105z" +
                "\125z\125z\125z\145z\205z\245z\305z\345z" +
                "\4z\4z\365z\24\1\64\1\124\1\4z\164\1" +
                "\4z\204\1\4z\4z\4z\4z\4z\4z" +
                "\4z\4z\4z\4z\4z\4z\4z\4z" +
                "\4z\4z\4z\4z\4z\4z\4z\4z" +
                "\4z\4z\4z\4z\4z\4z\4z\4z" +
                "\4z\4z\244\1\304\1\4z\4z\4z\4z" +
                "\4z\4z\4z\4z\4z\4z\344\1\4z" +
                "\4z\4\2\44\2\104\2\144\2\204\2\244\2\304\2" +
                "\337\2zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzz\14" +
                "zz\30\30\30\30\30\30\30\30\30\30zzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zz\14zzzzzzzzzzzzz" +
                "zzzz\34\34zzzzzzzzzz" +
                "zzzzzzzzzzzzz\14zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzz\2\2\2\40\43\43\43\43\43\43" +
                "\43\43\43\43\43\43\43\43\43\43\43\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\42\42\27\1\42\42\42\42\42\42\42\42\42\42\42\42" +
                "\42\37\42\42z\4\4zz\42\42\42\5\5\5\5" +
                "\5\5\5\5\43\43\42\42zz\30\30\30\30\30\30" +
                "\30\30\30\30zz\43\43\43\43\43\43\5\5\5\5" +
                "\5\5\5\5\14\2\2\40z\43\43\43\43\43\43\43" +
                "\43zz\43\43zz\43\43\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5z\5\5" +
                "\5\5\5\5\5z\5zzz\5\5\5\5zz" +
                "\27\1\42\42\42\42\42zz\42\42zz\42\42\37" +
                "\6zzzzzzzz\42zzzz\5\5" +
                "z\5\43\43\42\42zz\30\30\30\30\30\30\30\30" +
                "\30\30\5\5zzzzzzzzzz\2z" +
                "\34z\2\2\40z\43\43\43\43\43\43zzzz" +
                "\43\43zz\43\43\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5z\5\5\5\5\5" +
                "\5\5z\5\5z\5\5z\5\5zz\27z\42" +
                "\42\42zzzz\42\42zz\42\42\37zzz" +
                "\4zzzzzzz\5\5\5\5z\5zz" +
                "zzzzz\30\30\30\30\30\30\30\30\30\30\2" +
                "\22\14\14z\13zzzzzzzzzz\2" +
                "\2\40z\43\43\43\43\43\43\43\43\43z\43\43\43" +
                "z\43\43\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5z\5\5\5\5\5\5\5z" +
                "\5\5z\5\5\5\5\5zz\27\1\42\42\42\42" +
                "\42\42z\42\42\42z\42\42\37zzzzzz" +
                "zzzzzzzzzzzz\43\43\42\42" +
                "zz\30\30\30\30\30\30\30\30\30\30zzzz" +
                "zzzzz\5\4\4\4\27\27\27z\2\2\40" +
                "z\43\43\43\43\43\43\43\43zz\43\43zz\43" +
                "\43\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5z\5\5\5\5\5\5\5z\5\5" +
                "z\5\5\5\5\5zz\27\1\42\42\42\42\42z" +
                "z\42\42zz\42\42\37zzzzzzzz" +
                "\42\42zzzz\5\5z\5\43\43\42\42zz" +
                "\30\30\30\30\30\30\30\30\30\30z\5zzzz" +
                "zzzzzzzzzz\2\25z\43\43\43" +
                "\43\43\43zzz\43\43\43z\43\43\43\5zz" +
                "z\5\5z\5z\5\5zzz\5\5zzz" +
                "\5\5\5zzz\5\5\5\5\5\5\5\5\5\5" +
                "\5\5zzzz\42\42\42zzz\42\42\42z" +
                "\42\42\42\37zzzzzzzzz\42zz" +
                "zzzzzzzzzzzz\30\30\30\30" +
                "\30\30\30\30\30\30zzzzzzzzzz" +
                "zzzzzz\2\2\2\40\2\43\43\43\43\43" +
                "\43\43\43z\43\43\43z\43\43\43\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5z" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "zzz\1\42\42\42\42\42z\42\42\42z\42\42" +
                "\42\37zzzzzzz\42\42z\5\5\5z" +
                "zzzz\43\43\42\42zz\30\30\30\30\30\30" +
                "\30\30\30\30zzzzzzzzzzzz" +
                "zzzz\2\2\2\40z\43\43\43\43\43\43\43" +
                "\43z\43\43\43z\43\43\43\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5z\5\5" +
                "\5\5\5\5\5\5\5\5z\5\5\5\5\5zz" +
                "\27\1\42\42\42\42\42z\42\42\42z\42\42\42\37" +
                "zzzzzzz\42\42zzzzzzz" +
                "\5z\43\43\42\42zz\30\30\30\30\30\30\30\30" +
                "\30\30z\21\21zzzzzzzzzzz" +
                "zz\2\2\2\40z\43\43\43\43\43\43\43\43z" +
                "\43\43\43z\43\43\43\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\32\32\1" +
                "\42\42\42\42\42z\42\42\42z\42\42\42\37\15z" +
                "zzzz\6\6\6\42zzzzzzz\43" +
                "\43\43\42\42zz\30\30\30\30\30\30\30\30\30\30" +
                "zzzzzzzzzz\6\6\6\6\6\6" +
                "zz\2\40z\43\43\43\43\43\43\43\43\43\43\43" +
                "\43\43\43\43\43\43\43zzz\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5z\5\5\5\5\5\5\5\5\5z\5zz" +
                "\5\5\5\5\5\5\5zzz\37zzzz\42" +
                "\42\42\42\42\42z\42z\42\42\42\42\42\42\42\42" +
                "zzzzzz\30\30\30\30\30\30\30\30\30\30" +
                "zz\42\42zzzzzzzzzzzz" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5z\42" +
                "\42\42\42\42\42\42\42\42\42\32zzzzz\42" +
                "\42\42\42\42\42z\42\36\36\36\36\12\2\32z\30" +
                "\30\30\30\30\30\30\30\30\30zzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzz\5" +
                "\5z\5z\5\5\5\5\5z\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5z\5z\5\5\5\5\5\5\5\5z\42\42" +
                "\42\42\42\42\42\42\42\42\32\42\13\13zz\42\42" +
                "\42\42\42zzz\36\36\36\36z\2zz\30\30" +
                "\30\30\30\30\30\30\30\30zz\5\5\5\5zz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzz\30\30" +
                "\30\30\30\30\30\30\30\30\30\30\30\30\30\30\30\30" +
                "\30\30z\34z\34z\27zzzzzz\5\5" +
                "\5\5\5\5\5\5z\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\5\5\5\5\5\5zzzz\42" +
                "\42\42\42\42\42\42\42\42\42\42\42\42\2\40\42\42" +
                "\2\2\32\1zz\10\10\10\10\10\17\17\17\17\17" +
                "\17\17\17\17\17\17z\17\17\17\17\17\17\17\17\17" +
                "\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17" +
                "\17\17\17\17\17\17\17\17\17\17\17zzzzz" +
                "z\34zzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzz\5\43\43\43\43" +
                "\43\43\43\43\43\43\42\42\42\42\42\42\2\36\40\23" +
                "\32\13\13\13\13\5\30\30\30\30\30\30\30\30\30\30" +
                "z\14zz\14z\5\5\43\43\43\43\42\42\42\42" +
                "\5\5\5\5\13\13\5\42\36\36\5\5\42\42\36\36" +
                "\36\36\36\5\5\42\42\42\42\5\5\5\5\5\5\5" +
                "\5\5\5\5\13\42\42\42\42\36\36\36\36\36\36\36" +
                "\5\36\30\30\30\30\30\30\30\30\30\30\36\36\42\42" +
                "zz\43\43\43\5\5\5\5\5\5\5\5\5\5z" +
                "\5\5\42\42\32zzzzzzzzzzz" +
                "\5\5\42\42zzzzzzzzzzzz" +
                "\5z\42\42zzzzzzzzzzzz" +
                "\5\5\5\43\43\43\43\43\43\43\43\43\43\43\43\43" +
                "zz\42\42\42\42\42\42\42\42\42\42\2\40\42\33" +
                "\33\34\20\12\34\34\32\23\34zzzzzzz" +
                "z\1\34zz\14\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\5\5\42\42\42\42\42\42\42\42\42\17\17" +
                "\17zzzz\7\7\2\7\7\7\7\7\7\7\42" +
                "\34zzzz\5\5\5\41\41\41\41\41\41\41\41" +
                "\41\41\41zz\35\35\35\35\35zzzzzz" +
                "zzzzz\42\42\42\42\42\42\42\42\42\42\42" +
                "\42\42\42\42\42\7\7\7\7\7\7\7\36\36zz" +
                "zzzz\5\5\5\5\5\5\5\42\42\42\42\42" +
                "zzzz\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\43\43\43\5\5\13\13\17\7\7\11\17\17\17\17" +
                "z\23\42\42\42\42\42\42\42\42\42\42\42\42\42\42" +
                "\42\2\36\36\36\36\36\32\34\34zz\34\2\2\2" +
                "\20\40\43\43\43\43\43\43\43\43\43\43\43\5\5\5" +
                "\5\27\42\42\42\42\42\42\42\42\42\42\42\37\5\5" +
                "\5\5\5\5\5zzzz\2\20\40\43\43\43\43" +
                "\43\43\43\5\5\5\5\5\5\17\17\17\42\42\42\42" +
                "\42\42\32\23\17\17\5\5\30\30\30\30\30\30\30\30" +
                "\30\30\1\5\5\5\7\7\5\5\5\5\43\43\27\42" +
                "\42\42\42\42\42\42\42\42\7\7\32\32zzzz" +
                "zzzzzzzz\5\5\5\5\17\17\42\42" +
                "\42\42\42\42\42\7\7\7\7\2\2\34\27zzz" +
                "zzzzz\30\30\30\30\30\30\30\30\30\30z" +
                "zz\5\5\5\4\4\4z\4\4\4\4\4\4\4" +
                "\4\4\4\4\4zzzzzzzzzzz" +
                "zzz\6\6\4\21\21\4\4\4\14zzzz" +
                "zzzzzzz\34zzzzzzzz" +
                "zzzz\26\24zz\14\14\14\14\14zzz" +
                "zzzzzzzz\34\34\34zzzzz" +
                "zzzzzz\4zzzzzzzzz" +
                "zzzzzz\43\43\42\43\43\43\32\5\5\5" +
                "\5\2\5\5\5\5\42\42\42\42\42zzzzz" +
                "zzz\5\5\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\41\41\5\5\5\5\41\17\17\5\5\5\5\5\5" +
                "\5\17\5\2zzzzzzzzzzzz" +
                "\5\5\5\5\13\42\42\42\42\42\42\42\42\42\42\42" +
                "\37\2zzzzzzzzzz\4\4\4\4" +
                "\4\4\4\4\4\4\4\4\4\4\4\4\2\2zz" +
                "zzzzzzzz\43\42\30\30\30\30\30\30" +
                "\30\30\30\30\5\5\5\5\5\5\41\41\41\41\41\41" +
                "\41\41\41\36\36\36zz\5\5\5\5\5\5\5\42" +
                "\42\42\42\42\42\42\42\7\7\7\32zzzzz" +
                "zzzzzzz\2\2\20\40\43\43\43\43\43" +
                "\5\5\5\43\43\43\5\5\5\27\42\42\42\42\42\42" +
                "\42\42\42\17\13\13\5\5\5\5\5\42z\5\5\5" +
                "\5\5\5\5\5\5\30\30\30\30\30\30\30\30\30\30" +
                "\5\5\5\5\5z\42\42\42\13\13\13\13zzz" +
                "zzzzzz\7\7\7\7\7\7\7\7\7\7" +
                "\7\7\7\7zz\5\5\5\14\14\14zzz\5" +
                "\36\36\36\5\5\42\42\42\42\42\42\42\42\42\42\42" +
                "\42\42\42\42\36\35\36\35zzzzzzzz" +
                "zzzzz\43\43\5\5\5\5\5\5\5\5\5" +
                "\42\42\42\42\42zzzzz\40\23zzzz" +
                "zzzzz\5\5\5\5\5\5\5\5\5\5\5" +
                "\5\5\5\43\43\5\43\5\5\5\5\5\5\5\5\5" +
                "\7\7\7\7\7\42\42\42\42\42\42\42\42z\36\32" +
                "zz\5\42\42\42z\42\42zzzzz\42\42" +
                "\2\40\5\5\5\5z\5\5\5z\5\5\5\5\5" +
                "\5\5zz\27\27\27zzzz\23\2\2\40\21" +
                "\21\43\43\43\43\43\43\43\43\43\43\43\42\42\42\42" +
                "\42\42\37zzzzzzzzz\3\3\3\3" +
                "\3\3\3\3\3\3\3\3\3\3\30\30\30\30\30\30" +
                "\30\30\30\30zzzzzzzzzzzz" +
                "zzz\31\2\2\40\43\43\43\43\43\43\43\43\43" +
                "\43\5\5\5\42\42\42\42\42\42\42\42\42\37\27z" +
                "zzzz\2\2\40\43\43\43\43\5\5\5\5\5" +
                "\5\5\5\5\42\42\42\23\32z\30\30\30\30\30\30" +
                "\30\30\30\30zzzz\5\42\42zzzzz" +
                "zzzz\41\41\41\41\41\5\5\5\5\5\5\5" +
                "\5\5\5\5\27zzzzzzzzzzz" +
                "z\5\5\5\42\42\42\42\42\42\42\42\42\42\42\42" +
                "\42\37\1\16\16zzzzz\34\27\42\42zz" +
                "z\42\42\42\42\2\37\27\22zzzzzz\4" +
                "z\43\43\43\43\5\5\5z\5z\5\5\5\5z" +
                "\5\5\5\5\5\5\5\5\5zzzzzzz" +
                "\5\5\5\5\5\5\5\5\5\5\5\5\5\5\5\2" +
                "\42\42\42\42\42\42\42\42\42\27\32zzzzz" +
                "\2\2\2\40z\43\43\43\43\43\43\43\43zz\43" +
                "\5z\5\5z\5\5\5\5\5z\27\27\1\42\42" +
                "zzzzzzz\42zzzzzz\2\2" +
                "\43\43\42\42zz\4\4\4\4\4\4\4zzz" +
                "\5\5\5\5\5\42\42\42\42\42\42\42\42\42\42\42" +
                "\37\2\2\40\27\1zzzzzzzz\30\30" +
                "\30\30\30\30\30\30\30\30zzzz\34\2z\43" +
                "\43\43\43\43\43\43\43\43\43\43\43\43\43\5\2\40" +
                "\37\27\1zzzzzzzzzzz\42\42" +
                "\42\42\42\42zz\42\42\42\42\2\2\40\37\27z" +
                "zzzzzzzzzzzzzz\43\43" +
                "\43\43\42\42zz\42\42\42\42\42\42\42\42\42\42" +
                "\42\42\42\2\40\37\42zzzzzzzzz" +
                "zzzzzz\5\5\5\5\5\5\5\5\5\5" +
                "\5\2\40\42\42\42\42\42\42\37\27\5zzzz" +
                "zzz\5\5\5\5\5\5\5\5\5\5\5zz" +
                "\13\13\13\42\42\42\42\42\42\42\42\42\42\42\32z" +
                "zzz\30\30\30\30\30\30\30\30\30\30\30\30z" +
                "zzz\42\42\42\42\42\42\42\2\40\37\27zz" +
                "zzz\43\43\43\43\43\43\43\43zz\43\43\43" +
                "\43\5\5\42\42\42\42\42\42\42zz\42\42\42\42" +
                "\2\40\37\1zz\42zzzzzzzzz" +
                "zz\43\42\42\42\42\42\42\42\42\42\42\5\5\5" +
                "\5\5\34\32\2\2\2\2\40\16\13\13\13\13\14z" +
                "zzzz\14z\23zzzzzzzz\43" +
                "\42\42\42\42\42\42\42\42\42\42\42\5\5\5\5\16" +
                "\16\16\16\16\16\7\7\7\7\7\7\2\40\22\23z" +
                "zz\1zz\43\43\43\43\43\43\43\43\43z\43" +
                "\43\43\43\5\5\42\42\42\42\42\42\42z\42\42\42" +
                "\42\2\2\40\37\1zzzzzzzzzz" +
                "zzzzz\30\30\30\30\30\30\30\30\30\30\30" +
                "\30\30zzz\17\17\17\17\17\17\17\17\17\17\17" +
                "\17\17\17\42\42\42\42\42\2\2zzzzzz" +
                "zzz\43\43\43\43\43\43\43z\43\43z\43\5" +
                "\5\5\5\42\42\42\42\42\42zzz\42z\42\42" +
                "z\42\2\40\27\42\32\23\15\13zzzzzz" +
                "zz\43\43\43\43\43\43z\43\43z\43\43\5\5" +
                "\5\5\5\5\5\5\5\5\42\42\42\42\42z\42\42" +
                "\2\40\23zzzzzzzz\5\5\14\42\42" +
                "\42\42zzzzzzzzzz");
    }

    private static final class VoTrie {
        static final CodePointTrie INSTANCE = makeTrie(
                "\63\151\162\124\102z\36\4\74\3\14zzz\200\10" +
                "zz\100z\131z\230zzzzzzzzz" +
                "zzzzzz\320zzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\15\3\47\3\65\3\113\3\153\3\211\3\244\3\276\3" +
                "\47\3\47\3\47\3\336\3\47\3\47\3\47\3\336\3" +
                "\376\3\376\3\376\3\376\3\376\3\376\3\376\3\376\3" +
                "\376\3\376\3\376\3\376\3\376\3\376\3\376\3\376\3" +
                "\376\3\376\3\376\3\376\3\376\3\376\3\376\3\376\3" +
                "\376\3\376\3\376\3\376\3\376\3\376\3\376\3\376\3" +
                "\376\3\376\3\376\3\376\3\376\3\376\3\376\3\376\3" +
                "\376\3\376\3\376\3\376\3\47\3\47\3\47\3\336\3" +
                "\47\3\47\3\47\3\336\3zz\20z\40z\60z" +
                "\100z\120z\140z\160z\131z\151z\171z\211z" +
                "\230z\250z\270z\310zzz\20z\40z\60z" +
                "zz\20z\40z\60zzz\20z\40z\60z" +
                "zz\20z\40z\60z\320z\340z\360zz\1" +
                "zz\20z\40z\60zzz\20z\40z\60z" +
                "zz\20z\40z\60zzz\20z\40z\60z" +
                "zz\20z\40z\60zzz\20z\40z\60z" +
                "zz\20z\40z\60zzz\20z\40z\60z" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\17\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "\20\1\20\1\20\1\20\1\20\1zzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzz\251z\226z\36\1" +
                "\54\1\256z\252zzzzzzzzzzz" +
                "zz\3\1\74\1zz\114\1\130\1\146\1\13\1" +
                "\165\1\20\1\20\1\20\1\204\1zzzzzz" +
                "zzzzzzzz\162zzz\366zzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzz\220\1\20\1\230\1zzzz" +
                "zzzz\3\1\20\1\25\1zz\354z\250\1" +
                "\266\1\16\1\20\1\20\1\306\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1zzzzzzzzzzzz" +
                "zzzzzzzz\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\26\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\30\1" +
                "\12\1\20\1\322\1zzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\16\1\20\1zzzz" +
                "\26\1zzzzzzzzzz\10\1\20\1" +
                "\342\1\24\1\20\1zzzzzzzzzz" +
                "zzzzzz\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\361\1\377\1\20\1\16\2\35\2" +
                "\20\1\52\2\20\1\67\2\106\2\126\2\20\1\52\2" +
                "\20\1\67\2\141\2\20\1\20\1\156\2\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\176\2\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\176\2\176\2\176\2\176\2\176\2" +
                "\206\2\20\1\216\2\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1zzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zz\20\1\20\1zzzzzzzzzz" +
                "zzzzzz\20\1zz\20\1\27\1\233\2" +
                "\252\2zzzzzzzzzzzzzz" +
                "zzzz\272\2\311\2\20\1\331\2\20\1\351\2" +
                "\370\2zzzzzzzzzzzzzz" +
                "\10\3\30\3zzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzz\20\1\20\1zzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzz\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1zzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzz\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1zzzzzzzz" +
                "zzzzzzzz\50\3\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\20\1\20\1\20\1\20\1" +
                "\20\1\20\1\20\1\20\1\22\1\204z\230z\250z" +
                "\250z\250z\250z\250z\250z\310z\14z\350z" +
                "z\1\25\1\14z\14z\14z\64\1\123\1\162\1" +
                "\221\1\14z\253\1\14z\313\1\353\1\13\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\373z" +
                "\14z\103\2\14z\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\14z" +
                "\14z\14z\14z\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\370z\14z\142\2\14z\14z\14z\14z\202\2" +
                "\14z\14z\14z\14z\14z\234\2\14z\14z" +
                "\375z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\43\2\43\2\270\2\14z\14z" +
                "\14z\14z\14z\43\2z\1\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\274\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\370z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\43\2\370z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\43\2\314\2" +
                "\14z\14z\43\2\375z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\43\2\354\2" +
                "\43\2\43\2\310z\376z\14z\14z\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\43\2\43\2\43\2" +
                "\43\2\43\2\43\2\43\2\43\2\355\2\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14z\14z\14z" +
                "\14z\14z\14z\14z\14z\14zzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzz\3z\3z" +
                "zzz\3zz\3zzzzzzzzz" +
                "z\3\3\3zzzzzzzzzzzz" +
                "zzzzzzzzzzz\3zzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzz\3zzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzzzzzzzzzzzz" +
                "zzzzzz\3\3zzzzzzzz" +
                "zzzzzzzzzzzz\3\3\3\3" +
                "\3\3\3\3\3\3\3\3\3\3\3\3zzzz" +
                "zzzzz\3\3zzz\3zzzz\3" +
                "\3\3zzzzzz\3z\3\3\3zzz" +
                "zzzzzzzz\3\3z\3\3\3\3\3" +
                "\3\3zzzzz\3\3z\3\3zzzz" +
                "zz\3\3\3\3z\3z\3z\3zzzz" +
                "\3zzzzz\3\3\3\3\3\3z\3\3z" +
                "\3\3\3\3\3\3\3\3\3\3zz\3\3\3\3" +
                "\3\3\3\3zzzz\3\3\3\3\3\1\1\3" +
                "zzzz\3\3\3\3\3\3\3\3\3\3\3\3" +
                "\3\3z\3\3\3\3\3\3\3\3\3\3\3zz" +
                "zz\3\3\3z\3\3\3\3\3\3\3\3\3\3" +
                "\3\3zzzzzzzzzzzz\3\3" +
                "z\3\3\3\3\3\3\3\3\3\3\3\3\3\2\2" +
                "\3\3\3\3\3\1\1\1\1\1\1\1\1\3\3\1" +
                "\1\1\1\1\1\1\1\1\1\1\1\3\3\3\3\3" +
                "\3\3\3\3\3\3\3\3\3\3\2\3\2\3\2\3" +
                "\2\3\2\3\3\3\3\3\3\2\3\3\3\3\3\3" +
                "\3\3\3\3\3\3\2\3\2\3\2\3\3\3\3\3" +
                "\3\2\3\3\3\3\3\2\2\3\3\3\3\2\2\3" +
                "\3\3\1\2\3\2\3\2\3\2\3\2\3\3\3\3" +
                "\3\3\2\2\3\3\3\3\3\1\3\3\3\3\3\3" +
                "\3\2\3\3\3\3\3\3\3\3\2\2\2\2\2\2" +
                "\2\2\2\2\2\2\2\2\2\2\3\3\3\3\3\3" +
                "\3\3\3\3\3\2\2\2\2\2\3\3\3\3\3z" +
                "\1\1\1\1\1\1\3\3\3zzzz\3\3\3" +
                "\3\3\3\3\3\3z\2\3\3\3\3\3\3\1\1" +
                "\3\3\2z\2\3\3\3\3\3\3\3\3\3\3\1" +
                "\1zzz\2\3\3\3\3\3\3\3\3\3\3\3" +
                "\1\3\1\3\1\3\3\3\3\3\3\3\3\3\3\3" +
                "\1\1\1\1\1zzzzzzzzzzz" +
                "zzzz\3\3\3\1\3\3\3\3zzzz" +
                "zzzz\3\3\3\3\3\3\3\3\3zzz" +
                "\3\3zz\2\2\3\3\3\3\3\3\3\3\3\3" +
                "\3\3\3\3zzzz");
    }

    // public methods ----------------------------------------------------

    /**
    * Gets the main property value for code point ch.
    * @param ch code point whose property value is to be retrieved
    * @return property value of code point
    */
    public final int getProperty(int ch)
    {
        return m_trie_.get(ch);
    }

    /**
     * Gets the unicode additional properties.
     * Java version of C u_getUnicodeProperties().
     * @param codepoint codepoint whose additional properties is to be
     *                  retrieved
     * @param column The column index.
     * @return unicode properties
     */
    public int getAdditional(int codepoint, int column) {
        assert column >= 0;
        if (column >= m_additionalColumnsCount_) {
            return 0;
        }
        return m_additionalVectors_[m_additionalTrie_.get(codepoint) + column];
    }

    static final int MY_MASK = UCharacterProperty.TYPE_MASK
        & ((1<<UCharacterCategory.UPPERCASE_LETTER) |
            (1<<UCharacterCategory.LOWERCASE_LETTER) |
            (1<<UCharacterCategory.TITLECASE_LETTER) |
            (1<<UCharacterCategory.MODIFIER_LETTER) |
            (1<<UCharacterCategory.OTHER_LETTER));


       /**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a
     * character.</p>
     * <p>This can be useful to avoid emitting code points to receiving
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * @param codepoint The code point.
     * @return the Unicode version number
     */
    public VersionInfo getAge(int codepoint)
    {
        int version = getAdditional(codepoint, 0) >> AGE_SHIFT_;
        return VersionInfo.getInstance(
                           (version >> FIRST_NIBBLE_SHIFT_) & LAST_NIBBLE_MASK_,
                           version & LAST_NIBBLE_MASK_, 0, 0);
    }

    private static final int GC_CN_MASK = getMask(UCharacter.UNASSIGNED);
    private static final int GC_CC_MASK = getMask(UCharacter.CONTROL);
    private static final int GC_CS_MASK = getMask(UCharacter.SURROGATE);
    private static final int GC_ZS_MASK = getMask(UCharacter.SPACE_SEPARATOR);
    private static final int GC_ZL_MASK = getMask(UCharacter.LINE_SEPARATOR);
    private static final int GC_ZP_MASK = getMask(UCharacter.PARAGRAPH_SEPARATOR);
    /** Mask constant for multiple UCharCategory bits (Z Separators). */
    private static final int GC_Z_MASK = GC_ZS_MASK|GC_ZL_MASK|GC_ZP_MASK;

    /**
     * Checks if c is in
     * [^\p{space}\p{gc=Control}\p{gc=Surrogate}\p{gc=Unassigned}]
     * with space=\p{Whitespace} and Control=Cc.
     * Implements UCHAR_POSIX_GRAPH.
     * @internal
     */
    private static final boolean isgraphPOSIX(int c) {
        /* \p{space}\p{gc=Control} == \p{gc=Z}\p{Control} */
        /* comparing ==0 returns FALSE for the categories mentioned */
        return (getMask(UCharacter.getType(c))&
                (GC_CC_MASK|GC_CS_MASK|GC_CN_MASK|GC_Z_MASK))
               ==0;
    }

    // binary properties --------------------------------------------------- ***

    private class BinaryProperty {
        int column;  // SRC_PROPSVEC column, or "source" if mask==0
        int mask;
        BinaryProperty(int column, int mask) {
            this.column=column;
            this.mask=mask;
        }
        BinaryProperty(int source) {
            this.column=source;
            this.mask=0;
        }
        final int getSource() {
            return mask==0 ? column : SRC_PROPSVEC;
        }
        boolean contains(int c) {
            // systematic, directly stored properties
            return (getAdditional(c, column)&mask)!=0;
        }
    }

    private class CaseBinaryProperty extends BinaryProperty {  // case mapping properties
        int which;
        CaseBinaryProperty(int which) {
            super(SRC_CASE);
            this.which=which;
        }
        @Override
        boolean contains(int c) {
            return UCaseProps.INSTANCE.hasBinaryProperty(c, which);
        }
    }

    private class NormInertBinaryProperty extends BinaryProperty {  // UCHAR_NF*_INERT properties
        int which;
        NormInertBinaryProperty(int source, int which) {
            super(source);
            this.which=which;
        }
        @Override
        boolean contains(int c) {
            return Norm2AllModes.getN2WithImpl(which-UProperty.NFD_INERT).isInert(c);
        }
    }

    BinaryProperty[] binProps={
        /*
         * Binary-property implementations must be in order of corresponding UProperty,
         * and there must be exactly one entry per binary UProperty.
         */
        new BinaryProperty(1, (1<<ALPHABETIC_PROPERTY_)),
        new BinaryProperty(1, (1<<ASCII_HEX_DIGIT_PROPERTY_)),
        new BinaryProperty(SRC_BIDI) {  // UCHAR_BIDI_CONTROL
            @Override
            boolean contains(int c) {
                return UBiDiProps.INSTANCE.isBidiControl(c);
            }
        },
        new BinaryProperty(SRC_BIDI) {  // UCHAR_BIDI_MIRRORED
            @Override
            boolean contains(int c) {
                return UBiDiProps.INSTANCE.isMirrored(c);
            }
        },
        new BinaryProperty(1, (1<<DASH_PROPERTY_)),
        new BinaryProperty(1, (1<<DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_)),
        new BinaryProperty(1, (1<<DEPRECATED_PROPERTY_)),
        new BinaryProperty(1, (1<<DIACRITIC_PROPERTY_)),
        new BinaryProperty(1, (1<<EXTENDER_PROPERTY_)),
        new BinaryProperty(SRC_NFC) {  // UCHAR_FULL_COMPOSITION_EXCLUSION
            @Override
            boolean contains(int c) {
                // By definition, Full_Composition_Exclusion is the same as NFC_QC=No.
                Normalizer2Impl impl=Norm2AllModes.getNFCInstance().impl;
                return impl.isCompNo(impl.getNorm16(c));
            }
        },
        new BinaryProperty(1, (1<<GRAPHEME_BASE_PROPERTY_)),
        new BinaryProperty(1, (1<<GRAPHEME_EXTEND_PROPERTY_)),
        new BinaryProperty(1, (1<<GRAPHEME_LINK_PROPERTY_)),
        new BinaryProperty(1, (1<<HEX_DIGIT_PROPERTY_)),
        new BinaryProperty(1, (1<<HYPHEN_PROPERTY_)),
        new BinaryProperty(1, (1<<ID_CONTINUE_PROPERTY_)),
        new BinaryProperty(1, (1<<ID_START_PROPERTY_)),
        new BinaryProperty(1, (1<<IDEOGRAPHIC_PROPERTY_)),
        new BinaryProperty(1, (1<<IDS_BINARY_OPERATOR_PROPERTY_)),
        new BinaryProperty(1, (1<<IDS_TRINARY_OPERATOR_PROPERTY_)),
        new BinaryProperty(SRC_BIDI) {  // UCHAR_JOIN_CONTROL
            @Override
            boolean contains(int c) {
                return UBiDiProps.INSTANCE.isJoinControl(c);
            }
        },
        new BinaryProperty(1, (1<<LOGICAL_ORDER_EXCEPTION_PROPERTY_)),
        new CaseBinaryProperty(UProperty.LOWERCASE),
        new BinaryProperty(1, (1<<MATH_PROPERTY_)),
        new BinaryProperty(1, (1<<NONCHARACTER_CODE_POINT_PROPERTY_)),
        new BinaryProperty(1, (1<<QUOTATION_MARK_PROPERTY_)),
        new BinaryProperty(1, (1<<RADICAL_PROPERTY_)),
        new CaseBinaryProperty(UProperty.SOFT_DOTTED),
        new BinaryProperty(1, (1<<TERMINAL_PUNCTUATION_PROPERTY_)),
        new BinaryProperty(1, (1<<UNIFIED_IDEOGRAPH_PROPERTY_)),
        new CaseBinaryProperty(UProperty.UPPERCASE),
        new BinaryProperty(1, (1<<WHITE_SPACE_PROPERTY_)),
        new BinaryProperty(1, (1<<XID_CONTINUE_PROPERTY_)),
        new BinaryProperty(1, (1<<XID_START_PROPERTY_)),
        new CaseBinaryProperty(UProperty.CASE_SENSITIVE),
        new BinaryProperty(1, (1<<S_TERM_PROPERTY_)),
        new BinaryProperty(1, (1<<VARIATION_SELECTOR_PROPERTY_)),
        new NormInertBinaryProperty(SRC_NFC, UProperty.NFD_INERT),
        new NormInertBinaryProperty(SRC_NFKC, UProperty.NFKD_INERT),
        new NormInertBinaryProperty(SRC_NFC, UProperty.NFC_INERT),
        new NormInertBinaryProperty(SRC_NFKC, UProperty.NFKC_INERT),
        new BinaryProperty(SRC_NFC_CANON_ITER) {  // UCHAR_SEGMENT_STARTER
            @Override
            boolean contains(int c) {
                return Norm2AllModes.getNFCInstance().impl.
                    ensureCanonIterData().isCanonSegmentStarter(c);
            }
        },
        new BinaryProperty(1, (1<<PATTERN_SYNTAX)),
        new BinaryProperty(1, (1<<PATTERN_WHITE_SPACE)),
        new BinaryProperty(SRC_CHAR_AND_PROPSVEC) {  // UCHAR_POSIX_ALNUM
            @Override
            boolean contains(int c) {
                return UCharacter.isUAlphabetic(c) || UCharacter.isDigit(c);
            }
        },
        new BinaryProperty(SRC_CHAR) {  // UCHAR_POSIX_BLANK
            @Override
            boolean contains(int c) {
                // "horizontal space"
                if(c<=0x9f) {
                    return c==9 || c==0x20; /* TAB or SPACE */
                } else {
                    /* Zs */
                    return UCharacter.getType(c)==UCharacter.SPACE_SEPARATOR;
                }
            }
        },
        new BinaryProperty(SRC_CHAR) {  // UCHAR_POSIX_GRAPH
            @Override
            boolean contains(int c) {
                return isgraphPOSIX(c);
            }
        },
        new BinaryProperty(SRC_CHAR) {  // UCHAR_POSIX_PRINT
            @Override
            boolean contains(int c) {
                /*
                 * Checks if codepoint is in \p{graph}\p{blank} - \p{cntrl}.
                 *
                 * The only cntrl character in graph+blank is TAB (in blank).
                 * Here we implement (blank-TAB)=Zs instead of calling u_isblank().
                 */
                return (UCharacter.getType(c)==UCharacter.SPACE_SEPARATOR) || isgraphPOSIX(c);
            }
        },
        new BinaryProperty(SRC_CHAR) {  // UCHAR_POSIX_XDIGIT
            @Override
            boolean contains(int c) {
                /* check ASCII and Fullwidth ASCII a-fA-F */
                if(
                    (c<=0x66 && c>=0x41 && (c<=0x46 || c>=0x61)) ||
                    (c>=0xff21 && c<=0xff46 && (c<=0xff26 || c>=0xff41))
                ) {
                    return true;
                }
                return UCharacter.getType(c)==UCharacter.DECIMAL_DIGIT_NUMBER;
            }
        },
        new CaseBinaryProperty(UProperty.CASED),
        new CaseBinaryProperty(UProperty.CASE_IGNORABLE),
        new CaseBinaryProperty(UProperty.CHANGES_WHEN_LOWERCASED),
        new CaseBinaryProperty(UProperty.CHANGES_WHEN_UPPERCASED),
        new CaseBinaryProperty(UProperty.CHANGES_WHEN_TITLECASED),
        new BinaryProperty(SRC_CASE_AND_NORM) {  // UCHAR_CHANGES_WHEN_CASEFOLDED
            @Override
            boolean contains(int c) {
                String nfd=Norm2AllModes.getNFCInstance().impl.getDecomposition(c);
                if(nfd!=null) {
                    /* c has a decomposition */
                    c=nfd.codePointAt(0);
                    if(Character.charCount(c)!=nfd.length()) {
                        /* multiple code points */
                        c=-1;
                    }
                } else if(c<0) {
                    return false;  /* protect against bad input */
                }
                if(c>=0) {
                    /* single code point */
                    UCaseProps csp=UCaseProps.INSTANCE;
                    UCaseProps.dummyStringBuilder.setLength(0);
                    return csp.toFullFolding(c, UCaseProps.dummyStringBuilder,
                                             UCharacter.FOLD_CASE_DEFAULT)>=0;
                } else {
                    String folded=UCharacter.foldCase(nfd, true);
                    return !folded.equals(nfd);
                }
            }
        },
        new CaseBinaryProperty(UProperty.CHANGES_WHEN_CASEMAPPED),
        new BinaryProperty(SRC_NFKC_CF) {  // UCHAR_CHANGES_WHEN_NFKC_CASEFOLDED
            @Override
            boolean contains(int c) {
                Normalizer2Impl kcf=Norm2AllModes.getNFKC_CFInstance().impl;
                String src=UTF16.valueOf(c);
                StringBuilder dest=new StringBuilder();
                // Small destCapacity for NFKC_CF(c).
                Normalizer2Impl.ReorderingBuffer buffer=new Normalizer2Impl.ReorderingBuffer(kcf, dest, 5);
                kcf.compose(src, 0, src.length(), false, true, buffer);
                return !Normalizer2Impl.UTF16Plus.equal(dest, src);
            }
        },
        new BinaryProperty(2, 1<<PROPS_2_EMOJI),
        new BinaryProperty(2, 1<<PROPS_2_EMOJI_PRESENTATION),
        new BinaryProperty(2, 1<<PROPS_2_EMOJI_MODIFIER),
        new BinaryProperty(2, 1<<PROPS_2_EMOJI_MODIFIER_BASE),
        new BinaryProperty(2, 1<<PROPS_2_EMOJI_COMPONENT),
        new BinaryProperty(SRC_PROPSVEC) {  // REGIONAL_INDICATOR
            // Property starts are a subset of lb=RI etc.
            @Override
            boolean contains(int c) {
                return 0x1F1E6<=c && c<=0x1F1FF;
            }
        },
        new BinaryProperty(1, 1<<PREPENDED_CONCATENATION_MARK),
        new BinaryProperty(2, 1<<PROPS_2_EXTENDED_PICTOGRAPHIC),
    };

    public boolean hasBinaryProperty(int c, int which) {
         if(which<UProperty.BINARY_START || UProperty.BINARY_LIMIT<=which) {
            // not a known binary property
            return false;
        } else {
            return binProps[which].contains(c);
        }
    }

    // int-value and enumerated properties --------------------------------- ***

    public int getType(int c) {
        return getProperty(c)&TYPE_MASK;
    }

    /*
     * Map some of the Grapheme Cluster Break values to Hangul Syllable Types.
     * Hangul_Syllable_Type is fully redundant with a subset of Grapheme_Cluster_Break.
     */
    private static final int /* UHangulSyllableType */ gcbToHst[]={
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_OTHER */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_CONTROL */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_CR */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_EXTEND */
        HangulSyllableType.LEADING_JAMO,     /* U_GCB_L */
        HangulSyllableType.NOT_APPLICABLE,   /* U_GCB_LF */
        HangulSyllableType.LV_SYLLABLE,      /* U_GCB_LV */
        HangulSyllableType.LVT_SYLLABLE,     /* U_GCB_LVT */
        HangulSyllableType.TRAILING_JAMO,    /* U_GCB_T */
        HangulSyllableType.VOWEL_JAMO        /* U_GCB_V */
        /*
         * Omit GCB values beyond what we need for hst.
         * The code below checks for the array length.
         */
    };

    private class IntProperty {
        int column;  // SRC_PROPSVEC column, or "source" if mask==0
        int mask;
        int shift;
        IntProperty(int column, int mask, int shift) {
            this.column=column;
            this.mask=mask;
            this.shift=shift;
        }
        IntProperty(int source) {
            this.column=source;
            this.mask=0;
        }
        final int getSource() {
            return mask==0 ? column : SRC_PROPSVEC;
        }
        int getValue(int c) {
            // systematic, directly stored properties
            return (getAdditional(c, column)&mask)>>>shift;
        }
        int getMaxValue(int which) {
            return (getMaxValues(column)&mask)>>>shift;
        }
    }

    private class BiDiIntProperty extends IntProperty {
        BiDiIntProperty() {
            super(SRC_BIDI);
        }
        @Override
        int getMaxValue(int which) {
            return UBiDiProps.INSTANCE.getMaxValue(which);
        }
    }

    private class CombiningClassIntProperty extends IntProperty {
        CombiningClassIntProperty(int source) {
            super(source);
        }
        @Override
        int getMaxValue(int which) {
            return 0xff;
        }
    }

    private class NormQuickCheckIntProperty extends IntProperty {  // UCHAR_NF*_QUICK_CHECK properties
        int which;
        int max;
        NormQuickCheckIntProperty(int source, int which, int max) {
            super(source);
            this.which=which;
            this.max=max;
        }
        @Override
        int getValue(int c) {
            return Norm2AllModes.getN2WithImpl(which-UProperty.NFD_QUICK_CHECK).getQuickCheck(c);
        }
        @Override
        int getMaxValue(int which) {
            return max;
        }
    }

    IntProperty intProps[]={
        new BiDiIntProperty() {  // BIDI_CLASS
            @Override
            int getValue(int c) {
                return UBiDiProps.INSTANCE.getClass(c);
            }
        },
        new IntProperty(0, BLOCK_MASK_, BLOCK_SHIFT_),
        new CombiningClassIntProperty(SRC_NFC) {  // CANONICAL_COMBINING_CLASS
            @Override
            int getValue(int c) {
                return Normalizer2.getNFDInstance().getCombiningClass(c);
            }
        },
        new IntProperty(2, DECOMPOSITION_TYPE_MASK_, 0),
        new IntProperty(0, EAST_ASIAN_MASK_, EAST_ASIAN_SHIFT_),
        new IntProperty(SRC_CHAR) {  // GENERAL_CATEGORY
            @Override
            int getValue(int c) {
                return getType(c);
            }
            @Override
            int getMaxValue(int which) {
                return UCharacterCategory.CHAR_CATEGORY_COUNT-1;
            }
        },
        new BiDiIntProperty() {  // JOINING_GROUP
            @Override
            int getValue(int c) {
                return UBiDiProps.INSTANCE.getJoiningGroup(c);
            }
        },
        new BiDiIntProperty() {  // JOINING_TYPE
            @Override
            int getValue(int c) {
                return UBiDiProps.INSTANCE.getJoiningType(c);
            }
        },
        new IntProperty(2, LB_MASK, LB_SHIFT),  // LINE_BREAK
        new IntProperty(SRC_CHAR) {  // NUMERIC_TYPE
            @Override
            int getValue(int c) {
                return ntvGetType(getNumericTypeValue(getProperty(c)));
            }
            @Override
            int getMaxValue(int which) {
                return NumericType.COUNT-1;
            }
        },
        new IntProperty(0, SCRIPT_MASK_, 0) {
            @Override
            int getValue(int c) {
                return UScript.getScript(c);
            }
        },
        new IntProperty(SRC_PROPSVEC) {  // HANGUL_SYLLABLE_TYPE
            @Override
            int getValue(int c) {
                /* see comments on gcbToHst[] above */
                int gcb=(getAdditional(c, 2)&GCB_MASK)>>>GCB_SHIFT;
                if(gcb<gcbToHst.length) {
                    return gcbToHst[gcb];
                } else {
                    return HangulSyllableType.NOT_APPLICABLE;
                }
            }
            @Override
            int getMaxValue(int which) {
                return HangulSyllableType.COUNT-1;
            }
        },
        // max=1=YES -- these are never "maybe", only "no" or "yes"
        new NormQuickCheckIntProperty(SRC_NFC, UProperty.NFD_QUICK_CHECK, 1),
        new NormQuickCheckIntProperty(SRC_NFKC, UProperty.NFKD_QUICK_CHECK, 1),
        // max=2=MAYBE
        new NormQuickCheckIntProperty(SRC_NFC, UProperty.NFC_QUICK_CHECK, 2),
        new NormQuickCheckIntProperty(SRC_NFKC, UProperty.NFKC_QUICK_CHECK, 2),
        new CombiningClassIntProperty(SRC_NFC) {  // LEAD_CANONICAL_COMBINING_CLASS
            @Override
            int getValue(int c) {
                return Norm2AllModes.getNFCInstance().impl.getFCD16(c)>>8;
            }
        },
        new CombiningClassIntProperty(SRC_NFC) {  // TRAIL_CANONICAL_COMBINING_CLASS
            @Override
            int getValue(int c) {
                return Norm2AllModes.getNFCInstance().impl.getFCD16(c)&0xff;
            }
        },
        new IntProperty(2, GCB_MASK, GCB_SHIFT),  // GRAPHEME_CLUSTER_BREAK
        new IntProperty(2, SB_MASK, SB_SHIFT),  // SENTENCE_BREAK
        new IntProperty(2, WB_MASK, WB_SHIFT),  // WORD_BREAK
        new BiDiIntProperty() {  // BIDI_PAIRED_BRACKET_TYPE
            @Override
            int getValue(int c) {
                return UBiDiProps.INSTANCE.getPairedBracketType(c);
            }
        },
        new IntProperty(SRC_INPC) {
            @Override
            int getValue(int c) {
                return InPCTrie.INSTANCE.get(c);
            }
            @Override
            int getMaxValue(int which) {
                return 14;
            }
        },
        new IntProperty(SRC_INSC) {
            @Override
            int getValue(int c) {
                return InSCTrie.INSTANCE.get(c);
            }
            @Override
            int getMaxValue(int which) {
                return 35;
            }
        },
        new IntProperty(SRC_VO) {
            @Override
            int getValue(int c) {
                return VoTrie.INSTANCE.get(c);
            }
            @Override
            int getMaxValue(int which) {
                return 3;
            }
        },
    };

    public int getIntPropertyValue(int c, int which) {
        if(which<UProperty.INT_START) {
            if(UProperty.BINARY_START<=which && which<UProperty.BINARY_LIMIT) {
                return binProps[which].contains(c) ? 1 : 0;
            }
        } else if(which<UProperty.INT_LIMIT) {
            return intProps[which-UProperty.INT_START].getValue(c);
        } else if (which == UProperty.GENERAL_CATEGORY_MASK) {
            return getMask(getType(c));
        }
        return 0; // undefined
    }

    public int getIntPropertyMaxValue(int which) {
        if(which<UProperty.INT_START) {
            if(UProperty.BINARY_START<=which && which<UProperty.BINARY_LIMIT) {
                return 1;  // maximum TRUE for all binary properties
            }
        } else if(which<UProperty.INT_LIMIT) {
            return intProps[which-UProperty.INT_START].getMaxValue(which);
        }
        return -1; // undefined
    }

    final int getSource(int which) {
        if(which<UProperty.BINARY_START) {
            return SRC_NONE; /* undefined */
        } else if(which<UProperty.BINARY_LIMIT) {
            return binProps[which].getSource();
        } else if(which<UProperty.INT_START) {
            return SRC_NONE; /* undefined */
        } else if(which<UProperty.INT_LIMIT) {
            return intProps[which-UProperty.INT_START].getSource();
        } else if(which<UProperty.STRING_START) {
            switch(which) {
            case UProperty.GENERAL_CATEGORY_MASK:
            case UProperty.NUMERIC_VALUE:
                return SRC_CHAR;

            default:
                return SRC_NONE;
            }
        } else if(which<UProperty.STRING_LIMIT) {
            switch(which) {
            case UProperty.AGE:
                return SRC_PROPSVEC;

            case UProperty.BIDI_MIRRORING_GLYPH:
                return SRC_BIDI;

            case UProperty.CASE_FOLDING:
            case UProperty.LOWERCASE_MAPPING:
            case UProperty.SIMPLE_CASE_FOLDING:
            case UProperty.SIMPLE_LOWERCASE_MAPPING:
            case UProperty.SIMPLE_TITLECASE_MAPPING:
            case UProperty.SIMPLE_UPPERCASE_MAPPING:
            case UProperty.TITLECASE_MAPPING:
            case UProperty.UPPERCASE_MAPPING:
                return SRC_CASE;

            case UProperty.ISO_COMMENT:
            case UProperty.NAME:
            case UProperty.UNICODE_1_NAME:
                return SRC_NAMES;

            default:
                return SRC_NONE;
            }
        } else {
            switch(which) {
            case UProperty.SCRIPT_EXTENSIONS:
                return SRC_PROPSVEC;
            default:
                return SRC_NONE; /* undefined */
            }
        }
    }

    /**
     * <p>
     * Unicode property names and property value names are compared
     * "loosely". Property[Value]Aliases.txt say:
     * <quote>
     *   "With loose matching of property names, the case distinctions,
     *    whitespace, and '_' are ignored."
     * </quote>
     * </p>
     * <p>
     * This function does just that, for ASCII (char *) name strings.
     * It is almost identical to ucnv_compareNames() but also ignores
     * ASCII White_Space characters (U+0009..U+000d).
     * </p>
     * @param name1 name to compare
     * @param name2 name to compare
     * @return 0 if names are equal, < 0 if name1 is less than name2 and > 0
     *         if name1 is greater than name2.
     */
    /* to be implemented in 2.4
     * public static int comparePropertyNames(String name1, String name2)
    {
        int result = 0;
        int i1 = 0;
        int i2 = 0;
        while (true) {
            char ch1 = 0;
            char ch2 = 0;
            // Ignore delimiters '-', '_', and ASCII White_Space
            if (i1 < name1.length()) {
                ch1 = name1.charAt(i1 ++);
            }
            while (ch1 == '-' || ch1 == '_' || ch1 == ' ' || ch1 == '\t'
                   || ch1 == '\n' // synwee what is || ch1 == '\v'
                   || ch1 == '\f' || ch1=='\r') {
                if (i1 < name1.length()) {
                    ch1 = name1.charAt(i1 ++);
                }
                else {
                    ch1 = 0;
                }
            }
            if (i2 < name2.length()) {
                ch2 = name2.charAt(i2 ++);
            }
            while (ch2 == '-' || ch2 == '_' || ch2 == ' ' || ch2 == '\t'
                   || ch2 == '\n' // synwee what is || ch1 == '\v'
                   || ch2 == '\f' || ch2=='\r') {
                if (i2 < name2.length()) {
                    ch2 = name2.charAt(i2 ++);
                }
                else {
                    ch2 = 0;
                }
            }

            // If we reach the ends of both strings then they match
            if (ch1 == 0 && ch2 == 0) {
                return 0;
            }

            // Case-insensitive comparison
            if (ch1 != ch2) {
                result = Character.toLowerCase(ch1)
                                                - Character.toLowerCase(ch2);
                if (result != 0) {
                    return result;
                }
            }
        }
    }
    */

    /**
     * Get the the maximum values for some enum/int properties.
     * @return maximum values for the integer properties.
     */
    public int getMaxValues(int column)
    {
       // return m_maxBlockScriptValue_;

        switch(column) {
        case 0:
            return m_maxBlockScriptValue_;
        case 2:
            return m_maxJTGValue_;
        default:
            return 0;
        }
    }

    /**
     * Gets the type mask
     * @param type character type
     * @return mask
     */
    public static final int getMask(int type)
    {
        return 1 << type;
    }


    /**
     * Returns the digit values of characters like 'A' - 'Z', normal,
     * half-width and full-width. This method assumes that the other digit
     * characters are checked by the calling method.
     * @param ch character to test
     * @return -1 if ch is not a character of the form 'A' - 'Z', otherwise
     *         its corresponding digit will be returned.
     */
    public static int getEuropeanDigit(int ch) {
        if ((ch > 0x7a && ch < 0xff21)
            || ch < 0x41 || (ch > 0x5a && ch < 0x61)
            || ch > 0xff5a || (ch > 0xff3a && ch < 0xff41)) {
            return -1;
        }
        if (ch <= 0x7a) {
            // ch >= 0x41 or ch < 0x61
            return ch + 10 - ((ch <= 0x5a) ? 0x41 : 0x61);
        }
        // ch >= 0xff21
        if (ch <= 0xff3a) {
            return ch + 10 - 0xff21;
        }
        // ch >= 0xff41 && ch <= 0xff5a
        return ch + 10 - 0xff41;
    }

    public int digit(int c) {
        int value = getNumericTypeValue(getProperty(c)) - NTV_DECIMAL_START_;
        if(value<=9) {
            return value;
        } else {
            return -1;
        }
    }

    public int getNumericValue(int c) {
        // slightly pruned version of getUnicodeNumericValue(), plus getEuropeanDigit()
        int ntv = getNumericTypeValue(getProperty(c));

        if(ntv==NTV_NONE_) {
            return getEuropeanDigit(c);
        } else if(ntv<NTV_DIGIT_START_) {
            /* decimal digit */
            return ntv-NTV_DECIMAL_START_;
        } else if(ntv<NTV_NUMERIC_START_) {
            /* other digit */
            return ntv-NTV_DIGIT_START_;
        } else if(ntv<NTV_FRACTION_START_) {
            /* small integer */
            return ntv-NTV_NUMERIC_START_;
        } else if(ntv<NTV_LARGE_START_) {
            /* fraction */
            return -2;
        } else if(ntv<NTV_BASE60_START_) {
            /* large, single-significant-digit integer */
            int mant=(ntv>>5)-14;
            int exp=(ntv&0x1f)+2;
            if(exp<9 || (exp==9 && mant<=2)) {
                int numValue=mant;
                do {
                    numValue*=10;
                } while(--exp>0);
                return numValue;
            } else {
                return -2;
            }
        } else if(ntv<NTV_FRACTION20_START_) {
            /* sexagesimal (base 60) integer */
            int numValue=(ntv>>2)-0xbf;
            int exp=(ntv&3)+1;

            switch(exp) {
            case 4:
                numValue*=60*60*60*60;
                break;
            case 3:
                numValue*=60*60*60;
                break;
            case 2:
                numValue*=60*60;
                break;
            case 1:
                numValue*=60;
                break;
            case 0:
            default:
                break;
            }

            return numValue;
        } else if(ntv<NTV_RESERVED_START_) {
            // fraction-20 e.g. 3/80
            return -2;
        } else {
            /* reserved */
            return -2;
        }
    }

    public double getUnicodeNumericValue(int c) {
        // equivalent to c version double u_getNumericValue(UChar32 c)
        int ntv = getNumericTypeValue(getProperty(c));

        if(ntv==NTV_NONE_) {
            return UCharacter.NO_NUMERIC_VALUE;
        } else if(ntv<NTV_DIGIT_START_) {
            /* decimal digit */
            return ntv-NTV_DECIMAL_START_;
        } else if(ntv<NTV_NUMERIC_START_) {
            /* other digit */
            return ntv-NTV_DIGIT_START_;
        } else if(ntv<NTV_FRACTION_START_) {
            /* small integer */
            return ntv-NTV_NUMERIC_START_;
        } else if(ntv<NTV_LARGE_START_) {
            /* fraction */
            int numerator=(ntv>>4)-12;
            int denominator=(ntv&0xf)+1;
            return (double)numerator/denominator;
        } else if(ntv<NTV_BASE60_START_) {
            /* large, single-significant-digit integer */
            double numValue;
            int mant=(ntv>>5)-14;
            int exp=(ntv&0x1f)+2;
            numValue=mant;

            /* multiply by 10^exp without math.h */
            while(exp>=4) {
                numValue*=10000.;
                exp-=4;
            }
            switch(exp) {
            case 3:
                numValue*=1000.;
                break;
            case 2:
                numValue*=100.;
                break;
            case 1:
                numValue*=10.;
                break;
            case 0:
            default:
                break;
            }

            return numValue;
        } else if(ntv<NTV_FRACTION20_START_) {
            /* sexagesimal (base 60) integer */
            int numValue=(ntv>>2)-0xbf;
            int exp=(ntv&3)+1;

            switch(exp) {
            case 4:
                numValue*=60*60*60*60;
                break;
            case 3:
                numValue*=60*60*60;
                break;
            case 2:
                numValue*=60*60;
                break;
            case 1:
                numValue*=60;
                break;
            case 0:
            default:
                break;
            }

            return numValue;
        } else if(ntv<NTV_FRACTION32_START_) {
            // fraction-20 e.g. 3/80
            int frac20=ntv-NTV_FRACTION20_START_;  // 0..0x17
            int numerator=2*(frac20&3)+1;
            int denominator=20<<(frac20>>2);
            return (double)numerator/denominator;
        } else if(ntv<NTV_RESERVED_START_) {
            // fraction-32 e.g. 3/64
            int frac32=ntv-NTV_FRACTION32_START_;  // 0..15
            int numerator=2*(frac32&3)+1;
            int denominator=32<<(frac32>>2);
            return (double)numerator/denominator;
        } else {
            /* reserved */
            return UCharacter.NO_NUMERIC_VALUE;
        }
    }

    // protected variables -----------------------------------------------

    /**
     * Extra property trie
     */
    Trie2_16 m_additionalTrie_;
    /**
     * Extra property vectors, 1st column for age and second for binary
     * properties.
     */
    int m_additionalVectors_[];
    /**
     * Number of additional columns
     */
    int m_additionalColumnsCount_;
    /**
     * Maximum values for block, bits used as in vector word
     * 0
     */
    int m_maxBlockScriptValue_;
    /**
     * Maximum values for script, bits used as in vector word
     * 0
     */
     int m_maxJTGValue_;

    /**
     * Script_Extensions data
     */
    public char[] m_scriptExtensions_;

    // private variables -------------------------------------------------

    /**
    * Default name of the datafile
    */
    private static final String DATA_FILE_NAME_ = "uprops.icu";

    // property data constants -------------------------------------------------

    /**
     * Numeric types and values in the main properties words.
     */
    private static final int NUMERIC_TYPE_VALUE_SHIFT_ = 6;
    private static final int getNumericTypeValue(int props) {
        return props >> NUMERIC_TYPE_VALUE_SHIFT_;
    }
    /* constants for the storage form of numeric types and values */
    /** No numeric value. */
    private static final int NTV_NONE_ = 0;
    /** Decimal digits: nv=0..9 */
    private static final int NTV_DECIMAL_START_ = 1;
    /** Other digits: nv=0..9 */
    private static final int NTV_DIGIT_START_ = 11;
    /** Small integers: nv=0..154 */
    private static final int NTV_NUMERIC_START_ = 21;
    /** Fractions: ((ntv>>4)-12) / ((ntv&0xf)+1) = -1..17 / 1..16 */
    private static final int NTV_FRACTION_START_ = 0xb0;
    /**
     * Large integers:
     * ((ntv>>5)-14) * 10^((ntv&0x1f)+2) = (1..9)*(10^2..10^33)
     * (only one significant decimal digit)
     */
    private static final int NTV_LARGE_START_ = 0x1e0;
    /**
     * Sexagesimal numbers:
     * ((ntv>>2)-0xbf) * 60^((ntv&3)+1) = (1..9)*(60^1..60^4)
     */
    private static final int NTV_BASE60_START_=0x300;
    /**
     * Fraction-20 values:
     * frac20 = ntv-0x324 = 0..0x17 -> 1|3|5|7 / 20|40|80|160|320|640
     * numerator: num = 2*(frac20&3)+1
     * denominator: den = 20<<(frac20>>2)
     */
    private static final int NTV_FRACTION20_START_ = NTV_BASE60_START_ + 36;  // 0x300+9*4=0x324
    /**
     * Fraction-32 values:
     * frac32 = ntv-0x34c = 0..15 -> 1|3|5|7 / 32|64|128|256
     * numerator: num = 2*(frac32&3)+1
     * denominator: den = 32<<(frac32>>2)
     */
    private static final int NTV_FRACTION32_START_ = NTV_FRACTION20_START_ + 24;  // 0x324+6*4=0x34c
    /** No numeric value (yet). */
    private static final int NTV_RESERVED_START_ = NTV_FRACTION32_START_ + 16;  // 0x34c+4*4=0x35c

    private static final int ntvGetType(int ntv) {
        return
            (ntv==NTV_NONE_) ? NumericType.NONE :
            (ntv<NTV_DIGIT_START_) ?  NumericType.DECIMAL :
            (ntv<NTV_NUMERIC_START_) ? NumericType.DIGIT :
            NumericType.NUMERIC;
    }

    /*
     * Properties in vector word 0
     * Bits
     * 31..24   DerivedAge version major/minor one nibble each
     * 23..22   3..1: Bits 7..0 = Script_Extensions index
     *             3: Script value from Script_Extensions
     *             2: Script=Inherited
     *             1: Script=Common
     *             0: Script=bits 7..0
     * 21..20   reserved
     * 19..17   East Asian Width
     * 16.. 8   UBlockCode
     *  7.. 0   UScriptCode
     */

    /**
     * Script_Extensions: mask includes Script
     */
    public static final int SCRIPT_X_MASK = 0x00c000ff;
    //private static final int SCRIPT_X_SHIFT = 22;
    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_MASK
     */
    private static final int EAST_ASIAN_MASK_ = 0x000e0000;
    /**
     * Integer properties mask and shift values for East Asian cell width.
     * Equivalent to icu4c UPROPS_EA_SHIFT
     */
    private static final int EAST_ASIAN_SHIFT_ = 17;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_MASK
     */
    private static final int BLOCK_MASK_ = 0x0001ff00;
    /**
     * Integer properties mask and shift values for blocks.
     * Equivalent to icu4c UPROPS_BLOCK_SHIFT
     */
    private static final int BLOCK_SHIFT_ = 8;
    /**
     * Integer properties mask and shift values for scripts.
     * Equivalent to icu4c UPROPS_SHIFT_MASK
     */
    public static final int SCRIPT_MASK_ = 0x000000ff;

    /* SCRIPT_X_WITH_COMMON must be the lowest value that involves Script_Extensions. */
    public static final int SCRIPT_X_WITH_COMMON = 0x400000;
    public static final int SCRIPT_X_WITH_INHERITED = 0x800000;
    public static final int SCRIPT_X_WITH_OTHER = 0xc00000;

    /**
     * Additional properties used in internal trie data
     */
    /*
     * Properties in vector word 1
     * Each bit encodes one binary property.
     * The following constants represent the bit number, use 1<<UPROPS_XYZ.
     * UPROPS_BINARY_1_TOP<=32!
     *
     * Keep this list of property enums in sync with
     * propListNames[] in icu/source/tools/genprops/props2.c!
     *
     * ICU 2.6/uprops format version 3.2 stores full properties instead of "Other_".
     */
    private static final int WHITE_SPACE_PROPERTY_ = 0;
    private static final int DASH_PROPERTY_ = 1;
    private static final int HYPHEN_PROPERTY_ = 2;
    private static final int QUOTATION_MARK_PROPERTY_ = 3;
    private static final int TERMINAL_PUNCTUATION_PROPERTY_ = 4;
    private static final int MATH_PROPERTY_ = 5;
    private static final int HEX_DIGIT_PROPERTY_ = 6;
    private static final int ASCII_HEX_DIGIT_PROPERTY_ = 7;
    private static final int ALPHABETIC_PROPERTY_ = 8;
    private static final int IDEOGRAPHIC_PROPERTY_ = 9;
    private static final int DIACRITIC_PROPERTY_ = 10;
    private static final int EXTENDER_PROPERTY_ = 11;
    private static final int NONCHARACTER_CODE_POINT_PROPERTY_ = 12;
    private static final int GRAPHEME_EXTEND_PROPERTY_ = 13;
    private static final int GRAPHEME_LINK_PROPERTY_ = 14;
    private static final int IDS_BINARY_OPERATOR_PROPERTY_ = 15;
    private static final int IDS_TRINARY_OPERATOR_PROPERTY_ = 16;
    private static final int RADICAL_PROPERTY_ = 17;
    private static final int UNIFIED_IDEOGRAPH_PROPERTY_ = 18;
    private static final int DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_ = 19;
    private static final int DEPRECATED_PROPERTY_ = 20;
    private static final int LOGICAL_ORDER_EXCEPTION_PROPERTY_ = 21;
    private static final int XID_START_PROPERTY_ = 22;
    private static final int XID_CONTINUE_PROPERTY_ = 23;
    private static final int ID_START_PROPERTY_    = 24;
    private static final int ID_CONTINUE_PROPERTY_ = 25;
    private static final int GRAPHEME_BASE_PROPERTY_ = 26;
    private static final int S_TERM_PROPERTY_ = 27;
    private static final int VARIATION_SELECTOR_PROPERTY_ = 28;
    private static final int PATTERN_SYNTAX = 29;                   /* new in ICU 3.4 and Unicode 4.1 */
    private static final int PATTERN_WHITE_SPACE = 30;
    private static final int PREPENDED_CONCATENATION_MARK = 31;     // new in ICU 60 and Unicode 10

    /*
     * Properties in vector word 2
     * Bits
     * 31..26   http://www.unicode.org/reports/tr51/#Emoji_Properties
     * 25..20   Line Break
     * 19..15   Sentence Break
     * 14..10   Word Break
     *  9.. 5   Grapheme Cluster Break
     *  4.. 0   Decomposition Type
     */
    private static final int PROPS_2_EXTENDED_PICTOGRAPHIC=26;
    private static final int PROPS_2_EMOJI_COMPONENT = 27;
    private static final int PROPS_2_EMOJI = 28;
    private static final int PROPS_2_EMOJI_PRESENTATION = 29;
    private static final int PROPS_2_EMOJI_MODIFIER = 30;
    private static final int PROPS_2_EMOJI_MODIFIER_BASE = 31;

    private static final int LB_MASK          = 0x03f00000;
    private static final int LB_SHIFT         = 20;

    private static final int SB_MASK          = 0x000f8000;
    private static final int SB_SHIFT         = 15;

    private static final int WB_MASK          = 0x00007c00;
    private static final int WB_SHIFT         = 10;

    private static final int GCB_MASK         = 0x000003e0;
    private static final int GCB_SHIFT        = 5;

    /**
     * Integer properties mask for decomposition type.
     * Equivalent to icu4c UPROPS_DT_MASK.
     */
    private static final int DECOMPOSITION_TYPE_MASK_ = 0x0000001f;

    /**
     * First nibble shift
     */
    private static final int FIRST_NIBBLE_SHIFT_ = 0x4;
    /**
     * Second nibble mask
     */
    private static final int LAST_NIBBLE_MASK_ = 0xF;
    /**
     * Age value shift
     */
    private static final int AGE_SHIFT_ = 24;


    // private constructors --------------------------------------------------

    /**
     * Constructor
     * @exception IOException thrown when data reading fails or data corrupted
     */
    private UCharacterProperty() throws IOException
    {
        // consistency check
        if(binProps.length!=UProperty.BINARY_LIMIT) {
            throw new ICUException("binProps.length!=UProperty.BINARY_LIMIT");
        }
        if(intProps.length!=(UProperty.INT_LIMIT-UProperty.INT_START)) {
            throw new ICUException("intProps.length!=(UProperty.INT_LIMIT-UProperty.INT_START)");
        }

        // jar access
        ByteBuffer bytes=ICUBinary.getRequiredData(DATA_FILE_NAME_);
        m_unicodeVersion_ = ICUBinary.readHeaderAndDataVersion(bytes, DATA_FORMAT, new IsAcceptable());
        // Read or skip the 16 indexes.
        int propertyOffset = bytes.getInt();
        /* exceptionOffset = */ bytes.getInt();
        /* caseOffset = */ bytes.getInt();
        int additionalOffset = bytes.getInt();
        int additionalVectorsOffset = bytes.getInt();
        m_additionalColumnsCount_ = bytes.getInt();
        int scriptExtensionsOffset = bytes.getInt();
        int reservedOffset7 = bytes.getInt();
        /* reservedOffset8 = */ bytes.getInt();
        /* dataTopOffset = */ bytes.getInt();
        m_maxBlockScriptValue_ = bytes.getInt();
        m_maxJTGValue_ = bytes.getInt();
        ICUBinary.skipBytes(bytes, (16 - 12) << 2);

        // read the main properties trie
        m_trie_ = Trie2_16.createFromSerialized(bytes);
        int expectedTrieLength = (propertyOffset - 16) * 4;
        int trieLength = m_trie_.getSerializedLength();
        if(trieLength > expectedTrieLength) {
            throw new IOException("uprops.icu: not enough bytes for main trie");
        }
        // skip padding after trie bytes
        ICUBinary.skipBytes(bytes, expectedTrieLength - trieLength);

        // skip unused intervening data structures
        ICUBinary.skipBytes(bytes, (additionalOffset - propertyOffset) * 4);

        if(m_additionalColumnsCount_ > 0) {
            // reads the additional property block
            m_additionalTrie_ = Trie2_16.createFromSerialized(bytes);
            expectedTrieLength = (additionalVectorsOffset-additionalOffset)*4;
            trieLength = m_additionalTrie_.getSerializedLength();
            if(trieLength > expectedTrieLength) {
                throw new IOException("uprops.icu: not enough bytes for additional-properties trie");
            }
            // skip padding after trie bytes
            ICUBinary.skipBytes(bytes, expectedTrieLength - trieLength);

            // additional properties
            int size = scriptExtensionsOffset - additionalVectorsOffset;
            m_additionalVectors_ = ICUBinary.getInts(bytes, size, 0);
        }

        // Script_Extensions
        int numChars = (reservedOffset7 - scriptExtensionsOffset) * 2;
        if(numChars > 0) {
            m_scriptExtensions_ = ICUBinary.getChars(bytes, numChars, 0);
        }
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        @Override
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0] == 7;
        }
    }
    private static final int DATA_FORMAT = 0x5550726F;  // "UPro"

    // private methods -------------------------------------------------------

    /*
     * Compare additional properties to see if it has argument type
     * @param property 32 bit properties
     * @param type character type
     * @return true if property has type
     */
    /*private boolean compareAdditionalType(int property, int type)
    {
        return (property & (1 << type)) != 0;
    }*/

    // property starts for UnicodeSet -------------------------------------- ***

    private static final int TAB     = 0x0009;
    //private static final int LF      = 0x000a;
    //private static final int FF      = 0x000c;
    private static final int CR      = 0x000d;
    private static final int U_A     = 0x0041;
    private static final int U_F     = 0x0046;
    private static final int U_Z     = 0x005a;
    private static final int U_a     = 0x0061;
    private static final int U_f     = 0x0066;
    private static final int U_z     = 0x007a;
    private static final int DEL     = 0x007f;
    private static final int NL      = 0x0085;
    private static final int NBSP    = 0x00a0;
    private static final int CGJ     = 0x034f;
    private static final int FIGURESP= 0x2007;
    private static final int HAIRSP  = 0x200a;
    //private static final int ZWNJ    = 0x200c;
    //private static final int ZWJ     = 0x200d;
    private static final int RLM     = 0x200f;
    private static final int NNBSP   = 0x202f;
    private static final int WJ      = 0x2060;
    private static final int INHSWAP = 0x206a;
    private static final int NOMDIG  = 0x206f;
    private static final int U_FW_A  = 0xff21;
    private static final int U_FW_F  = 0xff26;
    private static final int U_FW_Z  = 0xff3a;
    private static final int U_FW_a  = 0xff41;
    private static final int U_FW_f  = 0xff46;
    private static final int U_FW_z  = 0xff5a;
    private static final int ZWNBSP  = 0xfeff;

    public UnicodeSet addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of the main trie */
        Iterator<Trie2.Range> trieIterator = m_trie_.iterator();
        Trie2.Range range;
        while(trieIterator.hasNext() && !(range=trieIterator.next()).leadSurrogate) {
            set.add(range.startCodePoint);
        }

        /* add code points with hardcoded properties, plus the ones following them */

        /* add for u_isblank() */
        set.add(TAB);
        set.add(TAB+1);

        /* add for IS_THAT_CONTROL_SPACE() */
        set.add(CR+1); /* range TAB..CR */
        set.add(0x1c);
        set.add(0x1f+1);
        set.add(NL);
        set.add(NL+1);

        /* add for u_isIDIgnorable() what was not added above */
        set.add(DEL); /* range DEL..NBSP-1, NBSP added below */
        set.add(HAIRSP);
        set.add(RLM+1);
        set.add(INHSWAP);
        set.add(NOMDIG+1);
        set.add(ZWNBSP);
        set.add(ZWNBSP+1);

        /* add no-break spaces for u_isWhitespace() what was not added above */
        set.add(NBSP);
        set.add(NBSP+1);
        set.add(FIGURESP);
        set.add(FIGURESP+1);
        set.add(NNBSP);
        set.add(NNBSP+1);

        /* add for u_charDigitValue() */
        // TODO remove when UCharacter.getHanNumericValue() is changed to just return
        // Unicode numeric values
        set.add(0x3007);
        set.add(0x3008);
        set.add(0x4e00);
        set.add(0x4e01);
        set.add(0x4e8c);
        set.add(0x4e8d);
        set.add(0x4e09);
        set.add(0x4e0a);
        set.add(0x56db);
        set.add(0x56dc);
        set.add(0x4e94);
        set.add(0x4e95);
        set.add(0x516d);
        set.add(0x516e);
        set.add(0x4e03);
        set.add(0x4e04);
        set.add(0x516b);
        set.add(0x516c);
        set.add(0x4e5d);
        set.add(0x4e5e);

        /* add for u_digit() */
        set.add(U_a);
        set.add(U_z+1);
        set.add(U_A);
        set.add(U_Z+1);
        set.add(U_FW_a);
        set.add(U_FW_z+1);
        set.add(U_FW_A);
        set.add(U_FW_Z+1);

        /* add for u_isxdigit() */
        set.add(U_f+1);
        set.add(U_F+1);
        set.add(U_FW_f+1);
        set.add(U_FW_F+1);

        /* add for UCHAR_DEFAULT_IGNORABLE_CODE_POINT what was not added above */
        set.add(WJ); /* range WJ..NOMDIG */
        set.add(0xfff0);
        set.add(0xfffb+1);
        set.add(0xe0000);
        set.add(0xe0fff+1);

        /* add for UCHAR_GRAPHEME_BASE and others */
        set.add(CGJ);
        set.add(CGJ+1);

        return set; // for chaining
    }

    public void upropsvec_addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of the properties vectors trie */
        if(m_additionalColumnsCount_>0) {
            /* if m_additionalColumnsCount_==0 then the properties vectors trie may not be there at all */
            Iterator<Trie2.Range> trieIterator = m_additionalTrie_.iterator();
            Trie2.Range range;
            while(trieIterator.hasNext() && !(range=trieIterator.next()).leadSurrogate) {
                set.add(range.startCodePoint);
            }
        }
    }

    public UnicodeSet ulayout_addPropertyStarts(int src, UnicodeSet set) {
        CodePointTrie trie;
        switch (src) {
        case SRC_INPC:
            trie = InPCTrie.INSTANCE;
            break;
        case SRC_INSC:
            trie = InSCTrie.INSTANCE;
            break;
        case SRC_VO:
            trie = VoTrie.INSTANCE;
            break;
        default:
            throw new IllegalStateException();
        }

        // Add the start code point of each same-value range of the trie.
        CodePointMap.Range range = new CodePointMap.Range();
        int start = 0;
        while (trie.getRange(start, null, range)) {
            set.add(start);
            start = range.getEnd() + 1;
        }
        return set;
    }

    // This static initializer block must be placed after
    // other static member initialization
    static {
        try {
            INSTANCE = new UCharacterProperty();
        }
        catch (IOException e) {
            throw new MissingResourceException(e.getMessage(),"","");
        }
    }

/*----------------------------------------------------------------
 * Inclusions list
 *----------------------------------------------------------------*/

    /*
     * Return a set of characters for property enumeration.
     * The set implicitly contains 0x110000 as well, which is one more than the highest
     * Unicode code point.
     *
     * This set is used as an ordered list - its code points are ordered, and
     * consecutive code points (in Unicode code point order) in the set define a range.
     * For each two consecutive characters (start, limit) in the set,
     * all of the UCD/normalization and related properties for
     * all code points start..limit-1 are all the same,
     * except for character names and ISO comments.
     *
     * All Unicode code points U+0000..U+10ffff are covered by these ranges.
     * The ranges define a partition of the Unicode code space.
     * ICU uses the inclusions set to enumerate properties for generating
     * UnicodeSets containing all code points that have a certain property value.
     *
     * The Inclusion List is generated from the UCD. It is generated
     * by enumerating the data tries, and code points for hardcoded properties
     * are added as well.
     *
     * --------------------------------------------------------------------------
     *
     * The following are ideas for getting properties-unique code point ranges,
     * with possible optimizations beyond the current implementation.
     * These optimizations would require more code and be more fragile.
     * The current implementation generates one single list (set) for all properties.
     *
     * To enumerate properties efficiently, one needs to know ranges of
     * repetitive values, so that the value of only each start code point
     * can be applied to the whole range.
     * This information is in principle available in the uprops.icu/unorm.icu data.
     *
     * There are two obstacles:
     *
     * 1. Some properties are computed from multiple data structures,
     *    making it necessary to get repetitive ranges by intersecting
     *    ranges from multiple tries.
     *
     * 2. It is not economical to write code for getting repetitive ranges
     *    that are precise for each of some 50 properties.
     *
     * Compromise ideas:
     *
     * - Get ranges per trie, not per individual property.
     *   Each range contains the same values for a whole group of properties.
     *   This would generate currently five range sets, two for uprops.icu tries
     *   and three for unorm.icu tries.
     *
     * - Combine sets of ranges for multiple tries to get sufficient sets
     *   for properties, e.g., the uprops.icu main and auxiliary tries
     *   for all non-normalization properties.
     *
     * Ideas for representing ranges and combining them:
     *
     * - A UnicodeSet could hold just the start code points of ranges.
     *   Multiple sets are easily combined by or-ing them together.
     *
     * - Alternatively, a UnicodeSet could hold each even-numbered range.
     *   All ranges could be enumerated by using each start code point
     *   (for the even-numbered ranges) as well as each limit (end+1) code point
     *   (for the odd-numbered ranges).
     *   It should be possible to combine two such sets by xor-ing them,
     *   but no more than two.
     *
     * The second way to represent ranges may(?!) yield smaller UnicodeSet arrays,
     * but the first one is certainly simpler and applicable for combining more than
     * two range sets.
     *
     * It is possible to combine all range sets for all uprops/unorm tries into one
     * set that can be used for all properties.
     * As an optimization, there could be less-combined range sets for certain
     * groups of properties.
     * The relationship of which less-combined range set to use for which property
     * depends on the implementation of the properties and must be hardcoded
     * - somewhat error-prone and higher maintenance but can be tested easily
     * by building property sets "the simple way" in test code.
     *
     * ---
     *
     * Do not use a UnicodeSet pattern because that causes infinite recursion;
     * UnicodeSet depends on the inclusions set.
     *
     * ---
     *
     * getInclusions() is commented out starting 2005-feb-12 because
     * UnicodeSet now calls the uxyz_addPropertyStarts() directly,
     * and only for the relevant property source.
     */
    /*
    public UnicodeSet getInclusions() {
        UnicodeSet set = new UnicodeSet();
        NormalizerImpl.addPropertyStarts(set);
        addPropertyStarts(set);
        return set;
    }
    */
}
