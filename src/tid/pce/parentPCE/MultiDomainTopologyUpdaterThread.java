package tid.pce.parentPCE;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import tid.ospf.ospfv2.lsa.InterASTEv2LSA;
import tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import tid.pce.pcep.objects.Notification;
import tid.pce.pcep.objects.tlvs.ITAdvertisementTLV;
import tid.pce.pcep.objects.tlvs.OSPFTE_LSA_TLV;
import tid.pce.pcep.objects.tlvs.ServerTLV;
import tid.pce.pcep.objects.tlvs.StorageTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.BlockSizeSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.CostSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.EPaddressSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.IdleConsumptionSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.InterStateLatenciesSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.LocationSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.MTUSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.MaxSpeedSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.MaximumConsumptionSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.NetworkAdapterSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.NetworkSpecSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.PowerInfoSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.PowerStateSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.PowerSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.ResourceIDSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.ServerStorageSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.SleepConsumptionSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.StorageInfoSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.StorageSizeSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.TNAIPv4SubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.TNAIPv6SubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.TNANSAPSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.VolumeInfoSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.VolumeSizeSubTLV;
import tid.pce.pcep.objects.tlvs.subtlvs.VolumeSubTLV;
import tid.pce.tedb.ITMDTEDB;
import tid.pce.tedb.MDTEDB;
/**
 * Receives notifications with topology updates and maintains the multidomain topology
 * @author ogondio
 *
 */
public class MultiDomainTopologyUpdaterThread extends Thread {
	/**
	 * The Logger
	 */
	private Logger log;

	/**
	 * Queue with the Updates to process
	 */
	private LinkedBlockingQueue<MultiDomainUpdate> multiDomainUpdateQueue;

	// Multi-domain TEDB
	private MDTEDB multiDomainTEDB;

	//IT Capable Multi-domain TEDB
	private ITMDTEDB ITmultiDomainTEDB;

	//List of the received LSAs
	private LinkedList<InterASTEv2LSA> interASTEv2LSAList;

	/**
	 * Constructor for normal Multi-domain TEDB
	 * @param multiDomainUpdateQueue
	 * @param multiDomainTEDB
	 */
	public MultiDomainTopologyUpdaterThread(LinkedBlockingQueue<MultiDomainUpdate> multiDomainUpdateQueue,MDTEDB multiDomainTEDB){
		log=Logger.getLogger("MultiDomainTologyUpdater");
		this.multiDomainUpdateQueue=multiDomainUpdateQueue;		
		this.multiDomainTEDB=multiDomainTEDB;
		interASTEv2LSAList=new LinkedList<InterASTEv2LSA>();
	}

	/**
	 * Constructor for IT TEDB
	 * @param multiDomainUpdateQueue
	 * @param ITmultiDomainTEDB
	 */
	public MultiDomainTopologyUpdaterThread(LinkedBlockingQueue<MultiDomainUpdate> multiDomainUpdateQueue,ITMDTEDB ITmultiDomainTEDB){
		log=Logger.getLogger("MultiDomainTologyUpdater");
		this.multiDomainUpdateQueue=multiDomainUpdateQueue;		
		this.ITmultiDomainTEDB=ITmultiDomainTEDB;
	}

