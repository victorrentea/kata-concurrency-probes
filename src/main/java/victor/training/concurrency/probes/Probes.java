package victor.training.concurrency.probes;

import java.util.function.BiConsumer;

public interface Probes {
   void setReceiveFunction(BiConsumer<String, Integer> receiveFunction);

   void requestMetricFromProbe(String probe);
}
