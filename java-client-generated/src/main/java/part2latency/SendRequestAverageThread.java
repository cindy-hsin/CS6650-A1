package part2latency;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.api.SwipeApi;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.RequestUtil;


public class SendRequestAverageThread implements Runnable{
  private final CountDownLatch latch;
  private final AtomicInteger numSuccessfulReqs;
  private final AtomicInteger numFailedReqs;

  private final BlockingQueue<Pair> recordsBuffer;


  public SendRequestAverageThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs,
      BlockingQueue<Pair> recordsBuffer) {
    this.latch = latch;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.numFailedReqs = numFailedReqs;
    this.recordsBuffer = recordsBuffer;
  }


  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);
    List<Record> records = new ArrayList<>();
    // Send multiple requests
    //Each thread averagely sends fixed amount of request.
    for (int i = 0; i < LoadTestConfig.NUM_TOTAL_REQUESTS / LoadTestConfig.NUM_THREADS; i++) {
      Record record = RequestUtilWithTimestamp.sendSingleRequest(RequestUtil.generateSingleRequest(), swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
      records.add(record);
    }

    this.latch.countDown();

    try {
      recordsBuffer.put(new Pair(Thread.currentThread().getName(), records));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);      // TODO: Error handling
    }

  }

}
