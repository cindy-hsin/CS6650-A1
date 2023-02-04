package config;

public class LoadTestConfig {
  public static final int NUM_TOTAL_REQUESTS = 500000;
  public static final int NUM_THREADS = 200;    // Change this value for experiment

  public static final int SUCCESS_CODE = 201;

  // remote: "http://xxxx:8080/A1-Server_war";
  // local: "http://localhost:8080/A1_Server_war_exploded"
  public static final String URL = "http://35.164.233.96:8080/A1-Server_war/";

}
