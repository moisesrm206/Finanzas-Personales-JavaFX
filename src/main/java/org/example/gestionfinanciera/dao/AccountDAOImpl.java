package org.example.gestionfinanciera.dao;

import com.google.cloud.firestore.DocumentSnapshot;
import org.example.gestionfinanciera.models.Account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountDAOImpl extends FirestoreBaseDAO<Account> implements AccountDAO {

    private static final String COLLECTION = "accounts";

    public AccountDAOImpl() {
        super(COLLECTION);
    }

    @Override
    public List<Account> findByUser(String userId) {
        return query(collection.whereEqualTo("userId", userId));
    }

    @Override
    protected Map<String, Object> toDocument(Account account) {
        Map<String, Object> data = new HashMap<>();
        data.put("accountId", account.getAccountId());
        data.put("userId", account.getUserId());
        data.put("accountName", account.getAccountName());
        data.put("accountNumber", account.getAccountNumber());
        return data;
    }

    @Override
    protected Account fromSnapshot(DocumentSnapshot snapshot) {
        return new Account(
                snapshot.getId(),
                snapshot.getString("userId"),
                snapshot.getString("accountName"),
                snapshot.getString("accountNumber"));
    }

    @Override
    protected String getId(Account account) {
        return account.getAccountId();
    }

    @Override
    protected void setId(Account account, String id) {
        account.setAccountId(id);
    }
}
