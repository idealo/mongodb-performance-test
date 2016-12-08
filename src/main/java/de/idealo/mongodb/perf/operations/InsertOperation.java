package de.idealo.mongodb.perf.operations;

import de.idealo.mongodb.perf.MongoDbAccessor;
import org.bson.Document;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class InsertOperation extends AbstractOperation {


    public InsertOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
    }


    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId) {

        final Document doc = new Document("_id", maxId + globalRunCount);
        doc.put(THREAD_ID, threadId);
        doc.put(THREAD_RUN_COUNT, threadRunCount);
        doc.put(RANDOM_LONG, randomId);
        doc.put(VERSION, Integer.valueOf(1));

        mongoCollection.insertOne(doc);

        return 1l;
    }

    @Override
    public OperationModes getOperationMode(){
        return OperationModes.INSERT;
    };

}
