Spatial Crunch Pipeline
====

[Apache Crunch](http://crunch.apache.org) simplifies the coding of data aggregation in [MapReduce](http://hadoop.apache.org/docs/r1.0.4/mapred_tutorial.html) jobs.
This project creates a pipeline to perform a spatial join between a very large set of static points and a small set of dynamic polygons.
The spatial operation is performed using the [Esri Geomerty API for Java](https://github.com/Esri/geometry-api-java).
The result of the spatial join is a count of points per polygon. The result can be related back to the polygons to generate a [choropleth map](http://en.wikipedia.org/wiki/Choropleth_map).

Crunch has simplified the process so much, that it came down to essentially a one line syntax:

    final PTable<Long, Long> counts = pipeline.
            readTextFile(args[0]).
            parallelDo(new PointInPolygon(), Writables.longs()).
            count();

In this project, the point data is stored in HDFS and the polygon data is sent to each hadoop node from the client node through the [DistributedCache](http://hadoop.apache.org/docs/stable/api/org/apache/hadoop/filecache/DistributedCache.html) mechanism.
As the number of polygons is small, they are maintained in a list and brute forcefully iterated over when performing a point in polygon operation.
However, if the number becomes large, a spatial index like a QuadTree will be a better way to store and search the set.

Download and Install Crunch
----

    $ git clone https://git-wip-us.apache.org/repos/asf/crunch.git
    $ cd crunch
    $ mvn -Phadoop-2 install

Compile and Package (CDH4)
----

    $ git clone https://github.com/mraad/SpatialCrunch.git
    $ cd SpatialCrunch
    $ mvn -Pcdh4 package

Generate Test Data
----

Generate one million random points:

    $ mvn -q exec:java -Dexec.mainClass=com.esri.GenPoints > points.txt

To generate a different number of random points, specify the count as an argument:

    $ mvn -q exec:java -Dexec.mainClass=com.esri.GenPoints -Dexec.args=10000 > points.txt

Generate 10 random square polygons with a one degree width and height:

    $ mvn -q exec:java -Dexec.mainClass=com.esri.GenPolygons > polygons.json

Launch a CDH Cluster on EC2
----

I use [Apache Whirr](http://whirr.apache.org) to launch a [CDH](http://www.cloudera.com/content/cloudera/en/products/cdh.html) test cluster.
[Here](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/4.2.0/CDH4-Installation-Guide/cdh4ig_topic_22.html) is some great documentation on getting started with Whirr.

The following is a sample set of Whirr properties:

    whirr.provider=aws-ec2
    whirr.identity=aws-identity
    whirr.credential=aws-credential
    whirr.cluster-name=hadoopcluster
    whirr.instance-templates=1 hadoop-jobtracker+hadoop-namenode,1 hadoop-datanode+hadoop-tasktracker
    whirr.private-key-file=${sys:user.home}/.ssh/id_rsa
    whirr.public-key-file=${sys:user.home}/.ssh/id_rsa.pub
    whirr.env.repo=cdh4
    whirr.hadoop.install-function=install_cdh_hadoop
    whirr.hadoop-configure-function=configure_cdh_hadoop
    whirr.hardware-id=m1.large
    whirr.image-id=us-east-1/ami-ccb35ea5
    whirr.location-id=us-east-1

Launch the cluster (it takes about 5 minutes, so been patient):

    $ whirr launch-cluster --config hadoop.properties

At the end of the launch, Whirr will report how to connect to each node using ssh.

Locate the instructions on how to ssh to the namenode as we will use its command line arguments to securely copy (scp) the pipeline jar and the test data to it.

    $ scp -i ~/.ssh/id_rsa -o "UserKnownHostsFile /dev/null" -o StrictHostKeyChecking=no target/SpatialCrunch-1.0-SNAPHOT-job.jar your-name@X.X.X.X:/home/users/your-name
    $ scp -i ~/.ssh/id_rsa -o "UserKnownHostsFile /dev/null" -o StrictHostKeyChecking=no points.txt your-name@X.X.X.X:/home/users/your-name
    $ scp -i ~/.ssh/id_rsa -o "UserKnownHostsFile /dev/null" -o StrictHostKeyChecking=no polygons.json your-name@X.X.X.X:/home/users/your-name

Connect to the cluster.

    $ ssh -i ~/.ssh/id_rsa -o "UserKnownHostsFile /dev/null" -o StrictHostKeyChecking=no your-name@X.X.X.X

Make an HDFS points folder that will contain the point data.

    $ hadoop fs -mkdir points
    $ hadoop fs -put points.txt points

Execute the crunch pipeline placing into the DistributedCache the polygon set and return the result in an HDFS folder named 'output'.

    $ hadoop jar SpatialCrunch-1.0-SNAPSHOT-job.jar -files polygons.json points/points.txt output

If all goes well, you should see something like the following:

    $ hadoop fs -ls output
    Found 1 items
    -rw-r--r--   3 mraad_admin supergroup         69 2013-05-26 06:43 output/part-r-00000
    $ hadoop fs -cat output/*
    [0,14]
    [1,18]
    [2,15]
    [3,13]
    [4,14]
    [5,8]
    [6,25]
    [7,14]
    [8,22]
    [9,16]

This is a small table, where the first column is the polygon row number in the polygons file, and the second
column is the number of points in that polygon.

You can repeat the process with different polygons, and when you are done with your analysis, you can destroy the cluster:

    $ whirr destroy-cluster --config hadoop.properties
