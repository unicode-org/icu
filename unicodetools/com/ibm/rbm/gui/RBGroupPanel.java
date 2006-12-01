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
import javax.swing.table.*;
import javax.swing.event.*;

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
			
			// Lower panel components
			JPanel  lowerPanel = new JPanel();
			JButton deleteButton = new JButton(Resources.getTranslation("button_delete_resource"));
			JButton translateButton = new JButton(Resources.getTranslation("button_mark_translated"));
			
			deleteButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			translateButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_mark_translated_trigger")));
			lowerPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("languageuntrans_selected_resources_options")));
			lowerPanel.setLayout(new GridLayout(1,2));
			
			jLabelGroupNameTitle.setHorizontalAlignment(SwingConstants.LEFT);
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableGroupTable.addMouseListener(listener);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(this));
			
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
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			removeAll();
			lowerPanel.add(deleteButton);
			lowerPanel.add(translateButton);

			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(jLabelGroupTitle, gbc);
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			add(jLabelGroupNameTitle, gbc);
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(jComboBoxGroup, gbc);
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			add(jLabelGroupCommentTitle, gbc);
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(jLabelGroupComment, gbc);
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weighty = 1.0;
			add(jScrollPaneGroupTable, gbc);
			gbc.weighty = 0.0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(lowerPanel, gbc);
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
			
			createItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_resource_trigger")));
			editItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_edit_resource_trigger")));
			deleteItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			createGroupButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_group_trigger")));
			
			jListGroup.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(this));
			
			jLabelGroupTitle.setFont(new Font("SansSerif",Font.PLAIN,18));

			// Add the listeners
			jListGroup.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) { 
					if(ev.getClickCount() == 2 && ev.getSource() instanceof JList) {
						// A double click means they want to edit a bundle item
						if (((JList)ev.getSource()).getSelectedValue() != null)
							new BundleItemCreationDialog((BundleItem)((JList)ev.getSource()).getSelectedValue(),
								listener.rbm, listener, Resources.getTranslation("dialog_title_edit_item"), true);
					}
				}
			});
			
			createItemButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					new BundleItemCreationDialog(((BundleGroup)jComboBoxGroup.getSelectedItem()).getName(),
												 listener.rbm, listener,
												 Resources.getTranslation("dialog_title_new_item"), true);
					updateComponents();
				}
			});
			createGroupButton.addActionListener(listener);
			editItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					if (jListGroup.getSelectedValue() != null)
						new BundleItemCreationDialog((BundleItem)jListGroup.getSelectedValue(),
							listener.rbm, listener, Resources.getTranslation("dialog_title_edit_item"), true);
					updateComponents();
				}
			});
			editGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					new BundleGroupEditDialog((BundleGroup)jComboBoxGroup.getSelectedItem(),
											  listener, Resources.getTranslation("dialog_title_edit_group"), true);
					updateComponents();
				}
			});
			deleteGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					int response = JOptionPane.showConfirmDialog(listener,
						Resources.getTranslation("dialog_warning_delete_group"),
						Resources.getTranslation("dialog_title_delete_group"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						// Delete the group 
						int index = jComboBoxGroup.getSelectedIndex();
						BundleGroup group = (BundleGroup)jComboBoxGroup.getSelectedItem();
						if (group.getName().equals("Ungrouped Items"))
							return;
						if (index < jComboBoxGroup.getItemCount()-1)
							jComboBoxGroup.setSelectedIndex(index+1);
						else
							jComboBoxGroup.setSelectedIndex(index-1);
						rbm.deleteGroup(group.getName());
					}
					updateComponents();
				}
			});
			
			deleteItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					int response = JOptionPane.showConfirmDialog(listener,
						Resources.getTranslation("dialog_warning_delete_item"),
						Resources.getTranslation("dialog_title_delete_item"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						Object o = jListGroup.getSelectedValue();
						if (o != null) {
							BundleItem item = (BundleItem) o;
							handleDeleteItem(item.getKey());
							//panel.rbm.deleteItem(item.getKey());
						}
					}
					updateComponents();
				}
			});
			
			// Update the display
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			removeAll();
			itemPanel.add(createItemButton, BorderLayout.WEST);
			itemPanel.add(editItemButton, BorderLayout.CENTER);
			itemPanel.add(deleteItemButton, BorderLayout.EAST);
			groupPanel.add(createGroupButton, BorderLayout.WEST);
			groupPanel.add(editGroupButton, BorderLayout.CENTER);
			groupPanel.add(deleteGroupButton, BorderLayout.EAST);

			
			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(jLabelGroupTitle, gbc);
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			add(jLabelGroupNameTitle, gbc);
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(jComboBoxGroup, gbc);
			gbc.weightx = 0.0;
			gbc.gridwidth = 1;
			add(jLabelGroupCommentTitle, gbc);
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(jLabelGroupComment, gbc);
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weighty = 1.0;
			add(jScrollPaneGroupTable, gbc);
			gbc.weighty = 0.0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(groupPanel, gbc);
			add(itemPanel, gbc);
		} else {
			removeAll();
		}
	}
	
	public void updateComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelGroupTitle.setText(bundle.name);
			
			((GroupItemsTableModel)jTableGroupTable.getModel()).setGroup((BundleGroup)jComboBoxGroup.getSelectedItem());
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			// Update the group comment
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			((GroupComboBoxModel)jComboBoxGroup.getModel()).update();
		} else if (rbm != null) {
			
			// Update the list of groups
//			try {
				((GroupComboBoxModel)jComboBoxGroup.getModel()).update();
//			}
//			catch (Exception e) {}
			// Update the group comment
			if ((BundleGroup)jComboBoxGroup.getSelectedItem() != null)
				jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			else
				jLabelGroupComment.setText("");
			// Update the list of resources
			ListModel lmodel = jListGroup.getModel();
			if (lmodel instanceof GroupItemsListModel) {
				//((GroupItemsListModel)lmodel).update();
				((GroupItemsListModel)lmodel).setGroup((BundleGroup)jComboBoxGroup.getSelectedItem());
			}
			else {
				GroupItemsListModel newModel = new GroupItemsListModel((BundleGroup)jComboBoxGroup.getSelectedItem());
				RBManagerGUI.debugMsg("List Model not as anticipated: " + lmodel.getClass().getName());
				jListGroup.setModel(newModel);
				newModel.update();
			}
		} else {
			RBManagerGUI.debugMsg("Update, but no active components");
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
	RBGroupPanel panel;
	
	protected GroupComboActionListener(RBGroupPanel panel) {
		this.panel = panel;
	}
	
	public void actionPerformed(ActionEvent ev) {
		panel.updateComponents();
	}
}

