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
import com.ibm.richtext.uiimpl.resources.MenuData;

public abstract class SingleCheckMenuItem extends MenuItemSet {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    protected MItem fItem;
    
    SingleCheckMenuItem(MenuData menuData) {
        
        fItem = MItem.createCheckboxItem(menuData);
    }
    
    abstract void setChecked();
    
    protected final void textPanelChanged() {
        
        MTextPanel textPanel = getTextPanel();
        if (textPanel == null) {
            fItem.setEnabled(false);
            fItem.setState(false);
        }
        else {
            fItem.setEnabled(true);
            setChecked();
        }
    }
}
