package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.PurchaseOrder;
import com.store.inventory.model.User;
import com.store.inventory.util.AlertHelper;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDate;

/**
 * Employee-only view: create and track purchase orders sent to vendors.
 * When a PO is marked "Received", the part's stockQty is incremented.
 */
public class PurchaseOrdersView {

    private final User user;
    private final VBox root;
    private final ObservableList<PurchaseOrder> orders = FXCollections.observableArrayList();
    private TableView<PurchaseOrder> table;

    public PurchaseOrdersView(User user) {
        this.user = user;
        this.root = buildUI();
        loadOrders();
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private VBox buildUI() {
        // Filter
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All","Ordered","Shipped","Received","Cancelled");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> loadOrdersFiltered(statusFilter.getValue()));

        HBox filterBar = new HBox(8, new Label("Filter by Status:"), statusFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // Table
        table = new TableView<>(orders);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PurchaseOrder, Integer> poCol   = col("PO ID",      "poID",       60);
        TableColumn<PurchaseOrder, Integer> empCol  = col("Emp ID",     "employeeID", 70);
        TableColumn<PurchaseOrder, Integer> venCol  = col("Vendor ID",  "vendorID",   80);
        TableColumn<PurchaseOrder, Integer> partCol = col("Part ID",    "partID",     70);
        TableColumn<PurchaseOrder, Integer> qtyCol  = col("Quantity",   "quantity",   80);
        TableColumn<PurchaseOrder, String>  dateCol = col("Date",       "date",      110);
        TableColumn<PurchaseOrder, String>  statCol = col("Status",     "status",    110);

        statCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(switch (v) {
                    case "Received"  -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "Ordered"   -> "-fx-text-fill: #e67e22;";
                    case "Cancelled" -> "-fx-text-fill: #e74c3c;";
                    default          -> "-fx-text-fill: #2980b9;";
                });
            }
        });

