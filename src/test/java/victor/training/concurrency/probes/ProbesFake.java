package victor.training.concurrency.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import victor.training.concurrency.probes.Util.ThreadNamingFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ProbesFake implements Probes {
   private static final Logger log = LoggerFactory.getLogger(ProbesFake.class);
   private static final ScheduledExecutorService replyPool = Executors.newScheduledThreadPool(100,
      new ThreadNamingFactory("probe"));
   private BiConsumer<String, Integer> receiveFunction;

   @Override
   public void setReceiveFunction(BiConsumer<String, Integer> receiveFunction) { // needed due to cyclic dependency
      this.receiveFunction = receiveFunction;
   }

   private final Deque<ValueAndDelay> responses;

   public ProbesFake(List<ValueAndDelay> responses) {
      this.responses = new ArrayDeque<>(responses);
   }

   private final AtomicInteger sampleNo = new AtomicInteger();

   @Override
   public void requestMetricFromProbe(String probe) {
      ValueAndDelay response = responses.poll();
      replyPool.schedule(() -> replyFromProbe(probe, response), response.delayMillis, MILLISECONDS);
   }

   private void replyFromProbe(String probe, ValueAndDelay response) {
      log.debug("{} sends sample #{}: {}", probe, sampleNo.incrementAndGet(), response.value);
      receiveFunction.accept(probe, response.value);
   }


   public static class ValueAndDelay {
      private final int value;
      private final int delayMillis;

      public ValueAndDelay(int value, int delayMillis) {
         this.value = value;
         this.delayMillis = delayMillis;
      }

      public int getDelayMillis() {
         return delayMillis;
      }

      public int getValue() {
         return value;
      }
   }

}
