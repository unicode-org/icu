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
package com.ibm.richtext.awtui;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.ibm.richtext.uiimpl.resources.MenuData;
import com.ibm.richtext.uiimpl.MItem;
import com.ibm.richtext.uiimpl.MItem.ItemFactory;

final class AwtMenuFactory implements ItemFactory {
        
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private final class AwtMItem extends MItem {
        
        private MenuItem fItem;
        
        AwtMItem(MenuItem item) {
            
            fItem = item;
            try {
                CheckboxMenuItem chItem = (CheckboxMenuItem) fItem;
                chItem.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        handleSelected();
                    }
                });
            }
            catch(ClassCastException e) {
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        handleSelected();
                    }
                });
            }
        }
        
        // Ack - how do you do this from inner classes??
        protected void handleSelected() {
            
            super.handleSelected();
        }
        
        public final void setEnabled(boolean enabled) {
            
            fItem.setEnabled(enabled);
        }
        
        public void setState(boolean checked) {
            
            try {
                CheckboxMenuItem item = (CheckboxMenuItem) fItem;
                item.setState(checked);
            }
            catch(ClassCastException e) {
                throw new Error("Cannot perform setChecked on non-checkbox item");
            }
        }
    }
    
    private Menu fMenu;
        
    AwtMenuFactory(Menu menu) {
            
        fMenu = menu;
    }
    
    private MItem handleCreate(MenuItem item,
                               MenuData menuData) {

        if (menuData.hasShortcut()) {
            item.setShortcut(new MenuShortcut(menuData.getShortcutChar()));
        }
        
        fMenu.add(item);
        
        return new AwtMItem(item);
    }
        
    public MItem createItem(MenuData menuData) {
        
        return handleCreate(new MenuItem(menuData.getName()), menuData);
    }        

    public MItem createCheckboxItem(MenuData menuData) {
        
        return handleCreate(new CheckboxMenuItem(menuData.getName()), menuData);
    }
    
    public void createSeparator() {
        
        fMenu.add(new MenuItem("-"));
    }
}
    
