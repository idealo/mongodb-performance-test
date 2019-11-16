package de.idealo.mongodb.perf;

/**
 * Created by kay.agahd on 23.11.16.
 */

import com.codahale.metrics.*;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.perf.operations.IOperation;
import de.idealo.mongodb.perf.operations.InsertOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;

public class OperationExecutor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(OperationExecutor.class);

    public static final String TIMER_PER_SECOND_PREFIX = "stats-per-second-";
    public static final String TIMER_PER_RUN_PREFIX = "stats-per-run-";

    private final int threadCount;
    private final long opsCount;
    private final long maxDurationInSeconds;
    private final Timer timerPerSecond;
    private final Timer timerPerRun;
    private final ConsoleReporter consoleReporterPerSecond;
    private final ConsoleReporter consoleReporterPerRun;
    private final CsvReporter csvReporterPerSecond;
    private final CsvReporter csvReporterPerRun;
    private final IOperation operation;
    private final File csvFolder;
    private final CountDownLatch runModeLatch;
    private final String timerPerSecondName;
    private final String timerPerRunName;

    public OperationExecutor(int threadCount, long opsCount, long maxDurationInSeconds, IOperation operation, CountDownLatch runModeLatch){
        LOG.info(">>> OperationExecutor threadCount: {}, opsCount: {}, maxDurationInSeconds: {}, operation: {}", threadCount, opsCount, maxDurationInSeconds, operation.getClass().getSimpleName());
        this.csvFolder = getJarLocation();
        this.threadCount = threadCount;
        this.opsCount = opsCount;
        this.maxDurationInSeconds = maxDurationInSeconds;
        this.operation = operation;
        this.runModeLatch = runModeLatch;
        this.timerPerSecondName = TIMER_PER_SECOND_PREFIX + operation.getOperationMode();
        this.timerPerRunName = TIMER_PER_RUN_PREFIX + operation.getOperationMode();
        final MetricRegistry registry = new MetricRegistry();
        final MetricRegistry registryAll = new MetricRegistry();
        //timer1 = registry.register("dbTimer", new Timer(new SlidingWindowReservoir((int)opsCount)));
        timerPerSecond = registry.register(timerPerSecondName, new Timer(new SlidingTimeWindowReservoir(1, TimeUnit.SECONDS)));
        timerPerRun = registryAll.timer(timerPerRunName);
        consoleReporterPerSecond = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        csvReporterPerSecond = CsvReporter.forRegistry(registry)
                .formatFor(java.util.Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(csvFolder);
        consoleReporterPerRun = ConsoleReporter.forRegistry(registryAll)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        csvReporterPerRun = CsvReporter.forRegistry(registryAll)
                .formatFor(java.util.Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(csvFolder);

        consoleReporterPerSecond.start(1, TimeUnit.SECONDS);

        csvReporterPerSecond.start(1, TimeUnit.SECONDS);

    }

    public void doOperation(int threadId, long threadRunCount, long globalRunCount) {
        final Timer.Context context = timerPerSecond.time();
        final Timer.Context allContext = timerPerRun.time();
        try {
            operation.operation(threadId, threadRunCount, globalRunCount);
        } finally {
            context.stop();
            allContext.stop();
        }
    }

    @Override
    public void run(){
        try {
            executeThreads();
        } catch (InterruptedException e) {
            LOG.error("InterruptedException while executing threads", e);
        }
        analysis();
        stopReporters();
    }

    private File getJarLocation(){
        try {
            final String s = OperationExecutor.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            LOG.info("java execution path: {}", s);
            return new File(s).getParentFile();
        } catch (URISyntaxException e) {
            LOG.error("Error while getting location of java class file", e);
        }
        return new File(".");
    }

    private void executeThreads() throws InterruptedException {


        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final long start = System.currentTimeMillis();
        final AtomicLong runCounter = new AtomicLong(0L);

        range(0, threadCount).forEach(t -> executor.submit(() -> {
                            try {
                                startGate.await();
                                int count=1;
                                while((runCounter.get() < opsCount || opsCount==0) && runModeLatch.getCount()>0){//if opsCount==0 then it terminates when maxDurationInSeconds is reached
                                    doOperation(t+1, count++, runCounter.incrementAndGet());
                                }


                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                endGate.countDown();
                            }
                        }));


        startGate.countDown();

        final boolean notTimedOut = endGate.await(maxDurationInSeconds, SECONDS);
        final long end = System.currentTimeMillis();
        final long durationInMs = end-start;


        LOG.info("Done ({}) in {} ms ", notTimedOut ? "in time" : "timed out", durationInMs);
        runModeLatch.countDown();
        endGate.await();//if maxDurationInSeconds was reached, threads are still running by working on connections that will be closed right now, so wait until all threads have stopped to avoid trying to use closed connections, which would throws errors
        executor.shutdownNow();
    }

    private void stopReporters() {
        LOG.info("stopReporters");
        if(consoleReporterPerSecond != null) {
            consoleReporterPerSecond.stop();
            LOG.info("consoleReporter stopped");
        };
        if(consoleReporterPerRun != null) {
            consoleReporterPerRun.stop();
            LOG.info("allConsoleReporter stopped");
        };
        if(csvReporterPerSecond != null) {
            csvReporterPerSecond.stop();
            LOG.info("csvReporter stopped");
            LOG.info("If run took more than 1 second, csv-file '{}.csv' saved in folder: {}", timerPerSecondName, csvFolder.getAbsolutePath());
        };
        if(csvReporterPerRun != null) {
            csvReporterPerRun.stop();
            LOG.info("csvReporterAll stopped");
            LOG.info("Summary of statistics saved in csv-file '{}.csv' located in folder: {}", timerPerRunName, csvFolder.getAbsolutePath());
        };

    }

    private void analysis() {
        TimeUnit durationUnit = TimeUnit.MILLISECONDS;
        double durationFactor = 1.0 / durationUnit.toNanos(1);

        LOG.info("affected documents: {}", operation.getAffectedDocuments());
        LOG.info("count: {}", timerPerRun.getCount());
        LOG.info("FifteenMinuteRate: {}", timerPerRun.getFifteenMinuteRate());//operations per second during 15 minutes
        LOG.info("FiveMinuteRate: {}", timerPerRun.getFiveMinuteRate());//operations per second during 5 minutes
        LOG.info("OneMinuteRate: {}", timerPerRun.getOneMinuteRate());//operations per second during 1 minute
        LOG.info("MeanRate: {}", timerPerRun.getMeanRate());//average operations per second
        Snapshot snapshot = timerPerRun.getSnapshot();
        LOG.info("Snapshot 75thPercentile: {}", snapshot.get75thPercentile()*durationFactor);//75% of all operations were faster than x milliseconds
        LOG.info("Snapshot 95thPercentile: {}", snapshot.get95thPercentile()*durationFactor);//95% of all operations were faster than x milliseconds
        LOG.info("Snapshot 98thPercentile: {}", snapshot.get98thPercentile()*durationFactor);//98% of all operations were faster than x milliseconds
        LOG.info("Snapshot 99thPercentile: {}", snapshot.get99thPercentile()*durationFactor);//99% of all operations were faster than x milliseconds
        LOG.info("Snapshot 999thPercentile: {}", snapshot.get999thPercentile()*durationFactor);//99.9% of all operations were faster than x milliseconds
        LOG.info("Snapshot Min: {}", snapshot.getMin()*durationFactor);//duration in milliseconds of the fastest operation
        LOG.info("Snapshot Max: {}", snapshot.getMax()*durationFactor);//duration in milliseconds of the slowest operation
        LOG.info("Snapshot Mean: {}", snapshot.getMean()*durationFactor);//mean in milliseconds of duration of all operations
        LOG.info("Snapshot Median: {}", snapshot.getMedian()*durationFactor);//median in milliseconds of duration of all operations
        LOG.info("Snapshot StdDev: {}", snapshot.getStdDev()*durationFactor);//standard deviation of duration of all operations
        LOG.info("Snapshot Size: {}", snapshot.size());



        consoleReporterPerRun.report();
        csvReporterPerRun.report();

    }

    public static void main(String[] args) throws InterruptedException {

        ServerAddress serverAddress = new ServerAddress("test-db:27017");
        MongoDbAccessor mongoDbAccessor = new MongoDbAccessor("user", "pw", "testdb", true, serverAddress);
        InsertOperation insertOperation = new InsertOperation(mongoDbAccessor, "testdb", "perf", IOperation.ID);
        OperationExecutor operationExecutor = new OperationExecutor(10, 1000000, 3600, insertOperation, new CountDownLatch(1));
        operationExecutor.executeThreads();
        operationExecutor.analysis();
        operationExecutor.stopReporters();
        mongoDbAccessor.closeConnections();
    }
}
