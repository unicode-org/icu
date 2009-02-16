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

import java.util.Vector;
import java.util.EventObject;

import com.ibm.richtext.uiimpl.resources.MenuData;

public abstract class MItem {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private Vector fListeners = new Vector(2);
    private EventObject fEvent = new EventObject(this);
    
    public abstract void setEnabled(boolean enabled);
    public abstract void setState(boolean checked);
    
    public final void addListener(EventListener listener) {
        
        fListeners.addElement(listener);
    }
    
    public final void removeListener(EventListener listener) {
        
        fListeners.removeElement(listener);
    }
    
    protected void handleSelected() {
            
        int length = fListeners.size();
        for (int i=0; i < length; i++) {
            EventListener l = (EventListener) fListeners.elementAt(i);
            l.eventOccurred(fEvent);
        }
    }
    
    // factory stuff
    
    /**
     * Clients should synchronize on LOCK while setting and using
     * global factory.
     */
    public static final Object LOCK = new Object();
    
    public static interface ItemFactory {
        
        public MItem createItem(MenuData menuData);
        public MItem createCheckboxItem(MenuData menuData);
        public void createSeparator();
    }
    
    private static ItemFactory fgFactory;
    
    public static MItem createItem(MenuData menuData) {
        
        return fgFactory.createItem(menuData);
    }
    
    public static MItem createCheckboxItem(MenuData menuData) {
        
        return fgFactory.createCheckboxItem(menuData);
    }
    
    public static void setItemFactory(ItemFactory factory) {
        
        fgFactory = factory;
    }
    
    public static ItemFactory getItemFactory() {
        
        return fgFactory;
    }
}
