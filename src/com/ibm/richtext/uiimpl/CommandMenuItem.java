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

import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.uiimpl.resources.MenuData;

public abstract class CommandMenuItem extends MenuItemSet {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private /*final*/ MItem fItem;
    
    protected abstract boolean isEnabled();
    protected abstract void performAction();

    protected CommandMenuItem(MenuData data) {
        
        this(data, false);
    }
    
    protected CommandMenuItem(MenuData data, boolean enableByDefault) {
        
        fItem = MItem.createItem(data);
        fItem.addListener(new EventListener() {
            public void eventOccurred(EventObject event) {
                performAction();
            }
        });
        fItem.setEnabled(enableByDefault);
    }
    
    protected void textPanelChanged() {
        
        MTextPanel textPanel = getTextPanel();
        if (textPanel == null) {
            fItem.setEnabled(false);
        }
        else {
            fItem.setEnabled(isEnabled());
        }
    }
    
    public final void textEventOccurred(TextPanelEvent event) {
        
        fItem.setEnabled(isEnabled());
    }
    
    public static final class CutCopyClear extends CommandMenuItem {
        
        public static final int CUT = 0;
        public static final int COPY = 1;
        public static final int CLEAR = 2;

        private final int fKind;
        
        public CutCopyClear(MenuData menuData, int kind) {
            
            super(menuData);
            if (kind != CUT && kind != COPY && kind != CLEAR) {
                throw new IllegalArgumentException("Invalid menu kind");
            }
            fKind = kind;
        }
        
        protected boolean isEnabled() {
            
            MTextPanel panel = getTextPanel();
            return panel.getSelectionStart() != panel.getSelectionEnd();
        }
        
        public boolean respondsToEventType(int type) {
            
            return type == TextPanelEvent.SELECTION_EMPTY_CHANGED;
        }
        
        protected void performAction() {
            
            MTextPanel panel = getTextPanel();
            switch (fKind) {
                case CUT:
                    panel.cut();
                    break;
                case COPY:
                    panel.copy();
                    break;
                case CLEAR:
                    panel.clear();
                    break;
            }
        }
    }
    
    public static final class UndoRedo extends CommandMenuItem {
        
        public static final boolean UNDO = true;
        public static final boolean REDO = false;
        
        private boolean fKind;
        
        public UndoRedo(MenuData menuData, boolean kind) {
            
            super(menuData);
            fKind = kind;
        }
        
        protected boolean isEnabled() {
            
            MTextPanel panel = getTextPanel();
            if (fKind == UNDO) {
                return panel.canUndo();
            }
            else {
                return panel.canRedo();
            }
        }

        public boolean respondsToEventType(int type) {
            
            return type == TextPanelEvent.UNDO_STATE_CHANGED;
        }

        protected void performAction() {
            
            MTextPanel panel = getTextPanel();
            if (fKind == UNDO) {
                panel.undo();
            }
            else {
                panel.redo();
            }
        }
    }

    public static final class Paste extends CommandMenuItem {
        
        public Paste(MenuData menuData) {
            
            super(menuData);
        }
        
        protected boolean isEnabled() {
            
            return getTextPanel().clipboardNotEmpty();
        }

        public boolean respondsToEventType(int type) {
            
            return type == TextPanelEvent.CLIPBOARD_CHANGED;
        }

        protected void performAction() {
            
            getTextPanel().paste();
        }
    }

    public static final class SelectAll extends CommandMenuItem {
        
        public SelectAll(MenuData menuData) {
            
            super(menuData);
        }
        
        protected boolean isEnabled() {
            
            return true;
        }

        public boolean respondsToEventType(int type) {
            
            return false;
        }

        protected void performAction() {
            
            getTextPanel().selectAll();
        }
    }
}
