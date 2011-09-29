/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.rbnf;

import java.util.Locale;

/**
 * A collection of example rule sets for use with RuleBasedNumberFormat.
 * These examples are intended to serve both as demonstrations of what can
 * be done with this framework, and as starting points for designing new
 * rule sets.
 *
 * For those that claim to represent number-spellout rules for languages
 * other than U.S. English, we make no claims of either accuracy or
 * completeness.  In fact, we know them to be incomplete, and suspect
 * most have mistakes in them.  If you see something that you know is wrong,
 * please tell us!
 *
 * @author Richard Gillam
 */
public class RbnfSampleRuleSets {
    /**
     * Puts a copyright in the .class file
     */
//    private static final String copyrightNotice
//        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    //========================================================================
    // Spellout rules for various languages
    //
    // The following RuleBasedNumberFormat descriptions show the rules for
    // spelling out numeric values in various languages.  As mentioned
    // before, we cannot vouch for the accuracy or completeness of this
    // data, although we believe it's pretty close.  Basically, this
    // represents one day's worth of Web-surfing.  If you can supply the
    // missing information in any of these rule sets, or if you find errors,
    // or if you can supply spellout rules for languages that aren't shown
    // here, we want to hear from you!
    //========================================================================

    /**
     * Spellout rules for U.S. English.  This demonstration version of the
     * U.S. English spellout rules has four variants: 1) %simplified is a
     * set of rules showing the simple method of spelling out numbers in
     * English: 289 is formatted as "two hundred eighty-nine".  2) %alt-teens
     * is the same as %simplified, except that values between 1,000 and 9,999
     * whose hundreds place isn't zero are formatted in hundreds.  For example,
     * 1,983 is formatted as "nineteen hundred eighty-three," and 2,183 is
     * formatted as "twenty-one hundred eighty-three," but 2,083 is still
     * formatted as "two thousand eighty-three."  3) %ordinal formats the
     * values as ordinal numbers in English (e.g., 289 is "two hundred eighty-
     * ninth").  4) %default uses a more complicated algorithm to format
     * numbers in a more natural way: 289 is formatted as "two hundred AND
     * eighty-nine" and commas are inserted between the thousands groups for
     * values above 100,000.
     */
    public static final String usEnglish =
        // This rule set shows the normal simple formatting rules for English
        "%simplified:\n"
               // negative number rule.  This rule is used to format negative
               // numbers.  The result of formatting the number's absolute
               // value is placed where the >> is.
        + "    -x: minus >>;\n"
               // faction rule.  This rule is used for formatting numbers
               // with fractional parts.  The result of formatting the
               // number's integral part is substituted for the <<, and
               // the result of formatting the number's fractional part
               // (one digit at a time, e.g., 0.123 is "zero point one two
               // three") replaces the >>.
        + "    x.x: << point >>;\n"
               // the rules for the values from 0 to 19 are simply the
               // words for those numbers
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
               // beginning at 20, we use the >> to mark the position where
               // the result of formatting the number's ones digit.  Thus,
               // we only need a new rule at every multiple of 10.  Text in
               // backets is omitted if the value being formatted is an
               // even multiple of 10.
        + "    20: twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
               // beginning at 100, we can use << to mark the position where
               // the result of formatting the multiple of 100 is to be
               // inserted.  Notice also that the meaning of >> has shifted:
               // here, it refers to both the ones place and the tens place.
               // The meanings of the << and >> tokens depend on the base value
               // of the rule.  A rule's divisor is (usually) the highest
               // power of 10 that is less than or equal to the rule's base
               // value.  The value being formatted is divided by the rule's
               // divisor, and the integral quotient is used to get the text
               // for <<, while the remainder is used to produce the text
               // for >>.  Again, text in brackets is omitted if the value
               // being formatted is an even multiple of the rule's divisor
               // (in this case, an even multiple of 100)
        + "    100: << hundred[ >>];\n"
               // The rules for the higher numbers work the same way as the
               // rule for 100: Again, the << and >> tokens depend on the
               // rule's divisor, which for all these rules is also the rule's
               // base value.  To group by thousand, we simply don't have any
               // rules between 1,000 and 1,000,000.
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000: << trillion[ >>];\n"
               // overflow rule.  This rule specifies that values of a
               // quadrillion or more are shown in numerals rather than words.
               // The == token means to format (with new rules) the value
               // being formatted by this rule and place the result where
               // the == is.  The #,##0 inside the == signs is a
               // DecimalFormat pattern.  It specifies that the value should
               // be formatted with a DecimalFormat object, and that it
               // should be formatted with no decimal places, at least one
               // digit, and a thousands separator.
        + "    1,000,000,000,000,000: =#,##0=;\n"

        // This rule set formats numbers between 1,000 and 9,999 somewhat
        // differently: If the hundreds digit is not zero, the first two
        // digits are treated as a number of hundreds.  For example, 2,197
        // would come out as "twenty-one hundred ninety-seven."
        + "%alt-teens:\n"
               // just use %simplified to format values below 1,000
        + "    =%simplified=;\n"
               // values between 1,000 and 9,999 are delegated to %%alt-hundreds
               // for formatting.  The > after "1000" decreases the exponent
               // of the rule's radix by one, causing the rule's divisor
               // to be 100 instead of 1,000.  This causes the first TWO
               // digits of the number, instead of just the first digit,
               // to be sent to %%alt-hundreds
        + "    1000>: <%%alt-hundreds<[ >>];\n"
               // for values of 10,000 and more, we again just use %simplified
        + "    10,000: =%simplified=;\n"
        // This rule set uses some obscure voodoo of the description language
        // to format the first two digits of a value in the thousands.
        // The rule at 10 formats the first two digits as a multiple of 1,000
        // and the rule at 11 formats the first two digits as a multiple of
        // 100.  This works because of something known as the "rollback rule":
        // if the rule applicable to the value being formatted has two
        // substitutions, the value being formatted is an even multiple of
        // the rule's divisor, and the rule's base value ISN'T an even multiple
        // if the rule's divisor, then the rule that precedes this one in the
        // list is used instead.  (The [] notation is implemented internally
        // using this notation: a rule containing [] is split into two rules,
        // and the right one is chosen using the rollback rule.) In this case,
        // it means that if the first two digits are an even multiple of 10,
        // they're formatted with the 10 rule (containing "thousand"), and if
        // they're not, they're formatted with the 11 rule (containing
        // "hundred").  %%empty is a hack to cause the rollback rule to be
        // invoked: it makes the 11 rule have two substitutions, even though
        // the second substitution (calling %%empty) doesn't actually do
        // anything.
        + "%%alt-hundreds:\n"
        + "    0: SHOULD NEVER GET HERE!;\n"
        + "    10: <%simplified< thousand;\n"
        + "    11: =%simplified= hundred>%%empty>;\n"
        + "%%empty:\n"
        + "    0:;"

        // this rule set is the same as %simplified, except that it formats
        // the value as an ordinal number: 234 is formatted as "two hundred
        // thirty-fourth".  Notice the calls to ^simplified: we have to
        // call %simplified to avoid getting "second hundred thirty-fourth."
        + "%ordinal:\n"
        + "    zeroth; first; second; third; fourth; fifth; sixth; seventh;\n"
        + "        eighth; ninth;\n"
        + "    tenth; eleventh; twelfth; thirteenth; fourteenth;\n"
        + "        fifteenth; sixteenth; seventeenth; eighteenth;\n"
        + "        nineteenth;\n"
        + "    twentieth; twenty->>;\n"
        + "    30: thirtieth; thirty->>;\n"
        + "    40: fortieth; forty->>;\n"
        + "    50: fiftieth; fifty->>;\n"
        + "    60: sixtieth; sixty->>;\n"
        + "    70: seventieth; seventy->>;\n"
        + "    80: eightieth; eighty->>;\n"
        + "    90: ninetieth; ninety->>;\n"
        + "    100: <%simplified< hundredth; <%simplified< hundred >>;\n"
        + "    1000: <%simplified< thousandth; <%simplified< thousand >>;\n"
        + "    1,000,000: <%simplified< millionth; <%simplified< million >>;\n"
        + "    1,000,000,000: <%simplified< billionth;\n"
        + "        <%simplified< billion >>;\n"
        + "    1,000,000,000,000: <%simplified< trillionth;\n"
        + "        <%simplified< trillion >>;\n"
        + "    1,000,000,000,000,000: =#,##0=;"

        // %default is a more elaborate form of %simplified;  It is basically
        // the same, except that it introduces "and" before the ones digit
        // when appropriate (basically, between the tens and ones digits) and
        // separates the thousands groups with commas in values over 100,000.
        + "%default:\n"
               // negative-number and fraction rules.  These are the same
               // as those for %simplified, but ave to be stated here too
               // because this is an entry point
        + "    -x: minus >>;\n"
        + "    x.x: << point >>;\n"
               // just use %simplified for values below 100
        + "    =%simplified=;\n"
               // for values from 100 to 9,999 use %%and to decide whether or
               // not to interpose the "and"
        + "    100: << hundred[ >%%and>];\n"
        + "    1000: << thousand[ >%%and>];\n"
               // for values of 100,000 and up, use %%commas to interpose the
               // commas in the right places (and also to interpose the "and")
        + "    100,000>>: << thousand[>%%commas>];\n"
        + "    1,000,000: << million[>%%commas>];\n"
        + "    1,000,000,000: << billion[>%%commas>];\n"
        + "    1,000,000,000,000: << trillion[>%%commas>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        // if the value passed to this rule set is greater than 100, don't
        // add the "and"; if it's less than 100, add "and" before the last
        // digits
        + "%%and:\n"
        + "    and =%default=;\n"
        + "    100: =%default=;\n"
        // this rule set is used to place the commas
        + "%%commas:\n"
               // for values below 100, add "and" (the apostrophe at the
               // beginning is ignored, but causes the space that follows it
               // to be significant: this is necessary because the rules
               // calling %%commas don't put a space before it)
        + "    ' and =%default=;\n"
               // put a comma after the thousands (or whatever preceded the
               // hundreds)
        + "    100: , =%default=;\n"
               // put a comma after the millions (or whatever precedes the
               // thousands)
        + "    1000: , <%default< thousand, >%default>;\n"
               // and so on...
        + "    1,000,000: , =%default=;"
        // %%lenient-parse isn't really a set of number formatting rules;
        // it's a set of collation rules.  Lenient-parse mode uses a Collator
        // object to compare fragments of the text being parsed to the text
        // in the rules, allowing more leeway in the matching text.  This set
        // of rules tells the formatter to ignore commas when parsing (it
        // already ignores spaces, which is why we refer to the space; it also
        // ignores hyphens, making "twenty one" and "twenty-one" parse
        // identically)
        + "%%lenient-parse:\n"
        + "    & ' ' , ',' ;\n";

