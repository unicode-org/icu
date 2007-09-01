/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.impl;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.awt.datatransfer.*;

// LIU: Changed from final to non-final
public class DumbTextComponent extends Canvas
  implements KeyListener, MouseListener, MouseMotionListener, FocusListener
{
    
    /**
     * For serialization
     */
    private static final long serialVersionUID = 8265547730738652151L;

//    private transient static final String copyright =
//      "Copyright \u00A9 1998, Mark Davis. All Rights Reserved.";
    private transient static boolean DEBUG = false;

    private String contents = "";
    private Selection selection = new Selection();
    private int activeStart = -1;
    private boolean editable = true;

    private transient Selection tempSelection = new Selection();
    private transient boolean focus;
    private transient BreakIterator lineBreaker = BreakIterator.getLineInstance();
    private transient BreakIterator wordBreaker = BreakIterator.getWordInstance();
    private transient BreakIterator charBreaker = BreakIterator.getCharacterInstance();
    private transient int lineAscent;
    private transient int lineHeight;
    private transient int lineLeading;
    private transient int lastHeight = 10;
    private transient int lastWidth = 50;
    private static final int MAX_LINES = 200; // LIU: Use symbolic name
    private transient int[] lineStarts = new int[MAX_LINES]; // LIU
    private transient int lineCount = 1;

    private transient boolean valid = false;
    private transient FontMetrics fm;
    private transient boolean redoLines = true;
    private transient boolean doubleClick = false;
    private transient TextListener textListener;
    private transient ActionListener selectionListener;
    private transient Image cacheImage;
    private transient Dimension mySize;
    private transient int xInset = 5;
    private transient int yInset = 5;
    private transient Point startPoint = new Point();
    private transient Point endPoint = new Point();
    private transient Point caretPoint = new Point();
    private transient Point activePoint = new Point();
    
    //private transient static String clipBoard;

    private static final char CR = '\015'; // LIU

    // ============================================

    public DumbTextComponent() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        addFocusListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

    }

