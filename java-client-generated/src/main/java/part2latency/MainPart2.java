package part2latency;


import config.LoadTestConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class MainPart2 {
  private static final int QUEUE_CAPACITY = LoadTestConfig.NUM_THREADS;
  private static final String ALL_RECORDS_CSV = "AllRecords.csv";
  private static final String START_TIME_GROUP_CSV = "StartTimeGroupedRequests.csv";
  private static final RunningMetrics runningMetrics = new RunningMetrics();

  private static Long startTime, endTime;

  public static void main(String[] args) throws InterruptedException, CsvExistException {
    CountDownLatch latch = new CountDownLatch(LoadTestConfig.NUM_THREADS);
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);

    BlockingQueue<List<Record>> recordsBuffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    startTime = System.currentTimeMillis();
    endTime = null;

    if (new File(ALL_RECORDS_CSV).isFile()) {
      throw new CsvExistException(ALL_RECORDS_CSV);
    }

    for (int i = 0; i < LoadTestConfig.NUM_THREADS; i++) {
      Runnable thread = new SendRequestAverageThread(latch, numSuccessfulReqs, numFailedReqs, recordsBuffer);
      new Thread(thread).start();
    }


    int numRecordListsTaken = 0;
    List<Record> inMemoryAllRecords = new ArrayList<>();
    while (numRecordListsTaken < LoadTestConfig.NUM_THREADS || endTime == null) {
      if (recordsBuffer.size() > 0) {
        // take from buffer
        List<Record> threadRecords = recordsBuffer.take(); // Might throw InterruptedException
        numRecordListsTaken ++;
        // Iterate through each record: Update, max, min, sum; increment count to time group (starting at which second)
        updateRunningMetrics(threadRecords);
        writeAllRecords(threadRecords);
        // inMemoryAllRecords.addAll(threadRecords); // FOR TESTING PURPOSE
      } else if  (latch.getCount() == 0) {
        // Mark endTime
        endTime = System.currentTimeMillis();
      }
    }

    // endTime might contain one unnecessary FileWrite time.
    // CASE1:
    // When the Last thread put a list to queue and call latch.countDown(),
    // if main thread happens to be in the latch block, then endTime will be accurately marked,
    // and then main thread goes on to take the last list from queue. END while loop.

    // CASE2:
    // When the Last thread put a list to queue and call latch.countDown().
    // if main thread happens to be in the recordsBuffer block, then main thread will
    // take the last list, WRITE TO CSV, and then mark endTime.
    // In this case, the endTime will be longer than the actual request-sending endTime, by a difference of
    // WRITE_ONE_LIST_TO_CSV TIME.

    // Group records by latency bucket
    readCsvToGroupLatency();

    // Write the startTimeGroupCount out to CSV for plotting
    writeToCsvByStartTime(runningMetrics.getStartTimeGroupCount());


    float wallTime = (endTime - startTime)/1000f;
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);
    System.out.println("Number of Threads: " + LoadTestConfig.NUM_THREADS);
    System.out.println("Multi-thread wall time:" + wallTime + "s");
    System.out.println("Throughput: " + numSuccessfulReqs.get() / wallTime + " req/s");
    System.out.println("\n");

    System.out.println("Mean Response Time (ms): " + (float)runningMetrics.getSumLatency() / runningMetrics.getNumTotalRecord());
    System.out.println("Median Response Time (ms): " + runningMetrics.calPercentileLatency(50));
    System.out.println("Throughput (req/s): " + (float)(numSuccessfulReqs.get()) / (endTime - startTime) * 1000);
    System.out.println("99th Percentile Response Time: " + runningMetrics.calPercentileLatency(99));
    System.out.println("Min Response Time (ms): " + runningMetrics.getMinLatency());
    System.out.println("Max Response Time (ms): " + runningMetrics.getMaxLatency());

    // TEST:
    // testCalcStatistics(inMemoryAllRecords);

  }

  private static void updateRunningMetrics(List<Record> records) {
    for (Record record: records) {
      if (record.getResponseCode() == LoadTestConfig.SUCCESS_CODE) {
        int curLatency = record.getLatency();
        runningMetrics.setMinLatency(Math.min(curLatency, runningMetrics.getMinLatency()));
        runningMetrics.setMaxLatency(Math.max(curLatency, runningMetrics.getMaxLatency()));
        runningMetrics.setSumLatency(runningMetrics.getSumLatency() + curLatency);
        runningMetrics.setNumTotalRecord(runningMetrics.getNumTotalRecord() + 1);
        runningMetrics.incrementStartTimeGroupCount(startTime, record.getStartTime());
      }
    }
  }

  private static void writeAllRecords(List<Record> records) {
    try (BufferedWriter outputFile = new BufferedWriter(new FileWriter(ALL_RECORDS_CSV, true))) {
      String line;

      for (Record record: records) {
        line = record.toString();
        outputFile.write(line + System.lineSeparator());
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("*** Write: CSV file was not found : " + fnfe.getMessage());
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      System.out.println("Error when writing to CSV " + ALL_RECORDS_CSV + ": " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  private static void readCsvToGroupLatency() {
    try (BufferedReader inputFile = new BufferedReader(new FileReader(ALL_RECORDS_CSV))) {

      String line;
      while ((line = inputFile.readLine()) != null) {
        String[] record = line.split(",");
        if (Integer.valueOf(record[3]).equals(LoadTestConfig.SUCCESS_CODE)) {
          int latency = Integer.valueOf(record[2]);
          runningMetrics.incrementLatencyGroupCount(latency);
        }
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("*** Read: CSV file was not found : " + fnfe.getMessage());
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      System.out.println("Error when reading CSV " + ALL_RECORDS_CSV + ": " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }


  private static void writeToCsvByStartTime(Map<Integer, Integer> startTimeGroupCount) {
    Map<Integer, Integer> sortedMap = new TreeMap<>(startTimeGroupCount);  // sort by key(second from programStartTime)

    try (BufferedWriter outputFile = new BufferedWriter(new FileWriter(START_TIME_GROUP_CSV))) {
      String line;

      for (Integer second: sortedMap.keySet()) {
        line = second + "," + sortedMap.get(second);
        outputFile.write(line + System.lineSeparator());
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("*** CSV file was not found : " + fnfe.getMessage());
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      System.out.println("Error when writing to CSV : " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  /**
   * Ues InMemory Collection & DescriptiveStatistics class
   * to calculate the correct statistics (Only for TESTING PURPOSE)
   */
  private static void testCalcStatistics( List<Record> inMemoryAllRecords) {

    DescriptiveStatistics ds = new DescriptiveStatistics();

    for (Record record: inMemoryAllRecords) {
      ds.addValue(record.getLatency());
    }
    System.out.println("====== TEST: Statistics calculated with in memory collection and DescriptiveStatistics ========");
    System.out.println("Mean Response Time (ms): " + ds.getMean());
    System.out.println("Median Response Time (ms): "+ds.getPercentile(50));
    System.out.print("99th Percentile Response Time: " + ds.getPercentile(99));
    System.out.println("Min Response Time (ms): "+ds.getMin());
    System.out.println("Max Response Time (ms): " + ds.getMax());
  }

}
