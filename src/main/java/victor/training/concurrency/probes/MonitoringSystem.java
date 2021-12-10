package victor.training.concurrency.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;

public class MonitoringSystem {
   private static final Logger log = LoggerFactory.getLogger(MonitoringSystem.class);
   private final Probes probes;
   private final Plotter plotter;
   private final ExecutorService pool = new ThreadPoolExecutor(
       1, 1,
       1, TimeUnit.HOURS,
//       new LinkedBlockingDeque<>()
       new ArrayBlockingQueue<>(40 / 5),
//       new SynchronousQueue<>(),
       new DiscardOldestPolicy()
   );

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

   //   List<Sample> sampleBufferToSend = new ArrayList<>();
   final Queue<Sample> queue = new LinkedList<>();

   // is called on multiple threads by Probes driver
   public void receive(String device, int value) {
      Sample sample = new Sample(LocalTime.now(), device, value);
      System.out.println("In receive");
      System.out.println(Thread.currentThread().getName());

      synchronized (queue) {
         queue.offer(sample);
         if (queue.size() > 40) {
            queue.poll();
         }
         if (queue.size() == 5) {
            List<Sample> clone = new ArrayList<>(queue);
            queue.clear();

            pool.submit(() -> submitTask(clone));
         }
      }

      probes.requestMetricFromProbe(device);
   }

   private void submitTask(List<Sample> pageOfFive) {
      this.plotter.sendToPlotter(pageOfFive); // 100ms
      synchronized (queue) {
         if (queue.size() >= 5) {
            List<Sample> pageToSend = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
               pageToSend.add(queue.poll());
            }
            pool.submit(() -> submitTask(pageToSend));
         }
      }
   }
}