    /**
     * Spellout rules for U.K. English.  U.K. English has one significant
     * difference from U.S. English: the names for values of 1,000,000,000
     * and higher.  In American English, each successive "-illion" is 1,000
     * times greater than the preceding one: 1,000,000,000 is "one billion"
     * and 1,000,000,000,000 is "one trillion."  In British English, each
     * successive "-illion" is one million times greater than the one before:
     * "one billion" is 1,000,000,000,000 (or what Americans would call a
     * "trillion"), and "one trillion" is 1,000,000,000,000,000,000.
     * 1,000,000,000 in British English is "one thousand million."  (This
     * value is sometimes called a "milliard," but this word seems to have
     * fallen into disuse.)
     */
    public static final String ukEnglish =
        "%simplified:\n"
        + "    -x: minus >>;\n"
        + "    x.x: << point >>;\n"
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
        + "    20: twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
        + "    100: << hundred[ >>];\n"
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%alt-teens:\n"
        + "    =%simplified=;\n"
        + "    1000>: <%%alt-hundreds<[ >>];\n"
        + "    10,000: =%simplified=;\n"
        + "    1,000,000: << million[ >%simplified>];\n"
        + "    1,000,000,000,000: << billion[ >%simplified>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%%alt-hundreds:\n"
        + "    0: SHOULD NEVER GET HERE!;\n"
        + "    10: <%simplified< thousand;\n"
        + "    11: =%simplified= hundred>%%empty>;\n"
        + "%%empty:\n"
        + "    0:;"
        + "%ordinal:\n"
        + "    zeroth; first; second; third; fourth; fifth; sixth; seventh;\n"
        + "        eighth; ninth;\n"
        + "    tenth; eleventh; twelfth; thirteenth; fourteenth;\n"
        + "        fifteenth; sixteenth; seventeenth; eighteenth;\n"
        + "        nineteenth;\n"
        + "    twentieth; twenty->>;\n"
        + "    30: thirtieth; thirty->>;\n"
        + "    40: fortieth; forty->>;\n"
        + "    50: fiftieth; fifty->>;\n"
        + "    60: sixtieth; sixty->>;\n"
        + "    70: seventieth; seventy->>;\n"
        + "    80: eightieth; eighty->>;\n"
        + "    90: ninetieth; ninety->>;\n"
        + "    100: <%simplified< hundredth; <%simplified< hundred >>;\n"
        + "    1000: <%simplified< thousandth; <%simplified< thousand >>;\n"
        + "    1,000,000: <%simplified< millionth; <%simplified< million >>;\n"
        + "    1,000,000,000,000: <%simplified< billionth;\n"
        + "        <%simplified< billion >>;\n"
        + "    1,000,000,000,000,000: =#,##0=;"
        + "%default:\n"
        + "    -x: minus >>;\n"
        + "    x.x: << point >>;\n"
        + "    =%simplified=;\n"
        + "    100: << hundred[ >%%and>];\n"
        + "    1000: << thousand[ >%%and>];\n"
        + "    100,000>>: << thousand[>%%commas>];\n"
        + "    1,000,000: << million[>%%commas>];\n"
        + "    1,000,000,000,000: << billion[>%%commas>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%%and:\n"
        + "    and =%default=;\n"
        + "    100: =%default=;\n"
        + "%%commas:\n"
        + "    ' and =%default=;\n"
        + "    100: , =%default=;\n"
        + "    1000: , <%default< thousand, >%default>;\n"
        + "    1,000,000: , =%default=;"
        + "%%lenient-parse:\n"
        + "    & ' ' , ',' ;\n";
    // Could someone please correct me if I'm wrong about "milliard" falling
    // into disuse, or have missed any other details of how large numbers
    // are rendered.  Also, could someone please provide me with information
    // on which other English-speaking countries use which system?  Right now,
    // I'm assuming that the U.S. system is used in Canada and that all the
    // other English-speaking countries follow the British system.  Can
    // someone out there confirm this?

    /**
     * Spellout rules for Spanish.  The Spanish rules are quite similar to
     * the English rules, but there are some important differences:
     * First, we have to provide separate rules for most of the twenties
     * because the ones digit frequently picks up an accent mark that it
     * doesn't have when standing alone.  Second, each multiple of 100 has
     * to be specified separately because the multiplier on 100 very often
     * changes form in the contraction: 500 is "quinientos," not
     * "cincocientos."  In addition, the word for 100 is "cien" when
     * standing alone, but changes to "ciento" when followed by more digits.
     * There also some other differences.
     */
    public static final String spanish =
        // negative-number and fraction rules
        "-x: menos >>;\n"
        + "x.x: << punto >>;\n"
        // words for values from 0 to 19
        + "cero; uno; dos; tres; cuatro; cinco; seis; siete; ocho; nueve;\n"
        + "diez; once; doce; trece; catorce; quince; diecis\u00e9is;\n"
        + "    diecisiete; dieciocho; diecinueve;\n"
        // words for values from 20 to 29 (necessary because the ones digit
        // often picks up an accent mark it doesn't have when standing alone)
        + "veinte; veintiuno; veintid\u00f3s; veintitr\u00e9s; veinticuatro;\n"
        + "    veinticinco; veintis\u00e9is; veintisiete; veintiocho;\n"
        + "    veintinueve;\n"
        // words for multiples of 10 (notice that the tens digit is separated
        // from the ones digit by the word "y".)
        + "30: treinta[ y >>];\n"
        + "40: cuarenta[ y >>];\n"
        + "50: cincuenta[ y >>];\n"
        + "60: sesenta[ y >>];\n"
        + "70: setenta[ y >>];\n"
        + "80: ochenta[ y >>];\n"
        + "90: noventa[ y >>];\n"
        // 100 by itself is "cien," but 100 followed by something is "cineto"
        + "100: cien;\n"
        + "101: ciento >>;\n"
        // words for multiples of 100 (must be stated because they're
        // rarely simple concatenations)
        + "200: doscientos[ >>];\n"
        + "300: trescientos[ >>];\n"
        + "400: cuatrocientos[ >>];\n"
        + "500: quinientos[ >>];\n"
        + "600: seiscientos[ >>];\n"
        + "700: setecientos[ >>];\n"
        + "800: ochocientos[ >>];\n"
        + "900: novecientos[ >>];\n"
        // for 1,000, the multiplier on "mil" is omitted: 2,000 is "dos mil,"
        // but 1,000 is just "mil."
        + "1000: mil[ >>];\n"
        + "2000: << mil[ >>];\n"
        // 1,000,000 is "un millon," not "uno millon"
        + "1,000,000: un mill\u00f3n[ >>];\n"
        + "2,000,000: << mill\u00f3n[ >>];\n"
        // overflow rule
        + "1,000,000,000: =#,##0= (incomplete data);";
    // The Spanish rules are incomplete.  I'm missing information on negative
    // numbers and numbers with fractional parts.  I also don't have
    // information on numbers higher than the millions

    /**
     * Spellout rules for French.  French adds some interesting quirks of its
     * own: 1) The word "et" is interposed between the tens and ones digits,
     * but only if the ones digit if 1: 20 is "vingt," and 2 is "vingt-deux,"
     * but 21 is "vingt-et-un."  2)  There are no words for 70, 80, or 90.
     * "quatre-vingts" ("four twenties") is used for 80, and values proceed
     * by score from 60 to 99 (e.g., 73 is "soixante-treize" ["sixty-thirteen"]).
     * Numbers from 1,100 to 1,199 are rendered as hundreds rather than
     * thousands: 1,100 is "onze cents" ("eleven hundred"), rather than
     * "mille cent" ("one thousand one hundred")
     */
    public static final String french =
        // the main rule set
        "%main:\n"
               // negative-number and fraction rules
        + "    -x: moins >>;\n"
        + "    x.x: << virgule >>;\n"
               // words for numbers from 0 to 10
        + "    z\u00e9ro; un; deux; trois; quatre; cinq; six; sept; huit; neuf;\n"
        + "    dix; onze; douze; treize; quatorze; quinze; seize;\n"
        + "        dix-sept; dix-huit; dix-neuf;\n"
               // ords for the multiples of 10: %%alt-ones inserts "et"
               // when needed
        + "    20: vingt[->%%alt-ones>];\n"
        + "    30: trente[->%%alt-ones>];\n"
        + "    40: quarante[->%%alt-ones>];\n"
        + "    50: cinquante[->%%alt-ones>];\n"
               // rule for 60.  The /20 causes this rule's multiplier to be
               // 20 rather than 10, allowinhg us to recurse for all values
               // from 60 to 79...
        + "    60/20: soixante[->%%alt-ones>];\n"
               // ...except for 71, which must be special-cased
        + "    71: soixante et onze;\n"
               // at 72, we have to repeat the rule for 60 to get us to 79
        + "    72/20: soixante->%%alt-ones>;\n"
               // at 80, we state a new rule with the phrase for 80.  Since
               // it changes form when there's a ones digit, we need a second
               // rule at 81.  This rule also includes "/20," allowing it to
               // be used correctly for all values up to 99
        + "    80: quatre-vingts; 81/20: quatre-vingt->>;\n"
               // "cent" becomes plural when preceded by a multiplier, and
               // the multiplier is omitted from the singular form
        + "    100: cent[ >>];\n"
        + "    200: << cents[ >>];\n"
        + "    1000: mille[ >>];\n"
               // values from 1,100 to 1,199 are rendered as "onze cents..."
               // instead of "mille cent..."  The > after "1000" decreases
               // the rule's exponent, causing its multiplier to be 100 instead
               // of 1,000.  This prevents us from getting "onze cents cent
               // vingt-deux" ("eleven hundred one hundred twenty-two").
        + "    1100>: onze cents[ >>];\n"
               // at 1,200, we go back to formating in thousands, so we
               // repeat the rule for 1,000
        + "    1200: mille >>;\n"
               // at 2,000, the multiplier is added
        + "    2000: << mille[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << milliarde[ >>];\n"
        + "    1,000,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        // %%alt-ones is used to insert "et" when the ones digit is 1
        + "%%alt-ones:\n"
        + "    ; et-un; =%main=;";

    /**
     * Spellout rules for Swiss French.  Swiss French differs from French French
     * in that it does have words for 70, 80, and 90.  This rule set shows them,
     * and is simpler as a result.
     */
    public static final String swissFrench =
        "%main:\n"
        + "    -x: moins >>;\n"
        + "    x.x: << virgule >>;\n"
        + "    z\u00e9ro; un; deux; trois; quatre; cinq; six; sept; huit; neuf;\n"
        + "    dix; onze; douze; treize; quatorze; quinze; seize;\n"
        + "        dix-sept; dix-huit; dix-neuf;\n"
        + "    20: vingt[->%%alt-ones>];\n"
        + "    30: trente[->%%alt-ones>];\n"
        + "    40: quarante[->%%alt-ones>];\n"
        + "    50: cinquante[->%%alt-ones>];\n"
        + "    60: soixante[->%%alt-ones>];\n"
               // notice new words for 70, 80, and 90
        + "    70: septante[->%%alt-ones>];\n"
        + "    80: octante[->%%alt-ones>];\n"
        + "    90: nonante[->%%alt-ones>];\n"
        + "    100: cent[ >>];\n"
        + "    200: << cents[ >>];\n"
        + "    1000: mille[ >>];\n"
        + "    1100>: onze cents[ >>];\n"
        + "    1200: mille >>;\n"
        + "    2000: << mille[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << milliarde[ >>];\n"
        + "    1,000,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%%alt-ones:\n"
        + "    ; et-un; =%main=;";
    // I'm not 100% sure about Swiss French.  Is
    // this correct?  Is "onze cents" commonly used for 1,100 in both France
    // and Switzerland?  Can someone fill me in on the rules for the other
    // French-speaking countries?  I've heard conflicting opinions on which
    // version is used in Canada, and I understand there's an alternate set
    // of words for 70, 80, and 90 that is used somewhere, but I don't know
    // what those words are or where they're used.

    /**
     * Spellout rules for German.  German also adds some interesting
     * characteristics.  For values below 1,000,000, numbers are customarily
     * written out as a single word.  And the ones digit PRECEDES the tens
     * digit (e.g., 23 is "dreiundzwanzig," not "zwanzigunddrei").
     */
    public static final String german =
        // 1 is "eins" when by itself, but turns into "ein" in most
        // combinations
        "%alt-ones:\n"
        + "    null; eins; =%%main=;\n"
        + "%%main:\n"
               // words for numbers from 0 to 12.  Notice that the values
               // from 13 to 19 can derived algorithmically, unlike in most
               // other languages
        + "    null; ein; zwei; drei; vier; f\u00fcnf; sechs; sieben; acht; neun;\n"
        + "    zehn; elf; zw\u00f6lf; >>zehn;\n"
               // rules for the multiples of 10.  Notice that the ones digit
               // goes on the front
        + "    20: [>>und]zwanzig;\n"
        + "    30: [>>und]drei\u00dfig;\n"
        + "    40: [>>und]vierzig;\n"
        + "    50: [>>und]f\u00fcnfzig;\n"
        + "    60: [>>und]sechzig;\n"
        + "    70: [>>und]siebzig;\n"
        + "    80: [>>und]achtzig;\n"
        + "    90: [>>und]neunzig;\n"
        + "    100: hundert[>%alt-ones>];\n"
        + "    200: <<hundert[>%alt-ones>];\n"
        + "    1000: tausend[>%alt-ones>];\n"
        + "    2000: <<tausend[>%alt-ones>];\n"
        + "    1,000,000: eine Million[ >%alt-ones>];\n"
        + "    2,000,000: << Millionen[ >%alt-ones>];\n"
        + "    1,000,000,000: eine Milliarde[ >%alt-ones>];\n"
        + "    2,000,000,000: << Milliarden[ >%alt-ones>];\n"
        + "    1,000,000,000,000: eine Billion[ >%alt-ones>];\n"
        + "    2,000,000,000,000: << Billionen[ >%alt-ones>];\n"
        + "    1,000,000,000,000,000: =#,##0=;";
    // again, I'm not 100% sure of these rules.  I think both "hundert" and
    // "einhundert" are correct or 100, but I'm not sure which is preferable
    // in situations where this framework is likely to be used.  Also, is it
    // really true that numbers are run together into compound words all the
    // time?  And again, I'm missing information on negative numbers and
    // decimals.

