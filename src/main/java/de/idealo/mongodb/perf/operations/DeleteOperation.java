package de.idealo.mongodb.perf.operations;

import com.mongodb.client.result.DeleteResult;
import de.idealo.mongodb.perf.MongoDbAccessor;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class DeleteOperation extends AbstractOperation {


    public DeleteOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
    }


    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId) {

        final DeleteResult res = THREAD_RUN_COUNT.equals(queriedField)?mongoCollection.deleteMany(eq(queriedField, selectorId))
                :ID.equals(queriedField)?mongoCollection.deleteOne(eq(queriedField, selectorId)):null;
        return res!=null?res.getDeletedCount():0l;

    }

    @Override
    public OperationModes getOperationMode() {
        if (IOperation.THREAD_RUN_COUNT.equals(queriedField)) return OperationModes.DELETE_MANY;
        else return OperationModes.DELETE_ONE;
    }

}
