package com.store.inventory.ui;

import com.store.inventory.db.DatabaseManager;
import com.store.inventory.model.User;
import com.store.inventory.util.AlertHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.sql.*;

/**
 * Login screen. Validates credentials against the User table,
 * then opens the appropriate Dashboard for the role.
 */
public class LoginView {

    private final Stage stage;
    private final VBox root;

    public LoginView(Stage stage) {
        this.stage = stage;
        this.root  = buildUI();
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    private VBox buildUI() {
        // --- Title ---
        Text title = new Text("MOODYPC");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-fill: #2c3e50;");
        Text subtitle = new Text("Inventory Management System");
        subtitle.setStyle("-fx-font-size: 13px; -fx-fill: #7f8c8d;");

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // --- Form ---
        Label userLabel = new Label("Username");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(260);

        Label passLabel = new Label("Password");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(260);

        Button loginBtn = new Button("Login");
        loginBtn.setDefaultButton(true);
        loginBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                          "-fx-font-size: 14px; -fx-padding: 8 30 8 30; -fx-background-radius: 5;");
        loginBtn.setMaxWidth(260);

        Label hintLabel = new Label("Demo: admin / admin123  |  alice / pass2");
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");

        VBox form = new VBox(8,
                userLabel, usernameField,
                passLabel, passwordField,
                new Region(),
                loginBtn,
                hintLabel);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(10, 40, 10, 40));
        VBox.setMargin(loginBtn, new Insets(8, 0, 0, 0));

        // --- Root ---
        VBox root = new VBox(24, titleBox, form);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #ecf0f1;");

        // --- Action ---
        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                AlertHelper.error("Login Error", "Please enter both username and password.");
                return;
            }
            User user = authenticate(u, p);
            if (user == null) {
                AlertHelper.error("Login Failed", "Invalid username or password.");
            } else {
                openDashboard(user);
            }
        });

        return root;
    }

    // -------------------------------------------------------------------------
    private User authenticate(String username, String password) {
        String sql = "SELECT * FROM User WHERE username = ? AND password = ?";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("userID"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getString("role")
                );
            }
        } catch (SQLException e) {
            AlertHelper.error("Database Error", e.getMessage());
        }
        return null;
    }

    private void openDashboard(User user) {
        DashboardView dashboard = new DashboardView(user);
        Scene scene = new Scene(dashboard.getRoot(), 1000, 680);
        stage.setScene(scene);
        stage.setTitle("MOODYPC — " + user.getUsername() + " [" + user.getRole() + "]");
        stage.setResizable(true);
        stage.centerOnScreen();
    }
}
