/*
 * ===========================================================================
 * IBM ICU Transliterator IME - TransliteratorInputMethod.java version 1.0
 * (C) Copyright IBM Corp. 2004. All Rights Reserved
 * ===========================================================================
 */

package com.ibm.icu.dev.tool.ime;


import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodContext;
import java.text.AttributedString;

import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.ReplaceableString;
import com.ibm.icu.text.Collator;


public class TransliteratorInputMethod implements InputMethod {

    // windows - shared by all instances of this input method
    private static Window statusWindow;

    // current or last statusWindow owner instance
    private static TransliteratorInputMethod statusWindowOwner;

    // true if Solaris style; false if PC style
    private static boolean attachedStatusWindow = false;

    // remember the statusWindow location per instance
    private Rectangle clientWindowLocation;

    // status window location in PC style
    private static Point globalStatusWindowLocation;

    // keep live input method instances (synchronized using statusWindow)
    private static HashSet inputMethodInstances = new HashSet(5);

    
    // per-instance state
    InputMethodContext inputMethodContext;
    private boolean active;
    private boolean disposed;
    private StringBuffer rawText;
    private Transliterator transliterator = null;
    private Transliterator.Position index = null;
    private ReplaceableString replaceableText = null;
    private ResourceBundle rb = null;

    public TransliteratorInputMethod() {
        rawText = new StringBuffer();
        replaceableText = new ReplaceableString(rawText);
        try {
            rb = ResourceBundle.getBundle("com.ibm.icu.dev.tool.ime.Transliterator");
        }
        catch(MissingResourceException m) {
            System.out.println("Transliterator resources missing: " + m);
        }
    }
    
