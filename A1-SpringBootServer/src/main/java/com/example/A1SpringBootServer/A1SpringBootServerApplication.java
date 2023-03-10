package com.example.A1SpringBootServer;

import javabean.SwipeDetails;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class A1SpringBootServerApplication extends SpringBootServletInitializer {
	private final static String LEFT = "left";
	private final static String RIGHT = "right";
	private final static int MAX_COMMENT_LEN = 256;
	private static final int MAX_SWIPER_ID = 5000;
	private static final int MAX_SWIPEE_ID = 1000000;
	public static void main(String[] args) {
		SpringApplication.run(A1SpringBootServerApplication.class, args);
	}

	@GetMapping("/swipe")
	public String doGet() {
		return "Hello!";
	}


	@PostMapping(path="/swipe/{leftorright}",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)

	public ResponseEntity<?> post(@PathVariable String leftorright, @RequestBody SwipeDetails swipeDetails) {
		System.out.println(leftorright);
		if (!leftorright.equals(LEFT) && !leftorright.equals(RIGHT)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid path parameter! Should be left or right");
		}

		if (!this.isSwiperValid(swipeDetails.getSwiper())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: invalid swiper id");
		}else if (!this.isSwipeeValid(swipeDetails.getSwipee())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: invalid swipee id");
		} else if (!this.isCommentValid(swipeDetails.getComment())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid comment length: comment cannot exceed 256 characters.");
		} else {
			return ResponseEntity.status(HttpStatus.OK).body("Write successful!");
		}

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

}
