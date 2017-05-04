package com.timerchina.datarecord.parser;

public class NodeData {
	int parentIndex;
	int childIndex;
	String nodeData;
	
	public int getChildIndex() {
		return childIndex;
	}
	public void setChildIndex(int childIndex) {
		this.childIndex = childIndex;
	}
	
	public NodeData(int parentIndex, String nodeData, int childIndex) {
		this.parentIndex = parentIndex;
		this.nodeData = nodeData;
		this.childIndex = childIndex;
	}
	public int getParentIndex() {
		return parentIndex;
	}
	public void setParentIndex(int parentIndex) {
		this.parentIndex = parentIndex;
	}
	public String getNodeData() {
		return nodeData;
	}
	public void setNodeData(String nodeData) {
		this.nodeData = nodeData;
	}
}
