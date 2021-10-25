package de.idealo.mongodb.perf;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * Created by kay.agahd on 23.11.16.
 */
public class MongoDbAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDbAccessor.class);

    private final ServerAddress[] serverAddress;
    private final int socketTimeOut;
    private final String user;
    private final String pw;
    private final String authDb;
    private final boolean ssl;
    private MongoClient mongo;



    private MongoDbAccessor(){
        this(-1, null, null, null, false, null);
    };

    public MongoDbAccessor(String user, String pw, String authDb, boolean ssl, ServerAddress ... serverAddress){
        this(-1, user, pw, authDb, ssl, serverAddress);
    }

    public MongoDbAccessor(int socketTimeOut, String user, String pw, String authDb, boolean ssl, ServerAddress ... serverAddress){
        this.serverAddress = serverAddress;
        this.socketTimeOut = socketTimeOut;
        this.user = user;
        this.pw = pw;
        this.authDb = authDb!=null&&!authDb.isEmpty()?authDb:"admin";
        this.ssl = ssl;
        init();
    }

    public MongoDatabase getMongoDatabase(String dbName) {
        return mongo.getDatabase(dbName);
    }


    private void init() {
        LOG.info(">>> init {}", serverAddress);
        try {
            MongoClientOptions options = MongoClientOptions.builder().
                    connectTimeout(1000*2).//fail fast, so we know this node is unavailable
                    maxConnectionIdleTime(1000 * 60).
                    maxConnectionLifeTime(1000 * 60).
                    socketTimeout(socketTimeOut==-1?1000*10:socketTimeOut).//default 10 seconds
                    readPreference(ReadPreference.secondaryPreferred()).
                    connectionsPerHost(5000).
                    threadsAllowedToBlockForConnectionMultiplier(10).
                    writeConcern(WriteConcern.ACKNOWLEDGED).
                    sslEnabled(ssl).
                    sslInvalidHostNameAllowed(true).
                    build();

            if(user != null && !user.isEmpty() && pw!= null && !pw.isEmpty()) {
                MongoCredential mc = MongoCredential.createCredential(user, authDb, pw.toCharArray());
                if(serverAddress.length == 1) {
                    mongo = new MongoClient(serverAddress[0], Lists.newArrayList(mc), options);
                }else {
                    mongo = new MongoClient(Lists.newArrayList(serverAddress), Lists.newArrayList(mc), options);
                }
            }else{
                if(serverAddress.length == 1) {
                    mongo = new MongoClient(serverAddress[0], options);
                }else {
                    mongo = new MongoClient(Lists.newArrayList(serverAddress), options);
                }
            }


        } catch (MongoException e) {
            LOG.error("Error while initializing mongo at address {}", serverAddress, e);
            closeConnections();
        }

        LOG.info("<<< init");
    }

    public Long getLong(Document dbObj, String name) {
        if(dbObj != null) {
            Object obj = dbObj.get(name);
            if(obj != null && obj instanceof Long) {
                return (Long)(obj);
            }
        }
        return null;
    }

    public Document getMinMax(MongoCollection<Document> mongoCollection, String field, boolean min){
        try {
            final int sort = min?1:-1;
            return mongoCollection.find().sort(new BasicDBObject(field, sort)).projection(new BasicDBObject(field, 1)).first();
        } catch (Exception e) {
            LOG.error("error while getting field '{}' from mongodb", field, e);
        }
        return null;
    }


    public Document runCommand(String dbName, DBObject cmd) throws IllegalStateException {
        checkMongo();
        if(dbName != null && !dbName.isEmpty()) {
            return getMongoDatabase(dbName).runCommand((Bson) cmd, ReadPreference.secondaryPreferred());
        }
        throw new IllegalStateException("Database not initialized");
    }


    private void checkMongo() {
        if(mongo == null /*|| !mongo.getConnector().isOpen()*/) {
            init();
        }
    }


    public void closeConnections() {
        LOG.info(">>> closeConnections {}", serverAddress);

        try {
            if(mongo != null) {
                mongo.close();
                mongo = null;
            }
        } catch (Throwable e) {
            LOG.error("Error while closing mongo ", e);
        }

        LOG.info("<<< closeConnections {}", serverAddress);
    }


    public static void main(String[] args) throws UnknownHostException {
        ServerAddress adr = new ServerAddress("localhost:27017");
        MongoDbAccessor monitor = new MongoDbAccessor(null, null, null, false, adr);
        Document doc = monitor.runCommand("admin", new BasicDBObject("isMaster", "1"));
        LOG.info("doc: {}", doc);
        LOG.info("ismaster: {}",  doc.get("ismaster"));
        monitor.closeConnections();

    }

}