    /**
     * Spellout rules for Italian.  Like German, most Italian numbers are
     * written as single words.  What makes these rules complicated is the rule
     * that says that when a word ending in a vowel and a word beginning with
     * a vowel are combined into a compound, the vowel is dropped from the
     * end of the first word: 180 is "centottanta," not "centoottanta."
     * The complexity of this rule set is to produce this behavior.
     */
    public static final String italian =
        // main rule set.  Follows the patterns of the preceding rule sets,
        // except that the final vowel is omitted from words ending in
        // vowels when they are followed by another word; instead, we have
        // separate rule sets that are identical to this one, except that
        // all the words that don't begin with a vowel have a vowel tacked
        // onto them at the front.  A word ending in a vowel calls a
        // substitution that will supply that vowel, unless that vowel is to
        // be elided.
        "%main:\n"
        + "    -x: meno >>;\n"
        + "    x.x: << virgola >>;\n"
        + "    zero; uno; due; tre; quattro; cinque; sei; sette; otto;\n"
        + "        nove;\n"
        + "    dieci; undici; dodici; tredici; quattordici; quindici; sedici;\n"
        + "        diciasette; diciotto; diciannove;\n"
        + "    20: venti; vent>%%with-i>;\n"
        + "    30: trenta; trent>%%with-i>;\n"
        + "    40: quaranta; quarant>%%with-a>;\n"
        + "    50: cinquanta; cinquant>%%with-a>;\n"
        + "    60: sessanta; sessant>%%with-a>;\n"
        + "    70: settanta; settant>%%with-a>;\n"
        + "    80: ottanta; ottant>%%with-a>;\n"
        + "    90: novanta; novant>%%with-a>;\n"
        + "    100: cento; cent[>%%with-o>];\n"
        + "    200: <<cento; <<cent[>%%with-o>];\n"
        + "    1000: mille; mill[>%%with-i>];\n"
        + "    2000: <<mila; <<mil[>%%with-a>];\n"
        + "    100,000>>: <<mila[ >>];\n"
        + "    1,000,000: =#,##0= (incomplete data);\n"
        + "%%with-a:\n"
        + "    azero; uno; adue; atre; aquattro; acinque; asei; asette; otto;\n"
        + "        anove;\n"
        + "    adieci; undici; adodici; atredici; aquattordici; aquindici; asedici;\n"
        + "        adiciasette; adiciotto; adiciannove;\n"
        + "    20: aventi; avent>%%with-i>;\n"
        + "    30: atrenta; atrent>%%with-i>;\n"
        + "    40: aquaranta; aquarant>%%with-a>;\n"
        + "    50: acinquanta; acinquant>%%with-a>;\n"
        + "    60: asessanta; asessant>%%with-a>;\n"
        + "    70: asettanta; asettant>%%with-a>;\n"
        + "    80: ottanta; ottant>%%with-a>;\n"
        + "    90: anovanta; anovant>%%with-a>;\n"
        + "    100: acento; acent[>%%with-o>];\n"
        + "    200: <%%with-a<cento; <%%with-a<cent[>%%with-o>];\n"
        + "    1000: amille; amill[>%%with-i>];\n"
        + "    2000: <%%with-a<mila; <%%with-a<mil[>%%with-a>];\n"
        + "    100,000: =%main=;\n"
        + "%%with-i:\n"
        + "    izero; uno; idue; itre; iquattro; icinque; isei; isette; otto;\n"
        + "        inove;\n"
        + "    idieci; undici; idodici; itredici; iquattordici; iquindici; isedici;\n"
        + "        idiciasette; idiciotto; idiciannove;\n"
        + "    20: iventi; ivent>%%with-i>;\n"
        + "    30: itrenta; itrent>%%with-i>;\n"
        + "    40: iquaranta; iquarant>%%with-a>;\n"
        + "    50: icinquanta; icinquant>%%with-a>;\n"
        + "    60: isessanta; isessant>%%with-a>;\n"
        + "    70: isettanta; isettant>%%with-a>;\n"
        + "    80: ottanta; ottant>%%with-a>;\n"
        + "    90: inovanta; inovant>%%with-a>;\n"
        + "    100: icento; icent[>%%with-o>];\n"
        + "    200: <%%with-i<cento; <%%with-i<cent[>%%with-o>];\n"
        + "    1000: imille; imill[>%%with-i>];\n"
        + "    2000: <%%with-i<mila; <%%with-i<mil[>%%with-a>];\n"
        + "    100,000: =%main=;\n"
        + "%%with-o:\n"
        + "    ozero; uno; odue; otre; oquattro; ocinque; osei; osette; otto;\n"
        + "        onove;\n"
        + "    odieci; undici; ododici; otredici; oquattordici; oquindici; osedici;\n"
        + "        odiciasette; odiciotto; odiciannove;\n"
        + "    20: oventi; ovent>%%with-i>;\n"
        + "    30: otrenta; otrent>%%with-i>;\n"
        + "    40: oquaranta; oquarant>%%with-a>;\n"
        + "    50: ocinquanta; ocinquant>%%with-a>;\n"
        + "    60: osessanta; osessant>%%with-a>;\n"
        + "    70: osettanta; osettant>%%with-a>;\n"
        + "    80: ottanta; ottant>%%with-a>;\n"
        + "    90: onovanta; onovant>%%with-a>;\n"
        + "    100: ocento; ocent[>%%with-o>];\n"
        + "    200: <%%with-o<cento; <%%with-o<cent[>%%with-o>];\n"
        + "    1000: omille; omill[>%%with-i>];\n"
        + "    2000: <%%with-o<mila; <%%with-o<mil[>%%with-a>];\n"
        + "    100,000: =%main=;\n";
    // Can someone confirm that I did the vowel-eliding thing right?  I'm
    // not 100% sure I'm doing it in all the right places, or completely
    // correctly.  Also, I don't have information for negatives and decimals,
    // and I lack words fror values from 1,000,000 on up.

    /**
     * Spellout rules for Swedish.
     */
    public static final String swedish =
        "noll; ett; tv\u00e5; tre; fyra; fem; sex; sjo; \u00e5tta; nio;\n"
        + "tio; elva; tolv; tretton; fjorton; femton; sexton; sjutton; arton; nitton;\n"
        + "20: tjugo[>>];\n"
        + "30: trettio[>>];\n"
        + "40: fyrtio[>>];\n"
        + "50: femtio[>>];\n"
        + "60: sextio[>>];\n"
        + "70: sjuttio[>>];\n"
        + "80: \u00e5ttio[>>];\n"
        + "90: nittio[>>];\n"
        + "100: hundra[>>];\n"
        + "200: <<hundra[>>];\n"
        + "1000: tusen[ >>];\n"
        + "2000: << tusen[ >>];\n"
        + "1,000,000: en miljon[ >>];\n"
        + "2,000,000: << miljon[ >>];\n"
        + "1,000,000,000: en miljard[ >>];\n"
        + "2,000,000,000: << miljard[ >>];\n"
        + "1,000,000,000,000: en biljon[ >>];\n"
        + "2,000,000,000,000: << biljon[ >>];\n"
        + "1,000,000,000,000,000: =#,##0=";
    // can someone supply me with information on negatives and decimals?

    /**
     * Spellout rules for Dutch.  Notice that in Dutch, as in German,
     * the ones digit precedes the tens digit.
     */
    public static final String dutch =
        " -x: min >>;\n"
        + "x.x: << komma >>;\n"
        + "(zero?); een; twee; drie; vier; vijf; zes; zeven; acht; negen;\n"
        + "tien; elf; twaalf; dertien; veertien; vijftien; zestien;\n"
        + "zeventien; achtien; negentien;\n"
        + "20: [>> en ]twintig;\n"
        + "30: [>> en ]dertig;\n"
        + "40: [>> en ]veertig;\n"
        + "50: [>> en ]vijftig;\n"
        + "60: [>> en ]zestig;\n"
        + "70: [>> en ]zeventig;\n"
        + "80: [>> en ]tachtig;\n"
        + "90: [>> en ]negentig;\n"
        + "100: << honderd[ >>];\n"
        + "1000: << duizend[ >>];\n"
        + "1,000,000: << miljoen[ >>];\n"
        + "1,000,000,000: << biljoen[ >>];\n"
        + "1,000,000,000,000: =#,##0=";

    /**
     * Spellout rules for Japanese.  In Japanese, there really isn't any
     * distinction between a number written out in digits and a number
     * written out in words: the ideographic characters are both digits
     * and words.  This rule set provides two variants:  %traditional
     * uses the traditional CJK numerals (which are also used in China
     * and Korea).  %financial uses alternate ideographs for many numbers
     * that are harder to alter than the traditional numerals (one could
     * fairly easily change a one to
     * a three just by adding two strokes, for example).  This is also done in
     * the other countries using Chinese idographs, but different ideographs
     * are used in those places.
     */
    public static final String japanese =
        "%financial:\n"
        + "    \u96f6; \u58f1; \u5f10; \u53c2; \u56db; \u4f0d; \u516d; \u4e03; \u516b; \u4e5d;\n"
        + "    \u62fe[>>];\n"
        + "    20: <<\u62fe[>>];\n"
        + "    100: <<\u767e[>>];\n"
        + "    1000: <<\u5343[>>];\n"
        + "    10,000: <<\u4e07[>>];\n"
        + "    100,000,000: <<\u5104[>>];\n"
        + "    1,000,000,000,000: <<\u5146[>>];\n"
        + "    10,000,000,000,000,000: =#,##0=;\n"
        + "%traditional:\n"
        + "    \u96f6; \u4e00; \u4e8c; \u4e09; \u56db; \u4e94; \u516d; \u4e03; \u516b; \u4e5d;\n"
        + "    \u5341[>>];\n"
        + "    20: <<\u5341[>>];\n"
        + "    100: <<\u767e[>>];\n"
        + "    1000: <<\u5343[>>];\n"
        + "    10,000: <<\u4e07[>>];\n"
        + "    100,000,000: <<\u5104[>>];\n"
        + "    1,000,000,000,000: <<\u5146[>>];\n"
        + "    10,000,000,000,000,000: =#,##0=;";
    // Can someone supply me with the right fraud-proof ideographs for
    // Simplified and Traditional Chinese, and for Korean?  Can someone
    // supply me with information on negatives and decimals?

