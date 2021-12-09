package victor.training.concurrency.probes;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import victor.training.concurrency.probes.ProbesFake.ValueAndDelay;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodName.class)
class MonitoringSystemTest {
   private static final Logger log = LoggerFactory.getLogger(MonitoringSystemTest.class);
   public static final int EARLY_VALUE = 0;
   public static final int LATE_VALUE = 1;

   @Test // see readme
   void step1() {
      List<ValueAndDelay> responses = new ArrayList<>();
      for (int i = 0; i < 10; i++) responses.add(new ValueAndDelay(i, 5));
      PlotterFake plotter = new PlotterFake(10);
      MonitoringSystem target = new MonitoringSystem(new ProbesFake(responses), plotter);

      target.start(List.of("probe1"));

      awaitUntilValueStabilizes(plotter::getReceivedValues, ofMillis(500));

      System.out.println(plotter.getReceivedValues());
      assertThat(plotter.getReceivedValues()).isEqualTo(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
   }

   @Test // see readme
   void step2_3probes() {
      List<ValueAndDelay> responses = new ArrayList<>();
      for (int i = 0; i < 10; i++) responses.add(new ValueAndDelay(i, 5));
      PlotterFake plotter = new PlotterFake(10);
      MonitoringSystem target = new MonitoringSystem(new ProbesFake(responses), plotter);

      target.start(List.of("probe1", "probe2", "probe3"));

      awaitUntilValueStabilizes(plotter::getReceivedValues, ofMillis(500));

      System.out.println(plotter.getReceivedValues());
      assertThat(plotter.getReceivedValues()).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
   }
   @Test // see readme
   void step3_dontBlock() {
      List<ValueAndDelay> responses = new ArrayList<>();
      for (int i = 0; i < 40; i++) responses.add(new ValueAndDelay(i, 5));
      PlotterFake plotter = new PlotterFake(10);
      plotter.detectBlockingInProbeThread = true;
      MonitoringSystem target = new MonitoringSystem(new ProbesFake(responses), plotter);

      target.start(List.of("probe1", "probe2", "probe3"));

      awaitUntilValueStabilizes(plotter::getReceivedValues, ofMillis(500));

      System.out.println(plotter.getReceivedValues());
      assertThat(plotter.getReceivedValues()).hasSize(40);
   }


   @Test
   void step4_discardsOldSamplesOnOverflow() {
      List<ValueAndDelay> responses = new ArrayList<>();
      for (int i = 0; i < 41; i++) responses.add(new ValueAndDelay(EARLY_VALUE, 5));
      for (int i = 0; i < 40; i++) responses.add(new ValueAndDelay(LATE_VALUE, 5));
      PlotterFake plotter = new PlotterFake(100);
      plotter.detectBlockingInProbeThread = true;
      MonitoringSystem target = new MonitoringSystem(new ProbesFake(responses), plotter);

      target.start(List.of("probe1", "probe2", "probe3"));

      awaitUntilValueStabilizes(plotter::getReceivedValues, ofMillis(500));

      System.out.println(plotter.getReceivedValues());
      try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
         softly.assertThat(plotter.getReceivedValues().stream().filter(v1 -> v1 == EARLY_VALUE).count())
             .describedAs("Older values should have been discarded, proving eviction")
             .isLessThan(39);
         softly.assertThat(plotter.getReceivedValues().stream().filter(v -> v == LATE_VALUE).count())
             .describedAs("Recent values should have been emitted")
             .isGreaterThan(37);
      }
   }
   @Test
   void step5_pagesToPlotter() {
      List<ValueAndDelay> responses = new ArrayList<>();
      for (int i = 0; i < 41; i++) responses.add(new ValueAndDelay(EARLY_VALUE, 5));
      for (int i = 0; i < 40; i++) responses.add(new ValueAndDelay(LATE_VALUE, 5));
      PlotterFake plotter = new PlotterFake(100);
      plotter.detectBlockingInProbeThread = true;
      plotter.plotterAcceptsOnlyPages = true;
      MonitoringSystem target = new MonitoringSystem(new ProbesFake(responses), plotter);

      target.start(List.of("probe1", "probe2", "probe3"));

      awaitUntilValueStabilizes(plotter::getReceivedValues, ofMillis(500));

      assertThat(plotter.getReceivedValues().size()).isGreaterThan(40);
   }

   private void awaitUntilValueStabilizes(Supplier<List<Integer>> valueSupplier, Duration duration) {
      AtomicInteger lastValue = new AtomicInteger(0);
      Awaitility.await()
          .pollDelay(ofSeconds(1))
          .pollInterval(duration)
          .timeout(ofSeconds(20))
          .until(() -> {
             int newReceivedCount = valueSupplier.get().size();
             return newReceivedCount == lastValue.getAndSet(newReceivedCount);
          });
   }
}