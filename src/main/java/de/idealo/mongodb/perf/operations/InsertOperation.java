package de.idealo.mongodb.perf.operations;

import de.idealo.mongodb.perf.MongoDbAccessor;
import org.bson.Document;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class InsertOperation extends AbstractOperation {

    private final ThreadLocalRandom random;
    private int randomFieldLength = 0;


    public InsertOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String field){
        super(mongoDbAccessor, db, collection, field);
        random = ThreadLocalRandom.current();
    }

    @Override
    long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId) {

        final Document doc = new Document("_id", maxId + globalRunCount);
        doc.put(THREAD_ID, threadId);
        doc.put(THREAD_RUN_COUNT, threadRunCount);
        doc.put(RANDOM_LONG, randomId);
        if(randomFieldLength > 0){
            doc.put(RANDOM_TEXT, generateRandomString(randomFieldLength));
        }
        doc.put(VERSION, Integer.valueOf(1));

        mongoCollection.insertOne(doc);

        return 1l;
    }

    @Override
    public OperationModes getOperationMode(){
        return OperationModes.INSERT;
    };

    public void setRandomFieldLength(int randomFieldLength){
        this.randomFieldLength = randomFieldLength;
    }

    private String generateRandomString(int length){
        return random.ints(48,123)
                .filter(i -> (i < 58) || (i > 64 && i < 91) || (i > 96))
                .limit(length)
                .collect(StringBuilder::new, (sb, i) -> sb.append((char) i), StringBuilder::append)
                .toString();
    }

}
