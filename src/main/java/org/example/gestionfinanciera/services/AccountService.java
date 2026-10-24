package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.Account;

import java.util.List;

public interface AccountService {

    Task<List<Account>> loadAccountsTask(String userId);

    /** Si el usuario no tiene cuentas, crea "Efectivo" y "Cuenta bancaria". */
    Task<List<Account>> ensureDefaultAccountsTask(String userId);

    Task<Account> addAccountTask(String userId, String name, String number);

    Task<Account> updateAccountTask(String userId, String accountId, String name, String number);

    /** No permite eliminar una cuenta usada como origen o destino de algún movimiento. */
    Task<Void> deleteAccountTask(String userId, String accountId);
}
