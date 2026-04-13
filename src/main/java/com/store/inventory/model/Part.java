package com.store.inventory.model;

import javafx.beans.property.*;

/**
 * Represents a PC component (CPU, GPU, RAM, etc.) in inventory.
 * Uses JavaFX properties so TableView can observe changes directly.
 */
public class Part {
    private final IntegerProperty partID   = new SimpleIntegerProperty();
    private final StringProperty  partName = new SimpleStringProperty();
    private final StringProperty  category = new SimpleStringProperty();
    private final DoubleProperty  price    = new SimpleDoubleProperty();
    private final IntegerProperty stockQty = new SimpleIntegerProperty();

    public Part(int partID, String partName, String category, double price, int stockQty) {
        this.partID.set(partID);
        this.partName.set(partName);
        this.category.set(category);
        this.price.set(price);
        this.stockQty.set(stockQty);
    }

    // --- Getters (plain) ---
    public int    getPartID()   { return partID.get(); }
    public String getPartName() { return partName.get(); }
    public String getCategory() { return category.get(); }
    public double getPrice()    { return price.get(); }
    public int    getStockQty() { return stockQty.get(); }

    // --- Setters ---
    public void setPartName(String v) { partName.set(v); }
    public void setCategory(String v) { category.set(v); }
    public void setPrice(double v)    { price.set(v); }
    public void setStockQty(int v)    { stockQty.set(v); }

    // --- Property accessors (required by TableView PropertyValueFactory) ---
    public IntegerProperty partIDProperty()   { return partID; }
    public StringProperty  partNameProperty() { return partName; }
    public StringProperty  categoryProperty() { return category; }
    public DoubleProperty  priceProperty()    { return price; }
    public IntegerProperty stockQtyProperty() { return stockQty; }
}
