/*
 * @(#)$RCSfile: KeyEventForwarder.java,v $ $Revision: 1.1 $ $Date: 2000/04/20 17:51:23 $
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
package com.ibm.richtext.textpanel;

import java.awt.event.KeyEvent;

/**
 * This class forwards key events to a TextPanel component for
 * testing purposes.
 * It's sole reason for existence is to prevent the key-event
 * API from being public on TextPanel, and being mistaken for
 * standard API.  This class is only for testing!
 */
public final class KeyEventForwarder {

    private TextPanel fRichText;

    public KeyEventForwarder(TextPanel richText) {

        fRichText = richText;
    }

    public void handleKeyEvent(KeyEvent keyEvent) {

        fRichText.handleKeyEvent(keyEvent);
    }
}