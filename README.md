# MongoDB performance test tool

This java application tests mongoDB performance, such as latency and throughput, by running one or more threads executing either all the same or different database operations, such as Inserts, Updates, Deletes, Counts or Finds until a defined number of operations is executed or a defined maximum runtime is reached.



## Start


### Preconditions

1. java 1.8 or newer

### Run


1. Open a terminal
2. Clone the project: `git clone https://github.com/idealo/mongodb-performance-test.git`
3. Cd to the project folder: `cd mongodb-performance-test`
4. Execute the jar file either by:
```
java -jar ./latest-version/mongodb-performance-test.jar
```
or by:
```
java -cp ./latest-version/mongodb-performance-test.jar de.idealo.mongodb.perf.Main
```
This will print a description how to use the program:
```
usage: de.idealo.mongodb.perf.Main [-H] [-v] [-m <MODE>] [-o <OPERATIONS_COUNT>] [-t <THREADS>] [-d <DURATION>] [-dropdb] [-s <RANDOM_TEXT_SIZE>] [-h <HOST>]
       [-port <PORT>] [-db <DB>] [-c <COLLECTION>] [-u <USER>] [-p <PASSWORD>] [-adb <AUTH_DB>]
*** mongoDB performance test (version 1.0.5)***
Please run first mode=INSERT in order to have a non-empty collection to test on.
You may add option 'dropdb' in order to drop the database before inserting documents.
Documents are inserted one by one (no bulk insert).
Once documents are inserted, run mode=UPDATE_ONE, mode=UPDATE_MANY, mode=COUNT_ONE, mode=COUNT_MANY, mode=ITERATE_ONE, mode=ITERATE_MANY, mode=DELETE_ONE or
mode=DELETE_MANY or a whole set of modes simultaneously.
  Modes explained:
  INSERT inserts documents with the following fields:
     _id: incremented long number starting from max(_id)+1, reflecting the number of inserts being executed
     threadId: number of the thread inserting the document, starting from 1
     threadRunCount: number of inserts being executed by this thread, starting from 1
     rnd: a random long number
     rndTxt: a random text, size defined by user (default 0, thus absent)
     v: version number of the document, starting from 1
  UPDATE_ONE updates one document randomly queried on field '_id' by incrementing the field 'v' and updating the field 'rnd' to a random value.
  UPDATE_MANY updates all documents randomly queried on field 'threadRunCount' by incrementing the field 'v' and updating the field 'rnd' to a random value.
  COUNT_ONE counts one document randomly queried on field '_id'.
  COUNT_MANY counts all documents randomly queried on field 'threadRunCount'.
  ITERATE_ONE finds one document randomly queried on field '_id'.
  ITERATE_MANY finds and iterates all documents randomly queried on field 'threadRunCount'.
  DELETE_ONE deletes one document randomly queried on field '_id'.
  DELETE_MANY deletes all documents randomly queried on field 'threadRunCount'.
The queried field is indexed in the forground before the test is run, so on first run it may take time to build the index.
At the end of each run, 2 csv-files with performance statistics are generated:
  1) File 'stats-per-second-[mode].csv' contains aggregated time series of 1 second per row for the defined [mode].
  2) File 'stats-per-run-[mode].csv' contains 1 row of aggregated data over the whole runtime for the defined [mode].
Options:
 -H,--help                                 print this message (overrides all other options and exits)
 -v,--version                              print version (overrides all other options and exits)
 -m,--mode <MODE>                          mode, INSERT, UPDATE_ONE, UPDATE_MANY, COUNT_ONE, COUNT_MANY, ITERATE_ONE, ITERATE_MANY, DELETE_ONE or DELETE_MANY
                                           (default: INSERT), for a set of modes to be executed simultaneously, separate multiple values by space, first value
                                           must be preceded by space too and number of thread parameters (-t) must be equal or be a multiple of number of mode
                                           parameters (-m). Defined modes are executed simultaneously with their corresponding number of threads as soon as all
                                           modes of the current run are terminated.
 -o,--operationscount <OPERATIONS_COUNT>   number of operations to be executed - Enter as many values as modes (-m) since each mode has its own operation count,
                                           separated by space, first value must be preceded by space too. May be left out in order to exclusively rely on
                                           parameter duration (-d).
 -t,--threads <THREADS>                    number of threads (1 or more, default 10) - Separate multiple values by space, first value must be preceded by space
                                           too. 1st value defines number of threads of 1st mode (-m), 2nd value defines number of threads of 2nd mode (-m) etc.
                                           If number of thread parameters (-t) is a multiple of mode parameters (-m), it restarts all modes simultaneously with
                                           their corresponding number of threads as soon as all modes of the current run are terminated.
 -d,--duration <DURATION>                  maximum duration in seconds of the performance test for each set of modes (default 3600)
 -dropdb,--dropdatabase                    drop database before inserting documents
 -s,--randomtextsize <RANDOM_TEXT_SIZE>    Size of random text field, absent if 0 (default 0)
 -h,--host <HOST>                          mongoDB host (default localhost)
 -port,--port <PORT>                       mongoDB port (default 27017)
 -db,--database <DB>                       mongoDB database on which the performance test is executed
 -c,--collection <COLLECTION>              mongoDB collection on which the performance test is executed
 -u,--user <USER>                          mongoDB user
 -p,--password <PASSWORD>                  mongoDB password
 -adb,--authdb <AUTH_DB>                   mongoDB database to be authenticated against (default: value of parameter -db)
```

