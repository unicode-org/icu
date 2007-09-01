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
import com.ibm.richtext.test.TestTextPanel;

public final class FmwkTestTextPanel extends TestFmwk {

    public static void main(String[] args) throws Exception {
    
        new FmwkTestTextPanel().run(args);
    }
    
    private TTP ttp;
    
    public FmwkTestTextPanel() {
    
        ttp = new TTP();
    }
    
    public void test() {
    
        ttp.test();
        // If TestTextPanel gets more tests, add them here
    }

    private final class TTP extends TestTextPanel {
    
        protected void reportError(String message) {
        
            errln(message);
        }
        
        protected void logMessage(String message) {
        
            logln(message);
        }
    }
}