/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default break-iterator rules.  These rules are more or less general for
 * all locales, although there are probably a few we're missing.  The
 * behavior currently mimics the behavior of BreakIterator in JDK 1.2.
 * There are known deficiencies in this behavior, including the fact that
 * the logic for handling CJK characters works for Japanese but not for
 * Chinese, and that we don't currently have an appropriate locale for
 * Thai.  The resources will eventually be updated to fix these problems.
 */

 /* Modified for Hindi 3/1/99. */

public class BreakIteratorRules extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // BreakIteratorClasses lists the class names to instantiate for each
        // built-in type of BreakIterator
        { "BreakIteratorClasses",
            new String[] { "RuleBasedBreakIterator_New",     // character-break iterator class
                           "RuleBasedBreakIterator_New",     // word-break iterator class
                           "RuleBasedBreakIterator_New",     // line-break iterator class
                           "RuleBasedBreakIterator_New",     // sentence-break iterator class
                           "RuleBasedBreakIterator_New"}     // Title-Case break iterator class
        },

        // rules describing how to break between logical characters
        { "CharacterBreakRules",
            // ignore non-spacing marks and enclosing marks (since we never
            // put a break before ignore characters, this keeps combining
            // accents with the base characters they modify)
            // FIXME: the virama thing is probably a hack...
              "$devaVirama=[\u094d];"
            + "$_ignore_=[[[:Mn:]-$devaVirama][:Me:]];"

            // other category definitions
            + "$choseong=[\u1100-\u115f];"
            + "$jungseong=[\u1160-\u11a7];"
            + "$jongseong=[\u11a8-\u11ff];"
            + "$surr_hi=[\ud800-\udbff];"
            + "$surr_lo=[\udc00-\udfff];"

            // break after every character, except as follows:
            + ".;"

            // keep CRLF sequences together
            + "\r\n;"

            // keep surrogate pairs together
            + "$surr_hi$surr_lo;"

            // keep Hangul syllables spelled out using conjoining jamo together
            + "$choseong*$jungseong*$jongseong*;"

            // revised Devanagari support - full syllables
            // simplified by allowing some nonsense syllables
            // FIXME: nukta, non-spacing matras, and the modifiers
            // are all ignorable, so they don't need to be mentioned
            // here... (but the rules read better if they are...)
            + "$devaNukta=[\u093c];"
            + "$devaVowel=[\u0905-\u0914];"
            + "$devaMatra=[\u093e-\u094c\u0962\u0963];"
            + "$devaConsonant=[\u0915-\u0939\u0958-\u095f];"
            + "$devaModifier=[\u0901-\u0903\u0951-\u0954];"
            + "$zwnj=[\u200c];"
            + "$zwj=[\u200d];"

            // consonant followed optionally by a nukta
            + "$devaCN=($devaConsonant$devaNukta?);"

            // a virama followed by an optional zwj or zwnj
            + "$devaJoin=($devaVirama[$zwj$zwnj]?);"

            // a syllable with at least one consonant
            + "($devaCN$devaJoin)*$devaCN($devaJoin|$devaMatra?$devaModifier*);"

            // a syllable without consonants
            + "$devaVowel$devaModifier*;"
        },

        // default rules for finding word boundaries
        { "WordBreakRules",

            // Surrogates.  Until better support is available, ignore low surrogates
            //              and classify high surrogates according to the characters within the block.
              "$surr_lo=[\udc00-\udfff];"
            + "$surr_hi_let=[\ud800\ud801\ud834\ud835];"   // Hi Surrogates for Old Italic, Gothic, Deseret, Music, Math
            + "$surr_hi_ideo=[\ud840-\ud880];"             // Hi Surrogates for CJK
            + "$surr_hi_tag=[\udb40];"                     // Hi Surrogates for Tags
            + "$surr_hi_pua=[\udb80-\udbff];"              // Hi Surrogates for Private Use.

            // Private Use Area.  Treat like ideographs.
            + "$pua=[\ue000-\uf8ff$surr_hi_pua];"

            // ignore non-spacing marks, enclosing marks, and format characters,
            // all of which should not influence the algorithm
            + "$_ignore_=[[:Mn:][:Me:][:Cf:]$surr_lo$surr_hi_tag];"

            // Hindi phrase separator, kanji, katakana, hiragana, CJK diacriticals,
            // other letters, and digits
            + "$danda=[\u0964\u0965];"
            + "$kanji=[\u3005\u3400-\u4db5\u4e00-\u9fa5\uf900-\ufa6a$surr_hi_ideo$pua];"
            + "$kata=[\u3099-\u309c\u30a1-\u30fe];"
            + "$hira=[\u3041-\u309e\u30fc];"
            + "$let=[[[:L:][:Mc:]$surr_hi_let]-[$kanji$kata$hira]];"
            + "$dgt=[:N:];"

            // punctuation that can occur in the middle of a word: currently
            // dashes, apostrophes, quotation marks, and periods
            + "$mid_word=[[:Pd:]\u00ad\u2027\\\"\\\'\\.];"

            // punctuation that can occur in the middle of a number: currently
            // apostrophes, qoutation marks, periods, commas, and the Arabic
            // decimal point
            + "$mid_num=[\\\"\\\'\\,\u066b\\.];"

            // punctuation that can occur at the beginning of a number: currently
            // the period, the number sign, and all currency symbols except the cents sign
            + "$pre_num=[[[:Sc:]-[\u00a2]]\\#\\.];"

            // punctuation that can occur at the end of a number: currently
            // the percent, per-thousand, per-ten-thousand, and Arabic percent
            // signs, the cents sign, and the ampersand
            + "$post_num=[\\%\\&\u00a2\u066a\u2030\u2031];"

            // line separators: currently LF, FF, PS, and LS
            + "$ls=[\n\u000c\u2028\u2029];"

            // whitespace: all space separators and the tab character
            + "$ws=[[:Zs:]\t];"

            // a word is a sequence of letters that may contain internal
            // punctuation, as long as it begins and ends with a letter and
            // never contains two punctuation marks in a row
            + "$word=($let+($mid_word$let+)*$danda?);"

            // a number is a sequence of digits that may contain internal
            // punctuation, as long as it begins and ends with a digit and
            // never contains two punctuation marks in a row.
            + "$number=($dgt+($mid_num$dgt+)*);"

            // break after every character, with the following exceptions
            // (this will cause punctuation marks that aren't considered
            // part of words or numbers to be treated as words unto themselves)
            + ".;"

            // keep together any sequence of contiguous words and numbers
            // (including just one of either), plus an optional trailing
            // number-suffix character
            + "$word?($number$word)*($number$post_num?)?;"

            // keep together and sequence of contiguous words and numbers
            // that starts with a number-prefix character and a number,
            // and may end with a number-suffix character
            + "$pre_num($number$word)*($number$post_num?)?;"

            // keep together runs of whitespace (optionally with a single trailing
            // line separator or CRLF sequence)
            + "$ws*\r?$ls?;"

            // keep together runs of Katakana
            + "$kata*;"

            // keep together runs of Hiragana
            + "$hira*;"

            // keep together runs of Kanji
            + "$kanji*;"
        },

        // default rules for determining legal line-breaking positions
        { "LineBreakRules",
            // Surrogates.  Until better support is available, ignore low surrogates
            //              and classify high surrogates according to the characters within the block.
              "$surr_lo=[\udc00-\udfff];"
            + "$surr_hi_let=[\ud800\ud801\ud834\ud835];"   // Hi Surrogates for Old Italic, Gothic, Deseret, Music, Math
            + "$surr_hi_ideo=[\ud840-\ud880];"             // Hi Surrogates for CJK
            + "$surr_hi_tag=[\udb40];"                     // Hi Surrogates for Tags
            + "$surr_hi_pua=[\udb80-\udbff];"              // Hi Surrogates for Private Use.

            // Private Use Area.  Treat like ideographs.
             + "$pua=[\ue000-\uf8ff$surr_hi_pua];"

            // ignore non-spacing marks, enclosing marks, and format characters
            + "$_ignore_=[[:Mn:][:Me:][:Cf:]$surr_lo$surr_hi_tag];"

            // Hindi phrase separators
            + "$danda=[\u0964\u0965];"

            // characters that always cause a break: ETX, tab, LF, FF, LS, and PS
            + "$break=[\u0003\t\n\f\u2028\u2029];"

            // characters that always prevent a break: the non-breaking space
            // and similar characters
            + "$nbsp=[\u00a0\u2007\u2011\ufeff];"

            // whitespace: space separators and control characters, except for
            // CR and the other characters mentioned above
            + "$space=[[[:Zs:][:Cc:]]-[$nbsp$break\r]];"

            // dashes: dash punctuation and the discretionary hyphen, except for
            // non-breaking hyphens
            + "$dash=[[[:Pd:]\u00ad]-[$nbsp]];"

            // characters that stick to a word if they precede it: currency symbols
            // (except the cents sign) and starting punctuation
            + "$pre_word=[[[:Sc:]-[\u00a2]][:Ps:][:Pi:]\\\"\\\'];"

            // characters that stick to a word if they follow it: ending punctuation,
            // other punctuation that usually occurs at the end of a sentence,
            // small Kana characters, some CJK diacritics, etc.
            + "$post_word=[[:Pe:][:Pf:]\\!\\\"\\\'\\%\\.\\,\\:\\;\\?\u00a2\u00b0\u066a\u2030-\u2034"
                    + "\u2103\u2105\u2109\u3001\u3002\u3005\u3041\u3043\u3045\u3047\u3049\u3063"
                    + "\u3083\u3085\u3087\u308e\u3099-\u309e\u30a1\u30a3\u30a5\u30a7\u30a9"
                    + "\u30c3\u30e3\u30e5\u30e7\u30ee\u30f5\u30f6\u30fc-\u30fe\uff01\uff0c"
                    + "\uff0e\uff1f];"

            // Kanji: actually includes both Kanji and Kana, except for small Kana and
            // CJK diacritics
            + "$kanji=[[$surr_hi_ideo$pua\u3400-\u4db5\u4e00-\u9fa5\uf900-\ufa6a\u3041-\u3094\u30a1-\u30fa]-[$post_word$_ignore_]];"

            // digits
            + "$digit=[[:Nd:][:No:]];"

            // punctuation that can occur in the middle of a number: periods and commas
            + "$mid_num=[\\.\\,];"

            // everything not mentioned above, plus the quote marks (which are both
            // <pre-word>, <post-word>, and <char>)
            + "$char=[^$break$space$dash$kanji$nbsp$_ignore_$pre_word$post_word$mid_num$danda\r\\\"\\\'];"

            // a "number" is a run of prefix characters and dashes, followed by one or
            // more digits with isolated number-punctuation characters interspersed
            + "$number=([$pre_word$dash]*$digit+($mid_num$digit+)*);"

            // the basic core of a word can be either a "number" as defined above, a single
            // "Kanji" character, or a run of any number of not-explicitly-mentioned
            // characters (this includes Latin letters)
            + "$word_core=([$pre_word$char]*|$kanji|$number);"

            // a word may end with an optional suffix that be either a run of one or
            // more dashes or a run of word-suffix characters, followed by an optional
            // run of whitespace
            + "$word_suffix=(($dash+|$post_word*)$space*);"

            // a word, thus, is an optional run of word-prefix characters, followed by
            // a word core and a word suffix (the syntax of <word-core> and <word-suffix>
            // actually allows either of them to match the empty string, putting a break
            // between things like ")(" or "aaa(aaa"
            + "$word=($pre_word*$word_core$word_suffix);"

            // finally, the rule that does the work: Keep together any run of words that
            // are joined by runs of one of more non-spacing mark.  Also keep a trailing
            // line-break character or CRLF combination with the word.  (line separators
            // "win" over nbsp's)
            + "$word($nbsp+$word)*\r?$break?;"
        },

        // default rules for finding sentence boundaries
        { "SentenceBreakRules",
            // Surrogates.  Until better support is available, ignore low surrogates
            //              and classify high surrogates according to the characters within the block.
              "$surr_lo=[\udc00-\udfff];"

            // ignore non-spacing marks, enclosing marks, and format characters
            + "$_ignore_=[[:Mn:][:Me:][:Cf:]$surr_lo];"

            // lowercase letters
            + "$lc=[:Ll:];"

            // uppercase Latin letters
            + "$ucLatin=[A-Z];"

            // whitespace (line separators are treated as whitespace)
            + "$space=[\t\r\f\n\u2028[:Zs:]];"

            // punctuation which may occur at the beginning of a sentence: "starting
            // punctuation" and quotation marks
            + "$start=[[:Ps:][:Pi:]\\\"\\\'];"

            // punctuation with may occur at the end of a sentence: "ending punctuation"
            // and quotation marks
            + "$end=[[:Pe:][:Pf:]\\\"\\\'];"

            // digits
            + "$digit=[:N:];"

            // characters that unambiguously signal the end of a sentence
            + "$term=[\\!\\?\u3002\uff01\uff1f];"

            // periods, which MAY signal the end of a sentence
            + "$period=[\\.\uff0e];"

            // characters that may occur at the beginning of a sentence: basically anything
            // not mentioned above (lowercase letters and digits are specifically excluded)
            + "$sent_start=[^$lc$ucLatin$space$start$end$digit$term$period\u2029$_ignore_];"

            // Hindi phrase separator
            + "$danda=[\u0964\u0965];"

            // always break sentences after paragraph separators
            + ".*?\u2029?;"

            // always break after a danda, if it's followed by whitespace
            + ".*?$danda$space*;"

            // if you see a period, skip over additional periods and ending punctuation
            // and if the next character is a paragraph separator, break after the
            // paragraph separator
            + ".*?$period[$period$end]*$space*\u2029;"

            // if you see a period, skip over additional periods and ending punctuation,
            // followed by optional whitespace, followed by optional starting punctuation,
            // and if the next character is something that can start a sentence
            // (basically, a capital letter), then put the sentence break between the
            // whitespace and the opening punctuation
            + ".*?$period[$period$end]*$space*/($start*$sent_start|$start+$ucLatin);"

            // same as above, except that there's a sentence break before a Latin capital
            // letter only if there's at least one space after the period
            + ".*?$period[$period$end]*$space+/$ucLatin;"

            // if you see a sentence-terminating character, skip over any additional
            // terminators, periods, or ending punctuation, followed by any whitespace,
            // followed by a SINGLE optional paragraph separator, and put the break there
            + ".*?$term[$term$period$end]*$space*\u2029?;"

            // The following rules are here to aid in backwards iteration.  The automatically
            // generated backwards state table will rewind to the beginning of the
            // paragraph all the time (or all the way to the beginning of the document
            // if the document doesn't use the Unicode PS character) because the only
            // unambiguous character pairs are those involving paragraph separators.
            // These specify a few more unambiguous breaking situations.

            // if you see a sentence-starting character, followed by starting punctuation
            // (remember, we're iterating backwards), followed by an optional run of
            // whitespace, followed by an optional run of ending punctuation, followed
            // by a period, this is a safe place to turn around
            + "![$sent_start$ucLatin]$start*$space+$end*$period;"

            // if you see a letter or a digit, followed by an optional run of
            // starting punctuation, followed by an optional run of whitespace,
            // followed by an optional run of ending punctuation, followed by
            // a sentence terminator, this is a safe place to turn around
            + "![$sent_start$lc$digit]$start*$space*$end*$term;"
        },

        // default rules for finding Title Case boundaries.
        //   See  Unicode Technical Report #21 more information about these rules.
        { "TitleBreakRules",
               "$case_ignorable=[[:Mn:][:Me:][:Cf:][:Lm:][:Sk:]\\u0027\u00AD\u2019];"
             + "$cased=[[[:Lu:][:Lt:][:Ll:]"
             +     "\u2160-\u216f"                       // Other Uppercase
             +     "\u24b6-\u24cf"
             +     "\u02b0-\u02b8"                       // Other Lower case
             +     "\u02c0-\u02c1"
             +     "\u02e0-\u02e4"
             +     "\u0345\u037a"
             +     "\u2170-\u217f"
             +     "\u24d0-\u24e9]"
             +     "-$case_ignorable];"                  // Remove anything that is case_ignorable
                                                         //   from $cased.
             + "$not_cased=[^$cased$case_ignorable];"
             // First time only, eat through any leading non-word-like stuff.
             + "[$not_cased$case_ignorable]*;"
             //  Match a word (a cased item), plus any following spaces or other non-cased junk,
             //    up to the start of the next cased item.
             + "$cased[$cased$case_ignorable]*[$not_cased]*;"
             + "!$not_cased*[$cased$case_ignorable]*$not_cased*;"   // Backwards rule.
        }

    };
}