    /**
     * Spellout rules for Greek.  Again in Greek we have to supply the words
     * for the multiples of 100 because they can't be derived algorithmically.
     * Also, the tens dgit changes form when followed by a ones digit: an
     * accent mark disappears from the tens digit and moves to the ones digit.
     * Therefore, instead of using the [] notation, we actually have to use
     * two separate rules for each multiple of 10 to show the two forms of
     * the word.
     */
    public static final String greek =
        "zero (incomplete data); \u03ad\u03bd\u03b1; \u03b4\u03cd\u03bf; \u03b4\u03c1\u03af\u03b1; "
        + "\u03c4\u03ad\u03c3\u03c3\u03b5\u03c1\u03b1; \u03c0\u03ad\u03bd\u03c4\u03b5; "
        + "\u03ad\u03be\u03b9; \u03b5\u03c0\u03c4\u03ac; \u03bf\u03ba\u03c4\u03ce; "
        + "\u03b5\u03bd\u03bd\u03ad\u03b1;\n"
        + "10: \u03b4\u03ad\u03ba\u03b1; "
        + "\u03ad\u03bd\u03b4\u03b5\u03ba\u03b1; \u03b4\u03ce\u03b4\u03b5\u03ba\u03b1; "
        + "\u03b4\u03b5\u03ba\u03b1>>;\n"
        + "20: \u03b5\u03af\u03ba\u03bf\u03c3\u03b9; \u03b5\u03b9\u03ba\u03bf\u03c3\u03b9>>;\n"
        + "30: \u03c4\u03c1\u03b9\u03ac\u03bd\u03c4\u03b1; \u03c4\u03c1\u03b9\u03b1\u03bd\u03c4\u03b1>>;\n"
        + "40: \u03c3\u03b1\u03c1\u03ac\u03bd\u03c4\u03b1; \u03c3\u03b1\u03c1\u03b1\u03bd\u03c4\u03b1>>;\n"
        + "50: \u03c0\u03b5\u03bd\u03ae\u03bd\u03c4\u03b1; \u03c0\u03b5\u03bd\u03b7\u03bd\u03c4\u03b1>>;\n"
        + "60: \u03b5\u03be\u03ae\u03bd\u03c4\u03b1; \u03b5\u03be\u03b7\u03bd\u03c4\u03b1>>;\n"
        + "70: \u03b5\u03b2\u03b4\u03bf\u03bc\u03ae\u03bd\u03c4\u03b1; "
        + "\u03b5\u03b2\u03b4\u03bf\u03bc\u03b7\u03bd\u03c4\u03b1>>;\n"
        + "80: \u03bf\u03b3\u03b4\u03cc\u03bd\u03c4\u03b1; \u03bf\u03b3\u03b4\u03bf\u03bd\u03c4\u03b1>>;\n"
        + "90: \u03b5\u03bd\u03bd\u03b5\u03bd\u03ae\u03bd\u03c4\u03b1; "
        + "\u03b5\u03bd\u03bd\u03b5\u03bd\u03b7\u03bd\u03c4\u03b1>>;\n"
        + "100: \u03b5\u03ba\u03b1\u03c4\u03cc[\u03bd >>];\n"
        + "200: \u03b4\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "300: \u03c4\u03c1\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "400: \u03c4\u03b5\u03c4\u03c1\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "500: \u03c0\u03b5\u03bd\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "600: \u03b5\u03be\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "700: \u03b5\u03c0\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "800: \u03bf\u03ba\u03c4\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "900: \u03b5\u03bd\u03bd\u03b9\u03b1\u03ba\u03cc\u03c3\u03b9\u03b1[ >>];\n"
        + "1000: \u03c7\u03af\u03bb\u03b9\u03b1[ >>];\n"
        + "2000: << \u03c7\u03af\u03bb\u03b9\u03b1[ >>];\n"
        + "1,000,000: << \u03b5\u03ba\u03b1\u03c4\u03bf\u03bc\u03bc\u03b9\u03cc\u03c1\u03b9\u03bf[ >>];\n"
        + "1,000,000,000: << \u03b4\u03b9\u03c3\u03b5\u03ba\u03b1\u03c4\u03bf\u03bc\u03bc\u03b9\u03cc\u03c1\u03b9\u03bf[ >>];\n"
        + "1,000,000,000,000: =#,##0=";
    // Can someone supply me with information on negatives and decimals?
    // I'm also missing the word for zero.  Can someone clue me in?

    /**
     * Spellout rules for Russian.
     */
    public static final String russian =
        "\u043d\u043e\u043b\u044c; \u043e\u0434\u0438\u043d; \u0434\u0432\u0430; \u0442\u0440\u0438; "
        + "\u0447\u0435\u0442\u044b\u0440\u0435; \u043f\u044f\u0442; \u0448\u0435\u0441\u0442; "
        + "\u0441\u0435\u043c\u044c; \u0432\u043e\u0441\u0435\u043c\u044c; \u0434\u0435\u0432\u044f\u0442;\n"
        + "10: \u0434\u0435\u0441\u044f\u0442; "
        + "\u043e\u0434\u0438\u043d\u043d\u0430\u0434\u0446\u0430\u0442\u044c;\n"
        + "\u0434\u0432\u0435\u043d\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0442\u0440\u0438\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0447\u0435\u0442\u044b\u0440\u043d\u0430\u0434\u0446\u0430\u0442\u044c;\n"
        + "15: \u043f\u044f\u0442\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0448\u0435\u0441\u0442\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0441\u0435\u043c\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0432\u043e\u0441\u0435\u043c\u043d\u0430\u0434\u0446\u0430\u0442\u044c; "
        + "\u0434\u0435\u0432\u044f\u0442\u043d\u0430\u0434\u0446\u0430\u0442\u044c;\n"
        + "20: \u0434\u0432\u0430\u0434\u0446\u0430\u0442\u044c[ >>];\n"
        + "30: \u0442\u0440\u043b\u0434\u0446\u0430\u0442\u044c[ >>];\n"
        + "40: \u0441\u043e\u0440\u043e\u043a[ >>];\n"
        + "50: \u043f\u044f\u0442\u044c\u0434\u0435\u0441\u044f\u0442[ >>];\n"
        + "60: \u0448\u0435\u0441\u0442\u044c\u0434\u0435\u0441\u044f\u0442[ >>];\n"
        + "70: \u0441\u0435\u043c\u044c\u0434\u0435\u0441\u044f\u0442[ >>];\n"
        + "80: \u0432\u043e\u0441\u0435\u043c\u044c\u0434\u0435\u0441\u044f\u0442[ >>];\n"
        + "90: \u0434\u0435\u0432\u044f\u043d\u043e\u0441\u0442\u043e[ >>];\n"
        + "100: \u0441\u0442\u043e[ >>];\n"
        + "200: << \u0441\u0442\u043e[ >>];\n"
        + "1000: \u0442\u044b\u0441\u044f\u0447\u0430[ >>];\n"
        + "2000: << \u0442\u044b\u0441\u044f\u0447\u0430[ >>];\n"
        + "1,000,000: \u043c\u0438\u043b\u043b\u0438\u043e\u043d[ >>];\n"
        + "2,000,000: << \u043c\u0438\u043b\u043b\u0438\u043e\u043d[ >>];\n"
        + "1,000,000,000: =#,##0=;";
    // Can someone supply me with information on negatives and decimals?
    // How about words for billions and trillions?

    /**
     * Spellout rules for Hebrew.  Hebrew actually has inflected forms for
     * most of the lower-order numbers.  The masculine forms are shown
     * here.
     */
    public static final String hebrew =
        "zero (incomplete data); \u05d0\u05d4\u05d3; \u05e9\u05d2\u05d9\u05d9\u05dd; \u05e9\u05dc\u05d5\u05e9\u05d4;\n"
        + "4: \u05d0\u05d3\u05d1\u05e6\u05d4; \u05d7\u05d2\u05d5\u05d9\u05e9\u05d4; \u05e9\u05e9\u05d4;\n"
        + "7: \u05e9\u05d1\u05e6\u05d4; \u05e9\u05de\u05d5\u05d2\u05d4; \u05ea\u05e9\u05e6\u05d4;\n"
        + "10: \u05e6\u05e9\u05d3\u05d4[ >>];\n"
        + "20: \u05e6\u05e9\u05d3\u05d9\u05dd[ >>];\n"
        + "30: \u05e9\u05dc\u05d5\u05e9\u05d9\u05dd[ >>];\n"
        + "40: \u05d0\u05d3\u05d1\u05e6\u05d9\u05dd[ >>];\n"
        + "50: \u05d7\u05de\u05d9\u05e9\u05d9\u05dd[ >>];\n"
        + "60: \u05e9\u05e9\u05d9\u05dd[ >>];\n"
        + "70: \u05e9\u05d1\u05e6\u05d9\u05dd[ >>];\n"
        + "80: \u05e9\u05de\u05d5\u05d2\u05d9\u05dd[ >>];\n"
        + "90: \u05ea\u05e9\u05e6\u05d9\u05dd[ >>];\n"
        + "100: \u05de\u05d0\u05d4[ >>];\n"
        + "200: << \u05de\u05d0\u05d4[ >>];\n"
        + "1000: \u05d0\u05dc\u05e3[ >>];\n"
        + "2000: << \u05d0\u05dc\u05e3[ >>];\n"
        + "1,000,000: =#,##0= (incomplete data);";
    // This data is woefully incomplete.  Can someone fill me in on the
    // various inflected forms of the numbers, which seem to be necessary
    // to do Hebrew correctly?  Can somone supply me with data for values
    // from 1,000,000 on up?  What about the word for zero?  What about
    // information on negatives and decimals?

    //========================================================================
    // Simple examples
    //========================================================================

    /**
     * This rule set adds an English ordinal abbreviation to the end of a
     * number.  For example, 2 is formatted as "2nd".  Parsing doesn't work with
     * this rule set.  To parse, use DecimalFormat on the numeral.
     */
    public static final String ordinal =
        // this rule set formats the numeral and calls %%abbrev to
        // supply the abbreviation
        "%main:\n"
        + "    =#,##0==%%abbrev=;\n"
        // this rule set supplies the abbreviation
        + "%%abbrev:\n"
               // the abbreviations.  Everything from 4 to 19 ends in "th"
        + "    th; st; nd; rd; th;\n"
               // at 20, we begin repeating the cycle every 10 (13 is "13th",
               // but 23 and 33 are "23rd" and "33rd")  We do this by
               // ignoring all bug the ones digit in selecting the abbreviation
        + "    20: >>;\n"
               // at 100, we repeat the whole cycle by considering only the
               // tens and ones digits in picking an abbreviation
        + "    100: >>;\n";

    /**
     * This is a simple message-formatting example.  Normally one would
     * use ChoiceFormat and MessageFormat to do something this simple,
     * but this shows it could be done with RuleBasedNumberFormat too.
     * A message-formatting example that might work better with
     * RuleBasedNumberFormat appears later.
     */
    public static final String message1 =
        // this rule surrounds whatever the other rules produce with the
        // rest of the sentence
        "x.0: The search found <<.;\n"
        // use words for values below 10 (and change to "file" for 1)
        + "no files; one file; two files; three files; four files; five files;\n"
        + "    six files; seven files; eight files; nine files;\n"
        // use numerals for values higher than 10
        + "=#,##0= files;";

    //========================================================================
    // Fraction handling
    //
    // The next few examples show how RuleBasedNumberFormat can be used for
    // more flexible handling of fractions
    //========================================================================

    /**
     * This example formats a number in one of the two styles often used
     * on checks.  %dollars-and-hundredths formats cents as hundredths of
     * a dollar (23.40 comes out as "twenty-three and 40/100 dollars").
     * %dollars-and-cents formats in dollars and cents (23.40 comes out as
     * "twenty-three dollars and forty cents")
     */
    public static final String dollarsAndCents =
        // this rule set formats numbers as dollars and cents
        "%dollars-and-cents:\n"
               // if the value is 1 or more, put "xx dollars and yy cents".
               // the "and y cents" part is suppressed if the value is an
               // even number of dollars
        + "    x.0: << [and >%%cents>];\n"
               // if the value is between 0 and 1, put "xx cents"
        + "    0.x: >%%cents>;\n"
               // these three rules take care of the singular and plural
               // forms of "dollar" and use %%main to format the number
        + "    0: zero dollars; one dollar; =%%main= dollars;\n"
        // these are the regular U.S. English number spellout rules
        + "%%main:\n"
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
        + "    20: twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
        + "    100: << hundred[ >>];\n"
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000: << trillion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        // this rule takes care of the fractional part of the value.  It
        // multiplies the fractional part of the number being formatted by
        // 100, formats it with %%main, and then addes the word "cent" or
        // "cents" to the end.  (The text in brackets is omitted if the
        // numerator of the fraction is 1.)
        + "%%cents:\n"
        + "    100: <%%main< cent[s];\n"