Required parameters are at least:
```
  -db,--database <DB>                       mongoDB database on which the performance test is executed
  -c,--collection <COLLECTION>              mongoDB collection on which the performance test is executed
```
If the database uses authentication, you'll need furthermore:
```
  -u,--user <USER>                          mongoDB user
  -p,--password <PASSWORD>                  mongoDB password
  -adb,--authdb <AUTH_DB>                   mongoDB database to be authenticated against 
```

### Some examples

To make the following commands shorter, use the following variable:
```
jarfile=./latest-version/mongodb-performance-test.jar
```

#### Insert test
To insert 1 million documents on localhost:27017 (default) by 10 threads into database `test`, collection `perf` would be: 
```
java -jar $jarfile -m insert -o 1000000 -t 10 -db test -c perf
```

#### Update-one test
To test the performance of updating one document per query using 10, 20 and finally 30 threads for 1 hour each run (3 hours in total) would be: 
```
java -jar $jarfile -m update_one -d 3600 -t 10 20 30 -db test -c perf
```

#### Balanced update/find test (1/1)
To test the performance of both updating one document per query and simultaneously iterating many documents per query using the same number of threads for both operation modes i.e. 10, 20 and finally 30 threads for 1 hour each run (3 hours in total) would be: 
```
java -jar $jarfile -m update_one iterate_many -d 3600 -t 10 10 20 20 30 30 -db test -c perf
```

#### Unbalanced update/find test (1/2)
To test the performance of both updating one document per query and simultaneously iterating many documents per query using half number of threads for updates as for reads i.e. 10, 20 and 30 threads for updates compared to 20, 40 and finally 60 threads for reads, for 1 hour each run (3 hours in total) would be: 
```
java -jar $jarfile -m update_one iterate_many -d 3600 -t 10 20 20 40 30 60 -db test -c perf
```


## Output

