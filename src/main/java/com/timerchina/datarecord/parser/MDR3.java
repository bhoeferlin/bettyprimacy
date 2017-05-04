package com.timerchina.datarecord.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/*
 * MDR：MiningDataRecord(核心类)
 * DataRecord：一条记录，相当于我们的block
 * 结合目录结构、mdr结果等对算法进行相关改进的（改进结果是否合理有待测试）
 */
public class MDR3
{
	public double simThreshold = 0.8;// 相似度阈值
	public int K = 10;// 广义节点DataRecord最大的节点数

	/*
	 * 深度优先遍历DOMTree prv--当前NodeIndex
	 */
	private void DfsTree(Element root, Tree tr, int nodeIndex) {
		if (root.tagName().endsWith("script"))
			return;
		if (root.tagName().endsWith("meta"))
			return;
		if (root.tagName().endsWith("style"))
			return;
		if (root.tagName().endsWith("font"))
			return;
		int z = tr.n;
		String nStr = root.tagName() + " ";
		if (!(root.attr("class").endsWith("odd") || root.attr("class")
				.endsWith("even")))
			nStr = nStr + root.attr("class");
		tr.InsertNode(nStr, nodeIndex);
		tr.cmt.set(z, root);
		for (Element ele : root.children())
			DfsTree(ele, tr, z);
	}

