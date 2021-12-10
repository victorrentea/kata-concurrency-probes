
You have to monitor a series of probes and report the data to a plotter. Since this is hardware, you have to request the data to the probe by calling
probes.requestMetricFromProbe(probeName);

After that, the probe driver calls you back the method `MonitoringSystem.receive` on another thread.

Once that is called you have to call again `requestMetricFromProbe` to request data from the probe.

- Step1: Warmup
  * You have 1 probe: make sure you receive data and request a new sample immediately
  * Send every sample immediately to plotter (as a size-one list), from the same thread where the `request()` method runs
  * Rule: Plotter should receive the samples in their chronological order

- Step2: You have to support 3 probes
  - Rule: Plotter should only be sent one request at a time each time with a one-element list
    - Task: violate this rule and observe the exception
    - [Pro]: study how the detection of this case is implemented

- Step3: Request as many samples possible from the probe, as fast as possible 
  - That is: you are NOT allowed to block in the thread running the `receive()`
  - Hint: You'll need your own thread pool. What kind? 
    - `Executors.newCachedThreadPool()` creating unbounded threads or 
    - `Executors.newFixedThreadPool(N)` creating a fixed number of threads; but `N`=?
  
- Step4: For memory efficiency, you should not keep more than 40 samples in memory
  - If too many samples arrive, discard the oldest (make the test pass)
  
- Step5: For network efficiency, Plotter now only accepts pages of 5 samples at a time
    - Hint: how do you accumulate a full page between threads?
    - IMPORTANT: don't mind about the previous test
- [HARD] Make the test still pass -> manual backpressure implementation
  - Hint: a queue of pages is too coarse-grained
  
