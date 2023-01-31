package producerconsumer;

import config.LoadTestConfig;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;

public class MainQueueVersion {
  private static final int QUEUE_CAPACITY = 10000;
  private static final int NUM_PRODUCER_THREAD = 1;
  private static final int NUM_CONSUMER_THREAD = LoadTestConfig.NUM_THREADS - NUM_PRODUCER_THREAD;
  public static void main(String[] args) throws InterruptedException {
    // Stop condition for Producer Thread: When 50K requests have been successfully pushed to queue
    // Stop condition for Consumer Threads: When 50K requests have been successfully sent
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);

    // 1 wait many!
    CountDownLatch pLatch = new CountDownLatch(NUM_PRODUCER_THREAD);
    CountDownLatch cLatch = new CountDownLatch(NUM_CONSUMER_THREAD);
    BlockingQueue<Request> buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < NUM_PRODUCER_THREAD; i++) {
      // Producer p = new Producer(buffer, numSuccessfulReqs, pLatch);
      Producer p = new Producer(buffer, numSuccessfulReqs, pLatch,LoadTestConfig.NUM_TOTAL_REQUESTS / NUM_PRODUCER_THREAD);
      Thread producerThread = new Thread(p);
      producerThread.setName("p");
      producerThread.setPriority(6);
      producerThread.start();
    }


    for (int i = 0; i < NUM_CONSUMER_THREAD; i++) {
      Consumer c = new Consumer(buffer, numSuccessfulReqs, numFailedReqs, cLatch);
      new Thread(c).start();
    }

    pLatch.await();
    cLatch.await();
    long endTime = System.currentTimeMillis();
    System.out.println("multi-thread total time:" + (endTime - startTime) + "ms");
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);

  }
}
