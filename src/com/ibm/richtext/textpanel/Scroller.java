/*
 * (C) Copyright IBM Corp. 1998-2005.  All Rights Reserved.
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
package com.ibm.richtext.textpanel;

/*
    7/9/97 - changed some deprecated methods in Scrollbar
            Also setting Unit and Block increment values.  Maybe
            it matters...
    6/29/98 - reimplemented this class.  Now this class talks to
              any component which implements Scroller.Client.
              ScrollHolder is gone, too.
    2/4/99 - No longer a Panel.  Also, doesn't create Scrollbars,
             and in fact doesn't even use the Scrollbar class
             directly.
*/

import java.awt.Component;
import java.awt.Rectangle;

import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.Adjustable;

/**
* This class manages the interaction between a scrollable client
* and vertical and horizontal scrollbars.  It calls the client's
* scrollTo method in response to manipulation of the scroll bars.
*
* This class used to be a Panel containing the scrollbars and
* the client panel.  As part of the migration away from direct
* AWT dependencies, this class is no longer part of the view
* hierarchy.  Instead it simply keeps a reference to its
* client and scroll bars.  It is the responsibility of higher-
* level classes to set up the view hierarchy.
*/
final class Scroller implements AdjustmentListener
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    static interface Client {
        void scrollTo(int x, int y);
        Rectangle getScrollSize();
        Rectangle getBounds();
    }

    private Adjustable fHorizScrollBar = null;
    private Adjustable fVertScrollBar = null;
    private Client fClient = null;

    /**
     * These are used if the respective Scrollbar is not present.
     */
    private int fHorizValue, fVertValue;

    private static final int DEFAULT_UNIT_INC = 10;

    /**
     * Construct a new Scroller with the given Adjustables,
     * which really should be scrollbars of some ilk.
     * Also, the Adjustables are required to be AWT Components,
     * so the Scroller can enable and disable them.
     * However, a Scroller can work with either AWT Scrollbars
     * or JFC JScrollbars.
     * @param horizScrollBar the horizontal scrollbar.  null if
     * there is no horizontal scrollbar.
     * @param vertScrollBar the vertical scrollbar.  null if
     * there is no vertical scrollbar.
     */
    public Scroller(Adjustable horizScrollBar,
                    Adjustable vertScrollBar) {

        //setLayout(new ScrollBarLayout());

        fHorizScrollBar = horizScrollBar;
        fVertScrollBar = vertScrollBar;

        if (fVertScrollBar != null) {
            fVertScrollBar.setUnitIncrement(DEFAULT_UNIT_INC);
            fVertScrollBar.addAdjustmentListener(this);
        }
        if (fHorizScrollBar != null) {
            fHorizScrollBar.setUnitIncrement(DEFAULT_UNIT_INC);
            fHorizScrollBar.addAdjustmentListener(this);
        }
    }

    public void setClient(Client client) {

        fClient = client;
        clientScrollSizeChanged();
    }

    public void adjustmentValueChanged(AdjustmentEvent event) {

        // variable not used boolean horizontal;
        if (event.getAdjustable() == fHorizScrollBar) {
            int vertVal = fVertScrollBar == null? fVertValue :
                                        fVertScrollBar.getValue();
            scrollTo(event.getValue(), vertVal);
        }
        else {
            int horizVal = fHorizScrollBar == null? fHorizValue :
                                        fHorizScrollBar.getValue();
            scrollTo(horizVal, event.getValue());
        }
    }

    private void setValues(Adjustable scrollbar,
                           int visible,
                           int minimum,
                           int height) {

        int maximum = minimum+height;
        
        if (scrollbar != null) {

            Component scrollbarToo = (Component) scrollbar;

            if (maximum <= visible) {
                scrollbarToo.setEnabled(false);
            }
            else {
                scrollbarToo.setEnabled(true);
            }
            
            scrollbar.setMinimum(minimum);
            scrollbar.setMaximum(maximum);
            scrollbar.setVisibleAmount(visible);
        // workaround setBlockIncrement warnings for increments < 1
        scrollbar.setBlockIncrement(Math.max(1, visible - DEFAULT_UNIT_INC));
        }
    }


    public void clientScrollSizeChanged()
    {
        Rectangle bounds = fClient.getBounds();
        Rectangle preferredSize = fClient.getScrollSize();

        setValues(fHorizScrollBar, bounds.width, preferredSize.x, preferredSize.width);
        setValues(fVertScrollBar, bounds.height, preferredSize.y, preferredSize.height);
    }

    public void setPosition(int x, int y) {

        if (fHorizScrollBar != null) {
            fHorizScrollBar.setValue(x);
        }
        else {
            fHorizValue = x;
        }
        if (fVertScrollBar != null) {
            fVertScrollBar.setValue(y);
        }
        else {
            fVertValue = y;
        }
    }

    private void scrollTo(int x, int y)
    {
        fClient.scrollTo(x, y);
    }

    public void setHorizLineDistance(int newDistance)
    {
        if (fHorizScrollBar != null) {
            fHorizScrollBar.setUnitIncrement(newDistance);
        }
    }

    public void setHorizPageOverlap(int newOverlap)
    {
        if (fHorizScrollBar != null) {
            fHorizScrollBar.setBlockIncrement( // workaround warnings for values < 1 on unix
                    Math.max(1, fHorizScrollBar.getVisibleAmount()-newOverlap));
        }
    }

    public void setVertLineDistance(int newDistance)
    {
        if (fVertScrollBar != null) {
            fVertScrollBar.setUnitIncrement(newDistance);
        }
    }

    public void setVertPageOverlap(int newOverlap)
    {
        if (fVertScrollBar != null) {
            fVertScrollBar.setBlockIncrement( // workaround warnings for values < 1 on unix
                    Math.max(1, fVertScrollBar.getVisibleAmount()-newOverlap));
        }
    }
}
