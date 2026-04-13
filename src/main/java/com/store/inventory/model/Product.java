package com.store.inventory.model;

import javafx.beans.property.*;

/**
 * Represents a complete PC or Laptop product for sale.
 */
public class Product {
    private final IntegerProperty productID   = new SimpleIntegerProperty();
    private final StringProperty  productName = new SimpleStringProperty();
    private final StringProperty  type        = new SimpleStringProperty(); // Desktop | Laptop
    private final DoubleProperty  price       = new SimpleDoubleProperty();
    private final IntegerProperty stockQty    = new SimpleIntegerProperty();
    private final StringProperty  specs       = new SimpleStringProperty();

    public Product(int productID, String productName, String type,
                   double price, int stockQty, String specs) {
        this.productID.set(productID);
        this.productName.set(productName);
        this.type.set(type);
        this.price.set(price);
        this.stockQty.set(stockQty);
        this.specs.set(specs);
    }

    public int    getProductID()   { return productID.get(); }
    public String getProductName() { return productName.get(); }
    public String getType()        { return type.get(); }
    public double getPrice()       { return price.get(); }
    public int    getStockQty()    { return stockQty.get(); }
    public String getSpecs()       { return specs.get(); }

    public void setProductName(String v) { productName.set(v); }
    public void setType(String v)        { type.set(v); }
    public void setPrice(double v)       { price.set(v); }
    public void setStockQty(int v)       { stockQty.set(v); }
    public void setSpecs(String v)       { specs.set(v); }

    public IntegerProperty productIDProperty()   { return productID; }
    public StringProperty  productNameProperty() { return productName; }
    public StringProperty  typeProperty()        { return type; }
    public DoubleProperty  priceProperty()       { return price; }
    public IntegerProperty stockQtyProperty()    { return stockQty; }
    public StringProperty  specsProperty()       { return specs; }
}
