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
package com.ibm.richtext.swingdemo;

import java.awt.Event;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import com.ibm.richtext.demo.DocumentWindow;
import com.ibm.richtext.demo.EditApplication;
import com.ibm.richtext.demo.FileMenuManager;
import com.ibm.richtext.demo.MenuData;
import com.ibm.richtext.demo.ResourceUtils;

final class SwingFileMenuManager extends FileMenuManager {

    private JMenu fMenu;
    
    public SwingFileMenuManager(JMenu menu,
                                EditApplication application, 
                                DocumentWindow document,
                                boolean supportStyledFormat,
                                boolean supportPlainFormat) {
                          
        super(application, document);
        
        fMenu = menu;
        createItems(supportStyledFormat, supportPlainFormat);
        fMenu = null;
    }
    
    protected Object addMenuItem(String key) {

        MenuData menuData = ResourceUtils.getMenuData(key);
        JMenuItem item = new JMenuItem(menuData.getName());
        if (menuData.hasShortcut()) {
            KeyStroke ks = KeyStroke.getKeyStroke(menuData.getShortcutKeyCode(),
                                                  Event.CTRL_MASK);
            item.setAccelerator(ks);
        }
        item.addActionListener(this);
        fMenu.add(item);
        return item;
    }
    
    protected void addSeparator() {
    
        fMenu.add(new JSeparator());
    }
}
