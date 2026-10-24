package org.example.gestionfinanciera.models;

public class Account {
    private String accountId;
    private String userId;
    private String accountName;
    private String accountNumber;

    public Account() {
    }

    public Account(String accountId, String userId, String accountName, String accountNumber) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