// ================ Events ====================

    // public boolean isFocusTraversable() { return true; }

    public void addActionListener(ActionListener l) {
        selectionListener = AWTEventMulticaster.add(selectionListener, l);
    }

    public void removeActionListener(ActionListener l) {
        selectionListener = AWTEventMulticaster.remove(selectionListener, l);
    }

    public void addTextListener(TextListener l) {
        textListener = AWTEventMulticaster.add(textListener, l);
    }

    public void removeTextListener(TextListener l) {
        textListener = AWTEventMulticaster.remove(textListener, l);
    }

    private transient boolean pressed;

    public void mousePressed(MouseEvent e) {
        if (DEBUG) System.out.println("mousePressed");
        if (pressed) {
            select(e,false);
        } else {
            doubleClick = e.getClickCount() > 1;
            requestFocus();
            select(e, true);
            pressed = true;
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (DEBUG) System.out.println("mouseDragged");
        select(e, false);
    }

    public void mouseReleased(MouseEvent e) {
        if (DEBUG) System.out.println("mouseReleased");
        pressed = false;
    }

    public void mouseEntered(MouseEvent e) {
        //if (pressed) select(e, false);
    }

    public void mouseExited(MouseEvent e){
        //if (pressed) select(e, false);
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}


    public void focusGained(FocusEvent e) {
        if (DEBUG) System.out.println("focusGained");
        focus = true;
        valid = false;
        repaint(16);
    }
    public void focusLost(FocusEvent e) {
        if (DEBUG) System.out.println("focusLost");
        focus = false;
        valid = false;
        repaint(16);
    }

    public void select(MouseEvent e, boolean first) {
        setKeyStart(-1);
        point2Offset(e.getPoint(), tempSelection);
        if (first) {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
                tempSelection.anchor = tempSelection.caret;
            }
        }
        // fix words
        if (doubleClick) {
            tempSelection.expand(wordBreaker);
        }
        select(tempSelection);
    }
    
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (DEBUG) System.out.println("keyPressed "
          + hex((char)code) + ", " + hex((char)e.getModifiers()));
        int start = selection.getStart();
        int end = selection.getEnd();
        boolean shift = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
        boolean ctrl = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
                
        switch (code) {
        case KeyEvent.VK_Q:
            if (!ctrl || !editable) break;
            setKeyStart(-1);
            fixHex();
            break;
        case KeyEvent.VK_V:
            if (!ctrl) break;
            if (!editable) {
                this.getToolkit().beep();
            } else {
                paste();
            }
            break;
        case KeyEvent.VK_C:
            if (!ctrl) break;
            copy();
            break;
        case KeyEvent.VK_X:
            if (!ctrl) break;
            if (!editable) {
                this.getToolkit().beep();
            } else {
                copy();
                insertText("");
            }
            break;
        case KeyEvent.VK_A:
            if (!ctrl) break;
            setKeyStart(-1);
            select(Integer.MAX_VALUE, 0, false);
            break;
        case KeyEvent.VK_RIGHT:
            setKeyStart(-1);
            tempSelection.set(selection);
            tempSelection.nextBound(ctrl ? wordBreaker : charBreaker, +1, shift);
            select(tempSelection);
            break;
        case KeyEvent.VK_LEFT:
            setKeyStart(-1);
            tempSelection.set(selection);
            tempSelection.nextBound(ctrl ? wordBreaker : charBreaker, -1, shift);
            select(tempSelection);
            break;
        case KeyEvent.VK_UP: // LIU: Add support for up arrow
            setKeyStart(-1);
            tempSelection.set(selection);
            tempSelection.caret = lineDelta(tempSelection.caret, -1);
            if (!shift) {
                tempSelection.anchor = tempSelection.caret;
            }
            select(tempSelection);
            break;
        case KeyEvent.VK_DOWN: // LIU: Add support for down arrow
            setKeyStart(-1);
            tempSelection.set(selection);
            tempSelection.caret = lineDelta(tempSelection.caret, +1);
            if (!shift) {
                tempSelection.anchor = tempSelection.caret;
            }
            select(tempSelection);
            break;
        case KeyEvent.VK_DELETE: // LIU: Add delete key support
            if (!editable) break;
            setKeyStart(-1);
            if (contents.length() == 0) break;
            start = selection.getStart();
            end = selection.getEnd();
            if (start == end) {
                ++end;
                if (end > contents.length()) {
                    getToolkit().beep();
                    return;
                }
            }
            replaceRange("", start, end);
            break;            
        }
    }

    void copy() {
        Clipboard cb = this.getToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(
            contents.substring(selection.getStart(), selection.getEnd()));
        cb.setContents(ss, ss);
    }
    
    void paste () {
        Clipboard cb = this.getToolkit().getSystemClipboard();
        Transferable t = cb.getContents(this);
        if (t == null) {
            this.getToolkit().beep();
            return;
        }
        try {
            String temp = (String) t.getTransferData(DataFlavor.stringFlavor);
            insertText(temp);
        } catch (Exception e) {
            this.getToolkit().beep();
        }            
    }

    /**
     * LIU: Given an offset into contents, moves up or down by lines,
     * according to lineStarts[].
     * @param off the offset into contents
     * @param delta how many lines to move up (< 0) or down (> 0)
     * @return the new offset into contents
     */
    private int lineDelta(int off, int delta) {
        int line = findLine(off, false);
        int posInLine = off - lineStarts[line];
        // System.out.println("off=" + off + " at " + line + ":" + posInLine);
        line += delta;
        if (line < 0) {
            line = posInLine = 0;
        } else if (line >= lineCount) {
            return contents.length();
        }
        off = lineStarts[line] + posInLine;
        if (off >= lineStarts[line+1]) {
            off = lineStarts[line+1] - 1;
        }
        return off;
    }
      
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (DEBUG) System.out.println("keyReleased "
          + hex((char)code) + ", " + hex((char)e.getModifiers()));
    }

    public void keyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        if (DEBUG) System.out.println("keyTyped "
          + hex((char)ch) + ", " + hex((char)e.getModifiers()));
        if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) return;
        int start, end;
        switch (ch) {
        case KeyEvent.CHAR_UNDEFINED:
            break;
        case KeyEvent.VK_BACK_SPACE:
            //setKeyStart(-1);
            if (!editable) break;
            if (contents.length() == 0) break;
            start = selection.getStart();
            end = selection.getEnd();
            if (start == end) {
                --start;
                if (start < 0) {
                    getToolkit().beep(); // LIU: Add audio feedback of NOP
                    return;
                }
            }
            replaceRange("", start, end);
            break;        
        case KeyEvent.VK_DELETE:
            //setKeyStart(-1);
            if (!editable) break;
            if (contents.length() == 0) break;
            start = selection.getStart();
            end = selection.getEnd();
            if (start == end) {
                ++end;
                if (end > contents.length()) {
                    getToolkit().beep(); // LIU: Add audio feedback of NOP
                    return;
                }
            }
            replaceRange("", start, end);
            break;
        default:
            if (!editable) break;
            // LIU: Dispatch to subclass API
            handleKeyTyped(e);
            break;
        }
    }

    // LIU: Subclass API for handling of key typing
    protected void handleKeyTyped(KeyEvent e) {
        insertText(String.valueOf(e.getKeyChar()));
    }
    
    protected void setKeyStart(int keyStart) {
        if (activeStart != keyStart) {
            activeStart = keyStart;
            repaint(10);
        }
    }
    
    protected void validateKeyStart() {
        if (activeStart > selection.getStart()) {
            activeStart = selection.getStart();
            repaint(10);
        }
    }
    
    protected int getKeyStart() {
        return activeStart;
    }

