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
package com.ibm.richtext.textpanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

class ScrollBarLayout implements LayoutManager {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    Component   fHorizScrollBar = null;
    Component   fVertScrollBar = null;
    Component   fChild = null;

    public ScrollBarLayout() {
    }

    public void addLayoutComponent(String name, Component comp) {

        if ("Center".equals(name))
            fChild = comp;
        else if ("South".equals(name))
            fHorizScrollBar = comp;
        else if ("East".equals(name))
            fVertScrollBar = comp;
    }

    public void layoutContainer(Container target) {

        Insets      insets = target.getInsets();
        Dimension   targetSize = target.getSize();
        int         hsbHeight = (fHorizScrollBar != null) ? fHorizScrollBar.getPreferredSize().
                            height : 0;
        int         vsbWidth = (fVertScrollBar != null) ? fVertScrollBar.getPreferredSize().
                            width : 0;

        if (fHorizScrollBar != null)
            fHorizScrollBar.setBounds(insets.left, targetSize.height - insets.bottom -
                                hsbHeight, targetSize.width - vsbWidth, hsbHeight);

        if (fVertScrollBar != null)
            fVertScrollBar.setBounds(targetSize.width - insets.right - vsbWidth,
                                insets.top, vsbWidth, targetSize.height - hsbHeight);

        if (fChild != null)
            fChild.setBounds(insets.left, insets.top, targetSize.width - insets.right - vsbWidth,
                                targetSize.height - insets.bottom - hsbHeight);
    }

    public Dimension minimumLayoutSize(Container target) {

        Dimension   returnVal = new Dimension(0, 0);
        Dimension   hsbSize;
        Dimension   vsbSize;
        Dimension   childSize;

        if (fHorizScrollBar != null && fHorizScrollBar.isVisible()) {
            hsbSize = fHorizScrollBar.getMinimumSize();
        }
        else {
            hsbSize = new Dimension(0, 0);
        }

        if (fVertScrollBar != null && fVertScrollBar.isVisible()) {
            vsbSize = fVertScrollBar.getMinimumSize();
        }
        else {
            vsbSize = new Dimension(0, 0);
        }

        if (fChild != null && fChild.isVisible()) {
            childSize = fChild.getMinimumSize();
        }
        else {
            childSize = new Dimension(0, 0);
        }

        returnVal.width = Math.max(childSize.width, hsbSize.width) + vsbSize.width;
        returnVal.height = Math.max(childSize.height, vsbSize.height) + hsbSize.height;

        Insets  insets = target.getInsets();

        returnVal.width += insets.left + insets.right;
        returnVal.height += insets.top + insets.bottom;

        return returnVal;
    }

    public Dimension preferredLayoutSize(Container target) {

        Dimension   returnVal = new Dimension(0, 0);
        Dimension   hsbSize;
        Dimension   vsbSize;
        Dimension   childSize;

        if (fHorizScrollBar != null && fHorizScrollBar.isVisible()) {
            hsbSize = fHorizScrollBar.getPreferredSize();
        }
        else {
            hsbSize = new Dimension(0, 0);
        }

        if (fVertScrollBar != null && fVertScrollBar.isVisible()) {
            vsbSize = fVertScrollBar.getPreferredSize();
        }
        else {
            vsbSize = new Dimension(0, 0);
        }

        if (fChild != null && fChild.isVisible()) {
            childSize = fChild.getPreferredSize();
        }
        else {
            childSize = new Dimension(0, 0);
        }

        returnVal.width = Math.max(childSize.width, hsbSize.width) + vsbSize.width;
        returnVal.height = Math.max(childSize.height, vsbSize.height) + hsbSize.height;

        Insets  insets = target.getInsets();

        returnVal.width += insets.left + insets.right;
        returnVal.height += insets.top + insets.bottom;

        return returnVal;
    }

    public void removeLayoutComponent(Component comp) {

        if (comp == fChild)
            fChild = null;
        else if (comp == fHorizScrollBar)
            fHorizScrollBar = null;
        else if (comp == fVertScrollBar)
            fVertScrollBar = null;
    }
}
