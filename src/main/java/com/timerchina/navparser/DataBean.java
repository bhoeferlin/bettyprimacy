package com.timerchina.navparser;

public class DataBean {
	String content;
	float linkTextRatio;
	float editDis;
	float derivs;
	
	public float getDerivs() {
		return derivs;
	}
	public void setDerivs(float derivs) {
		this.derivs = derivs;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public float getLinkTextRatio() {
		return linkTextRatio;
	}
	public void setLinkTextRatio(float linkTextRatio) {
		this.linkTextRatio = linkTextRatio;
	}
	public float getEditDis() {
		return editDis;
	}
	public void setEditDis(float editDis) {
		this.editDis = editDis;
	}
	public DataBean(String content,float linkTextRatio,float editDis,float derivs) {
		this.content = content;
		this.linkTextRatio = linkTextRatio;
		this.editDis = editDis;
		this.derivs = derivs;
	}
}
