/*
 * @(#)$RCSfile: FontUtils.java,v $ $Revision: 1.1 $ $Date: 2000/04/20 17:30:44 $
 *
 * (C) Copyright IBM Corp. 1998-1999.  All Rights Reserved.
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
package com.ibm.textlayout;

import java.awt.Font;

public final class FontUtils {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    public static Font getFont(java.util.Map attributes) {

        return Font.getFont(attributes);
    }
}