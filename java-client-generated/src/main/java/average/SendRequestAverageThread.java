package average;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import request.Request;
import request.RequestUtil;


public class SendRequestAverageThread implements Runnable{
  private final CountDownLatch latch;
  private final AtomicInteger numSuccessfulReqs;
  private final AtomicInteger numFailedReqs;


  public SendRequestAverageThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
    this.latch = latch;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.numFailedReqs = numFailedReqs;
  }


  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);

    // Send multiple requests
    //Each thread averagely sends fixed amount of request.
    for (int i = 0; i < LoadTestConfig.NUM_TOTAL_REQUESTS / LoadTestConfig.NUM_THREADS; i++) {
      RequestUtil.sendSingleRequest(RequestUtil.generateSingleRequest(), swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
    }

    this.latch.countDown();
  }
}
