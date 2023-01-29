package producerconsumer;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable{
  private static final int MAX_RETRY = 5;
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

    try {
      while (this.numSuccessfulReqs.get() < LoadTestConfig.NUM_TOTAL_REQUESTS) {
        Request request = buffer.take();
        consume(request, swipeApi);
        System.out.println("Thread:" + Thread.currentThread().getName() + " Success cnt:" + this.numSuccessfulReqs.get());
      }
      this.latch.countDown();
    } catch (InterruptedException e) {
      System.out.println("Consumer failed to take a request from the buffer.");
      e.printStackTrace();
    }

  }

  /**
   * Execute a single POST request.
   * */
  private void consume(Request request, SwipeApi swipeApi) {
    int retry = MAX_RETRY;

    while (retry > 0) {
      try {
        ApiResponse res = swipeApi.swipeWithHttpInfo(request.getBody(), request.getSwipeDir());
        // System.out.println(res.getStatusCode() + ": " + res.getData());
        //TODO: Number of successful request +1.
        this.numSuccessfulReqs.getAndIncrement();
        return;
      } catch (ApiException e) {
        System.out.println("Consumer failed to send request: " + e.getCode() + ": " + e.getResponseBody() + ".Request details:"
        + request.getSwipeDir() + " " + request.getBody().toString() + ". Go Retry");
        retry --;
      }
    }

    //TODO: Number of unsuccessful request +1.
    this.numFailedReqs.getAndIncrement();
  }
}
