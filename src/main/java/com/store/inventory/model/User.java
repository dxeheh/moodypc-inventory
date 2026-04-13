package com.store.inventory.model;

public class User {
    private int userID;
    private String username;
    private String password;
    private String email;
    private String role; // "employee" or "customer"

    public User(int userID, String username, String password, String email, String role) {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public int getUserID()       { return userID; }
    public String getUsername()  { return username; }
    public String getPassword()  { return password; }
    public String getEmail()     { return email; }
    public String getRole()      { return role; }
    public boolean isEmployee()  { return "employee".equalsIgnoreCase(role); }

    @Override public String toString() { return username + " (" + role + ")"; }
}
