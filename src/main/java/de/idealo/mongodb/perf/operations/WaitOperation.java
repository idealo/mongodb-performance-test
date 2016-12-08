package de.idealo.mongodb.perf.operations;

import de.idealo.mongodb.perf.MongoDbAccessor;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class WaitOperation extends AbstractOperation{


    public WaitOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
    }

    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId){

        try {
            Thread.sleep(selectorId);
        } catch (InterruptedException e) {
            LOG.error("InterruptedException while sleeping", e);
        }
        return 0;
    }

    @Override
    public OperationModes getOperationMode(){
        return OperationModes.WAIT;
    };

}
