/*
 * (C) Copyright IBM Corp. 1998-2007.  All Rights Reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.event.ComponentAdapter;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textformat.TextOffset;

import com.ibm.richtext.textformat.MFormatter;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

class TextComponent extends FakeComponent
                    implements BehaviorOwner,
                    FocusListener,
                    KeyListener,
                    MouseListener,
                    MouseMotionListener,
                    Scroller.Client {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    public static final int WINDOW_WIDTH = -10;
    public static final int DEFAULT_INSET = 10;

    private static final Color STRONG_CARET_COLOR = Color.black;
    private static final Color WEAK_CARET_COLOR = Color.darkGray;

    private Behavior fBehavior;
    private MText fText;
    private StyledTextClipboard fClipboard;
    private boolean fScrolls;
    private Scroller fScroller;

    private DocumentView fDocumentView = null;

    // sigh - can't create DocumentView until addNotify() is called.
    // These values hold DocumentView ctor args
    private AttributeMap fDefaultValues;
    private boolean fViewWraps;
    private int fViewWrapWidth;
    private int fViewInsetAmount;
    
    private PanelEventBroadcaster fListener;

    /**
     * Create a new TextComponent.
     * @param text the text model.  This model will be used for
     * the life of the component, even if setText is called
     * @param wraps if true, the text is wrapped to the specified
     * wrapping width.  If false, the text wraps only at paragraph breaks.
     * @param wrapWidth ignored if wraps is false.  Text wraps to this width
     * unless the width is WINDOW_WIDTH, in which case text wraps to width
     * of this component. Should not be negative (unless it is WINDOW_WIDTH).
     * @param insetAmount the size of the margins around the text
     * @param clipboard the clipboard to use for cut/copy/paste operations.
     * If null, the component will use its own clipboard.
     */
    public TextComponent(MText text,
                         AttributeMap defaultValues,
                         boolean wraps,
                         int wrapWidth,
                         int insetAmount,
                         StyledTextClipboard clipboard,
                         boolean scrolls,
                         Scroller scroller,
                         PanelEventBroadcaster listener) {

        fBehavior = null;

        if (text == null) {
            throw new IllegalArgumentException("Text is null.");
        }

        fText = text;
        fDefaultValues = defaultValues;
        
        if (clipboard == null) {
            throw new IllegalArgumentException("Clipboard is null.");
        }
        fClipboard = clipboard;

        fScrolls = scrolls;

        fScroller = scroller;

        fDocumentView = null;

        fViewWrapWidth = wrapWidth;
        fViewWraps = wraps;
        fViewInsetAmount = insetAmount;
        fListener = listener;
    }
    
    AttributeMap getDefaultValues() {
    
        return fDefaultValues;
    }
    
    void setHost(Component component) {
        
        super.setHost(component);
        
        component.addFocusListener(this);
        component.addKeyListener(this);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        
        component.addComponentListener(new ComponentAdapter() {        
            public void componentResized(ComponentEvent e) {
                if (fDocumentView != null) {
                    fDocumentView.hostSizeChanged();
                    scrollToShow(fDocumentView.getDocumentBounds());
                }
            }
        });
    }
    
    /**
     * ATextPanelImpl's use only!
     */
    Component getHost() {
    
        return fHost;
    }
    
    // Create document view here.  TextComponent isn't fully constructed
    // until this is called.
    // This must be called by host component!
    void addNotify() {

        Graphics g = getGraphics();
        if (g == null) {
            throw new Error("Graphics should be valid here but isn't.");
        }

        fDocumentView = new DocumentView(this,
                                         fText,
                                         fDefaultValues,
                                         fViewWraps,
                                         fViewWrapWidth,
                                         fViewInsetAmount,
                                         fListener);
        documentSizeChanged();
        fListener.textStateChanged(TextPanelEvent.FORMAT_WIDTH_CHANGED);
    }
    
    public Rectangle getBounds() {
        
        if (fHost != null) {
            return fHost.getBounds();
        }
        return new Rectangle(0, 0, 0, 0);
    }
    
    Graphics getGraphics() {
        
        return (fHost==null)? null : fHost.getGraphics();
    }
    
    void requestFocus() {
        
        if (fHost != null) {
            fHost.requestFocus();
        }
    }

    // *** Behavior management ***
    public Behavior getBehavior() {
        return fBehavior;
    }

    public void setBehavior(Behavior b) {
        fBehavior = b;
    }


    // *** Events - just forward to behavior ***
    public void focusGained(FocusEvent event) {
        if (fBehavior != null)
            fBehavior.focusGained(event);
    }

    public void focusLost(FocusEvent event) {
        if (fBehavior != null)
            fBehavior.focusLost(event);
    }

    public void keyPressed(KeyEvent event) {
        if (fBehavior != null)
            fBehavior.keyPressed(event);
    }

    public void keyTyped(KeyEvent event) {

        if (fBehavior != null) {
            fBehavior.keyTyped(event);
        }
    }

    public void keyReleased(KeyEvent event) {
        if (fBehavior != null)
            fBehavior.keyReleased(event);
    }

    public void mouseClicked(MouseEvent event) {
        // no behavior method for this
    }

    public void mouseDragged(MouseEvent event) {
        if (fBehavior != null)
            fBehavior.mouseDragged(event);
    }

    public void mouseEntered(MouseEvent event)  {
        if (fBehavior != null)
            fBehavior.mouseEntered(event);
    }

    public void mouseExited(MouseEvent event)  {
        if (fBehavior != null)
            fBehavior.mouseExited(event);
    }

    public void mouseMoved(MouseEvent event) {
        if (fBehavior != null)
            fBehavior.mouseMoved(event);
    }

    public void mousePressed(MouseEvent event) {
        if (fBehavior != null)
            fBehavior.mousePressed(event);
    }

    public void mouseReleased(MouseEvent event)  {
        if (fBehavior != null)
            fBehavior.mouseReleased(event);
    }

    public boolean textControlEventOccurred(Behavior.EventType event, Object what) {

        boolean handled = false;

        if (fBehavior != null) {
            handled = fBehavior.textControlEventOccurred(event, what);
        }
        return handled;
    }


    // *** Scroll methods - called by Behaviors

    // viewStart, viewLimit is visible bounds of window
    // targetStart, targetLimit is the region to scroll into view
    private static int getScrollDifference(int viewStart,
                                           int viewLimit,
                                           int targetStart,
                                           int targetLimit) {

        if (viewStart <= targetStart) {
            if (viewLimit >= targetLimit) {
                return 0;
            }
            return Math.max(viewStart-targetStart, viewLimit-targetLimit);
        }
        else if (viewLimit > targetLimit) {

            return viewLimit - targetLimit;
        }
        else {
            return 0;
        }
    }

    void scrollToShow(Rectangle showRect) {

        if (fDocumentView != null) {
            Rectangle bounds = getBounds();

            int dx = getScrollDifference(showRect.x, showRect.x + showRect.width,
                                         bounds.x, bounds.x + bounds.width);
            int dy = getScrollDifference(showRect.y, showRect.y + showRect.height,
                                         bounds.y, bounds.y + bounds.height);

            scrollSelf(dx, dy);
        }
    }

    void scrollToShow(int showX, int showY) {

        if (fDocumentView != null) {
            int dx = 0, dy = 0;

            Rectangle bounds = getBounds();
            if (showX < bounds.x) {
                dx = showX - bounds.x;
            }
            else if (showX > bounds.x + bounds.width) {
                dx = showX - (bounds.x + bounds.width);
            }

            if (showY < bounds.y) {
                dy = showY - bounds.y;
            }
            else if (showY > bounds.y + bounds.height) {
                dy = showY - (bounds.y + bounds.height);
            }

            scrollSelf(dx, dy);
        }
    }

    private int pinScrollOffset(int delta,
                                int contentStart,
                                int contentLength,
                                int viewStart,
                                int viewLength) {

        if (delta > 0) {
            int viewLimit = viewStart + viewLength;
            int contentLimit = contentStart + contentLength;

            if (viewLimit + delta > contentLimit) {
                delta = Math.max(0, contentLimit-viewLimit);
            }
        }
        else {
            if (viewStart + delta < contentStart) {
                delta = Math.min(0, contentStart-viewStart);
            }
        }

        return delta;
    }

    private void scrollSelf(int dx, int dy) {

        boolean scrolled = scrollBy(dx, dy);

        if (scrolled && fScroller != null) {
            Rectangle documentBounds = fDocumentView.getDocumentBounds();
            fScroller.setPosition(-documentBounds.x,
                                  -documentBounds.y);
        }
    }

    private synchronized boolean scrollBy(int dx, int dy) {

        boolean scrolled = false;

        if (fScrolls) {
            Rectangle documentBounds = fDocumentView.getDocumentBounds();
            Rectangle viewBounds = getBounds();
            
            // variable not used int oldDx = dx;
            dx = pinScrollOffset(dx, 
                                 documentBounds.x,
                                 documentBounds.width,
                                 viewBounds.x,
                                 viewBounds.width);
            dy = pinScrollOffset(dy, 
                                 documentBounds.y,
                                 documentBounds.height,
                                 viewBounds.y,
                                 viewBounds.height);

            if (dx != 0 || dy != 0) {
                scrolled = true;
                fDocumentView.moveBy(-dx, -dy);
            }
        }

        return scrolled;
    }

    // implementation of Scroller.Client - called by Scroller
    // they have to be public since they're in an interface
    // no one else should call these methods
    public Rectangle getScrollSize() {

        if (fDocumentView != null) {
            return fDocumentView.getScrollableArea();
        }
        return new Rectangle(0, 0, 0, 0);
    }

    public void scrollTo(int x, int y) {

        if (fDocumentView != null) {
            scrollBy(x + fDocumentView.getDocX(), y + fDocumentView.getDocY());
        }
    }

    // *** Text access ***
    MConstText getText() {
        return fText;
    }

    MText getModifiableText() {
        return fText;
    }

    StyledTextClipboard getClipboard() {
        return fClipboard;
    }

    public synchronized void paint(Graphics g) {

        if (fDocumentView != null) {
            fDocumentView.paint(g);
        }
    }


    // *** Metric info - used by Behaviors
    Rectangle getCaretRect(TextOffset offset) {

        if (fDocumentView != null) {
            return fDocumentView.getCaretRect(offset);
        }
        return new Rectangle(0, 0);
    }

    TextOffset pointToTextOffset(TextOffset result,
                                 int x,
                                 int y,
                                 TextOffset anchor,
                                 boolean infiniteMode) {

        if (fDocumentView != null) {
            return fDocumentView.pointToTextOffset(result, x, y, anchor, infiniteMode);
        }
        return new TextOffset();
    }

    // *** Other stuff used by Behaviors - mostly formatter exports
    int lineContaining(TextOffset offset) {

        if (fDocumentView != null) {
            return fDocumentView.lineContaining(offset);
        }
        return 0;
    }

    int lineRangeLow(int lineNumber) {

        if (fDocumentView != null) {
            return fDocumentView.lineRangeLow(lineNumber);
        }
        return 0;
    }

    int lineRangeLimit(int lineNumber) {

        if (fDocumentView != null) {
            return fDocumentView.lineRangeLimit(lineNumber);
        }
        return 0;
    }

    void stopBackgroundFormatting() {

        if (fDocumentView != null) {
            fDocumentView.stopBackgroundFormatting();
        }
    }

    Rectangle getBoundingRect(TextOffset offset1, TextOffset offset2) {

        if (fDocumentView != null) {
            return fDocumentView.getBoundingRect(offset1, offset2);
        }
        return new Rectangle(0, 0, 0, 0);
    }

    synchronized void reformatAndDrawText(int reformatStart,
                             int reformatLength,
                             TextOffset selStart,
                             TextOffset selEnd,
                             Rectangle additionalUpdateRect,
                             Color hiliteColor) {

        if (fDocumentView != null) {
            fDocumentView.reformatAndDrawText(reformatStart,
                                              reformatLength,
                                              selStart,
                                              selEnd,
                                              additionalUpdateRect,
                                              hiliteColor);
        }
    }

    TextOffset findNewInsertionOffset(TextOffset result,
                                      TextOffset initialOffset,
                                      TextOffset previousOffset,
                                      short direction) {

        if (fDocumentView != null) {
            return fDocumentView.findNewInsertionOffset(result, initialOffset, previousOffset, direction);
        }
        return new TextOffset(initialOffset);
    }

    synchronized void drawText(Graphics g,
                  Rectangle damagedRect,
                  boolean selectionVisible,
                  TextOffset selStart,
                  TextOffset selEnd,
                  Color hiliteColor) {

        if (fDocumentView != null) {
            fDocumentView.drawText(g, damagedRect, selectionVisible, selStart, selEnd, hiliteColor);
        }
    }

    private void documentSizeChanged() {

        if (fScroller != null) {
            fScroller.clientScrollSizeChanged();
        }
    }

    int getFormatWidth() {

        if (fDocumentView != null) {
            return fDocumentView.getFormatWidth();
        }
        return 0;
    }

    /**
     * Return true if the paragraph at the given offset is left-to-right.
     * @param offset an offset in the text
     * @return true if the paragraph at the given offset is left-to-right
     */
    boolean paragraphIsLeftToRight(int offset) {
        
        if (fDocumentView != null) {
            return fDocumentView.paragraphIsLeftToRight(offset);
        }
        return true;
    }
    
    private static final class DocumentView {

        private TextComponent fHost;
        private boolean fWrapToWindowWidth;
        private int fInsetAmount;
        private PanelEventBroadcaster fListener;

        // fBounds is the total scrollable area of the document (including insets)
        private Rectangle fBounds = new Rectangle();
        
        private Point fOrigin;

        private MFormatter fFormatter;

        private OffscreenBufferCache fBufferCache;

        // Note, when this is true the caret won't blink in 1.1.  Looks like an AWT bug.
        private static boolean fNoOffscreenBuffer =
                            Boolean.getBoolean("TextComponent.NoOffscreenBuffer");

        // Amount by which to reduce the format width to allow for right-aligned carets.
        private final int CARET_SLOP = 1;

        DocumentView(TextComponent host,
                     MConstText text,
                     AttributeMap defaultValues,
                     boolean wraps,
                     int wrapWidth,
                     int insetAmount,
                     PanelEventBroadcaster listener) {

            fHost = host;
            fWrapToWindowWidth = wrapWidth == WINDOW_WIDTH;
            fInsetAmount = insetAmount;
            fListener = listener;

            initFormatterAndSize(text, defaultValues, wraps, wrapWidth);

            fBufferCache = new OffscreenBufferCache(host.fHost);
        }
        
        /**
         * Note: this computes the bounds rectangle relative to fOrigin
         */
        private void calcBoundsRect() {
        
            final int insetDim = 2 * fInsetAmount;

            final int minX = fFormatter.minX();
            final int minY = fFormatter.minY();

            fBounds.setBounds(fOrigin.x + minX - fInsetAmount,
                              fOrigin.y + minY - fInsetAmount,
                              fFormatter.maxX() - minX + insetDim, 
                              fFormatter.maxY() - minY + insetDim);
            //if (minX <= 0) {
            //    System.out.println("calcBoundsRect: minX="+minX+
            //                       "; bounds.x="+fBounds.x+"; width="+fBounds.width);
            //}
        }

        private void initFormatterAndSize(MConstText text, 
                                          AttributeMap defaultValues,
                                          boolean wraps,
                                          int wrapWidth) {

            Rectangle hostBounds = fHost.getBounds();
            int formatWidth;

            if (!wraps || fWrapToWindowWidth) {
                formatWidth = hostBounds.width - 2 * fInsetAmount;
                if (formatWidth <= CARET_SLOP) {
                    formatWidth = CARET_SLOP+1;
                }
            }
            else {
                formatWidth = wrapWidth;
            }

            fFormatter = MFormatter.createFormatter(text,
                                                    defaultValues,
                                                    formatWidth-CARET_SLOP,
                                                    wraps,
                                                    fHost.getGraphics());

            fFormatter.formatToHeight(hostBounds.height * 2);
            fOrigin = new Point(fInsetAmount, fInsetAmount);
            calcBoundsRect();
        }

        // notification method called by TextComponent
        void hostSizeChanged() {

            final boolean wrap = fFormatter.wrap();
            if (fWrapToWindowWidth || !wrap) {

                Rectangle hostBounds = fHost.getBounds();
                // variable not used final int insetDim = 2 * fInsetAmount;

                int formatWidth = hostBounds.width - 2*fInsetAmount;
                if (formatWidth <= CARET_SLOP) {
                    formatWidth = CARET_SLOP+1;
                }
                fFormatter.setLineBound(formatWidth-CARET_SLOP);

                fFormatter.formatToHeight(hostBounds.y + (hostBounds.height*2) - fOrigin.y);

                calcBoundsRect();
                
                //System.out.println("Window bounds="+hostBounds+"; document bounds="+fBounds);

                fHost.documentSizeChanged();
                fListener.textStateChanged(TextPanelEvent.FORMAT_WIDTH_CHANGED);
                //System.out.println("formatWidth="+formatWidth);
                //System.out.println("document bounds="+fBounds);
                //System.out.println();
            }
            //dumpWidthInfo();
        }

        int getFormatWidth() {

            return fFormatter.lineBound();
        }
        
        boolean paragraphIsLeftToRight(int offset) {
            
            int lineNumber = fFormatter.lineContaining(offset);
            return fFormatter.lineIsLeftToRight(lineNumber);
        }

        private void textSizeMightHaveChanged() {

            boolean changed = false;
            final int insetDim = 2 * fInsetAmount;
            
            int textHeight = fFormatter.maxY() - fFormatter.minY() + insetDim;
            if (textHeight != fBounds.height) {
                fBounds.height = textHeight;
                changed = true;
            }

            if (!fFormatter.wrap()) {
                int textWidth = fFormatter.maxX() - fFormatter.minX() + insetDim;
                if (textWidth != fBounds.width) {
                    fBounds.width = textWidth;
                    changed = true;
                }
            }

            if (changed) {
                //System.out.println("Text size changed.  fBounds: " + fBounds);
                calcBoundsRect();
                fHost.documentSizeChanged();
                fHost.scrollToShow(getDocumentBounds());
            }
        }

        private void doDrawText(Graphics g,
                                Rectangle drawRect,
                                boolean selectionVisible,
                                TextOffset selStart,
                                TextOffset selEnd,
                                Color hiliteColor) {

            Color oldColor = g.getColor();
            g.setColor(fHost.getHost().getBackground());
            g.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
            g.setColor(oldColor);

            //            g.clearRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);

            if (selectionVisible) {
                fFormatter.draw(g, drawRect, fOrigin, selStart, selEnd, hiliteColor);
            }
            else {
                fFormatter.draw(g, drawRect, fOrigin, null, null, null);
            }

            if (selStart != null && selStart.equals(selEnd) && selectionVisible) {

                fFormatter.drawCaret(g, selStart, fOrigin,
                                    STRONG_CARET_COLOR, WEAK_CARET_COLOR);
            }
        }

        void drawText(Graphics g,
                      Rectangle drawRect,
                      boolean selectionVisible,
                      TextOffset selStart,
                      TextOffset selEnd,
                      Color hiliteColor) {

            if (g != null) {
                drawRect = drawRect.intersection(fHost.getBounds());
                //System.out.println("drawText:drawRect: " + drawRect);
                g.clipRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
                if (fNoOffscreenBuffer) {
                    doDrawText(g, drawRect, selectionVisible, selStart, selEnd, hiliteColor);
                }
                else {
                    Image offscreenBuffer = fBufferCache.getBuffer(drawRect.width, drawRect.height);
                    Graphics offscreenGraphics = offscreenBuffer.getGraphics();
                    offscreenGraphics.translate(-drawRect.x, -drawRect.y);
    
                    doDrawText(offscreenGraphics, drawRect, selectionVisible, selStart, selEnd, hiliteColor);
    
                    g.drawImage(offscreenBuffer, drawRect.x, drawRect.y, fHost.fHost);
                }
            }
            textSizeMightHaveChanged();
        }

        void reformatAndDrawText(int reformatStart,
                                 int reformatLength,
                                 TextOffset selStart,
                                 TextOffset selEnd,
                                 Rectangle additionalUpdateRect,
                                 Color hiliteColor) {

            Rectangle visibleBounds = fHost.getBounds();
            Rectangle redrawRect = fFormatter.updateFormat(reformatStart,
                                                           reformatLength,
                                                           visibleBounds,
                                                           fOrigin);
            //System.out.println("[1] redrawRect: " + redrawRect);

            if (additionalUpdateRect != null) {
                redrawRect.add(additionalUpdateRect);
                //System.out.println("[2] redrawRect: " + redrawRect);
            }

            boolean haveSelection;

            if (selStart != null && selEnd != null) {
                haveSelection = true;
                redrawRect.add(fFormatter.getBoundingRect(selStart, selEnd, fOrigin, MFormatter.LOOSE));
                //System.out.println("[3] redrawRect: " + redrawRect);
            }
            else {
                haveSelection = false;
            }

            drawText(fHost.getGraphics(), redrawRect, haveSelection, selStart, selEnd, hiliteColor);
        }

        private void letBehaviorDraw(Graphics g, Rectangle drawRect) {

            boolean result = false;

            if (fHost.fBehavior != null) {
                result = fHost.fBehavior.paint(g, drawRect);
            }

            if (!result) {
                drawText(g, drawRect, false, null, null, null);
            }
        }

        void moveBy(int dx, int dy) {

            Rectangle visibleBounds = fHost.getBounds();
            Graphics g = fHost.getGraphics();

            fBounds.x += dx;
            fBounds.y += dy;
            fOrigin.x += dx;
            fOrigin.y += dy;

            Rectangle refreshRect = new Rectangle(visibleBounds);

            if (dx == 0) {
                if (g != null) {
                    g.copyArea(visibleBounds.x, visibleBounds.y, visibleBounds.width, visibleBounds.height, dx, dy);
                }
                if (dy < 0) {
                    refreshRect.y = visibleBounds.y + visibleBounds.height + dy;
                }
                refreshRect.height = Math.abs(dy);
                //System.out.println("refreshRect=" + refreshRect);
            }

            letBehaviorDraw(g, refreshRect);
        }
        
        private Rectangle getInsetBounds() {
            
            int insetDim = 2 * fInsetAmount;
            return new Rectangle(fBounds.x-fInsetAmount,
                                 fBounds.y-fInsetAmount,
                                 fBounds.width+insetDim,
                                 fBounds.height+insetDim);
        }

        void paint(Graphics g) {

            Rectangle hostBounds = fHost.getBounds();
            Rectangle textRefreshRect = hostBounds.intersection(getInsetBounds());
            letBehaviorDraw(g, textRefreshRect);
        }

        Rectangle getCaretRect(TextOffset offset) {

            return fFormatter.getCaretRect(offset, fOrigin);
        }

        TextOffset pointToTextOffset(TextOffset result,
                                     int x,
                                     int y,
                                     TextOffset anchor,
                                     boolean infiniteMode) {

            return fFormatter.pointToTextOffset(result, x, y, fOrigin, anchor, infiniteMode);
        }

        Rectangle getScrollableArea() {

            Rectangle area = new Rectangle(fBounds);
            area.x += fInsetAmount - fOrigin.x;
            area.y += fInsetAmount - fOrigin.y;
            return area;
        }

        /**
         * Doesn't clone so TextComponent needs to be nice.  TextComponent
         * is the only class which can access this anyway.
         */
        Rectangle getDocumentBounds() {

            return fBounds;
        }
        
        int getDocX() {
        
            return fOrigin.x - fInsetAmount;
        }
        
        int getDocY() {
        
            return fOrigin.y - fInsetAmount;
        }

        int lineContaining(TextOffset offset) {

            return fFormatter.lineContaining(offset);
        }

        int lineRangeLow(int lineNumber) {

            return fFormatter.lineRangeLow(lineNumber);
        }

        int lineRangeLimit(int lineNumber) {

            return fFormatter.lineRangeLimit(lineNumber);
        }

        void stopBackgroundFormatting() {

            fFormatter.stopBackgroundFormatting();
        }

        Rectangle getBoundingRect(TextOffset offset1, TextOffset offset2) {

            Rectangle r = fFormatter.getBoundingRect(offset1, offset2, fOrigin, MFormatter.TIGHT);
            //r.width += CARET_SLOP;
            //System.out.println("offset1="+offset1+"; offset2="+offset2);
            //System.out.println("bounds width="+r.width+"; host width="+(fHost.getBounds().width));
            return r;
        }

        TextOffset findNewInsertionOffset(TextOffset result,
                                          TextOffset initialOffset,
                                          TextOffset previousOffset,
                                          short direction) {

            return fFormatter.findNewInsertionOffset(
                        result, initialOffset, previousOffset, direction);
        }
    }
}