	public void run() {
		log.info("Starting Multidomain Topology Upadater Thread");
		MultiDomainUpdate multiDomainUpdate;
		Notification notif;
		while(true){
			try {
				multiDomainUpdate=multiDomainUpdateQueue.take();
				notif=multiDomainUpdate.getNotif();
				log.finest("Processing Notification to update topology");

				ITAdvertisementTLV ITadv = notif.getITadvtlv();
				if (ITadv!=null){
					log.info("IT advertisement received!!");	//Ale: cambiar logs "info" por "finest"
					Inet4Address IT_site = ITadv.getVirtual_IT_Site_ID();
					log.info("Virtual IT site ID:"+IT_site);
					int AdvType = ITadv.getAdv_Type();
					log.info("Advertisement type:"+AdvType);
					int AdvTrigger = ITadv.getAdv_Trigger();
					log.info("Advertisement trigger:"+AdvTrigger);

					StorageTLV Storage = notif.getStoragetlv();
					if(Storage != null){
						Inet4Address ResourceID = null;
						int TotalStorageSize = 0;
						int AvailableStorageSize = 0;

						log.info("Storage:");

						ResourceIDSubTLV ResourceIDsubtlv = Storage.getResourceIDSubTLV();
						if(ResourceIDsubtlv != null){
							ResourceID = ResourceIDsubtlv.getResourceID();
							log.info("Resource ID:"+ResourceID);
						}

						LocationSubTLV Locationsubtlv = Storage.getLocationSubTLV();
						if(Locationsubtlv != null){
							int LaRes = Locationsubtlv.getLaRes();
							log.info("Latitude Resolution:"+LaRes);
							byte[] Latitude = Locationsubtlv.getLatitude();
							log.info("Latitude:"+Latitude);
							int LoRes = Locationsubtlv.getLoRes();
							log.info("Longitude Resolution:"+LoRes);
							byte[] Longitude = Locationsubtlv.getLongitude();
							log.info("Longitude:"+Longitude);
						}

						LinkedList<CostSubTLV> CostsubtlvList = Storage.getCostList();
						if(CostsubtlvList != null){
							for (int i=0; i<CostsubtlvList.size(); i++){
								CostSubTLV Costsubtlv = CostsubtlvList.get(i);
								log.info("Coste "+i+": Unit:"+Costsubtlv.getUsageUnit()+" Unitari price:"+Costsubtlv.getUnitaryPrice());
							}

						}

						NetworkSpecSubTLV NetworkSpecsubtlv = Storage.getNetworkSpecSubTLV();
						if(NetworkSpecsubtlv != null){

							EPaddressSubTLV EPaddresssubtlv = NetworkSpecsubtlv.getEPaddress();
							if(EPaddresssubtlv != null){
								String EPaddress = EPaddresssubtlv.getEPaddress();
							}

							TNAIPv4SubTLV TNAipv4subtlv = NetworkSpecsubtlv.getTNAIPv4();
							if(TNAipv4subtlv != null){
								Inet4Address IPv4address = TNAipv4subtlv.getIPv4address();
							}

							TNAIPv6SubTLV TNAipv6subtlv = NetworkSpecsubtlv.getTNAIPv6();
							if(TNAipv6subtlv != null){
								Inet6Address IPv6address = TNAipv6subtlv.getIPv6address();
							}

							TNANSAPSubTLV TNANSAPsubtlv = NetworkSpecsubtlv.getTNANSAP();
							if(TNANSAPsubtlv != null){
								byte[] NSAPaddress = TNANSAPsubtlv.getNSAPaddress();
							}

							MTUSubTLV MTUsubtlv = NetworkSpecsubtlv.getMTU();
							if(MTUsubtlv != null){
								byte[] MTU = MTUsubtlv.getMTU();
							}

							MaxSpeedSubTLV Maxspeedsubtlv = NetworkSpecsubtlv.getMaxSpeed();
							if(Maxspeedsubtlv != null){
								byte[] MaxSpeed = Maxspeedsubtlv.getMaxSpeed();
							}

							NetworkAdapterSubTLV Networkadaptersubtlv = NetworkSpecsubtlv.getNetworkAdapter();
							if(Networkadaptersubtlv != null){
								int NetworkAdapter = Networkadaptersubtlv.getAdapter_Type();
								int FullDupplex = Networkadaptersubtlv.getFullDupplex();
							}
						}

						PowerSubTLV Powersubtlv = Storage.getPowerSubTLV();
						if(Powersubtlv != null){

							PowerInfoSubTLV PowerInfosubtlv = Powersubtlv.getPowerInfo();
							if (PowerInfosubtlv != null){
								int PowerSource = PowerInfosubtlv.getPowerSource();
								int PowerClass = PowerInfosubtlv.getPowerClass();
								int Regeneration = PowerInfosubtlv.getRegeneration();
							}

							MaximumConsumptionSubTLV MaxConsumptionsubtlv = Powersubtlv.getMaximumConsumptionSubTLV();
							if (MaxConsumptionsubtlv != null){
								byte[] MaxConsumption = MaxConsumptionsubtlv.getMaximumConsumption();
							}

							IdleConsumptionSubTLV IdleConsumptionsubtlv = Powersubtlv.getIdleConsumption();
							if (IdleConsumptionsubtlv != null){
								byte[] IdleConsumption = IdleConsumptionsubtlv.getIdleConsumption();
							}

							SleepConsumptionSubTLV SleepConsumptionsubtlv = Powersubtlv.getSleepConsumption();
							if (SleepConsumptionsubtlv != null){
								byte[] SleepConsumption = SleepConsumptionsubtlv.getSleepConsumption();
							}

							InterStateLatenciesSubTLV InterStateLatenciessubtlv = Powersubtlv.getInterStateLatencies();
							if (InterStateLatenciessubtlv != null){
								byte[] WakeUpLatency = InterStateLatenciessubtlv.getWakeUpLatency();
								byte[] PowerUpLatency = InterStateLatenciessubtlv.getPowerUpLatency();
							}

							PowerStateSubTLV PowerStatesubtlv = Powersubtlv.getPowerState();
							if (PowerStatesubtlv != null){
								byte PowerState = PowerStatesubtlv.getPowerState();
							}

						}

						StorageSizeSubTLV StorageSizesubtlv = Storage.getStorageSizeSubTLV();
						if(StorageSizesubtlv != null){
							TotalStorageSize = StorageSizesubtlv.getTotalSize();
							log.info("Total Size:"+TotalStorageSize);
							AvailableStorageSize = StorageSizesubtlv.getAvailableSize();
							log.info("Available Size:"+AvailableStorageSize);
						}

						StorageInfoSubTLV StorageInfosubtlv = Storage.getStorageInfoSubTLV();
						if(StorageInfosubtlv != null){
							int AccessStatus = StorageInfosubtlv.getAccessStatus();
							int Volatil = StorageInfosubtlv.getVolatil();
						}

						LinkedList<VolumeSubTLV> VolumesubtlvList = Storage.getVolumeList();
						if(VolumesubtlvList != null){
							for (int i=0; i<VolumesubtlvList.size(); i++){
								VolumeSubTLV Volumesubtlv = VolumesubtlvList.get(i);

								VolumeSizeSubTLV VolumeSizesubtlv = Volumesubtlv.getVolumeSize();
								if(VolumeSizesubtlv != null){
									int TotalVolumeSize = VolumeSizesubtlv.getTotalSize();
									int AvailableVolumeSize = VolumeSizesubtlv.getAvailableSize();
								}

								BlockSizeSubTLV BlockSizesubtlv = Volumesubtlv.getBlockSizeSubTLV();
								if(BlockSizesubtlv != null){
									byte[] BlockSize = BlockSizesubtlv.getBlockSize();
								}

								VolumeInfoSubTLV VolumeInfosubtlv = Volumesubtlv.getVolumeInfo();
								if(VolumeInfosubtlv != null){
									int AccessStatus = VolumeInfosubtlv.getAccessStatus();
									int Volatil = VolumeInfosubtlv.getVolatil();
								}

							}

						}
						Inet4Address domainID=multiDomainUpdate.getDomainID();
						ITmultiDomainTEDB.addStorage(domainID, IT_site, AdvType, ResourceID, CostsubtlvList, TotalStorageSize, AvailableStorageSize);

					}

					ServerTLV Server = notif.getServertlv();
					if(Server != null){

						log.info("Server:");

						ResourceIDSubTLV ResourceIDsubtlv = Server.getResourceIDSubTLV();
						if(ResourceIDsubtlv != null){
							Inet4Address ResourceID = ResourceIDsubtlv.getResourceID();
							log.info("Resource ID:"+ResourceID);
						}

						LocationSubTLV Locationsubtlv = Server.getLocationSubTLV();
						if(Locationsubtlv != null){
							int LaRes = Locationsubtlv.getLaRes();
							log.info("Latitude Resolution:"+LaRes);
							byte[] Latitude = Locationsubtlv.getLatitude();
							log.info("Latitude:"+Latitude);
							int LoRes = Locationsubtlv.getLoRes();
							log.info("Longitude Resolution:"+LoRes);
							byte[] Longitude = Locationsubtlv.getLongitude();
							log.info("Longitude:"+Longitude);
						}

						LinkedList<CostSubTLV> CostsubtlvList = Server.getCostList();
						if(CostsubtlvList != null){
							for (int i=0; i<CostsubtlvList.size(); i++){
								CostSubTLV Costsubtlv = CostsubtlvList.get(i);
								log.info("Coste "+i+": Unit:"+Costsubtlv.getUsageUnit()+" Unitari price:"+Costsubtlv.getUnitaryPrice());
							}

						}

						NetworkSpecSubTLV NetworkSpecsubtlv = Server.getNetworkSpecSubTLV();
						if(NetworkSpecsubtlv != null){

							EPaddressSubTLV EPaddresssubtlv = NetworkSpecsubtlv.getEPaddress();
							if(EPaddresssubtlv != null){
								String EPaddress = EPaddresssubtlv.getEPaddress();
							}

							TNAIPv4SubTLV TNAipv4subtlv = NetworkSpecsubtlv.getTNAIPv4();
							if(TNAipv4subtlv != null){
								Inet4Address IPv4address = TNAipv4subtlv.getIPv4address();
							}

							TNAIPv6SubTLV TNAipv6subtlv = NetworkSpecsubtlv.getTNAIPv6();
							if(TNAipv6subtlv != null){
								Inet6Address IPv6address = TNAipv6subtlv.getIPv6address();
							}

							TNANSAPSubTLV TNANSAPsubtlv = NetworkSpecsubtlv.getTNANSAP();
							if(TNANSAPsubtlv != null){
								byte[] NSAPaddress = TNANSAPsubtlv.getNSAPaddress();
							}

							MTUSubTLV MTUsubtlv = NetworkSpecsubtlv.getMTU();
							if(MTUsubtlv != null){
								byte[] MTU = MTUsubtlv.getMTU();
							}

							MaxSpeedSubTLV Maxspeedsubtlv = NetworkSpecsubtlv.getMaxSpeed();
							if(Maxspeedsubtlv != null){
								byte[] MaxSpeed = Maxspeedsubtlv.getMaxSpeed();
							}

							NetworkAdapterSubTLV Networkadaptersubtlv = NetworkSpecsubtlv.getNetworkAdapter();
							if(Networkadaptersubtlv != null){
								int NetworkAdapter = Networkadaptersubtlv.getAdapter_Type();
								int FullDupplex = Networkadaptersubtlv.getFullDupplex();
							}
						}

						PowerSubTLV Powersubtlv = Server.getPowerSubTLV();
						if(Powersubtlv != null){

							PowerInfoSubTLV PowerInfosubtlv = Powersubtlv.getPowerInfo();
							if (PowerInfosubtlv != null){
								int PowerSource = PowerInfosubtlv.getPowerSource();
								int PowerClass = PowerInfosubtlv.getPowerClass();
								int Regeneration = PowerInfosubtlv.getRegeneration();
							}

							MaximumConsumptionSubTLV MaxConsumptionsubtlv = Powersubtlv.getMaximumConsumptionSubTLV();
							if (MaxConsumptionsubtlv != null){
								byte[] MaxConsumption = MaxConsumptionsubtlv.getMaximumConsumption();
							}

							IdleConsumptionSubTLV IdleConsumptionsubtlv = Powersubtlv.getIdleConsumption();
							if (IdleConsumptionsubtlv != null){
								byte[] IdleConsumption = IdleConsumptionsubtlv.getIdleConsumption();
							}

							SleepConsumptionSubTLV SleepConsumptionsubtlv = Powersubtlv.getSleepConsumption();
							if (SleepConsumptionsubtlv != null){
								byte[] SleepConsumption = SleepConsumptionsubtlv.getSleepConsumption();
							}

							InterStateLatenciesSubTLV InterStateLatenciessubtlv = Powersubtlv.getInterStateLatencies();
							if (InterStateLatenciessubtlv != null){
								byte[] WakeUpLatency = InterStateLatenciessubtlv.getWakeUpLatency();
								byte[] PowerUpLatency = InterStateLatenciessubtlv.getPowerUpLatency();
							}

							PowerStateSubTLV PowerStatesubtlv = Powersubtlv.getPowerState();
							if (PowerStatesubtlv != null){
								byte PowerState = PowerStatesubtlv.getPowerState();
							}

						}

						ServerStorageSubTLV ServerStoragesubtlv = Server.getServerStorageSubTLV();
						if(ServerStoragesubtlv != null){

							StorageSizeSubTLV StorageSizesubtlv = ServerStoragesubtlv.getStorageSize();
							if(StorageSizesubtlv != null){
								int TotalSize = StorageSizesubtlv.getTotalSize();
								log.info("Total Size:"+TotalSize);
								int AvailableSize = StorageSizesubtlv.getAvailableSize();
								log.info("Available Size:"+AvailableSize);
							}

							StorageInfoSubTLV StorageInfosubtlv = ServerStoragesubtlv.getStorageInfoSubTLV();
							if(StorageInfosubtlv != null){
								int AccessStatus = StorageInfosubtlv.getAccessStatus();
								int Volatil = StorageInfosubtlv.getVolatil();
							}

							LinkedList<VolumeSubTLV> VolumesubtlvList = ServerStoragesubtlv.getVolumeList();
							if(VolumesubtlvList != null){
								for (int i=0; i<VolumesubtlvList.size(); i++){
									VolumeSubTLV Volumesubtlv = VolumesubtlvList.get(i);

									VolumeSizeSubTLV VolumeSizesubtlv = Volumesubtlv.getVolumeSize();
									if(VolumeSizesubtlv != null){
										int TotalSize = VolumeSizesubtlv.getTotalSize();
										int AvailableSize = VolumeSizesubtlv.getAvailableSize();
									}

									BlockSizeSubTLV BlockSizesubtlv = Volumesubtlv.getBlockSizeSubTLV();
									if(BlockSizesubtlv != null){
										byte[] BlockSize = BlockSizesubtlv.getBlockSize();
									}

									VolumeInfoSubTLV VolumeInfosubtlv = Volumesubtlv.getVolumeInfo();
									if(VolumeInfosubtlv != null){
										int AccessStatus = VolumeInfosubtlv.getAccessStatus();
										int Volatil = VolumeInfosubtlv.getVolatil();
									}

								}

							}

						}

					}
				}else{
					//THIS IS THE PART OF MULTI-DOMAIN TOPOLOGY
					LinkedList<OSPFTE_LSA_TLV>LSATLVList= notif.getLSATLVList();
					if (LSATLVList!=null){
						log.finest("Received OPSF_TE_LSA_TLV "+LSATLVList.size()+" LSAs");
						for (int i=0; i<LSATLVList.size();++i){
							OSPFTE_LSA_TLV tlv= LSATLVList.get(i);
							//log.finest("Type of TLV: "+LSATLVList.size());
							InterASTEv2LSA interASTEv2LSA=tlv.getInterASTEv2LSA();
							if (interASTEv2LSA!=null){
								log.finest("InterASTEv2LSA received: "+interASTEv2LSA.printHeader());
								if (interASTEv2LSAList.contains(interASTEv2LSA)){
									log.finest("LSA already present");
								}else {
									log.finest("NEW LSA, adding to the list and processing");
									interASTEv2LSAList.add(interASTEv2LSA);		
									LinkTLV linkTLV =interASTEv2LSA.getLinkTLV();
									if (linkTLV!=null){

										log.finest("Processing LinkTLV");
										try {
											Inet4Address remoteAS=tlv.getInterASTEv2LSA().getLinkTLV().getRemoteASNumber().getRemoteASNumber();
											log.finest("Remote AS: "+remoteAS);
											Inet4Address remoteASBR=tlv.getInterASTEv2LSA().getLinkTLV().getiPv4RemoteASBRID().getIPv4RemoteASBRID();
											log.finest("Remote ASBR: "+remoteASBR);
											Inet4Address localAS=multiDomainUpdate.getDomainID();
											log.finest("Local AS: "+localAS);
											Inet4Address localASBR=tlv.getInterASTEv2LSA().getAdvertisingRouter();
											log.finest("Local ASBR: "+localASBR);
											long localRouterASBRIf=tlv.getInterASTEv2LSA().getLinkTLV().getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier();
											long remoteRouterASBRIf=tlv.getInterASTEv2LSA().getLinkTLV().getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier();

											if(ITmultiDomainTEDB==null){
												multiDomainTEDB.addInterdomainLink(localAS, localASBR, localRouterASBRIf, remoteAS, remoteASBR, remoteRouterASBRIf,null);											
											}else{
												ITmultiDomainTEDB.addInterdomainLink(localAS, localASBR, localRouterASBRIf, remoteAS, remoteASBR, remoteRouterASBRIf,null);
											}

										}catch (Exception e){
											log.severe("Problem with Link TLV "+e.getStackTrace());
										}
									}




								}
							}
						}
					}else {
						log.warning("LSATLVList esta a null");
					}

				}


				//notif.getNotifyList();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public String printLSAList(){
		StringBuffer sb=new StringBuffer(20+interASTEv2LSAList.size()*100);
		sb.append(interASTEv2LSAList.size());
		sb.append(" LSAs\r\n");
		for (int i=0;i<interASTEv2LSAList.size();++i){
			sb.append(i);
			sb.append("--> ");
			sb.append(interASTEv2LSAList.get(i).toString());
			sb.append("\r\n");
			sb.append("------------------------\r\n");
			sb.append("\r\n");
		}
		return sb.toString();
	}
	
	public String printLSAShortList(){
		StringBuffer sb=new StringBuffer(20+interASTEv2LSAList.size()*100);
		sb.append(interASTEv2LSAList.size());
		sb.append(" LSAs\r\n");
		for (int i=0;i<interASTEv2LSAList.size();++i){
			sb.append(i);
			sb.append("--> ");
			sb.append(interASTEv2LSAList.get(i).printShort());
			sb.append("\r\n");
			sb.append("------------------------\r\n");
			sb.append("\r\n");
		}
		return sb.toString();
	}
	

	public int sizeLSAList(){
		return interASTEv2LSAList.size();
	}

}