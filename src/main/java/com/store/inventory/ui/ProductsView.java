package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.Product;
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
 * Browse and manage complete PC / Laptop products.
 */
public class ProductsView {

    private final User user;
    private final VBox root;
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private TableView<Product> table;
    private int customerID = -1;

    public ProductsView(User user) {
        this.user = user;
        if (!user.isEmployee()) customerID = lookupCustomerID(user.getUserID());
        this.root = buildUI();
        loadProducts("");
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private VBox buildUI() {
        // Search
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or type…");
        searchField.setPrefWidth(280);
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> loadProducts(searchField.getText().trim()));
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> { searchField.clear(); loadProducts(""); });

        // Type filter
        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All", "Desktop", "Laptop");
        typeFilter.setValue("All");
        typeFilter.setOnAction(e -> {
            String t = typeFilter.getValue();
            loadProductsFiltered("All".equals(t) ? "" : t, searchField.getText().trim());
        });

        HBox searchBar = new HBox(8, new Label("🔍"), searchField, searchBtn, clearBtn,
                                  new Separator(javafx.geometry.Orientation.VERTICAL),
                                  new Label("Type:"), typeFilter);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // Table
        table = new TableView<>(products);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Product, Integer> idCol   = col("ID",          "productID",   60);
        TableColumn<Product, String>  nameCol = col("Product Name","productName", 200);
        TableColumn<Product, String>  typeCol = col("Type",        "type",         90);
        TableColumn<Product, Double>  prCol   = col("Price ($)",   "price",       100);
        TableColumn<Product, Integer> qtyCol  = col("Stock",       "stockQty",     70);
        TableColumn<Product, String>  specCol = col("Specs",       "specs",       300);

        prCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });
        qtyCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle(v <= 2 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "");
            }
        });

        table.getColumns().addAll(idCol, nameCol, typeCol, prCol, qtyCol, specCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Buttons
        HBox btnBar = new HBox(8);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        if (user.isEmployee()) {
            Button addBtn  = btn("➕ Add Product",    "#27ae60");
            Button editBtn = btn("✏ Edit",           "#2980b9");
            Button delBtn  = btn("🗑 Delete",         "#e74c3c");

            addBtn.setOnAction(e  -> showDialog(null));
            editBtn.setOnAction(e -> {
                Product sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Select a product first."); return; }
                showDialog(sel);
            });
            delBtn.setOnAction(e -> {
                Product sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Select a product first."); return; }
                deleteProduct(sel);
            });
            btnBar.getChildren().addAll(addBtn, editBtn, delBtn);
        }

        if (!user.isEmployee()) {
            Button orderBtn = btn("🛒 Order Product", "#8e44ad");
            orderBtn.setOnAction(e -> {
                Product sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { AlertHelper.info("Select", "Select a product first."); return; }
                if (sel.getStockQty() <= 0) { AlertHelper.info("Out of Stock", "Sorry, this product is currently out of stock."); return; }
                showOrderDialog(sel);
            });
            btnBar.getChildren().add(orderBtn);
        }

        Label countLabel = new Label();
        products.addListener((ListChangeListener<Product>) c ->
                countLabel.setText("Total records: " + products.size()));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        btnBar.getChildren().addAll(sp, countLabel);

        VBox root = new VBox(10, searchBar, table, btnBar);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ecf0f1;");
        return root;
    }

    // -------------------------------------------------------------------------
    private void loadProducts(String nameFilter) {
        loadProductsFiltered("", nameFilter);
    }

    private void loadProductsFiltered(String type, String name) {
        products.clear();
        boolean hasType = !type.isEmpty();
        boolean hasName = !name.isEmpty();

        StringBuilder sql = new StringBuilder("SELECT * FROM Product WHERE 1=1");
        if (hasType) sql.append(" AND type=?");
        if (hasName) sql.append(" AND productName LIKE ?");
        sql.append(" ORDER BY type, productName");

        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasType) ps.setString(idx++, type);
            if (hasName) ps.setString(idx, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("productID"),
                    rs.getString("productName"),
                    rs.getString("type"),
                    rs.getDouble("price"),
                    rs.getInt("stockQty"),
                    rs.getString("specs")
                ));
            }
        } catch (SQLException e) { AlertHelper.error("DB Error", e.getMessage()); }
    }

    private void showDialog(Product existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(isEdit ? "Edit Product" : "Add Product");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));

        TextField nameF  = new TextField(isEdit ? existing.getProductName() : "");
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("Desktop","Laptop");
        typeF.setValue(isEdit ? existing.getType() : "Desktop");
        TextField priceF = new TextField(isEdit ? String.valueOf(existing.getPrice()) : "0.00");
        TextField qtyF   = new TextField(isEdit ? String.valueOf(existing.getStockQty()) : "0");
        TextArea  specsF = new TextArea(isEdit ? existing.getSpecs() : "");
        specsF.setPrefRowCount(3);

        g.add(new Label("Name:"),     0, 0); g.add(nameF,  1, 0);
        g.add(new Label("Type:"),     0, 1); g.add(typeF,  1, 1);
        g.add(new Label("Price ($):"),0, 2); g.add(priceF, 1, 2);
        g.add(new Label("Stock:"),    0, 3); g.add(qtyF,   1, 3);
        g.add(new Label("Specs:"),    0, 4); g.add(specsF, 1, 4);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                String n = nameF.getText().trim();
                String t = typeF.getValue();
                double p = Double.parseDouble(priceF.getText());
                int    q = Integer.parseInt(qtyF.getText());
                String s = specsF.getText().trim();

                if (isEdit) {
                    String sql = "UPDATE Product SET productName=?,type=?,price=?,stockQty=?,specs=? WHERE productID=?";
                    try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                        ps.setString(1,n); ps.setString(2,t); ps.setDouble(3,p);
                        ps.setInt(4,q);    ps.setString(5,s); ps.setInt(6,existing.getProductID());
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO Product(productName,type,price,stockQty,specs) VALUES(?,?,?,?,?)";
                    try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                        ps.setString(1,n); ps.setString(2,t); ps.setDouble(3,p);
                        ps.setInt(4,q);    ps.setString(5,s);
                        ps.executeUpdate();
                    }
                }
                loadProducts("");
            } catch (NumberFormatException ex) {
                AlertHelper.error("Input Error","Price and Stock must be valid numbers.");
            } catch (SQLException ex) {
                AlertHelper.error("DB Error", ex.getMessage());
            }
        });
    }

    private void deleteProduct(Product p) {
        if (!AlertHelper.confirm("Delete","Delete \"" + p.getProductName() + "\"?")) return;
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                .prepareStatement("DELETE FROM Product WHERE productID=?")) {
            ps.setInt(1, p.getProductID());
            ps.executeUpdate();
            products.remove(p);
        } catch (SQLException e) { AlertHelper.error("Delete Error", e.getMessage()); }
    }

    private <T> TableColumn<Product, T> col(String t, String p, double w) {
        TableColumn<Product,T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(p));
        c.setPrefWidth(w);
        return c;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:"+color+"; -fx-text-fill:white; " +
                   "-fx-background-radius:5; -fx-padding:6 14 6 14;");
        return b;
    }

    private void showOrderDialog(Product product) {
        if (customerID == -1) {
            AlertHelper.error("Account Error", "No customer profile found for your account.");
            return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Order — " + product.getProductName());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));

        Label nameL  = new Label(product.getProductName());
        Label typeL  = new Label(product.getType());
        Label priceL = new Label(String.format("$%.2f each", product.getPrice()));
        Label stockL = new Label(product.getStockQty() + " in stock");
        stockL.setStyle("-fx-text-fill: #27ae60;");
        Label specsL = new Label(product.getSpecs());
        specsL.setWrapText(true); specsL.setMaxWidth(260);
        specsL.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Spinner<Integer> qtySpinner = new Spinner<>(1, product.getStockQty(), 1);
        qtySpinner.setEditable(true);

        Label totalL = new Label(String.format("$%.2f", product.getPrice()));
        totalL.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        qtySpinner.valueProperty().addListener((obs, old, qty) ->
            totalL.setText(String.format("$%.2f", product.getPrice() * qty)));

        g.add(new Label("Product:"),  0, 0); g.add(nameL,      1, 0);
        g.add(new Label("Type:"),     0, 1); g.add(typeL,      1, 1);
        g.add(new Label("Specs:"),    0, 2); g.add(specsL,     1, 2);
        g.add(new Label("Price:"),    0, 3); g.add(priceL,     1, 3);
        g.add(new Label("In Stock:"), 0, 4); g.add(stockL,     1, 4);
        g.add(new Label("Quantity:"), 0, 5); g.add(qtySpinner, 1, 5);
        g.add(new Label("Total:"),    0, 6); g.add(totalL,     1, 6);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            int qty = qtySpinner.getValue();
            double total = product.getPrice() * qty;
            String date = LocalDate.now().toString();

            try {
                String sql = "INSERT INTO CustomerOrder(customerID,itemType,itemID,itemName,quantity,totalPrice,date,status) VALUES(?,?,?,?,?,?,?,'Pending')";
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setInt(1, customerID);
                    ps.setString(2, "Product");
                    ps.setInt(3, product.getProductID());
                    ps.setString(4, product.getProductName());
                    ps.setInt(5, qty);
                    ps.setDouble(6, total);
                    ps.setString(7, date);
                    ps.executeUpdate();
                }
                // Decrement stock
                try (PreparedStatement ps = DatabaseManager.getInstance().getConnection()
                        .prepareStatement("UPDATE Product SET stockQty = stockQty - ? WHERE productID=?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, product.getProductID());
                    ps.executeUpdate();
                }
                loadProducts("");
                AlertHelper.info("Order Placed! 🎉",
                    "Your order for " + qty + "x " + product.getProductName() +
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