	/*
	 * 根据Jsoup将Html转化成DOMTree
	 */
	public Tree genDOMTree(String html) {
		// content = Jsoup.clean(content, Whitelist.relaxed());
		try {
			Document doc = Jsoup.parse(html);
			Element start = doc.getElementsByTag("html").first();
			Tree tr = new Tree();
			DfsTree(start, tr, -1);
			tr.generateIndexMap();
			return tr;
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
		return new Tree();
	}

	/*
	 * 根据部分树对齐的结果计算两颗树之间的相似度
	 */
	static public double Similarity(Tree t1, int n1, Tree t2, int n2) {
		return 2.0 * PartialTreeAlignment.STM(t1, n1, t2, n2).length
				/ (t1.getSize(n1) + t2.getSize(n2));
	}

	/*
	 * 计算nodeIndex节点下child1起始偏移offset个孩子节点与child2起始偏移offset个孩子节点的两颗子树的相似度
	 */
	static public double Similarity(Tree tr, int nodeIndex, int child1,
			int child2, int offset) {
		int z = tr.InsertNode("vnode", -1);
		for (int i = 0; i < offset; i++) {
			tr.chs.get(z).add(tr.getChild(nodeIndex, child1 + i));
		}
		z = tr.InsertNode("vnode", -1);
		for (int i = 0; i < offset; i++) {
			tr.chs.get(z).add(tr.getChild(nodeIndex, child2 + i));
		}
		double ret = Similarity(tr, tr.n - 1, tr, tr.n - 2);
		tr.RemoveNode();
		tr.RemoveNode();
		return ret;
	}

	/*
	 * 从nodeIndex位置获取DataRecord(广义节点)
	 */
	public List<String> fetchDR(Tree tr, int nodeIndex) {
		if (tr.getDepth(nodeIndex) < 3)
			return new ArrayList<String>();
		int kk = tr.chs.get(nodeIndex).size();
		List<String> chlTreeSimList = new ArrayList<String>();// 以字符串形式，空格作为分隔符存放每个节点下所有子树的相似度
		List<String> DRList = new ArrayList<String>();// 以字符串形式，空格作为分隔符存放每个可能的DR
		Set<Integer> DRNodeSet = new HashSet<Integer>();

		// 计算所有子树的相似度
		for (int k = 1; k <= K; k++) {
			for (int si = 0; si < k; si++)
				for (int i = si, j = 0; i + k <= kk; i = j + k) {
					double sum = 0, sim;
					for (j = i; j + k + k <= kk; j += k) {
						sim = Similarity(tr, nodeIndex, j, j + k, k);
						if (sim < simThreshold)
							break;
						sum += sim;
					}
					sim = Similarity(tr, nodeIndex, i, j, k);
					if (i < j && sim > simThreshold) {
						sum += sim;
						StringBuffer stringBuffer = new StringBuffer();
						stringBuffer.append(nodeIndex);
						stringBuffer.append(" ");
						stringBuffer.append(i);
						stringBuffer.append(" ");
						stringBuffer.append(j);
						stringBuffer.append(" ");
						stringBuffer.append(k);
						stringBuffer.append(" ");
						// 滑动节点的相似度取均值
						stringBuffer.append(sum * k / (j - i + k));
						chlTreeSimList.add(stringBuffer.toString());
					}
				}
		}
		for (String simStr : chlTreeSimList) {
			String[] simArray = simStr.split(" ");
			int k = Integer.parseInt(simArray[3]), child1 = Integer
					.parseInt(simArray[1]), child2 = Integer
					.parseInt(simArray[2]), flag = 1;
			double sim = Double.parseDouble(simArray[4]);
			for (String tempSimStr : chlTreeSimList) {
				if (tempSimStr.equals(simStr))
					continue;
				simArray = tempSimStr.split(" ");
				int tempK = Integer.parseInt(simArray[3]), tempChild1 = Integer
						.parseInt(simArray[1]), tempChild2 = Integer
						.parseInt(simArray[2]);
				double tempSim = Double.parseDouble(simArray[4]);
				/*
				 * 遍历所有子树相似度，查找DR，其中，已经比较过的不再比较
				 */
				if (!(child2 < tempChild1 || child1 > tempChild2)) {
					if (tempK < k)
						flag = 0;
					if (tempK == k && tempChild2 - tempChild1 > child2 - child1)
						flag = 0;
					if (tempK == k
							&& tempChild2 - tempChild1 == child2 - child1
							&& tempSim > sim)
						flag = 0;
					if (tempK == k
							&& tempChild2 - tempChild1 == child2 - child1
							&& tempSim == sim && child1 < tempChild1)
						flag = 0;
				}

				if (flag == 0)
					break;
			}
			if (flag == 1)
				DRList.add(simStr);
		}
		
		for (String DRStr : DRList) {
			String[] simAarry = DRStr.split(" ");
			int child1 = Integer.parseInt(simAarry[1]), child2 = Integer
					.parseInt(simAarry[2]);
			for (int i = child1; i <= child2; i++)
				DRNodeSet.add(i);
		}

		for (int i = 0; i < kk; i++) {
			if (DRNodeSet.contains(i))
				continue;
			for (String z : fetchDR(tr, tr.getChild(nodeIndex, i)))
				DRList.add(z);
		}
		return DRList;
	}

	/*
	 * 将DR的下标转化为DomTree中的子树
	 */
	public NodeData[] parseDR(Tree tr, String DRStr) {
		String[] DRArray = DRStr.split(" ");
		int node = Integer.parseInt(DRArray[0]), child1 = Integer
				.parseInt(DRArray[1]);
		int child2 = Integer.parseInt(DRArray[2]), offset = Integer
				.parseInt(DRArray[3]);
		List<NodeData> resultList = new ArrayList<NodeData>();
		for (int i = child1; i <= child2; i += offset) {
			int vNode = tr.InsertNode("vnode", -1);
			
			for (int j = 0; j < offset; j++)
			{
				tr.chs.get(vNode).add(tr.getChild(node, i + j));
			}

			List<String> tempDRList = null;

			if (offset > 1)
				tempDRList = fetchDR(tr, vNode);

			if (offset == 1 || tempDRList.isEmpty()) {
				int vanode = tr.getChild(node, i);
				int parentIndex = tr.childParentMap.get(vanode);
				String data = ((Element) tr.cmt.get(tr.getChild(node, i)))
						.outerHtml();
				resultList.add(new NodeData(parentIndex, data, vanode));
			} else {
				for (String tempDR : tempDRList)
					for (NodeData result : parseDR(tr, tempDR))
						resultList.add(result);
			}
			tr.RemoveNode();
		}
		return resultList.toArray(new NodeData[resultList.size()]);
	}
	/*
	 * 遍历整颗DomTree获取MainDR
	 */
	public List<NodeData[]> fetchMainDR(Tree tree) {//String[]
//		System.out.println("总节点数n："+tree.n);
		Map<Integer, List<NodeData>> clusterDataMap = new HashMap<Integer, List<NodeData>>();
		List<NodeData[]> resultList = new ArrayList<NodeData[]>();
		String[] result = new String[0];
		int max = 0, feature, countA;
		int minA = Integer.MAX_VALUE; 
		try {
			int vIndex = tree.InsertNode("vroot", -1);
			for (String DRStr : fetchDR(tree, 0)) {
//				System.out.println(DRStr);
				String[] DRArray = DRStr.split(" ");
				int node = Integer.parseInt(DRArray[0]);
				int child1 = Integer.parseInt(DRArray[1]);
				int child2 = Integer.parseInt(DRArray[2]), offset = Integer
						.parseInt(DRArray[3]);
//				System.out.println("node:"+node+"\t"+tree.cmt.get(node));
//				System.out.println("===============");
				for (int i = child1; i < child2 + offset; i++)
					tree.chs.get(vIndex).add(tree.getChild(node, i));
			}
			// System.out.println("===============");

			/*
			 * 将所有可能的DR集合统一到vNode下，计算出最终的DR集合,
			 * 取Text总和最大的DR作为唯一的DR      
			 */
			List<NodeData[]> resultBeanList = new ArrayList<NodeData[]>();
			for (String DRStr : fetchDR(tree, vIndex)) {
//				System.out.println(DRStr);
//				System.out.println("===============");
				NodeData[] drBean = parseDR(tree, DRStr);
				resultBeanList.add(drBean);
			}
			structureCluster(clusterDataMap, resultBeanList);
			nestSelect(clusterDataMap, resultBeanList);
			for(int parentIndex:clusterDataMap.keySet()){
				List<NodeData> DRArray = clusterDataMap.get(parentIndex);
				/*
				int i = 0;
				String[] data = new String[DRArray.size()];
				for (NodeData temp : DRArray)
				{
					 data[i++] = temp.getNodeData();
					
				}
				resultList.add(data);
				*/
				resultList.add(DRArray.toArray(new NodeData[DRArray.size()]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultList;
	}
	
	/*
	 * 遍历整颗DomTree获取MainDR
	 */
	public List<NodeData[]> fetchModifyMainDR(String html) {//List<NodeData[]>
		Tree tree = genDOMTree(html);
		int num = tree.n;
		Map<Integer, List<NodeData>> clusterDataMap = new HashMap<Integer, List<NodeData>>();
		List<NodeData[]> resultList = new ArrayList<NodeData[]>();
		List<NodeData> resultTemp = new ArrayList<NodeData>();
		
		int max = 0, feature, countA;
		float minA = Float.MAX_VALUE; 
		try {
			int vIndex = tree.InsertNode("vroot", -1);
			for (String DRStr : fetchDR(tree, 0)) {
//				System.out.println(DRStr);
				String[] DRArray = DRStr.split(" ");
				int node = Integer.parseInt(DRArray[0]);
				int child1 = Integer.parseInt(DRArray[1]);
				int child2 = Integer.parseInt(DRArray[2]), offset = Integer
						.parseInt(DRArray[3]);
//				System.out.println("node:"+node+"\t"+tree.cmt.get(node));
//				System.out.println("===============");
				for (int i = child1; i < child2 + offset; i++)
					tree.chs.get(vIndex).add(tree.getChild(node, i));
			}
			// System.out.println("===============");

			/*
			 * 将所有可能的DR集合统一到vNode下，计算出最终的DR集合
			 */
			List<NodeData[]> resultBeanList = new ArrayList<NodeData[]>();
			for (String DRStr : fetchDR(tree, vIndex)) {
//				System.out.println("DataRecords:" + DRStr);
//				System.out.println("===============");
				NodeData[] drBean = parseDR(tree, DRStr);
				resultBeanList.add(drBean);
			}
			
			//结构聚类
			structureCluster(clusterDataMap, resultBeanList);
			//嵌套处理（父亲节点的childIndex等于孩子节点parentIndex）
			nestSelect(clusterDataMap, resultBeanList);
			//parentIndex/n，剔除不满足阈值条件的
			indexSelect(num, clusterDataMap);
			
			//结果筛选
			for(int parentIndex:clusterDataMap.keySet()){
				List<NodeData> DRArray = clusterDataMap.get(parentIndex);
				String strAll = "";
				String strRawAll = "";
				feature = 0;
				countA = 0;
				
				for (NodeData temp : DRArray)
				{
					String tempDR = temp.getNodeData();
					strRawAll += tempDR;
					String str = tempDR.replaceAll("\\s+", "").replaceAll("<.*?>", "");
					feature += str.length();
					strAll += str;
//					System.out.println("feature长度:"+feature);
					//统计超链接标签密度
					countA += DomTreeUtils.numHyperlink(tempDR); 
				}
				float linkaDensity = (float)countA/(feature + 1);
				float tagTextLengthAvg = tagTextAvgCalculate(strAll, strRawAll);
//				System.out.println("********************************");
//				System.out.println(strRawAll);
//				System.out.println("超链接标签密度：" + linkaDensity);
				
				//剔除超链接标签密度不符合指定条件的（<0.05）;剔除标签块平均文本长度不符合条件的（>0.2）;统计每个块的文本长度（>30）
				if( linkaDensity > 0.05 && linkaDensity < 0.1 &&tagTextLengthAvg>2 && feature > 30){
					resultList.add(DRArray.toArray(new NodeData[DRArray.size()]));
				}
//				
//				if(linkaDensity < minA && feature > 30){     //
//					resultTemp = DRArray;
//					minA = linkaDensity;
//				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
//		String[] result = new String[resultTemp.size()];
//		for (int i = 0; i < resultTemp.size(); i++)
//		{
//			String data = resultTemp.get(i).getNodeData().replace("\n", "");
//			result[i] = data;
//		}

//		return result;
		return resultList;
	}
	/**
	 * 计算每个块的标签文本平均长度
	 * =(节点块总文本长度)/(节点总个数) 
	 **/
	private float tagTextAvgCalculate(String strAll, String strRawAll) {
		float tagTextAvg = 0;
		int WordLength = DomTreeUtils.getChineseLength(strAll);
		int tagNum = DomTreeUtils.numTag(strRawAll);
		tagTextAvg = (float)(WordLength+1)/(tagNum+1);
		return tagTextAvg;
	}
	
	/**
	 * 如果若干相似块具有相同的父节点，应该属于同一个相似块，将他们进行合并
	 * 该方法目前只考虑上一层的父节点相同情况，可能会有遗漏
	 **/
	private void structureCluster(Map<Integer, List<NodeData>> clusterDataMap,
			List<NodeData[]> resultBeanList) {
		for(NodeData[] drBean:resultBeanList){
			int parentIndex = -1;
			List<NodeData> dataList = new ArrayList<NodeData>();
			for(NodeData dr:drBean){
				parentIndex = dr.parentIndex;
//					String data = dr.getNodeData();
				dataList.add(dr);
			}
			if(!clusterDataMap.containsKey(parentIndex)){
				clusterDataMap.put(parentIndex, dataList);
			}else {
				clusterDataMap.get(parentIndex).addAll(dataList);
			}
		}
	}
	/**
	 * 根据是否嵌套，将结果相似块中，嵌套的子模块并入父模块
	 * 该方法没有考虑交叉嵌套的情况，具体效果不知  
	 **/
	private void nestSelect(Map<Integer, List<NodeData>> clusterDataMap,
			List<NodeData[]> resultBeanList) {
		Map<Integer, List<NodeData>> clusterDataMapTemp = new HashMap<Integer, List<NodeData>>();
		clusterDataMapTemp = clusterDataMap;
		for(NodeData[] nodeDataArr:resultBeanList){
			int parentIndex = nodeDataArr[0].getParentIndex();
			for(int pIndex:clusterDataMapTemp.keySet()){
				if(pIndex==parentIndex) continue;
				List<NodeData> nodeDataList = clusterDataMapTemp.get(pIndex);
				for(NodeData drBean:nodeDataList){
					int childIndex = drBean.getChildIndex();
					if(childIndex == parentIndex){//孩子节点置于父节点区域
						List<NodeData> childNodeDataList = clusterDataMapTemp.get(parentIndex);
						try {
							clusterDataMap.get(pIndex).addAll(childNodeDataList);
							clusterDataMap.remove(parentIndex);
						} catch (Exception e) {
							continue;
						}
					}
				}
			}
		}
	}

	/**
	 * 根据节点索引剔除不满足阈值条件的节点块
	 * 阈值：0.95 
	 **/
	private void indexSelect(int num,Map<Integer, List<NodeData>> clusterDataMap) {
		List<Integer> removeIndexList = new ArrayList<Integer>();
		for(int pI:clusterDataMap.keySet()){
			float indexRatio = (float)pI/num;
			if(indexRatio > 0.95|| indexRatio < 0.05)
				removeIndexList.add(pI);
		}
		for(int index:removeIndexList)
			clusterDataMap.remove(index);
	}
	
}
