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
package com.ibm.richtext.textlayout;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

/**
 * This class allows JDK 1.1 code to use a "fake" Graphics2D
 * for source-code compatibility with Java2 code.  On Java2 it's
 * a trivial class.  The JDK 1.1 version of this class does interesting
 * work.
 */ 
public final class Graphics2DConversion {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    public static Graphics2D getGraphics2D(Graphics g) {
        return (Graphics2D) g;
    }

    public static Graphics getGraphics(Graphics2D g) {
        return g;
    }

    /**
     * Will return an instance of Paint.
     */
    public static Object getColorState(Graphics2D g) {

        return g.getPaint();
    }

    /**
     * State must be an instance of Paint.
     */
    public static void restoreColorState(Graphics2D g, Object state) {

        g.setPaint((Paint) state);
    }
}