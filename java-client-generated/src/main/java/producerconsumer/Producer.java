package producerconsumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;
import request.RequestGenerator;

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
        Request request = RequestGenerator.generateSingleRequest();
        this.buffer.put(request);
        i ++;
        System.out.println("Thread:" + Thread.currentThread().getName() + Thread.currentThread().getId()+ " just put a request to buffer. Success cnt: " + this.numSuccessfulReqs.get() +
            "Requests in queue: " + this.buffer.size());
      } catch (Exception e) {
        System.out.println("Producer failed to put request into the buffer.");
        e.printStackTrace();
      }
    }

    this.latch.countDown();
  }
}
