package victor.training.concurrency.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class MonitoringSystem {
   private static final Logger log = LoggerFactory.getLogger(MonitoringSystem.class);
   private final Probes probes;
   private final Plotter plotter;

   public MonitoringSystem(Probes probes, Plotter plotter) {
      this.probes = probes;
      this.plotter = plotter;
      probes.setReceiveFunction(this::receive);
   }

   public void start(List<String> probeNames) {
      log.debug("START " + probeNames);
      for (String probeName : probeNames) {
         probes.requestMetricFromProbe(probeName);
      }
   }

   // is called on multiple threads by Probes driver
   public synchronized void receive(String device, int value) {
      Sample sample = new Sample(LocalTime.now(), device, value);
      System.out.println("In receive");
      System.out.println(Thread.currentThread().getName());
      this.plotter.sendToPlotter(List.of(sample));
      probes.requestMetricFromProbe(device);
   }
}
