package producerconsumer;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;

import io.swagger.client.api.SwipeApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;
import thread.AbsSendRequestThread;


public class Consumer extends AbsSendRequestThread implements Runnable {
  private final BlockingQueue<Request> buffer;

  public Consumer(BlockingQueue<Request> buffer, AtomicInteger numSuccessfulReqs,
      AtomicInteger numFailedReqs, CountDownLatch latch) {
    super(numSuccessfulReqs, numFailedReqs, latch);
    this.buffer = buffer;
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
          this.sendSingleRequest(request, swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
        }
      } catch (InterruptedException e) {
        System.out.println("Consumer failed to take a request from the buffer.");
        e.printStackTrace();
      }
    }
    this.latch.countDown();
  }

}
