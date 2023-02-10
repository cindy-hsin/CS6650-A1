package com.example.A1SpringBootServer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid comment length: comment cannot exceed 256 characters.")
public class InvalidCommentException extends Exception{

}
