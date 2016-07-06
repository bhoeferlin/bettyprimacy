package com.timerchina.navcluster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.timerchina.navparser.DataBean;
import com.timerchina.navparser.NavParser;

public class HacTest {
	//存放数据的矩阵
	private List[] clusterList;
    public static float[][] data;
    private int InstanceNumber ; 
    DisjointSets ds;
    
    public HacTest(int instanceNumber){
    	this.InstanceNumber = instanceNumber;
    	data = new float[InstanceNumber][InstanceNumber];
    }
    
    public void readData(String trainingFileName) {
		try {
			FileReader fr = new FileReader(trainingFileName);
			BufferedReader br = new BufferedReader(fr);
			// 存放数据的临时变量
			String lineData = null;
			String[] splitData = null;
			int line = 0;
			// 按行读取
			while (br.ready()) {
				// 得到原始的字符串
				lineData = br.readLine();
				splitData = lineData.split(" ");
				// 转化为数据
				// System.out.println("length:"+splitData.length);
				if (splitData.length > 1) {
					for (int i = 0; i < splitData.length; i++) {
						data[line][i] = Float.parseFloat(splitData[i]);					
					}
					line++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    public void normalize(){
    	float maxValue = Float.MIN_VALUE;
    	float minValue = Float.MAX_VALUE;
    	for(int i=0;i<data.length;i++){
    		for(int j=0;j<data[i].length;j++){
    			if(data[i][j]>maxValue){
    				maxValue = data[i][j];
    			}
    			if(data[i][j]<minValue){
					minValue = data[i][j];
				}
    		}
    	}
    	for(int i=0;i<data.length;i++){
//    		System.out.println("maxValue:"+maxValue+"  minValue:"+minValue);
    		for(int j=0;j<data[i].length;j++){
    			data[i][j] = (float)(data[i][j]-minValue)/(maxValue-minValue);
    		}
    	}
    } 
    
    public static void clusterAndResult() {
    	List<DataBean> dataList = NavParser.execute();
		HacTest cluster = new HacTest(dataList.size());
		//读取数据
		cluster.readData("matrixFile.txt");
		ClustererByCosin cl = new ClustererByCosin(data.length);
	    cl.cluster(data, 2);
//	    cl.cluster(data, data.length-30);
	    cl.output(dataList);
    }

	public static void main(String[] args) {
		clusterAndResult();
	}
}
