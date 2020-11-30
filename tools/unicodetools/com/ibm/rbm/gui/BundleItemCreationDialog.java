/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.ibm.rbm.*;

/**
 * A dialog which allows the user to create a new Bundle Item
 */
class BundleItemCreationDialog extends JDialog {
	RBManager rbm;
	String groupName;
	BundleItem item;
	boolean firstInit = true;
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 275;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	Box box4 = new Box(BoxLayout.X_AXIS);
	Box box5 = new Box(BoxLayout.X_AXIS);
	Box box6 = new Box(BoxLayout.X_AXIS);
	
	JLabel instructionsLabel = new JLabel("");
	JLabel groupLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_key"));
	JLabel transLabel = new JLabel(Resources.getTranslation("dialog_translation"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_comment"));
	JLabel lookupLabel = new JLabel(Resources.getTranslation("dialog_lookups"));
	
	JComboBox groupComboBox = new JComboBox();
	JTextField nameField = new JTextField("");
	JTextField transField = new JTextField("");
	JTextField commentField = new JTextField("");
	JTextField lookupFields[] = null;
	JLabel noLookupLabel = null;
	Box lookupBox = null;
	Box lookupBoxes[] = null;
	JLabel lookupLabels[] = null;
	
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton createMoreButton = new JButton(Resources.getTranslation("button_create_more"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	Hashtable lookups = new Hashtable();
	
	public BundleItemCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		groupName = null;
		item = null;
		initComponents();
	}
	
	public BundleItemCreationDialog(String groupName, RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		this.groupName = groupName;
		item = null;
		initComponents();
	}
	
	public BundleItemCreationDialog(BundleItem item, RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.item = item;
		this.rbm = rbm;
		groupName = item.getParentGroup().getName();
		initComponents();
	}
	
	boolean createItem() {
		if (rbm == null) return false;
		Hashtable lookupHash = new Hashtable();
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookupHash.put(name,value);
			}
		}
		return rbm.createItem(nameField.getText().trim(), transField.getText().trim(),
							  ((BundleGroup)groupComboBox.getSelectedItem()).getName(),
							  commentField.getText().trim(), lookupHash);
	}
	
	boolean editItem() {
		if (item == null) return false;
		Hashtable lookupHash = new Hashtable();
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookupHash.put(name,value);
			}
		}
		return rbm.editItem(item, nameField.getText().trim(),
							transField.getText().trim(), ((BundleGroup)groupComboBox.getSelectedItem()).getName(),
							commentField.getText().trim(), lookupHash); 
	}
	
	private void clearComponents() {
		nameField.setText("");
		transField.setText("");
		commentField.setText("");
		initComponents();
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.getID() == KeyEvent.KEY_RELEASED) {
			if (transField.hasFocus()) {
				// If we are in the translation field, then enter should create a new line character, not exit the dialog
				int caretPos = transField.getCaretPosition();
				String oldText = transField.getText();
				transField.setText(oldText.substring(0,caretPos) + "\n" + oldText.substring(caretPos,oldText.length()));
				transField.setCaretPosition(caretPos+1);
				validate();
				setSize(getPreferredSize());
				return;
			}
			
			BundleItemCreationDialog dialog = this;
			boolean success = false;
			if (dialog.item == null) success = dialog.createItem();
			else success = dialog.editItem();
			if (!success) {
				String alert = (item == null ? Resources.getTranslation("error_create_item") :
											   Resources.getTranslation("error_modify_item"));
				alert += " " + Resources.getTranslation("error_try_again_item");
				JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
											  JOptionPane.ERROR_MESSAGE);
			} else {
				((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
				((RBManagerGUI)dialog.getParent()).invalidate();
				//((RBManagerGUI)dialog.getParent()).validateMyTree();
				dialog.setVisible(false);
				dialog.dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
			closeWindow();
		}
	}
	
	private void initComponents(){
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		// Error check
		if (rbm == null || rbm.getBundles() == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_item");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
		if (firstInit) {
			groupComboBox = new JComboBox(mainBundle.getGroupsAsVector());
			if (groupName != null) {
				for (int i = 0; i < groupComboBox.getItemCount(); i++) {
					BundleGroup bg = (BundleGroup)groupComboBox.getItemAt(i);
					if (bg.getName().equals(groupName)) {
						groupComboBox.setSelectedIndex(i);
						break;
					}
				}
			}
		}
	
		if (firstInit && item != null) {
			// We are editing, not creating an item
			createButton.setText(Resources.getTranslation("button_edit"));
			createMoreButton.setText(Resources.getTranslation("button_edit_more"));
			if (item.getKey() != null) nameField.setText(item.getKey());
			if (item.getComment() != null) commentField.setText(item.getComment());
			if (item.getTranslation() != null) transField.setText(item.getTranslation());
			if (item.getLookups() != null) lookups = item.getLookups();
		}
		
		String currentTrans = transField.getText();
		// ** LOOKUPS **
		// Update the lookups if necessary
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookups.put(name,value);
			}
		}
		// Remove old lookups if necessary
		Enumeration keys = lookups.keys();
		while (keys.hasMoreElements()) {
			String name = (String)keys.nextElement();
			if (currentTrans.indexOf("{" + name + "}") < 0) {
				lookups.remove(name);
			}
		}
		// Add new lookups if necessary
		if (currentTrans != null && currentTrans.indexOf("{") >= 0) {
			while (currentTrans.indexOf("{") >= 0) {
				currentTrans = currentTrans.substring(currentTrans.indexOf("{")+1,currentTrans.length());
				String name = currentTrans.substring(0,currentTrans.indexOf("}"));
				if (!lookups.containsKey(name)) {
					lookups.put(name,"");
				}
			}
		}
		// Remove components
		box5.removeAll();
		
		// Now create the visual components for the lookups
		if (lookups.size() > 0) {
			noLookupLabel = null;
			lookupBox = new Box(BoxLayout.Y_AXIS);
			lookupBoxes = new Box[lookups.size()];
			lookupFields = new JTextField[lookups.size()];
			lookupLabels = new JLabel[lookups.size()];
			int count = 0;
			keys = lookups.keys();
			while (keys.hasMoreElements()) {
				String name = (String)keys.nextElement();
				String value = (String)lookups.get(name);
				RBManagerGUI.debugMsg("Lookup: " + name + " -> " + value);
				RBManagerGUI.debugMsg(lookups.toString());
				lookupBoxes[count] = new Box(BoxLayout.X_AXIS);
				lookupFields[count] = new JTextField((value == null ? "" : value));
				lookupLabels[count] = new JLabel("{" + name + "}");
				lookupBoxes[count].add(Box.createHorizontalGlue());
				lookupBoxes[count].add(lookupLabels[count]);
				lookupBoxes[count].add(Box.createHorizontalStrut(5));
				lookupBoxes[count].add(lookupFields[count]);
				lookupBox.add(lookupBoxes[count]);
				count++;
			}
		} else {
			lookupBox = null;
			lookupBoxes = null;
			lookupFields = null;
			lookupLabels = null;
			noLookupLabel = new JLabel(Resources.getTranslation("none"));
		}
		
		// Set up the components
		if (firstInit) {
			groupLabel.setPreferredSize(leftDim);
			groupComboBox.setPreferredSize(rightDim);
			nameLabel.setPreferredSize(leftDim);
			nameField.setColumns(30);
			commentLabel.setPreferredSize(leftDim);
			commentField.setColumns(30);
			transLabel.setPreferredSize(leftDim);
			transField.setColumns(30);
			lookupLabel.setPreferredSize(leftDim);
			
			box1.add(groupLabel); box1.add(groupComboBox);
			box2.add(nameLabel); box2.add(nameField);
			box4.add(commentLabel); box4.add(commentField);
			box3.add(transLabel); box3.add(transField);
			
			createButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_trigger")));
			createMoreButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_more_trigger")));
			getRootPane().setDefaultButton(createButton);
		}
		box5.add(Box.createHorizontalGlue()); box5.add(lookupLabel); box5.add(Box.createHorizontalStrut(5));
		if (noLookupLabel != null) {
			noLookupLabel.setPreferredSize(rightDim);
			box5.add(noLookupLabel);
		}
		else
			box5.add(lookupBox);
		if (firstInit) {
			box6.add(createButton);
			box6.add(Box.createHorizontalStrut(5));
			if (item == null)
				box6.add(createMoreButton);
			box6.add(Box.createHorizontalStrut(5));
			box6.add(cancelButton);
		}
		
		instructionsLabel.setBorder(BorderFactory.createEtchedBorder());
		
		// Add the appropriate listeners
		if (firstInit) {
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					dialog.setVisible(false);
					dialog.dispose();
				}
			});
		
			createButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					boolean success = false;
					if (dialog.item == null) success = dialog.createItem();
					else success = dialog.editItem();
					if (!success) {
						String alert = (item == null ? Resources.getTranslation("error_create_item") :
													   Resources.getTranslation("error_modify_item"));
						alert += " " + Resources.getTranslation("error_try_again_item");
						JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
													  JOptionPane.ERROR_MESSAGE);
					} else {
						((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
						((RBManagerGUI)dialog.getParent()).invalidate();
						//((RBManagerGUI)dialog.getParent()).validateMyTree();
						dialog.setVisible(false);
						dialog.dispose();
					}
				}
			});
			
			createMoreButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					boolean success = false;
					if (dialog.item == null) success = createItem();
					else success = dialog.editItem();
					if (!success) {
						String alert = (item == null ? Resources.getTranslation("error_create_item") :
													   Resources.getTranslation("error_modify_item"));
						alert += " " + Resources.getTranslation("error_try_again_item");
						JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
													  JOptionPane.ERROR_MESSAGE);
					} else {
						((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
						((RBManagerGUI)dialog.getParent()).invalidate();
						//((RBManagerGUI)dialog.getParent()).validateMyTree();
						dialog.clearComponents();
					}
				}
			});
		
			transField.addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent ev) {}
				public void focusLost(FocusEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JTextField)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					firstInit = false;
					dialog.initComponents();
				}
			});
		}
		
		// Complete the initialization of the frame
		if (firstInit)
			setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		//mainBox.add(instructionsLabel);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box1);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box2);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box3);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box4);
		mainBox.add(Box.createVerticalStrut(5));
		if (noLookupLabel == null) {
			mainBox.add(box5);
			mainBox.add(Box.createVerticalStrut(5));
		}
		mainBox.add(box6);
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
}

