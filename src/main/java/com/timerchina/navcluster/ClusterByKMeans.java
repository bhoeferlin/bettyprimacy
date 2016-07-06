package com.timerchina.navcluster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.experimental.theories.DataPoint;

import com.timerchina.itoolkit.common.utils.PropertiesTool;
import com.timerchina.navparser.DataBean;
import com.timerchina.navparser.NavParser;

public class ClusterByKMeans {
	/**
	 * @param args 
	 * betty.li  2015.06.25
	 * K-means算法实现
	 */
	public static PropertiesTool prop = PropertiesTool.getSpiderInstance();
	//聚类的数目
    private int ClassCount ;
	//样本数目（测试集）
    private int InstanceNumber ; 
    //样本属性数目（测试）
    private int FieldCount ;
    //自适应迭代次数
    private static int iteration = 2;
    //寻找初始点的迭代次数
    private static int initIteration= 1;
   
    //设置异常点阈值参数（每一类初始的最小数目为InstanceNumber/ClassCount^t）
    final static double t = 2;
    //存放数据的矩阵
    private float[][] data;
   
    //每个类的均值中心
    private float[][] classData;
   
    //噪声集合索引
    private ArrayList<Integer> noises;
   
    //存放每次变换结果的矩阵
    private ArrayList<ArrayList<Integer>> result;
   
//    //构造函数，初始化
    public ClusterByKMeans(int classCount, int instanceNumber, int fieldCount) {
		ClassCount = classCount;
		InstanceNumber = instanceNumber;
		FieldCount = fieldCount;
		//最后一位用来储存结果
		data = new float[InstanceNumber][FieldCount + 1];
		classData = new float[ClassCount][FieldCount];
		result = new ArrayList<ArrayList<Integer>>(ClassCount);
		noises = new ArrayList<Integer>();
	}
	/*
     * 读取测试集的数据
     *
     * @param trainingFileName 测试集文件名
     */
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
//			System.out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 聚类过程，主要分为两步 1.循环找初始点 2.不断调整直到分类不再发生变化
	 */
	public void cluster() {
		// 数据归一化
		// normalize();
		// 标记是否需要重新找初始点
		boolean needUpdataInitials = true;

		// 找初始点的迭代次数
		int times = initIteration;
		// 找初始点
		while (needUpdataInitials) {
			needUpdataInitials = false;
			result.clear();
			System.out.println("Find Initials Iteration " + (times++)
					+ " time(s)");

			// 一次找初始点的尝试和根据初始点的分类
			long start = System.currentTimeMillis();
			findInitials();
			long end = System.currentTimeMillis();
			
			firstClassify();

			// 如果某个分类的数目小于特定的阈值，则认为这个分类中的所有样本都是噪声点
			// 需要重新找初始点
			for (int i = 0; i < result.size(); i++) {
				if (result.get(i).size() < InstanceNumber
						/ Math.pow(ClassCount, t)) {
					needUpdataInitials = true;
					noises.addAll(result.get(i));
				}
			}
		}

		// 找到合适的初始点后
		// 不断的调整均值中心和分类，直到不再发生任何变化
		Adjust();
	}

