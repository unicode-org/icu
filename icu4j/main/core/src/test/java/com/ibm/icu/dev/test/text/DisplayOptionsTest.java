// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.text;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.DisplayOptions;
import com.ibm.icu.text.DisplayOptions.Capitalization;
import com.ibm.icu.text.DisplayOptions.DisplayLength;
import com.ibm.icu.text.DisplayOptions.GrammaticalCase;
import com.ibm.icu.text.DisplayOptions.NameStyle;
import com.ibm.icu.text.DisplayOptions.NounClass;
import com.ibm.icu.text.DisplayOptions.PluralCategory;
import com.ibm.icu.text.DisplayOptions.SubstituteHandling;

/**
 * @test
 * @summary Test of DisplayOptions Class.
 */
@RunWith(JUnit4.class)
public class DisplayOptionsTest extends CoreTestFmwk {

    @Test
    public void TestDisplayOptionsDefault(){
        DisplayOptions displayOptions = DisplayOptions.builder().build();
        assertEquals("Test setting parameters", GrammaticalCase.UNDEFINED,
                displayOptions.getGrammaticalCase());
        assertEquals("Test default values: ", NounClass.UNDEFINED, displayOptions.getNounClass());
        assertEquals("Test default values: ", PluralCategory.UNDEFINED,
                displayOptions.getPluralCategory());
        assertEquals("Test default values: ", Capitalization.UNDEFINED,
                displayOptions.getCapitalization());
        assertEquals("Test default values: ", NameStyle.UNDEFINED, displayOptions.getNameStyle());
        assertEquals("Test default values: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test default values: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());
    }

    @Test
    public void TestDisplayOptionsEachElement() {
        DisplayOptions displayOptions = DisplayOptions.builder()
                .setGrammaticalCase(GrammaticalCase.ABLATIVE).build();
        assertEquals("Test setting parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());

        displayOptions = DisplayOptions.builder().setNounClass(NounClass.PERSONAL).build();
        assertEquals("Test setting parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());

        displayOptions = DisplayOptions.builder().setPluralCategory(PluralCategory.FEW).build();
        assertEquals("Test setting parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());

        displayOptions = DisplayOptions.builder()
                .setCapitalization(Capitalization.BEGINNING_OF_SENTENCE).build();
        assertEquals("Test setting parameters: ", Capitalization.BEGINNING_OF_SENTENCE,
                displayOptions.getCapitalization());

        displayOptions = DisplayOptions.builder().setNameStyle(NameStyle.STANDARD_NAMES).build();
        assertEquals("Test setting parameters: ", NameStyle.STANDARD_NAMES,
                displayOptions.getNameStyle());

        displayOptions = DisplayOptions.builder().setDisplayLength(DisplayLength.LENGTH_FULL)
                .build();
        assertEquals("Test setting parameters: ", DisplayLength.LENGTH_FULL,
                displayOptions.getDisplayLength());

        displayOptions = DisplayOptions.builder()
                .setSubstituteHandling(SubstituteHandling.NO_SUBSTITUTE).build();
        assertEquals("Test setting parameters: ", SubstituteHandling.NO_SUBSTITUTE,
                displayOptions.getSubstituteHandling());
    }

    @Test
    public void TestDisplayOptionsUpdating() {
        DisplayOptions displayOptions = DisplayOptions.builder()
                .setGrammaticalCase(GrammaticalCase.ABLATIVE).build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.UNDEFINED,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.UNDEFINED,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.UNDEFINED,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.UNDEFINED,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder().setNounClass(NounClass.PERSONAL).build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.UNDEFINED,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.UNDEFINED,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.UNDEFINED,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder().setPluralCategory(PluralCategory.FEW)
                .build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.UNDEFINED,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.UNDEFINED,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder()
                .setCapitalization(Capitalization.BEGINNING_OF_SENTENCE).build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.BEGINNING_OF_SENTENCE,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.UNDEFINED,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder().setNameStyle(NameStyle.STANDARD_NAMES)
                .build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.BEGINNING_OF_SENTENCE,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.STANDARD_NAMES,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.UNDEFINED,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder().setDisplayLength(DisplayLength.LENGTH_FULL)
                .build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.BEGINNING_OF_SENTENCE,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.STANDARD_NAMES,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.LENGTH_FULL,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.UNDEFINED,
                displayOptions.getSubstituteHandling());

        displayOptions = displayOptions.copyToBuilder()
                .setSubstituteHandling(SubstituteHandling.NO_SUBSTITUTE).build();
        assertEquals("Test updating parameters: ", GrammaticalCase.ABLATIVE,
                displayOptions.getGrammaticalCase());
        assertEquals("Test updating parameters: ", NounClass.PERSONAL,
                displayOptions.getNounClass());
        assertEquals("Test updating parameters: ", PluralCategory.FEW,
                displayOptions.getPluralCategory());
        assertEquals("Test updating parameters: ", Capitalization.BEGINNING_OF_SENTENCE,
                displayOptions.getCapitalization());
        assertEquals("Test updating parameters: ", NameStyle.STANDARD_NAMES,
                displayOptions.getNameStyle());
        assertEquals("Test updating parameters: ", DisplayLength.LENGTH_FULL,
                displayOptions.getDisplayLength());
        assertEquals("Test updating parameters: ", SubstituteHandling.NO_SUBSTITUTE,
                displayOptions.getSubstituteHandling());
    }

    @Test
    public void TestDisplayOptionsGetIdentifier() {
        assertEquals("test get identifier: ", "undefined",
                GrammaticalCase.UNDEFINED.getIdentifier());
        assertEquals("test get identifier: ", "ablative", GrammaticalCase.ABLATIVE.getIdentifier());
        assertEquals("test get identifier: ", "accusative",
                GrammaticalCase.ACCUSATIVE.getIdentifier());
        assertEquals("test get identifier: ", "comitative",
                GrammaticalCase.COMITATIVE.getIdentifier());
        assertEquals("test get identifier: ", "dative", GrammaticalCase.DATIVE.getIdentifier());
        assertEquals("test get identifier: ", "ergative", GrammaticalCase.ERGATIVE.getIdentifier());
        assertEquals("test get identifier: ", "genitive", GrammaticalCase.GENITIVE.getIdentifier());
        assertEquals("test get identifier: ", "instrumental",
                GrammaticalCase.INSTRUMENTAL.getIdentifier());
        assertEquals("test get identifier: ", "locative", GrammaticalCase.LOCATIVE.getIdentifier());
        assertEquals("test get identifier: ", "locative_copulative",
                GrammaticalCase.LOCATIVE_COPULATIVE.getIdentifier());
        assertEquals("test get identifier: ", "nominative",
                GrammaticalCase.NOMINATIVE.getIdentifier());
        assertEquals("test get identifier: ", "oblique", GrammaticalCase.OBLIQUE.getIdentifier());
        assertEquals("test get identifier: ", "prepositional",
                GrammaticalCase.PREPOSITIONAL.getIdentifier());
        assertEquals("test get identifier: ", "sociative",
                GrammaticalCase.SOCIATIVE.getIdentifier());
        assertEquals("test get identifier: ", "vocative", GrammaticalCase.VOCATIVE.getIdentifier());

        assertEquals("test get identifier: ", "undefined",
                PluralCategory.UNDEFINED.getIdentifier());
        assertEquals("test get identifier: ", "zero", PluralCategory.ZERO.getIdentifier());
        assertEquals("test get identifier: ", "one", PluralCategory.ONE.getIdentifier());
        assertEquals("test get identifier: ", "two", PluralCategory.TWO.getIdentifier());
        assertEquals("test get identifier: ", "few", PluralCategory.FEW.getIdentifier());
        assertEquals("test get identifier: ", "many", PluralCategory.MANY.getIdentifier());
        assertEquals("test get identifier: ", "other", PluralCategory.OTHER.getIdentifier());

        assertEquals("test get identifier: ", "undefined", NounClass.UNDEFINED.getIdentifier());
        assertEquals("test get identifier: ", "other", NounClass.OTHER.getIdentifier());
        assertEquals("test get identifier: ", "neuter", NounClass.NEUTER.getIdentifier());
        assertEquals("test get identifier: ", "feminine", NounClass.FEMININE.getIdentifier());
        assertEquals("test get identifier: ", "masculine", NounClass.MASCULINE.getIdentifier());
        assertEquals("test get identifier: ", "animate", NounClass.ANIMATE.getIdentifier());
        assertEquals("test get identifier: ", "inanimate", NounClass.INANIMATE.getIdentifier());
        assertEquals("test get identifier: ", "personal", NounClass.PERSONAL.getIdentifier());
        assertEquals("test get identifier: ", "common", NounClass.COMMON.getIdentifier());
    }

    @Test
    public void TestDisplayOptionsFromIdentifier() {
        assertEquals("test from identifier: ", GrammaticalCase.UNDEFINED,
                GrammaticalCase.fromIdentifier(""));
        assertEquals("test from identifier: ", GrammaticalCase.UNDEFINED,
                GrammaticalCase.fromIdentifier("undefined"));
        assertEquals("test from identifier: ", GrammaticalCase.ABLATIVE,
                GrammaticalCase.fromIdentifier("ablative"));
        assertEquals("test from identifier: ", GrammaticalCase.ACCUSATIVE,
                GrammaticalCase.fromIdentifier("accusative"));
        assertEquals("test from identifier: ", GrammaticalCase.COMITATIVE,
                GrammaticalCase.fromIdentifier("comitative"));
        assertEquals("test from identifier: ", GrammaticalCase.DATIVE,
                GrammaticalCase.fromIdentifier("dative"));
        assertEquals("test from identifier: ", GrammaticalCase.ERGATIVE,
                GrammaticalCase.fromIdentifier("ergative"));
        assertEquals("test from identifier: ", GrammaticalCase.GENITIVE,
                GrammaticalCase.fromIdentifier("genitive"));
        assertEquals("test from identifier: ", GrammaticalCase.INSTRUMENTAL,
                GrammaticalCase.fromIdentifier("instrumental"));
        assertEquals("test from identifier: ", GrammaticalCase.LOCATIVE,
                GrammaticalCase.fromIdentifier("locative"));
        assertEquals("test from identifier: ", GrammaticalCase.LOCATIVE_COPULATIVE,
                GrammaticalCase.fromIdentifier("locative_copulative"));
        assertEquals("test from identifier: ", GrammaticalCase.NOMINATIVE,
                GrammaticalCase.fromIdentifier("nominative"));
        assertEquals("test from identifier: ", GrammaticalCase.OBLIQUE,
                GrammaticalCase.fromIdentifier("oblique"));
        assertEquals("test from identifier: ", GrammaticalCase.PREPOSITIONAL,
                GrammaticalCase.fromIdentifier("prepositional"));
        assertEquals("test from identifier: ", GrammaticalCase.SOCIATIVE,
                GrammaticalCase.fromIdentifier("sociative"));
        assertEquals("test from identifier: ", GrammaticalCase.VOCATIVE,
                GrammaticalCase.fromIdentifier("vocative"));

        assertEquals("test from identifier: ", PluralCategory.UNDEFINED,
                PluralCategory.fromIdentifier(""));
        assertEquals("test from identifier: ", PluralCategory.UNDEFINED,
                PluralCategory.fromIdentifier("undefined"));
        assertEquals("test from identifier: ", PluralCategory.ZERO,
                PluralCategory.fromIdentifier("zero"));
        assertEquals("test from identifier: ", PluralCategory.ONE,
                PluralCategory.fromIdentifier("one"));
        assertEquals("test from identifier: ", PluralCategory.TWO,
                PluralCategory.fromIdentifier("two"));
        assertEquals("test from identifier: ", PluralCategory.FEW,
                PluralCategory.fromIdentifier("few"));
        assertEquals("test from identifier: ", PluralCategory.MANY,
                PluralCategory.fromIdentifier("many"));
        assertEquals("test from identifier: ", PluralCategory.OTHER,
                PluralCategory.fromIdentifier("other"));

        assertEquals("test from identifier: ", NounClass.UNDEFINED, NounClass.fromIdentifier(""));
        assertEquals("test from identifier: ", NounClass.UNDEFINED,
                NounClass.fromIdentifier("undefined"));
        assertEquals("test from identifier: ", NounClass.OTHER, NounClass.fromIdentifier("other"));
        assertEquals("test from identifier: ", NounClass.NEUTER,
                NounClass.fromIdentifier("neuter"));
        assertEquals("test from identifier: ", NounClass.FEMININE,
                NounClass.fromIdentifier("feminine"));
        assertEquals("test from identifier: ", NounClass.MASCULINE,
                NounClass.fromIdentifier("masculine"));
        assertEquals("test from identifier: ", NounClass.ANIMATE,
                NounClass.fromIdentifier("animate"));
        assertEquals("test from identifier: ", NounClass.INANIMATE,
                NounClass.fromIdentifier("inanimate"));
        assertEquals("test from identifier: ", NounClass.PERSONAL,
                NounClass.fromIdentifier("personal"));
        assertEquals("test from identifier: ", NounClass.COMMON,
                NounClass.fromIdentifier("common"));
    }
}
