/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.ibm.rbm.*;

/**
 * A dialog which allows the user to create a new Bundle Group
 */
class BundleGroupEditDialog extends JDialog {
	BundleGroup group;
	
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
	
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_group_comment"));
	JTextField nameField = new JTextField("");
	JTextField commentField = new JTextField("");
	JButton editButton = new JButton(Resources.getTranslation("button_edit"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	
	public BundleGroupEditDialog(BundleGroup group, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.group = group;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = editGroup();
			if (!success) {
				String alert = Resources.getTranslation("error_modify_group");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error_internal"),
											  JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	boolean editGroup() {
		if (group == null) return false;
		group.setName(nameField.getText().trim());
		group.setComment(commentField.getText().trim());
		return true;
	}
	
	private void initComponents(){
		// Error check
		if (group == null) {
			String alert = Resources.getTranslation("error_modify_group");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error_internal"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		
		// Set up the components
		nameLabel.setPreferredSize(leftDim);
		nameField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		commentField.setColumns(30);
		
		nameField.setText(group.getName());
		commentField.setText(group.getComment());
		getRootPane().setDefaultButton(editButton);
		
		box1.add(nameLabel); box1.add(nameField);
		box2.add(commentLabel); box2.add(commentField);
		box3.add(editButton);
		box3.add(Box.createHorizontalStrut(5));
		box3.add(cancelButton);
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				BundleGroupEditDialog dialog =
					(BundleGroupEditDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.editGroup();
				if (!success) {
					String alert = Resources.getTranslation("error_modify_group");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error_internal"),
												  JOptionPane.ERROR_MESSAGE);
				} else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		
		// Complete the initialization of the frame
		setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		mainBox.add(box1);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box2);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().removeAll();
		getContentPane().add(mainBox, BorderLayout.CENTER);
		getContentPane().add(box3, BorderLayout.SOUTH);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}

