cne-pce v1.1.0
==============

Java based Path Computation Element and Test PCC

#Parent PCE

#PCE

#QuickClient

This is a command line tool that acts as PCC and sends PCEP messages to a PCE

(*) Used for testing purposes only. 

#Dependencies

The dependencies are specified in the pom.xml file.
Here is the dependency tree:
 ```
 es.tid.netphony:pce:jar:1.0.1
 +- es.tid.netphony:topology:jar:1.1.1:compile
 |  +- org.jgrapht:jgrapht-core:jar:0.9.1:compile
 |  +- com.google.code.gson:gson:jar:2.2.2:compile
 |  +- com.googlecode.json-simple:json-simple:jar:1.1.1:compile
 |  |  \- junit:junit:jar:4.10:compile
 |  |     \- org.hamcrest:hamcrest-core:jar:1.1:compile
 |  \- com.metaparadigm:json-rpc:jar:1.0:compile
 +- org.slf4j:slf4j-api:jar:1.7.7:compile
 +- es.tid.netphony:network-protocols:jar:1.1.1:compile
 +- colt:colt:jar:1.2.0:compile
 |  \- concurrent:concurrent:jar:1.3.4:compile
 \- redis.clients:jedis:jar:2.1.0:compile
    \- commons-pool:commons-pool:jar:1.5.5:compile

 ```


