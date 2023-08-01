// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.PersonName;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.text.SimplePersonName;

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

    private SimplePersonName buildPersonName(String fieldsAndValues) {
        SimplePersonName.Builder builder = SimplePersonName.builder();

        StringTokenizer entrySplitter = new StringTokenizer(fieldsAndValues, ",");
        while (entrySplitter.hasMoreTokens()) {
            String entry = entrySplitter.nextToken();
            int equalPos = entry.indexOf('=');
            if (equalPos < 0) {
                throw new IllegalArgumentException("No = found in name field entry");
            }
            String fieldName = entry.substring(0, equalPos);
            String fieldValue = entry.substring(equalPos + 1);

            if (fieldName.equals("locale")) {
                // cheating here, because java.util.Locale doesn't have a constructor that parses an ICU-style
                // locale ID
                builder.setLocale(Locale.forLanguageTag(fieldValue.replace("_", "-")));
            } else if (fieldName.equals("preferredOrder")) {
                if (fieldValue.equals("givenFirst")) {
                    builder.setPreferredOrder(PersonName.PreferredOrder.GIVEN_FIRST);
                } else if (fieldValue.equals("surnameFirst")) {
                    builder.setPreferredOrder(PersonName.PreferredOrder.SURNAME_FIRST);
                }
            } else if (fieldName.indexOf('-') < 0) {
                builder.addField(PersonName.NameField.forString(fieldName), null, fieldValue);
            } else {
                StringTokenizer fieldNameSplitter = new StringTokenizer(fieldName, "-");
                Set<PersonName.FieldModifier> modifiers = new HashSet<>();
                PersonName.NameField fieldID = PersonName.NameField.forString(fieldNameSplitter.nextToken());
                while (fieldNameSplitter.hasMoreTokens()) {
                    modifiers.add(PersonName.FieldModifier.forString(fieldNameSplitter.nextToken()));
                }
                builder.addField(fieldID, modifiers, fieldValue);
            }
        }
        return builder.build();
    }

    private void executeTestCases(NameAndTestCases[] namesAndTestCases, boolean forDebugging) {
        for (NameAndTestCases nameAndTestCases : namesAndTestCases) {
            SimplePersonName name = buildPersonName(nameAndTestCases.nameFields);
            if (forDebugging) {
                System.out.println(nameAndTestCases.nameFields);
            }

            for (String[] testCase : nameAndTestCases.testCases) {
                Locale formatterLocale = Locale.forLanguageTag(testCase[0].replace('_', '-'));
                PersonNameFormatter.Length formatterLength = PersonNameFormatter.Length.valueOf(testCase[1]);
                PersonNameFormatter.Usage formatterUsage = PersonNameFormatter.Usage.valueOf(testCase[2]);
                PersonNameFormatter.Formality formatterFormality = PersonNameFormatter.Formality.valueOf(testCase[3]);
                PersonNameFormatter.DisplayOrder formatterDisplayOrder = PersonNameFormatter.DisplayOrder.valueOf(testCase[4]);
                boolean surnameAllCaps = testCase[5].equals("SURNAME_ALLCAPS");
                String expectedResult = testCase[6];

                PersonNameFormatter formatter = PersonNameFormatter.builder().setLocale(formatterLocale).setLength(formatterLength).
                        setUsage(formatterUsage).setFormality(formatterFormality).setDisplayOrder(formatterDisplayOrder).
                        setSurnameAllCaps(surnameAllCaps).build();
                String actualResult = formatter.formatToString(name);

                if (forDebugging) {
                    System.out.println("    " + formatterLocale + "," + formatterLength + "," + formatterUsage + "," + formatterFormality + "," + formatterDisplayOrder + "," + surnameAllCaps + " => " + actualResult);
                } else {
                    assertEquals("Wrong formatting result for " + nameAndTestCases.nameFields + "," + Arrays.toString(testCase), expectedResult, actualResult);
                }
            }
        }
    }

    @Test
    public void TestEnglishName() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,title=Mr.,given=Richard,given-informal=Rich,given2=Theodore,surname=Gillam", new String[][] {
                // test all the different combinations of parameters with the normal name order
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Mr. Richard Theodore Gillam" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "DEFAULT", "", "Rich Gillam" },
                { "en_US", "LONG",   "ADDRESSING", "FORMAL",   "DEFAULT", "", "Mr. Gillam" },
                { "en_US", "LONG",   "ADDRESSING", "INFORMAL", "DEFAULT", "", "Rich" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "DEFAULT", "", "Richard T. Gillam" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "DEFAULT", "", "Rich Gillam" },
                { "en_US", "MEDIUM", "ADDRESSING", "FORMAL",   "DEFAULT", "", "Mr. Gillam" },
                { "en_US", "MEDIUM", "ADDRESSING", "INFORMAL", "DEFAULT", "", "Rich" },
                //{ "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "R. T. Gillam" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "R.T. Gillam" }, // result changed with CLDR 43-alpha1
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "Rich G." },
                { "en_US", "SHORT",  "ADDRESSING", "FORMAL",   "DEFAULT", "", "Mr. Gillam" },
                { "en_US", "SHORT",  "ADDRESSING", "INFORMAL", "DEFAULT", "", "Rich" },

                // test all the different combinations of parameters for "sorting" order
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING", "", "Gillam, Richard Theodore" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING", "", "Gillam, Rich" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING", "", "Gillam, Richard T." },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING", "", "Gillam, Rich" },
                //{ "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING", "", "Gillam, R. T." },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING", "", "Gillam, R.T." }, // result changed with CLDR 43-alpha1
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING", "", "Gillam, Rich" },

                // we don't really support ADDRESSING in conjunction with SORTING-- it should always
                // do the same thing as REFERRING
                { "en_US", "LONG",   "ADDRESSING", "FORMAL",   "SORTING", "", "Gillam, Richard Theodore" },
                { "en_US", "LONG",   "ADDRESSING", "INFORMAL", "SORTING", "", "Gillam, Rich" },
                { "en_US", "MEDIUM", "ADDRESSING", "FORMAL",   "SORTING", "", "Gillam, Richard T." },
                { "en_US", "MEDIUM", "ADDRESSING", "INFORMAL", "SORTING", "", "Gillam, Rich" },
                //{ "en_US", "SHORT",  "ADDRESSING", "FORMAL",   "SORTING", "", "Gillam, R. T." },
                { "en_US", "SHORT",  "ADDRESSING", "FORMAL",   "SORTING", "", "Gillam, R.T." }, // result changed with CLDR 43-alpha1
                { "en_US", "SHORT",  "ADDRESSING", "INFORMAL", "SORTING", "", "Gillam, Rich" },

                // finally, try the different variations of MONOGRAM
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "RTG" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "RG" },
                { "en_US", "MEDIUM", "MONOGRAM",   "FORMAL",   "DEFAULT", "", "G" },
                { "en_US", "MEDIUM", "MONOGRAM",   "INFORMAL", "DEFAULT", "", "R" },
                { "en_US", "SHORT",  "MONOGRAM",   "FORMAL",   "DEFAULT", "", "G" },
                { "en_US", "SHORT",  "MONOGRAM",   "INFORMAL", "DEFAULT", "", "R" },

                // and again, we don't support SORTING for monograms, so it should also do the
                // same thing as GIVEN_FIRST
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "SORTING", "", "RTG" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "SORTING", "", "RG" },
                { "en_US", "MEDIUM", "MONOGRAM",   "FORMAL",   "SORTING", "", "G" },
                { "en_US", "MEDIUM", "MONOGRAM",   "INFORMAL", "SORTING", "", "R" },
                { "en_US", "SHORT",  "MONOGRAM",   "FORMAL",   "SORTING", "", "G" },
                { "en_US", "SHORT",  "MONOGRAM",   "INFORMAL", "SORTING", "", "R" },
            })
        }, false);
    }

    @Test
    public void TestPrefixCore() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,given=Willem,surname-prefix=van der,surname-core=Plas", new String[][] {
                // for normal formatting, the {surname} field is just "{surname-prefix} {surname-core}"
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Willem van der Plas" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "DEFAULT", "", "Willem van der Plas" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "DEFAULT", "", "Willem van der Plas" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "DEFAULT", "", "Willem van der Plas" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "W. van der Plas" },

                // for FORMAL SORTING, we sort by "surname-core", with "surname-prefix" at the end
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING", "", "Plas, Willem van der" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING", "", "Plas, Willem van der" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING", "", "Plas, W. van der" },

                // but for INFORMAL SORTING, we keep the surname together and sort by the prefix
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },

                // the default (English) logic for initials doesn't do anything special with the surname-prefix--
                // it gets initials too, which is probably wrong
                //{ "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "Willem v. d. P." },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "Willem v.d.P." }, // result changed with CLDR 43-alpha1

                // and (English) monogram generation doesn't do anything special with the prefix either
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WV" },

                // but Dutch monogram generation _does_ handle the prefix specially
