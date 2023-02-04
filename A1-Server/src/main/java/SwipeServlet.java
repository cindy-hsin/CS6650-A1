import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import javabean.*;
import com.google.gson.Gson;

@WebServlet(name = "SwipeServlet", value = "/swipe")
public class SwipeServlet extends HttpServlet {
  private final static String LEFT = "left";
  private final static String RIGHT = "right";
  private final static int MAX_COMMENT_LEN = 256;
  private static final int MAX_SWIPER_ID = 5000;
  private static final int MAX_SWIPEE_ID = 1000000;

  private void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    ResponseMsg responseMsg = new ResponseMsg();
    Gson gson = new Gson();

    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      responseMsg.setMessage("missing path parameter");

      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return;
    }

    // check if URL is valid! "left" or right""
    if (!this.isUrlValid(urlPath)) {
      responseMsg.setMessage("invalid path parameter: should be " + LEFT + " or " + RIGHT);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return;
    }

    // Check if request body is valid!
    // swiper and swipee id should match a user in database
    // comment: max 256 characters
    try {
      StringBuilder sb = new StringBuilder();
      String s;
      while ((s = request.getReader().readLine()) != null) {
        sb.append(s);
      }

      SwipeDetails swipeDetails = (SwipeDetails) gson.fromJson(sb.toString(), SwipeDetails.class);

      // TODO: isSwiperValid method probably should not be in this class...?
      if (!this.isSwiperValid(swipeDetails.getSwiper())) {
        responseMsg.setMessage("User not found: invalid swiper id: "+ swipeDetails.getSwiper());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!this.isSwipeeValid(swipeDetails.getSwipee())) {
        responseMsg.setMessage("User not found: invalid swipee id: " + swipeDetails.getSwipee());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!this.isCommentValid(swipeDetails.getComment())) {
        responseMsg.setMessage("Invalid inputs: comment cannot exceed 256 characters");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        responseMsg.setMessage("Write successful");
        response.setStatus(HttpServletResponse.SC_CREATED);
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      responseMsg.setMessage(ex.getMessage());
    } finally {
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
    }
  }

  private boolean isUrlValid(String urlPath) {
    /**
     * Check if url path param: {leftorright} has value "left" or "right"
     */
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    String[] urlParts = urlPath.split("/");
    if (urlParts.length == 2 && (urlParts[1].equals(LEFT) || urlParts[1].equals(RIGHT)))
      return true;
    return false;
  }

  private boolean isSwiperValid(String id) {
    int id_int = Integer.parseInt(id);
    return 1 <= id_int && id_int <= MAX_SWIPER_ID ;
  }

  private boolean isSwipeeValid(String id) {
    int id_int = Integer.parseInt(id);
    return 1 <= id_int && id_int <= MAX_SWIPEE_ID ;
  }
  private boolean isCommentValid(String comment) {
    return comment.length() <= MAX_COMMENT_LEN;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    this.processRequest(request, response);
  }
}
