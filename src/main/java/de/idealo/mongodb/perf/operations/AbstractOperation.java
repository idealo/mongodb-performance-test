package de.idealo.mongodb.perf.operations;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import de.idealo.mongodb.perf.MongoDbAccessor;
import org.bson.Document;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kay.agahd on 23.11.16.
 */
public abstract class AbstractOperation implements IOperation{


    private final Random random = ThreadLocalRandom.current();
    private final AtomicLong affectedDocs = new AtomicLong();

    final String db; 
    final String collection;
    final String queriedField;

    final MongoDbAccessor mongoDbAccessor;

    MongoCollection<Document> mongoCollection;
    long minId;
    long maxId;

    public AbstractOperation(MongoDbAccessor mongoDbAccessor, String db, String collection, String queriedField){
        this.mongoDbAccessor = mongoDbAccessor;
        this.db = db;
        this.collection = collection;
        this.queriedField = queriedField;

        initCollectionInfo();
    }

    public void initCollectionInfo() 
    {
        mongoCollection = mongoDbAccessor.getMongoDatabase(db).getCollection(collection);

        final IndexOptions options = new IndexOptions();
        options.background(false);
        mongoCollection.createIndex(new BasicDBObject(queriedField, 1), options);
        minId = getMinMax(mongoDbAccessor, queriedField, true);
        maxId = getMinMax(mongoDbAccessor, queriedField, false);
    }

    /**
     *
     * @param threadId
     * @param threadRunCount
     * @param globalRunCount
     * @param randomId
     * @return number of affected documents
     */

    abstract long executeQuery(int threadId, long threadRunCount, long globalRunCount, long selectorId, long randomId);


    private long getMinMax(MongoDbAccessor mongoDbAccessor, String field, boolean min){
        final Document document = mongoDbAccessor.getMinMax(mongoCollection, field, min);
        final Long id = mongoDbAccessor.getLong(document, field);
        if(id != null){
            return id.longValue();
        }
        return 0;
    }

    @Override
    public void operation(int threadId, long threadRunCount, long globalRunCount) {

        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final long selectorId = rnd.nextLong(minId, maxId+1l);//2nd paramter is exlusive, thus add 1
        final long randomId = rnd.nextLong();
        LOG.debug("{}: {} {}: {} {}: {} selectorId:{} {}: {}",
                THREAD_ID, threadId,
                THREAD_RUN_COUNT, threadRunCount,
                GLOBAL_RUN_COUNT, globalRunCount,
                selectorId,
                RANDOM_LONG, randomId);
        try {
            final long lAffectedDocs = executeQuery(threadId, threadRunCount, globalRunCount, selectorId, randomId);
            affectedDocs.addAndGet(lAffectedDocs);

            // buggin'
            // if (random.nextInt(100) < 1) {
            //     synchronized(mongoDbAccessor) {    
            //         mongoDbAccessor.closeConnections();
            //     }
            // }
        } 
        catch (IllegalStateException ee) {

            synchronized(mongoDbAccessor) {
                try {
                    LOG.error("mongoDbAccessor in illegal state... attempting to fixup collection", ee);
                    initCollectionInfo();
                    final long lAffectedDocs = executeQuery(threadId, threadRunCount, globalRunCount, selectorId, randomId);
                    affectedDocs.addAndGet(lAffectedDocs);
                }
                catch (IllegalStateException eee) {
                    
                    try {
                        LOG.error("mongoDbAccessor in illegal state... attempting to reconnect", eee);
                        mongoDbAccessor.closeConnections();
                        mongoDbAccessor.init();                    
                        initCollectionInfo();
                        final long lAffectedDocs = executeQuery(threadId, threadRunCount, globalRunCount, selectorId, randomId);
                        affectedDocs.addAndGet(lAffectedDocs);
                    }
                    catch (IllegalStateException eeee) {
                        LOG.error("mongoDbAccessor in illegal state... can't seem to fix it", eee);
                        System.exit(-1);
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.error("error while executing query on field '{}' with value '{}'", queriedField, selectorId, e);
        } 
    }

    @Override
    public long getAffectedDocuments() {
        return affectedDocs.get();
    }


}
