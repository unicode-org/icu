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
package com.ibm.richtext.styledtext;

final class TestFastIntBinarySearch {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static void main(String[] args) {

        boolean result = new TestFastIntBinarySearch().test();
        System.out.println(result? "PASSED" : "FAILED");
    }

    public boolean test() {

        boolean result = true;
        int[] test = {-5, -3, 0, 2, 5};
        FastIntBinarySearch fibs = new FastIntBinarySearch(test);

        for (int i=0; i < 2; i++) {
            int beforeAny = fibs.findIndex(-6);
            if (beforeAny != -1) {
                result = false;
            }
            
            int atEnd = fibs.findIndex(5);
            if (atEnd != test.length-1) {
                result = false;
            }
            
            int afterAny = fibs.findIndex(6);
            if (afterAny != test.length-1) {
                result = false;
            }
            
            int exactly = fibs.findIndex(-3);
            if (exactly != 1) {
                result = false;
            }
            
            fibs = new FastIntBinarySearch(new int[] {20, 40});
            fibs.setData(test);
        }
        
        return result;
    }
}