package part2latency;



import config.LoadTestConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class Main {
  private static final int QUEUE_CAPACITY = LoadTestConfig.NUM_THREADS;
  private static final String CSV_FILE_NAME = "AllRecords.csv";
  private static final RunningMetrics runningMetrics = new RunningMetrics();

  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(LoadTestConfig.NUM_THREADS);
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);

    BlockingQueue<Pair> recordsBuffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    Long startTime = System.currentTimeMillis();
    Long endTime = null;

    for (int i = 0; i < LoadTestConfig.NUM_THREADS; i++) {
      Runnable thread = new SendRequestAverageThread(latch, numSuccessfulReqs, numFailedReqs, recordsBuffer);
      new Thread(thread).start();
    }


    int numRecordListsTaken = 0;
    List<Pair> inMemoryAllRecords = new ArrayList<>();
    // Map<String, Integer> numWrittenRecords = new HashMap<>();
    while (numRecordListsTaken < LoadTestConfig.NUM_THREADS || endTime == null) {
      if (recordsBuffer.size() > 0) {
        // take from buffer
        Pair threadRecords = recordsBuffer.take(); // Might throw InterruptedException
        numRecordListsTaken ++;
        // System.out.println("Taken thread records from queue: " + threadRecords.getThreadId() + ": " + threadRecords.getRecords().size());
        // For each record: Update, max, min, sum; Add frequency to corresponding latency group
        updateRunningMetrics(threadRecords.getRecords());
        inMemoryAllRecords.add(threadRecords);
        // numWrittenRecords.put(threadRecords.getThreadId(),
        writeAllRecords(threadRecords.getThreadId(),threadRecords.getRecords());

      } else if  (latch.getCount() == 0) { // TODO: getCount typically used for testing purpose??
        // Mark endTime
        endTime = System.currentTimeMillis();
      }
    }

    readCsvToGroup();
    // endTime might contain one unnecessary FileWrite time.
    // CASE1:
    // When the Last thread put a list to queue and call latch.countDown(),
    // if main thread happens to be in the wallTimeLatch block, then endTime will be accurately marked,
    // and then main thread goes on to take the last list from queue. END while loop.

    // CASE2:
    // When the Last thread put a list to queue and call latch.countDown().
    // if main thread happens to be in the recordsBuffer block, then main thread will
    // take the last list, WRITE TO CSV, and then mark endTime.
    // In this case, the endTime will be longer than the actual request-sending endTime, by a difference of
    // WRITE_ONE_LIST_TO_CSV TIME.

    System.out.println("Num of Threads: " + LoadTestConfig.NUM_THREADS);
//    System.out.println("StartTime: " + startTime);
//    System.out.println("EndTime: " + endTime);
    System.out.println("Multi-threaded Wall Time:" + (endTime - startTime) + "ms");
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);



    System.out.println("Mean Response Time (ms): " + (float)runningMetrics.getSumLatency() / runningMetrics.getNumTotalRecord());
    System.out.println("Median Response Time (ms): " + calPercentileLatency(50));
    System.out.println("Throughput (req/s): " + (float)(numSuccessfulReqs.get()) / (endTime - startTime) * 1000);
    System.out.println("99th Percentile Response Time: " + calPercentileLatency(99));
    System.out.println("Min Response Time (ms): " + runningMetrics.getMinLatency());
    System.out.println("Max Response Time (ms): " + runningMetrics.getMaxLatency());


    int[] latencyGroupCount = runningMetrics.getLatencyGroupCount();
    for (int i = 0; i < RunningMetrics.NUM_BUCKET; i++) {
      System.out.println("GroupId: " + i + "; " + latencyGroupCount[i]);
    }
    System.out.println(runningMetrics.getLatencyGroupCount());

    /**
     * Ues InMemory Collection & DescriptiveStatistics class
     * to calculate the correct statistics (for testing)
     */
    DescriptiveStatistics ds = new DescriptiveStatistics();

    for (Pair pair: inMemoryAllRecords) {
      for (Record record : pair.getRecords()) {
        ds.addValue(record.getLatency());
      }
    }
    System.out.println(ds.getMean());
    System.out.println(ds.getMax());
    System.out.println(ds.getMin());
    System.out.println(ds.getPercentile(50));
    System.out.print(ds.getPercentile(99));

//    // Calculate inMemory Collection's number of records
//    int inMemoryTotalRecords = 0;
//    for (Pair pair: inMemoryAllRecords) {
//      inMemoryTotalRecords += pair.getRecords().size();
//    }


