/*
 * @(#)$RCSfile: TestAll.java,v $ $Revision: 1.2 $ $Date: 2000/04/24 21:11:30 $
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
package com.ibm.test.richtext;

import com.ibm.test.TestFmwk;

public class TestAll extends TestFmwk {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }
    
    public void TestAttributeSet() throws Exception {
        run(new TestAttributeSet());
    }
    
    public void TestAttributeMap() throws Exception {
        run(new TestAttributeMap());
    }
    
    public void TestFormatter() throws Exception {
        run(new TestFormatter());
    }
    
    public void TestMText() throws Exception {
        run(new TestMText());
    }
    
    public void TestParagraphStyles() throws Exception {
        run(new TestParagraphStyles());
    }
    
    public void TestMTextStreaming() throws Exception {
        run(new TestMTextStreaming());
    }
    
    public void TestTextPanel() throws Exception {
        run(new FmwkTestTextPanel());
    }
}