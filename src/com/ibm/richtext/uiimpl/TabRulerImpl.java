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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;

import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import com.ibm.richtext.styledtext.MTabRuler;
import com.ibm.richtext.styledtext.StandardTabRuler;
import com.ibm.richtext.styledtext.TabStop;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textpanel.TextPanelListener;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;

/**
 * TabRuler is a Component which presents a user interface for
 * setting the leading margin, trailing margin, first line indent,
 * and tab types and positions.
 * <p>
 * TabRuler does not implement TextPanelListener directly;  however,
 * it can receive updates from a MTextPanel.  To have a TabRuler listen
 * to a panel, call <code>listenToPanel</code>.  TabRuler responds to
 * user manipulation by modifying the paragraph styles on its MTextPanel
 * (if any).
 */
public final class TabRulerImpl implements MouseListener, MouseMotionListener
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final class TabStopBuffer {
        public int fPosition;
        public byte fType;

        TabStopBuffer(int position, byte type) {
            fPosition = position;
            fType = type;
        }

        TabStopBuffer(TabStop tab) {
            this(tab.getPosition(), tab.getType());
        }

        TabStop getTabStop() {
            return new TabStop(fPosition, fType);
        }
    }

    private static final class TabRulerModifier extends StyleModifier {

        private TabStop fOldTab; // tab to remove
        private TabStop fNewTab; // tab to add
        private AttributeMap fPanelDefaults;
        
        TabRulerModifier(TabStop oldTab,
                         TabStop newTab,
                         AttributeMap panelDefaults) {
            fOldTab = oldTab;
            fNewTab = newTab;
            fPanelDefaults = panelDefaults;
        }

        public AttributeMap modifyStyle(AttributeMap oldStyle) {

            MTabRuler oldRuler = (MTabRuler) getWithDefault(TextAttribute.TAB_RULER, 
                                                            oldStyle, 
                                                            fPanelDefaults);
            MTabRuler ruler = oldRuler;

            if (fOldTab != null) {
                if (ruler.containsTab(fOldTab)) {
                    ruler = ruler.removeTab(fOldTab.getPosition());
                }
            }
            if (fNewTab != null) {
                ruler = ruler.addTab(fNewTab);
            }

            if (ruler != oldRuler) {
                return oldStyle.addAttribute(TextAttribute.TAB_RULER, ruler);
            }
            else {
                return oldStyle;
            }
        }
    }

    private static final class ImageCache {

        static final String COPYRIGHT =
                    "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
        private Image fImage = null;
        private boolean fIsValid = false;
        private Component fComponent;   // workaround for compiler bug, 
                                        // should just be able to say Component.this
                                        // if this were not a static class
        
        ImageCache(Component component) {
        
            fComponent = component;
        }

        Graphics getGraphics(int width, int height) {

            if (width <= 0 || height <= 0) {
                return null;
            }
            
            Image image = fImage;
            if (image == null || image.getWidth(fComponent) < width
                              || image.getHeight(fComponent) < height) {
        if (!fComponent.isVisible()) { // fix race condition if component not fully initialized
            return null;
        }
                image = fComponent.createImage(width, height);
            }
            Graphics g = image.getGraphics();
            fImage = image;
            return g;
        }

        void drawImage(Graphics g, int x, int y, Color color) {

            if (!fIsValid) {
                throw new Error("Drawing image when not valid");
            }
            g.drawImage(fImage, x, y, color, fComponent);
        }

        boolean isValid() {

            return fIsValid;
        }

        void setValid(boolean isValid) {

            fIsValid = isValid;
        }
    }

    /**
    * This class listens to a MTextPanel for changes which
    * affect a TabRuler's appearance, and updates the TabRuler
    * as necessary.
    * @see TabRuler
    * @see com.ibm.richtext.textpanel.MTextPanel
    */
    private static final class Updater implements TextPanelListener {

        static final String COPYRIGHT =
                    "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
        private TabRulerImpl fTabRuler;
        private MTextPanel fTextPanel;

        /**
        * Create a new TabRulerUpdater.
        * @param tabRuler the TabRuler to update when a change occurs
        *     in the MTextPanel
        */
        Updater(TabRulerImpl tabRuler) {

            fTabRuler = tabRuler;
        }

        /**
        * Remove self as listener from previous MTextPanel,
        * set current MTextPanel and listen to it (if not null).
        */
        void setTextPanel(MTextPanel textPanel) {

            if (fTextPanel != null) {
                fTextPanel.removeListener(this);
            }

            fTextPanel = textPanel;

            if (fTextPanel != null) {
                fTextPanel.addListener(this);
                setAll();
            }
        }

        private void setAll() {

            int offset = fTextPanel.getSelectionStart();
            boolean leftToRight = fTextPanel.paragraphIsLeftToRight(offset);
            AttributeMap style = fTextPanel.getText().paragraphStyleAt(offset);
            fTabRuler.set(style, false);
            fTabRuler.setFormatWidth(fTextPanel.getFormatWidth(), false);
            fTabRuler.setLeftToRight(leftToRight, true);
        }

        /**
        * TextPanelListener method.  This class responds to text
        * changes by updating its TabRuler.
        */
        public void textEventOccurred(TextPanelEvent event) {

            int changeCode = event.getID();

            if (changeCode == TextPanelEvent.SELECTION_STYLES_CHANGED ||
                    changeCode == TextPanelEvent.TEXT_CHANGED) {

                int offset = fTextPanel.getSelectionStart();
                AttributeMap style = fTextPanel.getText().paragraphStyleAt(offset);
                boolean leftToRight = fTextPanel.paragraphIsLeftToRight(offset);
                fTabRuler.set(style, false);
                fTabRuler.setLeftToRight(leftToRight, true);
            }
            else if (changeCode == TextPanelEvent.FORMAT_WIDTH_CHANGED) {
                
                fTabRuler.setFormatWidth(fTextPanel.getFormatWidth(), true);
            }
        }
        
        /**
        * TextPanelListener method.
        */
        public boolean respondsToEventType(int type) {
            
            return type == TextPanelEvent.SELECTION_STYLES_CHANGED ||
                    type == TextPanelEvent.TEXT_CHANGED ||
                    type == TextPanelEvent.FORMAT_WIDTH_CHANGED;
        }
    }

    /**
     * The default background color for TabRulers.
     * @see #setBackColor
     */
    public static final Color DEFAULT_BACK_COLOR = Color.lightGray;

    private static final int kTrackNone = 0;
    private static final int kTrackTab = 1;
    private static final int kTrackLM = 2;
    private static final int kTrackFLI = 3;
    private static final int kTrackTM = 4;

    private Component fHost;
    private MTabRuler fRuler;
    private int fLeadingMargin;
    private int fFirstLineIndent;
    private int fFormatWidth;
    private int fTrailingMarginPosition; // opposite of actual trailing margin
    private boolean fLeftToRight;
    private int fBaseline;
    private int fOrigin;
    private Color fBackColor = DEFAULT_BACK_COLOR;

    private int fTrackItem; // 0 - none, 1 - tab, 2 - lm, 3 - fli, 4 - tm
    private TabStopBuffer fTrackTab;
    private TabStop fOldTab;
    private int fTrackDelta;
    private boolean fTrackVisible;
    private Updater fUpdater;
    private MTextPanel fTextPanel = null;

    private ImageCache fImageCache;

    /**
     * Create a new TabRuler.
     * @param baseline the y-coordinate of the ruler's baseline
     * @param origin the x-coordinate in this Component where
     *     the left margin appears
     * @param textPanel the MTextPanel to listen to.  This TabRuler
     *     will reflect the MTextPanel's paragraph styles, and update
     *     the paragraph styles when manipulated.
     */
    public TabRulerImpl(int baseline, 
                        int origin, 
                        MTextPanel textPanel,
                        Component host) {

        fHost = host;
        fImageCache = new ImageCache(host);
        fUpdater = new Updater(this);
        fBaseline = baseline;
        fOrigin = origin;
        host.addMouseListener(this);
        host.addMouseMotionListener(this);
        if (textPanel != null) {
            listenToTextPanel(textPanel);
        }
        else {
            fRuler = new StandardTabRuler();
        }
    }

    /**
     * Listen to the given MTextPanel and reflect its changes,
     * and update its paragraph styles when TabRuler is
     * manipulated.
     * @param textPanel the MTextPanel to listen to
     */
    public void listenToTextPanel(MTextPanel textPanel) {

        fTextPanel = textPanel;
        fUpdater.setTextPanel(textPanel);
    }

    /**
     * Return the background color of this TabRuler.
     * @return the background color of this TabRuler
     */
    public Color getBackColor() {

        return fBackColor;
    }

    /**
     * Set the background color of this TabRuler.
     * @param backColor the new background color of this TabRuler
     */
    public void setBackColor(Color backColor) {

        if (!backColor.equals(fBackColor)) {
            fBackColor = backColor;
            Graphics g = fHost.getGraphics();
            if (g != null) {
                paint(g);
            }
        }
    }

    private static Object getWithDefault(Object key,
                                         AttributeMap style,
                                         AttributeMap defaults) {
        Object value = style.get(key);
        if (value == null) {
            value = defaults.get(key);
        }
        return value;
    }

    private static float getFloatWithDefault(Object key,
                                             AttributeMap style,
                                             AttributeMap defaults) {
        Object value = getWithDefault(key, style, defaults);
        return ((Float)value).floatValue();
    }
    
    private void setLeftToRight(boolean leftToRight, boolean update) {
        
        if (fLeftToRight != leftToRight) {
            
            fLeftToRight = leftToRight;
            redrawSelf(update);
        }
    }

    private void setFormatWidth(int formatWidth, boolean update) {
        
        if (fFormatWidth != formatWidth) {
            
            fTrailingMarginPosition += (formatWidth - fFormatWidth);
            fFormatWidth = formatWidth;
            redrawSelf(update);
        }
    }
    
    /**
     * Set TabRuler from values in paragraphStyle.  Only TabRulerUpdater
     * should call this method.
     * @param paragraphStyle the paragraph style which the TabRuler will
     *     reflect
     */
    private void set(AttributeMap paragraphStyle, boolean update) {

        AttributeMap panelDefaults;
        
        if (fTextPanel==null) {
            panelDefaults = TextPanel.getDefaultSettings().getDefaultValues();
        }
        else {
             panelDefaults = fTextPanel.getDefaultValues();
        }
                                                         
        int leadingMargin =  (int) getFloatWithDefault(TextAttribute.LEADING_MARGIN,
                                                       paragraphStyle,
                                                       panelDefaults);
        int firstLineIndent = (int) getFloatWithDefault(TextAttribute.FIRST_LINE_INDENT,
                                                        paragraphStyle,
                                                        panelDefaults);
        int trailingMargin = (int) getFloatWithDefault(TextAttribute.TRAILING_MARGIN,
                                                       paragraphStyle,
                                                       panelDefaults);

        MTabRuler ruler = (MTabRuler) getWithDefault(TextAttribute.TAB_RULER,
                                                     paragraphStyle,
                                                     panelDefaults);

        int ourFli = leadingMargin + firstLineIndent;
        int ourTmp = fFormatWidth - trailingMargin;
        
        if (leadingMargin == fLeadingMargin &&
                fFirstLineIndent == ourFli &&
                fTrailingMarginPosition == ourTmp &&
                ruler.equals(fRuler)) {
            return;
        }
        
        fLeadingMargin = leadingMargin;
        fFirstLineIndent = ourFli;
        fTrailingMarginPosition = ourTmp;
        fRuler = ruler;
    
        redrawSelf(update);
    }
    
    private void redrawSelf(boolean drawNow) {

        fImageCache.setValid(false);

        Graphics g = fHost.getGraphics();
        if (g != null)
            paint(g);
    }

    /**
     * Return debugging info.
     */
    public String toString() {

        return "TabRuler{fLeadingMargin="+fLeadingMargin+
                "}{fFirstLineIndent="+fFirstLineIndent+
                "}{fFormatWidth="+fFormatWidth+
                "}{fTrailingMarginPosition="+fTrailingMarginPosition+
                "}{fRuler="+fRuler+
                "}";
    }

    /**
     * Return the MTabRuler represented by this TabRuler.
     * @return the MTabRuler represented by this TabRuler
     */
    public MTabRuler getRuler()
    {
        return fRuler;
    }

    /**
     * Return the leading margin of this TabRuler.
     * @return the leading margin of this TabRuler
     */
    public int getLeadingMargin()
    {
        return fLeadingMargin;
    }

    /**
     * Return the first line indent of this TabRuler.
     * @return the first line indent of this TabRuler
     */
    public int getFirstLineIndent()
    {
        return fFirstLineIndent - fLeadingMargin;
    }

    /**
     * Return the trailing margin of this TabRuler.
     * @return the trailing margin of this TabRuler
     */
    public final int getTrailingMargin()
    {
        return fFormatWidth - fTrailingMarginPosition;
    }
    
    private int visualToRulerPos(int visPos) {
        
        if (fLeftToRight) {
            return visPos - fOrigin;
        }
        else {
            return fOrigin + fFormatWidth - visPos;
        }
    }
    
    private int rulerToVisualPos(int rulerPos) {
        
        if (fLeftToRight) {
            return fOrigin + rulerPos;
        }
        else {
            return fOrigin + fFormatWidth - rulerPos;
        }
    }
    
    private int dirMult() {
        
        return fLeftToRight? 1 : -1;
    }

    /**
     * @param tabPosition the logical (ruler) position of the tab
     */
    private void drawTab(Graphics g, int tabPosition, byte tabType, int tabTop, int tabBottom)
    {
        int pos = rulerToVisualPos(tabPosition);
        int wid = 0;
        switch (tabType) {
            case TabStop.kLeading: wid = 3; break;
            case TabStop.kCenter: wid = 0; break;
            case TabStop.kTrailing: wid = -3; break;
            case TabStop.kDecimal: wid = 0; break;
            default: break;
        }
        wid *= dirMult();
        
        if (tabType != TabStop.kAuto) {
            g.drawLine(pos, tabTop, pos, tabBottom);
            if (wid != 0)
                g.drawLine(pos, tabBottom, pos + wid, tabBottom);
        }
        g.drawLine(pos-2, tabTop+2, pos, tabTop);
        g.drawLine(pos, tabTop, pos+2, tabTop+2);
        if (tabType == TabStop.kDecimal) {
            g.drawLine(pos + 3, tabBottom, pos + 4, tabBottom);
        }
    }

    private void drawLM(Graphics g)
    {
        int pos = rulerToVisualPos(fLeadingMargin);
        int[] xpts = { pos, pos, pos + (4*dirMult()), pos };
        int[] ypts = { fBaseline + 12, fBaseline + 7, fBaseline + 7, fBaseline + 12 };
        g.fillPolygon(xpts, ypts, 3);
        g.drawPolygon(xpts, ypts, 4);
    }

    private void drawFLI(Graphics g)
    {
        int pos = rulerToVisualPos(fFirstLineIndent);
        int[] xpts = { pos, pos, pos + (4*dirMult()), pos };
        int[] ypts = { fBaseline, fBaseline + 5, fBaseline + 5, fBaseline };
        g.fillPolygon(xpts, ypts, 3);
        g.drawPolygon(xpts, ypts, 4);
    }

    private void drawRM(Graphics g)
    {
        int pos = rulerToVisualPos(fTrailingMarginPosition);
        int[] xpts = { pos, pos, pos - (6*dirMult()), pos };
        int[] ypts = { fBaseline, fBaseline + 12, fBaseline + 6, fBaseline };
        g.fillPolygon(xpts, ypts, 3);
        g.drawPolygon(xpts, ypts, 4);
    }

    private static int alignInt(int value) {
        
        return (int)((int)(value / 4.5) * 4.5);
    }

    private static final int[] fgLengths = { 10, 2, 4, 2, 6, 2, 4, 2 };

    /**
     * Component method override.
     */
    public void paint(Graphics g)
    {
        Dimension size = fHost.getSize();

        int width = size.width;
        int baseline = fBaseline;
        int baseline2 = baseline + 2;
        int baseline10 = baseline + 10;
        int baseline12 = baseline + 12;

        if (!fImageCache.isValid()) {

            Graphics gCache = fImageCache.getGraphics(width, baseline12 + 1);
            if (gCache == null) {
                return;
            }
            
            // set background color

            gCache.setColor(fBackColor);
            gCache.setPaintMode();
            gCache.fillRect(0, 0, width, baseline12 + 1);

            // paint ticks

            gCache.setColor(Color.black);
            gCache.drawLine(0, 0, width, 0);
            gCache.drawLine(0, baseline, width, baseline);

            int[] lengths = fgLengths;

            int index = 0;
            int inchnum = 0;
            FontMetrics fm = null;
            if (!fLeftToRight) {
                fm = gCache.getFontMetrics();
            }
            
            for (int i = 0; i < fFormatWidth; i += 9) {
                int len = lengths[index];
                int pos = rulerToVisualPos(i);
                gCache.drawLine(pos, baseline, pos, baseline - len);

                if (index == 0) {
                    String str = Integer.toString(inchnum++);
                    int drawX;
                    if (fLeftToRight) {
                        drawX = pos + 2;
                    }
                    else {
                        drawX = pos - fm.stringWidth(str) - 2;
                    }
                        
                    gCache.drawString(str, drawX, baseline - 2);
                }

                if (++index == lengths.length)
                    index = 0;
            }

            // paint tabs
            TabStop tab = fRuler.firstTab();
            while (tab != null && tab.getPosition() < fTrailingMarginPosition) {
                boolean dodraw = true;
                if (tab.getType() == TabStop.kAuto) {
                    if (tab.getPosition() <= Math.max(fLeadingMargin, fFirstLineIndent))
                        dodraw = false;
                    else if (tab.getPosition() >= fTrailingMarginPosition)
                        dodraw = false;
                }

                if (dodraw)
                    drawTab(gCache, tab.getPosition(), tab.getType(), baseline2, baseline10);

                tab = fRuler.nextTab(tab.getPosition());
            }

            gCache.drawLine(0, baseline12, width, baseline12);

            // paint others except for tracked item
            if (fTrackItem != kTrackLM) drawLM(gCache);
            if (fTrackItem != kTrackTM) drawRM(gCache);
            if (fTrackItem != kTrackFLI && fTrackItem != kTrackLM) drawFLI(gCache);
            fImageCache.setValid(true);
        }

        fImageCache.drawImage(g, 0, 0, Color.lightGray);

        switch (fTrackItem) {
            case kTrackTab: if (fTrackVisible) drawTab(g, fTrackTab.fPosition, fTrackTab.fType, baseline2, baseline10); break;
            case kTrackLM: drawLM(g); drawFLI(g); break;
            case kTrackTM: drawRM(g); break;
            case kTrackFLI: drawFLI(g); break;
            default: break;
        }
    }

    /**
     * MouseListener method.
     */
    public void mouseClicked(MouseEvent e) {}
    /**
     * MouseListener method.
     */
    public void mouseEntered(MouseEvent e) {}
    /**
     * MouseListener method.
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * MouseListener method.
     */
    public void mousePressed(MouseEvent e)
    {
        // find out if we hit a tabstop
        int x = visualToRulerPos(e.getX());
        int y = e.getY();

        if (y > fBaseline && y < fBaseline + 12) {
            if (y >= fBaseline + 7 && x >= fLeadingMargin - 3 && x <= fLeadingMargin + 3) {
                fTrackItem = kTrackLM;
                fTrackDelta = fLeadingMargin - x;
            } else if (y < fBaseline + 7 && x >= fFirstLineIndent - 3 && x <= fFirstLineIndent + 3) {
                fTrackItem = kTrackFLI;
                fTrackDelta = fFirstLineIndent - x;
            } else if (x >= fTrailingMarginPosition - 3 && x <= fTrailingMarginPosition + 3) {
                fTrackItem = kTrackTM;
                fTrackDelta = fTrailingMarginPosition - x;
            } else if (e.isControlDown()) {
                fTrackItem = kTrackTab;
                fTrackTab = new TabStopBuffer(alignInt(x), TabStop.kLeading);
                fTrackDelta = fTrackTab.fPosition - x;
                fTrackVisible = true;
            } else {
                TabStop tab = fRuler.firstTab();
                while (tab.getType() != TabStop.kAuto) {
                    if (x < tab.getPosition() - 3)
                        break;
                    if (x < tab.getPosition() + 3) {
                        fOldTab = tab;
                        fTrackTab = new TabStopBuffer(tab);
                        fRuler = fRuler.removeTab(fOldTab.getPosition());

                        if (e.getClickCount() > 1) {
                            switch (fTrackTab.fType) {
                                case TabStop.kLeading: fTrackTab.fType = TabStop.kCenter; break;
                                case TabStop.kCenter: fTrackTab.fType = TabStop.kTrailing; break;
                                case TabStop.kTrailing: fTrackTab.fType = TabStop.kDecimal; break;
                                case TabStop.kDecimal: fTrackTab.fType = TabStop.kLeading; break;
                                default: break;
                            }
                        }
                        fTrackItem = kTrackTab;
                        fTrackDelta = tab.getPosition() - x;
                        fTrackVisible = true;
                        break;
                    }

                    tab = fRuler.nextTab(tab.getPosition());
                }
            }

            if (fTrackItem != kTrackNone) {
                fImageCache.setValid(false);
                paint(fHost.getGraphics());
                return;
            }
        }
    }

    /**
     * MouseListener method.
     */
    public void mouseDragged(MouseEvent e)
    {
        int x = visualToRulerPos(e.getX());
        int y = e.getY();

        if (fTrackItem != kTrackNone) {
            boolean repaint = false;
            boolean inrange = y > fBaseline && y < fBaseline + 12;
            boolean inbigrange = y > 0 && y < fHost.getSize().height + 20;
            int newpos = alignInt(x + fTrackDelta);
            if (newpos < 0)
                newpos = 0;

            switch (fTrackItem) {
                case kTrackTab: {
                    if (inrange) {
                        repaint = !fTrackVisible;
                        fTrackVisible = true;
                        if (newpos != fTrackTab.fPosition) {
                            fTrackTab.fPosition = newpos;
                            repaint = true;
                        }
                    } else if (fTrackVisible) {
                        fTrackVisible = false;
                        repaint = true;
                    }
                } break;

/* It would be nice to optionally track the margin 'independently' of the first line indent.
 Unfortunately this makes for more work when we have multiple paragraph styles selected.
 Since internally the first line indent is relative to the margin, moving the margin
 independently so that all affected paragraphs share the same margin but retain first
 line indents in the 'same' positions means that I need to also adjust the first line
 indents in each paragraph by some delta. I'm not ready to do that yet. */

                case kTrackLM: {
                    if (inbigrange && newpos != fLeadingMargin) {
                        fFirstLineIndent += newpos - fLeadingMargin;
                        fLeadingMargin = newpos;
                        repaint = true;
                    }
                } break;

                case kTrackFLI: {
                    if (inbigrange && newpos != fFirstLineIndent) {
                        fFirstLineIndent = newpos;
                        repaint = true;
                    }
                } break;

                case kTrackTM: {
                    if (inbigrange && newpos != fTrailingMarginPosition) {
                        fTrailingMarginPosition = newpos;
                        repaint = true;
                    }
                } break;
            }


            if (repaint)
                paint(fHost.getGraphics());
        }
    }

    /**
     * MouseListener method.
     */
    public void mouseReleased(MouseEvent e)
    {
        if (fTrackItem != kTrackNone) {
            if (fTrackItem == kTrackTab && fTrackVisible) {
                fRuler = fRuler.addTab(fTrackTab.getTabStop());
            } else {
                fTrackTab = null;
            }

            notify(fTrackItem);

            fTrackItem = kTrackNone;
            fTrackTab = null;
            fOldTab = null;

            fImageCache.setValid(false);
            paint(fHost.getGraphics());
        }
    }

    /**
     * MouseListener method.
     */
    public void mouseMoved(MouseEvent e) {}

    private void notify(int change)
    {
        if (fTextPanel != null) {

            StyleModifier modifier;

            if (change == kTrackTab) {
                TabStop newTab = fTrackTab==null? null : fTrackTab.getTabStop();
                modifier = new TabRulerModifier(fOldTab, newTab, fTextPanel.getDefaultValues());
            }
            else {
                Object key;
                Object value;

                switch(change) {
                    case kTrackLM:
                        key = TextAttribute.LEADING_MARGIN;
                        value = new Float(getLeadingMargin());
                        break;

                    case kTrackTM:
                        key = TextAttribute.TRAILING_MARGIN;
                        value = new Float(getTrailingMargin());
                        break;

                    case kTrackFLI:
                        key = TextAttribute.FIRST_LINE_INDENT;
                        value = new Float(getFirstLineIndent());
                        break;

                    default:
                        throw new Error("Invalid change code.");
                }

                modifier = StyleModifier.createAddModifier(key, value);
            }

            fTextPanel.modifyParagraphStyleOnSelection(modifier);
        }
    }

    /**
     * Component override.
     */
    public Dimension getMinimumSize()
    {
        return new Dimension(100, fBaseline + 13);
    }

    /**
     * Component override.
     */
    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }
}
