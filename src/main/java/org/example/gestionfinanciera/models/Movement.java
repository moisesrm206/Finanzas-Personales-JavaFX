package org.example.gestionfinanciera.models;

import java.util.Date;

public class Movement {
    int movementId;
    int categoryId;
    int originAccountId;
    int destinationAccountId;
    int userId;
    double amount;
    String type;
    String concept;
    Date date;
    PaymentMethod paymentMethod;
    String state;
    String[] tags = new String[0];

    enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CHECK
    }

    public Movement(int movementId, int categoryId, int originAccountId, int destinationAccountId,
                    int userId, double amount, String type, String concept, PaymentMethod paymentMethod,
                    Date date, String state, String[] tags) {
        this.movementId = movementId;
        this.categoryId = categoryId;
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.concept = concept;
        this.paymentMethod = paymentMethod;
        this.date = date;
        this.state = state;
        this.tags = tags;
    }

    public int getMovementId() {
        return movementId;
    }

    public void setMovementId(int movementId) {
        this.movementId = movementId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(int originAccountId) {
        this.originAccountId = originAccountId;
    }

    public int getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(int destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
