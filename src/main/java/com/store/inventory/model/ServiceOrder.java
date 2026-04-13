package com.store.inventory.model;

import javafx.beans.property.*;

public class ServiceOrder {
    private final IntegerProperty orderID    = new SimpleIntegerProperty();
    private final IntegerProperty customerID = new SimpleIntegerProperty();
    private final StringProperty  orderType  = new SimpleStringProperty();
    private final StringProperty  status     = new SimpleStringProperty();
    private final DoubleProperty  totalCost  = new SimpleDoubleProperty();

    public ServiceOrder(int orderID, int customerID, String orderType,
                        String status, double totalCost) {
        this.orderID.set(orderID);
        this.customerID.set(customerID);
        this.orderType.set(orderType);
        this.status.set(status);
        this.totalCost.set(totalCost);
    }

    public int    getOrderID()    { return orderID.get(); }
    public int    getCustomerID() { return customerID.get(); }
    public String getOrderType()  { return orderType.get(); }
    public String getStatus()     { return status.get(); }
    public double getTotalCost()  { return totalCost.get(); }

    public void setOrderType(String v)  { orderType.set(v); }
    public void setStatus(String v)     { status.set(v); }
    public void setTotalCost(double v)  { totalCost.set(v); }

    public IntegerProperty orderIDProperty()    { return orderID; }
    public IntegerProperty customerIDProperty() { return customerID; }
    public StringProperty  orderTypeProperty()  { return orderType; }
    public StringProperty  statusProperty()     { return status; }
    public DoubleProperty  totalCostProperty()  { return totalCost; }
}
