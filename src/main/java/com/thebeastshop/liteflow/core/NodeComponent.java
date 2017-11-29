/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.data.DataBus;
import com.thebeastshop.liteflow.entity.data.Slot;
import com.thebeastshop.liteflow.entity.monitor.CompStatistics;
import com.thebeastshop.liteflow.monitor.MonitorBus;
import com.thebeastshop.liteflow.parser.FlowParser;

public abstract class NodeComponent {
	
	private static final Logger LOG = LoggerFactory.getLogger(NodeComponent.class);
	
	private InheritableThreadLocal<Integer> slotIndexTL = new InheritableThreadLocal<Integer>();
	
	private String nodeId;
	
	private boolean continueOnError;
	
	public void execute() throws Exception{
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		long initm=Runtime.getRuntime().freeMemory();
		
		process();
		stopWatch.stop();
		long timeSpent = stopWatch.getTime();
		long endm=Runtime.getRuntime().freeMemory();
		
		this.getSlot().addStep(nodeId);
		
		//性能统计
		CompStatistics statistics = new CompStatistics();
		statistics.setComponentClazzName(this.getClass().getSimpleName());
		statistics.setTimeSpent(timeSpent);
		statistics.setMemorySpent(initm-endm);
		MonitorBus.addStatistics(statistics);
		
		
		if(this instanceof NodeCondComponent){
			String condNodeId = this.getSlot().getCondResult(this.getClass().getName());
			if(StringUtils.isNotBlank(condNodeId)){
				Node thisNode = FlowParser.getNode(nodeId);
				Node condNode = thisNode.getCondNode(condNodeId);
				if(condNode != null){
					NodeComponent condComponent = condNode.getInstance();
					condComponent.setSlotIndex(slotIndexTL.get());
					condComponent.execute();
				}
			}
		}
		
		LOG.debug("componnet[{}] finished in {} milliseconds",this.getClass().getSimpleName(),timeSpent);
	}
	
	protected abstract void process() throws Exception;
	
	protected boolean isAccess(){
		return true;
	}
	
	public boolean isContinueOnError() {
		return continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public NodeComponent setSlotIndex(Integer slotIndex) {
		this.slotIndexTL.set(slotIndex);
		return this;
	}
	
	public Slot getSlot(){
		return DataBus.getSlot(this.slotIndexTL.get());
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}