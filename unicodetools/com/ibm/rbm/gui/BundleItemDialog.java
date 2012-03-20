/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.*;

import javax.swing.*;

import com.ibm.rbm.*;

//A dialog which displays the properties of a Bundle Item in an editable way

class BundleItemDialog extends JDialog implements ActionListener {
	RBManager rbm;
	BundleItem item;
	String user;
	boolean firstInit = true;	
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 375;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box0 = new Box(BoxLayout.X_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	Box box4 = new Box(BoxLayout.X_AXIS);
	Box box5 = new Box(BoxLayout.X_AXIS);
	Box box6 = new Box(BoxLayout.X_AXIS);
	Box box7 = new Box(BoxLayout.X_AXIS);
	Box box8 = new Box(BoxLayout.X_AXIS);
	
	JLabel groupLabel = new JLabel(Resources.getTranslation("dialog_group")); 
	JLabel keyLabel = new JLabel(Resources.getTranslation("dialog_key"));
	JLabel defTransLabel = new JLabel(Resources.getTranslation("dialog_default_translation"));
	JLabel transLabel = new JLabel(Resources.getTranslation("dialog_translation"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_comment"));
	JLabel lookupLabel = new JLabel(Resources.getTranslation("dialog_lookups"));
	JLabel createdLabel = new JLabel(Resources.getTranslation("dialog_created"));
	JLabel modifiedLabel = new JLabel(Resources.getTranslation("dialog_modified"));
	
	JComboBox groupComboBox;
	JTextField keyField;
	JTextField transField;
	JTextField defTransField;
	JTextField commentField;
	JLabel createdLabel2;
	JLabel modifiedLabel2;
	JLabel lookupLabel2 = null;
	JCheckBox transCheckBox;
	JButton saveButton = new JButton(Resources.getTranslation("button_edit"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	Box lookupBox = null;
	Box lookups[] = null;
	JLabel lookupLabels[] = null;
	JTextField lookupFields[] = null;
	
	public BundleItemDialog(RBManager rbm, BundleItem item, String user, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		this.user = user;
		this.item = item;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.getID() == KeyEvent.KEY_RELEASED) {
			actionPerformed(null);
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	private void initComponents(){
		// Error check
		if (item == null) closeWindow();
		if (!firstInit) closeWindow();
		
		// Initialize values
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		Bundle bundle = item.getParentGroup().getParentBundle();
		
		// Lookup the default translation
		String defTrans = new String();
		Object o = ((Bundle)rbm.getBundles().firstElement()).allItems.get(item.getKey());
		if (o != null)
			defTrans = ((BundleItem)o).getTranslation();
		
		keyField = new JTextField(item.getKey());
		keyField.setEnabled(false);
		defTransField = new JTextField(defTrans);
		defTransField.setEnabled(false);
		transField = new JTextField(item.getTranslation());
		commentField = new JTextField(item.getComment());
		String created[] = {df.format(item.getCreatedDate()), item.getCreator()};
		String modified[] = {df.format(item.getModifiedDate()), item.getModifier()};
		String createdString = Resources.getTranslation("dialog_date_person", created);
		String modifiedString = Resources.getTranslation("dialog_date_person", modified);
		createdLabel2 = new JLabel(item.getCreator() == null ? df.format(item.getCreatedDate()) : createdString);
		modifiedLabel2 = new JLabel(item.getModifier() == null ? df.format(item.getModifiedDate()) : modifiedString);
		transCheckBox = new JCheckBox(Resources.getTranslation("dialog_checkbox_translated"),item.isTranslated());
		
		groupComboBox = new JComboBox(bundle.getGroupsAsVector());
		for (int i=0; i < groupComboBox.getItemCount(); i++) {
			BundleGroup bg = (BundleGroup)groupComboBox.getItemAt(i);
			if (bg.getName().equals(item.getParentGroup().getName())) {
				groupComboBox.setSelectedIndex(i);
				break;
			}
		}
		groupComboBox.setEnabled(false);
		
		// Set up the components
		groupLabel.setPreferredSize(leftDim);
		groupComboBox.setPreferredSize(rightDim);
		keyLabel.setPreferredSize(leftDim);
		//keyField.setPreferredSize(rightDim);
		keyField.setColumns(30);
		defTransLabel.setPreferredSize(leftDim);
		//defTransField.setPreferredSize(rightDim);
		defTransField.setColumns(30);
		transLabel.setPreferredSize(leftDim);
		//transField.setPreferredSize(rightDim);
		transField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		//commentField.setPreferredSize(rightDim);
		commentField.setColumns(30);
		lookupLabel.setPreferredSize(leftDim);
		createdLabel.setPreferredSize(leftDim);
		createdLabel2.setPreferredSize(rightDim);
		modifiedLabel.setPreferredSize(leftDim);
		modifiedLabel2.setPreferredSize(rightDim);
		// Special setup for the lookup items if they exist
		if (item.getLookups().size() < 1) {
			lookupLabel2 = new JLabel(Resources.getTranslation("none"));
			lookupLabel2.setPreferredSize(rightDim);
		} else {
			lookupBox = new Box(BoxLayout.Y_AXIS);
			lookups = new Box[item.getLookups().size()];
			lookupLabels = new JLabel[item.getLookups().size()];
			lookupFields = new JTextField[item.getLookups().size()];
			Enumeration keys = item.getLookups().keys();
			for (int i = 0; i < item.getLookups().size(); i++) {
				String name = (String)keys.nextElement();
				String value = (String)item.getLookups().get(name);
				RBManagerGUI.debugMsg("X - Lookup: " + name + " -> " + value);
				lookups[i] = new Box(BoxLayout.X_AXIS);
				lookupLabels[i] = new JLabel("{" + name + "}");
				lookupLabels[i].setPreferredSize(new Dimension(30,row_height));
				lookupFields[i] = new JTextField(value);
				lookupFields[i].setPreferredSize(new Dimension(right_col_width-35,row_height));
				lookups[i].add(Box.createHorizontalGlue());
				lookups[i].add(lookupLabels[i]);
				lookups[i].add(Box.createHorizontalStrut(5));
				lookups[i].add(lookupFields[i]);
				lookupBox.add(lookups[i]);
			}
		}
		
		// Add the appropriate listeners
		if (firstInit) {
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					dialog.setVisible(false);
					dialog.dispose();
				}
			});
		
			saveButton.addActionListener(this);
			getRootPane().setDefaultButton(saveButton);

			transField.addFocusListener(new TranslationFocusListener(item.getTranslation(),transCheckBox));
		}
		
		box0.add(groupLabel); box0.add(groupComboBox);
		box1.add(keyLabel); box1.add(keyField);
		box8.add(defTransLabel); box8.add(defTransField);
		box2.add(transLabel); box2.add(transField);
		box3.add(commentLabel); box3.add(commentField);
		box4.add(Box.createHorizontalGlue()); box4.add(lookupLabel);
		if (lookupLabel2 != null) {
			box4.add(Box.createHorizontalStrut(5));
			box4.add(lookupLabel2);
		} else if (lookupBox != null) {
			box4.add(Box.createHorizontalStrut(5));
			box4.add(lookupBox);
		}
		box5.add(Box.createHorizontalGlue()); box5.add(createdLabel);
		box5.add(Box.createHorizontalStrut(5)); box5.add(createdLabel2);
		box6.add(Box.createHorizontalGlue()); box6.add(modifiedLabel);
		box6.add(Box.createHorizontalStrut(5)); box6.add(modifiedLabel2);
		box7.add(transCheckBox); box7.add(saveButton); box7.add(cancelButton);
		
		// Complete the initialization of the frame
		setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		mainBox.add(box0);
		mainBox.add(box1);
		mainBox.add(box8);
		mainBox.add(box2);
		mainBox.add(box3);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box4);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box5);
		mainBox.add(box6);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box7);
		getContentPane().add(mainBox, BorderLayout.CENTER);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
		
		firstInit = false;
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
	
	public void actionPerformed(ActionEvent ev) {
		if (ev == null && transField.hasFocus()) {
			// If we are in the translation field, then enter should create a new line character, not exit the dialog
			int caretPos = transField.getCaretPosition();
			String oldText = transField.getText();
			transField.setText(oldText.substring(0,caretPos) + "\n" + oldText.substring(caretPos,oldText.length()));
			transField.setCaretPosition(caretPos+1);
			validate();
			setSize(getPreferredSize());
			return;
		}
		
		// This action is called when the 'Edit' button is pressed
		item.setTranslation(transField.getText().trim());
		if (!item.getKey().equals(keyField.getText())) item.setKey(keyField.getText().trim());
		item.setComment(commentField.getText());
		item.setModifiedDate(new Date());
		item.setModifier(user);
		item.setTranslated(transCheckBox.isSelected());
		if (transCheckBox.isSelected()) {
			// Remove this item from the untranslated items, if it is there
			item.getParentGroup().getParentBundle().removeUntranslatedItem(item.getKey());
		} else {
			item.getParentGroup().getParentBundle().addUntranslatedItem(item);
		}
		if (lookups != null) {
			item.setLookups(new Hashtable());
			for (int i=0; i < lookups.length; i++) {
				String name = lookupLabels[i].getText().trim();
				if (name.indexOf("{") >= 0) name = name.substring(name.indexOf("{")+1,name.length());
				if (name.indexOf("}") >= 0) name = name.substring(0, name.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				item.getLookups().put(name,value);
			}
		}
		closeWindow();
	}
}
 
/**
 * A listener which checks a translation box to see if it changes, if it does, it marks the word as translated in a check box
 */
class TranslationFocusListener implements FocusListener {
	String original;
	JCheckBox cbox;
	boolean selected;
	
	public TranslationFocusListener(String original, JCheckBox cbox) {
		this.original = original;
		this.cbox = cbox;
		selected = cbox.isSelected();
	}
	
	public void focusGained(FocusEvent ev) {}
	
	public void focusLost(FocusEvent ev) {
		JTextField field = (JTextField)ev.getSource();
		if (field.getText().equals(original)) {
			cbox.setSelected(selected);
			return;
		}
		cbox.setSelected(true);
	}
}

