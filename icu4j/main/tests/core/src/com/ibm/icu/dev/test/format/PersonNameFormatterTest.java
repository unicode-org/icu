// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.text.SimplePersonName;
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

@RunWith(JUnit4.class)
public class PersonNameFormatterTest extends TestFmwk{
    private static class NameAndTestCases {
        public String nameFields;
        public String[][] testCases;

        public NameAndTestCases(String nameFields, String[][] testCases) {
            this.nameFields = nameFields;
            this.testCases = testCases;
        }
    }

    private void executeTestCases(NameAndTestCases[] namesAndTestCases, boolean forDebugging) {
        for (NameAndTestCases nameAndTestCases : namesAndTestCases) {
            SimplePersonName name = new SimplePersonName(nameAndTestCases.nameFields);
            if (forDebugging) {
                System.out.println(nameAndTestCases.nameFields);
            }

            for (String[] testCase : nameAndTestCases.testCases) {
                ULocale formatterLocale = new ULocale(testCase[0]);
                PersonNameFormatter.Length formatterLength = PersonNameFormatter.Length.valueOf(testCase[1]);
                PersonNameFormatter.Usage formatterUsage = PersonNameFormatter.Usage.valueOf(testCase[2]);
                PersonNameFormatter.Formality formatterFormality = PersonNameFormatter.Formality.valueOf(testCase[3]);
                Set<PersonNameFormatter.Options> formatterOptions = makeOptionsSet(testCase[4]);
                String expectedResult = testCase[5];

                PersonNameFormatter formatter = new PersonNameFormatter(formatterLocale, formatterLength, formatterUsage, formatterFormality, formatterOptions);
                String actualResult = formatter.format(name);

                if (forDebugging) {
                    System.out.println("    " + formatterLocale + "," + formatterLength + "," + formatterUsage + "," + formatterFormality + "," + formatterOptions + " => " + actualResult);
                } else {
                    assertEquals("Wrong formatting result for " + nameAndTestCases.nameFields + "," + Arrays.toString(testCase), expectedResult, actualResult);
                }
            }
        }
    }

    private static Set<PersonNameFormatter.Options> makeOptionsSet(String optionsStr) {
        Set<PersonNameFormatter.Options> result = new HashSet<>();
        StringTokenizer tok = new StringTokenizer(optionsStr, ",");
        while (tok.hasMoreTokens()) {
            String optionStr = tok.nextToken();
            PersonNameFormatter.Options option = PersonNameFormatter.Options.valueOf(optionStr);
            result.add(option);
        }
        return result;
    }

    @Test
    public void TestEnglishName() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,prefix=Mr.,given=Richard,given-informal=Rich,given2=Theodore,surname=Gillam", new String[][] {
                // test all the different combinations of parameters with the normal name order
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Richard Theodore Gillam" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "",              "Rich Gillam" },
                { "en_US", "LONG",   "ADDRESSING", "FORMAL",   "",              "Mr. Gillam" },
                { "en_US", "LONG",   "ADDRESSING", "INFORMAL", "",              "Rich" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "",              "Richard T. Gillam" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "",              "Rich Gillam" },
                { "en_US", "MEDIUM", "ADDRESSING", "FORMAL",   "",              "Mr. Gillam" },
                { "en_US", "MEDIUM", "ADDRESSING", "INFORMAL", "",              "Rich" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "R. T. Gillam" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "Rich G." },
                { "en_US", "SHORT",  "ADDRESSING", "FORMAL",   "",              "Mr. Gillam" },
                { "en_US", "SHORT",  "ADDRESSING", "INFORMAL", "",              "Rich" },

