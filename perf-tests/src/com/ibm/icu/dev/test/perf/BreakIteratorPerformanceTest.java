/*
 **********************************************************************
 * Copyright (c) 2002-2008, International Business Machines           *
 * Corporation and others.  All Rights Reserved.                      *
 **********************************************************************
 */
package com.ibm.icu.dev.test.perf;

import java.io.FileInputStream;
import java.util.ArrayList;

public class BreakIteratorPerformanceTest extends PerfTest {

    String fileContents;

    com.ibm.icu.text.BreakIterator iSentenceIter;
    com.ibm.icu.text.BreakIterator iWordIter;
    com.ibm.icu.text.BreakIterator iLineIter;
    com.ibm.icu.text.BreakIterator iCharacterIter;
    java.text.BreakIterator jSentenceIter;
    java.text.BreakIterator jWordIter;
    java.text.BreakIterator jLineIter;
    java.text.BreakIterator jCharacterIter;
    String[] iSentences;
    String[] iWords;
    String[] iLines;
    String[] iCharacters;
    String[] jSentences;
    String[] jWords;
    String[] jLines;
    String[] jCharacters;

    public static void main(String[] args) throws Exception {
        new BreakIteratorPerformanceTest().run(args);
    }

    protected void setup(String[] args) {
        try {
            // read in the input file, being careful with a possible BOM
            FileInputStream in = new FileInputStream(fileName);
            BOMFreeReader reader = new BOMFreeReader(in, encoding);
            fileContents = new String(readToEOS(reader));

            // // get rid of any characters that may cause differences between ICU4J and Java BreakIterator
            // // fileContents = fileContents.replaceAll("[\t\f\r\n\\-/ ]+", " ");
            // String res = "";
            // StringTokenizer tokenizer = new StringTokenizer(fileContents, "\t\f\r\n-/ ");
            // while (tokenizer.hasMoreTokens())
            // res += tokenizer.nextToken() + " ";
            // fileContents = res.trim();

            // create the break iterators with respect to locale
            if (locale == null) {
                iSentenceIter = com.ibm.icu.text.BreakIterator.getSentenceInstance();
                iWordIter = com.ibm.icu.text.BreakIterator.getWordInstance();
                iLineIter = com.ibm.icu.text.BreakIterator.getLineInstance();
                iCharacterIter = com.ibm.icu.text.BreakIterator.getCharacterInstance();

                jSentenceIter = java.text.BreakIterator.getSentenceInstance();
                jWordIter = java.text.BreakIterator.getWordInstance();
                jLineIter = java.text.BreakIterator.getLineInstance();
                jCharacterIter = java.text.BreakIterator.getCharacterInstance();
            } else {
                iSentenceIter = com.ibm.icu.text.BreakIterator.getSentenceInstance(locale);
                iWordIter = com.ibm.icu.text.BreakIterator.getWordInstance(locale);
                iLineIter = com.ibm.icu.text.BreakIterator.getLineInstance(locale);
                iCharacterIter = com.ibm.icu.text.BreakIterator.getCharacterInstance(locale);

                jSentenceIter = java.text.BreakIterator.getSentenceInstance(locale);
                jWordIter = java.text.BreakIterator.getWordInstance(locale);
                jLineIter = java.text.BreakIterator.getLineInstance(locale);
                jCharacterIter = java.text.BreakIterator.getCharacterInstance(locale);
            }

            iSentences = init(iSentenceIter);
            iWords = init(iWordIter);
            iLines = init(iLineIter);
            iCharacters = init(iCharacterIter);
            jSentences = init(jSentenceIter);
            jWords = init(jWordIter);
            jLines = init(jLineIter);
            jCharacters = init(jCharacterIter);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }

        // we created some heavy objects, so lets try to clean up a little before running the tests
        gc();
    }

    private String[] init(com.ibm.icu.text.BreakIterator iter) {
        // set the string to iterate on
        iter.setText(fileContents);

        // produce a token list
        ArrayList tokenList = new ArrayList();
        int start = iter.first();
        for (int end = iter.next(); end != com.ibm.icu.text.BreakIterator.DONE; start = end, end = iter.next())
            tokenList.add(fileContents.substring(start, end));

        // return the token list as a string array
        return (String[]) tokenList.toArray(new String[0]);
    }

    private String[] init(java.text.BreakIterator iter) {
        // set the string to iterate on
        iter.setText(fileContents);

        // produce a token list
        ArrayList tokenList = new ArrayList();
        int start = iter.first();
        for (int end = iter.next(); end != com.ibm.icu.text.BreakIterator.DONE; start = end, end = iter.next())
            tokenList.add(fileContents.substring(start, end));

        // return the token list as a string array
        return (String[]) tokenList.toArray(new String[0]);
    }

    PerfTest.Function createTestICU(final com.ibm.icu.text.BreakIterator iIter, final String[] correct,
            final String breakType) {
        return new PerfTest.Function() {
            public void call() {
                int k = 0;
                int start = iIter.first();
                for (int end = iIter.next(); end != com.ibm.icu.text.BreakIterator.DONE; start = end, end = iIter
                        .next())
                    if (!correct[k++].equals(fileContents.substring(start, end)))
                        throw new RuntimeException("ICU4J BreakIterator gave the wrong answer for " + breakType + " "
                                + (k - 1) + " during the performance test. Cannot continue the performance test.");
                if (k != correct.length)
                    throw new RuntimeException("ICU4J BreakIterator gave the wrong number of " + breakType
                            + "s during the performance test. Cannot continue the performance test.");
            }

            public long getOperationsPerIteration() {
                return fileContents.length();
            }
        };
    }

    PerfTest.Function createTestJava(final java.text.BreakIterator jIter, final String[] correct, final String breakType) {
        return new PerfTest.Function() {
            public void call() {
                int k = 0;
                int start = jIter.first();
                for (int end = jIter.next(); end != java.text.BreakIterator.DONE; start = end, end = jIter.next())
                    if (!correct[k++].equals(fileContents.substring(start, end)))
                        throw new RuntimeException("Java BreakIterator gave the wrong answer for " + breakType + " "
                                + (k - 1) + " during the performance test. Cannot continue the performance test.");
                if (k != correct.length)
                    throw new RuntimeException("Java BreakIterator gave the wrong number of " + breakType
                            + "s during the performance test. Cannot continue the performance test.");
            }

            public long getOperationsPerIteration() {
                return fileContents.length();
            }
        };
    }

    PerfTest.Function TestICUSentences() {
        return createTestICU(iSentenceIter, iSentences, "sentence");
    }

    PerfTest.Function TestICUWords() {
        return createTestICU(iWordIter, iWords, "word");
    }

    PerfTest.Function TestICULines() {
        return createTestICU(iLineIter, iLines, "line");
    }

    PerfTest.Function TestICUCharacters() {
        return createTestICU(iCharacterIter, iCharacters, "character");
    }

    PerfTest.Function TestJavaSentences() {
        return createTestJava(jSentenceIter, jSentences, "sentence");
    }

    PerfTest.Function TestJavaWords() {
        return createTestJava(jWordIter, jWords, "word");
    }

    PerfTest.Function TestJavaLines() {
        return createTestJava(jLineIter, jLines, "line");
    }

    PerfTest.Function TestJavaCharacters() {
        return createTestJava(jCharacterIter, jCharacters, "character");
    }
}
