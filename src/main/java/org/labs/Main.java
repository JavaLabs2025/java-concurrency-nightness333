package org.labs;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    static class Restaurant {
        private final AtomicInteger soupPortions;
        private final Semaphore waiters;

        public Restaurant(int totalSoupPortions, int numberOfWaiters) {
            this.soupPortions = new AtomicInteger(totalSoupPortions);
            this.waiters = new Semaphore(numberOfWaiters, true);
            System.out.println("В ресторане работает " + numberOfWaiters + " официантов.");
        }

        public boolean getSoupPortion() throws InterruptedException {
            waiters.acquire();

            try {
                while (true) {
                    int remaining = soupPortions.get();

                    if (remaining <= 0) {
                        return false;
                    }

                    if (soupPortions.compareAndSet(remaining, remaining - 1)) {
                        return true;
                    }
                }
            } finally {
                waiters.release();
            }
        }
    }

    static class Programmer implements Runnable {
        public final int id;
        private final Lock lowerIndexSpoon;
        private final Lock higherIndexSpoon;
        private final Restaurant restaurant;
        private int mealsEaten = 0;

        public Programmer(int id, Lock leftSpoon, int leftIndex, Lock rightSpoon, int rightIndex, Restaurant restaurant) {
            this.id = id;
            this.restaurant = restaurant;

            if (leftIndex < rightIndex) {
                this.lowerIndexSpoon = leftSpoon;
                this.higherIndexSpoon = rightSpoon;
            } else {
                this.lowerIndexSpoon = rightSpoon;
                this.higherIndexSpoon = leftSpoon;
            }
        }

        private void think() throws InterruptedException {
            Thread.sleep(0, ThreadLocalRandom.current().nextInt(5, 100));
        }

        private void eat() throws InterruptedException {
            lowerIndexSpoon.lock();
            try {
                higherIndexSpoon.lock();
                try {
                    mealsEaten++;
                    Thread.sleep(0, ThreadLocalRandom.current().nextInt(5, 100));
                } finally {
                    higherIndexSpoon.unlock();
                }
            } finally {
                lowerIndexSpoon.unlock();
            }
        }

        @Override
        public void run() {
            try {
                while (restaurant.getSoupPortion()) {
                    think();
                    eat();
                    Thread.sleep(0, ThreadLocalRandom.current().nextInt(5, 100));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public int getMealsEaten() {
            return mealsEaten;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final int NUM_PROGRAMMERS = 7;
        final int TOTAL_SOUP_PORTIONS = 100_000;
        final int NUM_WAITERS = 2;

        System.out.println("Обед начинается!");
        System.out.println("Программистов: " + NUM_PROGRAMMERS);
        System.out.println("Всего порций супа: " + TOTAL_SOUP_PORTIONS);

        final Restaurant restaurant = new Restaurant(TOTAL_SOUP_PORTIONS, NUM_WAITERS);
        final Lock[] spoons = new ReentrantLock[NUM_PROGRAMMERS];

        for (int i = 0; i < NUM_PROGRAMMERS; i++) {
            spoons[i] = new ReentrantLock(true);
        }

        final Programmer[] programmers = new Programmer[NUM_PROGRAMMERS];
        final ExecutorService executorService = Executors.newFixedThreadPool(NUM_PROGRAMMERS);

        for (int i = 0; i < NUM_PROGRAMMERS; i++) {
            Lock leftSpoon = spoons[i];
            Lock rightSpoon = spoons[(i + 1) % NUM_PROGRAMMERS];
            int leftIndex = i;
            int rightIndex = (i + 1) % NUM_PROGRAMMERS;

            programmers[i] = new Programmer(i, leftSpoon, leftIndex, rightSpoon, rightIndex, restaurant);
            executorService.submit(programmers[i]);
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
            System.err.println("Программисты не смогли закончить обед вовремя!");
            executorService.shutdownNow();
        }

        System.out.println("\nОбед закончен!");

        System.out.println("--- Статистика по приемам пищи ---");

        int totalMeals = 0;

        for (int i = 0; i < NUM_PROGRAMMERS; i++) {
            System.out.println("Программист " + i + " поел " + programmers[i].getMealsEaten() + " раз.");
            totalMeals += programmers[i].getMealsEaten();
        }

        System.out.println("------------------------------------");
        System.out.println("Всего съедено порций: " + totalMeals);
        System.out.println("Осталось нетронутых порций: " + (TOTAL_SOUP_PORTIONS - totalMeals));
    }
}