// ===================== Control ======================

    public synchronized void setEditable(boolean b) {
        editable = b;
    }

    public boolean isEditable() {
        return editable;
    }

    public void select(Selection newSelection) {
        newSelection.pin(contents);
        if (!selection.equals(newSelection)) {
            selection.set(newSelection);
            if (selectionListener != null) {
                selectionListener.actionPerformed(
                  new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    "Selection Changed", 0));
            }
            repaint(10);
            valid = false;
        }
    }

    public void select(int start, int end) {
        select(start, end, false);
    }

    public void select(int start, int end, boolean clickAfter) {
        tempSelection.set(start, end, clickAfter);
        select(tempSelection);
    }

    public int getSelectionStart() {
        return selection.getStart();
    }

    public int getSelectionEnd() {
        return selection.getEnd();
    }

    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x,y,w,h);
        redoLines = true;
    }

    public Dimension getPreferredSize() {
        return new Dimension(lastWidth,lastHeight);
    }

    public Dimension getMaximumSize() {
        return new Dimension(lastWidth,lastHeight);
    }

    public Dimension getMinimumSize() {
        return new Dimension(lastHeight,lastHeight);
    }

    public void setText(String text) {
        setText2(text);
        select(tempSelection.set(selection).pin(contents));
    }

    public void setText2(String text) {
        contents = text;
        charBreaker.setText(text);
        wordBreaker.setText(text);
        lineBreaker.setText(text);
        redoLines = true;
        if (textListener != null)
            textListener.textValueChanged(
              new TextEvent(this, TextEvent.TEXT_VALUE_CHANGED));
        repaint(16);
    }

    public void insertText(String text) {
        if (activeStart == -1) activeStart = selection.getStart();
        replaceRange(text, selection.getStart(), selection.getEnd());
    }

    public void replaceRange(String s, int start, int end) {
        setText2(contents.substring(0,start) + s
          + contents.substring(end));
        select(tempSelection.set(selection).
          fixAfterReplace(start, end, s.length()));
        validateKeyStart();
    }

    public String getText() {
        return contents;
    }

    public void setFont(Font font) {
        super.setFont(font);
        redoLines = true;
        repaint(16);
    }

    // ================== Graphics ======================

    public void update(Graphics g) {
        if (DEBUG) System.out.println("update");
        paint(g);
    }

    public void paint(Graphics g) {
        mySize = getSize();
        if (cacheImage == null
          || cacheImage.getHeight(this) != mySize.height
          || cacheImage.getWidth(this) != mySize.width) {
            cacheImage = createImage(mySize.width, mySize.height);
            valid = false;
        }
        if (!valid || redoLines) {
            if (DEBUG) System.out.println("painting");
            paint2(cacheImage.getGraphics());
            valid = true;
        }
        //getToolkit().sync();
        if (DEBUG) System.out.println("copying");
        g.drawImage(cacheImage,
          0, 0, mySize.width, mySize.height,
          0, 0, mySize.width, mySize.height,
          this);
    }

    public void paint2(Graphics g) {
        g.clearRect(0, 0, mySize.width, mySize.height);
        if (DEBUG) System.out.println("print");
        if (focus) g.setColor(Color.black);
        else g.setColor(Color.gray);
        g.drawRect(0,0,mySize.width-1,mySize.height-1);
        g.setClip(1,1,
          mySize.width-2,mySize.height-2);
        g.setColor(Color.black);
        g.setFont(getFont());
        fm = g.getFontMetrics();
        lineAscent = fm.getAscent();
        lineLeading = fm.getLeading();
        lineHeight = lineAscent + fm.getDescent() + lineLeading;
        int y = yInset + lineAscent;
        String lastSubstring = "";
        if (redoLines) fixLineStarts(mySize.width-xInset-xInset);
        for (int i = 0; i < lineCount; y += lineHeight, ++i) {
            // LIU: Don't display terminating ^M characters
            int lim = lineStarts[i+1];
            if (lim > 0 && contents.length() > 0 &&
                contents.charAt(lim-1) == CR) --lim;
            lastSubstring = contents.substring(lineStarts[i],lim);
            g.drawString(lastSubstring, xInset, y);
        }
        drawSelection(g, lastSubstring);
        lastHeight = y + yInset - lineHeight + yInset;
        lastWidth = mySize.width-xInset-xInset;
    }

    void paintRect(Graphics g, int x, int y, int w, int h) {
        if (focus) {
            g.fillRect(x, y, w, h);
        } else {
            g.drawRect(x, y, w-1, h-1);
        }
    }

    public void drawSelection(Graphics g, String lastSubstring) {
        g.setXORMode(Color.black);
        if (activeStart != -1) {
            offset2Point(activeStart, false, activePoint);
            g.setColor(Color.magenta);
            int line = activePoint.x - 1;
            g.fillRect(line, activePoint.y, 1, lineHeight);
        }
        if (selection.isCaret()) {
            offset2Point(selection.caret, selection.clickAfter, caretPoint);
        } else {
            if (focus) g.setColor(Color.blue);
            else g.setColor(Color.yellow);
            offset2Point(selection.getStart(), true, startPoint);
            offset2Point(selection.getEnd(), false, endPoint);
            if (selection.getStart() == selection.caret)
                caretPoint.setLocation(startPoint);
            else caretPoint.setLocation(endPoint);
            if (startPoint.y == endPoint.y) {
                paintRect(g, startPoint.x, startPoint.y,
                  Math.max(1,endPoint.x-startPoint.x), lineHeight);
            } else {
                paintRect(g, startPoint.x, startPoint.y,
                  (mySize.width-xInset)-startPoint.x, lineHeight);
                if (startPoint.y + lineHeight < endPoint.y)
                  paintRect(g, xInset, startPoint.y + lineHeight,
                  (mySize.width-xInset)-xInset, endPoint.y - startPoint.y - lineHeight);
                paintRect(g, xInset, endPoint.y, endPoint.x-xInset, lineHeight);
            }
        }
        if (focus || selection.isCaret()) {
            if (focus) g.setColor(Color.green);
            else g.setColor(Color.red);
            int line = caretPoint.x - (selection.clickAfter ? 0 : 1);
            g.fillRect(line, caretPoint.y, 1, lineHeight);
            int w = lineHeight/12 + 1;
            int braces = line - (selection.clickAfter ? -1 : w);
            g.fillRect(braces, caretPoint.y, w, 1);
            g.fillRect(braces, caretPoint.y + lineHeight - 1, w, 1);
        }
    }

    public Point offset2Point(int off, boolean start, Point p) {
        int line = findLine(off, start);
        int width = 0;
        try {
            width = fm.stringWidth(
              contents.substring(lineStarts[line], off));
        } catch (Exception e) {
            System.out.println(e);
        }
        p.x = width + xInset;
        if (p.x > mySize.width - xInset)
            p.x = mySize.width - xInset;
        p.y = lineHeight * line + yInset;
        return p;
    }

    private int findLine(int off, boolean start) {
        // if it is start, then go to the next line!
        if (start) ++off;
        for (int i = 1; i < lineCount; ++i) {
            // LIU: This was <= ; changed to < to make caret after
            // final CR in line appear at START of next line.
            if (off < lineStarts[i]) return i-1;
        }
        // LIU: Check for special case; after CR at end of the last line
        if (off == lineStarts[lineCount] &&
            off > 0 && contents.length() > 0 && contents.charAt(off-1) == CR) {
            return lineCount;
        }
        return lineCount-1;
    }

    // offsets on any line will go from start,true to end,false
    // excluding start,false and end,true
    public Selection point2Offset(Point p, Selection o) {
        if (p.y < yInset) {
            o.caret = 0;
            o.clickAfter = true;
            return o;
        }
        int line = (p.y - yInset)/lineHeight;
        if (line >= lineCount) {
            o.caret = contents.length();
            o.clickAfter = false;
            return o;
        }
        int target = p.x - xInset;
        if (target <= 0) {
            o.caret = lineStarts[line];
            o.clickAfter = true;
            return o;
        }
        int lowGuess = lineStarts[line];
        int lowWidth = 0;
        int highGuess = lineStarts[line+1];
        int highWidth = fm.stringWidth(contents.substring(lineStarts[line],highGuess));
        if (target >= highWidth) {
            o.caret = lineStarts[line+1];
            o.clickAfter = false;
            return o;
        }
        while (lowGuess < highGuess - 1) {
            int guess = (lowGuess + highGuess)/2;
            int width = fm.stringWidth(contents.substring(lineStarts[line],guess));
            if (width <= target) {
                lowGuess = guess;
                lowWidth = width;
                if (width == target) break;
            } else {
                highGuess = guess;
                highWidth = width;
            }
        }
        // at end, either lowWidth < target < width(low+1), or lowWidth = target
        int highBound = charBreaker.following(lowGuess);
        int lowBound = charBreaker.previous();
        // we are now at character boundaries
        if (lowBound != lowGuess)
            lowWidth = fm.stringWidth(contents.substring(lineStarts[line],lowBound));
        if (highBound != highGuess)
            highWidth = fm.stringWidth(contents.substring(lineStarts[line],highBound));
        // we now have the right widths
        if (target - lowWidth < highWidth - target) {
            o.caret = lowBound;
            o.clickAfter = true;
        } else {
            o.caret = highBound;
            o.clickAfter = false;
        }
        // we now have the closest!
        return o;
    }

    private void fixLineStarts(int width) {
        lineCount = 1;
        lineStarts[0] = 0;
        if (contents.length() == 0) {
            lineStarts[1] = 0;
            return;
        }
        int end = 0;
        // LIU: Add check for MAX_LINES
        for (int start = 0; start < contents.length() && lineCount < MAX_LINES;
             start = end) {
            end = nextLine(fm, start, width);
            lineStarts[lineCount++] = end;
            if (end == start) { // LIU: Assertion
                throw new RuntimeException("nextLine broken");
            }
        }
        --lineCount;
        redoLines = false;
    }

    // LIU: Enhanced to wrap long lines.  Bug with return of start fixed.
    public int nextLine(FontMetrics fm, int start, int width) {
        int len = contents.length();
        for (int i = start; i < len; ++i) {
            // check for line separator
            char ch = (contents.charAt(i));
            if (ch >= 0x000A && ch <= 0x000D || ch == 0x2028 || ch == 0x2029) {
                len = i + 1;
                if (ch == 0x000D && i+1 < len && contents.charAt(i+1) == 0x000A) // crlf
                    ++len; // grab extra char
                break;
            }
        }
        String subject = contents.substring(start,len);
        if (visibleWidth(fm, subject) <= width)
          return len;

        // LIU: Remainder of this method rewritten to accomodate lines
        // longer than the component width by first trying to break
        // into lines; then words; finally chars.
        int n = findFittingBreak(fm, subject, width, lineBreaker);
        if (n == 0) {
            n = findFittingBreak(fm, subject, width, wordBreaker);
        }
        if (n == 0) {
            n = findFittingBreak(fm, subject, width, charBreaker);
        }
        return n > 0 ? start + n : len;
    }

    /**
     * LIU: Finds the longest substring that fits a given width
     * composed of subunits returned by a BreakIterator.  If the smallest
     * subunit is too long, returns 0.
     * @param fm metrics to use
     * @param line the string to be fix into width
     * @param width line.substring(0, result) must be <= width
     * @param breaker the BreakIterator that will be used to find subunits
     * @return maximum characters, at boundaries returned by breaker,
     * that fit into width, or zero on failure
     */
    private int findFittingBreak(FontMetrics fm, String line, int width,
                                 BreakIterator breaker) {
        breaker.setText(line);
        int last = breaker.first();
        int end = breaker.next();
        while (end != BreakIterator.DONE &&
               visibleWidth(fm, line.substring(0, end)) <= width) {
            last = end;
            end = breaker.next();
        }
        return last;
    }

    public int visibleWidth(FontMetrics fm, String s) {
        int i;
        for (i = s.length()-1; i >= 0; --i) {
            char ch = s.charAt(i);
            if (!(ch == ' ' || ch >= 0x000A && ch <= 0x000D || ch == 0x2028 || ch == 0x2029))
            	return fm.stringWidth(s.substring(0,i+1));
        }
        return 0;
    }

