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

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.AttributeSet;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.uiimpl.resources.MenuData;

public final class SubtractStyleMenuItem extends SingleCheckMenuItem {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private final Object[] fKeys;
    
    private final boolean fCharacter;
    
    public SubtractStyleMenuItem(Object[] keys,
                                 MenuData menuData,
                                 boolean character) {
        
        super(menuData);
        
        fKeys = (Object[]) keys.clone();
        fCharacter = character;
        
        AttributeSet keySet = new AttributeSet(keys);
        final StyleModifier modifier = StyleModifier.createRemoveModifier(keySet);
        
        fItem.addListener(new EventListener() {
            public void eventOccurred(EventObject event) {
                MTextPanel panel = getTextPanel();
                if (panel == null) {
                    throw new Error("Menu item is enabled when panel is null!");
                }

                if (fCharacter == CHARACTER) {
                    panel.modifyCharacterStyleOnSelection(modifier);
                }
                else {
                    panel.modifyParagraphStyleOnSelection(modifier);
                }
            }
        });
    }
    
    private static boolean objectsAreEqual(Object lhs, Object rhs) {
    
        if (lhs == null) {
            return rhs == null;
        }
        else {
            return lhs.equals(rhs);
        }
    }

    protected void setChecked() {

        MTextPanel panel = getTextPanel();
        AttributeMap defaults = panel.getDefaultValues();
        
        for (int i=0; i < fKeys.length; i++) {
            Object defaultV = defaults.get(fKeys[i]);
            
            Object value = (fCharacter == CHARACTER)? 
                    panel.getCharacterStyleOverSelection(fKeys[i]) :
                    panel.getParagraphStyleOverSelection(fKeys[i]);
                    
            if (!objectsAreEqual(defaultV, value)) {
                fItem.setState(false);
                return;
            }
        }

        fItem.setState(true);
    }

    public boolean respondsToEventType(int type) {
        
        return type == TextPanelEvent.SELECTION_STYLES_CHANGED;
    }
    
    public final void textEventOccurred(TextPanelEvent event) {
        
        setChecked();
    }
}
