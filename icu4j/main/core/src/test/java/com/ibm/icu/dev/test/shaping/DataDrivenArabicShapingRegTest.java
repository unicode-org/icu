// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.shaping;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.util.Arrays;
import java.util.Collection;
import java.util.MissingResourceException;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;

/**
 * Regression test for Arabic shaping.
 */
@RunWith(Enclosed.class)
public class DataDrivenArabicShapingRegTest extends CoreTestFmwk {

    /* constants copied from ArabicShaping for convenience */

    public static final int LENGTH_GROW_SHRINK = 0;
    public static final int LENGTH_FIXED_SPACES_NEAR = 1;
    public static final int LENGTH_FIXED_SPACES_AT_END = 2;
    public static final int LENGTH_FIXED_SPACES_AT_BEGINNING = 3;

    public static final int TEXT_DIRECTION_LOGICAL = 0;
    public static final int TEXT_DIRECTION_VISUAL_LTR = 4;

    public static final int LETTERS_NOOP = 0;
    public static final int LETTERS_SHAPE = 8;
    public static final int LETTERS_SHAPE_TASHKEEL_ISOLATED = 0x18;
    public static final int LETTERS_UNSHAPE = 0x10;

    public static final int DIGITS_NOOP = 0;
    public static final int DIGITS_EN2AN = 0x20;
    public static final int DIGITS_AN2EN = 0x40;
    public static final int DIGITS_EN2AN_INIT_LR = 0x60;
    public static final int DIGITS_EN2AN_INIT_AL = 0x80;
    //    private static final int DIGITS_RESERVED = 0xa0;

    public static final int DIGIT_TYPE_AN = 0;
    public static final int DIGIT_TYPE_AN_EXTENDED = 0x100;

    @RunWith(Parameterized.class)
    public static class StandardDataTest extends CoreTestFmwk {
        private String source;
        private int flags;
        private String expected;

