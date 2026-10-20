package org.example.gestionfinanciera.models;

public class Account {
    int accountId;
    int userId;
    String accountName;
    String accountNumber;

    public Account(int accountId, int userId, String accountName, String accountNumber) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
