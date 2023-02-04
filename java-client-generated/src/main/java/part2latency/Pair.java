package part2latency;

import java.util.Collection;
import java.util.List;

public class Pair {
  private String threadId;
  private List<Record> records;

  public Pair(String threadId, List<Record> records) {
    this.threadId = threadId;
    this.records = records;
  }

  public String getThreadId() {
    return threadId;
  }

  public List<Record> getRecords() {
    return records;
  }
}
