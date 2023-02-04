package part2latency;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import request.Request;

public class RequestUtilWithTimestamp {
  private static final String[] SWIPE_VALUES = new String[]{"left", "right"};
  private static final int MIN_ID = 1;
  private static final int MAX_SWIPER_ID = 5000;
  private static final int MAX_SWIPEE_ID = 1000000;

  private static final int COMMENT_LENGTH = 256;

  private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "0123456789"
      + "abcdefghijklmnopqrstuvxyz";
  private static final int MAX_RETRY = 5;


  // ThreadLocalRandom rather than shared Random objects in concurrent programs will encounter less overhead and contention
  // For concurrent access, using ThreadLocalRandom instead of Math.random() results in less contention and, ultimately, better performance.
  // REF: https://docs.oracle.com/javase/tutorial/essential/concurrency/threadlocalrandom.html
  /**
   * Generate random fields for Request
   */
  public static Request generateSingleRequest() {
    String leftorright = generateSwipeDir();
    String swiperId = generateSwiperId();
    String swipeeId = generateSwipeeId();
    String comment = generateComment();

    SwipeDetails body = new SwipeDetails().swipee(swipeeId).swiper(swiperId).comment(comment);
    return new Request(leftorright, body);
  }


  private static String generateComment() {
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
  private static String generateSwipeDir() {
    String swipeDir =
        SWIPE_VALUES[ThreadLocalRandom.current().nextInt(SWIPE_VALUES.length)];
    return swipeDir;
  }

  private static String generateSwiperId() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ID, MAX_SWIPER_ID+1));

  }

  private static String generateSwipeeId() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ID, MAX_SWIPEE_ID+1));
  }

  /**
   * Execute a single POST request. If failed, retry up to 5 times before counting it as a failed request.
   * Returns the latency record containing {start time, request type, latecny, response code}
   * */
  public static Record sendSingleRequest(Request request, SwipeApi swipeApi, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
    int retry = MAX_RETRY;

    long startTime = System.currentTimeMillis();
    long endTime;
    while (retry > 0) {
      try {
        ApiResponse res = swipeApi.swipeWithHttpInfo(request.getBody(), request.getSwipeDir());

        endTime = System.currentTimeMillis();
        numSuccessfulReqs.getAndIncrement();
        //System.out.println("Thread:" + Thread.currentThread().getName() + " Success cnt:" + numSuccessfulReqs.get() + " Status:" + res.getStatusCode());

        return new Record(startTime, RequestType.POST, (int)(endTime-startTime), res.getStatusCode());
      } catch (ApiException e) {
        System.out.println("Consumer failed to send request: " + e.getCode() + ": " + e.getResponseBody() + ".request.Request details:"
            + request.getSwipeDir() + " " + request.getBody().toString() + ". Go retry");
        retry --;
        if (retry == 0) {
          endTime = System.currentTimeMillis();
          numFailedReqs.getAndIncrement();
          return new Record(startTime, RequestType.POST, (int)(endTime-startTime), e.getCode());
        }
      }
    }

    return null;

  }
}
