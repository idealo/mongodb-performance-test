package de.idealo.mongodb.perf;

import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import de.idealo.mongodb.perf.operations.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 27017;
    private static final int DEFAULT_THREADS = 10;
    private static final long DEFAULT_MAX_DURATION_IN_SECONDS = 3600;


    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    String database = null;
    String collection = null;
    String url = null;
    String user = null;
    String password = null;
    String authDb = null;
    boolean ssl = false;
    ArrayList<String> modes = new ArrayList<String>();
    ArrayList<Long> operationsCounts = new ArrayList<Long>();
    ArrayList<Integer> threadCounts = new ArrayList<Integer>();
    long maxDurationInSeconds = DEFAULT_MAX_DURATION_IN_SECONDS;
    boolean dropDb = false;
    private final String version;
    private int randomFieldLength = 0;
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

    public Main(){
        version = getClass().getPackage().getImplementationVersion();
    }

    private void validateInput(String... args){
        boolean helpRequested = false;
        boolean exception = false;
        Options cliOptions = cliOptions();
        try {
            CommandLine cmdLine = new DefaultParser().parse(cliOptions, args);
            if(cmdLine.hasOption("v")){
                System.out.print("version " + version);
                System.exit(0);
            }
            helpRequested = cmdLine.hasOption("H");
            host = cmdLine.getOptionValue("h", DEFAULT_HOST);
            if (cmdLine.hasOption("port")) {
                long lPort = (long) cmdLine.getParsedOptionValue("port");
                if (lPort < 0) {
                    throw new IllegalArgumentException("Invalid port number!");
                }
                port = (int)lPort;
            }
            database = cmdLine.getOptionValue("db");
            if(database == null || database.isEmpty()){
                throw new IllegalArgumentException("Database name must not be empty!");
            }
            collection = cmdLine.getOptionValue("c");
            if(collection == null || collection.isEmpty()){
                throw new IllegalArgumentException("Collection name must not be empty!");
            }
            if (cmdLine.hasOption("m")) {
                final String[] m_arg = cmdLine.getOptionValues("m");
                for (int i = 0; i < m_arg.length; i++) {
                    final String mode = m_arg[i].toUpperCase();
                    if(!(mode.equals(OperationModes.INSERT.name()) ||
                            mode.equals(OperationModes.UPDATE_ONE.name()) ||
                            mode.equals(OperationModes.UPDATE_MANY.name()) ||
                            mode.equals(OperationModes.COUNT_ONE.name()) ||
                            mode.equals(OperationModes.COUNT_MANY.name()) ||
                            mode.equals(OperationModes.ITERATE_ONE.name()) ||
                            mode.equals(OperationModes.ITERATE_MANY.name()) ||
                            mode.equals(OperationModes.DELETE_ONE.name()) ||
                            mode.equals(OperationModes.DELETE_MANY.name())
                    )){
                        throw new IllegalArgumentException("Mode must be either " + OperationModes.INSERT.name() + ", "
                                + OperationModes.UPDATE_ONE.name() + ", "
                                + OperationModes.UPDATE_MANY.name() + ", "
                                + OperationModes.COUNT_ONE.name() + ", "
                                + OperationModes.COUNT_MANY.name() + ", "
                                + OperationModes.ITERATE_ONE.name() + ", "
                                + OperationModes.ITERATE_MANY.name() + ", "
                                + OperationModes.DELETE_ONE.name() + " or "
                                + OperationModes.DELETE_MANY.name()
                        );
                    }
                    modes.add(mode);
                }
            }else{
                modes.add(OperationModes.INSERT.name());
            }

            if (cmdLine.hasOption("o")) {
                final String[] o_arg = cmdLine.getOptionValues("o");
                for (int i = 0; i < o_arg.length; i++) {
                    final Long operationsCount = Long.valueOf(o_arg[i]);
                    if (operationsCount < 1L) {
                        throw new IllegalArgumentException("Number of operations must >= 1!");
                    }
                    operationsCounts.add(operationsCount);
                }
                if(modes.size() != operationsCounts.size()){
                    throw new IllegalArgumentException("Number of mode parameters (-m) must be equal to number of operations parameters (-o) but was "+modes.size()+" and "+operationsCounts.size()+".");
                }
            }else{
                operationsCounts.add(0l);
            }

            if (cmdLine.hasOption("d")) {
                final String d_arg = cmdLine.getOptionValue("d");
                maxDurationInSeconds = Long.valueOf(d_arg);
                if (maxDurationInSeconds < 1L) {
                    throw new IllegalArgumentException("duration in seconds must >= 1!");
                }
            }

            if (cmdLine.hasOption("t")) {
                final String[] t_arg = cmdLine.getOptionValues("t");
                for (int i = 0; i < t_arg.length; i++) {
                    final Integer threadCount = Integer.valueOf(t_arg[i]);
                    if (threadCount < 1L) {
                        throw new IllegalArgumentException("threads must >= 1!");
                    }
                    threadCounts.add(threadCount);
                }

            }else {
                threadCounts.add(DEFAULT_THREADS);
            }
            if(threadCounts.size() % modes.size() != 0){
                throw new IllegalArgumentException("Number of thread parameters (-t) must be a multiple of number of modes parameters (-m) but was "+threadCounts.size()+" and "+modes.size()+".");
            }
            url = cmdLine.getOptionValue("url");
            user = cmdLine.getOptionValue("u");
            password = cmdLine.getOptionValue("p");
            authDb = cmdLine.getOptionValue("adb");
            if (cmdLine.hasOption("ssl")) {
                ssl = true;
            }
            if(authDb==null || authDb.isEmpty()){
                authDb=database;
            }
            if (cmdLine.hasOption("dropdb")) {
                dropDb = true;
            }

            if (cmdLine.hasOption("s")) {
                final String s_arg = cmdLine.getOptionValue("s");
                randomFieldLength = Integer.valueOf(s_arg);
                if (randomFieldLength < 0) {
                    throw new IllegalArgumentException("Size of random text field must be >= 0!");
                }
            }

            if (cmdLine.hasOption("writeconcern")) {
                String wcOption = cmdLine.getOptionValue("writeconcern").toUpperCase();
                switch (wcOption) {
                    case "ACKNOWLEDGED":
                        writeConcern = WriteConcern.ACKNOWLEDGED;
                        break;
                    case "UNACKNOWLEDGED":
                        writeConcern = WriteConcern.UNACKNOWLEDGED;
                        break;
                    case "JOURNALED":
                        writeConcern = WriteConcern.JOURNALED;
                        break;
                    case "MAJORITY":
                        writeConcern = WriteConcern.MAJORITY;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid WriteConcern value: " + wcOption);
                }
            }

        } catch (Exception e) {
            LOG.error(e.getMessage());
            exception = true;
        }
        if (exception || helpRequested) {
            HelpFormatter hf = new HelpFormatter();
            hf.setOptionComparator(null);
            hf.printHelp(160, Main.class.getName(), "*** mongoDB performance test (version "+version+")***\n" +
                            "Please run first mode="+ OperationModes.INSERT.name()+" in order to have a non-empty collection to test on.\n" +
                            "You may add option 'dropdb' in order to drop the database before inserting documents.\n" +
                            "Documents are inserted one by one (no bulk insert).\n" +
                            "Once documents are inserted, run mode=" +
                            OperationModes.UPDATE_ONE.name() + ", mode=" +
                            OperationModes.UPDATE_MANY.name() + ", mode=" +
                            OperationModes.COUNT_ONE.name() + ", mode=" +
                            OperationModes.COUNT_MANY.name()+", mode=" +
                            OperationModes.ITERATE_ONE.name() + ", mode=" +
                            OperationModes.ITERATE_MANY.name()+", mode=" +
                            OperationModes.DELETE_ONE.name() + " or mode=" +
                            OperationModes.DELETE_MANY.name()+" or a whole set of modes simultaneously." +
                            "\n  Modes explained:" +
                            "\n  " + OperationModes.INSERT.name() + " inserts documents with the following fields:" +
                            "\n     " + IOperation.ID + ": incremented long number starting from max(_id)+1, reflecting the number of inserts being executed" +
                            "\n     " + IOperation.THREAD_ID + ": number of the thread inserting the document, starting from 1" +
                            "\n     " + IOperation.THREAD_RUN_COUNT + ": number of inserts being executed by this thread, starting from 1" +
                            "\n     " + IOperation.RANDOM_LONG + ": a random long number" +
                            "\n     " + IOperation.RANDOM_TEXT + ": a random text, size defined by user (default 0, thus absent)" +
                            "\n     " + IOperation.VERSION + ": version number of the document, starting from 1" +
                            "\n  " + OperationModes.UPDATE_ONE.name() + " updates one document randomly queried on field '" + IOperation.ID + "'" +
                                 " by incrementing the field '"+IOperation.VERSION + "' and updating the field '"+IOperation.RANDOM_LONG+"' to a random value." +
                            "\n  " + OperationModes.UPDATE_MANY.name() + " updates all documents randomly queried on field '" + IOperation.THREAD_RUN_COUNT + "'" +
                                 " by incrementing the field '"+IOperation.VERSION + "' and updating the field '"+IOperation.RANDOM_LONG+"' to a random value." +
                            "\n  " + OperationModes.COUNT_ONE.name() + " counts one document randomly queried on field '" + IOperation.ID + "'." +
                            "\n  " + OperationModes.COUNT_MANY.name() + " counts all documents randomly queried on field '" + IOperation.THREAD_RUN_COUNT + "'." +
                            "\n  " + OperationModes.ITERATE_ONE.name() + " finds one document randomly queried on field '" + IOperation.ID + "'." +
                            "\n  " + OperationModes.ITERATE_MANY.name() + " finds and iterates all documents randomly queried on field '" + IOperation.THREAD_RUN_COUNT + "'." +
                            "\n  " + OperationModes.DELETE_ONE.name() + " deletes one document randomly queried on field '" + IOperation.ID + "'." +
                            "\n  " + OperationModes.DELETE_MANY.name() + " deletes all documents randomly queried on field '" + IOperation.THREAD_RUN_COUNT + "'." +
                            "\nThe queried field is indexed in the forground before the test is run, so on first run it may take time to build the index." +
                            "\nAt the end of each run, 2 csv-files with performance statistics are generated:"  +
                            "\n  1) File '"+OperationExecutor.TIMER_PER_SECOND_PREFIX +"[mode].csv' contains aggregated time series of 1 second per row for the defined [mode]." +
                            "\n  2) File '"+OperationExecutor.TIMER_PER_RUN_PREFIX +"[mode].csv' contains 1 row of aggregated data over the whole runtime for the defined [mode]." +
                            "\nOptions:", cliOptions,
                    "@author kay.agahd@idealo.de", true);
            System.exit(helpRequested ? 0 : 1);
        }

    }



    private static Options cliOptions() {
        Options options = new Options();
        options
                .addOption(new Option("H", "help", false, "print this message (overrides all other options and exits)"))
                .addOption(new Option("v", "version", false, "print version (overrides all other options and exits)"))
                .addOption(Option.builder("m").longOpt("mode").hasArgs().argName("MODE")
                        .desc("mode, " + OperationModes.INSERT.name() + ", "
                                + OperationModes.UPDATE_ONE.name() + ", "
                                + OperationModes.UPDATE_MANY.name() + ", "
                                + OperationModes.COUNT_ONE.name() + ", "
                                + OperationModes.COUNT_MANY.name() + ", "
                                + OperationModes.ITERATE_ONE.name() + ", "
                                + OperationModes.ITERATE_MANY.name() + ", "
                                + OperationModes.DELETE_ONE.name() + " or "
                                + OperationModes.DELETE_MANY.name()
                                + " (default: " + OperationModes.INSERT.name() + "), for a set of modes to be executed simultaneously, separate multiple values by space,"
                                + " first value must be preceded by space too and number of thread parameters (-t) must be equal or be a multiple of number of mode parameters (-m)."
                                + " Defined modes are executed simultaneously with their corresponding number of threads as soon as all modes of the current run are terminated.").build())
                .addOption(Option.builder("o").longOpt("operationscount").hasArgs().argName("OPERATIONS_COUNT")
                        .desc("number of operations to be executed - Enter as many values as modes (-m) since each mode has its own operation count, separated by space, first value must be preceded by space too."
                         + " May be left out in order to exclusively rely on parameter duration (-d).").type(Number.class).build())
                .addOption(Option.builder("t").longOpt("threads").hasArgs().argName("THREADS")
                        .desc("number of threads (1 or more, default " + DEFAULT_THREADS + ") - Separate multiple values by space, first value must be preceded by space too."
                         + " 1st value defines number of threads of 1st mode (-m),"
                         + " 2nd value defines number of threads of 2nd mode (-m) etc."
                         + " If number of thread parameters (-t) is a multiple of mode parameters (-m), it restarts all modes simultaneously with their corresponding number of threads as soon as all modes of the current run are terminated.")
                        .type(Number.class).build())
                .addOption(Option.builder("d").longOpt("duration").hasArg().argName("DURATION")
                        .desc("maximum duration in seconds of the performance test for each set of modes (default " + DEFAULT_MAX_DURATION_IN_SECONDS + ")")
                        .type(Number.class).build())
                .addOption(new Option("dropdb", "dropdatabase", false, "drop database before inserting documents"))
                .addOption(Option.builder("s").longOpt("randomtextsize").hasArg().argName("RANDOM_TEXT_SIZE")
                        .desc("Size in bytes of random text field, absent if 0 (default 0)")
                        .type(Number.class).build())
                .addOption(Option.builder("h").longOpt("host").hasArg().argName("HOST").desc("mongoDB host (default " + DEFAULT_HOST + ")").build())
                .addOption(Option.builder("port").longOpt("port").hasArg().argName("PORT").desc("mongoDB port (default " + DEFAULT_PORT + ")").type(Number.class)
                        .build())
                .addOption(Option.builder("db").longOpt("database").hasArg().argName("DB").desc("mongoDB database on which the performance test is executed").build())
                .addOption(Option.builder("c").longOpt("collection").hasArg().argName("COLLECTION").desc("mongoDB collection on which the performance test is executed").build())
                .addOption(Option.builder("url").longOpt("url").hasArg().argName("URL").desc("mongoDB URL").build())
                .addOption(Option.builder("u").longOpt("user").hasArg().argName("USER").desc("mongoDB user").build())
                .addOption(Option.builder("p").longOpt("password").hasArg().argName("PASSWORD").desc("mongoDB password").build())
                .addOption(Option.builder("adb").longOpt("authdb").hasArg().argName("AUTH_DB").desc("mongoDB database to be authenticated against (default: value of parameter -db)").build())
                .addOption(new Option("ssl", "ssl", false, "use SSL to connect to mongoDB"))
                .addOption(Option.builder("wc").longOpt("writeconcern").hasArg().argName("WRITE_CONCERN")
                        .desc("WriteConcern for MongoDB operations (ACKNOWLEDGED, UNACKNOWLEDGED, JOURNALED, MAJORITY)")
                        .build());
        return options;
    }

    private void executeOperations() {

        final ServerAddress serverAddress = new ServerAddress(host, port);
        final MongoDbAccessor mongoDbAccessor = new MongoDbAccessor(
                -1, // socketTimeOut
                user,
                password,
                authDb,
                ssl,
                url,
                writeConcern,
                serverAddress
        );

        int run=0;
        CountDownLatch runModeLatch = new CountDownLatch(modes.size());

        final ExecutorService executor = Executors.newFixedThreadPool(modes.size());

        LOG.info("OPERATION SETUP: Total modes {}", modes.size());

        try {
            for(int threadCount : threadCounts) {
                if(run >= modes.size()){
                    run = 0;
                    LOG.info("OPERATION SETUP: All run modes are running with their specified number of threads. Waiting on finishing of each run mode before continuing...");
                    runModeLatch.await();
                    runModeLatch = new CountDownLatch(modes.size());
                }

                final String mode = modes.get(run);
                final long operationsCount = operationsCounts.size()>run?operationsCounts.get(run):operationsCounts.get(0);
                IOperation operation = null;
                LOG.info("OPERATION SETUP: Adding run mode {}", mode);
                if (mode.equals(OperationModes.UPDATE_ONE.name())) {
                    operation = new UpdateOperation(mongoDbAccessor, database, collection, IOperation.ID);
                } else if (mode.equals(OperationModes.UPDATE_MANY.name())) {
                    operation = new UpdateOperation(mongoDbAccessor, database, collection, IOperation.THREAD_RUN_COUNT);
                } else if (mode.equals(OperationModes.COUNT_ONE.name())) {
                    operation = new CountOperation(mongoDbAccessor, database, collection, IOperation.ID);
                } else if (mode.equals(OperationModes.COUNT_MANY.name())) {
                    operation = new CountOperation(mongoDbAccessor, database, collection, IOperation.THREAD_RUN_COUNT);
                } else if (mode.equals(OperationModes.ITERATE_ONE.name())) {
                    operation = new IterateOperation(mongoDbAccessor, database, collection, IOperation.ID);
                } else if (mode.equals(OperationModes.ITERATE_MANY.name())) {
                    operation = new IterateOperation(mongoDbAccessor, database, collection, IOperation.THREAD_RUN_COUNT);
                } else if (mode.equals(OperationModes.DELETE_ONE.name())) {
                    operation = new DeleteOperation(mongoDbAccessor, database, collection, IOperation.ID);
                } else if (mode.equals(OperationModes.DELETE_MANY.name())) {
                    operation = new DeleteOperation(mongoDbAccessor, database, collection, IOperation.THREAD_RUN_COUNT);
                } else {
                    InsertOperation insertOperation = new InsertOperation(mongoDbAccessor, database, collection, IOperation.ID);
                    if (dropDb) {
                        LOG.info("OPERATION SETUP: drop database '{}'", database);
                        mongoDbAccessor.getMongoDatabase(database).drop();
                        LOG.info("OPERATION SETUP: database '{}' dropped", database);
                    }
                    if(randomFieldLength > 0){
                        insertOperation.setRandomFieldLength(randomFieldLength);
                    }
                    operation = insertOperation;
                }

                OperationExecutor operationExecutor = new OperationExecutor(threadCount, operationsCount, maxDurationInSeconds, operation, runModeLatch);
                executor.execute(operationExecutor);
                run++;
            }

            runModeLatch.await();

        } catch (Exception e) {
            LOG.error("OPERATION SETUP: Error while waiting on thread... exiting now.", e);
            System.exit(-1);
        }finally {
            executor.shutdown();
            mongoDbAccessor.closeConnections();
        }
    }

    public static void main(String... args){
        Main m = new Main();
        m.validateInput(args);
        m.executeOperations();
    }
}