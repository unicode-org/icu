/*
 * $RCSfile: ExhaustiveTest.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:49 $
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.test.normalizer;

import com.ibm.test.*;
import com.ibm.text.*;

public class ExhaustiveTest extends TestFmwk
{
    private static UInfo info;

    public static void main(String[] args) throws Exception
    {
        String[] tempArgs = new String[args.length];
        int count = 0;

        // Allow the test to be pointed at a specific version of the Unicode database
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-data")) {
                info = new UInfo(args[++i]);
            } else {
                tempArgs[count++] = args[i];
            }
        }

        args = new String[count];
        System.arraycopy(tempArgs, 0, args, 0, count);


        if (info == null) {
            info = new UInfo();
        }

        new ExhaustiveTest().run(args);
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
            String normDecomp = Normalizer.decompose(chString, compat, 0);

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
            String decomp = Normalizer.decompose(xString, compat, options);
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
            String decomp = Normalizer.decompose(chStr, compat, options);
            String comp = Normalizer.compose(decomp, compat, options);

            short cClass = info.getCanonicalClass(decomp.charAt(0));

            if (info.isExcludedComposition(ch)) {
                logln("Skipped excluded char " + hex(ch) + " (" + info.getName(ch,true) + ")" );
                continue;
            }

            // Avoid disparaged characters
            if (info.getDecomposition(ch).length() == 4) continue;

            if (!comp.equals(chStr)) {
                errln("ERROR: Round trip invalid: " + hex(chStr) + " --> " + hex(decomp)
                    + " --> " + hex(comp));

                errln("  char decomp is '" + info.getDecomposition(ch) + "'");
            }
        }
    }
}