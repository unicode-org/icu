/*
 * @(#)$RCSfile: TestAll.java,v $ $Revision: 1.1 $ $Date: 2000/04/20 17:46:57 $
 *
 * (C) Copyright IBM Corp. 1998-1999.  All Rights Reserved.
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
package com.ibm.richtext.tests;

public class TestAll {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    public static void main(String[] args) {

        TestAttributeSet.main(args);
        TestAttributeMap.main(args);
        TestFormatter.main(args);
        TestMText.main(args);
        TestMTextStreaming.main(args);
        TestParagraphStyles.main(args);
        TestTextPanel.main(args);
        System.out.println("TestAll:  DONE");
    }
}