package average;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.api.SwipeApi;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


import thread.AbsSendRequestThread;
import request.RequestGenerator;


public class SendRequestAverageThread extends AbsSendRequestThread implements Runnable  {

  public SendRequestAverageThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
    super(numSuccessfulReqs, numFailedReqs, latch);
  }


  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);

    // Send multiple requests
    // Each thread averagely sends fixed amount of request.
    for (int i = 0; i < LoadTestConfig.NUM_TOTAL_REQUESTS / LoadTestConfig.NUM_THREADS; i++) {
      this.sendSingleRequest(RequestGenerator.generateSingleRequest(), swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
    }

    this.latch.countDown();
  }


}
