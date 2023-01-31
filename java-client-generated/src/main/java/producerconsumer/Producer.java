package producerconsumer;

import io.swagger.client.model.SwipeDetails;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;
import request.RequestUtil;

/**
 * Generate the request and store into a BlockingQueue
 */
public class Producer implements Runnable{
  private final BlockingQueue<Request> buffer;
  private final AtomicInteger numSuccessfulReqs;
  private final CountDownLatch latch;

  private final int targetNumReqs;

  public Producer(BlockingQueue<Request> buffer, AtomicInteger numSuccessfulReqs, CountDownLatch latch, int targetNumReqs) {
    this.buffer = buffer;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.latch = latch;
    this.targetNumReqs = targetNumReqs;
  }

  @Override
  public void run() {
    int i = 0;
    while (i < this.targetNumReqs) {
      try {
        Request request = RequestUtil.generateSingleRequest();
        this.buffer.put(request);
        i ++;
        System.out.println("Thread:" + Thread.currentThread().getName() + Thread.currentThread().getId()+ " just put a request to buffer. Success cnt: " + this.numSuccessfulReqs.get() +
            "Requests in queue: " + this.buffer.size());
      } catch (Exception e) {
        System.out.println("Producer failed to put request into the buffer.");
        e.printStackTrace();
      }
    }
    System.out.println("Producer while loop ends");
    this.latch.countDown();
    System.out.println("Producer thread should be closed, latch count: " + this.latch.getCount());
  }
}
