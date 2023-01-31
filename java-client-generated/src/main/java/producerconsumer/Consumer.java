package producerconsumer;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;
import request.RequestUtil;

public class Consumer implements Runnable{
  private final BlockingQueue<Request> buffer;
  private final AtomicInteger numSuccessfulReqs;

  private final AtomicInteger numFailedReqs;
  private final CountDownLatch latch;

  public Consumer(BlockingQueue<Request> buffer, AtomicInteger numSuccessfulReqs,
      AtomicInteger numFailedReqs, CountDownLatch latch) {
    this.buffer = buffer;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.numFailedReqs = numFailedReqs;
    this.latch = latch;
  }

  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);

    while (this.numSuccessfulReqs.get() < LoadTestConfig.NUM_TOTAL_REQUESTS) {
      try {
        if (this.buffer.size() > 0) {
          Request request = buffer.take();
          RequestUtil.sendSingleRequest(request, swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
          // System.out.println("Thread:" + Thread.currentThread().getName() + " Success cnt:" + this.numSuccessfulReqs.get());
        }
      } catch (InterruptedException e) {
        System.out.println("Consumer failed to take a request from the buffer.");
        e.printStackTrace();
      }
    }
    this.latch.countDown();
    System.out.println("Consumer thread should be closed, latch count: " + this.latch.getCount());

  }

}
