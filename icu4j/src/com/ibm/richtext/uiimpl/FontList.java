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
// Requires Java2
package com.ibm.richtext.uiimpl;

import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

final class FontList {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private static final String[] stripThese = {
        ".bold", ".bolditalic", ".italic"
    };

    public static String[] getFontList() {

        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                            .getAvailableFontFamilyNames();
        Vector v = new Vector(names.length);
        for (int i=0; i < names.length; i++) {
            v.addElement(names[i]);
        }

        Collections.sort(v);
                
        String last = "";
        
        Iterator iter = v.listIterator();
        while (iter.hasNext()) {
            String current = (String) iter.next();
            testSuffixes: for (int i=0; i < stripThese.length; i++) {
                if (current.endsWith(stripThese[i])) {
                    int baseLen = current.length()-stripThese[i].length();
                    String base = current.substring(0, baseLen);
                    if (base.equalsIgnoreCase(last)) {
                        iter.remove();
                        current = last;
                        break testSuffixes;
                    }
                }
            }
            last = current;
        }
        
        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }
}
