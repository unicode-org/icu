/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/normalizer/Attic/ExhaustiveTest.java,v $ 
 * $Date: 2002/06/20 01:16:24 $ 
 * $Revision: 1.11 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.impl.NormalizerImpl;

public class ExhaustiveTest extends TestFmwk
{
 	
    public static void main(String[] args) throws Exception
    {
    	String[] tempArgs = new String[args.length];
        int count = 0;

        // Allow the test to be pointed at a specific version of the Unicode database
        //for (int i = 0; i < args.length; i++)
        //{
        //    if (args[i].equals("-data")) {
        //        tempInfo = new UInfo(args[++i], args[++i]);
        //    } else {
        //        tempArgs[count++] = args[i];
        //    }
        //}

        args = new String[count];
        System.arraycopy(tempArgs, 0, args, 0, count);



        new ExhaustiveTest().run(args);
    }
    
    public ExhaustiveTest() {
    }

 

    /**
     * Run through all of the characters returned by a composed-char iterator
     * and make sure that:
     * <ul>
     * <li>a) They do indeed have decompositions.
     * <li>b) The decomposition according to the iterator is the same as
     *          returned by Normalizer.decompose().
     * <li>c) All characters <em>not</em> returned by the iterator do not
     *          have decompositions.
     * </ul>
     */
    public void TestComposedCharIter() {
        doTestComposedChars(false);
    }

    public void doTestComposedChars(boolean compat) {
        int options = Normalizer.IGNORE_HANGUL;
        ComposedCharIter iter = new ComposedCharIter(compat, options);

        char lastChar = 0;

        while (iter.hasNext()) {
            char ch = iter.next();

            // Test all characters between the last one and this one to make
            // sure that they don't have decompositions
            assertNoDecomp(lastChar, ch, compat, options);
            lastChar = ch;

            // Now make sure that the decompositions for this character
            // make sense
            String chString   = new StringBuffer().append(ch).toString();
            String iterDecomp = iter.decomposition();
            String normDecomp = Normalizer.decompose(chString, compat);

            if (iterDecomp.equals(chString)) {
                errln("ERROR: " + hex(ch) + " has identical decomp");
            }
            else if (!iterDecomp.equals(normDecomp)) {
                errln("ERROR: Normalizer decomp for " + hex(ch) + " (" + hex(normDecomp) + ")"
                    + " != iter decomp (" + hex(iterDecomp) + ")" );
            }
        }
        assertNoDecomp(lastChar, '\uFFFF', compat, options);
    }

    void assertNoDecomp(char start, char limit, boolean compat, int options)
    {
        for (char x = ++start; x < limit; x++) {
            String xString   = new StringBuffer().append(x).toString();
            String decomp = Normalizer.decompose(xString, compat);
            if (!decomp.equals(xString)) {
                errln("ERROR: " + hex(x) + " has decomposition (" + hex(decomp) + ")"
                    + " but was not returned by iterator");
            }
        }
    }


    public void TestRoundTrip() {
        int options = Normalizer.IGNORE_HANGUL;
        boolean compat = false;

        ComposedCharIter iter = new ComposedCharIter(false, options);
        while (iter.hasNext()) {
            char ch = iter.next();

            String chStr = new StringBuffer().append(ch).toString();
            String decomp = Normalizer.decompose(chStr, compat);
            String comp = Normalizer.compose(decomp, compat);

            int cClass = UCharacter.getCombiningClass(decomp.charAt(0));
            cClass = 0;

            if (NormalizerImpl.isFullCompositionExclusion(ch)) {
                logln("Skipped excluded char " + hex(ch) + " (" + UCharacter.getName(ch) + ")" );
                continue;
            }

            // Avoid disparaged characters
            if (getDecomposition(ch,compat).length() == 4) continue;

            if (!comp.equals(chStr)) {
                errln("ERROR: Round trip invalid: " + hex(chStr) + " --> " + hex(decomp)
                    + " --> " + hex(comp));

                errln("  char decomp is '" + getDecomposition(ch,compat) + "'");
            }
        }
    }
    private String getDecomposition(char ch, boolean compat){
        char[] dest = new char[10];   
        int length = NormalizerImpl.getDecomposition(ch,compat,dest,0,dest.length);   
        return new String(dest,0,length);
    }
}
