package com.story.helper;

import lombok.SneakyThrows;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 两个并行线程分别打印1-100奇数和偶数，并行保证打印出来的结果是有序的
 *
 * @author storys.zhang@gmail.com
 * <p>
 * Created at 2019/4/4 by Storys.Zhang
 */
public class OddEvenCrossover {

    private static ReentrantLock lock = new ReentrantLock(true);

    private static Condition condition = lock.newCondition();

    private static volatile int i = 1;

    private static final int total = 100;

    @SneakyThrows
    public static void main(String[] args) {

        Thread thread1 = new Thread(() -> {

            while (i < total) {
                try {
                    lock.lock();
                    if (i % 2 == 1) {
                        System.out.println("奇数线程打印：   " + i);
                        i++;
                        condition.signal();
                        lock.unlock();
                    }else {
                        condition.await();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if (lock.isLocked()) {
                        lock.unlock();
                    }
                }
            }
        });

        Thread thread2 = new Thread(() -> {

            while (i <= total) {
                try {
                    lock.lock();
                    if (i % 2 == 0) {
                        System.out.println("偶数线程打印：   " + i);
                        i++;
                        condition.signal();
                        lock.unlock();
                    }else {
                        condition.await();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                } finally {
                    if (lock.isLocked()) {
                        lock.unlock();
                    }
                }
            }
        });
        thread2.start();
        thread1.start();

    }
}
