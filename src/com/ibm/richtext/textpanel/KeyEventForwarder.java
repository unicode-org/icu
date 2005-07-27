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
package com.ibm.richtext.textpanel;

import java.awt.event.KeyEvent;

/**
 * This class forwards key events to a TextPanel component for
 * testing purposes.
 * Its sole reason for existence is to prevent the key-event
 * API from being public on MTextPanel, and being mistaken for
 * standard API.  This class is only for testing!  It may be
 * removed from public API at any time.  Do not depend on this
 * class.
 */
public final class KeyEventForwarder {

    private ATextPanelImpl fPanelImpl;

    public KeyEventForwarder(TextPanel textPanel) {

        fPanelImpl = textPanel.getImpl();
    }
    
    public KeyEventForwarder(JTextPanel textPanel) {
    
        fPanelImpl = textPanel.getImpl();
    }

    public void handleKeyEvent(KeyEvent keyEvent) {

        fPanelImpl.handleKeyEvent(keyEvent);
    }
}