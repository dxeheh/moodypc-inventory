package com.store.inventory.model;

import javafx.beans.property.*;

public class PurchaseOrder {
    private final IntegerProperty poID       = new SimpleIntegerProperty();
    private final IntegerProperty employeeID = new SimpleIntegerProperty();
    private final IntegerProperty vendorID   = new SimpleIntegerProperty();
    private final IntegerProperty partID     = new SimpleIntegerProperty();
    private final IntegerProperty quantity   = new SimpleIntegerProperty();
    private final StringProperty  date       = new SimpleStringProperty();
    private final StringProperty  status     = new SimpleStringProperty();

    public PurchaseOrder(int poID, int employeeID, int vendorID, int partID,
                         int quantity, String date, String status) {
        this.poID.set(poID);
        this.employeeID.set(employeeID);
        this.vendorID.set(vendorID);
        this.partID.set(partID);
        this.quantity.set(quantity);
        this.date.set(date);
        this.status.set(status);
    }

    public int    getPoID()       { return poID.get(); }
    public int    getEmployeeID() { return employeeID.get(); }
    public int    getVendorID()   { return vendorID.get(); }
    public int    getPartID()     { return partID.get(); }
    public int    getQuantity()   { return quantity.get(); }
    public String getDate()       { return date.get(); }
    public String getStatus()     { return status.get(); }

    public void setStatus(String v) { status.set(v); }

    public IntegerProperty poIDProperty()       { return poID; }
    public IntegerProperty employeeIDProperty() { return employeeID; }
    public IntegerProperty vendorIDProperty()   { return vendorID; }
    public IntegerProperty partIDProperty()     { return partID; }
    public IntegerProperty quantityProperty()   { return quantity; }
    public StringProperty  dateProperty()       { return date; }
    public StringProperty  statusProperty()     { return status; }
}
