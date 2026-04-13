package com.store.inventory.db;

import java.sql.*;

/**
 * Singleton that manages the SQLite connection and DDL.
 * The database file (store.db) is created in the working directory.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:store.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot open database: " + e.getMessage(), e);
        }
        return connection;
    }

    /** Creates tables and inserts seed data if tables are empty. */
    public void initialize() {
        createTables();
        seedData();
    }

    // -------------------------------------------------------------------------
    // DDL
    // -------------------------------------------------------------------------
    private void createTables() {
        String[] ddl = {
            // User (login credentials + role)
            """
            CREATE TABLE IF NOT EXISTS User (
                userID    INTEGER PRIMARY KEY AUTOINCREMENT,
                username  TEXT NOT NULL UNIQUE,
                password  TEXT NOT NULL,
                email     TEXT,
                role      TEXT NOT NULL DEFAULT 'customer'
            )
            """,

            // Employee (extends User logically via userID FK)
            """
            CREATE TABLE IF NOT EXISTS Employee (
                employeeID INTEGER PRIMARY KEY AUTOINCREMENT,
                userID     INTEGER NOT NULL UNIQUE,
                role       TEXT NOT NULL DEFAULT 'technician',
                FOREIGN KEY (userID) REFERENCES User(userID)
            )
            """,

            // Customer (extends User logically via userID FK)
            """
            CREATE TABLE IF NOT EXISTS Customer (
                customerID INTEGER PRIMARY KEY AUTOINCREMENT,
                userID     INTEGER UNIQUE,
                address    TEXT,
                phone      TEXT,
                FOREIGN KEY (userID) REFERENCES User(userID)
            )
            """,

            // Vendor
            """
            CREATE TABLE IF NOT EXISTS Vendor (
                vendorID    INTEGER PRIMARY KEY AUTOINCREMENT,
                vendorName  TEXT NOT NULL,
                contactInfo TEXT
            )
            """,

            // Part (PC components: GPU, CPU, RAM, etc.)
            """
            CREATE TABLE IF NOT EXISTS Part (
                partID    INTEGER PRIMARY KEY AUTOINCREMENT,
                partName  TEXT NOT NULL,
                category  TEXT NOT NULL DEFAULT 'General',
                price     REAL NOT NULL DEFAULT 0.0,
                stockQty  INTEGER NOT NULL DEFAULT 0
            )
            """,

            // Product (complete PCs or Laptops for sale)
            """
            CREATE TABLE IF NOT EXISTS Product (
                productID   INTEGER PRIMARY KEY AUTOINCREMENT,
                productName TEXT NOT NULL,
                type        TEXT NOT NULL DEFAULT 'Desktop',
                price       REAL NOT NULL DEFAULT 0.0,
                stockQty    INTEGER NOT NULL DEFAULT 0,
                specs       TEXT
            )
            """,

            // ServiceOrder
            """
            CREATE TABLE IF NOT EXISTS ServiceOrder (
                orderID    INTEGER PRIMARY KEY AUTOINCREMENT,
                customerID INTEGER NOT NULL,
                orderType  TEXT NOT NULL DEFAULT 'Repair',
                status     TEXT NOT NULL DEFAULT 'Pending',
                totalCost  REAL DEFAULT 0.0,
                FOREIGN KEY (customerID) REFERENCES Customer(customerID)
            )
            """,

            // Appointment
            """
            CREATE TABLE IF NOT EXISTS Appointment (
                apptID      INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID     INTEGER,
                date        TEXT NOT NULL,
                status      TEXT NOT NULL DEFAULT 'Scheduled',
                confirmedBy INTEGER,
                FOREIGN KEY (orderID)     REFERENCES ServiceOrder(orderID),
                FOREIGN KEY (confirmedBy) REFERENCES Employee(employeeID)
            )
            """,

            // Payment
            """
            CREATE TABLE IF NOT EXISTS Payment (
                paymentID   INTEGER PRIMARY KEY AUTOINCREMENT,
                orderID     INTEGER,
                amount      REAL NOT NULL DEFAULT 0.0,
                paymentDate TEXT NOT NULL,
                FOREIGN KEY (orderID) REFERENCES ServiceOrder(orderID)
            )
            """,

            // PurchaseOrder (store ordering parts from vendors)
            """
            CREATE TABLE IF NOT EXISTS PurchaseOrder (
                poID       INTEGER PRIMARY KEY AUTOINCREMENT,
                employeeID INTEGER,
                vendorID   INTEGER,
                partID     INTEGER,
                quantity   INTEGER NOT NULL DEFAULT 1,
                date       TEXT NOT NULL,
                status     TEXT NOT NULL DEFAULT 'Ordered',
                FOREIGN KEY (employeeID) REFERENCES Employee(employeeID),
                FOREIGN KEY (vendorID)   REFERENCES Vendor(vendorID),
                FOREIGN KEY (partID)     REFERENCES Part(partID)
            )
            """,

            // CustomerOrder (customer purchases of parts or products)
            """
            CREATE TABLE IF NOT EXISTS CustomerOrder (
                orderID    INTEGER PRIMARY KEY AUTOINCREMENT,
                customerID INTEGER NOT NULL,
                itemType   TEXT NOT NULL,
                itemID     INTEGER NOT NULL,
                itemName   TEXT NOT NULL,
                quantity   INTEGER NOT NULL DEFAULT 1,
                totalPrice REAL NOT NULL DEFAULT 0.0,
                date       TEXT NOT NULL,
                status     TEXT NOT NULL DEFAULT 'Pending'
            )
            """
        };

        try (Statement stmt = getConnection().createStatement()) {
            for (String sql : ddl) stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Schema creation failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Seed Data
    // -------------------------------------------------------------------------
    private void seedData() {
        try (Statement stmt = getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM User");
            if (rs.getInt(1) > 0) return; // already seeded
        } catch (SQLException e) {
            return;
        }

        String[] seeds = {
            // Users: admin employee + two sample customers
            "INSERT INTO User(username,password,email,role) VALUES('admin','admin123','admin@store.com','employee')",
            "INSERT INTO User(username,password,email,role) VALUES('jsmith','pass1','john@email.com','employee')",
            "INSERT INTO User(username,password,email,role) VALUES('alice','pass2','alice@email.com','customer')",
            "INSERT INTO User(username,password,email,role) VALUES('bob','pass3','bob@email.com','customer')",

            // Employees (userID 1 = admin, 2 = jsmith)
            "INSERT INTO Employee(userID,role) VALUES(1,'Manager')",
            "INSERT INTO Employee(userID,role) VALUES(2,'Technician')",

            // Customers (userID 3 = alice, 4 = bob)
            "INSERT INTO Customer(userID,address,phone) VALUES(3,'123 Elm St','555-1001')",
            "INSERT INTO Customer(userID,address,phone) VALUES(4,'456 Oak Ave','555-1002')",

            // Vendors
            "INSERT INTO Vendor(vendorName,contactInfo) VALUES('TechSource Inc','contact@techsource.com')",
            "INSERT INTO Vendor(vendorName,contactInfo) VALUES('PartsWorld','sales@partsworld.com')",

            // Parts
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('Intel Core i9-14900K','CPU',589.99,15)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('AMD Ryzen 9 7950X','CPU',549.99,10)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('NVIDIA RTX 4090','GPU',1599.99,5)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('AMD RX 7900 XTX','GPU',899.99,8)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('Corsair 32GB DDR5-6000','RAM',139.99,30)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('Samsung 2TB NVMe SSD','Storage',159.99,25)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('WD 4TB HDD','Storage',89.99,20)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('ASUS ROG Strix Z790-E','Motherboard',449.99,12)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('Corsair RM1000x PSU','PSU',189.99,18)",
            "INSERT INTO Part(partName,category,price,stockQty) VALUES('Lian Li O11 Dynamic','Case',149.99,14)",

            // Products
            "INSERT INTO Product(productName,type,price,stockQty,specs) VALUES('ProBuild X1','Desktop',2499.99,4,'i9-14900K | RTX 4090 | 32GB DDR5 | 2TB NVMe')",
            "INSERT INTO Product(productName,type,price,stockQty,specs) VALUES('ValueBuild V3','Desktop',899.99,7,'Ryzen 5 7600 | RX 6700 | 16GB DDR5 | 1TB NVMe')",
            "INSERT INTO Product(productName,type,price,stockQty,specs) VALUES('StoreBook Pro 15','Laptop',1299.99,6,'Intel Core Ultra 7 | Intel Arc | 16GB LPDDR5 | 512GB NVMe')",
            "INSERT INTO Product(productName,type,price,stockQty,specs) VALUES('StoreBook Air 14','Laptop',749.99,10,'AMD Ryzen 7 8840U | Radeon 780M | 16GB | 512GB NVMe')",

            // A sample service order and appointment
            "INSERT INTO ServiceOrder(customerID,orderType,status,totalCost) VALUES(1,'Repair','Pending',150.00)",
            "INSERT INTO Appointment(orderID,date,status,confirmedBy) VALUES(1,'2025-06-15','Scheduled',1)",

            // A sample purchase order
            "INSERT INTO PurchaseOrder(employeeID,vendorID,partID,quantity,date,status) VALUES(1,1,3,2,'2025-05-01','Ordered')"
        };

        try (Statement stmt = getConnection().createStatement()) {
            for (String sql : seeds) stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Seed warning: " + e.getMessage());
        }
    }
}
