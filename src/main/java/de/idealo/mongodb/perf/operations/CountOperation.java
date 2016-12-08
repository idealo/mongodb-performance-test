package de.idealo.mongodb.perf.operations;

import de.idealo.mongodb.perf.MongoDbAccessor;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class CountOperation extends AbstractOperation {

    public CountOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
    }

    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId) {
         return mongoCollection.count(eq(queriedField, selectorId));
    }

    @Override
    public OperationModes getOperationMode(){
        if(IOperation.THREAD_RUN_COUNT.equals(queriedField)) return OperationModes.COUNT_MANY;
        else return OperationModes.COUNT_ONE;
    };

}