// =============== Utility ====================

    private void fixHex() {
        if (selection.getEnd() == 0) return;
        int store = 0;
        int places = 1;
        int count = 0;
        int min = Math.min(8,selection.getEnd());
        for (int i = 0; i < min; ++i) {
            char ch = contents.charAt(selection.getEnd()-1-i);
            int value = Character.getNumericValue(ch);
            if (value < 0 || value > 15) break;
            store += places * value;
            ++count;
            places *= 16;
        }
        String add = "";
        int bottom = store & 0xFFFF;
        if (store >= 0xD8000000 && store < 0xDC000000
          && bottom >= 0xDC00 && bottom < 0xE000) { // surrogates
            add = "" + (char)(store >> 16) + (char)bottom;
        } else if (store > 0xFFFF && store <= 0x10FFFF) {
            store -= 0x10000;
            add = "" + (char)(((store >> 10) & 0x3FF) + 0xD800)
              + (char)((store & 0x3FF) + 0xDC00);
              
        } else if (count >= 4) {
            count = 4;
            add = ""+(char)(store & 0xFFFF);
        } else {
            count = 1;
            char ch = contents.charAt(selection.getEnd()-1);
            add = hex(ch);
            if (ch >= 0xDC00 && ch <= 0xDFFF && selection.getEnd() > 1) {
                ch = contents.charAt(selection.getEnd()-2);
                if (ch >= 0xD800 && ch <= 0xDBFF) {
                    count = 2;
                    add = hex(ch) + add;
                }
            }
        }
        replaceRange(add, selection.getEnd()-count, selection.getEnd());
    }

    public static String hex(char ch) {
        String result = Integer.toString(ch,16).toUpperCase();
        result = "0000".substring(result.length(),4) + result;
        return result;
    }
}
