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

import java.util.EventObject;

import java.util.Hashtable;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.uiimpl.resources.MenuData;

public abstract class ChoiceMenuItemSet extends MenuItemSet {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private MItem[] fItems;
    private Hashtable fItemToStyleMap;
    
    ChoiceMenuItemSet(Object[] values,
                      MenuData[] menuData) {
        
        if (values.length != menuData.length) {
            throw new IllegalArgumentException(
                                "Values and names must have equal length");
        }
        
        fItems = new MItem[menuData.length];
        fItemToStyleMap = new Hashtable(menuData.length);
        
        EventListener listener = new EventListener() {
            public void eventOccurred(EventObject event) {
                handleValueSelected(fItemToStyleMap.get(event.getSource()));
            }
        };
        
        for (int i=0; i < menuData.length; i++) {
            fItems[i] = MItem.createCheckboxItem(menuData[i]);
            if (values[i] != null) {
                fItemToStyleMap.put(fItems[i], values[i]);
            }
            fItems[i].addListener(listener);
            fItems[i].setEnabled(false);
        }
    }
    
    protected abstract void handleValueSelected(Object item);
    protected abstract Object getCurrentValue();
    
    protected final void setChecked() {
        
        Object value = getCurrentValue();
        
        for (int i=0; i < fItems.length; i++) {
            Object itemVal = fItemToStyleMap.get(fItems[i]);
            if (itemVal == null) {
                fItems[i].setState(value == null);
            }
            else {
                fItems[i].setState(itemVal.equals(value));
            }
        }
    }

    protected final void textPanelChanged() {
        
        MTextPanel textPanel = getTextPanel();
        if (textPanel == null) {
            for (int i=0; i < fItems.length; i++) {
                fItems[i].setEnabled(false);
                fItems[i].setState(false);
            }
        }
        else {
            for (int i=0; i < fItems.length; i++) {
                fItems[i].setEnabled(true);
                setChecked();
            }
        }
    }
}
