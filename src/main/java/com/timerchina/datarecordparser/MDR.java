package com.timerchina.datarecordparser;

import java.io.*;
import java.util.*;

import org.jsoup.*;
import org.jsoup.nodes.*;

/*
 * MDR：MiningDataRecord(核心类)
 * DataRecord：一条记录，相当于我们的block
 */
public class MDR
{
	public double simThreshold = 0.8;//相似度阈值
	public int K = 10;//广义节点DataRecord最大的节点数
	
	/*
	 * 深度优先遍历DOMTree
	 * prv--当前NodeIndex
	 */
	private void DfsTree(Element root, Tree tr, int nodeIndex)
	{
		if (root.tagName().endsWith("script"))  return;
		if (root.tagName().endsWith("meta"))  return;
		if (root.tagName().endsWith("style"))  return;
		if (root.tagName().endsWith("font"))  return;
		int z = tr.n;
		String nStr = root.tagName() + " ";
		if (!(root.attr("class").endsWith("odd") || root.attr("class").endsWith("even")) )
			nStr = nStr + root.attr("class");
		tr.InsertNode(nStr, nodeIndex);
		tr.cmt.set(z, root);
		for (Element ele : root.children())
			DfsTree(ele, tr, z);
	}

	/*
	 *根据Jsoup将Html转化成DOMTree 
	 * 
	 */
	public Tree genDOMTree(String html)
	{
		//content = Jsoup.clean(content, Whitelist.relaxed());
		try
		{
			Document doc = Jsoup.parse(html);
			Element start = doc.getElementsByTag("html").first();
			Tree tr = new Tree();
			DfsTree(start, tr, -1);
			return tr;
		}
		catch (Exception ex)
		{
			System.out.println(ex.toString());
		}
		return new Tree();
	}
	
	/*
	 * 根据部分树对齐的结果计算两颗树之间的相似度
	 */
	static public double Similarity(Tree t1, int n1, Tree t2, int n2)
	{
		return 2.0 * PartialTreeAlignment.STM(t1, n1, t2, n2).length / (t1.getSize(n1) + t2.getSize(n2));
	}
	
	/*
	 * 计算nodeIndex节点下child1起始偏移offset个孩子节点与child2起始偏移offset个孩子节点的两颗子树的相似度
	 */
	static public double Similarity(Tree tr, int nodeIndex, int child1, int child2, int offset)
	{
		int z = tr.InsertNode("vnode", -1);
		for (int i = 0; i < offset; i ++)
		{
			tr.chs.get(z).add(tr.getChild(nodeIndex, child1+i));
		}
		z = tr.InsertNode("vnode", -1);
		for (int i = 0; i < offset; i ++)
		{
			tr.chs.get(z).add(tr.getChild(nodeIndex, child2+i));
		}
		double ret = Similarity(tr, tr.n-1, tr, tr.n-2);
		tr.RemoveNode();
		tr.RemoveNode();
		return ret;
	}
	
