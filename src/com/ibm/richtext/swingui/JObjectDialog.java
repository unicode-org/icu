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
package com.ibm.richtext.swingui;

import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import com.ibm.richtext.textlayout.attributes.AttributeSet;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.uiimpl.MenuItemSet;

import com.ibm.richtext.uiimpl.resources.FrameResources;
import com.ibm.richtext.uiimpl.ResourceUtils;

/**
* Simple dialog that sets an attribute.
*/
final class JObjectDialog extends JDialog {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private final MTextPanel fTextPanel;
    private final Object fKey;
    private boolean fCharacter;
    private Hashtable fNameToValueMap;
    
    private final JButton fOKButton;
    private final JButton fCancelButton;
    private final JList fItems;
    
    /**
    * Construct a new JObjectDialog.
    * @param parent the dialog's parent frame
    * @param title the dialogs title
    * @param message the message displayed next to the input box
    */
    JObjectDialog(Frame parent,
                  String title, 
                  String message, 
                  MTextPanel textPanel,
                  Object key,
                  boolean character,
                  String[] names,
                  Object[] values) {
                    
        super(parent, title, false);

        setupMap(names, values);
        
        Dimension size = new Dimension(250, 200);
        
        fTextPanel = textPanel;
        fKey = key;
        fCharacter = character;
        
        fItems = new JList(names);
        fItems.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        fItems.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    closeWindow(true);
                }
            }
        });
        
        JScrollPane listScroller = new JScrollPane(fItems);
        listScroller.setPreferredSize(size);
        listScroller.setPreferredSize(size);
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(message);
        label.setLabelFor(fItems);
        
        JPanel itemPanel = new JPanel();
        itemPanel.add(label);
        itemPanel.add(Box.createRigidArea(new Dimension(0,5)));
        itemPanel.add(listScroller);
        itemPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        
        fCancelButton = new JButton(ResourceUtils.getResourceString(FrameResources.CANCEL));
        fOKButton = new JButton(ResourceUtils.getResourceString(FrameResources.OK));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.add(fCancelButton);
        buttonPanel.add(fOKButton);
        
        Container content = getContentPane();
        content.add(itemPanel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        pack();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeWindow(false);
            }
        });
        
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            
                Object source = e.getSource();
                if (source == fOKButton) {
                    closeWindow(true);
                }
                else if (source == fCancelButton) {
                    closeWindow(false);
                }
            }
        };
        
        fOKButton.addActionListener(listener);
        fCancelButton.addActionListener(listener);
        
        selectStyles(values);
    }
    
    private void setupMap(String[] names, Object[] values) {
    
        if (names.length != values.length) {
            throw new IllegalArgumentException("Must have same number of names and values.");
        }
        
        fNameToValueMap = new Hashtable(names.length);
        
        for (int i=0; i < names.length; i++) {
            if (values[i] != null) {
                fNameToValueMap.put(names[i], values[i]);
            }
        }

    }

    private void closeWindow(boolean sendAction) {

        setVisible(false);
        
        if (sendAction && fItems.getMinSelectionIndex() != fItems.getMaxSelectionIndex()) {
            sendAction = false;
        }
        
        if (sendAction) {
            Object value = fNameToValueMap.get(fItems.getSelectedValue());
            sendAction(value);
        }
        dispose();
    }

    /**
    * Handle the user input
    * @param obj the value object
    */
    private void sendAction(Object value) {

        StyleModifier modifier;
        if (value != null) {
            modifier = StyleModifier.createAddModifier(fKey, value);
        }
        else {
            AttributeSet set = new AttributeSet(fKey);
            modifier = StyleModifier.createRemoveModifier(set);
        }

        if (fCharacter == MenuItemSet.CHARACTER) {
            fTextPanel.modifyCharacterStyleOnSelection(modifier);
        }
        else {
            fTextPanel.modifyParagraphStyleOnSelection(modifier);
        }
    }
    
    private void selectValue(Object value, Object[] values) {
    
        for (int i=0; i < values.length; i++) {
        
            if ((value != null && value.equals(values[i])) || (value == null && values[i] == null)) {
                fItems.addSelectionInterval(i, i);
                fItems.ensureIndexIsVisible(i);
                return;
            }
        }
    }
    
    private void selectStyles(Object[] values) {
    
        Object value;
 
        if (fCharacter) {
            value = fTextPanel.getCharacterStyleOverSelection(fKey);
        }
        else {
            value = fTextPanel.getParagraphStyleOverSelection(fKey);
        }
        
        if (value != MTextPanel.MULTIPLE_VALUES) {
            selectValue(value, values);
        }
        else {
            fOKButton.setEnabled(false);
            
            int selLimit = fTextPanel.getSelectionEnd();
            MConstText text = fTextPanel.getText();
            for (int runStart = fTextPanel.getSelectionStart(); runStart <= selLimit;
                    runStart = fCharacter? text.characterStyleLimit(runStart) : 
                                           text.paragraphLimit(runStart)) {
            
                Object runVal;
                if (fCharacter) {
                    runVal = text.characterStyleAt(runStart).get(fKey);
                }
                else {
                    runVal = text.paragraphStyleAt(runStart).get(fKey);
                }
                if (runVal == null) {
                    runVal = fTextPanel.getDefaultValues().get(fKey);
                }
                
                selectValue(runVal, values);
                if (runStart == text.length()) {
                    break;
                }
            }
        }
        
        fItems.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                fOKButton.setEnabled(true);
                fItems.removeListSelectionListener(this);
            }
        });
    }
}
