package config;

public class LoadTestConfig {
  public static final int NUM_TOTAL_REQUESTS = 500000;
  public static final int NUM_THREADS = 200;    // Change this value for experiment

  public static final int SUCCESS_CODE = 201;

  public static final int MAX_RETRY = 5;

  // remote: "http://xxxx:8080/A1-Server_war";
  // local: "http://localhost:8080/A1_Server_war_exploded"
  public static final String URL = "http://54.191.169.180:8080/A1-Server_war/";

}
