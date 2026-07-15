package com.utopia.dms;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing entry point for the UTOPIA Order Management System - Phase 3.
 * Replaces the console UI: every CRUD operation and the custom action are
 * reachable only through this window's buttons and dialogs. Still no
 * database in this phase - all data lives in the OrderService instance
 * held by this frame.
 */
public class UtopiaOrderGui extends JFrame {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderService orderService;
    private final OrderTableModel tableModel;
    private final JTable table;
    private final JTextField filePathField;

    public UtopiaOrderGui(OrderService orderService) {
        super("UTOPIA Order Management System");
        this.orderService = orderService;
        this.tableModel = new OrderTableModel();
        this.table = new JTable(tableModel);
        this.filePathField = new JTextField();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        setSize(1000, 550);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UtopiaOrderGui(new OrderService()).setVisible(true));
    }

    private JPanel buildTopPanel() {
        JLabel title = new JLabel("UTOPIA Order Management System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.add(new JLabel("Data File Path:"), BorderLayout.WEST);
        filePanel.add(filePathField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseForFile());

        JButton loadButton = new JButton("Load Data");
        loadButton.addActionListener(e -> handleLoad());

        JPanel fileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        fileButtons.add(browseButton);
        fileButtons.add(loadButton);
        filePanel.add(fileButtons, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(filePanel, BorderLayout.SOUTH);
        return topPanel;
    }

    private JPanel buildTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildButtonPanel() {
        JButton createButton = new JButton("Create Order...");
        createButton.addActionListener(e -> handleCreate());

        JButton updateButton = new JButton("Update Selected...");
        updateButton.addActionListener(e -> handleUpdate());

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> handleRemove());

        JButton completeButton = new JButton("Complete Selected (Custom Action)");
        completeButton.addActionListener(e -> handleComplete());

        JButton refreshButton = new JButton("Refresh Display");
        refreshButton.addActionListener(e -> refreshTable());

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        panel.add(createButton);
        panel.add(updateButton);
        panel.add(removeButton);
        panel.add(completeButton);
        panel.add(refreshButton);
        panel.add(exitButton);
        return panel;
    }

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void refreshTable() {
        tableModel.setOrders(orderService.getAllOrders());
    }

    private void handleLoad() {
        String path = filePathField.getText();
        int count = orderService.loadOrdersFromFile(path);
        if (count < 0) {
            JOptionPane.showMessageDialog(this,
                    "Could not read a file at \"" + path + "\". Check the path and try again.",
                    "Load Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        JOptionPane.showMessageDialog(this, "Loaded " + count + " order(s) from file.",
                "Load Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleCreate() {
        JTextField nameField = new JTextField();
        JComboBox<String> itemCombo = new JComboBox<>(orderService.getMenuItemNames().toArray(new String[0]));
        JTextField quantityField = new JTextField();
        JComboBox<Integer> laneCombo = new JComboBox<>(new Integer[]{1, 2, 3});

        JPanel form = buildFormPanel(
                new String[]{"Customer Name:", "Item:", "Quantity:", "Pickup Lane:"},
                new JComponent[]{nameField, itemCombo, quantityField, laneCombo});

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, "Create New Order",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());
                String item = (String) itemCombo.getSelectedItem();
                int lane = (Integer) laneCombo.getSelectedItem();
                CustomerOrder created = orderService.createOrder(nameField.getText(), item, quantity, lane);
                refreshTable();
                JOptionPane.showMessageDialog(this, "Order #" + created.getOrderID() + " created.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity must be a whole number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (InvalidOrderDataException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUpdate() {
        CustomerOrder selected = getSelectedOrder("update");
        if (selected == null) {
            return;
        }
        if (!selected.isActive()) {
            JOptionPane.showMessageDialog(this, "Only pending orders can be edited.",
                    "Cannot Update", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = new JTextField(selected.getCustomerName());
        JComboBox<String> itemCombo = new JComboBox<>(orderService.getMenuItemNames().toArray(new String[0]));
        itemCombo.setSelectedItem(selected.getItemName());
        JTextField quantityField = new JTextField(String.valueOf(selected.getQuantity()));
        JComboBox<Integer> laneCombo = new JComboBox<>(new Integer[]{1, 2, 3});
        laneCombo.setSelectedItem(selected.getPickupLane());

        JPanel form = buildFormPanel(
                new String[]{"Customer Name:", "Item:", "Quantity:", "Pickup Lane:"},
                new JComponent[]{nameField, itemCombo, quantityField, laneCombo});

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, "Update Order #" + selected.getOrderID(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());
                int lane = (Integer) laneCombo.getSelectedItem();
                String item = (String) itemCombo.getSelectedItem();

                if (!nameField.getText().trim().equals(selected.getCustomerName())) {
                    orderService.updateOrder(selected.getOrderID(), "customerName", nameField.getText());
                }
                if (!item.equals(selected.getItemName())) {
                    orderService.updateOrder(selected.getOrderID(), "itemName", item);
                }
                if (quantity != selected.getQuantity()) {
                    orderService.updateOrder(selected.getOrderID(), "quantity", String.valueOf(quantity));
                }
                if (lane != selected.getPickupLane()) {
                    orderService.updateOrder(selected.getOrderID(), "pickupLane", String.valueOf(lane));
                }
                refreshTable();
                JOptionPane.showMessageDialog(this, "Order #" + selected.getOrderID() + " updated.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity must be a whole number.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (OrderNotFoundException | InvalidOrderDataException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Could Not Update", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleRemove() {
        CustomerOrder selected = getSelectedOrder("remove");
        if (selected == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove order #" + selected.getOrderID() + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            orderService.removeOrder(selected.getOrderID());
            refreshTable();
        } catch (OrderNotFoundException | InvalidOrderDataException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could Not Remove", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleComplete() {
        CustomerOrder selected = getSelectedOrder("complete");
        if (selected == null) {
            return;
        }
        try {
            long prepTimeMinutes = orderService.completeOrder(selected.getOrderID());
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Order #" + selected.getOrderID() + " marked Completed.\nPrep time: " + prepTimeMinutes + " minute(s).",
                    "Order Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (OrderNotFoundException | InvalidOrderDataException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could Not Complete", JOptionPane.ERROR_MESSAGE);
        }
    }

    private CustomerOrder getSelectedOrder(String actionVerb) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an order to " + actionVerb + ".",
                    "No Order Selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return tableModel.getOrderAt(row);
    }

    private JPanel buildFormPanel(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }
        return panel;
    }

    /** Backs the JTable with the current list of orders from OrderService. */
    private static class OrderTableModel extends AbstractTableModel {

        private static final String[] COLUMN_NAMES = {
                "Order ID", "Customer", "Item", "Qty", "Total", "Status", "Placed At", "Lane", "Completed At"
        };

        private List<CustomerOrder> orders = new ArrayList<>();

        void setOrders(List<CustomerOrder> orders) {
            this.orders = orders;
            fireTableDataChanged();
        }

        CustomerOrder getOrderAt(int row) {
            return orders.get(row);
        }

        @Override
        public int getRowCount() {
            return orders.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CustomerOrder order = orders.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return order.getOrderID();
                case 1:
                    return order.getCustomerName();
                case 2:
                    return order.getItemName();
                case 3:
                    return order.getQuantity();
                case 4:
                    return String.format("$%.2f", order.getOrderTotal());
                case 5:
                    return order.getOrderStatus();
                case 6:
                    return order.getOrderPlacedAt().format(DISPLAY_FORMAT);
                case 7:
                    return order.getPickupLane();
                case 8:
                    return order.getCompletedAt() == null ? "N/A" : order.getCompletedAt().format(DISPLAY_FORMAT);
                default:
                    return "";
            }
        }
    }
}