	/*
	 * 从nodeIndex位置获取DataRecord(广义节点)
	 */
	public List<String> FindDR(Tree tr, int nodeIndex)
	{
		if (tr.getDepth(nodeIndex) < 3)  return new ArrayList<String>();
		int kk = tr.chs.get(nodeIndex).size();
		List<String> chlTreeSimList = new ArrayList<String>();//以字符串形式，空格作为分隔符存放每个节点下所有子树的相似度
		List<String> DRList = new ArrayList<String>();//以字符串形式，空格作为分隔符存放每个可能的DR
		Set<Integer> DRNodeSet = new HashSet<Integer>();
		
		//计算所有子树的相似度
		for (int k = 1; k <= K; k ++)
		{
			for (int si = 0; si < k; si ++)
				for (int i = si, j = 0; i + k <= kk; i = j + k)
				{
					double sum = 0, sim;
					for (j = i; j + k + k <= kk; j += k)
					{
						sim = Similarity(tr, nodeIndex, j, j+k, k);
						if (sim < simThreshold)  break;
						sum += sim;
					}
					sim = Similarity(tr, nodeIndex, i, j, k);
					if (i < j && sim > simThreshold)	
					{
						sum += sim;
						StringBuffer stringBuffer = new StringBuffer();
						stringBuffer.append(nodeIndex);	stringBuffer.append(" ");
						stringBuffer.append(i);	stringBuffer.append(" ");
						stringBuffer.append(j);	stringBuffer.append(" ");
						stringBuffer.append(k);	stringBuffer.append(" ");
						//滑动节点的相似度取均值
						stringBuffer.append(sum * k / (j - i + k));
						chlTreeSimList.add(stringBuffer.toString());
					}
				}
		}
		
		for (String simStr : chlTreeSimList)
		{
			String[] simArray = simStr.split(" ");
			int k = Integer.parseInt(simArray[3]), child1 = Integer.parseInt(simArray[1]), child2 = Integer.parseInt(simArray[2]), flag = 1;
			double sim = Double.parseDouble(simArray[4]);
			for (String tempSimStr : chlTreeSimList)
			{
				if (tempSimStr.equals(simStr))  continue;
				simArray = tempSimStr.split(" ");
				int tempK = Integer.parseInt(simArray[3]), tempChild1 = Integer.parseInt(simArray[1]), tempChild2 = Integer.parseInt(simArray[2]);
				double tempSim = Double.parseDouble(simArray[4]);
				/*
				 *遍历所有子树相似度，查找DR
				 *其中，已经比较过的不再比较 
				 */
				if ( !(child2 < tempChild1 || child1 > tempChild2) )
				{
					if (tempK < k)  flag = 0;
					if (tempK == k && tempChild2 - tempChild1 > child2 - child1)  flag = 0;
					if (tempK == k && tempChild2 - tempChild1 == child2 - child1 && tempSim > sim)  flag = 0;
					if (tempK == k && tempChild2 - tempChild1 == child2 - child1 && tempSim == sim && child1 < tempChild1) flag = 0;
				}
				
				if (flag == 0)  break;
			}
			if (flag == 1)
				DRList.add(simStr);
		}
		
		for (String DRStr : DRList)
		{
			String[] simAarry = DRStr.split(" ");
			int child1 = Integer.parseInt(simAarry[1]), child2 = Integer.parseInt(simAarry[2]);
			for (int i = child1; i <= child2; i ++)
				DRNodeSet.add(i);
		}
		
		for (int i = 0; i < kk; i ++)
		{
			if (DRNodeSet.contains(i))  continue;
			for (String z : FindDR(tr, tr.getChild(nodeIndex, i)))
				DRList.add(z);
		}
		return DRList;
	}
	