	// 关于初始向量的一次找寻尝试
	public void findInitials() {
		
		// a,b为标志距离最远的两个向量的索引
		int i, j, a, b;
		i = j = a = b = 0;

		// 最远距离
		float maxDis = 0;

		// 已经找到的初始点个数
		int alreadyCls = 2;

		// 存放已经标记为初始点的向量索引
		ArrayList<Integer> initials = new ArrayList<Integer>();

		// 从两个开始
		for (; i < InstanceNumber; i++) {
			// 噪声点
			if (noises.contains(i))
				continue;
			// long startTime = System.currentTimeMillis();
			j = i + 1;
			for (; j < InstanceNumber; j++) {
				// 噪声点
				if (noises.contains(j))
					continue;
				// 找出最大的距离并记录下来
				float sumi = arraySum(data[i]);
				float sumj = arraySum(data[j]);
				float newDis=0;
				if(sumi==1.0&&sumj==1.0) {
					newDis = (float)Math.pow(2, 0.5);
				}
				else {
					newDis = calDis(data[i], data[j]);
				}
				if (maxDis < newDis) {
					a = i;
					b = j;
					maxDis = newDis;
				}
			}
			// long endTime = System.currentTimeMillis();
			// System.out.println(i +
			// "Vector Caculation Time:"+(endTime-startTime)+"ms");
		}

		// 将前两个初始点记录下来
		initials.add(a);
		initials.add(b);
		classData[0] = data[a];
		classData[1] = data[b];

		// 在结果中新建存放某样本索引的对象，并把初始点添加进去
		ArrayList<Integer> resultOne = new ArrayList<Integer>();
		ArrayList<Integer> resultTwo = new ArrayList<Integer>();
		resultOne.add(a);
		resultTwo.add(b);
		result.add(resultOne);
		result.add(resultTwo);

		// 找到剩余的几个初始点
		while (alreadyCls < ClassCount) {
			i = j = 0;
			float maxMin = 0;
			int newClass = 0;

			// 找最小值中的最大值
			for (; i < InstanceNumber; i++) {
				float min = 0;
				float newMin = 0;
				// 找和已有类的最小值
				if (initials.contains(i))
					continue;
				// 噪声点去除
				if (noises.contains(i))
					continue;
				for (j = 0; j < alreadyCls; j++) {
					//////////////////
					float sumi = arraySum(data[i]);
					float sumj = arraySum(classData[j]);
					if(sumi==1.0&&sumj==1.0) {
						newMin = (float)Math.pow(2, 0.5);
					}
					else {
						newMin = calDis(data[i], classData[j]);
					}
					////////////////////
					//newMin = calDis(data[i], classData[j]);
					if (min == 0 || newMin < min)
						min = newMin;
				}

				// 新最小距离较大,新生成一类
				if (min > maxMin) {
					maxMin = min;
					newClass = i;
				}
			}
			// 添加到均值集合和结果集合中
			// System.out.println("NewClass"+newClass);
			initials.add(newClass);
			classData[alreadyCls++] = data[newClass];
			ArrayList<Integer> rslt = new ArrayList<Integer>();
			rslt.add(newClass);
			result.add(rslt);
		}
	}

	// 第一次分类
	public void firstClassify() {
		// 根据初始向量分类
		for (int i = 0; i < InstanceNumber; i++) {
			float min = 0f;
			int clsId = -1;
			for (int j = 0; j < classData.length; j++) {
				// 欧式距离
				float newMin = calDis(classData[j], data[i]);
				if (clsId == -1 || newMin < min) {
					clsId = j;
					min = newMin;
				}

			}
			// 本身不再添加
			if (!result.get(clsId).contains(i))
				result.get(clsId).add(i);
		}
	}

	// 迭代分类，直到各个类的数据不再变化
	public void Adjust()
	{
		// 记录是否发生变化
		boolean change = true;

		// 循环的次数
		int times = iteration;
		
		while (change) {
			// 复位
			change = false;
			System.out.println("Adjust Iteration" + (times++) + "time(s)");

			// 重新计算每个类的均值
			for (int i = 0; i < ClassCount; i++) {
				// 原有的数据
				ArrayList<Integer> cls = result.get(i);

				// 新的均值
				float[] newMean = new float[FieldCount];

				// 计算均值
				for (Integer index : cls) {
					for (int j = 0; j < FieldCount; j++)
						newMean[j] += data[index][j];
				}
				for (int j = 0; j < FieldCount; j++)
					newMean[j] /= cls.size();
				if (!compareMean(newMean, classData[i])) {
					classData[i] = newMean;
					change = true;
				}
			}
			// 清空之前的数据
			for (ArrayList<Integer> cls : result)
			{
				cls.clear();
			}
				
			// 重新分配
			for (int i = 0; i < InstanceNumber; i++) {
				float min = 0f;
				int clsId = -1;
				for (int j = 0; j < classData.length; j++) {
//					float newMin = calDis(classData[j], data[i]);
					//////////////////
					float newMin = 0;
					float sumi = arraySum(classData[j]);
					float sumj = arraySum(data[i]);
					if(sumi==1.0 && sumj==1.0) {
						newMin = (float)Math.pow(2, 0.5);
					}
					else {
						newMin = calDis(classData[j],data[i]);
					}
					//////////////////
					if (clsId == -1 || newMin < min) {
						clsId = j;
						min = newMin;
					}
				}
				data[i][FieldCount] = clsId;
				result.get(clsId).add(i);
			}
		}
	}

