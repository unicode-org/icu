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
package com.ibm.richtext.demo;

import java.awt.Menu;
import java.awt.MenuItem;

final class AwtFileMenuManager extends FileMenuManager {

    private Menu fMenu;
    
    public AwtFileMenuManager(Menu menu,
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

        MenuItem item = ResourceUtils.createMenuItem(key);
        item.addActionListener(this);
        fMenu.add(item);
        return item;
    }
    
    protected void addSeparator() {
    
        fMenu.add(new MenuItem("-"));
    }
}
