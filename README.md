netphony PCE v1.3.3
==============

Repository branch build status:

| **Master**  | **Develop**   |
|:---:|:---:|
| [![Build Status](https://travis-ci.org/telefonicaid/netphony-pce.svg?branch=master)](https://travis-ci.org/telefonicaid/netphony-pce) | [![Build Status](https://travis-ci.org/telefonicaid/netphony-pce.svg?branch=develop)](https://travis-ci.org/telefonicaid/netphony-pce) |

Latest Maven Central Release: 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/es.tid.netphony/pce/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/es.tid.netphony/pce/)

This repository contains a Java based implementation of a Path Computation Element, with several flavours, and a Path Computation Client, following RFC 4655 architecture. According to RFC 4655 (citing literally): "A Path Computation Element (PCE) is an entity that is capable of computing a network path or route based on a network graph, and of applying computational constraints during the computation. The PCE entity is an application that can be located within a network node or component, on an out-of-network server, etc. For example, a PCE would be able to compute the path of a TE LSP by operating on the TED and considering bandwidth and other constraints applicable to the TE LSP service request."

The PCE follows a modular architecture and allows to add customized algorithms. The PCE has also stateful and remote inititation capabilities. In current version, three components can be built, a domain PCE (aka child PCE), a parent PCE (ready for the H-PCE architecture) and a PCC (path computation client).

More documentation in the [PCE Wiki](https://github.com/telefonicaid/netphony-pce/wiki). 

# Latest news!
- Compiles with netphony-network-protocols 1.3.3 and netphony-topology 1.3.3
- Examples added.
- Moved to slf4j logging framework
- Apache 2.0 license for all the code
- Sample Configurationfiles in src/main/sample-config-files/
- Integration test support with docker
- Dockerfile available for Domain PCE

# Requirements 

The requirements to build and run the components are: 

Java 7 or greater.

Set JAVA_HOME correctly. 

Apache maven 3.0.3 or greater.

In order to use the OSPF-TE functionality, raw multicast socket support is needed, which is built using JNI, so a c++ compiler is needed. If that functionality is not used, using just java is OK.  

# License

The sofware includes a modification of the rocksaw 1.0.1 library to include OSPF multicast support. Rockswaw library is licensed under Apache License Version 2.0.

The rest of the software is also licenced under Apache License Version 2.0 since v1.3.2.

##Parent PCE

The PCE can act as a parent PCE, following the procedures described in RFC 6805.

The main class to launch the Parent PCE is es.tid.pce.parentPCE.ParentPCEServer

To generate the auto-executable ParentPCE-jar-with-dependencies.jar run bellow command:
```
mvn package -P generate-autojar-ParentPCE
```
To execute, in the main folder:
```
java -jar -Dlog4j.configurationFile=path_tp_log4j2.xml  target/ParentPCE-jar-with-dependencies.jar path_to_PCEServerConfiguration_BGPLS.xml
```

##Domain PCE

The domain PCE uses a traffic engineering database built from either a BGP-LS Speaker (https://github.com/telefonicaid/netphony-topology), reading an XML file, or by peering in a OSPF-TE session. 

To generate the auto-executable PCE-jar-with-dependencies.jar run bellow command:
```
mvn package -P generate-autojar-PCE
```  

To execute the PCE with OSPF-TE support it is needed to build the JNI part, available in the [netphony-topology repository](https://github.com/telefonicaid/netphony-topology).  Remember, it is important to have JAVA_HOME variable correclty set.
```
cd netphony-topology/src/main/jni
make
``` 
At this step, there will be a librocksaw.so which you will need to copy to a directory in your LD_LIBRARY_PATH

##QuickClient

This is a command line tool that acts as PCC and sends PCEP messages to a PCE. Current version is able to send Path Computation Requests and Initiate messages. The use is as follows:

```
usage: java -jar PCC-jar-with-dependencies.jar <host> <port> <src> <dst> [options]
 -ero            Explicit Route Object
 -g              Generalized end points
 -ini            Send init message
 -li <value>     local interface
 -of <value>     set of value
 -rgbw <value>   set rgbw value
 ```

To generate the auto-executable PCC-jar-with-dependencies.jar run bellow command:
```
mvn package -P generate-autojar-PCC
```
 
##Acknowledgements 

The software has been developed by Telefonica I+D Core & Transport Team, led by Juan Pedro Fernandez Palacios, in internal innovation projects and through several EU funded research proyects, which continuously added functionality. The core of of the PCE was developed in the PhD thesis of Oscar Gonzalez de Dios. The Core & Transport Team group of Telefonica working with the PCE is formed by Victor Lopez, Oscar Gonzalez de Dios, Felipe Jiménez, Luis Miguel Contreras.   

The effort to release as open source of the code was funded by the E.U. CSA PACE. 

The developers of the code are (some of them developed code before it was published in github, so they do not appear threre as members): Oscar Gonzalez de Dios, Carlos Fernando Muñoz del Nuevo, Marta Cuaresma, Arturo Mayoral, Sergio, Alejandro Aguado, Jaume Marhuenda, Maria Victoria, Ruben Rosales, Jose Manuel Gran Josa, Victor Uceda, Andrea Sgambelluri (KTH).

The institutions contributing to the code are: Telefonica I+D, KTH.

As the software is now open source, all contributors, indviduals and insititution will be listed in the Acknowledgement section.

##List of publications using this software.
- O. Gonzalez de Dios, R. Casellas, F. Paolucci, A. Napoli, L. Gifre, A. Dupas, E, Hugues-Salas, R. Morro, S. Belotti, G. Meloni, T. Rahman, V. López, R. Martínez, F. Fresi, M. Bohn, S. Yan, L. Velasco, P. Layec and J. P. Fernandez-Palacios: Experimental Demonstration of Multivendor and Multidomain EON With Data and Control Interoperability Over a Pan-European Test Bed, in Journal of Lightwave Technology, Dec. 2016, Vol. 34, Issue 7, pp. 1610-1617. [DOI]
- O. Gonzalez de Dios, R. Casellas, R. Morro, F. Paolucci, V. López, R. Martínez, R. Muñoz, R. Villalta, P. Castoldi: Multi-partner Demonstration of BGP-LS enabled multi-domain EON, accepted for publication in Journal of Optical Communications and Networking, Dec. 2015, Vol. 7, Issue 12, pp. B153-B162. [DOI] 
- O. Gonzalez de Dios, V. López, M. Cuaresma, F. Muñoz, M. Chamania and A. Jukan: Coordinated Computation and Setup of Multi-layer Paths via Inter-layer PCE Communication: Standards, Interoperability and Deployment, in IEEE Communications Magazine, December 2013, Vol. 51, pp. 144 - 154. [DOI]
- M. Cuaresma, V. Lopez, O. Gonzalez de Dios, F. Muñoz del Nuevo and J.P. Fernández-Palacios: Implementation and Assessment of Pre-Reservation mechanism for stateless PCE environments, in Springer's Journal of the Network and Systems Management, November 2013. [DOI]



