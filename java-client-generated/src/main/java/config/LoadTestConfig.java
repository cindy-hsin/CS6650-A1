package config;

public class LoadTestConfig {
  public static final int NUM_TOTAL_REQUESTS = 500000;
  public static final int NUM_THREADS = 200;    // Change this value for experiment

  // remote: "http://xxxx:8080/A1-Server_war";
  // local: "http://localhost:8080/A1_Server_war_exploded"
  public static final String URL = "http://52.12.174.62:8080/A1-Server_war";

}
