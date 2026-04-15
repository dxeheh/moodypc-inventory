package com.store.inventory.ui;

import com.store.inventory.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Main dashboard shown after login. Uses a TabPane to host the major views.
 * Employees see all tabs; customers see Parts, Products, and My Orders.
 */
public class DashboardView {

    private final User user;
    private final BorderPane root;

    public DashboardView(User user) {
        this.user = user;
        this.root = buildUI();
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-background-color: #2c3e50;");

        // --- Top bar ---
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10 20 10 20;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Text appTitle = new Text("🖥  MOODYPC Inventory");
        appTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + user.getUsername() + "  |  " + user.getRole().toUpperCase());
        userLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;");

        Button signOutBtn = new Button("Sign Out");
        signOutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 5 12 5 12; -fx-background-radius: 4;");
        signOutBtn.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) bp.getScene().getWindow();
            LoginView loginView = new LoginView(stage);
            javafx.scene.Scene scene = new javafx.scene.Scene(loginView.getRoot(), 420, 340);
            stage.setScene(scene);
            stage.setTitle("MOODYPC — Inventory System");
            stage.setResizable(false);
            stage.centerOnScreen();
        });

        topBar.getChildren().addAll(appTitle, spacer, userLabel, new javafx.scene.layout.Region() {{
            setMinWidth(12);
        }}, signOutBtn);

        // --- Tab Pane ---
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #ecf0f1;");

        // Parts/Components tab (everyone)
        Tab partsTab = new Tab("🔩  Parts / Components");
        partsTab.setContent(new PartsView(user).getRoot());

        // Products tab (PCs & Laptops) — everyone
        Tab productsTab = new Tab("💻  Products (PCs & Laptops)");
        productsTab.setContent(new ProductsView(user).getRoot());

        // My Orders tab — customers see their own orders, employees see all
        MyOrdersView myOrdersView = new MyOrdersView(user);
        Tab ordersLabel = user.isEmployee()
                ? new Tab("🛒  All Customer Orders")
                : new Tab("🛒  My Orders");
        ordersLabel.setContent(myOrdersView.getRoot());
        // Refresh the orders table every time the tab is clicked
        ordersLabel.setOnSelectionChanged(e -> {
            if (ordersLabel.isSelected()) myOrdersView.refresh();
        });

        tabPane.getTabs().addAll(partsTab, productsTab, ordersLabel);

        // Employee-only tabs
        if (user.isEmployee()) {
            Tab serviceTab = new Tab("🛠  Service Orders");
            serviceTab.setContent(new ServiceOrdersView(user).getRoot());

            Tab poTab = new Tab("📦  Purchase Orders");
            poTab.setContent(new PurchaseOrdersView(user).getRoot());

            tabPane.getTabs().addAll(serviceTab, poTab);
        }

        // Wrap TabPane in a light container
        StackPane center = new StackPane(tabPane);
        center.setStyle("-fx-background-color: #ecf0f1;");
        center.setPadding(new Insets(0));

        bp.setTop(topBar);
        bp.setCenter(center);
        return bp;
    }
}
