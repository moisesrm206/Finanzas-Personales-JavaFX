package org.example.gestionfinanciera.models;

import java.util.Date;

public class Movement {
    private String movementId;
    private String categoryId;
    private String originAccountId;
    private String destinationAccountId; // solo se usa en las transferencias
    private String userId;
    private double amount;
    private String type;
    private String concept;
    private Date date;
    private PaymentMethod paymentMethod;
    private String state;
    private String[] tags = new String[0];

    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CHECK, SPEI
    }

    public Movement() {
    }

    public Movement(String movementId, String categoryId, String originAccountId, String destinationAccountId,
                    String userId, double amount, String type, String concept, PaymentMethod paymentMethod,
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

    public String getMovementId() {
        return movementId;
    }

    public void setMovementId(String movementId) {
        this.movementId = movementId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(String originAccountId) {
        this.originAccountId = originAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
