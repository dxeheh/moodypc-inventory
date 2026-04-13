package com.store.inventory.model;

import javafx.beans.property.*;

/**
 * Represents a customer's purchase order for a Part or Product.
 */
public class CustomerOrder {
    private final IntegerProperty orderID    = new SimpleIntegerProperty();
    private final IntegerProperty customerID = new SimpleIntegerProperty();
    private final StringProperty  itemType   = new SimpleStringProperty(); // "Part" or "Product"
    private final IntegerProperty itemID     = new SimpleIntegerProperty();
    private final StringProperty  itemName   = new SimpleStringProperty();
    private final IntegerProperty quantity   = new SimpleIntegerProperty();
    private final DoubleProperty  totalPrice = new SimpleDoubleProperty();
    private final StringProperty  date       = new SimpleStringProperty();
    private final StringProperty  status     = new SimpleStringProperty();

    public CustomerOrder(int orderID, int customerID, String itemType, int itemID,
                         String itemName, int quantity, double totalPrice,
                         String date, String status) {
        this.orderID.set(orderID);
        this.customerID.set(customerID);
        this.itemType.set(itemType);
        this.itemID.set(itemID);
        this.itemName.set(itemName);
        this.quantity.set(quantity);
        this.totalPrice.set(totalPrice);
        this.date.set(date);
        this.status.set(status);
    }

    public int    getOrderID()    { return orderID.get(); }
    public int    getCustomerID() { return customerID.get(); }
    public String getItemType()   { return itemType.get(); }
    public int    getItemID()     { return itemID.get(); }
    public String getItemName()   { return itemName.get(); }
    public int    getQuantity()   { return quantity.get(); }
    public double getTotalPrice() { return totalPrice.get(); }
    public String getDate()       { return date.get(); }
    public String getStatus()     { return status.get(); }

    public void setStatus(String v) { status.set(v); }

    public IntegerProperty orderIDProperty()    { return orderID; }
    public IntegerProperty customerIDProperty() { return customerID; }
    public StringProperty  itemTypeProperty()   { return itemType; }
    public IntegerProperty itemIDProperty()     { return itemID; }
    public StringProperty  itemNameProperty()   { return itemName; }
    public IntegerProperty quantityProperty()   { return quantity; }
    public DoubleProperty  totalPriceProperty() { return totalPrice; }
    public StringProperty  dateProperty()       { return date; }
    public StringProperty  statusProperty()     { return status; }
}
