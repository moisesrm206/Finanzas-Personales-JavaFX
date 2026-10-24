package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.AccountDAO;
import org.example.gestionfinanciera.dao.MovementDAO;
import org.example.gestionfinanciera.models.Account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.example.gestionfinanciera.services.TaskUtil.task;

public class AccountServiceImpl implements AccountService {

    private final AccountDAO accountDAO;
    private final MovementDAO movementDAO;

    public AccountServiceImpl(AccountDAO accountDAO, MovementDAO movementDAO) {
        this.accountDAO = accountDAO;
        this.movementDAO = movementDAO;
    }

    @Override
    public Task<List<Account>> loadAccountsTask(String userId) {
        return task(() -> sortedByName(accountDAO.findByUser(requireUser(userId))));
    }

    @Override
    public Task<List<Account>> ensureDefaultAccountsTask(String userId) {
        return task(() -> {
            List<Account> existing = accountDAO.findByUser(requireUser(userId));
            if (!existing.isEmpty()) {
                return sortedByName(existing);
            }
            List<Account> defaults = new ArrayList<>();
            defaults.add(new Account(null, userId, "Efectivo", ""));
            defaults.add(new Account(null, userId, "Cuenta bancaria", ""));
            accountDAO.createAll(defaults);
            return sortedByName(defaults);
        });
    }

    @Override
    public Task<Account> addAccountTask(String userId, String name, String number) {
        return task(() -> {
            String cleanName = validName(name);
            requireUser(userId);
            ensureNotDuplicated(userId, cleanName, null);
            return accountDAO.create(new Account(null, userId, cleanName, cleanNumber(number)));
        });
    }

    @Override
    public Task<Account> updateAccountTask(String userId, String accountId, String name, String number) {
        return task(() -> {
            String cleanName = validName(name);
            Account account = ownedAccount(userId, accountId);
            ensureNotDuplicated(userId, cleanName, accountId);
            account.setAccountName(cleanName);
            account.setAccountNumber(cleanNumber(number));
            accountDAO.update(account);
            return account;
        });
    }

    @Override
    public Task<Void> deleteAccountTask(String userId, String accountId) {
        return task(() -> {
            ownedAccount(userId, accountId);
            if (movementDAO.existsByAccount(userId, accountId)) {
                throw new IllegalArgumentException(
                        "No se puede eliminar: la cuenta tiene movimientos registrados.");
            }
            accountDAO.delete(accountId);
            return null;
        });
    }

    private Account ownedAccount(String userId, String accountId) {
        Account account = accountDAO.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe."));
        if (userId == null || !userId.equals(account.getUserId())) {
            throw new IllegalArgumentException("La cuenta no pertenece al usuario.");
        }
        return account;
    }

    private void ensureNotDuplicated(String userId, String name, String ignoredAccountId) {
        boolean duplicated = accountDAO.findByUser(userId).stream()
                .filter(account -> !account.getAccountId().equals(ignoredAccountId))
                .anyMatch(account -> name.equalsIgnoreCase(account.getAccountName()));
        if (duplicated) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese nombre.");
        }
    }

    private String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la cuenta es obligatorio.");
        }
        String clean = name.trim();
        if (clean.length() > 40) {
            throw new IllegalArgumentException("El nombre de la cuenta es demasiado largo (máximo 40).");
        }
        return clean;
    }

    private String cleanNumber(String number) {
        return number == null ? "" : number.trim();
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("No hay un usuario con sesión iniciada.");
        }
        return userId;
    }

    private List<Account> sortedByName(List<Account> accounts) {
        List<Account> sorted = new ArrayList<>(accounts);
        sorted.sort(Comparator.comparing(Account::getAccountName, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }
}
