package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;

/** Два списания по 80 при балансе 100: защищать нужно проверку вместе с изменением. */
public final class AccountRaceDemo {
    /** Сравнивает ошибочное списание с целостной операцией и проверяет итоговые балансы. */
    public static void main(String[] args) throws InterruptedException {
        WithdrawalAccount unsafe = demonstrateStaleCheck();
        WithdrawalAccount safe = new WithdrawalAccount();
        runPair(() -> safe.withdraw(80));
        printResult("Проверка вне монитора", unsafe);
        printResult("Проверка и изменение вместе", safe);
        if (unsafe.successfulWithdrawals() != 2 || unsafe.balance() != -60
                || safe.successfulWithdrawals() != 1 || safe.balance() != 20) {
            throw new AssertionError("Неожиданный результат демонстрации");
        }
    }

    /** Заставляет оба потока проверить баланс до первого изменения, воспроизводя гонку. */
    private static WithdrawalAccount demonstrateStaleCheck() throws InterruptedException {
        WithdrawalAccount account = new WithdrawalAccount();
        CountDownLatch bothChecked = new CountDownLatch(2);
        runPair(() -> {
            boolean enough = account.hasEnough(80);
            bothChecked.countDown();
            try {
                bothChecked.await();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }
            // NOTICE: монитор вокруг списания уже не исправляет устаревший результат проверки.
            if (enough) {
                account.withdrawWithoutCheck(80);
            }
        });
        return account;
    }

    private static void printResult(String label, WithdrawalAccount account) {
        System.out.printf("%s: списаний=%d, баланс=%d%n", label,
                account.successfulWithdrawals(), account.balance());
    }

    /** Запускает два конкурентных действия и не оставляет их работать при прерывании main. */
    private static void runPair(Runnable action) throws InterruptedException {
        Thread first = Thread.ofPlatform().name("withdrawal-1").unstarted(action);
        Thread second = Thread.ofPlatform().name("withdrawal-2").unstarted(action);
        first.start();
        second.start();
        try {
            first.join();
            second.join();
        } finally {
            first.interrupt();
            second.interrupt();
            first.join();
            second.join();
        }
    }
}