//                { "nl_NL", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WvP" },   Temporary change the test because data was set to provisional
//                { "nl_NL", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WvP" },
                { "nl_NL", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WV" },
                { "nl_NL", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WV" },
            }),
            new NameAndTestCases("locale=en_US,given=Willem,surname=van der Plas", new String[][] {
                // if we just use the "surname" field instead of "surname-prefix" and "surname-core", everything's
                // the same, except (obviously) for the cases where we were doing something special with the
                // prefix and core
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Willem van der Plas" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "W. van der Plas" },

                // for example, SORTING works the same way regardless of formality
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "SORTING", "", "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "SORTING", "", "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING", "", "van der Plas, W." },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "SORTING", "", "van der Plas, Willem" },

                // and monogram generation works the same in English and Dutch
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WV" },
                { "nl_NL", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WV" },
                { "nl_NL", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WV" },
            }),
            new NameAndTestCases("locale=en_US,given=Willem,surname-prefix=van der,surname-core=Plas,surname-initial=vdP.,surname-monogram=vdP", new String[][] {
                // we can work around the initial generation by providing a "surname-initial" field in the name object
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "Willem vdP." },

                // we could also (theoretically) work around the monogram-generation problem in English in the same way
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "WVDP" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "WVDP" },
            }),
            new NameAndTestCases("locale=en_US,given=John,surname-core=Smith", new String[][] {
                // if the PersonName object just fills in the "surname-core" field, treat it as the "surname" field
                { "en_US", "LONG",   "REFERRING",  "FORMAL", "DEFAULT", "", "John Smith" },
                { "en_US", "LONG",   "REFERRING",  "INFORMAL", "DEFAULT", "", "John Smith" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL", "DEFAULT", "", "John Smith" },
                { "en_US", "MEDIUM", "REFERRING",  "INFORMAL", "DEFAULT", "", "John Smith" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL", "DEFAULT", "", "J. Smith" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "John S." },
            }),
        }, false);
    }

    @Test
    public void TestInitialGeneration() {
        executeTestCases(new NameAndTestCases[]{
            new NameAndTestCases("locale=en_US,given=George,given2=Herbert Walker,surname=Bush", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "George Herbert Walker Bush" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "DEFAULT", "", "George H.W. Bush" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "G.H.W. Bush" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "George B." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "GHB" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "GB" },
            }),
            new NameAndTestCases("locale=en_US,given=Ralph,surname=Vaughan Williams", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Ralph Vaughan Williams" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "DEFAULT", "", "Ralph Vaughan Williams" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "R. Vaughan Williams" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "Ralph V.W." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "RV" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "RV" },
            }),
            new NameAndTestCases("locale=en_US,given=John Paul,given2=Stephen David George,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "John Paul Stephen David George Smith" },
                { "en_US", "MEDIUM", "REFERRING",  "FORMAL",   "DEFAULT", "", "John Paul S.D.G. Smith" },
                { "en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "J.P.S.D.G. Smith" },
                { "en_US", "SHORT",  "REFERRING",  "INFORMAL", "DEFAULT", "", "John Paul S." },
                { "en_US", "LONG",   "MONOGRAM",   "FORMAL",   "DEFAULT", "", "JSS" },
                { "en_US", "LONG",   "MONOGRAM",   "INFORMAL", "DEFAULT", "", "JS" },
            }),
        }, false);
    }

    @Test
    public void TestLiteralTextElision() {
        executeTestCases(new NameAndTestCases[]{
            // literal text elision is difficult to test with the real locale data, although this is a start
            // perhaps we could add an API for debugging that lets us pass in real pattern strings, but I'd like to stay away from that
            new NameAndTestCases("locale=en_US,given=John,given2=Paul,surname=Smith,generation=Jr.", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "John Paul Smith Jr." },
            }),
            new NameAndTestCases("locale=en_US,given=John,given2=Paul,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "John Paul Smith" },
            }),
            new NameAndTestCases("locale=en_US,given2=Paul,surname=Smith", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Paul Smith" },
            }),
            new NameAndTestCases("locale=en_US,given2=Paul", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Paul" },
            }),
            new NameAndTestCases("locale=en_US,given=John", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "John" },
            }),
            new NameAndTestCases("locale=en_US,given=John,generation=Jr.", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "John Jr." },
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
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Andrés Manuel López" },
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "SORTING", "", "López Obrador, Andrés Manuel" },
            }),
            new NameAndTestCases("locale=es_ES,given=Andrés,given2=Manuel,surname=López", new String[][] {
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Andrés Manuel López" },
                    { "es_ES", "LONG",   "REFERRING",  "FORMAL",   "SORTING", "", "López, Andrés Manuel" },
            }),
        }, false);
    }

    @Test
    public void TestNameOrder() {
        executeTestCases(new NameAndTestCases[]{
            // the name's locale is used to determine the field order.  For the English name formatter, if the
            // name is English, the order is GN first.  If it's Japanese, it's SN first.  And if the name is written
            // in Japanese characters, we just use the Japanese formatter.
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Shinzo Abe" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Abe Shinzo" },
            }),
            new NameAndTestCases("locale=ja_JP,given=晋三,surname=安倍", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "安倍晋三" },
            }),

            // the name can also declare its order directly, with the optional "preferredOrder" field.  If it does this,
            // the value of that field holds for all formatter locales and overrides determining the order
            // by looking at the name's locale
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe,preferredOrder=surnameFirst", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Abe Shinzo" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe,preferredOrder=givenFirst", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Shinzo Abe" },
            }),
        }, false);
    }

    @Test
    public void TestCapitalizedSurname() {
        executeTestCases(new NameAndTestCases[]{
            // the SURNAME_ALLCAPS option does just what it says: it causes the surname field
            // to be displayed in all caps
            new NameAndTestCases("locale=en_US,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "",                 "Shinzo Abe" },
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "SURNAME_ALLCAPS",  "Shinzo ABE" },
            }),
            new NameAndTestCases("locale=ja_JP,given=Shinzo,surname=Abe", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "",                 "Abe Shinzo" },
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "SURNAME_ALLCAPS",  "ABE Shinzo" },
            }),
        }, false);
    }

    @Test
    public void TestNameSpacing() {
        executeTestCases(new NameAndTestCases[]{
            // if the name uses the same characters as the formatter locale, even if the name locale doesn't
            // match (i.e., the name is transliterated), we use the formatter's format and the name's
            // field order
            new NameAndTestCases("locale=ja_JP,given=Hayao,surname=Miyazaki", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Miyazaki Hayao" },
            }),

            // if the name is in a script the formatter's locale doesn't use, we just use a formatter for
            // whatever the name locale is
            new NameAndTestCases("locale=ja_JP,given=駿,surname=宮崎", new String[][] {
                { "en_US", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "宮崎駿" },
            }),
            new NameAndTestCases("locale=en_US,given=Albert,surname=Einstein", new String[][] {
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Albert Einstein" },
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "Albert Einstein" },
            }),

            // if the name is in a script the formatter's locale does use, we use it, but if the name locale's
            // language doesn't match the formatter locale's language, we replace any spaces in the result
            // with the foreignSpaceReplacement character
            new NameAndTestCases("locale=en_US,given=アルベルト,surname=アインシュタイン", new String[][] {
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "アルベルト・アインシュタイン" },
            }),
            new NameAndTestCases("locale=en_US,given=阿尔伯特,surname=爱因斯坦", new String[][] {
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "阿尔伯特·爱因斯坦" },
            }),

            // if the name's script and locale both match the formatter, we format as normal, but replace
            // any spaces in the result with the nativeSpaceReplacement character (which, for Japanese,
            // is the empty string, giving us the name without spaces)
            new NameAndTestCases("locale=ja_JP,given=駿,surname=宮崎", new String[][] {
                { "ja_JP", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "宮崎駿" },
                { "zh_CN", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "宮崎駿" },
            }),
            // (Thai, despite not using spaces between words, DOES use spaces between the given name and surname_
            new NameAndTestCases("locale=th_TH,given=ไอริณ,surname=กล้าหาญ", new String[][] {
                { "th_TH", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "ไอริณ กล้าหาญ" },
            }),
            // (Lao, on the other hand, does NOT put a space between the given name and surname)
            new NameAndTestCases("locale=lo_LA,given=ໄອຣີນ,surname=ແອດເລີ", new String[][] {
                { "lo_LA", "LONG",   "REFERRING",  "FORMAL",   "DEFAULT", "", "ໄອຣີນແອດເລີ" },
            }),
        }, false);
    }

    @Test
    public void TestScriptGuessing() {
        executeTestCases(new NameAndTestCases[]{
            // here, we're leaving out the locale on the name object.  In the first case, we
            // see the Latin letters and assume English, giving us GN-first ordering.  In the
            // second, we see the Han characters and guess Japanese, giving us SN-first ordering
            // (and the Japanese format with no space between the fields).
            new NameAndTestCases("given=Hayao,surname=Miyazaki", new String[][]{
                    {"en_US", "LONG", "REFERRING", "FORMAL", "DEFAULT", "", "Hayao Miyazaki"},
            }),
            new NameAndTestCases("given=駿,surname=宮崎", new String[][]{
                    {"en_US", "LONG", "REFERRING", "FORMAL", "DEFAULT", "", "宮崎駿"},
            }),
        }, false);
    }

    @Test
    public void TestMissingSurname() {
        executeTestCases(new NameAndTestCases[]{
                // test handling of monomyns: names that only have a given name.  Formatting patterns that only
                // use the surname field will display as empty (or, in some of the examples below, with just
                // the title) unless we do something special.  The special thing we do is that when the pattern
                // has no given-name field and the name object has no surname field, we behave as though the
                // contents of the given-name field are in the surname field.  (Note that this only happens
                // for the "given" and "surname" fields; "given2" and "surname2" don't have this logic.)
                new NameAndTestCases("title=Ms.,given=Zendaya", new String[][]{
                        {"en_US", "MEDIUM", "ADDRESSING", "FORMAL",   "DEFAULT", "", "Ms. Zendaya"},
                        {"en_US", "SHORT",  "ADDRESSING", "FORMAL",   "DEFAULT", "", "Ms. Zendaya"},
                        {"en_US", "MEDIUM", "ADDRESSING", "INFORMAL", "DEFAULT", "", "Zendaya"},
                        {"en_US", "SHORT",  "ADDRESSING", "INFORMAL", "DEFAULT", "", "Zendaya"},
                        {"en_US", "SHORT",  "MONOGRAM",   "FORMAL",   "DEFAULT", "", "Z"},
                        {"en_US", "SHORT",  "REFERRING",  "FORMAL",   "DEFAULT", "", "Zendaya"},
                        {"en_US", "SHORT",  "REFERRING",  "FORMAL",   "SORTING", "", "Zendaya"},
                }),
        }, false);
    }

    @Test
    public void TestLiteralTextElision2() {
        // a more extensive text of the literal text elision logic
        PersonNameFormatter pnf = new PersonNameFormatter(Locale.US, new String[] {
            "1{title}1 2{given}2 3{given2}3 4{surname}4 5{surname2}5 6{generation}6"
        }, null, null, null);

        String[][] testCases = new String[][] {
            { "locale=en_US,title=Dr.,given=Richard,given2=Theodore,surname=Gillam,surname2=Morgan,generation=III", "1Dr.1 2Richard2 3Theodore3 4Gillam4 5Morgan5 6III6" },
            { "locale=en_US,title=Mr.,given=Richard,given2=Theodore,surname=Gillam", "1Mr.1 2Richard2 3Theodore3 4Gillam" },
            { "locale=en_US,given=Richard,given2=Theodore,surname=Gillam",            "Richard2 3Theodore3 4Gillam" },
            { "locale=en_US,given=Richard,surname=Gillam",                            "Richard2 4Gillam" },
            { "locale=en_US,given=Richard",                                           "Richard" },
            { "locale=en_US,title=Dr.,generation=III",                                "1Dr.1 6III6" }
        };

        for (String[] testCase : testCases) {
            SimplePersonName name = buildPersonName(testCase[0]);
            String expectedResult = testCase[1];
            String actualResult = pnf.formatToString(name);

            assertEquals("Wrong result", expectedResult, actualResult);
        }
    }

    @Test
    public void TestPatternSelection() {
        // a more extensive test of the logic that selects an appropriate pattern when the formatter has more than one
        PersonNameFormatter pnf = new PersonNameFormatter(Locale.US, new String[] {
            "A {title} {given} {given2} {surname} {surname2} {generation}",
            "B {given} {given2} {surname} {surname2}",
            "C {given} {surname}",
        }, null, null, null);

        String[][] testCases = new String[][] {
//                { "locale=en_US,title=Dr.,given=Richard,given2=Theodore,surname=Gillam,surname2=Morgan,generation=III", "A Dr. Richard Theodore Gillam Morgan III" },
//                { "locale=en_US,title=Mr.,given=Richard,given2=Theodore,surname=Gillam", "A Mr. Richard Theodore Gillam" },
//                { "locale=en_US,given=Richard,given2=Theodore,surname=Gillam",            "B Richard Theodore Gillam" },
//                { "locale=en_US,given=Richard,surname=Gillam",                            "C Richard Gillam" },
//                { "locale=en_US,given=Richard",                                           "C Richard" },
                { "locale=en_US,title=Dr.,generation=III",                                "A Dr. III" }
        };

        for (String[] testCase : testCases) {
            SimplePersonName name = buildPersonName(testCase[0]);
            String expectedResult = testCase[1];
            String actualResult = pnf.formatToString(name);

            assertEquals("Wrong result", expectedResult, actualResult);
        }
    }

    @Test
    public void TestCapitalization() {
        // a more extensive test of the capitalization logic to make sure it works right with characters that
        // have a separate titlecase form
        SimplePersonName name = buildPersonName("locale=hu_HU,given=ǳsárgál,surname=ǳiatkowicz");

        String[][] testCases = new String[][] {
            { "{surname} {given}",                       "ǳiatkowicz ǳsárgál" },
            { "{surname-initialCap} {given-initialCap}", "ǲiatkowicz ǲsárgál"},
            { "{surname-allCaps} {given-allCaps}",       "ǱIATKOWICZ ǱSÁRGÁL" },
            { "{surname-monogram}{given-monogram}",      "ǳǳ" },
            { "{surname-initial} {given-initial}",       "ǳ. ǳ." }
        };

        for (String[] testCase : testCases) {
            PersonNameFormatter pnf = new PersonNameFormatter(new Locale("hu", "HU"), new String[] { testCase[0] }, null, null, null );
            String expectedResult = testCase[1];
            String actualResult = pnf.formatToString(name);

            assertEquals("Wrong result", expectedResult, actualResult);
        }
    }

    @Test
    public void TestLocaleDerivation() {
        // Test for https://unicode-org.atlassian.net/browse/ICU-22379, which implements the algorithm
        // described in https://unicode-org.atlassian.net/browse/CLDR-16623.
        executeTestCases(new NameAndTestCases[]{
            // If we have a name that's tagged as Japanese, but contains Latin characters, and we're using
            // a Japanese formatter, we actually use the English formatter to format it, but because the name is
            // tagged as Japanese, we still use Japanese field order
            new NameAndTestCases("given=Richard,surname=Gillam,locale=ja_AQ", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "Gillam Richard"},
            }),
            // If the name is instead tagged as English, we still use the English formatter, this time
            // with English field order
            new NameAndTestCases("given=Richard,surname=Gillam,locale=en_US", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "Richard Gillam"},
            }),
            // If the name is tagged as Japanese, uses Katakana, and we're using a Japanese formatter,
            // we just use the Japanese formatter: we use native (no) space replacement and Japanese
            // field order
            new NameAndTestCases("given=リチャード,surname=ギラム,locale=ja_AQ", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "ギラムリチャード"},
            }),
            // If the name is tagged as English, but written in Katakana, and we're using the Japanese
            // formatter, we use the Japanese formatter, but with foreign space replacement and
            // English field order
            new NameAndTestCases("given=リチャード,surname=ギラム,locale=en_US", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "リチャード・ギラム"},
            }),
            // a few tests with alternate script codes for Japanese, just to make sure those things work
            new NameAndTestCases("given=Richard,surname=Gillam,locale=ja_Hani", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "Gillam Richard"},
            }),
            new NameAndTestCases("given=Richard,surname=Gillam,locale=ja_Jpan", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "Gillam Richard"},
            }),
            new NameAndTestCases("given=リチャード,surname=ギラム,locale=ja_Kana", new String[][]{
                {"ja", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "ギラムリチャード"},
            }),
            // A few test cases for Chinese to make sure we're not switching Chinese name formats
            // based on the name locale we pass in (we're using the given2 field to tell whether
            // we got the zh_Hans or zh_Hant formatter)
            new NameAndTestCases("given=港生,surname=陳,given2=Test", new String[][]{
                {"en", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "陳港生Test"},
            }),
            new NameAndTestCases("given=港生,surname=陳,given2=Test,locale=zh", new String[][]{
                {"en", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "陳港生Test"},
            }),
            new NameAndTestCases("given=港生,surname=陳,given2=Test,locale=zh_Hant", new String[][]{
//                {"en", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "陳港生T."},  Temporary change the test because data was set to provisional
                {"en", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "陳港生Test"},
            }),
            new NameAndTestCases("given=港生,surname=陳,given2=Test,locale=zh_Hani", new String[][]{
                {"en", "MEDIUM", "REFERRING", "FORMAL",   "DEFAULT", "", "陳港生Test"},
            }),
        }, false);
    }

    @Test
    public void TestNameOrderFromLocale() {
        PersonNameFormatter pnf = new PersonNameFormatter(Locale.US,
                new String[] { "{given} {surname}" },       // gnFirstPatterns
                new String[] { "{surname} {given}" },       // snFirstPatterns
                new String[] { "und", "zh_Hant" },          // gnFirstLocales
                new String[] { "zh", "und_CN", "und_SG" }   // snFirstLocales
        );

        String[][] testCases = new String[][] {
                { "en",         "Given Sur" }, // should match "und"
                { "zh",         "Sur Given" }, // should match "zh"
                { "en_US",      "Given Sur" }, // should match "und"
                { "zh_CN",      "Sur Given" }, // should match "und_CN"
                { "zh_TW",      "Given Sur" }, // should match "zh_Hant"
                { "zh_Hans",    "Sur Given" }, // should match "zh"
                { "zh_Hant",    "Given Sur" }, // should match "zh_Hant"
                { "zh_Hant_CN", "Given Sur" }, // should match "zh_Hant", NOT "und_CN"
                { "en_CN",      "Sur Given" }, // should match "und_CN"
                { "de_DE",      "Given Sur" }, // should match "und"
        };

        for (String[] testCase : testCases) {
            String localeID = testCase[0];
            String expectedResult = testCase[1];

            SimplePersonName name = buildPersonName("given=Given,surname=Sur,locale=" + localeID);
            String actualResult = pnf.formatToString(name);
            assertEquals("Wrong result for " + localeID, expectedResult, actualResult);
        }
    }
}
