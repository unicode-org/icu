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

package com.ibm.richtext.textformat;

import java.awt.Shape;
///*JDK12IMPORTS
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Line2D;
//JDK12IMPORTS*/
/*JDK11IMPORTS
import com.ibm.richtext.textlayout.Graphics2D;
import com.ibm.richtext.textlayout.Rectangle2D;
JDK11IMPORTS*/

/**
 * This class exists to work around a clipping bug in JDK 1.2.
 */
final class ClipWorkaround {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
///*JDK12IMPORTS
    private static final String excuse =
        "Sorry, this method is a very limited workaround for a JDK 1.2 bug.";
//JDK12IMPORTS*/
    
    static Object saveClipState(Graphics2D g) {
///*JDK12IMPORTS
        return null;
//JDK12IMPORTS*/
/*JDK11IMPORTS
        return g.getClip();
JDK11IMPORTS*/
    }
    
    static void restoreClipState(Graphics2D g, Object state) {
///*JDK12IMPORTS
        if (state != null) {
            throw new Error("Invalid clip state for this class.");
        }
//JDK12IMPORTS*/
/*JDK11IMPORTS
        g.setClip((Shape)state);
JDK11IMPORTS*/
    }
    
    /**
     * Draw the given Shape into the Graphics, translated by (dx, dy)
     * and clipped to clipRect.
     */
    static void translateAndDrawShapeWithClip(Graphics2D g,
                                              int dx,
                                              int dy,
                                              Rectangle2D clipRect,
                                              Shape shape) {
///*JDK12IMPORTS
        // really bogus implementation right now:  basically only
        // draws carets from a TextLayout.
        // Oh yeah, it doesn't really clip correctly either...

        PathIterator pathIter = shape.getPathIterator(null);
        float[] points = new float[6];

        int type = pathIter.currentSegment(points);
        if (type != PathIterator.SEG_MOVETO) {
            throw new Error(excuse);
        }
        float x1 = points[0] + dx;
        float y1 = points[1] + dy;

        if (pathIter.isDone()) {
            throw new Error(excuse);
        }

        pathIter.next();
        type = pathIter.currentSegment(points);
        if (type != PathIterator.SEG_LINETO) {
            throw new Error(excuse);
        }
        float x2 = points[0] + dx;
        float y2 = points[1] + dy;

        float minY = (float) clipRect.getY();
        float maxY = (float) clipRect.getMaxY();

        // Now clip within vertical limits in clipRect
        if (y1 == y2) {
            if (y1 < minY || y1 >= maxY) {
                return;
            }
        }
        else {
            if (y1 > y2) {
                float t = x1;
                x1 = x2;
                x2 = t;
                t = y1;
                y1 = y2;
                y2 = t;
            }

            float invSlope = (x2-x1) / (y2-y1);
            if (y1 < minY) {
                x1 -= (minY-y1) * invSlope;
                y1 = minY;
            }
            if (y2 >= maxY) {
                x1 += (y2-maxY) * invSlope;
                y2 = maxY;
            }
        }

        g.draw(new Line2D.Float(x1, y1, x2, y2));
//JDK12IMPORTS*/
/*JDK11IMPORTS
        g.setClip(clipRect);
        g.translate(dx, dy);
        try {
            g.draw(shape);
        }
        finally {
            g.translate(-dx, -dy);
        }
JDK11IMPORTS*/
    }
}
