package producerconsumer;

import config.LoadTestConfig;
import io.swagger.client.model.SwipeDetails;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generate the request and store into a BlockingQueue
 */
public class Producer implements Runnable{

  private static final String[] SWIPE_VALUES = new String[]{"left", "right"};
  private static final int MIN_ID = 1;
  private static final int MAX_SWIPER_ID = 5000;
  private static final int MAX_SWIPEE_ID = 1000000;

  private static final int COMMENT_LENGTH = 256;

  private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "0123456789"
      + "abcdefghijklmnopqrstuvxyz";
  private final BlockingQueue<Request> buffer;
  private final AtomicInteger numSuccessfulReqs;
  private final CountDownLatch latch;


  public Producer(BlockingQueue<Request> buffer, AtomicInteger numSuccessfulReqs, CountDownLatch latch) {
    this.buffer = buffer;
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.latch = latch;
  }

  @Override
  public void run() {
    try {
      while (this.numSuccessfulReqs.get() < LoadTestConfig.NUM_TOTAL_REQUESTS) {
        Request request = this.produce();
        this.buffer.put(request);
        System.out.println("Thread:" + Thread.currentThread().getName() + " just put a request to buffer. Success cnt: " + this.numSuccessfulReqs.get());
      }
      this.latch.countDown();
    } catch (InterruptedException e) {
      System.out.println("Producer failed to put request into the buffer.");
      e.printStackTrace();
    }
  }

  /**
   * Generate random fields for Request, and put the Request into buffer queue.
   */
  private Request produce() {
      String leftorright = this.generateSwipeDir();
      String swiperId = this.generateSwiperId();
      String swipeeId = this.generateSwipeeId();
      String comment = this.generateComment();

      SwipeDetails body = new SwipeDetails().swipee(swipeeId).swiper(swiperId).comment(comment);
      return new Request(leftorright, body);
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
}
