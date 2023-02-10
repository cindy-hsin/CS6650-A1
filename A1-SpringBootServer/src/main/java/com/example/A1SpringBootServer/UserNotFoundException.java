package com.example.A1SpringBootServer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class UserNotFoundException extends Exception {
  @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found: invalid swiper id")
  protected static class InvalidSwiperException extends Exception {

  }
  @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found: invalid swipee id")
  protected static class InvalidSwipeeException extends Exception {

  }
}