        // this rule set formats numbers as dollars and hundredths of dollars
        + "%dollars-and-hundredths:\n"
               // this rule takes care of the general shell of the output
               // string.  We always show the cents, even when there aren't
               // any.  Because of this, the word is always "dollars"--
               // we don't have to worry about the singular form.  We use
               // %%main to format the number of dollars and %%hundredths to
               // format the number of cents
        + "    x.0: <%%main< and >%%hundredths>/100 dollars;\n"
        // this rule set formats the cents for %dollars-and-hundredths.
        // It multiplies the fractional part of the number by 100 and formats
        // the result using a DecimalFormat ("00" tells the DecimalFormat to
        // always use two digits, even for numbers under 10)
        + "%%hundredths:\n"
        + "    100: <00<;\n";

    /**
     * This rule set shows the fractional part of the number as a fraction
     * with a power of 10 as the denominator.  Some languages don't spell
     * out the fractional part of a number as "point one two three," but
     * always render it as a fraction.  If we still want to treat the fractional
     * part of the number as a decimal, then the fraction's denominator
     * is always a power of 10.  This example does that: 23.125 is formatted
     * as "twenty-three and one hundred twenty-five thousandths" (as opposed
     * to "twenty-three point one two five" or "twenty-three and one eighth").
     */
    public static final String decimalAsFraction =
        // the regular U.S. English spellout rules, with one difference
        "%main:\n"
        + "    -x: minus >>;\n"
               // the difference.  This rule uses %%frac to show the fractional
               // part of the number.  Text in brackets is omitted when the
               // value is between 0 and 1 (causing 0.3 to come out as "three
               // tenths" instead of "zero and three tenths").
        + "    x.x: [<< and ]>%%frac>;\n"
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
        + "    twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
        + "    100: << hundred[ >>];\n"
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000: << trillion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        // the rule set that formats the fractional part of the number.
        // The rule that is used is the one that, when its baase value is
        // multiplied by the fractional part of the number being formatted,
        // produces the result closest to zero.  Thus, the base values are
        // prospective denominators of the fraction.  The << marks the place
        // where the numerator of the fraction (the result of multiplying the
        // fractional part of the number by the rule's base value) is
        // placed.  Text in brackets is omitted when the numerator is 1, giving
        // us the singular and plural forms of the words.
        // [In languages where the singular and plural are completely different
        // words, the rule can just be stated twice: the second time with
        // the plural form.]
        + "%%frac:\n"
        + "    10: << tenth[s];\n"
        + "    100: << hundredth[s];\n"
        + "    1000: << thousandth[s];\n"
        + "    10,000: << ten-thousandth[s];\n"
        + "    100,000: << hundred-thousandth[s];\n"
        + "    1,000,000: << millionth[s];";

    /**
     * Number with closest fraction.  This example formats a value using
     * numerals, but shows the fractional part as a ratio (fraction) rather
     * than a decimal.  The fraction always has a denominator between 2 and 10.
     */
    public static final String closestFraction =
        "%main:\n"
               // this rule formats the number if it's 1 or more.  It formats
               // the integral part using a DecimalFormat ("#,##0" puts
               // thousands separators in the right places) and the fractional
               // part using %%frac.  If there is no fractional part, it
               // just shows the integral part.
        + "    x.0: <#,##0<[ >%%frac>];\n"
               // this rule formats the number if it's between 0 and 1.  It
               // shows only the fractional part (0.5 shows up as "1/2," not
               // "0 1/2")
        + "    0.x: >%%frac>;\n"
        // the fraction rule set.  This works the same way as the one in the
        // preceding example: We multiply the fractional part of the number
        // being formatted by each rule's base value and use the rule that
        // produces the result closest to 0 (or the first rule that produces 0).
        // Since we only provide rules for the numbers from 2 to 10, we know
        // we'll get a fraction with a denominator between 2 and 10.
        // "<0<" causes the numerator of the fraction to be formatted
        // using numerals
        + "%%frac:\n"
        + "    2: 1/2;\n"
        + "    3: <0</3;\n"
        + "    4: <0</4;\n"
        + "    5: <0</5;\n"
        + "    6: <0</6;\n"
        + "    7: <0</7;\n"
        + "    8: <0</8;\n"
        + "    9: <0</9;\n"
        + "    10: <0</10;\n";

    /**
     * American stock-price formatting.  Non-integral stock prices are still
     * generally shown in eighths or sixteenths of dollars instead of dollars
     * and cents.  This example formats stock prices in this way if possible,
     * and in dollars and cents if not.
     */
    public static final String stock =
        "%main:\n"
               // this rule formats the integral part of the number in numerals
               // and (if necessary) the fractional part using %%frac1
        + "    x.0: <#,##0<[>%%frac1>];\n"
               // this rule is used for values between 0 and 1 and omits the
               // integral part
        + "    0.x: >%%frac2>;\n"
        // this rule set is used to format the fractional part of the number when
        // there's an integral part before it (again, we try all denominators
        // and use the "best" one)
        + "%%frac1:\n"
               // for even multiples of 1/4, format the fraction using the
               // typographer's fractions
        + "    4: <%%quarters<;\n"
               // format the value as a number of eighths, sixteenths, or
               // thirty-seconds, whichever produces the most accurate value.
               // The apostrophe at the front of these rules is ignored, but
               // it makes the space that follows it significant.  This puts a
               // space between the value's integral and fractional parts so
               // you can read it
        + "    8: ' <0</8;\n"
        + "    16: ' <0</16;\n"
        + "    32: ' <0</32;\n"
               // if we can't reasonably format the number in powers of 2,
               // then show it as dollars and cents
        + "    100: .<00<;\n"
        // this rule set is used when the fractional part of the value stands
        // alone
        + "%%frac2:\n"
        + "    4: <%%quarters<;\n"
               // for fractions that we can't show using typographer's fractions,
               // we don't have to put a space before the fraction
        + "    8: <0</8;\n"
        + "    16: <0</16;\n"
        + "    32: <0</32;\n"
               // but dollars and cents look better with a leading 0
        + "    100: 0.<00<;\n"
        // this rule set formats 1/4, 1/2, and 3/4 using typographer's fractions
        + "%%quarters:\n"
        + "    ; \u00bc; \u00bd; \u00be;\n"
        // there are the lenient-parse rules.  These allow the user to type
        // "1/4," "1/2," and "3/4" instead of their typographical counterparts
        // and still have them be understood by the formatter
        + "%%lenient-parse:\n"
        + "    & '1/4' , \u00bc\n"
        + "    & '1/2' , \u00bd\n"
        + "    & '3/4' , \u00be\n;";

    //========================================================================
    // Changing dimensions
    //
    // The next few examples demonstrate using a RuleBasedNumberFormat to
    // change the units a value is denominated in depending on its magnitude
    //========================================================================

    /**
     * The example shows large numbers the way they often appear is nwespapers:
     * 1,200,000 is formatted as "1.2 million".
     */
    public static final String abbEnglish =
        "=#,##0=;\n"
        // this is fairly self-explanatory, but note that the << substitution
        // can show the fractional part of the substitution value if the user
        // wants it
        + "1,000,000: <##0.###< million;\n"
        + "1,000,000,000: <##0.###< billion;\n"
        + "1,000,000,000,000: <##0.###< trillion;\n";

    /**
     * This example takes a number of meters and formats it in whatever unit
     * will produce a number with from one to three digits before the decimal
     * point.  For example, 230,000 is formatted as "230 km".
     */
    public static final String units =
        "%main:\n"
               // for values between 0 and 1, delegate to %%small
        + "    0.x: >%%small>;\n"
               // otherwise, show between 3 and 6 significant digits of the value
               // along with the most appropriate unit
        + "    0: =##0.###= m;\n"
        + "    1,000: <##0.###< km;\n"
        + "    1,000,000: <##0.###< Mm;\n"
        + "    1,000,000,000: <##0.###< Gm;\n"
        + "    1,000,000,000,000: <#,##0.###< Tm;\n"
        // %%small formats the number when it's less then 1.  It multiplies the
        // value by one billion, and then uses %%small2 to actually do the
        // formatting.
        + "%%small:\n"
        + "    1,000,000,000,000: <%%small2<;\n"
        // this rule set actually formats small values.  %%small passes this
        // rule set a number of picometers, and it takes care of scaling up as
        // appropriate in exactly the same way %main does (we can't normally
        // handle fractional values this way: here, we're concerned about
        // magnitude; most of the time, we're concerned about precsion)
        + "%%small2:\n"
        + "    0: =##0= pm;\n"
        + "    1,000: <##0.###< nm;\n"
        + "    1,000,000: <##0.###< \u00b5m;\n"
        + "    1,000,000,000: <##0.###< mm;\n";

    /**
     * A more complicated message-formatting example.  Here, in addition to
     * handling the singular and plural versions of the word, the value is
     * denominated in bytes, kilobytes, or megabytes depending on its magnitude.
     * Also notice that it correctly treats a kilobyte as 1,024 bytes (not 1,000),
     * and a megabyte as 1,024 kilobytes (not 1,000).
     */
    public static final String message2 =
        // this rule supplies the shell of the sentence
        "x.0: There << free space on the disk.;\n"
        // handle singular and plural forms of "byte" (and format 0 as
        // "There is no free space...")
        + "0: is no;\n"
        + "is one byte of;\n"
        + "are =0= bytes of;\n"
        // for values above 1,024, format the number in K (since "K" is usually
        // promounced "K" regardless of whether it's singular or plural, we
        // don't worry about the plural form).  The "/1024" here causes us to
        // treat a K as 1,024 bytes rather than 1,000 bytes.
        + "1024/1024: is <0<K of;\n"
        // for values about 1,048,576, format the number in Mb.  Since "Mb" is
        // usually promounced "meg" in singular and "megs" in plural, we do have
        // both singular and plural forms.  Again, notice we treat a megabyte
        // as 1,024 kilobytes.
        + "1,048,576/1024: is 1 Mb of;\n"
        + "2,097,152/1024: are <0< Mb of;";

    //========================================================================
    // Alternate radices
    //========================================================================

    /**
     * This example formats a number in dozens and gross.  This is intended to
     * demonstrate how this rule set can be used to format numbers in systems
     * other than base 10.  The "/12" after the rules' base values controls this.
     * Also notice that the base doesn't have to be consistent throughout the
     * whole rule set: we go back to base 10 for values over 1,000.
     */
    public static final String dozens =
        // words for numbers...
        "zero; one; two; three; four; five; six;\n"
        + "seven; eight; nine; ten; eleven;\n"
        // format values over 12 in dozens
        + "12/12: << dozen[ and >>];\n"
        // format values over 144 in gross
        + "144/12: << gross[, >>];\n"
        // format values over 1,000 in thousands
        + "1000: << thousand[, >>];\n"
        // overflow rule.  Format values over 10,000 in numerals
        + "10,000: =#,##0=;\n";

    //========================================================================
    // Major and minor units
    //
    // These examples show how a single value can be divided up into major
    // and minor units that don't relate to each other by a factor of 10.
    //========================================================================