        table.getColumns().addAll(poCol, empCol, venCol, partCol, qtyCol, dateCol, statCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Info label
        Label infoLabel = new Label("💡 Marking a PO as 'Received' will automatically increase the part's stock.");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        // Buttons
        Button addBtn    = btn("➕ New PO",         "#27ae60");
        Button updateBtn = btn("🔄 Update Status",  "#2980b9");
        Button delBtn    = btn("🗑 Delete",         "#e74c3c");

        addBtn.setOnAction(e    -> showAddDialog());
        updateBtn.setOnAction(e -> {
            PurchaseOrder sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertHelper.info("Select","Select a PO first."); return; }
            showUpdateStatusDialog(sel);
        });
        delBtn.setOnAction(e -> {
            PurchaseOrder sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertHelper.info("Select","Select a PO first."); return; }
            deletePO(sel);
        });

        HBox btnBar = new HBox(8, addBtn, updateBtn, delBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, filterBar, table, infoLabel, btnBar);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ecf0f1;");
        return root;
    }

    // -------------------------------------------------------------------------
    private void loadOrders() { loadOrdersFiltered("All"); }

    private void loadOrdersFiltered(String status) {
        orders.clear();
        boolean filtered = !"All".equals(status);
        String sql = "SELECT * FROM PurchaseOrder"
                   + (filtered ? " WHERE status=?" : "")
                   + " ORDER BY poID DESC";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            if (filtered) ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                orders.add(new PurchaseOrder(
                    rs.getInt("poID"), rs.getInt("employeeID"), rs.getInt("vendorID"),
                    rs.getInt("partID"), rs.getInt("quantity"),
                    rs.getString("date"), rs.getString("status")
                ));
            }
        } catch (SQLException e) { AlertHelper.error("DB Error", e.getMessage()); }
    }

    private void showAddDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Create Purchase Order");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));

        // Try to get the employee ID for the logged-in user
        int empID = lookupEmployeeID(user.getUserID());

        TextField empField  = new TextField(empID > 0 ? String.valueOf(empID) : "");
        TextField venField  = new TextField();  venField.setPromptText("e.g. 1");
        TextField partField = new TextField();  partField.setPromptText("Part ID");
        TextField qtyField  = new TextField("1");

        g.add(new Label("Employee ID:"), 0, 0); g.add(empField,  1, 0);
        g.add(new Label("Vendor ID:"),   0, 1); g.add(venField,  1, 1);
        g.add(new Label("Part ID:"),     0, 2); g.add(partField, 1, 2);
        g.add(new Label("Quantity:"),    0, 3); g.add(qtyField,  1, 3);

        // Show vendor & part lookup helpers
        Label hint = new Label("Vendor IDs — 1: TechSource  |  2: PartsWorld\n"
                             + "Part IDs — check the Parts tab for IDs.");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        g.add(hint, 0, 4, 2, 1);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                int eID = Integer.parseInt(empField.getText().trim());
                int vID = Integer.parseInt(venField.getText().trim());
                int pID = Integer.parseInt(partField.getText().trim());
                int qty = Integer.parseInt(qtyField.getText().trim());
                String date = LocalDate.now().toString();

                String sql = "INSERT INTO PurchaseOrder(employeeID,vendorID,partID,quantity,date,status) VALUES(?,?,?,?,?,'Ordered')";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setInt(1,eID); ps.setInt(2,vID); ps.setInt(3,pID);
                    ps.setInt(4,qty); ps.setString(5,date);
                    ps.executeUpdate();
                    loadOrders();
                }
            } catch (NumberFormatException ex) {
                AlertHelper.error("Input Error","All IDs and Quantity must be numbers.");
            } catch (SQLException ex) {
                AlertHelper.error("DB Error", ex.getMessage());
            }
        });
    }

    private void showUpdateStatusDialog(PurchaseOrder po) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Update PO #" + po.getPoID() + " Status");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Ordered","Shipped","Received","Cancelled");
        statusBox.setValue(po.getStatus());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.add(new Label("New Status:"), 0, 0);
        g.add(statusBox, 1, 0);

        if (!"Received".equals(po.getStatus())) {
            Label note = new Label("Setting to 'Received' will add " + po.getQuantity() + " unit(s) to Part #" + po.getPartID() + " stock.");
            note.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
            g.add(note, 0, 1, 2, 1);
        }

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String newStatus = statusBox.getValue();
            try {
                String sql = "UPDATE PurchaseOrder SET status=? WHERE poID=?";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setString(1, newStatus); ps.setInt(2, po.getPoID());
                    ps.executeUpdate();
                }
                // If marked received, increase stock
                if ("Received".equals(newStatus) && !"Received".equals(po.getStatus())) {
                    String upd = "UPDATE Part SET stockQty = stockQty + ? WHERE partID=?";
                    try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(upd)) {
                        ps.setInt(1, po.getQuantity()); ps.setInt(2, po.getPartID());
                        ps.executeUpdate();
                    }
                    AlertHelper.info("Stock Updated", "Part #" + po.getPartID()
                        + " stock increased by " + po.getQuantity() + " unit(s).");
                }
                loadOrders();
            } catch (SQLException ex) { AlertHelper.error("DB Error", ex.getMessage()); }
        });
    }

    private void deletePO(PurchaseOrder po) {
        if (!AlertHelper.confirm("Delete","Delete PO #" + po.getPoID() + "?")) return;
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("DELETE FROM PurchaseOrder WHERE poID=?")) {
            ps.setInt(1, po.getPoID());
            ps.executeUpdate();
            orders.remove(po);
        } catch (SQLException e) { AlertHelper.error("Delete Error", e.getMessage()); }
    }

    private int lookupEmployeeID(int userID) {
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("SELECT employeeID FROM Employee WHERE userID=?")) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("employeeID");
        } catch (SQLException ignored) {}
        return -1;
    }

    private <T> TableColumn<PurchaseOrder, T> col(String title, String prop, double w) {
        TableColumn<PurchaseOrder, T> c = new TableColumn<>(title);
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
