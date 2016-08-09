package com.timerchina.datarecordparser;

import java.io.*;
import java.util.*;

public class Tree implements Comparable
{
	public int n;//存放树中节点总数
	public List<String> dat;//存放树中所有节点的dat域
	public List<Object> cmt;//存放树中所有节点的Dom子树
	public List<List<Integer>> chs; //存放每个节点的孩子节点,List表示
	public Map<Integer, Integer> childParentMap;//存放每个孩子节点的父节点索引
	
	public Tree()
	{
		n = 0;
		dat = new ArrayList<String>();
		cmt = new ArrayList<Object>();
		chs = new ArrayList<List<Integer>>();
		childParentMap = new HashMap<Integer, Integer>();
	}
	
	public int getChild(int r, int no)
	{
		return chs.get(r).get(no);
	}
	
	public void LoadTree(String filename)
	{
		try
		{
			Scanner scanner = new Scanner(new File(filename));
			while (scanner.hasNextLine())
			{
				String[] ss = scanner.nextLine().trim().split(" ");
				dat.add(ss[0]);
				chs.add(new ArrayList<Integer>());
				int pp = Integer.parseInt(ss[1]);
				if (pp >= 0)
					chs.get(pp).add(n);
				n ++;
			}
			scanner.close();
		}
		catch (Exception ex)
		{
			System.out.println(ex.toString());
		}
	}
	
	public int getSize(int nd)
	{
		int ret = 1;
		for (int x : chs.get(nd))
			ret += getSize(x);
		return ret;
	}
	
	/*
	 * 获取某颗子树的层高
	 */
	public int getDepth(int nodeIndex)
	{
		int ret = 0;
		for (int x : chs.get(nodeIndex))
			ret = Math.max(ret, getDepth(x));
		return ret + 1;
	}
	
	/*
	 * sr:某个node节点的key
	 * pp:该节点的父亲节点Index
	 * no:该节点对应父亲节点子树中下标
	 */
	public int InsertNode(String sr, int pp, int no)
	{
		int z = n;
		dat.add(sr);
		cmt.add(null);
		chs.add(new ArrayList<Integer>());
		if (pp >= 0)  
		{
			if (no >= 0) chs.get(pp).add(no, n);
			else chs.get(pp).add(n);
		}
		n ++;
		return z;
	}
	
	public void RemoveNode()
	{
		n --;
		dat.remove(n);
		cmt.remove(n);
		chs.remove(n);
	}
	
	public int InsertNode(String sr, int pp)
	{
		return InsertNode(sr, pp, -1);
	}
	
	public void Output()
	{
		System.out.println(n);
		int[] qq = new int[n];
		int[] pp = new int[n];
		int qh = 0, ql = 0;
		qq[ql++] = 0;  pp[0] = -1;
		while (qh < ql && qh < 100)
		{
			int z = qq[qh++];
			System.out.print(z);
			System.out.print(" ");
			System.out.print(dat.get(z));
			System.out.print(" ");
			if(cmt != null && cmt.size()-1>z){
				System.out.print(cmt.get(z).toString());
			}
			System.out.print(" ");
			System.out.println(pp[z]);
			for (int x : chs.get(z))
			{
				qq[ql++] = x;
				pp[x] = z;
			}
		}
	}
	/**
	 * @param args
	 * 生成孩子节点的父节点索引
	 */
	public void generateIndexMap(){
		int i = 0;
		for(List<Integer> childList:chs){
			for(int index:childList){
				childParentMap.put(index, i);
			}
			i++;
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
	}

	/*
	 * 根据树的节点数降序排序
	 */
	@Override
	public int compareTo(Object o)
	{
		Tree t = (Tree)o;
		return n - t.n;
	}
}
