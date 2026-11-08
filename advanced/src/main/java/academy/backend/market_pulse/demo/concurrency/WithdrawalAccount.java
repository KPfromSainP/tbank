package academy.backend.market_pulse.demo.concurrency;

/** Учебный счёт для сравнения целостного списания с проверкой, выполненной вне блокировки. */
final class WithdrawalAccount {
    /** Остаток счёта; изменения выполняются под монитором, итог читается после join. */
    private int balance = 100;
    /** Число списаний для проверки, что деньги не были списаны дважды. */
    private int successfulWithdrawals;

    /** Проверяет доступную сумму; отдельный вызов не резервирует деньги для последующего списания. */
    boolean hasEnough(int amount) {
        return balance >= amount;
    }

    /** Атомарно выполняет проверку и списание под одним монитором. */
    synchronized boolean withdraw(int amount) {
        if (!hasEnough(amount)) {
            return false;
        }
        withdrawWithoutCheck(amount);
        return true;
    }

    /** Меняет баланс под монитором, но намеренно не проверяет актуальность прежнего решения. */
    synchronized void withdrawWithoutCheck(int amount) {
        balance -= amount;
        successfulWithdrawals++;
    }

    int balance() {
        return balance;
    }

    int successfulWithdrawals() {
        return successfulWithdrawals;
    }
}
