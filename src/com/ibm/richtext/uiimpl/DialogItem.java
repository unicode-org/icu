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

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.uiimpl.resources.MenuData;

public final class DialogItem extends CommandMenuItem {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static abstract class DialogFactory {
        
        public abstract Window createDialog(MTextPanel textPanel);
    }
    
    private DialogFactory fDialogFactory;
    private Window fDialog = null;
    
    public DialogItem(MenuData menuData,
                      DialogFactory dialogFactory) {
        
        super(menuData, true);
        fDialogFactory = dialogFactory;
    }
    
    protected void textPanelChanged() {
        // do nothing
    }
        
    protected boolean isEnabled() {
            
        // should never get called...
        return true;
    }

    public boolean respondsToEventType(int type) {
            
        return false;
    }

    protected void performAction() {

        if (fDialog == null) {
            MTextPanel panel = getTextPanel();
            if (panel != null) {
                fDialog = fDialogFactory.createDialog(panel);
                fDialog.addWindowListener(new WindowAdapter() {
                    public void windowClosed(WindowEvent e) {
                        fDialog = null;
                    }
                });
            }
        }
        fDialog.show();
    }
}