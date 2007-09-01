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

import com.ibm.richtext.textlayout.attributes.AttributeSet;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.uiimpl.resources.MenuData;

public final class BooleanStyleMenuItem extends SingleCheckMenuItem {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private final Object fKey;
    private final boolean fCharacter;

    private final Object fOnValue;
    
    public BooleanStyleMenuItem(Object style,
                                Object onValue,
                                MenuData menuData,
                                boolean character) {
        
        super(menuData);
        if (onValue == null) {
            throw new IllegalArgumentException("On value cannot be null");
        }

        fKey = style;
        fCharacter = character;
        
        fOnValue = onValue;
        fItem.addListener(new EventListener() {
            public void eventOccurred(EventObject event) {
                StyleModifier modifier;
                MTextPanel panel = getTextPanel();
                if (panel == null) {
                    throw new Error("Menu item is enabled when panel is null!");
                }
                if (continuousAndCommand()) {
                    AttributeSet set = new AttributeSet(fKey);
                    modifier = StyleModifier.createRemoveModifier(set);
                }
                else {
                    modifier = StyleModifier.createAddModifier(fKey, fOnValue);
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
    
    private boolean continuousAndCommand() {
        
        MTextPanel panel = getTextPanel();

        Object value = (fCharacter == CHARACTER)? 
                            panel.getCharacterStyleOverSelection(fKey) :
                            panel.getParagraphStyleOverSelection(fKey);
        return fOnValue.equals(value);
    }
    
    protected void setChecked() {
        
        fItem.setState(continuousAndCommand());
    }
    
    public boolean respondsToEventType(int type) {
        
        return type == TextPanelEvent.SELECTION_STYLES_CHANGED;
    }
    
    public final void textEventOccurred(TextPanelEvent event) {
        
        setChecked();
    }
}