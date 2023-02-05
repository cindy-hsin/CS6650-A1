package part2latency;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.util.HashMap;
import java.util.Map;

public class RunningMetrics {
  protected static final int NUM_BUCKET = 10000;
  private int minLatency = new Double(POSITIVE_INFINITY).intValue();
  private int  maxLatency = new Double(NEGATIVE_INFINITY).intValue();
  private int sumLatency = 0;
  private int numTotalRecord = 0;
  private int[] latencyGroupCount = new int[NUM_BUCKET];
  private float bucketSize;

  private Map<Integer, Integer> startTimeGroupCount = new HashMap<>();



  public int getMinLatency() {
    return minLatency;
  }

  public void setMinLatency(int minLatency) {
    this.minLatency = minLatency;
  }

  public int getMaxLatency() {
    return maxLatency;
  }

  public void setMaxLatency(int maxLatency) {
    this.maxLatency = maxLatency;
  }

  public int getSumLatency() {
    return sumLatency;
  }

  public void setSumLatency(int sumLatency) {
    this.sumLatency = sumLatency;
  }

  public int getNumTotalRecord() {
    return numTotalRecord;
  }

  public void setNumTotalRecord(int numTotalRecord) {
    this.numTotalRecord = numTotalRecord;
  }

  public void incrementLatencyGroupCount(int latency) {
      this.bucketSize = (float)(this.maxLatency - this.minLatency) / NUM_BUCKET;
      int groupIdx = (int) Math.floor((latency - this.minLatency) / this.bucketSize);
      if (groupIdx == NUM_BUCKET) { // latency  == maxLatency
        this.latencyGroupCount[groupIdx-1] += 1;
      } else {
        this.latencyGroupCount[groupIdx] += 1;
      }
      System.out.println("bucketSize:"+this.bucketSize + " groupIdx:" + groupIdx);

  }

  public int[] getLatencyGroupCount() {
    return this.latencyGroupCount;
  }

  public float getBucketSize() {
    if (this.maxLatency == new Double(NEGATIVE_INFINITY).intValue()) {
      throw new RuntimeException("Max & Min Latency haven't been set yet");
    }
    return this.bucketSize;
  }


  public void incrementStartTimeGroupCount(long programStartTime, long recordStartTime) {
    int second = (int) Math.floor((recordStartTime - programStartTime) / 1000f);
    if (this.startTimeGroupCount.containsKey(second)) {
      this.startTimeGroupCount.put(second, this.startTimeGroupCount.get(second) + 1);
    } else {
      this.startTimeGroupCount.put(second, 1);
    }
  }

  public Map<Integer, Integer> getStartTimeGroupCount() {
    return this.startTimeGroupCount;
  }
}
