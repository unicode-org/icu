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
class BundleGroupCreationDialog extends JDialog {
	RBManager rbm;
	
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
	
	JTextArea instructionsArea = new JTextArea("");
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_group_comment"));
	JTextField nameField = new JTextField("");
	JTextField commentField = new JTextField("");
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	
	public BundleGroupCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	boolean createGroup() {
		if (rbm == null) return false;
		return rbm.createGroup(nameField.getText().trim(), commentField.getText().trim());
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = createGroup();
			if (!success) {
				String alert = Resources.getTranslation("error_create_group") + " " +
							   Resources.getTranslation("error_try_again_group");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	private void initComponents(){
		// Error check
		if (rbm == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_group");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		
		// Set up the components
		nameLabel.setPreferredSize(leftDim);
		nameField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		commentField.setColumns(30);
		getRootPane().setDefaultButton(createButton);
		
		box1.add(nameLabel); box1.add(nameField);
		box2.add(commentLabel); box2.add(commentField);
		box3.add(createButton);
		box3.add(Box.createHorizontalStrut(5));
		box3.add(cancelButton);
		
		instructionsArea.setBorder(BorderFactory.createEtchedBorder());
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				BundleGroupCreationDialog dialog =
					(BundleGroupCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.createGroup();
				if (!success) {
					String alert = Resources.getTranslation("error_create_group") + " " +
								   Resources.getTranslation("error_try_again_group");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
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
		//getContentPane().add(instructionsArea, BorderLayout.NORTH);
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

