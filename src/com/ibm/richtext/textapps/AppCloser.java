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
package com.ibm.richtext.textapps;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Frame;
import java.awt.Window;

class AppCloser {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private int fCount = 0;

    private WindowAdapter fAdapter = new WindowAdapter() {

        public void windowClosing(WindowEvent e) {
            --fCount;
            if (fCount == 0) {
                System.exit(0);
            }
            Window w = e.getWindow();
            w.setVisible(false);
            w.dispose();
        }
    };

    public void listenToFrame(Frame frame) {

        ++fCount;
        frame.addWindowListener(fAdapter);
    }
}