/**
 * The list model for groups
 */
class GroupItemsListModel extends AbstractListModel {
	BundleGroup group;
	
	public void setGroup(BundleGroup group) {
		this.group = group;
		update();
	}
	
	public GroupItemsListModel(BundleGroup group) {
		this.group = group;
	}
	
	public int getSize() {
		if (group == null)
			return 0;
		int result = group.getItemCount();
		return result;
	}
	
	public Object getElementAt(int index) {
		return group.getBundleItem(index);
	}
	
	public void update() {
		fireContentsChanged(this, 0, getSize()-1);
	}
}

/**
 * The table model for searched Items
 */
class GroupComboBoxModel extends DefaultComboBoxModel {
	Bundle bundle;
	
	public GroupComboBoxModel (Bundle bundle) {
		this.bundle = bundle;
		setSelectedItem(bundle.getBundleGroup(0));
	}
	
	public int getSize() {
		return bundle.getGroupCount();
	}
	
	public Object getElementAt(int index) {
		return bundle.getBundleGroup(index);
	}
	
	public Object getSelectedItem() {
		return super.getSelectedItem();
		//return getElementAt(0);
	}
	
	public void update() {
		fireContentsChanged(this, 0, getSize()-1);
	}
}

/**
 * The table model for bundle groups
 */
class GroupItemsTableModel extends AbstractTableModel {
	BundleGroup group;
	
	public GroupItemsTableModel(BundleGroup group) {
		this.group = group;
	}
	
	public int getColumnCount() { return 3; }
		    
	public int getRowCount() {
		return group.getItemCount();
	}
	
	public void setGroup(BundleGroup bg) {
		group = bg;
		fireTableChanged(new TableModelEvent(this));
	}
			
	public Object getValueAt(int row, int col) {
		BundleItem item = group.getBundleItem(row);
				
		String retStr = null;
				
		switch(col) {
		case 0:
			retStr = item.getKey();
			break;
		case 1:
			retStr = item.getTranslation();
			break;
		case 2:
			retStr = (item.getComment() == null ? "" : item.getComment());
			break;
		default:
			retStr = Resources.getTranslation("table_cell_error");
		}
				
		return retStr;
	}
	
	public String getColumnName(int col) {
		if (col == 0) return Resources.getTranslation("languagegroup_column_key");
		else if (col == 1) return Resources.getTranslation("languagegroup_column_translation");
		else if (col == 2) return Resources.getTranslation("languagegroup_column_comment");
		else return Resources.getTranslation("table_column_error");
	}
	
	public BundleItem getBundleItem(int row) {
		if (row >= group.getItemCount())
		    return null;
		return group.getBundleItem(row);
	}
	
	public void update() {
		fireTableDataChanged();
	}
}

