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
package com.ibm.richtext.swingui;

import java.awt.Event;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.ibm.richtext.uiimpl.resources.MenuData;
import com.ibm.richtext.uiimpl.MItem;
import com.ibm.richtext.uiimpl.MItem.ItemFactory;

final class SwingMenuFactory implements ItemFactory {
        
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private final class SwingMItem extends MItem {
        
        private JMenuItem fItem;
        
        SwingMItem(JMenuItem item) {
            
            fItem = item;
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    handleSelected();
                }
            });
        }
        
        protected void handleSelected() {
            
            super.handleSelected();
        }
        
        public final void setEnabled(boolean enabled) {
            
            fItem.setEnabled(enabled);
        }
        
        public void setState(boolean checked) {
            
            try {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) fItem;
                item.setState(checked);
            }
            catch(ClassCastException e) {
                throw new Error("Cannot perform setChecked on non-checkbox item");
            }
        }
    }
    
    private JMenu fMenu;
        
    SwingMenuFactory(JMenu menu) {
            
        fMenu = menu;
    }
    
    private MItem handleCreate(JMenuItem item,
                               MenuData menuData) {

        if (menuData.hasShortcut()) {
            KeyStroke ks = KeyStroke.getKeyStroke(menuData.getShortcutKeyCode(),
                                                  Event.CTRL_MASK);
            item.setAccelerator(ks);
        }
        
        fMenu.add(item);
        
        return new SwingMItem(item);
    }
        
    public MItem createItem(MenuData menuData) {
        
        return handleCreate(new JMenuItem(menuData.getName()), menuData);
    }        

    public MItem createCheckboxItem(MenuData menuData) {
        
        return handleCreate(new JCheckBoxMenuItem(menuData.getName()), menuData);
    }
    
    public void createSeparator() {
        
        fMenu.add(new JSeparator());
    }
}
