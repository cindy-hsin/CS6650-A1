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

public class SendRequestAverageThread implements Runnable{


  // ThreadLocalRandom rather than shared Random objects in concurrent programs will encounter less overhead and contention
  // For concurrent access, using ThreadLocalRandom instead of Math.random() results in less contention and, ultimately, better performance.
  // REF: https://docs.oracle.com/javase/tutorial/essential/concurrency/threadlocalrandom.html
  private static final String[] SWIPE_VALUES = new String[]{"left", "right"};
  private static final int MIN_ID = 1;
  private static final int MAX_SWIPER_ID = 5000;
  private static final int MAX_SWIPEE_ID = 1000000;

  private static final int COMMENT_LENGTH = 256;

  private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "0123456789"
      + "abcdefghijklmnopqrstuvxyz";

  private static final int MAX_RETRY = 5;

  private final CountDownLatch latch;
  private final AtomicInteger numSuccessfulReqs;
  private final AtomicInteger numFailedReqs;


  public SendRequestAverageThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
    this.latch = latch;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.numFailedReqs = numFailedReqs;

  }

  private String generateComment() {
    // create StringBuffer size of AlphaNumericString
    StringBuilder sb = new StringBuilder(COMMENT_LENGTH);

    for (int i = 0; i < COMMENT_LENGTH; i++) {
      // generate a random number between
      // 0 to AlphaNumericString variable length
      int index = ThreadLocalRandom.current().nextInt(ALPHA_NUMERIC_STRING.length());
      // add Character one by one in end of sb
      sb.append(ALPHA_NUMERIC_STRING.charAt(index));
    }

    return sb.toString();
  }
  private String generateSwipeDir() {
    String swipeDir =
        SWIPE_VALUES[ThreadLocalRandom.current().nextInt(SWIPE_VALUES.length)];
    return swipeDir;
  }

  private String generateSwiperId() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ID, MAX_SWIPER_ID+1));

  }

  private String generateSwipeeId() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ID, MAX_SWIPEE_ID+1));
  }

  /**
   * Send a single Request. If failed, retry up to 5 times before counting it as a failed request.
   */
  private void sendSingleRequest(SwipeApi swipeApi) {
    String leftorright = this.generateSwipeDir();
    String swiperId = this.generateSwiperId();
    String swipeeId = this.generateSwipeeId();
    String comment = this.generateComment();

    if (Integer.valueOf(swiperId) < 1 || Integer.valueOf(swiperId) > MAX_SWIPER_ID) {
      System.out.println("Invalid swiperId:" + swiperId);
    }

    if (Integer.valueOf(swipeeId) < 1 || Integer.valueOf(swipeeId) > MAX_SWIPEE_ID) {
      System.out.println("Invalid swipeeId:" + swipeeId);
    }

    SwipeDetails body = new SwipeDetails().swipee(swipeeId).swiper(swiperId).comment(comment);

    int retry = MAX_RETRY;

    while (retry > 0) {
      try {
        ApiResponse res = swipeApi.swipeWithHttpInfo(body, leftorright);
        // System.out.println(res.getStatusCode() + ": " + res.getData());
        //Number of successful request +1.
        this.numSuccessfulReqs.getAndIncrement();
        return;
      } catch (ApiException e) {
        System.out.println("Failed: " + e.getCode() + ": " + e.getResponseBody());
        retry --;
      }
    }

    //Number of unsuccessful request +1.
    this.numFailedReqs.getAndIncrement();
  }




  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);

    // Send multiple requests

    //Each thread averagely sends fixed amount of request.
    for (int i = 0; i < LoadTestConfig.NUM_TOTAL_REQUESTS / LoadTestConfig.NUM_THREADS; i++) {
      this.sendSingleRequest(swipeApi);
    }

    this.latch.countDown();
  }
}
