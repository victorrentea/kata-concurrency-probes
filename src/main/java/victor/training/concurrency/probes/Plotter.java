package victor.training.concurrency.probes;

import java.util.List;

public interface Plotter {
   void sendToPlotter(List<Sample> samples);
}
