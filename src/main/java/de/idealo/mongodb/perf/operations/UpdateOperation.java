package de.idealo.mongodb.perf.operations;

import com.mongodb.client.result.UpdateResult;
import de.idealo.mongodb.perf.MongoDbAccessor;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class UpdateOperation extends AbstractOperation {

    public UpdateOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
    }

    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId){

        final Document doc = new Document("$set", new Document(RANDOM_LONG, randomId))
                .append("$inc", new Document(VERSION, 1));

        final UpdateResult res = THREAD_RUN_COUNT.equals(queriedField)?mongoCollection.updateMany(eq(queriedField, selectorId), doc)
                :ID.equals(queriedField)?mongoCollection.updateOne(eq(queriedField, selectorId), doc):null;
        return res!=null?res.getModifiedCount():0l;
    }

    @Override
    public OperationModes getOperationMode(){
        if(IOperation.THREAD_RUN_COUNT.equals(queriedField)) return OperationModes.UPDATE_MANY;
        else return OperationModes.UPDATE_ONE;
    };

}