                // test all the different combinations of parameters for "sorting" order
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING",       "Gillam, Richard Theodore" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING",       "Gillam, Rich" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING",       "Gillam, Richard T." },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING",       "Gillam, Rich" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING",       "Gillam, R. T." },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING",       "Gillam, Rich" },

                // we don't really support ADDRESSING in conjunction with SORTING-- it should always
                // do the same thing as REFERRING
                { "en_US", "LONG",   "ADDRESSING", "FORMAL",   "SORTING",       "Gillam, Richard Theodore" },
                { "en_US", "LONG",   "ADDRESSING", "INFORMAL", "SORTING",       "Gillam, Rich" },
                { "en_US", "MEDIUM", "ADDRESSING", "FORMAL",   "SORTING",       "Gillam, Richard T." },
                { "en_US", "MEDIUM", "ADDRESSING", "INFORMAL", "SORTING",       "Gillam, Rich" },
                { "en_US", "SHORT",  "ADDRESSING", "FORMAL",   "SORTING",       "Gillam, R. T." },
                { "en_US", "SHORT",  "ADDRESSING", "INFORMAL", "SORTING",       "Gillam, Rich" },

                // finally, try the different variations of MONOGRAM
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "RTG" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "RG" },
                { "en_US", "MEDIUM", "MONOGRAM",   "FORMAL",   "",              "G" },
                { "en_US", "MEDIUM", "MONOGRAM",   "INFORMAL", "",              "R" },
                { "en_US", "SHORT",  "MONOGRAM",   "FORMAL",   "",              "G" },
                { "en_US", "SHORT",  "MONOGRAM",   "INFORMAL", "",              "R" },

                // and again, we don't support SORTING for monograms, so it should also do the
                // same thing as GIVEN_FIRST
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "SORTING",       "RTG" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "SORTING",       "RG" },
                { "en_US", "MEDIUM", "MONOGRAM",   "FORMAL",   "SORTING",       "G" },
                { "en_US", "MEDIUM", "MONOGRAM",   "INFORMAL", "SORTING",       "R" },
                { "en_US", "SHORT",  "MONOGRAM",   "FORMAL",   "SORTING",       "G" },
                { "en_US", "SHORT",  "MONOGRAM",   "INFORMAL", "SORTING",       "R" },
            })
        }, false);
    }

    @Test
    public void TestPrefixCore() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,given=Willem,surname-prefix=van der,surname-core=Plas", new String[][] {
                // for normal formatting, the {surname} field is just "{surname-prefix} {surname-core}"
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Willem van der Plas" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "",              "Willem van der Plas" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "",              "Willem van der Plas" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "",              "Willem van der Plas" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "W. van der Plas" },

                // for FORMAL SORTING, we sort by "surname-core", with "surname-prefix" at the end
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING",       "Plas, Willem van der" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING",       "Plas, Willem van der" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING",       "Plas, W. van der" },

                // but for INFORMAL SORTING, we keep the surname together and sort by the prefix
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },

                // the default (English) logic for initials doesn't do anything special with the surname-prefix--
                // it gets initials too, which is probably wrong
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "Willem v. d. P." },

                // and (English) monogram generation doesn't do anything special with the prefix either
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "WV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "WV" },

                // but Dutch monogram generation _does_ handle the prefix specially
                { "nl_NL", "LONG",   "MONOGRAM",   "FORMAL",   "",              "WvP" },
                { "nl_NL", "LONG",   "MONOGRAM",   "INFORMAL", "",              "WvP" },
            }),
            new NameAndTestCases("locale=en_US,given=Willem,surname=van der Plas", new String[][] {
                // if we just use the "surname" field instead of "surname-prefix" and "surname-core", everything's
                // the same, except (obviously) for the cases where we were doing something special with the
                // prefix and core
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Willem van der Plas" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "W. van der Plas" },

                // for example, SORTING works the same way regardless of formality
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING",       "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING",       "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING",       "van der Plas, W." },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING",       "van der Plas, Willem" },

                // and monogram generation works the same in English and Dutch
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "WV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "WV" },
                { "nl_NL", "LONG",   "MONOGRAM",   "FORMAL",   "",              "WV" },
                { "nl_NL", "LONG",   "MONOGRAM",   "INFORMAL", "",              "WV" },
            }),
            new NameAndTestCases("locale=en_US,given=Willem,surname-prefix=van der,surname-core=Plas,surname-initial=vdP.,surname-monogram=vdP", new String[][] {
                // we can work around the initial generation by providing a "surname-initial" field in the name object
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "Willem vdP." },

                // we could also (theoretically) work around the monogram-generation problem in English in the same way
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "WVDP" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "WVDP" },
            }),
        }, false);
    }

    @Test
    public void TestInitialGeneration() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,given=George,given2=Herbert Walker,surname=Bush", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "George Herbert Walker Bush" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "",              "George H. W. Bush" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "G. H. W. Bush" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "George B." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "GHB" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "GB" },
            }),
            new NameAndTestCases("locale=en_US,given=Ralph,surname=Vaughan Williams", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Ralph Vaughan Williams" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "",              "Ralph Vaughan Williams" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "R. Vaughan Williams" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "Ralph V. W." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "RV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "RV" },
            }),
            new NameAndTestCases("locale=en_US,given=John Paul,given2=Stephen David George,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "John Paul Stephen David George Smith" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "",              "John Paul S. D. G. Smith" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "",              "J. P. S. D. G. Smith" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "",              "John Paul S." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "",              "JSS" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "",              "JS" },
            }),
        }, false);
    }

    @Test
    public void TestLiteralTextElision() {
        executeTestCases(new NameAndTestCases[]{
            // literal text elision is difficult to test with the real locale data, although this is a start
            // perhaps we could add an API for debugging that lets us pass in real pattern strings, but I'd like to stay away from that
            new NameAndTestCases("locale=en_US,given=John,given2=Paul,surname=Smith,suffix=Jr.", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "John Paul Smith Jr." },
            }),
            new NameAndTestCases("locale=en_US,given=John,given2=Paul,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "John Paul Smith" },
            }),
            new NameAndTestCases("locale=en_US,given2=Paul,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Paul Smith" },
            }),
            new NameAndTestCases("locale=en_US,given2=Paul", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Paul" },
            }),
            new NameAndTestCases("locale=en_US,given=John", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "John" },
            }),
            new NameAndTestCases("locale=en_US,given=John,suffix=Jr.", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "John Jr." },
            }),
        }, false);
    }

    @Test
    public void TestMultiplePatterns() {
        executeTestCases(new NameAndTestCases[]{
            // the Spanish rules have two name patterns for many of the sorting cases: one to use if the surname2
            // field is populated and one to use if not-- these allow the comma between the fields to be displayed
            // in the right place.  This test checks to make sure we're using the right pattern based on which
            // fields are present in the actual name
            new NameAndTestCases("locale=es_ES,given=Andrés,given2=Manuel,surname=López,surname2=Obrador", new String[][] {
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "",              "Andrés Manuel López Obrador" },
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "SORTING"    ,   "López Obrador, Andrés Manuel" },
            }),
            new NameAndTestCases("locale=es_ES,given=Andrés,given2=Manuel,surname=López", new String[][] {
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "",              "Andrés Manuel López" },
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "SORTING"    ,   "López, Andrés Manuel" },
            }),
        }, false);
    }

    @Test
    public void TestNameOrder() {
        executeTestCases(new NameAndTestCases[]{
            // the name's locale is used to determine the field order.  For the English name formatter, if the
            // name is English, the order is GN first.  If it's Japanese, it's SN first.  This is true whether the
            // Japanese name is written in Latin letters or Han characters
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Shinzo Abe" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Abe Shinzo" },
            }),
            new NameAndTestCases("locale=ja_JP,given=晋三,surname=安倍", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "安倍 晋三" },
            }),

            // the name can also declare its order directly, with the optional "preferredOrder" field.  If it does this,
            // the value of that field holds for all formatter locales and overrides determining the order
            // by looking at the name's locale
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe,preferredOrder=surnameFirst", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Abe Shinzo" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe,preferredOrder=givenFirst", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",              "Shinzo Abe" },
            }),
        }, false);
    }

    @Test
    public void TestCapitalizedSurname() {
        executeTestCases(new NameAndTestCases[]{
            // the SURNAME_ALLCAPS option does just what it says: it causes the surname field
            // to be displayed in all caps
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",                "Shinzo Abe" },
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SURNAME_ALLCAPS", "Shinzo ABE" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",                "Abe Shinzo" },
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SURNAME_ALLCAPS", "ABE Shinzo" },
            }),
        }, false);
    }

    @Test
    public void TestNameSpacing() {
        executeTestCases(new NameAndTestCases[]{
            // if the formatter locale uses spaces, the result will use its formats (complete with spaces),
            // regardless of locale
            new NameAndTestCases("locale=ja_JP,given=Hayao,surname=Miyazaki", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",                "Miyazaki Hayao" },
            }),
            new NameAndTestCases("locale=ja_JP,given=駿,surname=宮崎", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "",                "宮崎 駿" },
            }),

            // if the formatter locale doesn't use spaces and the name's locale doesn't either, just use
            // the native formatter
            new NameAndTestCases("locale=ja_JP,given=駿,surname=宮崎", new String[][] {
                // (the Japanese name formatter actually inserts a space even for native names)
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "",                "宮崎 駿" },
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "",                "宮崎駿" },
            }),

            // if the formatter locale doesn't use spaces and the name's locale does, use the name locale's formatter,
            // but if the name is still using the formatter locale's script, use the native formatter's
            // "foreign space replacement" character instead of spaces
            new NameAndTestCases("locale=en_US,given=Albert,surname=Einstein", new String[][] {
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "",                "Albert Einstein" },
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "",                "Albert Einstein" },
            }),
            new NameAndTestCases("locale=en_US,given=アルベルト,surname=アインシュタイン", new String[][] {
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "",                "アルベルト・アインシュタイン" },
            }),
            new NameAndTestCases("locale=en_US,given=阿尔伯特,surname=爱因斯坦", new String[][] {
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "",                "阿尔伯特·爱因斯坦" },
            }),
        }, false);
    }

    // need tests (and implementation?) for:
    // - foreign space replacement
}
