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
 * The class used to display groups
 */
class RBGroupPanel extends JPanel {
	RBManager rbm;
	Bundle bundle;
	RBManagerGUI listener;
	
	// Components
	JLabel      jLabelGroupTitle;
	JLabel      jLabelGroupNameTitle;
	JLabel      jLabelGroupCommentTitle;
	JLabel      jLabelGroupComment;
	JComboBox   jComboBoxGroup;
	JTable      jTableGroupTable;
	JScrollPane jScrollPaneGroupTable;
	Box         boxGroupInfo;
	Box         box1;
	Box         box2;
	Box         box3;
	Box         box4;
	Dimension   topDim = new Dimension();
	Dimension   leftDim = new Dimension();
	Dimension   rightDim = new Dimension();
	
	// Components - Manager
	JList       jListGroup;
	JButton     createItemButton;
	JButton     createGroupButton;
	JButton     editItemButton;
	JButton     editGroupButton;
	JButton     deleteItemButton;
	JButton     deleteGroupButton;
	JPanel      itemPanel;
	JPanel      groupPanel;
	
	public RBGroupPanel(RBManagerGUI gui) {
		super();
		listener = gui;
	}
	
	public void setBundle(Bundle b) {
		rbm = null;
		if (bundle == null) {
			bundle = b;
			initComponents();
		} else if (bundle != b) {
			bundle = b;
			updateComponents();
		}
	}
	
	public void setManager(RBManager m) {
		bundle = null;
		if (rbm == null) {
			rbm = m;
			initComponents();
		} else if (rbm != m) {
			rbm = m;
			updateComponents();
		}
	}
	
	public void removeElements() {
		if (rbm != null || bundle != null) {
			rbm = null;
			bundle = null;
			initComponents();
		}
	}
	
	// Marks the selected resource as translated and removes from this view
	private void markSelectedResourceAsTranslated() {
		if (bundle == null) return;
		if (jTableGroupTable.getSelectedRow() < 0) return;
		if (jTableGroupTable.getModel() instanceof GroupItemsTableModel) {
			int row = jTableGroupTable.getSelectedRow();
			GroupItemsTableModel model = (GroupItemsTableModel)jTableGroupTable.getModel();
			BundleItem item = model.getBundleItem(row);
			item.setTranslated(true);
			model.update();
		}
	}
	
	// Removes the selected resource from the resource file
	private void deleteSelectedResource() {
		if (bundle == null) return;
		if (jTableGroupTable.getSelectedRow() < 0) return;
		if (jTableGroupTable.getModel() instanceof GroupItemsTableModel) {
			int row = jTableGroupTable.getSelectedRow();
			GroupItemsTableModel model = (GroupItemsTableModel)jTableGroupTable.getModel();
			BundleItem item = model.getBundleItem(row);
			if (item.getParentGroup() != null && item.getParentGroup().getParentBundle() != null) {
				Bundle parentBundle = item.getParentGroup().getParentBundle();
				parentBundle.removeItem(item.getKey());
			}
			model.update();
		}
	}
		
	private void initComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelGroupTitle          = new JLabel(bundle.name);
			jComboBoxGroup            = new JComboBox(new GroupComboBoxModel(bundle));
			
