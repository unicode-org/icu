/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ComposedCharIter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringCharacterIterator;

public class TestDeprecatedNormalizerAPI extends TestFmwk
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



        new TestDeprecatedNormalizerAPI().run(args);
    }
    
    public TestDeprecatedNormalizerAPI() {
    }

    public void TestNormalizerAPI(){
         // instantiate a Normalizer from a CharacterIterator
        String s=Utility.unescape("a\u0308\uac00\\U0002f800");
        // make s a bit longer and more interesting
        java.text.CharacterIterator iter = new StringCharacterIterator(s+s);
        //test deprecated constructors
        Normalizer norm = new Normalizer(iter, Normalizer.NFC,0);
        if(norm.next()!=0xe4) {
            errln("error in Normalizer(CharacterIterator).next()");
        }       
        Normalizer norm2 = new Normalizer(s,Normalizer.NFC,0);
        if(norm2.next()!=0xe4) {
            errln("error in Normalizer(CharacterIterator).next()");
        }       
        // test clone(), ==, and hashCode()
        Normalizer clone=(Normalizer)norm.clone();
        if(clone.getBeginIndex()!= norm.getBeginIndex()){
           errln("error in Normalizer.getBeginIndex()");
        }
        
        if(clone.getEndIndex()!= norm.getEndIndex()){
           errln("error in Normalizer.getEndIndex()");
        }
        // test setOption() and getOption()
        clone.setOption(0xaa0000, true);
        clone.setOption(0x20000, false);
        if(clone.getOption(0x880000) ==0|| clone.getOption(0x20000)==1) {
           errln("error in Normalizer::setOption() or Normalizer::getOption()");
        }
        //test deprecated normalize method
        Normalizer.normalize(s,Normalizer.NFC,0);
        //test deprecated compose method
        Normalizer.compose(s,false,0);
        //test deprecated decompose method
        Normalizer.decompose(s,false,0);

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
