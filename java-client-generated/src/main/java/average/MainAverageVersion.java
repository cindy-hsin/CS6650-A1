package average;

import config.LoadTestConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MainAverageVersion {

  public static void main(String[] args) throws InterruptedException {
    // count successful/unsuccessful requests
    CountDownLatch latch = new CountDownLatch(LoadTestConfig.NUM_THREADS);
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < LoadTestConfig.NUM_THREADS; i++) {
      Runnable thread = new SendRequestAverageThread(latch, numSuccessfulReqs, numFailedReqs);
      new Thread(thread).start();
    }

    latch.await();
    long endTime = System.currentTimeMillis();
    System.out.println("multi-thread total time:" + (endTime - startTime) + "ms");
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);
  }
}