        public StandardDataTest(String source, int flags, String expected) {
            this.source = source;
            this.flags = flags;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            String lamAlefSpecialVLTR =
                "\u0020\u0646\u0622\u0644\u0627\u0020\u0646\u0623\u064E\u0644\u0627\u0020" +
                "\u0646\u0627\u0670\u0644\u0627\u0020\u0646\u0622\u0653\u0644\u0627\u0020" +
                "\u0646\u0625\u0655\u0644\u0627\u0020\u0646\u0622\u0654\u0644\u0627\u0020" +
                "\uFEFC\u0639";
            String tashkeelSpecialVLTR =
                "\u064A\u0628\u0631\u0639\u0020\u064A\u0628\u0651\u0631\u064E\u0639\u0020" +
                "\u064C\u064A\u0628\u0631\u064F\u0639\u0020\u0628\u0670\u0631\u0670\u0639" +
                "\u0020\u0628\u0653\u0631\u0653\u0639\u0020\u0628\u0654\u0631\u0654\u0639" +
                "\u0020\u0628\u0655\u0631\u0655\u0639\u0020";
            String tashkeelShaddaRTL=
                "\u0634\u0651\u0645\u0652\u0633";
            String tashkeelShaddaLTR=
                "\u0633\u0652\u0645\u0651\u0634";
            String ArMathSym =
                "\uD83B\uDE00\uD83B\uDE01\uD83B\uDE02\uD83B\uDE03\u0020\uD83B\uDE24\uD83B" +
                "\uDE05\uD83B\uDE06\u0020\uD83B\uDE07\uD83B\uDE08\uD83B\uDE09\u0020\uD83B" +
                "\uDE0A\uD83B\uDE0B\uD83B\uDE0C\uD83B\uDE0D\u0020\uD83B\uDE0E\uD83B\uDE0F" +
                "\uD83B\uDE10\uD83B\uDE11\u0020\uD83B\uDE12\uD83B\uDE13\uD83B\uDE14\uD83B" +
                "\uDE15\u0020\uD83B\uDE16\uD83B\uDE17\uD83B\uDE18\u0020\uD83B\uDE19\uD83B" +
                "\uDE1A\uD83B\uDE1B";
            String ArMathSymLooped =
                "\uD83B\uDE80\uD83B\uDE81\uD83B\uDE82\uD83B\uDE83\u0020\uD83B\uDE84\uD83B" +
                "\uDE85\uD83B\uDE86\u0020\uD83B\uDE87\uD83B\uDE88\uD83B\uDE89\u0020\uD83B" +
                "\uDE8B\uD83B\uDE8C\uD83B\uDE8D\u0020\uD83B\uDE8E\uD83B\uDE8F\uD83B\uDE90" +
                "\uD83B\uDE91\u0020\uD83B\uDE92\uD83B\uDE93\uD83B\uDE94\uD83B\uDE95\u0020" +
                "\uD83B\uDE96\uD83B\uDE97\uD83B\uDE98\u0020\uD83B\uDE99\uD83B\uDE9A\uD83B" +
                "\uDE9B";
            String ArMathSymDoubleStruck =
                "\uD83B\uDEA1\uD83B\uDEA2\uD83B\uDEA3\u0020\uD83B\uDEA5\uD83B\uDEA6\u0020" +
                "\uD83B\uDEA7\uD83B\uDEA8\uD83B\uDEA9\u0020\uD83B\uDEAB\uD83B\uDEAC\uD83B" +
                "\uDEAD\u0020\uD83B\uDEAE\uD83B\uDEAF\uD83B\uDEB0\uD83B\uDEB1\u0020\uD83B" +
                "\uDEB2\uD83B\uDEB3\uD83B\uDEB4\uD83B\uDEB5\u0020\uD83B\uDEB6\uD83B\uDEB7" +
                "\uD83B\uDEB8\u0020\uD83B\uDEB9\uD83B\uDEBA\uD83B\uDEBB";
            String ArMathSymInitial =
                "\uD83B\uDE21\uD83B\uDE22\u0020\uD83B\uDE27\uD83B\uDE29\u0020\uD83B\uDE2A" +
                "\uD83B\uDE2B\uD83B\uDE2C\uD83B\uDE2D\u0020\uD83B\uDE2E\uD83B\uDE2F\uD83B" +
                "\uDE30\uD83B\uDE31\u0020\uD83B\uDE32\uD83B\uDE34\uD83B\uDE35\u0020\uD83B" +
                "\uDE36\uD83B\uDE37\u0020\uD83B\uDE39\uD83B\uDE3B";
            String ArMathSymTailed =
                "\uD83B\uDE42\uD83B\uDE47\uD83B\uDE49\uD83B\uDE4B\u0020\uD83B\uDE4D\uD83B" +
                "\uDE4E\uD83B\uDE4F\u0020\uD83B\uDE51\uD83B\uDE52\uD83B\uDE54\uD83B\uDE57" +
                "\u0020\uD83B\uDE59\uD83B\uDE5B\uD83B\uDE5D\uD83B\uDE5F";
            String ArMathSymStretched =
                "\uD83B\uDE21\u0633\uD83B\uDE62\u0647";
            String logicalUnshape =
                "\u0020\u0020\u0020\uFE8D\uFEF5\u0020\uFEE5\u0020\uFE8D\uFEF7\u0020\uFED7" +
                "\uFEFC\u0020\uFEE1\u0020\uFE8D\uFEDF\uFECC\uFEAE\uFE91\uFEF4\uFE94\u0020" +
                "\uFE8D\uFEDF\uFEA4\uFEAE\uFE93\u0020\u0020\u0020\u0020";
            String numSource =
                "\u0031" +  /* en:1 */
                "\u0627" +  /* arabic:alef */
                "\u0032" +  /* en:2 */
                "\u06f3" +  /* an:3 */
                "\u0061" +  /* latin:a */
                "\u0034";   /* en:4 */
                
            return Arrays.asList(new Object[][] {
                    /* lam alef special visual ltr */
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_NEAR,
                     "\u0020\ufee5\u0020\ufef5\ufe8d\u0020\ufee5\u0020\ufe76\ufef7\ufe8d\u0020" +
                     "\ufee5\u0020\u0670\ufefb\ufe8d\u0020\ufee5\u0020\u0653\ufef5\ufe8d\u0020" +
                     "\ufee5\u0020\u0655\ufef9\ufe8d\u0020\ufee5\u0020\u0654\ufef5\ufe8d\u0020" +
                     "\ufefc\ufecb"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_AT_END,
                     "\u0020\ufee5\ufef5\ufe8d\u0020\ufee5\ufe76\ufef7\ufe8d\u0020\ufee5\u0670" +
                     "\ufefb\ufe8d\u0020\ufee5\u0653\ufef5\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d" +
                     "\u0020\ufee5\u0654\ufef5\ufe8d\u0020\ufefc\ufecb\u0020\u0020\u0020\u0020" +
                     "\u0020\u0020"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_AT_BEGINNING,
                     "\u0020\u0020\u0020\u0020\u0020\u0020\u0020\ufee5\ufef5\ufe8d\u0020\ufee5" +
                     "\ufe76\ufef7\ufe8d\u0020\ufee5\u0670\ufefb\ufe8d\u0020\ufee5\u0653\ufef5" +
                     "\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d\u0020\ufee5\u0654\ufef5\ufe8d\u0020" +
                     "\ufefc\ufecb"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_GROW_SHRINK,
                     "\u0020\ufee5\ufef5\ufe8d\u0020\ufee5\ufe76\ufef7\ufe8d\u0020\ufee5\u0670" +
                     "\ufefb\ufe8d\u0020\ufee5\u0653\ufef5\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d" +
                     "\u0020\ufee5\u0654\ufef5\ufe8d\u0020\ufefc\ufecb"},
                    /* TASHKEEL */
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE_TASHKEEL_ISOLATED | TEXT_DIRECTION_VISUAL_LTR |
                     LENGTH_FIXED_SPACES_NEAR,
                     "\u0020\ufee5\u0020\ufef5\ufe8d\u0020\ufee5\u0020\ufe76\ufef7\ufe8d\u0020" +
                     "\ufee5\u0020\u0670\ufefb\ufe8d\u0020\ufee5\u0020\u0653\ufef5\ufe8d\u0020" +
                     "\ufee5\u0020\u0655\ufef9\ufe8d\u0020\ufee5\u0020\u0654\ufef5\ufe8d\u0020" +
                     "\ufefc\ufecb"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE_TASHKEEL_ISOLATED | TEXT_DIRECTION_VISUAL_LTR |
                     LENGTH_FIXED_SPACES_AT_END,
                     "\u0020\ufee5\ufef5\ufe8d\u0020\ufee5\ufe76\ufef7\ufe8d\u0020\ufee5\u0670" +
                     "\ufefb\ufe8d\u0020\ufee5\u0653\ufef5\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d" +
                     "\u0020\ufee5\u0654\ufef5\ufe8d\u0020\ufefc\ufecb\u0020\u0020\u0020\u0020" +
                     "\u0020\u0020"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE_TASHKEEL_ISOLATED | TEXT_DIRECTION_VISUAL_LTR |
                     LENGTH_FIXED_SPACES_AT_BEGINNING,
                     "\u0020\u0020\u0020\u0020\u0020\u0020\u0020\ufee5\ufef5\ufe8d\u0020\ufee5" +
                     "\ufe76\ufef7\ufe8d\u0020\ufee5\u0670\ufefb\ufe8d\u0020\ufee5\u0653\ufef5" +
                     "\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d\u0020\ufee5\u0654\ufef5\ufe8d\u0020" +
                     "\ufefc\ufecb"},
                    {lamAlefSpecialVLTR,
                     LETTERS_SHAPE_TASHKEEL_ISOLATED | TEXT_DIRECTION_VISUAL_LTR |
                     LENGTH_GROW_SHRINK,
                     "\u0020\ufee5\ufef5\ufe8d\u0020\ufee5\ufe76\ufef7\ufe8d\u0020\ufee5\u0670" +
                     "\ufefb\ufe8d\u0020\ufee5\u0653\ufef5\ufe8d\u0020\ufee5\u0655\ufef9\ufe8d" +
                     "\u0020\ufee5\u0654\ufef5\ufe8d\u0020\ufefc\ufecb"},
                    /* tashkeel special visual ltr */
                    {tashkeelSpecialVLTR,
                     LETTERS_SHAPE | TEXT_DIRECTION_VISUAL_LTR | LENGTH_FIXED_SPACES_NEAR,
                     "\ufef2\ufe91\ufeae\ufecb\u0020\ufef2\ufe91\ufe7c\ufeae\ufe77\ufecb\u0020" +
                     "\ufe72\ufef2\ufe91\ufeae\ufe79\ufecb\u0020\ufe8f\u0670\ufeae\u0670\ufecb" +
                     "\u0020\ufe8f\u0653\ufeae\u0653\ufecb\u0020\ufe8f\u0654\ufeae\u0654\ufecb" +
                     "\u0020\ufe8f\u0655\ufeae\u0655\ufecb\u0020"},
                    {tashkeelSpecialVLTR,
                     LETTERS_SHAPE_TASHKEEL_ISOLATED | TEXT_DIRECTION_VISUAL_LTR |
                     LENGTH_FIXED_SPACES_NEAR,
                     "\ufef2\ufe91\ufeae\ufecb\u0020\ufef2\ufe91\ufe7c\ufeae\ufe76\ufecb\u0020" +
                     "\ufe72\ufef2\ufe91\ufeae\ufe78\ufecb\u0020\ufe8f\u0670\ufeae\u0670\ufecb" +
                     "\u0020\ufe8f\u0653\ufeae\u0653\ufecb\u0020\ufe8f\u0654\ufeae\u0654\ufecb" +
                     "\u0020\ufe8f\u0655\ufeae\u0655\ufecb\u0020"},
                    {tashkeelShaddaRTL,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_BEGIN |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\u0020\ufeb7\ufe7d\ufee4\ufeb2"},
                    {tashkeelShaddaRTL,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_END |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\ufeb7\ufe7d\ufee4\ufeb2\u0020"},
                    {tashkeelShaddaRTL,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_RESIZE |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\ufeb7\ufe7d\ufee4\ufeb2"},
                    {tashkeelShaddaRTL,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_REPLACE_BY_TATWEEL |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\ufeb7\ufe7d\ufee4\u0640\ufeb2"},
                    {tashkeelShaddaLTR,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_BEGIN |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\u0020\ufeb2\ufee4\ufe7d\ufeb7"},
                    {tashkeelShaddaLTR,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_END |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\ufeb2\ufee4\ufe7d\ufeb7\u0020"},
                    {tashkeelShaddaLTR,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_RESIZE |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\ufeb2\ufee4\ufe7d\ufeb7"},
                    {tashkeelShaddaLTR,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_REPLACE_BY_TATWEEL |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\ufeb2\u0640\ufee4\ufe7d\ufeb7"},
                    {ArMathSym,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_BEGIN |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\uD83B\uDE00\uD83B\uDE01\uD83B\uDE02\uD83B\uDE03\u0020\uD83B\uDE24\uD83B" +
                     "\uDE05\uD83B\uDE06\u0020\uD83B\uDE07\uD83B\uDE08\uD83B\uDE09\u0020\uD83B" +
                     "\uDE0A\uD83B\uDE0B\uD83B\uDE0C\uD83B\uDE0D\u0020\uD83B\uDE0E\uD83B\uDE0F" +
                     "\uD83B\uDE10\uD83B\uDE11\u0020\uD83B\uDE12\uD83B\uDE13\uD83B\uDE14\uD83B" +
                     "\uDE15\u0020\uD83B\uDE16\uD83B\uDE17\uD83B\uDE18\u0020\uD83B\uDE19\uD83B" +
                     "\uDE1A\uD83B\uDE1B"},
                    {ArMathSymLooped,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_END |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\uD83B\uDE80\uD83B\uDE81\uD83B\uDE82\uD83B\uDE83\u0020\uD83B\uDE84\uD83B" +
                     "\uDE85\uD83B\uDE86\u0020\uD83B\uDE87\uD83B\uDE88\uD83B\uDE89\u0020\uD83B" +
                     "\uDE8B\uD83B\uDE8C\uD83B\uDE8D\u0020\uD83B\uDE8E\uD83B\uDE8F\uD83B\uDE90" +
                     "\uD83B\uDE91\u0020\uD83B\uDE92\uD83B\uDE93\uD83B\uDE94\uD83B\uDE95\u0020" +
                     "\uD83B\uDE96\uD83B\uDE97\uD83B\uDE98\u0020\uD83B\uDE99\uD83B\uDE9A\uD83B" +
                     "\uDE9B"},
                    {ArMathSymDoubleStruck,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_RESIZE|
                     ArabicShaping.TEXT_DIRECTION_VISUAL_RTL,
                     "\uD83B\uDEA1\uD83B\uDEA2\uD83B\uDEA3\u0020\uD83B\uDEA5\uD83B\uDEA6\u0020" +
                     "\uD83B\uDEA7\uD83B\uDEA8\uD83B\uDEA9\u0020\uD83B\uDEAB\uD83B\uDEAC\uD83B" +
                     "\uDEAD\u0020\uD83B\uDEAE\uD83B\uDEAF\uD83B\uDEB0\uD83B\uDEB1\u0020\uD83B" +
                     "\uDEB2\uD83B\uDEB3\uD83B\uDEB4\uD83B\uDEB5\u0020\uD83B\uDEB6\uD83B\uDEB7" +
                     "\uD83B\uDEB8\u0020\uD83B\uDEB9\uD83B\uDEBA\uD83B\uDEBB"},
                    {ArMathSymInitial,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_BEGIN |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\uD83B\uDE21\uD83B\uDE22\u0020\uD83B\uDE27\uD83B\uDE29\u0020\uD83B\uDE2A" +
                     "\uD83B\uDE2B\uD83B\uDE2C\uD83B\uDE2D\u0020\uD83B\uDE2E\uD83B\uDE2F\uD83B" +
                     "\uDE30\uD83B\uDE31\u0020\uD83B\uDE32\uD83B\uDE34\uD83B\uDE35\u0020\uD83B" +
                     "\uDE36\uD83B\uDE37\u0020\uD83B\uDE39\uD83B\uDE3B"},
                    {ArMathSymTailed,
                     ArabicShaping.LETTERS_SHAPE | ArabicShaping.TASHKEEL_END |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\uD83B\uDE42\uD83B\uDE47\uD83B\uDE49\uD83B\uDE4B\u0020\uD83B\uDE4D\uD83B" +
                     "\uDE4E\uD83B\uDE4F\u0020\uD83B\uDE51\uD83B\uDE52\uD83B\uDE54\uD83B\uDE57" +
                     "\u0020\uD83B\uDE59\uD83B\uDE5B\uD83B\uDE5D\uD83B\uDE5F"},
                    {ArMathSymStretched,
                     ArabicShaping.LETTERS_SHAPE|ArabicShaping.TASHKEEL_RESIZE |
                     ArabicShaping.TEXT_DIRECTION_VISUAL_LTR,
                     "\uD83B\uDE21\uFEB1\uD83B\uDE62\uFEE9"},
                     /* logical unshape */
                    {logicalUnshape,
                     LETTERS_UNSHAPE | TEXT_DIRECTION_LOGICAL | LENGTH_FIXED_SPACES_NEAR,
                     "\u0020\u0020\u0020\u0627\u0644\u0622\u0646\u0020\u0627\u0644\u0623\u0642" +
                     "\u0644\u0627\u0645\u0020\u0627\u0644\u0639\u0631\u0628\u064a\u0629\u0020" +
                     "\u0627\u0644\u062d\u0631\u0629\u0020\u0020\u0020\u0020"},
                    {logicalUnshape,
                     LETTERS_UNSHAPE | TEXT_DIRECTION_LOGICAL | LENGTH_FIXED_SPACES_AT_END,
                     "\u0020\u0020\u0020\u0627\u0644\u0622\u0020\u0646\u0020\u0627\u0644\u0623" +
                     "\u0020\u0642\u0644\u0627\u0020\u0645\u0020\u0627\u0644\u0639\u0631\u0628" +
                     "\u064a\u0629\u0020\u0627\u0644\u062d\u0631\u0629\u0020"},
                    {logicalUnshape,
                     LETTERS_UNSHAPE | TEXT_DIRECTION_LOGICAL | LENGTH_FIXED_SPACES_AT_BEGINNING,
                     "\u0627\u0644\u0622\u0020\u0646\u0020\u0627\u0644\u0623\u0020\u0642\u0644" +
                     "\u0627\u0020\u0645\u0020\u0627\u0644\u0639\u0631\u0628\u064a\u0629\u0020" +
                     "\u0627\u0644\u062d\u0631\u0629\u0020\u0020\u0020\u0020"},
                    {logicalUnshape,
                     LETTERS_UNSHAPE | TEXT_DIRECTION_LOGICAL | LENGTH_GROW_SHRINK,
                     "\u0020\u0020\u0020\u0627\u0644\u0622\u0020\u0646\u0020\u0627\u0644\u0623" +
                     "\u0020\u0642\u0644\u0627\u0020\u0645\u0020\u0627\u0644\u0639\u0631\u0628" +
                     "\u064a\u0629\u0020\u0627\u0644\u062d\u0631\u0629\u0020\u0020\u0020\u0020"},
                    /* numbers */
                    {numSource,
                     DIGITS_EN2AN | DIGIT_TYPE_AN,
                     "\u0661\u0627\u0662\u06f3\u0061\u0664"},
                    {numSource,
                     DIGITS_AN2EN | DIGIT_TYPE_AN_EXTENDED,
                    "\u0031\u0627\u0032\u0033\u0061\u0034"},
                    {numSource,
                     DIGITS_EN2AN_INIT_LR | DIGIT_TYPE_AN,
                     "\u0031\u0627\u0662\u06f3\u0061\u0034" },
                    {numSource,
                     DIGITS_EN2AN_INIT_AL | DIGIT_TYPE_AN_EXTENDED,
                     "\u06f1\u0627\u06f2\u06f3\u0061\u0034"},
                    {numSource,
                     DIGITS_EN2AN_INIT_LR | DIGIT_TYPE_AN | TEXT_DIRECTION_VISUAL_LTR,
                     "\u0661\u0627\u0032\u06f3\u0061\u0034"},
                    {numSource,
                     DIGITS_EN2AN_INIT_AL | DIGIT_TYPE_AN_EXTENDED | TEXT_DIRECTION_VISUAL_LTR,
                     "\u06f1\u0627\u0032\u06f3\u0061\u06f4"},
                    /* no-op */
                    {numSource, 0, numSource}
                });
        }

        @Test
        public void TestStandard() {
            Exception ex = null;
            String actual = null;
            ArabicShaping shaper = null;

            try {
                shaper = new ArabicShaping(flags);
                actual = shaper.shape(source);
            }
            catch(MissingResourceException e){
                throw e;
            }
            catch (IllegalStateException ie){
                warnln("IllegalStateException: "+ ie.toString());
                return;
            }
            catch (Exception e) {
                ex = e;
            }

            if (ex != null) {
                err("Error: Shaper " + shaper + "\n throws exception '" + ex + "'\n for input '" + source);
            } else if (!expected.equals(actual)) {
                StringBuffer buf = new StringBuffer();
                buf.append("Error: Shaper: " + shaper + "\n Input: " + source + "\n Actual: " + actual +
                           "\n Expected: " + expected + "\n");

                for (int i = 0; i < Math.max(expected.length(), actual.length()); ++i) {
                    String temp = Integer.toString(i);
                    if (temp.length() < 2) {
                        temp = " ".concat(temp);
                    }
                    char trg = i < expected.length() ? expected.charAt(i) : '\uffff';
                    char res = i < actual.length() ? actual.charAt(i) : '\uffff';

                    buf.append("[" + temp + "] ");
                    buf.append(escapedString("" + trg) + " ");
                    buf.append(escapedString("" + res) + " ");
                    if (trg != res) {
                        buf.append("***");
                    }
                    buf.append("\n");
                }
                err(buf.toString());
            }
        }

        private static String escapedString(String str) {
            if (str == null) {
                return null;
            }

            StringBuffer buf = new StringBuffer(str.length() * 6);
            for (int i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                buf.append("\\u");
                if (ch < 0x1000) {
                    buf.append('0');
                }
                if (ch < 0x0100) {
                    buf.append('0');
                }
                if (ch < 0x0010) {
                    buf.append('0');
                }
                buf.append(Integer.toHexString(ch));
            }
            return buf.toString();
        }
    }

    @RunWith(Parameterized.class)
    public static class PreflightDataTest extends CoreTestFmwk {
        private String source;
        private int flags;
        private int length;

        public PreflightDataTest(String source, int flags, int length) {
            this.source = source;
            this.flags = flags;
            this.length = length;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    {"\u0644\u0627", LETTERS_SHAPE | LENGTH_GROW_SHRINK, 1},
                    {"\u0644\u0627\u0031",
                     DIGITS_EN2AN | DIGIT_TYPE_AN_EXTENDED | LENGTH_GROW_SHRINK, 3},
                    {"\u0644\u0644", LETTERS_SHAPE | LENGTH_GROW_SHRINK, 2},
                    {"\ufef7", LETTERS_UNSHAPE | LENGTH_GROW_SHRINK, 2}
                });
        }

        @Test
        public void TestPreflight() {
            Exception ex = null;
            char src[] = null;
            int len = 0;
            ArabicShaping shaper = null;

            if (source != null) {
                src = source.toCharArray();
            }

            try {
                shaper = new ArabicShaping(flags);
                len = shaper.shape(src, 0, src.length, null, 0, 0);
            }
            catch (Exception e) {
                ex = e;
            }

            if (ex != null) {
                err("Error: Shaper " + shaper + "\n throws exception '" + ex + "'\n for input '" + source);
            } else if (length != len) {
                err("Error: Shaper " + shaper + "\n returns " + len + " characters for input '" +
                    source + "'\n Expected were " + length + " characters");
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class ErrorDataTest extends CoreTestFmwk {
        private String source;
        private int flags;
        private Class error;

        public ErrorDataTest(String source, int flags, Class error) {
            this.source = source;
            this.flags = flags;
            this.error = error;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    /* bad data */
                    {"\u0020\ufef7\u0644\u0020", LETTERS_UNSHAPE | LENGTH_FIXED_SPACES_NEAR,
                     ArabicShapingException.class},
                    {"\u0020\ufef7", LETTERS_UNSHAPE | LENGTH_FIXED_SPACES_AT_END,
                     ArabicShapingException.class},
                    {"\ufef7\u0020", LETTERS_UNSHAPE | LENGTH_FIXED_SPACES_AT_BEGINNING,
                     ArabicShapingException.class},
                    /* bad options */
                    {"\ufef7", 0xffffffff, IllegalArgumentException.class},
                    {"\ufef7", LETTERS_UNSHAPE | LENGTH_GROW_SHRINK, ArabicShapingException.class},
                    {null, LETTERS_UNSHAPE | LENGTH_FIXED_SPACES_AT_END,
                     IllegalArgumentException.class}
                });
        }

        @Test
        public void TestError() {
            Exception ex = null;
            char src[] = null;
            int len = 0;
            ArabicShaping shaper = null;

            if (source != null) {
                src = source.toCharArray();
                len = src.length;
            }

            try {
                shaper = new ArabicShaping(flags);
                shaper.shape(src, 0, len);
            }
            catch (Exception e) {
                ex = e;
            }

            if (!error.isInstance(ex)) {
                err("Error: Shaper " + shaper + "\n throws exception '" + ex + "'\n for input '" +
                    source + "'\n Expected exception: " + error);
            }
        }
    }
}