    public void setInputMethodContext(InputMethodContext context) {
        inputMethodContext = context;
        String title = null;

	if (statusWindow == null) {
            try {
                title = rb.getString("title");
            }
            catch (MissingResourceException m) {
                title = "Transliterator Input Method";
            }

            Window sw = context.createInputMethodWindow(title, false);

	    // This is to fix a problem in Java 1.4
	    try {
		sw.setFocusableWindowState(true);
	    }
	    catch(NoSuchMethodError err) {
		/* we do not need to worry about catching this, the setFocusableWindowState
		   only exists in Java 1.4. In Java 1.3 it is not necessary to make the
		   status window focusable, because the default setting is focusable. */
	    }

	    // get all the ICU Transliterators
	    Enumeration e = Transliterator.getAvailableIDs();
	    TreeSet types = new TreeSet(new LabelComparator());

	    while(e.hasMoreElements()) {
		String id = (String) e.nextElement();
		String name = Transliterator.getDisplayName(id);
		JLabel label = new JLabel(name);
		label.setName(id);
		types.add(label);
	    }
	    
	    // add the transliterators to the combo box
	    System.out.println("types: " + types);

	    JComboBox choices = new JComboBox(types.toArray());
       
	    choices.setEditable(false);
	    choices.setRenderer(new NameRenderer());
	    choices.setActionCommand("transliterator");

	    synchronized (this.getClass()) {
		if (statusWindow == null) {
		    statusWindow = sw;
		    statusWindow.addComponentListener(new ComponentListener() {
			    public void componentResized(ComponentEvent e) {}
			    public void componentMoved(ComponentEvent e) {
				synchronized (statusWindow) {
				    if (!attachedStatusWindow) {
					Component comp = e.getComponent();
					if (comp.isVisible()) {
					    globalStatusWindowLocation = comp.getLocation();
					}
				    }
				}
			    }
			    public void componentShown(ComponentEvent e) {}
			    public void componentHidden(ComponentEvent e) {}
			});

		    // setup the listener, to handle selection of a transliterator
		    choices.addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				JLabel item = (JLabel)cb.getSelectedItem();

				// construct the actual transliterator
				// commit any text that may be present first
				commitAll();
				transliterator = Transliterator.getInstance(item.getName());
				index = new Transliterator.Position();
				rawText.setLength(0);
				index.contextLimit = replaceableText.length();
				index.contextStart = 0;
				index.start = 0;
				index.limit = replaceableText.length();
			    }
			});
		    statusWindow.add(choices);
		    statusWindowOwner = this;
		    updateStatusWindow();
		    statusWindow.pack();
		    choices.setSelectedIndex(0);
		}
	    }
	}
	inputMethodContext.enableClientWindowNotification(this, attachedStatusWindow);
	synchronized (statusWindow) {
	    inputMethodInstances.add(this);
	}
    }
    
    public boolean setLocale(Locale locale) {
        return false;
    }
    
    public Locale getLocale() {
        return Locale.getDefault();
    }
    
    void updateStatusWindow() {
	synchronized (statusWindow) {
	    statusWindow.pack();
	    if (attachedStatusWindow) {
		if (clientWindowLocation != null) {
		    statusWindow.setLocation(clientWindowLocation.x,
					     clientWindowLocation.y + clientWindowLocation.height);
		}
	    } else {
		setPCStyleStatusWindow();
	    }
	}
    }

    private void setPCStyleStatusWindow() {
	synchronized (statusWindow) {
	    if (globalStatusWindowLocation == null) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		globalStatusWindowLocation = new Point(d.width - statusWindow.getWidth(),
						       d.height - statusWindow.getHeight() - 25);
	    }
	    statusWindow.setLocation(globalStatusWindowLocation.x, globalStatusWindowLocation.y);
	}
    }

    private void setStatusWindowForeground(Color fg) {
	synchronized (statusWindow) {
	    if (statusWindowOwner != this) {
		return;
	    }
	}
    }

    private void toggleStatusWindowStyle() {
	synchronized (statusWindow) {
	    if (attachedStatusWindow) {
		attachedStatusWindow = false;
		setPCStyleStatusWindow();
	    } else {
		attachedStatusWindow = true;
	    }
	    Iterator itr = inputMethodInstances.iterator();
	    while (itr.hasNext()) {
		TransliteratorInputMethod im = (TransliteratorInputMethod)itr.next();
		im.inputMethodContext.enableClientWindowNotification(im, attachedStatusWindow);
	    }
	}
    }	    

    public void setCharacterSubsets(Character.Subset[] subsets) {
        // ignore
    }

    public void reconvert() {
	// not supported yet
	throw new UnsupportedOperationException();
    }

    public void dispatchEvent(AWTEvent event) {
        if (!active && (event instanceof KeyEvent)) {
            System.out.println(rb.getString("activeError"));
        }
        if (disposed) {
            System.out.println(rb.getString("disposedError"));
        }
        if (!(event instanceof InputEvent)) {
            System.out.println(rb.getString("inputError"));
        }
		
        if (event.getID() == KeyEvent.KEY_TYPED) {
            KeyEvent e = (KeyEvent) event;
            if (handleCharacter(e.getKeyChar())) {
                e.consume();
            }
        }
    }

    public void activate() {
        if (active) {
            System.out.println(rb.getString("activateError"));
        }
	System.out.println("activate");

        active = true;
	synchronized (statusWindow) {
	    statusWindowOwner = this; 
	    updateStatusWindow();
	    if (!statusWindow.isVisible()) {
		statusWindow.setVisible(true);
	    }
	    setStatusWindowForeground(Color.black);
	}
    }
    
    public void deactivate(boolean isTemporary) {
        if (!active) {
            System.out.println(rb.getString("deactivateError"));
        }
	System.out.println("deactivate");
	setStatusWindowForeground(Color.lightGray);
        active = false;
    }
    
    public void hideWindows() {
        if (active) {
            System.out.println(rb.getString("hideError"));
        }
	synchronized (statusWindow) {
	    if (statusWindowOwner == this) {
		statusWindow.setVisible(false);
	    }
	}
    }
    
    public void removeNotify() {
    }
    
    public void endComposition() {
        commitAll();
    }

    public void notifyClientWindowChange(Rectangle location) {
	clientWindowLocation = location;
	synchronized (statusWindow) {
	    if (!attachedStatusWindow || statusWindowOwner != this) {
		return;
	    }
	    if (location != null) {
		statusWindow.setLocation(location.x, location.y+location.height);
		if (!statusWindow.isVisible()) {
		    if (active) {
			setStatusWindowForeground(Color.black);
		    } else {
			setStatusWindowForeground(Color.lightGray);
		    }
		    statusWindow.setVisible(true);
		}
	    } else {
		statusWindow.setVisible(false);
	    }
	}
    }

    public void dispose() {
        if (active) {
            System.out.println(rb.getString("disposeError"));
        }
        if (disposed) {
            System.out.println(rb.getString("multipleDisposeError"));
        }
	synchronized (statusWindow) {
	    inputMethodInstances.remove(this);
	}
        disposed = true;
    }
    
    public Object getControlObject() {
        return null;
    }
    
    public void setCompositionEnabled(boolean enable) {
	// not supported yet
	throw new UnsupportedOperationException();
    }

    public boolean isCompositionEnabled() {
        // always enabled
	return true;
    }

    /**
     * Attempts to handle a typed character.
     * @return whether the character was handled
     */
    private boolean handleCharacter(char ch) {
	if (ch == '\n') {
	    commitAll();
	    return true;
	}
	else if (ch == '\b') {
	    if (index.contextLimit > index.contextStart) {
		rawText.deleteCharAt(index.contextLimit - 1);
		--index.limit;
		--index.contextLimit;
		sendText();
		return true;
	    }
	}
	else {
	    rawText.append(ch);
	    index.limit = rawText.length();
	    index.contextLimit = rawText.length();
	    transliterator.transliterate(replaceableText, index);
	    sendText();
	    index.contextStart = index.start;
	    return true;
	}
         
	return false;
    }
    
    /* commits all chunks */
    void commitAll() {
        if(transliterator != null) {
            transliterator.finishTransliteration(replaceableText, index);
            sendText();
            rawText.setLength(0);
            index.start = 0;
            index.contextStart = 0;
            index.limit = 0;
            index.contextLimit = 0;
        }
    }
    
    void sendText() {
	AttributedString as = null;
	TextHitInfo caret = null;
	int committedCharacterCount = 0;
	InputMethodHighlight highlight = InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT;
	String text = null;      

	if(index.start > index.contextStart) {
	    text = rawText.substring(index.contextStart, index.start);
	    as = new AttributedString(text);
	    committedCharacterCount = text.length();
	    inputMethodContext.dispatchInputMethodEvent(
						        InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
						        as.getIterator(),
						        committedCharacterCount,
						        null,
						        null);
	}

	if(index.contextLimit > index.start) {
	    text = rawText.substring(index.start, index.contextLimit);
	    as = new AttributedString(text);
	    committedCharacterCount = 0;
	    as.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT, highlight);
	    caret = TextHitInfo.leading(text.length());
	    inputMethodContext.dispatchInputMethodEvent(
						        InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
						        as.getIterator(),
						        committedCharacterCount,
						        caret,
						        null);
	}
    }
}

class NameRenderer extends JLabel implements ListCellRenderer {

    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

        String s = ((JLabel)value).getText();
        setText(s);

        if(isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {
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


