package producerconsumer;

import config.LoadTestConfig;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MainQueueVersion {
  private static final int QUEUE_CAPACITY = 5000;
  public static void main(String[] args) throws InterruptedException {
    // 1 Producer Thread
    // 100 Consumer Threads

    // Stop condition for Producer Thread: When 50K requests have been successfully sent
    // Stop condition for Consumer Threads:
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);

    // 1 wait many!
    // NOTE: NUM_THREADS + 1 because we need one more Producer Thread
    CountDownLatch latch = new CountDownLatch(LoadTestConfig.NUM_THREADS + 1);
    BlockingQueue<Request> buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    long startTime = System.currentTimeMillis();
    Producer p = new Producer(buffer, numSuccessfulReqs, latch);
    Thread producerThread = new Thread(p);
    producerThread.setName("p");
    producerThread.setPriority(6);
    producerThread.start();

    try {
      for (int i = 0; i < LoadTestConfig.NUM_THREADS; i++) {
        Consumer c = new Consumer(buffer, numSuccessfulReqs, numFailedReqs, latch);
        new Thread(c).start();
      }
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      System.out.println("OutOfMemoryError:" + e.getMessage());
    }
    latch.await();
    long endTime = System.currentTimeMillis();
    System.out.println("multi-thread total time:" + (endTime - startTime) + "ms");
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);

  }
}
