package thread;

import io.swagger.client.api.SwipeApi;
import java.util.concurrent.atomic.AtomicInteger;
import part2latency.Record;
import request.Request;

public interface ISendRequestThread {
 boolean sendSingleRequest(Request request, SwipeApi swipeApi, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs);
}
