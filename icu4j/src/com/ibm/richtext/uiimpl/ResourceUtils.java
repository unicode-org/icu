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

import java.util.ResourceBundle;
import java.util.MissingResourceException;

import com.ibm.richtext.uiimpl.resources.FrameResources;
import com.ibm.richtext.uiimpl.resources.MenuData;

public class ResourceUtils {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static ResourceBundle BUNDLE;
    static {
        try {
            BUNDLE = ResourceBundle.getBundle("com.ibm.richtext.uiimpl.resources.FrameResources");
        }
        catch(MissingResourceException e) {
            System.out.println("Couldn't load resourceXXX.  " +
                               "Exception: " + e);
            BUNDLE = new FrameResources();
        }
    }

    public static MenuData getMenuData(String key) {
        
        return (MenuData) BUNDLE.getObject(key);
    }
    
    public static String getResourceString(String key) {
        
        return BUNDLE.getString(key);
    }
}