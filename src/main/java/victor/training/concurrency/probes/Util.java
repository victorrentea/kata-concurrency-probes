package victor.training.concurrency.probes;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {
   static Random random = new Random();

   public static int randomInt(int min, int max) {
      if (min == max) {
         return min;
      }
      return min + random.nextInt(max - min);
   }

   /**
    * Sleeps quietly (without throwing a checked exception)
    */
   public static void sleepq(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(e);
      }
   }

   public static String formatSize(long usedHeapBytes) {
      return String.format("%,d B", usedHeapBytes);
   }

   public static long getUsedHeapBytes() {
      return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
   }


   static class ThreadNamingFactory implements ThreadFactory {
      private final String baseName;
      private static final AtomicInteger counter = new AtomicInteger(0);

      ThreadNamingFactory(String baseName) {
         this.baseName = baseName;
      }

      @Override
      public Thread newThread(Runnable r) {
         Thread thread = new Thread(r);
         thread.setName(baseName + "-" + counter.incrementAndGet());
         return thread;
      }
   }

}
