package org.labs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    private void runSimulation(int numProgrammers, int totalSoupPortions, int numWaiters, Main.Programmer[] programmers) throws InterruptedException {
        Main.Restaurant restaurant = new Main.Restaurant(totalSoupPortions, numWaiters);
        Lock[] spoons = new ReentrantLock[numProgrammers];

        for (int i = 0; i < numProgrammers; i++) {
            spoons[i] = new ReentrantLock(true);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(numProgrammers);

        for (int i = 0; i < numProgrammers; i++) {
            Lock leftSpoon = spoons[i];
            Lock rightSpoon = spoons[(i + 1) % numProgrammers];
            int leftIndex = i;
            int rightIndex = (i + 1) % numProgrammers;

            programmers[i] = new Main.Programmer(i, leftSpoon, leftIndex, rightSpoon, rightIndex, restaurant);
            executorService.submit(programmers[i]);
        }

        executorService.shutdown();

        if (!executorService.awaitTermination(2, TimeUnit.MINUTES)) {
            executorService.shutdownNow();
            fail("Симуляция не завершилась за 2 минуты, возможно, произошла блокировка.");
        }
    }

    @Test
    void testTotalMealsEatenShouldEqualInitialPortions() throws InterruptedException {
        final int numProgrammers = 7;
        final int totalSoupPortions = 10_000;
        final int numWaiters = 2;

        Main.Programmer[] programmers = new Main.Programmer[numProgrammers];
        runSimulation(numProgrammers, totalSoupPortions, numWaiters, programmers);

        long totalMeals = 0;
        for (Main.Programmer p : programmers) {
            totalMeals += p.getMealsEaten();
        }

        assertEquals(totalSoupPortions, totalMeals, "Общее число съеденных порций не совпадает с начальным количеством.");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testShouldFinishExecutionAndNotDeadlock() throws InterruptedException {
        final int numProgrammers = 7;
        final int totalSoupPortions = 1_000_000;
        final int numWaiters = 2;

        Main.Programmer[] programmers = new Main.Programmer[numProgrammers];
        runSimulation(numProgrammers, totalSoupPortions, numWaiters, programmers);

        assertTrue(true, "Программа успешно завершилась без блокировок.");
    }

    @Test
    void testFairnessAmongProgrammers() throws InterruptedException {
        final int numProgrammers = 7;
        final int totalSoupPortions = 100_000;
        final int numWaiters = 2;

        Main.Programmer[] programmers = new Main.Programmer[numProgrammers];
        runSimulation(numProgrammers, totalSoupPortions, numWaiters, programmers);

        double averageMeals = (double) totalSoupPortions / numProgrammers;
        double allowedDeviation = averageMeals * 0.2;

        for (Main.Programmer p : programmers) {
            assertNotNull(p, "Объект программиста не должен быть null");
            double actualMeals = p.getMealsEaten();
            assertTrue(
                    Math.abs(actualMeals - averageMeals) < allowedDeviation,
                    "Программист " + p.id + " съел " + (int)actualMeals + " порций, что сильно отклоняется от среднего " + (int)averageMeals
            );
        }
    }
}
