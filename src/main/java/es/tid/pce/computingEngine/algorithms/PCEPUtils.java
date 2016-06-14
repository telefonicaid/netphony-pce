package es.tid.pce.computingEngine.algorithms;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import es.tid.of.DataPathID;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.AvailableLabels;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.pce.pcep.constructs.Path;
import es.tid.pce.pcep.constructs.Request;
import es.tid.pce.pcep.objects.Metric;
import es.tid.pce.pcep.objects.ObjectParameters;
import es.tid.tedb.IntraDomainEdge;
import es.tid.tedb.TE_Information;

/**
 * Convenient class for useful methods used in different algorithms.
 * 
 * @author ogondio
 *
 */
public class PCEPUtils {
	
	private Logger log=LoggerFactory.getLogger("PCEServer");
	
	public static void completeMetric(Path path, Request req,List<IntraDomainEdge> edge_list){
		for (int i=0;i<req.getMetricList().size();++i){
			if (req.getMetricList().get(i).isComputedMetricBit()){
				int metric_type=req.getMetricList().get(i).getMetricType();
				switch (metric_type){
				case ObjectParameters.PCEP_METRIC_TYPE_HOP_COUNT:
					Metric metric=new Metric();
					metric.setMetricType(ObjectParameters.PCEP_METRIC_TYPE_HOP_COUNT);
					float metricValue=(float)edge_list.size();
					metric.setMetricValue(metricValue);
					path.getMetricList().add(metric);
					break;
				case ObjectParameters.PCEP_METRIC_TYPE_LATENCY_METRIC:
					Metric metricLat=new Metric();
					metricLat.setMetricType(ObjectParameters.PCEP_METRIC_TYPE_LATENCY_METRIC);
					float metricValueLat=0;
					for (int j=0;j<edge_list.size();++j){
						metricValueLat+=edge_list.get(j).getDelay_ms();
					}
					metricLat.setMetricValue(metricValueLat);
					path.getMetricList().add(metricLat);
					break;
				}
			}
		}
	}
	
	public static SimpleDirectedWeightedGraph<Object,IntraDomainEdge>  duplicateTEDDB(SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraph){
		SimpleDirectedWeightedGraph<Object,IntraDomainEdge> networkGraphDuplicated= new SimpleDirectedWeightedGraph<Object,IntraDomainEdge> (IntraDomainEdge.class);
		Set<Object> nodes= networkGraph.vertexSet();
		Iterator<Object> iter=nodes.iterator();
		Set<IntraDomainEdge> fiberEdges= networkGraph.edgeSet();
		DataPathID dpid = new DataPathID();
		Iterator<IntraDomainEdge> iterFiberLink;
		while (iter.hasNext()){
			networkGraphDuplicated.addVertex( iter.next());			
		}
		iterFiberLink=fiberEdges.iterator();
		while (iterFiberLink.hasNext()){
			IntraDomainEdge fiberEdge =iterFiberLink.next();
			IntraDomainEdge edge=new IntraDomainEdge();
			TE_Information informationTEDB=new TE_Information();
			edge.setTE_info(informationTEDB);
			informationTEDB.setAvailableLabels(new AvailableLabels());
			BitmapLabelSet newBitmapLabelSet = new BitmapLabelSet();
			int numLabels = fiberEdge.getTE_info().getAvailableLabels().getLabelSet().getNumLabels();
			newBitmapLabelSet.createBytesBitMap(((BitmapLabelSet)fiberEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
			
			int numberBytes = numLabels/8;
			if ((numberBytes*8)<numLabels){
				numberBytes++;
			}
			byte[] bytesBitMapReserved =  new byte[numberBytes];
			for (int i=0;i<numberBytes;i++)
				bytesBitMapReserved[i]=0x00;	
			newBitmapLabelSet.setBytesBitmapReserved(bytesBitMapReserved);
			newBitmapLabelSet.setNumLabels(numLabels);
			informationTEDB.getAvailableLabels().setLabelSet(newBitmapLabelSet);
			networkGraphDuplicated.addEdge(fiberEdge.getSource(),fiberEdge.getTarget(),edge);			
		}
		return networkGraphDuplicated;
	}

}
