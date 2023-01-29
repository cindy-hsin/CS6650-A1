package config;

public class LoadTestConfig {
  public static final int NUM_TOTAL_REQUESTS = 500000;
  public static final int NUM_THREADS = 100;    // Change this value for experiment

  public static final String URL = "http://localhost:8080/A1_Server_war_exploded";
  //remote: "http://54.212.0.107:8080/A1-Server_war";
}
