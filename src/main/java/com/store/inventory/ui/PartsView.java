package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.Part;
import com.store.inventory.model.User;
import com.store.inventory.util.AlertHelper;
import java.time.LocalDate;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.*;

/**
 * Displays the Part inventory. Employees get Add / Edit / Delete controls.
 * Customers can browse and search only.
 */
public class PartsView {

    private final User user;
    private final VBox root;
    private final ObservableList<Part> parts = FXCollections.observableArrayList();
    private TableView<Part> table;
    private int customerID = -1;

    public PartsView(User user) {
        this.user = user;
        if (!user.isEmployee()) customerID = lookupCustomerID(user.getUserID());
        this.root = buildUI();
        loadParts("");
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private VBox buildUI() {
        // --- Search bar ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or category…");
        searchField.setPrefWidth(280);

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> loadParts(searchField.getText().trim()));

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> { searchField.clear(); loadParts(""); });

        HBox searchBar = new HBox(8, new Label("🔍"), searchField, searchBtn, clearBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // --- Table ---
        table = new TableView<>(parts);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Part, Integer> idCol   = col("ID",       "partID",   80);
        TableColumn<Part, String>  nameCol = col("Part Name","partName", 220);
        TableColumn<Part, String>  catCol  = col("Category", "category", 120);
        TableColumn<Part, Double>  priceCol= col("Price ($)", "price",   100);
        TableColumn<Part, Integer> qtyCol  = col("In Stock", "stockQty", 100);

        // Format price
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        // Color-code low stock
        qtyCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle(v <= 5 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "");
            }
        });

        table.getColumns().addAll(idCol, nameCol, catCol, priceCol, qtyCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // --- Buttons (employee only) ---
        HBox btnBar = new HBox(8);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        if (user.isEmployee()) {
            Button addBtn  = styledBtn("➕ Add Part",    "#27ae60");
            Button editBtn = styledBtn("✏ Edit Part",   "#2980b9");
            Button delBtn  = styledBtn("🗑 Delete Part", "#e74c3c");

            addBtn.setOnAction(e  -> showAddDialog());
            editBtn.setOnAction(e -> {
                Part sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Please select a part first."); return; }
                showEditDialog(sel);
            });
            delBtn.setOnAction(e -> {
                Part sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Please select a part first."); return; }
                deletePart(sel);
            });

            btnBar.getChildren().addAll(addBtn, editBtn, delBtn);
        }

        if (!user.isEmployee()) {
            Button orderBtn = styledBtn("🛒 Order Part", "#8e44ad");
            orderBtn.setOnAction(e -> {
                Part sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Please select a part first."); return; }
                if (sel.getStockQty() <= 0) { AlertHelper.info("Out of Stock", "Sorry, this part is currently out of stock."); return; }
                showOrderDialog(sel);
            });
            btnBar.getChildren().add(orderBtn);
        }

        Label countLabel = new Label();
        parts.addListener((ListChangeListener<Part>) c ->
                countLabel.setText("Total records: " + parts.size()));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        btnBar.getChildren().addAll(sp, countLabel);

        VBox root = new VBox(10, searchBar, table, btnBar);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ecf0f1;");
        return root;
    }

    // -------------------------------------------------------------------------
    // DB operations
    // -------------------------------------------------------------------------
    private void loadParts(String filter) {
        parts.clear();
        String sql = filter.isEmpty()
            ? "SELECT * FROM Part ORDER BY category, partName"
            : "SELECT * FROM Part WHERE partName LIKE ? OR category LIKE ? ORDER BY category, partName";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            if (!filter.isEmpty()) {
                String like = "%" + filter + "%";
                ps.setString(1, like); ps.setString(2, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                parts.add(new Part(
                    rs.getInt("partID"),
                    rs.getString("partName"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stockQty")
                ));
            }
        } catch (SQLException e) {
            AlertHelper.error("DB Error", e.getMessage());
        }
    }

    private void insertPart(String name, String cat, double price, int qty) {
        String sql = "INSERT INTO Part(partName,category,price,stockQty) VALUES(?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, cat);
            ps.setDouble(3, price); ps.setInt(4, qty);
            ps.executeUpdate();
            loadParts("");
        } catch (SQLException e) { AlertHelper.error("Insert Error", e.getMessage()); }
    }

    private void updatePart(Part p, String name, String cat, double price, int qty) {
        String sql = "UPDATE Part SET partName=?,category=?,price=?,stockQty=? WHERE partID=?";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, cat);
            ps.setDouble(3, price); ps.setInt(4, qty);
            ps.setInt(5, p.getPartID());
            ps.executeUpdate();
            loadParts("");
        } catch (SQLException e) { AlertHelper.error("Update Error", e.getMessage()); }
    }

    private void deletePart(Part p) {
        if (!AlertHelper.confirm("Delete", "Delete \"" + p.getPartName() + "\"?")) return;
        String sql = "DELETE FROM Part WHERE partID=?";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, p.getPartID());
            ps.executeUpdate();
            parts.remove(p);
        } catch (SQLException e) { AlertHelper.error("Delete Error", e.getMessage()); }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------
    private void showAddDialog() {
        Dialog<ButtonType> dlg = baseDialog("Add New Part");
        GridPane grid = formGrid();

        TextField nameF  = new TextField();
        ComboBox<String> catF = categoryCombo();
        TextField priceF = new TextField("0.00");
        TextField qtyF   = new TextField("0");

        addRow(grid, 0, "Part Name:", nameF);
        addRow(grid, 1, "Category:",  catF);
        addRow(grid, 2, "Price ($):", priceF);
        addRow(grid, 3, "Stock Qty:", qtyF);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    insertPart(nameF.getText().trim(), catF.getValue(),
                               Double.parseDouble(priceF.getText()),
                               Integer.parseInt(qtyF.getText()));
                } catch (NumberFormatException ex) {
                    AlertHelper.error("Input Error", "Price and Qty must be numbers.");
                }
            }
        });
    }

    private void showEditDialog(Part p) {
        Dialog<ButtonType> dlg = baseDialog("Edit Part — " + p.getPartName());
        GridPane grid = formGrid();

        TextField nameF  = new TextField(p.getPartName());
        ComboBox<String> catF = categoryCombo();
        catF.setValue(p.getCategory());
        TextField priceF = new TextField(String.valueOf(p.getPrice()));
        TextField qtyF   = new TextField(String.valueOf(p.getStockQty()));

        addRow(grid, 0, "Part Name:", nameF);
        addRow(grid, 1, "Category:",  catF);
        addRow(grid, 2, "Price ($):", priceF);
        addRow(grid, 3, "Stock Qty:", qtyF);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    updatePart(p, nameF.getText().trim(), catF.getValue(),
                               Double.parseDouble(priceF.getText()),
                               Integer.parseInt(qtyF.getText()));
                } catch (NumberFormatException ex) {
                    AlertHelper.error("Input Error", "Price and Qty must be numbers.");
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private <T> TableColumn<Part, T> col(String title, String prop, double w) {
        TableColumn<Part, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private ComboBox<String> categoryCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("CPU","GPU","RAM","Storage","Motherboard","PSU","Case","Cooling","Other");
        cb.setValue("Other");
        return cb;
    }

    private Button styledBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                   "-fx-background-radius:5; -fx-padding: 6 14 6 14;");
        return b;
    }

    private Dialog<ButtonType> baseDialog(String title) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return d;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new Insets(16));
        return g;
    }

    private void addRow(GridPane g, int row, String label, javafx.scene.Node field) {
        g.add(new Label(label), 0, row);
        g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void showOrderDialog(Part part) {
        if (customerID == -1) {
            AlertHelper.error("Account Error", "No customer profile found for your account.");
            return;
        }

        Dialog<ButtonType> dlg = baseDialog("Order Part — " + part.getPartName());
        GridPane grid = formGrid();

        Label nameL  = new Label(part.getPartName());
        Label priceL = new Label(String.format("$%.2f each", part.getPrice()));
        Label stockL = new Label(part.getStockQty() + " in stock");
        stockL.setStyle("-fx-text-fill: #27ae60;");

        Spinner<Integer> qtySpinner = new Spinner<>(1, part.getStockQty(), 1);
        qtySpinner.setEditable(true);

        Label totalL = new Label(String.format("$%.2f", part.getPrice()));
        totalL.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        qtySpinner.valueProperty().addListener((obs, old, qty) ->
            totalL.setText(String.format("$%.2f", part.getPrice() * qty)));

        addRow(grid, 0, "Part:",      nameL);
        addRow(grid, 1, "Price:",     priceL);
        addRow(grid, 2, "Available:", stockL);
        addRow(grid, 3, "Quantity:",  qtySpinner);
        addRow(grid, 4, "Total:",     totalL);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            int qty = qtySpinner.getValue();
            double total = part.getPrice() * qty;
            String date = LocalDate.now().toString();

            try {
                // Insert the customer order
                String sql = "INSERT INTO CustomerOrder(customerID,itemType,itemID,itemName,quantity,totalPrice,date,status) VALUES(?,?,?,?,?,?,?,'Pending')";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setInt(1, customerID);
                    ps.setString(2, "Part");
                    ps.setInt(3, part.getPartID());
                    ps.setString(4, part.getPartName());
                    ps.setInt(5, qty);
                    ps.setDouble(6, total);
                    ps.setString(7, date);
                    ps.executeUpdate();
                }
                // Decrement stock
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                        .prepareStatement("UPDATE Part SET stockQty = stockQty - ? WHERE partID=?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, part.getPartID());
                    ps.executeUpdate();
                }
                loadParts("");
                AlertHelper.info("Order Placed! 🎉",
                    "Your order for " + qty + "x " + part.getPartName() +
                    " has been placed.\nTotal: $" + String.format("%.2f", total) +
                    "\n\nCheck 'My Orders' to track your order.");
            } catch (SQLException ex) {
                AlertHelper.error("Order Error", ex.getMessage());
            }
        });
    }

    private int lookupCustomerID(int userID) {
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("SELECT customerID FROM Customer WHERE userID=?")) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("customerID");
        } catch (SQLException ignored) {}
        return -1;
    }
}
