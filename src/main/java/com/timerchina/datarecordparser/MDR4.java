package com.timerchina.datarecordparser;

import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

import com.timerchina.spider.downloader.HttpClientDownloader;
import com.timerchina.spider.pojo.Page;
import com.timerchina.spider.pojo.Request;

/*
 * MDR：MiningDataRecord(核心类)
 * DataRecord：一条记录，相当于我们的block
 */
public class MDR4
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
	public List<NodeData[]> fetchMainDR(Tree tree,int parentIndex) {
		List<NodeData[]> resultList = new ArrayList<NodeData[]>();
		try {
			int vIndex = tree.InsertNode("vroot", -1);
			for (String DRStr : fetchDR(tree, 0)) {
				String[] DRArray = DRStr.split(" ");
				int node = Integer.parseInt(DRArray[0]);
				int child1 = Integer.parseInt(DRArray[1]);
				int child2 = Integer.parseInt(DRArray[2]), offset = Integer
						.parseInt(DRArray[3]);
				for (int i = child1; i < child2 + offset; i++)
					tree.chs.get(vIndex).add(tree.getChild(node, i));
			}

			/*
			 * 将所有可能的DR集合统一到vNode下，计算出最终的DR集合,
			 */
			for (String DRStr : fetchDR(tree, parentIndex)) {
				NodeData[] drBean = parseDR(tree, DRStr);
				resultList.add(drBean);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultList;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MDR4 lp = new MDR4();
		Request request = new Request();
		request.setUrl("http://www.xcar.com.cn/bbs/forumdisplay.php?fid=739");
		Page page = new HttpClientDownloader().download(request);
		String content = page.getRawText();
		Tree tree = lp.genDOMTree(content);
		
		long start = System.currentTimeMillis();
		List<NodeData[]> resultList = lp.fetchMainDR(tree, 240);
		long end = System.currentTimeMillis();
		System.out.println("****MDR解析共用时"+(end-start)/1000+"s****");
		
		System.out.println(resultList.size());
	}
}
