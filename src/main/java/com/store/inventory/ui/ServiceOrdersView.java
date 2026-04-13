package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.ServiceOrder;
import com.store.inventory.model.User;
import com.store.inventory.util.AlertHelper;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.*;

/**
 * Employee-only view for Service Orders (repair jobs, consultations, etc.)
 */
public class ServiceOrdersView {

    private final User user;
    private final VBox root;
    private final ObservableList<ServiceOrder> orders = FXCollections.observableArrayList();
    private TableView<ServiceOrder> table;

    public ServiceOrdersView(User user) {
        this.user = user;
        this.root = buildUI();
        loadOrders();
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private VBox buildUI() {
        // Status filter
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All","Pending","In Progress","Completed","Cancelled");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> loadOrdersFiltered(statusFilter.getValue()));

        HBox filterBar = new HBox(8, new Label("Filter by Status:"), statusFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Table
        table = new TableView<>(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ServiceOrder, Integer> idCol    = col("Order ID",    "orderID",    80);
        TableColumn<ServiceOrder, Integer> custCol  = col("Customer ID", "customerID", 100);
        TableColumn<ServiceOrder, String>  typeCol  = col("Type",        "orderType",  120);
        TableColumn<ServiceOrder, String>  statCol  = col("Status",      "status",     120);
        TableColumn<ServiceOrder, Double>  costCol  = col("Total Cost",  "totalCost",  110);

        costCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        statCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(switch (v) {
                    case "Completed" -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "Pending"   -> "-fx-text-fill: #e67e22; -fx-font-weight: bold;";
                    case "Cancelled" -> "-fx-text-fill: #e74c3c;";
                    default          -> "-fx-text-fill: #2980b9;";
                });
            }
        });

        table.getColumns().addAll(idCol, custCol, typeCol, statCol, costCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Details panel (right side)
        TextArea detailArea = new TextArea("Select an order to see details.");
        detailArea.setEditable(false);
        detailArea.setPrefWidth(260);
        detailArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                detailArea.setText(
                    "Order ID   : " + sel.getOrderID()    + "\n" +
                    "Customer ID: " + sel.getCustomerID() + "\n" +
                    "Type       : " + sel.getOrderType()  + "\n" +
                    "Status     : " + sel.getStatus()     + "\n" +
                    "Total Cost : $" + String.format("%.2f", sel.getTotalCost())
                );
            }
        });

        HBox mainContent = new HBox(10, table, detailArea);
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Buttons
        Button addBtn    = btn("➕ New Order",         "#27ae60");
        Button updateBtn = btn("🔄 Update Status",     "#2980b9");
        Button delBtn    = btn("🗑 Delete",            "#e74c3c");

        addBtn.setOnAction(e    -> showAddDialog());
        updateBtn.setOnAction(e -> {
            ServiceOrder sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertHelper.info("Select","Select an order first."); return; }
            showUpdateStatusDialog(sel);
        });
        delBtn.setOnAction(e -> {
            ServiceOrder sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertHelper.info("Select","Select an order first."); return; }
            deleteOrder(sel);
        });

        HBox btnBar = new HBox(8, addBtn, updateBtn, delBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, filterBar, mainContent, btnBar);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ecf0f1;");
        return root;
    }

    // -------------------------------------------------------------------------
    private void loadOrders() { loadOrdersFiltered("All"); }

    private void loadOrdersFiltered(String status) {
        orders.clear();
        boolean filtered = !"All".equals(status);
        String sql = "SELECT * FROM ServiceOrder" + (filtered ? " WHERE status=?" : "") + " ORDER BY orderID DESC";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            if (filtered) ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orders.add(new ServiceOrder(
                    rs.getInt("orderID"), rs.getInt("customerID"),
                    rs.getString("orderType"), rs.getString("status"),
                    rs.getDouble("totalCost")
                ));
            }
        } catch (SQLException e) { AlertHelper.error("DB Error", e.getMessage()); }
    }

    private void showAddDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("New Service Order");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));

        TextField custIDField = new TextField();
        custIDField.setPromptText("Customer ID");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Repair","Upgrade","Consultation","Diagnostics","Build");
        typeBox.setValue("Repair");
        TextField costField = new TextField("0.00");

        g.add(new Label("Customer ID:"), 0, 0); g.add(custIDField, 1, 0);
        g.add(new Label("Order Type:"),  0, 1); g.add(typeBox,     1, 1);
        g.add(new Label("Total Cost:"),  0, 2); g.add(costField,   1, 2);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                int custID = Integer.parseInt(custIDField.getText().trim());
                double cost = Double.parseDouble(costField.getText().trim());
                String sql = "INSERT INTO ServiceOrder(customerID,orderType,status,totalCost) VALUES(?,?,'Pending',?)";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setInt(1, custID); ps.setString(2, typeBox.getValue()); ps.setDouble(3, cost);
                    ps.executeUpdate();
                    loadOrders();
                }
            } catch (NumberFormatException ex) {
                AlertHelper.error("Input Error","Customer ID and Cost must be numbers.");
            } catch (SQLException ex) {
                AlertHelper.error("DB Error", ex.getMessage());
            }
        });
    }

    private void showUpdateStatusDialog(ServiceOrder so) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Update Status — Order #" + so.getOrderID());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Pending","In Progress","Completed","Cancelled");
        statusBox.setValue(so.getStatus());

        TextField costField = new TextField(String.valueOf(so.getTotalCost()));

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.add(new Label("New Status:"),   0, 0); g.add(statusBox, 1, 0);
        g.add(new Label("Total Cost ($:"),0, 1); g.add(costField, 1, 1);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                String sql = "UPDATE ServiceOrder SET status=?, totalCost=? WHERE orderID=?";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setString(1, statusBox.getValue());
                    ps.setDouble(2, Double.parseDouble(costField.getText().trim()));
                    ps.setInt(3, so.getOrderID());
                    ps.executeUpdate();
                    loadOrders();
                }
            } catch (NumberFormatException | SQLException ex) {
                AlertHelper.error("Error", ex.getMessage());
            }
        });
    }

    private void deleteOrder(ServiceOrder so) {
        if (!AlertHelper.confirm("Delete","Delete Order #" + so.getOrderID() + "?")) return;
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("DELETE FROM ServiceOrder WHERE orderID=?")) {
            ps.setInt(1, so.getOrderID());
            ps.executeUpdate();
            orders.remove(so);
        } catch (SQLException e) { AlertHelper.error("Delete Error", e.getMessage()); }
    }

    private <T> TableColumn<ServiceOrder, T> col(String title, String prop, double w) {
        TableColumn<ServiceOrder, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:"+color+"; -fx-text-fill:white; " +
                   "-fx-background-radius:5; -fx-padding:6 14 6 14;");
        return b;
    }
}
