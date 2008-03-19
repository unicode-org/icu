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

import java.util.Hashtable;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

///*JDK12IMPORTS
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
//JDK12IMPORTS*/

/*JDK11IMPORTS
import com.ibm.richtext.textlayout.FontRenderContext;
import com.ibm.richtext.textlayout.TextLayout;
JDK11IMPORTS*/

/**
 * This class is used by the Formatter to estimate the height
 * of characters in a particular style.
 */
final class DefaultCharacterMetric {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    
    final class Metric {
    
        private float fAscent;
        private float fDescent;
        private float fLeading;
    
        private Metric(float ascent, float descent, float leading) {
    
            fAscent = ascent;
            fDescent = descent;
            fLeading = leading;
        }
    
        public int getAscent() {
            return (int) Math.ceil(fAscent);
        }
    
        public int getDescent() {
            return (int) Math.ceil(fDescent);
        }
    
        public int getLeading() {
            return (int) Math.ceil(fLeading);
        }
    }
    
    private final Hashtable fCache = new Hashtable();
    private /*final*/ FontResolver fResolver;
    private /*final*/ FontRenderContext fFrc;
    
    public DefaultCharacterMetric(FontResolver resolver,
                                  FontRenderContext frc) {
    
        fResolver = resolver;
        fFrc = frc;
    }
    
    /**
     * Get a DefaultCharacterMetric instance for the given style.  The
     * style is first resolved with FontResolver.
     */
    public Metric getMetricForStyle(AttributeMap style) {

        style = fResolver.applyFont(style);
        Metric metric = (Metric) fCache.get(style);
        if (metric == null) {
            TextLayout layout = new TextLayout(" ", style, fFrc);
            metric = new Metric(layout.getAscent(),
                                layout.getDescent(),
                                layout.getLeading());
            fCache.put(style, metric);
        }
        return metric;
    }
}