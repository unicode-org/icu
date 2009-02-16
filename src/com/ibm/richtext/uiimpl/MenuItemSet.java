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
package com.ibm.richtext.uiimpl;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.textpanel.TextPanelListener;

public abstract class MenuItemSet implements TextPanelListener {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static final boolean CHARACTER = true;
    public static final boolean PARAGRAPH = false;

    private MTextPanel fTextPanel = null;
    
    public abstract void textEventOccurred(TextPanelEvent event);
    public abstract boolean respondsToEventType(int type);
    
    public final void setTextPanel(MTextPanel textPanel) {
        
        if (fTextPanel != null) {
            fTextPanel.removeListener(this);
        }
        fTextPanel = textPanel;
        if (fTextPanel != null) {
            fTextPanel.addListener(this);
        }
        
        textPanelChanged();
    }
    
    public final MTextPanel getTextPanel() {
        
        return fTextPanel;
    }
    
    protected void textPanelChanged() {
    }
}