    /**
     * This example formats a number of seconds in sexagesimal notation
     * (i.e., hours, minutes, and seconds).  %with-words formats it with
     * words (3740 is "1 hour, 2 minutes, 20 seconds") and %in-numerals
     * formats it entirely in numerals (3740 is "1:02:20").
     */
    public static final String durationInSeconds =
        // main rule set for formatting with words
        "%with-words:\n"
               // take care of singular and plural forms of "second"
        + "    0 seconds; 1 second; =0= seconds;\n"
               // use %%min to format values greater than 60 seconds
        + "    60/60: <%%min<[, >>];\n"
               // use %%hr to format values greater than 3,600 seconds
               // (the ">>>" below causes us to see the number of minutes
               // when when there are zero minutes)
        + "    3600/60: <%%hr<[, >>>];\n"
        // this rule set takes care of the singular and plural forms
        // of "minute"
        + "%%min:\n"
        + "    0 minutes; 1 minute; =0= minutes;\n"
        // this rule set takes care of the singular and plural forms
        // of "hour"
        + "%%hr:\n"
        + "    0 hours; 1 hour; =0= hours;\n"

        // main rule set for formatting in numerals
        + "%in-numerals:\n"
               // values below 60 seconds are shown with "sec."
        + "    =0= sec.;\n"
               // higher values are shown with colons: %%min-sec is used for
               // values below 3,600 seconds...
        + "    60: =%%min-sec=;\n"
               // ...and %%hr-min-sec is used for values of 3,600 seconds
               // and above
        + "    3600: =%%hr-min-sec=;\n"
        // this rule causes values of less than 10 minutes to show without
        // a leading zero
        + "%%min-sec:\n"
        + "    0: :=00=;\n"
        + "    60/60: <0<>>;\n"
        // this rule set is used for values of 3,600 or more.  Minutes are always
        // shown, and always shown with two digits
        + "%%hr-min-sec:\n"
        + "    0: :=00=;\n"
        + "    60/60: <00<>>;\n"
        + "    3600/60: <#,##0<:>>>;\n"
        // the lenient-parse rules allow several different characters to be used
        // as delimiters between hours, minutes, and seconds
        + "%%lenient-parse:\n"
        + "    & : = . = ' ' = -;\n";

    /**
     * This example formats a number of hours in sexagesimal notation (i.e.,
     * hours, minutes, and seconds).  %with-words formats the value using
     * words for the units, and %in-numerals formats the value using only
     * numerals.
     */
    public static final String durationInHours =
        // main entry point for formatting with words
        "%with-words:\n"
               // this rule omits minutes and seconds when the value is
               // an even number of hours
        + "    x.0: <<[, >%%min-sec>];\n"
               // these rules take care of the singular and plural forms
               // of hours
        + "    0 hours; 1 hour; =#,##0= hours;\n"
        // this rule set takes the fractional part of the number and multiplies
        // it by 3,600 (turning it into a number of seconds).  Then it delegates
        // to %%min-sec-implementation to format the resulting value
        + "%%min-sec:\n"
        + "    3600: =%%min-sec-implementation=;\n"
        // this rule set formats the seconds as either seconds or minutes and
        // seconds, and takes care of the singular and plural forms of
        // "minute" and "second"
        + "%%min-sec-implementation:\n"
        + "    0 seconds; 1 second; =0= seconds;\n"
        + "    60/60: 1 minute[, >>];\n"
        + "    120/60: <0< minutes[, >>];\n"

        // main entry point for formatting in numerals
        + "%in-numerals:\n"
               // show minutes even for even numbers of hours
        + "    x.0: <#,##0<:00;\n"
               // delegate to %%min-sec2 to format minutes and seconds
        + "    x.x: <#,##0<:>%%min-sec2>;\n"
        // this rule set formats minutes when there is an even number of
        // minutes, and delegates to %%min-sec2-implementation when there
        // are seconds
        + "%%min-sec2:\n"
        + "    60: <00<;\n"
        + "    3600: <%%min-sec2-implementation<;\n"
        // these two rule sets are used to format the minutes and seconds
        + "%%min-sec2-implementation:\n"
               // if there are fewer than 60 seconds, show the minutes anyway
        + "    0: 00:=00=;\n"
               // if there are minutes, format them too, and always use 2 digits
               // for both minutes and seconds
        + "    60: =%%min-sec3=;\n"
        + "%%min-sec3:\n"
        + "    0: :=00=;\n"
        + "    60/60: <00<>>;\n"
        // the lenient-parse rules allow the user to use any of several
        // characters as delimiters between hours, minutes, and seconds
        + "%%lenient-parse:\n"
        + "    & : = . = ' ' = -;\n";

    /**
     * This rule set formats a number of pounds as pounds, shillings, and
     * pence in the old English system of currency.
     */
    public static final String poundsShillingsAndPence =
        // for values of 1 or more, format the integral part with a pound
        // sign in front, and show shillings and pence if necessary
        "%main:\n"
        + "    x.0: \u00a3<#,##0<[ >%%shillings-and-pence>];\n"
        // for values between 0 and 1, omit the number of pounds
        + "    0.x: >%%pence-alone>;\n"
        // this rule set is used to show shillings and pence.  It multiplies
        // the fractional part of the number by 240 (the number of pence in a
        // pound) and uses %%shillings-and-pence-implementation to format
        // the result
        + "%%shillings-and-pence:\n"
        + "    240: <%%shillings-and-pence-implementation<;\n"
        // this rule set is used to show shillings and pence when there are
        // no pounds.  It also multiplies the value by 240, and then it uses
        // %%pence-alone-implementation to format the result.
        + "%%pence-alone:\n"
        + "    240: <%%pence-alone-implementation<;\n"
        // this rule set formats a number of pence when we know we also
        // have pounds.  We always show shillings (with a 0 if necessary),
        // but only show pence if the value isn't an even number of shillings
        + "%%shillings-and-pence-implementation:\n"
        + "    0/; 0/=0=;\n"
        + "    12/12: <0</[>0>];\n"
        // this rule set formats a number of pence when we know there are
        // no pounds.  Values less than a shilling are shown with "d." (the
        // abbreviation for pence), and values greater than a shilling are
        // shown with a shilling bar (and without pence when the value is
        // an even number of shillings)
        + "%%pence-alone-implementation:\n"
        + "    =0= d.;\n"
        + "    12/12: <0</[>0>];\n";

    //========================================================================
    // Alternate numeration systems
    //
    // These examples show how RuleBasedNumberFormat can be used to format
    // numbers using non-positional numeration systems.
    //========================================================================

    /**
     * Arabic digits.  This example formats numbers in Arabic numerals.
     * Normally, you'd do this with DecimalFormat, but this shows that
     * RuleBasedNumberFormat can handle it too.
     */
    public static final String arabicNumerals =
        "0; 1; 2; 3; 4; 5; 6; 7; 8; 9;\n"
        + "10: <<>>;\n"
        + "100: <<>>>;\n"
        + "1000: <<,>>>;\n"
        + "1,000,000: <<,>>>;\n"
        + "1,000,000,000: <<,>>>;\n"
        + "1,000,000,000,000: <<,>>>;\n"
        + "1,000,000,000,000,000: =#,##0=;\n"
        + "-x: ->>;\n"
        + "x.x: <<.>>;";

    /**
     * Words for digits.  Follows the same pattern as the Arabic-numerals
     * example above, but uses words for the various digits (e.g., 123 comes
     * out as "one two three").
     */
    public static final String wordsForDigits =
        "-x: minus >>;\n"
        + "x.x: << point >>;\n"
        + "zero; one; two; three; four; five; six;\n"
        + "    seven; eight; nine;\n"
        + "10: << >>;\n"
        + "100: << >>>;\n"
        + "1000: <<, >>>;\n"
        + "1,000,000: <<, >>>;\n"
        + "1,000,000,000: <<, >>>;\n"
        + "1,000,000,000,000: <<, >>>;\n"
        + "1,000,000,000,000,000: =#,##0=;\n";

    /**
     * This example formats numbers using Chinese characters in the Arabic
     * place-value method.  This was used historically in China for a while.
     */
    public static final String chinesePlaceValue =
        "\u3007; \u4e00; \u4e8c; \u4e09; \u56db; \u4e94; \u516d; \u4e03; \u516b; \u4e5d;\n"
        + "10: <<>>;\n"
        + "100: <<>>>;\n"
        + "1000: <<>>>;\n"
        + "1,000,000: <<>>>;\n"
        + "1,000,000,000: <<>>>;\n"
        + "1,000,000,000,000: <<>>>;\n"
        + "1,000,000,000,000,000: =#,##0=;\n";

    /**
     * Roman numerals.  This example has two variants: %modern shows how large
     * numbers are usually handled today; %historical ses the older symbols for
     * thousands.
     */
    public static final String romanNumerals =
        "%historical:\n"
        + "    =%modern=;\n"
               // in early Roman numerals, 1,000 was shown with a circle
               // bisected by a vertical line.  Additional thousands were
               // shown by adding more concentric circles, and fives were
               // shown by cutting the symbol for next-higher power of 10
               // in half (the letter D for 500 evolved from this).
               // We could go beyond 40,000, but Unicode doesn't encode
               // the symbols for higher numbers/
        + "    1000: \u2180[>>]; 2000: \u2180\u2180[>>]; 3000: \u2180\u2180\u2180[>>]; 4000: \u2180\u2181[>>];\n"
        + "    5000: \u2181[>>]; 6000: \u2181\u2180[>>]; 7000: \u2181\u2180\u2180[>>];\n"
        + "    8000: \u2181\u2180\u2180\u2180[>>]; 9000: \u2180\u2182[>>];\n"
        + "    10,000: \u2182[>>]; 20,000: \u2182\u2182[>>]; 30,000: \u2182\u2182\u2182[>>];\n"
        + "    40,000: =#,##0=;\n"
        + "%modern:\n"
        + "    ; I; II; III; IV; V; VI; VII; VIII; IX;\n"
        + "    10: X[>>]; 20: XX[>>]; 30: XXX[>>]; 40: XL[>>]; 50: L[>>];\n"
        + "    60: LX[>>]; 70: LXX[>>]; 80: LXXX[>>]; 90: XC[>>];\n"
        + "    100: C[>>]; 200: CC[>>]; 300: CCC[>>]; 400: CD[>>]; 500: D[>>];\n"
        + "    600: DC[>>]; 700: DCC[>>]; 800: DCCC[>>]; 900: CM[>>];\n"
               // in modern Roman numerals, high numbers are generally shown
               // by placing a bar over the letters for the lower numbers:
               // the bar multiplied a letter's value by 1,000
        + "    1000: M[>>]; 2000: MM[>>]; 3000: MMM[>>]; 4000: MV\u0306[>>];\n"
        + "    5000: V\u0306[>>]; 6000: V\u0306M[>>]; 7000: V\u0306MM[>>];\n"
        + "    8000: V\u0306MMM[>>]; 9000: MX\u0306[>>];\n"
        + "    10,000: X\u0306[>>]; 20,000: X\u0306X\u0306[>>]; 30,000: X\u0306X\u0306X\u0306[>>];\n"
        + "    40,000: X\u0306L\u0306[>>]; 50,000: L\u0306[>>]; 60,000: L\u0306X\u0306[>>];\n"
        + "    70,000: L\u0306X\u0306X\u0306[>>]; 80,000: L\u0306X\u0306X\u0306X\u0306[>>];\n"
        + "    90,000: X\u0306C\u0306[>>];\n"
        + "    100,000: C\u0306[>>]; 200,000: C\u0306C\u0306[>>]; 300,000: C\u0306C\u0306[>>];\n"
        + "    400,000: C\u0306D\u0306[>>]; 500,000: D\u0306[>>]; 600,000: D\u0306C\u0306[>>];\n"
        + "    700,000: D\u0306C\u0306C\u0306[>>]; 800,000: D\u0306C\u0306C\u0306C\u0306[>>];\n"
        + "    900,000: =#,##0=;\n";

