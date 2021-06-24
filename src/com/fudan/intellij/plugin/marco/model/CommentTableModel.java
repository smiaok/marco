package com.fudan.intellij.plugin.marco.model;

import javax.swing.table.DefaultTableModel;

public class CommentTableModel extends DefaultTableModel {

    public CommentTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }


}
