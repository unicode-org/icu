/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
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
package com.ibm.richtext.test.unit;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.richtext.textlayout.attributes.AttributeSet;
import java.util.Enumeration;

public class TestAttributeSet extends TestFmwk {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    public static void main(String[] args) throws Exception {

        new TestAttributeSet().run(args);
    }

    public void test() {

        final Object elem1 = new Object();
        final Object elem2 = new Float(4);
        final Object elem3 = "String";
        final Object elem4 = Boolean.FALSE;

        AttributeSet set1 = new AttributeSet(new Object[] {elem1, elem2, elem3});
        if (set1.size() != 3) {
            errln("Size is wrong.");
        }

        if (set1.contains(elem4)){
            errln("Set contents are wrong.");
        }

        if (!set1.contains(elem1)) {
            errln("Set contents are wrong.");
        }

        AttributeSet set2 = new AttributeSet(elem4);

        if (set2.size() != 1) {
            errln("Size is wrong.");
        }

        if (!set2.contains(elem4)){
            errln("Set contents are wrong.");
        }

        if (set2.contains(elem1)) {
            errln("Set contents are wrong.");
        }

        Enumeration iter = set2.elements();
        if (!iter.nextElement().equals(elem4)) {
            errln("Invalid object in iterator.");
        }

        AttributeSet union = set2.unionWith(set1);
        if (!set1.unionWith(set2).equals(union)) {
            errln("unionWith is not commutative.");
        }

        if (!union.contains(elem1) || !union.contains(elem4)) {
            errln("Set contents are wrong.");
        }

        if (!set1.addElement(elem4).equals(union)) {
            errln("addElement is wrong.");
        }

        if (!union.intersectWith(set1).equals(set1)) {
            errln("intersectWith is wrong.");
        }

        if (!union.subtract(set1).equals(set2)) {
            errln("subtract is wrong.");
        }
    }
}