During the test, statistics over the last second are printed every second in the console. You'll find these stats also in the file `stats-per-second-[mode].csv` which is located in the same folder as the jar file. `[mode]` is a placeholder for the executed mode(s), i.e. either `INSERT`, `UPDATE_ONE`, `UPDATE_MANY`, `COUNT_ONE`, `COUNT_MANY`, `ITERATE_ONE`, `ITERATE_MANY`, `DELETE_ONE` or `DELETE_MANY`. Each line in the file represents one second runtime.
Once finished the test, statistics over the whole test run are saved in file `stats-per-run-[mode].csv`. One line in this file represents one test run.
Statistics will be appended at the end of the file if the file exists already.
A csv-file may look like this:
```
t,count,max,mean,min,stddev,p50,p75,p95,p98,p99,p999,mean_rate,m1_rate,m5_rate,m15_rate,rate_unit,duration_unit
1480512965,1029,113.330273,9.658569,8.077813,9.511101,8.694442,8.935988,9.389485,9.816357,22.115234,113.315388,1013.330119,0.000000,0.000000,0.000000,calls/second,milliseconds
1480512966,2189,15.540594,8.571115,7.950870,0.632813,8.516873,8.669907,8.893378,9.027324,9.134963,15.536228,1089.643728,0.000000,0.000000,0.000000,calls/second,milliseconds
1480512967,3374,9.007461,8.396873,7.910648,0.181113,8.394915,8.511345,8.693716,8.760010,8.854439,9.000213,1122.521204,0.000000,0.000000,0.000000,calls/second,milliseconds
1480512968,4560,9.120956,8.385222,7.915487,0.169216,8.411585,8.491783,8.622020,8.704812,8.770961,9.114671,1138.934044,0.000000,0.000000,0.000000,calls/second,milliseconds
1480512969,5769,9.184245,8.288568,7.872285,0.212133,8.318201,8.446540,8.605013,8.672987,8.744656,9.149277,1151.930725,1151.800000,1151.800000,1151.800000,calls/second,milliseconds
1480512970,7009,8.504902,8.033251,7.805362,0.115184,8.008264,8.095955,8.260041,8.333445,8.387036,8.503900,1166.167975,1151.800000,1151.800000,1151.800000,calls/second,milliseconds
1480512971,8229,8.660829,8.212544,7.834811,0.137328,8.219335,8.307989,8.419099,8.492634,8.532298,8.660810,1174.379401,1151.800000,1151.800000,1151.800000,calls/second,milliseconds
1480512972,9449,8.695316,8.213226,7.822064,0.154449,8.212766,8.314221,8.472281,8.544149,8.577363,8.693777,1179.500415,1151.800000,1151.800000,1151.800000,calls/second,milliseconds
```
Columns explained:
* t = timestamp in seconds since epoch 1970-01-01
* count = number of operations (which is not necessarily the number of affected documents)
* max = maximum duration of an operation in this period of time
* min = maximum duration of an operation in this period of time
* stddev = standard deviation of the duration of all operations in this period of time
* p50 = 50th percentile i.e. 50% of all operations were faster in this period of time
* p75 = 75th percentile i.e. 75% of all operations were faster in this period of time
* p95 = 95th percentile i.e. 95% of all operations were faster in this period of time
* p98 = 98th percentile i.e. 98% of all operations were faster in this period of time
* p99 = 99th percentile i.e. 99% of all operations were faster in this period of time
* p999 = 999th percentile i.e. 99.9% of all operations were faster in this period of time
* mean_rate = mean rate of all operations in this period of time
* m1_rate = mean rate of all operations in 1 minute
* m5_rate = mean rate of all operations in 5 minutes
* m15_rate = mean rate of all operations in 15 minutes
* rate_unit = unit of measurement for the throughput i.e. calls/second
* duration_unit = unit of measurement for the time period i.e. milliseconds



## How it works

### Document structure

The inserted documents have the following fields:
* `_id`: incremented long number starting from max(_id)+1, reflecting the number of inserts being executed
* `threadId`: number of the thread inserting the document, starting from 1
* `threadRunCount`: number of inserts being executed by this thread, starting from 1
* `rnd`: a random long number
* `rndTxt`: a random text, size defined by parameter -s (default 0, thus absent)
* `v`: version number of the document, starting from 1

### Operations "...ONE" and "...MANY"

The operations "...ONE" rely on field `_id`. Since `_id` is unique, it will match exactly one document for one operation. The tool selects min and max of `_id` before it starts the performance test, which allows to randomly select single documents in the right range of `_id`.

The operations "...MANY" rely on field `threadRunCount`. The field is indexed before queried the first time in order to determine min and max. Also, it would not make any sense to query an unindexed field when testing performance. Building the index may take a longer time, which may lead the test to a time-out-exception, so you have to wait until the index is built before you restart the test run.

The field `threadRunCount` reflects the number of documents inserted by a particular thread. The range is the total number of inserted documents divided by number of threads. Since not all threads will insert the exact same number of documents, the range may slightly vary. The selectivity of field `threadRunCount` corresponds to the number of threads. 
For example, 50 threads inserted a total of 1 million of documents, so the range of `threadRunCount` will go from 1 to 20,000. One query on `threadRunCount` with one value between 1 and 20,000 will return almost always 50 documents.


## Version history

* v1.0.5
    + new: option to add random text of user defined length to documents to insert
* v1.0.4
    + new: initial commit


## Third party libraries

* mongo-java-driver: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* slf4j: [MIT License](http://opensource.org/licenses/MIT)
* logback: [LGPL 2.1](http://www.gnu.org/licenses/old-licenses/lgpl-2.1)
* google-collections (Guava): [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


## License

This software is licensed under [AGPL 3.0](http://www.gnu.org/licenses/agpl-3.0.html).
For details about the license, please see file "LICENSE", located in the same folder as this "README.md" file.



