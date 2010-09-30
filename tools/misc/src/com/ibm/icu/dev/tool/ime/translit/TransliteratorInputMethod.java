/*
 *******************************************************************************
 * Copyright (C) 2004-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.translit;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodContext;
import java.text.AttributedString;
import java.text.Collator;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.Transliterator;

public class TransliteratorInputMethod implements InputMethod {

    private static boolean usesAttachedIME() {
        // we're in the ext directory so permissions are not an issue
        String os = System.getProperty("os.name");
        if (os != null) {
            return os.indexOf("Windows") == -1;
        }
        return false;
    }

    // true if Solaris style; false if PC style, assume Apple uses PC style for now
    private static final boolean attachedStatusWindow = usesAttachedIME();

    // the shared status window
    private static Window statusWindow;

    // current or last owner
    private static TransliteratorInputMethod statusWindowOwner;

    // cache location limits for attached
    private static Rectangle attachedLimits;

    // convenience of access, to reflect the current state
    private static JComboBox choices;

    //
    // per-instance state
    //

    // if we're attached, the status window follows the client window
    private Point attachedLocation;

    private static int gid;

    private int id = gid++;

    InputMethodContext imc;
    private boolean enabled = true;

    private int selectedIndex = -1; // index in JComboBox corresponding to our transliterator
    private Transliterator transliterator;
    private int desiredContext;
    private StringBuffer buffer;
    private ReplaceableString replaceableText;
    private Transliterator.Position index;

    // debugging
    private static boolean TRACE_EVENT = false;
    private static boolean TRACE_MESSAGES = false;
    private static boolean TRACE_BUFFER = false;

    public TransliteratorInputMethod() {
        if (TRACE_MESSAGES)
            dumpStatus("<constructor>");

        buffer = new StringBuffer();
        replaceableText = new ReplaceableString(buffer);
        index = new Transliterator.Position();
    }

    public void dumpStatus(String msg) {
        System.out.println("(" + this + ") " + msg);
    }

    public void setInputMethodContext(InputMethodContext context) {
        initStatusWindow(context);

        imc = context;
        imc.enableClientWindowNotification(this, attachedStatusWindow);
    }

    private static void initStatusWindow(InputMethodContext context) {
        if (statusWindow == null) {
            String title;
            try {
                ResourceBundle rb = ResourceBundle
                        .getBundle("com.ibm.icu.dev.tool.ime.translit.Transliterator");
                title = rb.getString("title");
            } catch (MissingResourceException m) {
                System.out.println("Transliterator resources missing: " + m);
                title = "Transliterator Input Method";
            }

            Window sw = context.createInputMethodWindow(title, false);

            // get all the ICU Transliterators
            Enumeration en = Transliterator.getAvailableIDs();
            TreeSet types = new TreeSet(new LabelComparator());

            while (en.hasMoreElements()) {
                String id = (String) en.nextElement();
                String name = Transliterator.getDisplayName(id);
                JLabel label = new JLabel(name);
                label.setName(id);
                types.add(label);
            }

            // add the transliterators to the combo box

            choices = new JComboBox(types.toArray());

            choices.setEditable(false);
            choices.setSelectedIndex(0);
            choices.setRenderer(new NameRenderer());
            choices.setActionCommand("transliterator");

            choices.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (statusWindowOwner != null) {
                        statusWindowOwner.statusWindowAction(e);
                    }
                }
            });

            sw.add(choices);
            sw.pack();

            Dimension sd = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension wd = sw.getSize();
            if (attachedStatusWindow) {
                attachedLimits = new Rectangle(0, 0, sd.width - wd.width,
                        sd.height - wd.height);
            } else {
                sw.setLocation(sd.width - wd.width, sd.height - wd.height - 25);
            }

            synchronized (TransliteratorInputMethod.class) {
                if (statusWindow == null) {
                    statusWindow = sw;
                }
            }
        }
    }

    private void statusWindowAction(ActionEvent e) {
        if (TRACE_MESSAGES)
            dumpStatus(">>status window action");
        JComboBox cb = (JComboBox) e.getSource();
        int si = cb.getSelectedIndex();
        if (si != selectedIndex) { // otherwise, we don't need to change
            if (TRACE_MESSAGES)
                dumpStatus("status window action oldIndex: " + selectedIndex
                        + " newIndex: " + si);

            selectedIndex = si;

            JLabel item = (JLabel) cb.getSelectedItem();

            // construct the actual transliterator
            // commit any text that may be present first
            commitAll();

            transliterator = Transliterator.getInstance(item.getName());
            desiredContext = transliterator.getMaximumContextLength();

            reset();
        }
        if (TRACE_MESSAGES)
            dumpStatus("<<status window action");
    }

    // java has no pin to rectangle function?
    private static void pin(Point p, Rectangle r) {
        if (p.x < r.x) {
            p.x = r.x;
        } else if (p.x > r.x + r.width) {
            p.x = r.x + r.width;
        }
        if (p.y < r.y) {
            p.y = r.y;
        } else if (p.y > r.y + r.height) {
            p.y = r.y + r.height;
        }
    }

    public void notifyClientWindowChange(Rectangle location) {
        if (TRACE_MESSAGES)
            dumpStatus(">>notify client window change: " + location);
        synchronized (TransliteratorInputMethod.class) {
            if (statusWindowOwner == this) {
                if (location == null) {
                    statusWindow.setVisible(false);
                } else {
                    attachedLocation = new Point(location.x, location.y
                            + location.height);
                    pin(attachedLocation, attachedLimits);
                    statusWindow.setLocation(attachedLocation);
                    statusWindow.setVisible(true);
                }
            }
        }
        if (TRACE_MESSAGES)
            dumpStatus("<<notify client window change: " + location);
    }

    public void activate() {
        if (TRACE_MESSAGES)
            dumpStatus(">>activate");

        synchronized (TransliteratorInputMethod.class) {
            if (statusWindowOwner != this) {
                if (TRACE_MESSAGES)
                    dumpStatus("setStatusWindowOwner from: " + statusWindowOwner + " to: " + this);

                statusWindowOwner = this;
                // will be null before first change notification
                if (attachedStatusWindow && attachedLocation != null) {
                    statusWindow.setLocation(attachedLocation);
                }
                choices.setSelectedIndex(selectedIndex == -1 ? choices
                        .getSelectedIndex() : selectedIndex);
            }

            choices.setForeground(Color.BLACK);
            statusWindow.setVisible(true);
        }
        if (TRACE_MESSAGES)
            dumpStatus("<<activate");
    }
    
    public void deactivate(boolean isTemporary) {
        if (TRACE_MESSAGES)
            dumpStatus(">>deactivate" + (isTemporary ? " (temporary)" : ""));
        if (!isTemporary) {
            synchronized (TransliteratorInputMethod.class) {
                choices.setForeground(Color.LIGHT_GRAY);
            }
        }
        if (TRACE_MESSAGES)
            dumpStatus("<<deactivate" + (isTemporary ? " (temporary)" : ""));
    }
    
    public void hideWindows() {
        if (TRACE_MESSAGES)
            dumpStatus(">>hideWindows");
        synchronized (TransliteratorInputMethod.class) {
            if (statusWindowOwner == this) {
                if (TRACE_MESSAGES)
                    dumpStatus("hiding");
                statusWindow.setVisible(false);
            }
        }
        if (TRACE_MESSAGES)
            dumpStatus("<<hideWindows");
    }
    
    public boolean setLocale(Locale locale) {
        return false;
    }
    
    public Locale getLocale() {
        return Locale.getDefault();
    }
    
    public void setCharacterSubsets(Character.Subset[] subsets) {
    }

    public void reconvert() {
        throw new UnsupportedOperationException();
    }

    public void removeNotify() {
        if (TRACE_MESSAGES)
            dumpStatus("**removeNotify");
    }

    public void endComposition() {
        commitAll();
    }

    public void dispose() {
        if (TRACE_MESSAGES)
            dumpStatus("**dispose");
    }
    
    public Object getControlObject() {
        return null;
    }
    
    public void setCompositionEnabled(boolean enable) {
        enabled = enable;
    }

    public boolean isCompositionEnabled() {
        return enabled;
    }

    // debugging
    private String eventInfo(AWTEvent event) {
        String info = event.toString();
        StringBuffer buf = new StringBuffer();
        int index1 = info.indexOf("[");
        int index2 = info.indexOf(",", index1);
        buf.append(info.substring(index1 + 1, index2));

        index1 = info.indexOf("] on ");
        index2 = info.indexOf("[", index1);
        if (index2 != -1) {
            int index3 = info.lastIndexOf(".", index2);
            if (index3 < index1 + 4) {
                index3 = index1 + 4;
            }
            buf.append(" on ");
            buf.append(info.substring(index3 + 1, index2));
        }
        return buf.toString();
    }

    public void dispatchEvent(AWTEvent event) {
        final int MODIFIERS = 
            InputEvent.CTRL_MASK | 
            InputEvent.META_MASK | 
            InputEvent.ALT_MASK | 
            InputEvent.ALT_GRAPH_MASK;
    
        switch (event.getID()) {
        case MouseEvent.MOUSE_PRESSED:
            if (enabled) {
                if (TRACE_EVENT) System.out.println("TIM: " + eventInfo(event));
                // we'll get this even if the user is scrolling, can we rely on the component?
                // commitAll(); // don't allow even clicks within our own edit area
            }
            break;
    
        case KeyEvent.KEY_TYPED: {
            if (enabled) {
                KeyEvent ke = (KeyEvent)event;
                if (TRACE_EVENT) System.out.println("TIM: " + eventInfo(ke));
                if ((ke.getModifiers() & MODIFIERS) != 0) {
                    commitAll(); // assume a command, let it go through
                } else {
                    if (handleTyped(ke.getKeyChar())) {
                        ke.consume();
                    }
                }
            }
        } break;
    
        case KeyEvent.KEY_PRESSED: {
            if (enabled) {
            KeyEvent ke = (KeyEvent)event;
            if (TRACE_EVENT) System.out.println("TIM: " + eventInfo(ke));
                if (handlePressed(ke.getKeyCode())) {
                    ke.consume();
                }
            }
        } break;
    
        case KeyEvent.KEY_RELEASED: {
            // this won't autorepeat, which is better for toggle actions
            KeyEvent ke = (KeyEvent)event;
            if (ke.getKeyCode() == KeyEvent.VK_SPACE && ke.isControlDown()) {
                setCompositionEnabled(!enabled);
            }
        } break;
    
        default:
            break;
        }
    }

    /** Wipe clean */
    private void reset() {
        buffer.delete(0, buffer.length());
        index.contextStart = index.contextLimit = index.start = index.limit = 0;
    }

    // committed}context-composed|composed
    //          ^       ^        ^
    //         cc     start    ctxLim

    private void traceBuffer(String msg, int cc, int off) {
        if (TRACE_BUFFER)
            System.out.println(Utility.escape(msg + ": '"
                    + buffer.substring(0, cc) + '}'
                    + buffer.substring(cc, index.start) + '-'
                    + buffer.substring(index.start, index.contextLimit) + '|'
                    + buffer.substring(index.contextLimit) + '\''));
    }

    private void update(boolean flush) {
        int len = buffer.length();
        String text = buffer.toString();
        AttributedString as = new AttributedString(text);

        int cc, off;
        if (flush) {
            off = index.contextLimit - len; // will be negative
            cc = index.start = index.limit = index.contextLimit = len;
        } else {
            cc = index.start > desiredContext ? index.start - desiredContext
                    : 0;
            off = index.contextLimit - cc;
        }

        if (index.start < len) {
            as.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT,
                    InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT,
                    index.start, len);
        }

        imc.dispatchInputMethodEvent(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, as.getIterator(),
                cc, TextHitInfo.leading(off), null);

        traceBuffer("update", cc, off);

        if (cc > 0) {
            buffer.delete(0, cc);
            index.start -= cc;
            index.limit -= cc;
            index.contextLimit -= cc;
        }
    }

    private void updateCaret() {
        imc.dispatchInputMethodEvent(InputMethodEvent.CARET_POSITION_CHANGED,
                null, 0, TextHitInfo.leading(index.contextLimit), null);
        traceBuffer("updateCaret", 0, index.contextLimit);
    }
    
    private void caretToStart() {
        if (index.contextLimit > index.start) {
            index.contextLimit = index.limit = index.start;
            updateCaret();
        }
    }

    private void caretToLimit() {
        if (index.contextLimit < buffer.length()) {
            index.contextLimit = index.limit = buffer.length();
            updateCaret();
        }
    }

    private boolean caretTowardsStart() {
        int bufpos = index.contextLimit;
        if (bufpos > index.start) {
            --bufpos;
            if (bufpos > index.start
                    && UCharacter.isLowSurrogate(buffer.charAt(bufpos))
                    && UCharacter.isHighSurrogate(buffer.charAt(bufpos - 1))) {
                --bufpos;
            }
            index.contextLimit = index.limit = bufpos;
            updateCaret();
            return true;
        }
        return commitAll();
    }

    private boolean caretTowardsLimit() {
        int bufpos = index.contextLimit;
        if (bufpos < buffer.length()) {
            ++bufpos;
            if (bufpos < buffer.length()
                    && UCharacter.isLowSurrogate(buffer.charAt(bufpos))
                    && UCharacter.isHighSurrogate(buffer.charAt(bufpos - 1))) {
                ++bufpos;
            }
            index.contextLimit = index.limit = bufpos;
            updateCaret();
            return true;
        }
        return commitAll();
    }

    private boolean canBackspace() {
        return index.contextLimit > 0;
    }

    private boolean backspace() {
        int bufpos = index.contextLimit;
        if (bufpos > 0) {
            int limit = bufpos;
            --bufpos;
            if (bufpos > 0 && UCharacter.isLowSurrogate(buffer.charAt(bufpos))
                    && UCharacter.isHighSurrogate(buffer.charAt(bufpos - 1))) {
                --bufpos;
            }
            if (bufpos < index.start) {
                index.start = bufpos;
            }
            index.contextLimit = index.limit = bufpos;
            doDelete(bufpos, limit);
            return true;
        }
        return false;
    }

    private boolean canDelete() {
        return index.contextLimit < buffer.length();
    }

    private boolean delete() {
        int bufpos = index.contextLimit;
        if (bufpos < buffer.length()) {
            int limit = bufpos + 1;
            if (limit < buffer.length()
                    && UCharacter.isHighSurrogate(buffer.charAt(limit - 1))
                    && UCharacter.isLowSurrogate(buffer.charAt(limit))) {
                ++limit;
            }
            doDelete(bufpos, limit);
            return true;
        }
        return false;
    }

    private void doDelete(int start, int limit) {
        buffer.delete(start, limit);
        update(false);
    }

    private boolean commitAll() {
        if (buffer.length() > 0) {
            boolean atStart = index.start == index.contextLimit;
            boolean didConvert = buffer.length() > index.start;
            index.contextLimit = index.limit = buffer.length();
            transliterator.finishTransliteration(replaceableText, index);
            if (atStart) {
                index.start = index.limit = index.contextLimit = 0;
            }
            update(true);
            return didConvert;
        }
        return false;
    }

    private void clearAll() {
        int len = buffer.length();
        if (len > 0) {
            if (len > index.start) {
                buffer.delete(index.start, len);
            }
            update(true);
        }
    }

    private boolean insert(char c) {
        transliterator.transliterate(replaceableText, index, c);
        update(false);
        return true;
    }

    private boolean editing() {
        return buffer.length() > 0;
    }

    /**
     * The big problem is that from release to release swing changes how it
     * handles some characters like tab and backspace.  Sometimes it handles
     * them as keyTyped events, and sometimes it handles them as keyPressed
     * events.  If you want to allow the event to go through so swing handles
     * it, you have to allow one or the other to go through.  If you don't want
     * the event to go through so you can handle it, you have to stop the
     * event both places.
     * @return whether the character was handled
     */
    private boolean handleTyped(char ch) {
        if (enabled) {
            switch (ch) {
            case '\b': if (editing()) return backspace(); break;
            case '\t': if (editing()) { return commitAll(); } break;
            case '\u001b': if (editing()) { clearAll(); return true; } break;
            case '\u007f': if (editing()) return delete(); break;
            default: return insert(ch);
            }
        }
        return false;
    }

    /**
     * Handle keyPressed events.
     */
    private boolean handlePressed(int code) {
        if (enabled && editing()) {
            switch (code) {
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_HOME:
            caretToStart(); return true;
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_END:
            caretToLimit(); return true;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
            return caretTowardsStart();
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
            return caretTowardsLimit();
            case KeyEvent.VK_BACK_SPACE: 
            return canBackspace(); // unfortunately, in 1.5 swing handles this in keyPressed instead of keyTyped
            case KeyEvent.VK_DELETE: 
            return canDelete(); // this too?
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_ENTER:
            return commitAll(); // so we'll never handle VK_TAB in keyTyped
            
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_ALT:
            return false; // ignore these unless a key typed event gets generated
            default: 
            // by default, let editor handle it, and we'll assume that it will tell us
            // to endComposition if it does anything funky with, e.g., function keys.
            return false;
            }
        }
        return false;
    }

    public String toString() {
        final String[] names = { 
            "alice", "bill", "carrie", "doug", "elena", "frank", "gertie", "howie", "ingrid", "john" 
        };
    
        if (id < names.length) {
            return names[id];
        } else {
            return names[id] + "-" + (id/names.length);
        }
    }
}

class NameRenderer extends JLabel implements ListCellRenderer {

    /**
     * For serialization
     */
    private static final long serialVersionUID = -210152863798631747L;

    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

        String s = ((JLabel)value).getText();
        setText(s);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);
        return this;
    }
}

class LabelComparator implements Comparator {
    public int compare(Object obj1, Object obj2) {
        Collator collator = Collator.getInstance();
        return collator.compare(((JLabel)obj1).getText(), ((JLabel)obj2).getText());
    }

    public boolean equals(Object obj1) {
        return this.equals(obj1);
    }
}
