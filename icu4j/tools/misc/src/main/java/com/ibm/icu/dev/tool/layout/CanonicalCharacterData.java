// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;

public class CanonicalCharacterData {
    private static int THRESHOLD = 4;

    public class Record {
        // TODO: might want to save arrays of Char32's rather than UTF16 strings...
        Record(int character, int script) {
            String char32 = UCharacter.toString(character);
            CanonicalIterator iterator = new CanonicalIterator(char32);
            ArrayList<String> equivs = new ArrayList<>();

            composed = character;

            for (String equiv = iterator.next(); equiv != null; equiv = iterator.next()) {
                // Skip all equivalents of length 1; it's either the original
                // character or something like Angstrom for A-Ring, which we don't care about
                if (UTF16.countCodePoint(equiv) > 1) {
                    equivs.add(equiv);
                }
            }

            int nEquivalents = equivs.size();

            if (nEquivalents > maxEquivalents[script]) {
                maxEquivalents[script] = nEquivalents;
            }

            if (nEquivalents > 0) {
                equivalents = new String[nEquivalents];

                if (nEquivalents > THRESHOLD) {
                    dumpEquivalents(character, equivs);
                }

                sortEquivalents(equivalents, equivs);
            }
        }

        public int getComposedCharacter() {
            return composed;
        }

        public int countEquivalents() {
            if (equivalents == null) {
                return 0;
            }

            return equivalents.length;
        }

        public String[] getEquivalents() {
            return equivalents;
        }

        public String getEquivalent(int index) {
            if (equivalents == null || index < 0 || index >= equivalents.length) {
                return null;
            }

            return equivalents[index];
        }

        private void dumpEquivalents(int character, ArrayList<String> equivs) {
            int count = equivs.size();

            System.out.println(Utility.hex(character, 6) + " - " + count + ":");

            for (int i = 0; i < count; i += 1) {
                String equiv = equivs.get(i);
                int codePoints = UTF16.countCodePoint(equiv);

                for (int c = 0; c < codePoints; c += 1) {
                    if (c > 0) {
                        System.out.print(" ");
                    }

                    System.out.print(Utility.hex(UTF16.charAt(equiv, c), 6));
                }

                System.out.println();
            }

            System.out.println();
        }

        private int composed;
        private String[] equivalents = null;
    }

    public CanonicalCharacterData() {
        // nothing to do...
    }

    public void add(int character) {
        int script = UScript.getScript(character);
        ArrayList<Record> records = recordLists.get(script);

        if (records == null) {
            records = new ArrayList<>();
            recordLists.set(script, records);
        }

        records.add(new Record(character, script));
    }

    public int getMaxEquivalents(int script) {
        if (script < 0 || script >= UScript.CODE_LIMIT) {
            return 0;
        }

        return maxEquivalents[script];
    }

    public Record getRecord(int script, int index) {
        if (script < 0 || script >= UScript.CODE_LIMIT) {
            return null;
        }

        ArrayList<Record> records = recordLists.get(script);

        if (records == null || index < 0 || index >= records.size()) {
            return null;
        }

        return records.get(index);
    }

    public int countRecords(int script) {
        if (script < 0 || script >= UScript.CODE_LIMIT || recordLists.get(script) == null) {
            return 0;
        }

        return recordLists.get(script).size();
    }

    public static CanonicalCharacterData factory(UnicodeSet characterSet) {
        int charCount = characterSet.size();
        CanonicalCharacterData data = new CanonicalCharacterData();

        System.out.println(
                "There are " + charCount + " characters with a canonical decomposition.");

        for (int i = 0; i < charCount; i += 1) {
            data.add(characterSet.charAt(i));
        }

        return data;
    }

    private static int compareEquivalents(String a, String b) {
        int result = UTF16.countCodePoint(a) - UTF16.countCodePoint(b);

        if (result == 0) {
            return a.compareTo(b);
        }

        return result;
    }

    //
    // Straight insertion sort from Knuth vol. III, pg. 81
    //
    private static void sortEquivalents(String[] equivalents, ArrayList<String> unsorted) {
        int nEquivalents = equivalents.length;

        for (int e = 0; e < nEquivalents; e += 1) {
            String v = unsorted.get(e);
            int i;

            for (i = e - 1; i >= 0; i -= 1) {
                if (compareEquivalents(v, equivalents[i]) >= 0) {
                    break;
                }

                equivalents[i + 1] = equivalents[i];
            }

            equivalents[i + 1] = v;
        }
    }

    private ArrayList<ArrayList<Record>> recordLists = new ArrayList<>(UScript.CODE_LIMIT);
    private int maxEquivalents[] = new int[UScript.CODE_LIMIT];
}