	public String[] ParseDR(Tree tr, String rr)
	{
		String[] rv = rr.split(" ");
		int rt = Integer.parseInt(rv[0]), ii = Integer.parseInt(rv[1]);
		int jj = Integer.parseInt(rv[2]), kk = Integer.parseInt(rv[3]);
		List<String> ret = new ArrayList<String>();
		for (int i = ii; i <= jj; i += kk)
		{
			int z = tr.InsertNode("vnode", -1);
			for (int j = 0; j < kk; j ++)
				tr.chs.get(z).add(tr.getChild(rt, i+j));
			
			List<String> fd = null;
			
			if (kk > 1) 
				fd = FindDR(tr, z);
			
			if (kk == 1 || fd.isEmpty())
			{
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < kk; j ++)
					sb.append(((Element)tr.cmt.get(tr.getChild(rt, i+j))).outerHtml());
				ret.add(sb.toString());
			} else 
			{
				for (String pdr : fd)
					for (String nrr : ParseDR(tr, pdr))
						ret.add(nrr);
			}
			tr.RemoveNode();
		}
		return ret.toArray(new String[ret.size()]);
	}
	
	
	public void parse(String content)
	{
		Tree tr = genDOMTree(content);
		
		BufferedWriter bw;
		try
		{
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("temp.txt"), "UTF-8"));
			for (String tempString : FindDR(tr, 0))
			{
				System.out.println(tempString);
				String[] sv = ParseDR(tr, tempString);
				if (sv.length < 2)  continue;
				for (String ss : sv)
					bw.write(ss.replace("\n", "") + "\n");
				bw.write("-------------\n");
			}
			
			bw.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * 遍历整颗DomTree获取MainDR
	 */
	public String[] FindMainDR(String html)
	{
		Tree tree = genDOMTree(html);
		String[] ret = new String[0];
		int ttl = 0, ntl;
		try
		{
			int vIndex = tree.InsertNode("vroot", -1);
			for (String rr : FindDR(tree, 0))
			{
				String[] rv = rr.split(" ");
				int rt = Integer.parseInt(rv[0]), ii = Integer.parseInt(rv[1]);
				int jj = Integer.parseInt(rv[2]), kk = Integer.parseInt(rv[3]);
				for (int i = ii; i < jj + kk; i ++)
					tree.chs.get(vIndex).add(tree.getChild(rt, i));
			}
			
			for (String rr : FindDR(tree, vIndex))
			{
				String[] sv = ParseDR(tree, rr);
				ntl = 0;
				for (String z : sv)
					ntl += z.length();
				if (ntl > ttl)
				{
					ret = sv;
					ttl = ntl;
				}
			}
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		for (int i = 0; i < ret.length; i ++)
			ret[i] = ret[i].replace("\n", "");
		
		return ret;
	}
	
	public void OutputTemp(String[] rs)
	{
		BufferedWriter bw;
		try
		{
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("temp1.txt"), "UTF-8"));
			for (String rr : rs)
				bw.write(rr.replace("\n", "") + "\n");
			bw.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String[] ParseMain(String content)
	{
		Tree tr = genDOMTree(content);
		String[] ret = new String[0];
		double maxAvg = 0;
		try
		{
			for (String rr : FindDR(tr, 0))
			{
				String[] sv = ParseDR(tr, rr);
				if (sv.length < 4)  continue;
				double sm = 0;
				for (int i = 0; i < sv.length; i ++)
				{
					sv[i] = sv[i].replace("\n", "");
					sm += sv[i].length();
				}
				sm = sm / sv.length;
				if (sm > maxAvg)
				{
					maxAvg = sm;
					ret = sv;
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return ret;
	}

	public void Test()
	{
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream("zhaojincapital.txt"), "UTF-8"));
			List<String> tn = new ArrayList<String>();
			List<String> tt = new ArrayList<String>();
			StringBuffer stringBuffer = new StringBuffer("");
			String sr = null;
			while ((sr = br.readLine()) != null)
			{
				stringBuffer.append(sr);
				
			}
			br.close();
			
			String[] ss = stringBuffer.toString().split("!=-=!");
			tn.add(ss[0]);
			tt.add(ss[1]);
			
			File dir = new File("test/");  
			dir.mkdirs();
			
			BufferedWriter bw;
			for (int i = 0; i < tn.size(); i ++)
			{
				System.out.println(tn.get(i));
				String content = tt.get(i), fn = "test/";

				bw = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(fn + tn.get(i) + ".txt"), "UTF-8"));
				for (String z : FindMainDR(content))
					bw.write(z + "\n");
				bw.close();
				
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(fn + tn.get(i) + ".html"), "UTF-8"));
				bw.write(content.replace("gb2312", "utf-8"));
				bw.close();
			}
		}
		catch (Exception ex)
		{
			System.out.println(ex.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream("tot.txt"), "UTF-8"));
			List<String> tempList = new ArrayList<String>();
			String string = null;
			while ((string = br.readLine()) != null)
			{
				String[] stringArray = string.split("\t");
				if (stringArray.length < 2)  break;
				tempList.add(stringArray[1]);
			}
			br.close();
			/*
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream("aa.html"), "UTF-8"));
			String bing = br.readLine();
			br.close();*/
			MDR lp = new MDR();
//			String content = tempList.get(0);
//			lp.parse(content);
//			lp.OutputTemp(lp.FindMainDR(content));
			
			lp.Test();
		}
		catch (Exception ex)
		{
			System.out.println(ex.toString());
		}
	}

}