    /**
     * Hebrew alphabetic numerals.  Before adoption of Arabic numerals, Hebrew speakers
     * used the letter of their alphabet as numerals.  The first nine letters of
     * the alphabet repesented the values from 1 to 9, the second nine letters the
     * multiples of 10, and the remaining letters the multiples of 100.  Since they
     * ran out of letters at 400, the remaining multiples of 100 were represented
     * using combinations of the existing letters for the hundreds.  Numbers were
     * distinguished from words in a number of different ways: the way shown here
     * uses a single mark after a number consisting of one letter, and a double
     * mark between the last two letters of a number consisting of two or more
     * letters.  Two dots over a letter multiplied its value by 1,000.  Also, since
     * the letter for 10 is the first letter of God's name and the letters for 5 and 6
     * are letters in God's name, which wasn't supposed to be written or spoken, 15 and
     * 16 were usually written as 9 + 6 and 9 + 7 instead of 10 + 5 and 10 + 6.
     */
    public static final String hebrewAlphabetic =
        // letters for the ones
        "%%ones:\n"
        + "    (no zero); \u05d0; \u05d1; \u05d2; \u05d3; \u05d4; \u05d5; \u05d6; \u05d7; \u05d8;\n"
        // letters for the tens
        + "%%tens:\n"
        + "    ; \u05d9; \u05db; \u05dc; \u05de; \u05e0; \u05e1; \u05e2; \u05e4; \u05e6;\n"
        // letters for the first four hundreds
        + "%%hundreds:\n"
        + "    ; \u05e7; \u05e8; \u05e9; \u05ea;\n"
        // this rule set is used to write the combination of the tens and ones digits
        // when we know that no other digits precede them: they put the numeral marks
        // in the right place and properly handle 15 and 16 (I'm using the mathematical
        // prime characters for the numeral marks because my Unicode font doesn't
        // include the real Hebrew characters, which look just like the prime marks)
        + "%%tens-and-ones:\n"
               // for values less than 10, just use %%ones and put the numeral mark
               // afterward
        + "    =%%ones=\u2032;\n"
               // put the numeral mark at the end for 10, but in the middle for
               // 11 through 14
        + "    10: <%%tens<\u2032; <%%tens<\u2033>%%ones>;\n"
               // special-case 15 and 16
        + "    15: \u05d8\u2033\u05d5; 16: \u05d8\u2033\u05d6;\n"
               // go back to the normal method at 17
        + "    17: <%%tens<\u2033>%%ones>;\n"
               // repeat the rules for 10 and 11 to cover the values from 20 to 99
        + "    20: <%%tens<\u2032; <%%tens<\u2033>%%ones>;\n"
        // this rule set is used to format numbers below 1,000.  It relies on
        // %%tens-and-ones to format the tens and ones places, and adds logic
        // to handle the high hundreds and the numeral marks when there is no
        // tens digit.  Notice how the rules are paired: all of these pairs of
        // rules take advantage of the rollback rule: if the value (between 100
        // and 499) is an even multiple of 100, the rule for 100 is used; otherwise,
        // the rule for 101 (the following rule) is used.  The first rule in each
        // pair (the one for the even multiple) places the numeral mark in a different
        // spot than the second rule in each pair (which knows there are more digits
        // and relies on the rule supplying them to also supply the numeral mark).
        // The call to %%null in line 10 is there simply to invoke the rollback
        // rule.
        + "%%low-order:\n"
               // this rule is only called when there are other characters before.
               // It places the numeral mark before the last digit
        + "    \u2033=%%ones=;\n"
               // the rule for 10 places the numeral mark before the 10 character
               // (because we know it's the last character); the rule for 11 relies
               // on %%tens-and-ones to place the numeral mark
        + "    10: \u2033<%%tens<; =%%tens-and-ones=>%%null>;\n"
               // the rule for 100 places the numeral mark before the 100 character
               // (we know it's the last character); the rule for 101 recurses to
               // fill in the remaining digits and the numeral mark
        + "    100: <%%hundreds<\u2032; <%%hundreds<>>;\n"
               // special-case the hundreds from 500 to 900 because they consist of
               // more than one character
        + "    500: \u05ea\u2033\u05e7; \u05ea\u05e7>>;\n"
        + "    600: \u05ea\u2033\u05e8; \u05ea\u05e8>>;\n"
        + "    700: \u05ea\u2033\u05e9; \u05ea\u05e9>>;\n"
        + "    800: \u05ea\u2033\u05ea; \u05ea\u05ea>>;\n"
        + "    900: \u05ea\u05ea\u2033\u05e7; \u05ea\u05ea\u05e7>>;\n"
        // this rule set is used to format values of 1,000 or more.  Here, we don't
        // worry about the numeral mark, and we add two dots (the Unicode combining
        // diaeresis character) to ever letter
        + "%%high-order:\n"
               // put the ones digit, followed by the diaeresis
        + "    =%%ones=\u0308;\n"
               // the tens can be handled with recursion
        + "    10: <%%tens<\u0308[>>];\n"
               // still have to special-case 15 and 16
        + "    15: \u05d8\u0308\u05d5\u0308; 16: \u05d8\u003078\u05d6\u0308;\n"
               // back to the regular rules at 17
        + "    17: <%%tens<\u0308[>>];\n"
               // the hundreds with the dots added (and without worrying about
               // placing the numeral mark)
        + "    100: <%%hundreds<\u0308[>>];\n"
        + "    500: \u05ea\u0308\u05e7\u0308[>>];\n"
        + "    600: \u05ea\u0308\u05e8\u0308[>>];\n"
        + "    700: \u05ea\u0308\u05e9\u0308[>>];\n"
        + "    800: \u05ea\u0308\u05ea\u0308[>>];\n"
        + "    900: \u05ea\u0308\u05ea\u0308\u05e7\u0308[>>];\n"
        // this rule set doesn't do anything; it's used by some other rules to
        // invoke the rollback rule
        + " %%null:\n"
        + "    ;\n"
        // the main rule set.
        + "%main:\n"
               // for values below 10, just output the letter and the numeral mark
        + "    =%%ones=\u2032;\n"
               // for values from 10 to 99, use %%tens-and-ones to do the formatting
        + "    10: =%%tens-and-ones=;\n"
               // for values from 100 to 999, use %%low-order to do the formatting
        + "    100: =%%low-order=;\n"
               // for values of 1,000 and over, use %%high-order to do the formatting
        + "    1000: <%%high-order<[>%%low-order>];\n";

    /**
     * Greek alphabetic numerals.  The Greeks, before adopting the Arabic numerals,
     * also used the letters of their alphabet as numerals.  There are three now-
     * obsolete Greek letters that are used as numerals; many fonts don't have them.
     * Large numbers were handled many different ways; the way shown here divides
     * large numbers into groups of four letters (factors of 10,000), and separates
     * the groups with the capital letter mu (for myriad).  Capital letters are used
     * for values below 10,000; small letters for higher numbers (to make the capital
     * mu stand out).
     */
    public static final String greekAlphabetic =
        // this rule set is used for formatting numbers below 10,000.  It uses
        // capital letters.
        "%%low-order:\n"
        + "    (no zero); \u0391; \u0392; \u0393; \u0394; \u0395; \u03dc; \u0396; \u0397; \u0398;\n"
        + "    10: \u0399[>>]; 20: \u039a[>>]; 30: \u039b[>>]; 40: \u039c[>>]; 50: \u039d[>>];\n"
        + "    60: \u039e[>>]; 70: \u039f[>>]; 80: \u03a0[>>]; 90: \u03de[>>];\n"
        + "    100: \u03a1[>>]; 200: \u03a3[>>]; 300: \u03a4[>>]; 400: \u03a5[>>];\n"
        + "    500: \u03a6[>>]; 600: \u03a7[>>]; 700: \u03a8[>>]; 800: \u03a9[>>];\n"
        + "    900: \u03e0[>>];\n"
               // the thousands are represented by the same numbers as the ones, but
               // with a comma-like mark added to their left shoulder
        + "    1000: \u0391\u0313[>>]; 2000: \u0392\u0313[>>]; 3000: \u0393\u0313[>>];\n"
        + "    4000: \u0394\u0313[>>]; 5000: \u0395\u0313[>>]; 6000: \u03dc\u0313[>>];\n"
        + "    7000: \u0396\u0313[>>]; 8000: \u0397\u0313[>>]; 9000: \u0398\u0313[>>];\n"
        // this rule set is the same as above, but uses lowercase letters.  It is used
        // for formatting the groups in numbers above 10,000.
        + "%%high-order:\n"
        + "    (no zero); \u03b1; \u03b2; \u03b3; \u03b4; \u03b5; \u03dc; \u03b6; \u03b7; \u03b8;\n"
        + "    10: \u03b9[>>]; 20: \u03ba[>>]; 30: \u03bb[>>]; 40: \u03bc[>>]; 50: \u03bd[>>];\n"
        + "    60: \u03be[>>]; 70: \u03bf[>>]; 80: \u03c0[>>]; 90: \u03de[>>];\n"
        + "    100: \u03c1[>>]; 200: \u03c3[>>]; 300: \u03c4[>>]; 400: \u03c5[>>];\n"
        + "    500: \u03c6[>>]; 600: \u03c7[>>]; 700: \u03c8[>>]; 800: \u03c9[>>];\n"
        + "    900: \u03c0[>>];\n"
        + "    1000: \u03b1\u0313[>>]; 2000: \u03b2\u0313[>>]; 3000: \u03b3\u0313[>>];\n"
        + "    4000: \u03b4\u0313[>>]; 5000: \u03b5\u0313[>>]; 6000: \u03dc\u0313[>>];\n"
        + "    7000: \u03b6\u0313[>>]; 8000: \u03b7\u0313[>>]; 9000: \u03b8\u0313[>>];\n"
        // the main rule set
        + "%main:\n"
               // for values below 10,000, just use %%low-order
        + "    =%%low-order=;\n"
               // for values above 10,000, split into two groups of four digits
               // and format each with %%high-order (putting an M in betwen)
        + "    10,000: <%%high-order<\u039c>%%high-order>;\n"
               // for values above 100,000,000, add another group onto the front
               // and another M
        + "    100,000,000: <%%high-order<\u039c>>\n";

    /**
     * A list of all the sample rule sets, used by the demo program.
     */
    public static final String[] sampleRuleSets =
        { usEnglish,
          ukEnglish,
          spanish,
          french,
          swissFrench,
          german,
          italian,
          swedish,
          dutch,
          japanese,
          greek,
          russian,
          hebrew,
          ordinal,
          message1,
          dollarsAndCents,
          decimalAsFraction,
          closestFraction,
          stock,
          abbEnglish,
          units,
          message2,
          dozens,
          durationInSeconds,
          durationInHours,
          poundsShillingsAndPence,
          arabicNumerals,
          wordsForDigits,
          chinesePlaceValue,
          romanNumerals,
          hebrewAlphabetic,
          greekAlphabetic };

    /**
     * The displayable names for all the sample rule sets, in the same order as
     * the preceding array.
     */
    public static final String[] sampleRuleSetNames =
        { "English (US)",
          "English (UK)",
          "Spanish",
          "French (France)",
          "French (Switzerland)",
          "German",
          "Italian",
          "Swedish",
          "Dutch",
          "Japanese",
          "Greek",
          "Russian",
          "Hebrew",
          "English ordinal abbreviations",
          "Simple message formatting",
          "Dollars and cents",
          "Decimals as fractions",
          "Closest fraction",
          "Stock prices",
          "Abbreviated US English",
          "Changing dimensions",
          "Complex message formatting",
          "Dozens",
          "Duration (value in seconds)",
          "Duration (value in hours)",
          "Pounds, shillings, and pence",
          "Arabic numerals",
          "Words for digits",
          "Chinese place-value notation",
          "Roman numerals",
          "Hebrew ahlphabetic numerals",
          "Greek alphabetic numerals" };

    /**
     * The base locale for each of the sample rule sets.  The locale is used to
     * determine DecimalFormat behavior, lenient-parse behavior, and text-display
     * selection (we have a hack in here to allow display of non-Latin scripts).
     * Null means the locale setting is irrelevant and the default can be used.
     */
    public static final Locale[] sampleRuleSetLocales =
        { Locale.US,
          Locale.UK,
          new Locale("es", "", ""),
          Locale.FRANCE,
          new Locale("fr", "CH", ""),
          Locale.GERMAN,
          Locale.ITALIAN,
          new Locale("sv", "", ""),
          new Locale("nl", "", ""),
          Locale.JAPANESE,
          new Locale("el", "", ""),
          new Locale("ru", "", ""),
          new Locale("iw", "", ""),
          Locale.ENGLISH,
          Locale.ENGLISH,
          Locale.US,
          Locale.ENGLISH,
          null,
          null,
          Locale.ENGLISH,
          null,
          Locale.ENGLISH,
          Locale.ENGLISH,
          null,
          null,
          Locale.UK,
          null,
          Locale.ENGLISH,
          new Locale("zh", "", ""),
          null,
          new Locale("iw", "", ""),
          new Locale("el", "", ""),
          null };

