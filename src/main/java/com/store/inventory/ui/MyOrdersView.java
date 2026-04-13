package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.CustomerOrder;
import com.store.inventory.model.User;
import com.store.inventory.util.AlertHelper;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.sql.*;

/**
 * Shows a customer their full order history and a running total.
 * Employees see ALL customer orders with the ability to update status.
 */
public class MyOrdersView {

    private final User user;
    private final VBox root;
    private final ObservableList<CustomerOrder> orders = FXCollections.observableArrayList();
    private TableView<CustomerOrder> table;
    private int customerID = -1;

    public MyOrdersView(User user) {
        this.user = user;
        if (!user.isEmployee()) customerID = lookupCustomerID(user.getUserID());
        this.root = buildUI();
        loadOrders();
    }

    public Parent getRoot() { return root; }

    // refresh hook called from DashboardView when tab is selected
    public void refresh() { loadOrders(); }

    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private VBox buildUI() {

        // --- Header ---
        Text heading = new Text(user.isEmployee() ? "All Customer Orders" : "My Orders");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #2c3e50;");

        // --- Status filter ---
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "Pending", "Processing", "Shipped", "Delivered", "Cancelled");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> loadOrdersFiltered(statusFilter.getValue()));

        HBox topBar = new HBox(16, heading, new Region(),
                new Label("Filter:"), statusFilter);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // --- Table ---
        table = new TableView<>(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No orders yet. Browse Parts or Products and click '🛒 Order'!"));

        TableColumn<CustomerOrder, Integer> idCol    = col("Order #",    "orderID",    70);
        TableColumn<CustomerOrder, String>  typeCol  = col("Type",       "itemType",   80);
        TableColumn<CustomerOrder, String>  nameCol  = col("Item",       "itemName",  200);
        TableColumn<CustomerOrder, Integer> qtyCol   = col("Qty",        "quantity",   60);
        TableColumn<CustomerOrder, Double>  priceCol = col("Total ($)",  "totalPrice", 100);
        TableColumn<CustomerOrder, String>  dateCol  = col("Date",       "date",      110);
        TableColumn<CustomerOrder, String>  statCol  = col("Status",     "status",    110);

        if (user.isEmployee()) {
            TableColumn<CustomerOrder, Integer> custCol = col("Cust ID", "customerID", 80);
            table.getColumns().add(custCol);
        }

        priceCol.setCellFactory(tc -> new TableCell<>() {
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
                    case "Delivered"  -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "Pending"    -> "-fx-text-fill: #e67e22; -fx-font-weight: bold;";
                    case "Cancelled"  -> "-fx-text-fill: #e74c3c;";
                    case "Shipped"    -> "-fx-text-fill: #2980b9;";
                    default           -> "-fx-text-fill: #8e44ad;";
                });
            }
        });

        table.getColumns().addAll(idCol, typeCol, nameCol, qtyCol, priceCol, dateCol, statCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // --- Summary bar ---
        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        orders.addListener((ListChangeListener<CustomerOrder>) c -> updateTotal(totalLabel));

        // --- Bottom bar ---
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        // Customers can cancel a pending order
        if (!user.isEmployee()) {
            Button cancelBtn = new Button("❌ Cancel Order");
            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                               "-fx-background-radius: 5; -fx-padding: 6 14 6 14;");
            cancelBtn.setOnAction(e -> {
                CustomerOrder sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Select an order first."); return; }
                if (!"Pending".equals(sel.getStatus())) {
                    AlertHelper.info("Cannot Cancel", "Only Pending orders can be cancelled."); return;
                }
                if (AlertHelper.confirm("Cancel Order", "Cancel order for \"" + sel.getItemName() + "\"?")) {
                    updateStatus(sel, "Cancelled");
                }
            });
            bottomBar.getChildren().add(cancelBtn);
        }

        // Employees can update any order's status
        if (user.isEmployee()) {
            Button updateBtn = new Button("🔄 Update Status");
            updateBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                               "-fx-background-radius: 5; -fx-padding: 6 14 6 14;");
            updateBtn.setOnAction(e -> {
                CustomerOrder sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Select an order first."); return; }
                showStatusDialog(sel);
            });
            bottomBar.getChildren().add(updateBtn);
        }

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        bottomBar.getChildren().addAll(sp, totalLabel);

        VBox root = new VBox(12, topBar, table, bottomBar);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ecf0f1;");
        return root;
    }

    // -------------------------------------------------------------------------
    private void loadOrders() { loadOrdersFiltered("All"); }

    private void loadOrdersFiltered(String status) {
        orders.clear();
        boolean byStatus = !"All".equals(status);

        StringBuilder sql = new StringBuilder("SELECT * FROM CustomerOrder WHERE 1=1");
        if (!user.isEmployee()) sql.append(" AND customerID=?");
        if (byStatus)           sql.append(" AND status=?");
        sql.append(" ORDER BY orderID DESC");

        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            if (!user.isEmployee()) ps.setInt(idx++, customerID);
            if (byStatus)           ps.setString(idx, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orders.add(new CustomerOrder(
                    rs.getInt("orderID"),    rs.getInt("customerID"),
                    rs.getString("itemType"), rs.getInt("itemID"),
                    rs.getString("itemName"), rs.getInt("quantity"),
                    rs.getDouble("totalPrice"), rs.getString("date"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) { AlertHelper.error("DB Error", e.getMessage()); }
    }

    private void updateStatus(CustomerOrder order, String newStatus) {
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("UPDATE CustomerOrder SET status=? WHERE orderID=?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, order.getOrderID());
            ps.executeUpdate();
            loadOrders();
        } catch (SQLException e) { AlertHelper.error("DB Error", e.getMessage()); }
    }

    private void showStatusDialog(CustomerOrder order) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Update Order #" + order.getOrderID());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Pending", "Processing", "Shipped", "Delivered", "Cancelled");
        statusBox.setValue(order.getStatus());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.add(new Label("Item:"),       0, 0);
        g.add(new Label(order.getItemName()), 1, 0);
        g.add(new Label("New Status:"), 0, 1);
        g.add(statusBox, 1, 1);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) updateStatus(order, statusBox.getValue());
        });
    }

    private void updateTotal(Label label) {
        double total = orders.stream()
                .filter(o -> !"Cancelled".equals(o.getStatus()))
                .mapToDouble(CustomerOrder::getTotalPrice)
                .sum();
        long count = orders.stream()
                .filter(o -> !"Cancelled".equals(o.getStatus()))
                .count();
        label.setText(String.format("Active Orders: %d  |  Total Spent: $%.2f", count, total));
    }

    // -------------------------------------------------------------------------
    private int lookupCustomerID(int userID) {
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("SELECT customerID FROM Customer WHERE userID=?")) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("customerID");
        } catch (SQLException ignored) {}
        return -1;
    }

    private <T> TableColumn<CustomerOrder, T> col(String title, String prop, double w) {
        TableColumn<CustomerOrder, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
}
