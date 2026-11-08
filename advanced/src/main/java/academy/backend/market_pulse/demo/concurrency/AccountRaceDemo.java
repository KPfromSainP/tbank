package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.CountDownLatch;

/** Два списания по 80 при балансе 100: защищать нужно проверку вместе с изменением. */
public final class AccountRaceDemo {
    private static final class Account {
        private int balance = 100;
        private int successfulWithdrawals;

        private synchronized boolean withdraw(int amount) {
            if (balance < amount) {
                return false;
            }
            balance -= amount;
            successfulWithdrawals++;
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account unsafe = new Account();
        CountDownLatch bothChecked = new CountDownLatch(2);
        Runnable unsafeWithdrawal = () -> {
            // Барьер намеренно воспроизводит ситуацию со старой схемы: обе проверки успешны.
            boolean enough = unsafe.balance >= 80;
            bothChecked.countDown();
            try {
                bothChecked.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (enough) {
                synchronized (unsafe) {
                    // Одного защищённого изменения недостаточно: проверка уже устарела.
                    unsafe.balance -= 80;
                    unsafe.successfulWithdrawals++;
                }
            }
        };
        runPair(unsafeWithdrawal);
        System.out.printf("Проверка вне монитора: списаний=%d, баланс=%d%n",
                unsafe.successfulWithdrawals, unsafe.balance);

        Account safe = new Account();
        runPair(() -> safe.withdraw(80));
        System.out.printf("Проверка и изменение вместе: списаний=%d, баланс=%d%n",
                safe.successfulWithdrawals, safe.balance);
        if (unsafe.successfulWithdrawals != 2 || unsafe.balance != -60
                || safe.successfulWithdrawals != 1 || safe.balance != 20) {
            throw new AssertionError("Неожиданный результат демонстрации");
        }
    }

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
