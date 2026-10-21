package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Account;

public interface accountDAO {
    Account getAccountById(int id);
    void saveAccount(Account account);
    void updateAccount(Account account);
    void deleteAccount(Account account);
}
