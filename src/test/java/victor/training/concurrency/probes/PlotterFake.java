package victor.training.concurrency.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class PlotterFake implements Plotter {
   private static final Logger log = LoggerFactory.getLogger(PlotterFake.class);
   private final List<String> callingThreads = new ArrayList<>();
   private final List<Integer> receivedValues = new ArrayList<>();
   private final int blockingMillis;
   public boolean detectBlockingInProbeThread = false;
   public boolean plotterAcceptsOnlyPages = false;

   public PlotterFake(int blockingMillis) {
      this.blockingMillis = blockingMillis;
   }

   @Override
   public void sendToPlotter(List<Sample> samples) {
      log.debug("SEND " + samples + " to plotter ... ");
      checkReceivedPage(samples);
      synchronized (callingThreads) {
         callingThreads.add(Thread.currentThread().getName());
         if (callingThreads.size() >= 2) {
            System.err.println("PLOTTER ERROR: received more than 1 parallel requests, from threads: " + callingThreads);
            System.err.println("STOP PROCESS");
            System.exit(1);
         }
      }
      if (detectBlockingInProbeThread) {
         if (Thread.currentThread().getName().startsWith("probe")) {
            throw new IllegalArgumentException("You should not block in the plotter thread");
         }
      }
      checkSamplesInOrder(samples);
      Util.sleepq(blockingMillis);
      receivedValues.addAll(samples.stream().map(Sample::getValue).collect(toList()));
      log.debug("SEND done");
      synchronized (callingThreads) {
         callingThreads.remove(Thread.currentThread().getName());
      }
   }

   public List<Integer> getReceivedValues() {
      return receivedValues;
   }

   private void checkReceivedPage(List<Sample> samples) {
      if (plotterAcceptsOnlyPages) {
         if (samples.size() != 5) {
            System.err.println("PLOTTER ERROR: received a page of size != 5: " + samples);
            System.err.println("STOP PROCESS");
            System.exit(2);
         }
      }
   }
   private LocalTime lastSampleTime = LocalTime.now();
   private void checkSamplesInOrder(List<Sample> samples) {
      for (Sample sample : samples) {
         if (lastSampleTime.isAfter(sample.getTimestamp())) {
            System.err.println("PLOTTER ERROR: Samples received out of order.\nOffending timestamp:"+ sample.getTimestamp() + "\nis before\nlast timestamp:"+lastSampleTime);
            System.err.println("STOP PROCESS");
            System.exit(3);
         }
         lastSampleTime = sample.getTimestamp();
      }
   }
}
