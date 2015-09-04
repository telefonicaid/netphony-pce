##PCE Parameters

+ ####PCEServerPort
TCP port where the PCE is listening for incoming pcep connections. `<PCEServerPort> </PCEServerPort>`

+ ####LocalPCEAddress
`<LocalPCEAddress> </LocalPCEAddress>`

+ ####PCEManagementPort
TCP port to connect to manage the PCE.
`<PCEManagementPort> </PCEManagementPort>`

+ ####timerOSPFupdatesToParentPCE
Time betweeen updates to parent PCE.
`<timerOSPFupdatesToParentPCE> </timerOSPFupdatesToParentPCE>`

+ ####isCompletedAuxGraph
`<isCompletedAuxGraph> </isCompletedAuxGraph>`

+ ####isSSOn
Parameter meaning SSON network computation.
`<isSSOn> </isSSOn>`

+ ####isWLAN
Parameter meaning WLAN network computation.
`<isWLAN> </isWLAN>`

+ ####timeSendReachabilityTime
Time between updates of reachability information to parent PCE.
`<timeSendReachabilityTime> </timeSendReachabilityTime>`

+ ####OFCodeParentPCE
Objective Function code for the Parent PCE Algorithm.
`<OFCodeParentPCE> </OFCodeParentPCE>`

+ ####parentPCEAddress
Address of the parent PCE. If it is null, there is no parent PCE.
`<parentPCEAddress> </parentPCEAddress>`

+ ####parentPCEPort
Port of the parent PCE.
`<parentPCEPort> </parentPCEPort>`

+ ####OSPFTCPPort
Port of the protocol OSPF over TCP.
`<OSPFTCPPort> </OSPFTCPPort>`

+ ####PCCRequestsProcessors
Number of computing processors to handle requests from PCCs.
`<PCCRequestsProcessors> </PCCRequestsProcessors>`

+ ####ParentPCERequestProcessors
Number of computing processors to handle requests from parent PCE.
`<ParentPCERequestProcessors> </ParentPCERequestProcessors>`

+ ####PCEServerLogFile
Log file.
`<PCEServerLogFile> </PCEServerLogFile>`

+ ####PCEPParserLogFile
Log file.
`<PCEPParserLogFile> </PCEPParserLogFile>`

+ ####TEDBParserLogFile
Log file.
`<TEDBParserLogFile> </TEDBParserLogFile>`

+ ####OSPFParserLogFile
Log file.
`<OSPFParserLogFile> </OSPFParserLogFile>`

+ ####networkDescriptionFile
Log file.
`<networkDescriptionFile> </networkDescriptionFile>`

+ ####ITnetworkDescriptionFile
Name of the file that describes the IT network (if there is any).
`<ITnetworkDescriptionFile> </ITnetworkDescriptionFile>`

+ ####KeepAliveTimer
KeepAlive Timer of the PCEP session.
`<KeepAliveTimer> </KeepAliveTimer>`

+ ####minKeepAliveTimerPCCAccepted
Minimum keepalive timer accepted from the peer PCE/PCC.
`<minKeepAliveTimerPCCAccepted> </minKeepAliveTimerPCCAccepted>`

+ ####maxDeadTimerPCCAccepted
Maximum DeadTimer accepted from the peer PCE/PCC.
`<maxDeadTimerPCCAccepted> </maxDeadTimerPCCAccepted>`

+ ####zeroDeadTimerPCCAccepted
If a deadTimer of 0 is accepted from the peer PCE/PCC.
`<zeroDeadTimerPCCAccepted> </zeroDeadTimerPCCAccepted>`

+ ####DeadTimer
Dead timer of the pcep session.
`<DeadTimer> </DeadTimer>`

+ ####defaultPCELayer
Default layer of the PCE.
`<defaultPCELayer> </defaultPCELayer>`

+ ####ITcapable
If the PCE is IT capable
`<ITcapable> </ITcapable>`

+ ####nodelay
If the tcp no delay option is used or not.
`<nodelay> </nodelay>`

+ ####multilayer
`<multilayer> </multilayer>`

+ ####multidomain
`<multidomain> </multidomain>`

+ ####useMaxReqTime
`<useMaxReqTime> </useMaxReqTime>`

+ ####reservation
If reservation is allowed
`<reservation> </reservation>`

+ ####optimizedRead
If the experimental optimized method to read is used. 
`<optimizedRead> </optimizedRead>`

+ ####OSPFSession
If OSPF with raw socket is used to receive topology.
`<OSPFSession> </OSPFSession>`

+ ####OSPFListenerIP
IP Address from which the OSPF is listen
`<OSPFListenerIP> </OSPFListenerIP>`

+ ####OSPFMulticast
If it is multicast OSPF
`<OSPFMulticast> </OSPFMulticast>`

+ ####OSPFUnicast
If it is unicast OSPF
`<OSPFUnicast> </OSPFUnicast>`

+ ####OSPFTCPSession
If a TCP socket, sending OSPF packets over it is used to receive topology.
`<OSPFTCPSession> </OSPFTCPSession>`

+ ####analyzeRequestTime
If the request Time is analyzed (for statistics only)
`<analyzeRequestTime> </analyzeRequestTime>`

+ ####setTraces
`<setTraces> </setTraces>`

+ ####isActive
`<isActive> </isActive>`

+ ####isStateful
`<isStateful> </isStateful>`

+ ####statefulDFlag
`<statefulDFlag> </statefulDFlag>`

+ ####statefulSFlag
`<statefulSFlag> </statefulSFlag>`

+ ####statefulTFlag
`<statefulTFlag> </statefulTFlag>`

+ ####isSRCapable
`<isSRCapable> </isSRCapable>`

+ ####MSD
`<MSD> </MSD>`

+ ####timeSendTopologyTask
`<timeSendTopologyTask> </timeSendTopologyTask>`

+ ####timeSendReachabilityTask
`<timeSendReachabilityTask> </timeSendReachabilityTask>`

+ ####isCompletedAuxGraph
`<isCompletedAuxGraph> </isCompletedAuxGraph>`

+ ####actingAsBGP4Peer
BGP. This variable indeicates if the PCE has a BGP module. 
`<actingAsBGP4Peer> </actingAsBGP4Peer>`

+ ####BGP4File
File where read the BGP parameters to configure.
`<BGP4File> </BGP4File>`
