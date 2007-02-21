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

import com.ibm.richtext.textpanel.KeyRemap;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.uiimpl.resources.MenuData;

public final class KeymapMenuItemSet extends ChoiceMenuItemSet {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public KeymapMenuItemSet(KeyRemap[] values,
                             MenuData[] menuData) {
        
        super(values, menuData);
    }
    
    protected void handleValueSelected(Object value) {
        
        MTextPanel textPanel = getTextPanel();
        if (textPanel == null) {
            throw new Error("Menu item is enabled when panel is null!");
        }

        textPanel.setKeyRemap((KeyRemap) value);
    }
    
    protected Object getCurrentValue() {
        
        MTextPanel textPanel = getTextPanel();
        if (textPanel == null) {
            throw new Error("Shouldn't call this without a text panel!");
        }
        return textPanel.getKeyRemap();
    }
    
    public void textEventOccurred(TextPanelEvent event) {
        
        setChecked();
    }
    
    public boolean respondsToEventType(int type) {
        
        return type == TextPanelEvent.KEYREMAP_CHANGED;
    }

}
