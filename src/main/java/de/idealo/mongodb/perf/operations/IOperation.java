package de.idealo.mongodb.perf.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kay.agahd on 23.11.16.
 */
public interface IOperation {

    Logger LOG = LoggerFactory.getLogger(IOperation.class);

    void operation(int threadId, long localRunCount, long globalRunCount);

    OperationModes getOperationMode();

    long getAffectedDocuments();

    //document field names:
    String ID = "_id";
    String THREAD_ID = "threadId";
    String THREAD_RUN_COUNT = "threadRunCount";
    String GLOBAL_RUN_COUNT = "globbalRunCount";
    String RANDOM_LONG = "rnd";
    String VERSION = "v";
}