			jTableGroupTable          = new JTable(new GroupItemsTableModel((BundleGroup)jComboBoxGroup.getSelectedItem()));
			jScrollPaneGroupTable     = new JScrollPane(jTableGroupTable);
			jLabelGroupNameTitle      = new JLabel(Resources.getTranslation("basegroup_group_name"));
			jLabelGroupCommentTitle   = new JLabel(Resources.getTranslation("basegroup_group_comment"));
			jLabelGroupComment        = new JLabel(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			boxGroupInfo              = new Box(BoxLayout.Y_AXIS);
			box1                      = new Box(BoxLayout.X_AXIS);
			box2                      = new Box(BoxLayout.X_AXIS);
			box3                      = new Box(BoxLayout.X_AXIS);
			
			// Lower panel components
			JPanel  lowerPanel = new JPanel();
			JButton deleteButton = new JButton(Resources.getTranslation("button_delete_resource"));
			JButton translateButton = new JButton(Resources.getTranslation("button_mark_translated"));
			
			deleteButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			translateButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_mark_translated_trigger")));
			lowerPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("languageuntrans_selected_resources_options")));
			lowerPanel.setLayout(new GridLayout(1,2));
			
			topDim   = new Dimension(getSize().width, 35);
			leftDim  = new Dimension(150,25);
			rightDim = new Dimension(getSize().width - leftDim.width, leftDim.height);
			
			jLabelGroupNameTitle.setPreferredSize(leftDim);
			jLabelGroupNameTitle.setHorizontalAlignment(SwingConstants.LEFT);
			jLabelGroupCommentTitle.setPreferredSize(leftDim);
			jComboBoxGroup.setPreferredSize(rightDim);
			jLabelGroupTitle.setPreferredSize(topDim);
			jLabelGroupComment.setPreferredSize(rightDim);
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableGroupTable.addMouseListener(listener);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jListGroup));
			
			jLabelGroupTitle.setFont(new Font("SansSerif",Font.PLAIN,18));
			
			// Add action listeners
			deleteButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					deleteSelectedResource();
				}
			});
			
			translateButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					markSelectedResourceAsTranslated();
				}
			});
			
			// Update the display
			setLayout(new BorderLayout());
			removeAll();
			lowerPanel.add(deleteButton);
			lowerPanel.add(translateButton);
			box1.add(Box.createHorizontalGlue());
			box1.add(jLabelGroupTitle);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			boxGroupInfo.add(box1);
			boxGroupInfo.add(box2);
			boxGroupInfo.add(box3);
			boxGroupInfo.add(jScrollPaneGroupTable);
			add(boxGroupInfo, BorderLayout.CENTER);
			add(lowerPanel, BorderLayout.SOUTH);
		} else if (rbm != null) {
			Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
			jLabelGroupTitle          = new JLabel(rbm.getBaseClass() + " - " + Resources.getTranslation("groups"));
			jComboBoxGroup            = new JComboBox(new GroupComboBoxModel(mainBundle));//mainBundle.getGroupsAsVector());
			
			jListGroup                = new JList(new GroupItemsListModel((BundleGroup)jComboBoxGroup.getSelectedItem()));
			jScrollPaneGroupTable     = new JScrollPane(jListGroup);
			jLabelGroupNameTitle      = new JLabel(Resources.getTranslation("basegroup_group_name"));
			jLabelGroupCommentTitle   = new JLabel(Resources.getTranslation("basegroup_group_comment"));
			try {
				jLabelGroupComment    = new JLabel(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			} catch (NullPointerException npe) {
				jLabelGroupComment    = new JLabel("");
			}
			
			boxGroupInfo              = new Box(BoxLayout.Y_AXIS);
			box1                      = new Box(BoxLayout.X_AXIS);
			box2                      = new Box(BoxLayout.X_AXIS);
			box3                      = new Box(BoxLayout.X_AXIS);
			box4                      = new Box(BoxLayout.Y_AXIS);
			
			createItemButton          = new JButton(Resources.getTranslation("button_create_resource"));
			createGroupButton         = new JButton(Resources.getTranslation("button_create_group"));
			deleteItemButton          = new JButton(Resources.getTranslation("button_delete_resource"));
			deleteGroupButton         = new JButton(Resources.getTranslation("button_delete_group"));
			editItemButton            = new JButton(Resources.getTranslation("button_edit_resource"));
			editGroupButton           = new JButton(Resources.getTranslation("button_edit_group"));
			
			itemPanel                 = new JPanel();
			groupPanel                = new JPanel();
			
			itemPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																  Resources.getTranslation("basegroup_item_options")));
			groupPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																  Resources.getTranslation("basegroup_group_options")));
			itemPanel.setLayout(new GridLayout(1,3));
			groupPanel.setLayout(new GridLayout(1,3));
			itemPanel.setMaximumSize(new Dimension(20000,50));
			groupPanel.setMaximumSize(new Dimension(20000,50));
			
			topDim   = new Dimension(getSize().width, 35);
			leftDim  = new Dimension(150,25);
			rightDim = new Dimension(getSize().width - leftDim.width, leftDim.height);
			
			createItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_resource_trigger")));
			editItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_edit_resource_trigger")));
			deleteItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			createGroupButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_group_trigger")));
			
			jLabelGroupNameTitle.setPreferredSize(leftDim);
			jLabelGroupCommentTitle.setPreferredSize(leftDim);
			jComboBoxGroup.setPreferredSize(rightDim);
			jLabelGroupTitle.setPreferredSize(topDim);
			jLabelGroupComment.setPreferredSize(rightDim);
			
			jListGroup.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jListGroup));
			
			jLabelGroupTitle.setFont(new Font("SansSerif",Font.PLAIN,18));

			// Add the listeners
			jListGroup.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) { 
					if(ev.getClickCount() == 2 && ev.getSource() instanceof JList) {
						// A double click means they want to edit a bundle item
						RBGroupPanel panel = (RBGroupPanel)
							((JList)ev.getSource()).getParent().getParent().getParent().getParent();
						
						if (((JList)ev.getSource()).getSelectedValue() != null)
							new BundleItemCreationDialog((BundleItem)((JList)ev.getSource()).getSelectedValue(),
								panel.listener.rbm, panel.listener, Resources.getTranslation("dialog_title_edit_item"), true);
					}
				}
			});
			
			createItemButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					new BundleItemCreationDialog(((BundleGroup)panel.jComboBoxGroup.getSelectedItem()).getName(),
												 panel.listener.rbm, panel.listener,
												 Resources.getTranslation("dialog_title_new_item"), true);
					panel.updateComponents();
				}
			});
			createGroupButton.addActionListener(listener);
			editItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					if (panel.jListGroup.getSelectedValue() != null)
						new BundleItemCreationDialog((BundleItem)panel.jListGroup.getSelectedValue(),
							panel.listener.rbm, panel.listener, Resources.getTranslation("dialog_title_edit_item"), true);
					panel.updateComponents();
				}
			});
			editGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					new BundleGroupEditDialog((BundleGroup)panel.jComboBoxGroup.getSelectedItem(),
											  panel.listener, Resources.getTranslation("dialog_title_edit_group"), true);
				panel.updateComponents();
				}
			});
			deleteGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					int response = JOptionPane.showConfirmDialog(panel.listener,
						Resources.getTranslation("dialog_warning_delete_group"),
						Resources.getTranslation("dialog_title_delete_group"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						// Delete the group 
						int index = panel.jComboBoxGroup.getSelectedIndex();
						BundleGroup group = (BundleGroup)panel.jComboBoxGroup.getSelectedItem();
						if (group.getName().equals("Ungrouped Items")) return;
						if (index < panel.jComboBoxGroup.getItemCount()-1) panel.jComboBoxGroup.setSelectedIndex(index+1);
						else panel.jComboBoxGroup.setSelectedIndex(index-1);
						panel.rbm.deleteGroup(group.getName());
					}
					panel.updateComponents();
				}
			});
			
			deleteItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					int response = JOptionPane.showConfirmDialog(panel.listener,
						Resources.getTranslation("dialog_warning_delete_item"),
						Resources.getTranslation("dialog_title_delete_item"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						Object o = panel.jListGroup.getSelectedValue();
						if (o != null) {
							BundleItem item = (BundleItem) o;
							handleDeleteItem(item.getKey());
							//panel.rbm.deleteItem(item.getKey());
						}
					}
					panel.updateComponents();
				}
			});
			
			// Update the display
			setLayout(new BorderLayout());
			removeAll();
			itemPanel.add(createItemButton, BorderLayout.WEST);
			itemPanel.add(editItemButton, BorderLayout.CENTER);
			itemPanel.add(deleteItemButton, BorderLayout.EAST);
			groupPanel.add(createGroupButton, BorderLayout.WEST);
			groupPanel.add(editGroupButton, BorderLayout.CENTER);
			groupPanel.add(deleteGroupButton, BorderLayout.EAST);
			box1.add(Box.createHorizontalGlue());
			box1.add(jLabelGroupTitle);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			box4.add(Box.createVerticalStrut(5));
			box4.add(groupPanel);
			box4.add(Box.createVerticalStrut(10));
			box4.add(itemPanel);
			box4.add(Box.createVerticalStrut(5));
			boxGroupInfo.add(box1);
			boxGroupInfo.add(box2);
			boxGroupInfo.add(box3);
			boxGroupInfo.add(jScrollPaneGroupTable);
			boxGroupInfo.add(box4);
			add(boxGroupInfo, BorderLayout.CENTER);
		} else {
			removeAll();
		}
	}
	
	public void updateComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelGroupTitle.setText(bundle.name);
			
			topDim.width = getSize().width;
			rightDim.width = getSize().width - leftDim.width;
			
			box2.removeAll();
			box3.removeAll();
			boxGroupInfo.remove(jScrollPaneGroupTable);
			
			String selName = null;
			try {
				selName = ((BundleGroup)jComboBoxGroup.getSelectedItem()).getName();
			} catch (Exception e) {}
			jComboBoxGroup = new JComboBox(new GroupComboBoxModel(bundle));//bundle.getGroupsAsVector());
			for (int i = 0; i < jComboBoxGroup.getItemCount(); i++) {
				BundleGroup bg = (BundleGroup)jComboBoxGroup.getItemAt(i);
				if (bg.getName().equals(selName)) jComboBoxGroup.setSelectedIndex(i);
			}
			
			((GroupItemsTableModel)jTableGroupTable.getModel()).setGroup((BundleGroup)jComboBoxGroup.getSelectedItem());
			jScrollPaneGroupTable = new JScrollPane(jTableGroupTable);
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jTableGroupTable));
			
			// Update the group comment
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			// Update the display
			jComboBoxGroup.setPreferredSize(rightDim);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			boxGroupInfo.add(jScrollPaneGroupTable);
			
		} else if (rbm != null) {
			
			// Update the list of groups
			try {((GroupComboBoxModel)jComboBoxGroup.getModel()).update();}
			catch (Exception e) {}
			// Update the group comment
			if ((BundleGroup)jComboBoxGroup.getSelectedItem() != null)
				jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			else jLabelGroupComment.setText("");
			// Update the list of resources
			ListModel lmodel = jListGroup.getModel();
			if (lmodel instanceof GroupItemsListModel)
				((GroupItemsListModel)lmodel).update();
			else {
				GroupItemsListModel newModel = new GroupItemsListModel((BundleGroup)jComboBoxGroup.getSelectedItem());
				RBManagerGUI.debugMsg("List Model not as anticipated: " + lmodel.getClass().getName());
				jListGroup.setModel(newModel);
				newModel.update();
			}
			/*
			try {GroupItemsListModel mod = (GroupItemsListModel) lmodel; }
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
			*/
			if (lmodel instanceof AbstractListModel) {
				RBManagerGUI.debugMsg("List Model is an AbstractListModel");
			}  else {
				RBManagerGUI.debugMsg("List Model is not an AbstractListModel");
			}
		} else {
			removeAll();
		}
		//validate();
	}
	
	private void handleDeleteItem(String key) {
		if (rbm != null) rbm.deleteItem(key);
	}
}

/**
 * The action listener which monitors changes in the group to display
 */
class GroupComboActionListener implements ActionListener {
	JTable table;
	JList list;
	
	protected GroupComboActionListener(JTable table) {
		list = null;
		this.table = table;
	}
	
	protected GroupComboActionListener(JList list) {
		table = null;
		this.list = list;
	}
	
	public void actionPerformed(ActionEvent ev) {
		JComboBox cbox = (JComboBox)ev.getSource();
		
		if (table != null) {
			BundleGroup bg = (BundleGroup)cbox.getSelectedItem();
			((GroupItemsTableModel)table.getModel()).setGroup((BundleGroup)cbox.getSelectedItem());
			//table.validate();
			Container c = table.getParent();
			while (!(c instanceof RBGroupPanel)) {
			    c = c.getParent();
			} 
			((RBGroupPanel)c).updateComponents();
		} else if (list != null) {
			list.setListData(((BundleGroup)cbox.getSelectedItem()).getItemsAsVector());
			//list.validate();
			Container c = list.getParent();
			while (!(c instanceof RBGroupPanel)) { c = c.getParent(); } 
			((RBGroupPanel)c).updateComponents();
		} else RBManagerGUI.debugMsg("Selection changed, but no active components");
	}
}
