package org.example.gestionfinanciera.models;

public class Budget {
    int budgetId;
    int userId;
    int categoryId;
    double limit;
    int month;

    public Budget(int budgetId, int userId, int categoryId, double limit, int month) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.limit = limit;
        this.month = month;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }
}