	/**
	 * 计算a样本和b样本的欧式距离作为不相似度
	 * 
	 * @param a
	 *            样本a
	 * @param b
	 *            样本b
	 * @return 欧式距离长度
	 */
	private float calDis(float[] aVector, float[] bVector) {
		double dis = 0;
		int i = 0;
		// 最后一个数据在训练集中为结果，所以不考虑 
		for (; i < aVector.length; i++)
		{
			if((bVector[i]==0.0)&&(aVector[i]==0)) continue;
			dis += Math.pow(bVector[i] - aVector[i], 2);
		}
		dis = Math.pow(dis, 0.5);
		return (float) dis;
	}
	//余弦计算距离
	/*private float calDis(float[] aVector, float[] bVector) {
		double dis = 0;
		int i = 0;
		double denoA = 0;
		double denoB = 0;
		// 最后一个数据在训练集中为结果，所以不考虑 
		for (; i < aVector.length; i++)
		{
			if((bVector[i]==0.0)&&(aVector[i]==0)){
				continue;
			} 
			denoA += Math.pow(aVector[i], 2);
			denoB += Math.pow(bVector[i], 2);
			dis += bVector[i] * aVector[i];//分子
		}
		dis = (double)dis/(Math.pow(denoA, 0.5)*Math.pow(denoB, 0.5));
		return (float) dis;
	}*/
	         
	/**
	 * 判断两个均值向量是否相等
	 * 
	 * @param a
	 *            向量a
	 * @param b
	 *            向量b
	 * @return
	 */
	private boolean compareMean(float[] a, float[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] > 0 && b[i] > 0 && a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 将结果输出到一个文件中
	 * 
	 * @param fileName
	 */
	public HashMap<Integer, List<DataBean>> printResult(String fileName,List<DataBean> fileList) {//
		//存储聚类结果
		HashMap<Integer, List<DataBean>> clusterTempResult = new HashMap<Integer, List<DataBean>>();
		//存储一个聚类所有信息的ID,关键词和特征向量
		
		//结果写到文件，显示用
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(fileName);
			bw = new BufferedWriter(fw);
			// 写入文件
			for (int i = 0; i < InstanceNumber; i++) {
				//存储信息的ID,关键词和特征向量
				List<DataBean> informationAll = new ArrayList<DataBean>();
				//存储信息的关键词和特征向量
				String keyWord = fileList.get(i).getContent();
				int clusterTempId = Integer.parseInt(String.valueOf(data[i][FieldCount]).substring(0, 3).replace(".0", "").replace(".", ""));
				
				//已有信息加入聚类
				if(clusterTempResult.containsKey(clusterTempId)){
					clusterTempResult.get(clusterTempId).add(new DataBean(keyWord, 0, 0, 0));
				}
				else{//第一次加入聚类
					informationAll.add(new DataBean(keyWord, 0, 0, 0));
					clusterTempResult.put(clusterTempId, informationAll);
				}
				
				//结果写入文件，显示用
				bw.write(keyWord+"-=");
				bw.write(Integer.toString(clusterTempId));
				bw.newLine();
			}

			// 统计每类的数目，打印到控制台
			for (int i = 0; i < ClassCount; i++) {
				System.out.println("第" + (i + 1) + "类数目: "
						+ result.get(i).size());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭资源
			if (bw != null)
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return clusterTempResult;
	}
	
	//计算数据和
	public float arraySum(float[] array) {
		float sum = 0;
		for(int i=0;i<array.length;i++){
			sum+= array[i];
		}
		return sum;
	}
	
	public static void execute() {
		List<DataBean> fileListBeans = NavParser.execute();
		int fileSize = fileListBeans.size();
		ClusterByKMeans cluster = new ClusterByKMeans(2, fileSize, 3);
	    //读取数据
		cluster.readData("matrixFile.txt");
		// 聚类过程
		cluster.cluster();
		//输出到文件，显示用
		HashMap<Integer, List<DataBean>> clusterTempResult = cluster
				.printResult("Statistics"+System.currentTimeMillis()+".txt", fileListBeans);
	}
	
	public static void main(String[] args) {
		ClusterByKMeans.execute();
	}
}