        public static final String[] sampleRuleSetCommentary = {
            "This demonstration version of the "
            + "U.S. English spellout rules has four variants: 1) %simplified is a "
            + "set of rules showing the simple method of spelling out numbers in "
            + "English: 289 is formatted as \"two hundred eighty-nine\".  2) %alt-teens "
            + "is the same as %simplified, except that values between 1,000 and 9,999 "
            + "whose hundreds place isn't zero are formatted in hundreds.  For example, "
            + "1,983 is formatted as \"nineteen hundred eighty-three,\" and 2,183 is "
            + "formatted as \"twenty-one hundred eighty-three,\" but 2,083 is still "
            + "formatted as \"two thousand eighty-three.\"  3) %ordinal formats the "
            + "values as ordinal numbers in English (e.g., 289 is \"two hundred eighty-"
            + "ninth\").  4) %default uses a more complicated algorithm to format "
            + "numbers in a more natural way: 289 is formatted as \"two hundred AND "
            + "eighty-nine\" and commas are inserted between the thousands groups for "
            + "values above 100,000.",

            "U.K. English has one significant "
            + "difference from U.S. English: the names for values of 1,000,000,000 "
            + "and higher.  In American English, each successive \"-illion\" is 1,000 "
            + "times greater than the preceding one: 1,000,000,000 is \"one billion\" "
            + "and 1,000,000,000,000 is \"one trillion.\"  In British English, each "
            + "successive \"-illion\" is one million times greater than the one before: "
            + "\"one billion\" is 1,000,000,000,000 (or what Americans would call a "
            + "\"trillion\"), and \"one trillion\" is 1,000,000,000,000,000,000.  "
            + "1,000,000,000 in British English is \"one thousand million.\"  (This "
            + "value is sometimes called a \"milliard,\" but this word seems to have "
            + "fallen into disuse.)",

            "The Spanish rules are quite similar to "
            + "the English rules, but there are some important differences: "
            + "First, we have to provide separate rules for most of the twenties "
            + "because the ones digit frequently picks up an accent mark that it "
            + "doesn't have when standing alone.  Second, each multiple of 100 has "
            + "to be specified separately because the multiplier on 100 very often "
            + "changes form in the contraction: 500 is \"quinientos,\" not "
            + "\"cincocientos.\"  In addition, the word for 100 is \"cien\" when "
            + "standing alone, but changes to \"ciento\" when followed by more digits.  "
            + "There also some other differences.",

            "French adds some interesting quirks of its "
            + "own: 1) The word \"et\" is interposed between the tens and ones digits, "
            + "but only if the ones digit if 1: 20 is \"vingt,\" and 2 is \"vingt-deux,\" "
            + "but 21 is \"vingt-et-un.\"  2)  There are no words for 70, 80, or 90.  "
            + "\"quatre-vingts\" (\"four twenties\") is used for 80, and values proceed "
            + "by score from 60 to 99 (e.g., 73 is \"soixante-treize\" [\"sixty-thirteen\"]).  "
            + "Numbers from 1,100 to 1,199 are rendered as hundreds rather than "
            + "thousands: 1,100 is \"onze cents\" (\"eleven hundred\"), rather than "
            + "\"mille cent\" (\"one thousand one hundred\")",

            "Swiss French differs from French French "
            + "in that it does have words for 70, 80, and 90.  This rule set shows them, "
            + "and is simpler as a result.",

            "German also adds some interesting "
            + "characteristics.  For values below 1,000,000, numbers are customarily "
            + "written out as a single word.  And the ones digit PRECEDES the tens "
            + "digit (e.g., 23 is \"dreiundzwanzig,\" not \"zwanzigunddrei\").",

            "Like German, most Italian numbers are "
            + "written as single words.  What makes these rules complicated is the rule "
            + "that says that when a word ending in a vowel and a word beginning with "
            + "a vowel are combined into a compound, the vowel is dropped from the "
            + "end of the first word: 180 is \"centottanta,\" not \"centoottanta.\"  "
            + "The complexity of this rule set is to produce this behavior.",

            "Spellout rules for Swedish.",

            "Spellout rules for Dutch.  Notice that in Dutch, as in German,"
            + "the ones digit precedes the tens digit.",

            "In Japanese, there really isn't any "
            + "distinction between a number written out in digits and a number "
            + "written out in words: the ideographic characters are both digits "
            + "and words.  This rule set provides two variants:  %traditional "
            + "uses the traditional CJK numerals (which are also used in China "
            + "and Korea).  %financial uses alternate ideographs for many numbers "
            + "that are harder to alter than the traditional numerals (one could "
            + "fairly easily change a one to "
            + "a three just by adding two strokes, for example).  This is also done in "
            + "the other countries using Chinese idographs, but different ideographs "
            + "are used in those places.",

            "Again in Greek we have to supply the words "
            + "for the multiples of 100 because they can't be derived algorithmically.  "
            + "Also, the tens dgit changes form when followed by a ones digit: an "
            + "accent mark disappears from the tens digit and moves to the ones digit.  "
            + "Therefore, instead of using the [] notation, we actually have to use "
            + "two separate rules for each multiple of 10 to show the two forms of "
            + "the word.",

            "Spellout rules for Russian.",

            "Spellout rules for Hebrew.  Hebrew actually has inflected forms for "
            + "most of the lower-order numbers.  The masculine forms are shown "
            + "here.",

            "This rule set adds an English ordinal abbreviation to the end of a "
            + "number.  For example, 2 is formatted as \"2nd\".  Parsing doesn't work with "
            + "this rule set.  To parse, use DecimalFormat on the numeral.",

            "This is a simple message-formatting example.  Normally one would "
            + "use ChoiceFormat and MessageFormat to do something this simple, "
            + "but this shows it could be done with RuleBasedNumberFormat too.  "
            + "A message-formatting example that might work better with "
            + "RuleBasedNumberFormat appears later.",

            "The next few examples demonstrate fraction handling.  "
            + "This example formats a number in one of the two styles often used "
            + "on checks.  %dollars-and-hundredths formats cents as hundredths of "
            + "a dollar (23.40 comes out as \"twenty-three and 40/100 dollars\").  "
            + "%dollars-and-cents formats in dollars and cents (23.40 comes out as "
            + "\"twenty-three dollars and forty cents\")",

            "This rule set shows the fractional part of the number as a fraction "
            + "with a power of 10 as the denominator.  Some languages don't spell "
            + "out the fractional part of a number as \"point one two three,\" but "
            + "always render it as a fraction.  If we still want to treat the fractional "
            + "part of the number as a decimal, then the fraction's denominator "
            + "is always a power of 10.  This example does that: 23.125 is formatted "
            + "as \"twenty-three and one hundred twenty-five thousandths\" (as opposed "
            + "to \"twenty-three point one two five\" or \"twenty-three and one eighth\").",

            "Number with closest fraction.  This example formats a value using "
            + "numerals, but shows the fractional part as a ratio (fraction) rather "
            + "than a decimal.  The fraction always has a denominator between 2 and 10.",

            "American stock-price formatting.  Non-integral stock prices are still "
            + "generally shown in eighths or sixteenths of dollars instead of dollars "
            + "and cents.  This example formats stock prices in this way if possible, "
            + "and in dollars and cents if not.",

            "The next few examples demonstrate using a RuleBasedNumberFormat to "
            + "change the units a value is denominated in depending on its magnitude.  "
            + "The example shows large numbers the way they often appear is nwespapers: "
            + "1,200,000 is formatted as \"1.2 million\".",

            "This example takes a number of meters and formats it in whatever unit "
            + "will produce a number with from one to three digits before the decimal "
            + "point.  For example, 230,000 is formatted as \"230 km\".",

            "A more complicated message-formatting example.  Here, in addition to "
            + "handling the singular and plural versions of the word, the value is "
            + "denominated in bytes, kilobytes, or megabytes depending on its magnitude.  "
            + "Also notice that it correctly treats a kilobyte as 1,024 bytes (not 1,000), "
            + "and a megabyte as 1,024 kilobytes (not 1,000).",

            "This example formats a number in dozens and gross.  This is intended to "
            + "demonstrate how this rule set can be used to format numbers in systems "
            + "other than base 10.  The \"/12\" after the rules' base values controls this.  "
            + "Also notice that the base doesn't have to be consistent throughout the "
            + "whole rule set: we go back to base 10 for values over 1,000.",

            "The next few examples show how a single value can be divided up into major "
            + "and minor units that don't relate to each other by a factor of 10.  "
            + "This example formats a number of seconds in sexagesimal notation "
            + "(i.e., hours, minutes, and seconds).  %with-words formats it with "
            + "words (3740 is \"1 hour, 2 minutes, 20 seconds\") and %in-numerals "
            + "formats it entirely in numerals (3740 is \"1:02:20\").",

            "This example formats a number of hours in sexagesimal notation (i.e., "
            + "hours, minutes, and seconds).  %with-words formats the value using "
            + "words for the units, and %in-numerals formats the value using only "
            + "numerals.",

            "This rule set formats a number of pounds as pounds, shillings, and "
            + "pence in the old English system of currency.",

            "These examples show how RuleBasedNumberFormat can be used to format "
            + "numbers using non-positional numeration systems.  "
            + "This example formats numbers in Arabic numerals.  "
            + "Normally, you'd do this with DecimalFormat, but this shows that "
            + "RuleBasedNumberFormat can handle it too.",

            "This example follows the same pattern as the Arabic-numerals "
            + "example, but uses words for the various digits (e.g., 123 comes "
            + "out as \"one two three\").",

            "This example formats numbers using Chinese characters in the Arabic "
            + "place-value method.  This was used historically in China for a while.",

            "Roman numerals.  This example has two variants: %modern shows how large "
            + "numbers are usually handled today; %historical ses the older symbols for "
            + "thousands.  Not all of the characters are displayable with most fonts.",

            "Hebrew alphabetic numerals.  Before adoption of Arabic numerals, Hebrew speakers "
            + "used the letter of their alphabet as numerals.  The first nine letters of "
            + "the alphabet repesented the values from 1 to 9, the second nine letters the "
            + "multiples of 10, and the remaining letters the multiples of 100.  Since they "
            + "ran out of letters at 400, the remaining multiples of 100 were represented "
            + "using combinations of the existing letters for the hundreds.  Numbers were "
            + "distinguished from words in a number of different ways: the way shown here "
            + "uses a single mark after a number consisting of one letter, and a double "
            + "mark between the last two letters of a number consisting of two or more "
            + "letters.  Two dots over a letter multiplied its value by 1,000.  Also, since "
            + "the letter for 10 is the first letter of God's name and the letters for 5 and 6 "
            + "are letters in God's name, which wasn't supposed to be written or spoken, 15 and "
            + "16 were usually written as 9 + 6 and 9 + 7 instead of 10 + 5 and 10 + 6.",

            "Greek alphabetic numerals.  The Greeks, before adopting the Arabic numerals, "
            + "also used the letters of their alphabet as numerals.  There are three now-"
            + "obsolete Greek letters that are used as numerals; many fonts don't have them.  "
            + "Large numbers were handled many different ways; the way shown here divides "
            + "large numbers into groups of four letters (factors of 10,000), and separates "
            + "the groups with the capital letter mu (for myriad).  Capital letters are used "
            + "for values below 10,000; small letters for higher numbers (to make the capital "
            + "mu stand out).",

            "This is a custom (user-defined) rule set."
        };
}
