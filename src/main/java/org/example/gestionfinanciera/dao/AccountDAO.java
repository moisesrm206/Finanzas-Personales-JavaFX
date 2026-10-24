package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Account;

import java.util.List;

public interface AccountDAO extends BaseDAO<Account> {

    List<Account> findByUser(String userId);
}
