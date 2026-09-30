import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    static Queue<String> orders = new ConcurrentLinkedQueue<>();
    static Queue<String> tickets = new LinkedList<>(); // Черга чеків для шефа
    static volatile boolean isOpen = true;
    static volatile boolean waitersFinished = false;

    public static void main(String[] args) throws InterruptedException {
        orders.add("Стіл 1: Піца, Чай");
        orders.add("Стіл 2: Чизбургер, Картопля фрі");
        orders.add("Стіл 3: Стейк");
        orders.add("Стіл 4: Борщ");
        orders.add("Стіл 5: 2x Капучино");
        orders.add("Стіл 6: Паста");
        orders.add("Стіл 7: Шашлик");
        orders.add("Стіл 8: Салат");

        List<Thread> waiters = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            waiters.add(new Thread(Main::takeOrder, "Офіціант " + i));
        }

        Thread chef = new Thread(Main::cookOrder, "Шеф-кухар");

        // Симуляція настання 21:00 через 2.5 секунди
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isOpen = false;
                System.out.println("\n>>> 21:00! Прийом замовлень закрито! <<<\n");
            }
        }, 2500);

        // Запуск офіціантів та шефа
        waiters.forEach(Thread::start);
        chef.start();

        // Очікуємо, поки офіціанти завершать прийом замовлень
        for (Thread waiter : waiters) {
            waiter.join();
        }

        // Сповіщаємо шефа, що офіціанти закінчили роботу
        synchronized (tickets) {
            waitersFinished = true;
            tickets.notifyAll(); // розбудити шефа, якщо він очікував на нові чеки
        }

        // Повідомлення про столи, які не встигли зробити замовлення
        if (!orders.isEmpty()) {
            System.out.println("Столи, які не встигли замовити до 21:00: " + orders);
        }

        // Очікуємо, поки шеф доготує всі прийняті чеки
        chef.join();
        timer.cancel();

        System.out.println("\nВсі замовлення виконано. Ресторан повністю зачинено!");
    }

    private static void takeOrder() {
        String waiter = Thread.currentThread().getName();
        while (isOpen) {
            String order = orders.poll();
            if (order == null) {
                break;
            }

            System.out.printf("%s взяв замовлення: %s\n", waiter, order);
            try {
                Thread.sleep(800); // час на прийом замовлення та формування чека
            } catch (InterruptedException e) {
                return;
            }

            String ticket = "Чек [" + order + ", оформив " + waiter + "]";
            System.out.printf("%s сформував і передав на кухню: %s\n", waiter, ticket);

            // Додаємо чек і сповіщаємо шефа про нове замовлення
            synchronized (tickets) {
                tickets.add(ticket);
                tickets.notify();
            }
        }
        System.out.printf("%s завершив роботу.\n", waiter);
    }

    private static void cookOrder() {
        while (true) {
            String ticket;

            // Очікування нового чека або завершення зміни
            synchronized (tickets) {
                while (tickets.isEmpty()) {
                    if (waitersFinished) {
                        // Офіціанти завершили зміну і чеків більше немає
                        System.out.println("\nШеф-кухар доготував усі замовлення для столів, які встигли замовити до 21:00.");
                        return;
                    }
                    try {
                        tickets.wait(); // Шеф чекає на сповіщення (notify) від офіціанта
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                ticket = tickets.poll();
            }

            // Готування страви (виконується окремо, щоб не блокувати чергу)
            if (!isOpen) {
                System.out.printf("Шеф-кухар ДОГОТОВЛЮЄ після 21:00: %s\n", ticket);
            } else {
                System.out.printf("Шеф-кухар готує: %s\n", ticket);
            }

            try {
                Thread.sleep(1200); // час приготування страви
            } catch (InterruptedException e) {
                return;
            }

            System.out.printf("Шеф-кухар ПРИГОТУВАВ: %s\n", ticket);
        }
    }
}