//    try (BufferedWriter outputFile = new BufferedWriter(new FileWriter("AllRecordsFromMemory.csv"))){
//
//      String line;
//      for (Pair pair: inMemoryAllRecords) {
//        for (Record record: pair.getRecords()) {
//          line = record.toString() + "," + pair.getThreadId();
//          outputFile.write(line + System.lineSeparator());
//        }
//        System.out.println("From Memory Collection: Finished writing records of thread:" + pair.getThreadId());
//
//      }
//    } catch (FileNotFoundException fnfe) {
//      System.out.println("*** CSV file was not found : " + fnfe.getMessage());
//      fnfe.printStackTrace();
//    } catch (IOException ioe) {
//      System.out.println("Error when writing to CSV : " + ioe.getMessage());
//      ioe.printStackTrace();
//    }


    // System.out.println("Write from queue, written record cnt(each thread): " + numWrittenRecords);
    // System.out.println("Write from memory collection, written record cnt(all threads):" + inMemoryTotalRecords);

  }

  private static void updateRunningMetrics(List<Record> records) {
    for (Record record: records) {
      if (record.getResponseCode() == LoadTestConfig.SUCCESS_CODE) {
        int curLatency = record.getLatency();
        runningMetrics.setMinLatency(Math.min(curLatency, runningMetrics.getMinLatency()));
        runningMetrics.setMaxLatency(Math.max(curLatency, runningMetrics.getMaxLatency()));
        runningMetrics.setSumLatency(runningMetrics.getSumLatency() + curLatency);
        runningMetrics.setNumTotalRecord(runningMetrics.getNumTotalRecord() + 1);
      }
    }
  }

  // TODO: Move to RunningMetrics class
  private static float calPercentileLatency(int percentile) {
    int[] latencyGroupCount = runningMetrics.getLatencyGroupCount();
    int minLatency = runningMetrics.getMinLatency();
    int numTotalRecord = runningMetrics.getNumTotalRecord();
    float bucketSize = runningMetrics.getBucketSize();

    float accumulatePercentage = 0;
    int i = -1;
    System.out.println("accup: " + accumulatePercentage + "targetPercentile:" +  percentile/100f);
    while (accumulatePercentage < percentile / 100f) {
      accumulatePercentage += (float) latencyGroupCount[++i] / numTotalRecord;
    }
    // i = 0: target percentile value falls in the 0th index group
    System.out.println("i:" + i);
    float lower = minLatency + bucketSize * i;
    float upper = lower + bucketSize;

    // Assume even distribution:
    // (upper - pLatency) / bucketSize =
    // (accumulatePercentage - percentile/100) / (latencyGroupCount[i] / numTotalRecord)

    float pLatency = upper - bucketSize * (accumulatePercentage - percentile/100f) / ((float)latencyGroupCount[i] / numTotalRecord);
    return pLatency;
  }

  // TESTING PURPOSE
  private static void writeAllRecords(String threadId, List<Record> records) {
    System.out.println("From Queue: Started writing records of thread:" + threadId);
//    int numRecordsInThread = 0;

    try (BufferedWriter outputFile = new BufferedWriter(new FileWriter(CSV_FILE_NAME, true))) {
      String line;

      for (Record record: records) {
        line = record.toString() + "," + threadId;
        outputFile.write(line + System.lineSeparator());
//        numRecordsInThread ++;
      }


    } catch (FileNotFoundException fnfe) {
      System.out.println("*** Write: CSV file was not found : " + fnfe.getMessage());
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      System.out.println("Error when writing to CSV : " + ioe.getMessage());
      ioe.printStackTrace();
    }

    System.out.println("From Queue: Finished writing records of thread:" + threadId);
//    return numRecordsInThread;

  }

  private static void readCsvToGroup() {
    try (BufferedReader inputFile = new BufferedReader(new FileReader(CSV_FILE_NAME))) {

      String line;
      while ((line = inputFile.readLine()) != null) {
        String[] record = line.split(",");
        if (Integer.valueOf(record[3]).equals(LoadTestConfig.SUCCESS_CODE)) {
          int latency = Integer.valueOf(record[2]);
          System.out.println("latency:" + latency);
          System.out.println("max, min:" + runningMetrics.getMaxLatency() + " " + runningMetrics.getMinLatency());
          runningMetrics.incrementLatencyGroupCount(latency);
        }
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("*** Read: CSV file was not found : " + fnfe.getMessage());
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      System.out.println("Error when reading CSV : " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }
    //  private void writeToCSVByStartTime(List<Record> records) {
//    try (BufferedReader inputFile =new BufferedReader(new FileReader("country_codes.csv"));
//        BufferedWriter outputFile = new BufferedWriter(new FileWriter("country_code.out.csv"))) {
//      List<String> lines
//      inputFile
//
//    } catch (FileNotFoundException fnfe) {
//      System.out.println("*** CSV file was not found : " + fnfe.getMessage());
//      fnfe.printStackTrace();
//    } catch (IOException ioe) {
//      System.out.println("Error when writing to CSV : " + ioe.getMessage());
//      ioe.printStackTrace();
//    }
//
//  }


  //  List<String> lines = Files.readAllLines(file.toPath());
//lines.set(line, dataType.toUpperCase() + ":" + newData);
//Files.write(file.toPath(), lines); // You can add a charset and other options too




//  I suggest reading the whole file into a variable (string, array or list). You can easily modify specific lines then and write everything back to the file.
//
//      1) Reading File into an array: Store text file content line by line into array
//
//2) Manipulate array:
//
//      for (int i = 0; i < lines.length; i++) {
//    String line = lines[i];
//    if (line.contains("Done")) {
//      lines[i] = line + "\nCHECK!\n";
//      break;
//    }
//  }
//3) Write string array to file: Writing a string array to file using Java - separate lines
//
//  The problem also can be related to this line:
//
//      writer.write("\nCHECK!\n");
//  the first "\n" forces the "CHECK" to be written in a new line. This assumes that your "Done" line is the last line and does not end with "\n".
}
