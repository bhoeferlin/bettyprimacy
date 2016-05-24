package com.timerchina.datarecordparser;

import java.util.*;

/*
 * 部分树对齐（核心类）
 */
public class PartialTreeAlignment
{	
	//将x,y编码成唯一数
	static public int makePair(int x, int y)
	{	return (x << 20) + y;	}
	//解码x
	static public int fecthHead(int x)
	{	return x >> 20;	}
	//解码y
	static public int fetchTaril(int x)
	{	return x & 0xfffff;	}
	
	/*
	 * 简单树匹配算法
	 * node1:tree1的起始节点下标
	 * node2:tree2的起始节点下标
	 */
	static public int[] STM(Tree tree1, int node1, Tree tree2, int node2)
	{
		if (!tree1.dat.get(node1).equals(tree2.dat.get(node2)))	return new int[0];
		int k = tree1.chs.get(node1).size();//node1的孩子节点数量
		int l = tree2.chs.get(node2).size();//node2的孩子节点数量
		//若有一颗树没有子树，则直接返回这两个节点
		if (k == 0 || l == 0)
		{
			int[] result = new int[1];
			result[0] = makePair(node1, node2);
			return result;
		}
		//动态规划求解STM
		int[][] m = new int[k+1][l+1];
		int[][] bp = new int[k+1][l+1];
		int[][][] li = new int[k+1][l+1][];
		// 当node1没有子树时，只有0个匹配
		for (int i = 0; i <= k; i ++){
			m[i][0] = 0;
		}
		// 当node2没有子树时，只有0个匹配
		for (int j = 0; j <= l; j ++){
			m[0][j] = 0;
		}
		for (int i = 1; i <= k; i ++)
			for (int j = 1; j <= l; j ++)
			{
				li[i][j] = STM(tree1, tree1.getChild(node1, i-1), tree2, tree2.getChild(node2, j-1));
				int w = li[i][j].length;
				m[i][j] = Math.max(Math.max(m[i][j-1], m[i-1][j]), m[i-1][j-1] + w);
				if (m[i][j] == m[i][j-1])  bp[i][j] = 1;
				else if (m[i][j] == m[i-1][j])  bp[i][j] = 2;
				else bp[i][j] = 3;
			}
		int[] result = new int[1+m[k][l]];
		int x = k, y = l, v = 0;
		while (x != 0 && y != 0)
		{
			if (bp[x][y] == 1)  y --;
			else if (bp[x][y] == 2)  x --;
			else 
			{
				for (int tempResult : li[x][y])
					result[v++] = tempResult;
				x --;  y --;
			}
		}
		result[v++] = makePair(node1, node2);
		return result;
	}
	
	/*
	 * 根据STM的结果合并seedTree与tempTree,若tempTree能全部被合并到seedTree则为True,否则为false
	 */
	private boolean insertIntoSeed(Tree seedTree, Tree tempTree, int[] stmResult)
	{
		int[] yy = new int[tempTree.n];
		for (int i = 0; i < tempTree.n; i ++)		yy[i] = -1;
		for (int pair : stmResult)	yy[fetchTaril(pair)] = fecthHead(pair);
		if (yy[0] == -1)  return false;
		int[] qq = new int[tempTree.n];
		int qh = 0, ql = 0, rn = stmResult.length;
		qq[ql++] = 0;
		while (qh < ql)
		{
			int z = qq[qh++];
			if (yy[z] == -1)	continue;
			int kk = tempTree.chs.get(z).size(), ii = -1, jj = -1;
			for (int i = 0; i < kk && ii == -1; i ++)
			{
				if (yy[tempTree.getChild(z, i)] != -1)
				{
					ii = i;
				}
			}
			//若z的子树中没有匹配seedTree的子树，则将z的子树中的节点添加到相应SeedTree相应子树的右侧
			if (ii == -1)
			{
				for (int i = 0; i < kk; i ++)
				{
					int vi = tempTree.getChild(z, i);
					seedTree.InsertNode(tempTree.dat.get(vi), yy[z], seedTree.chs.get(yy[z]).size());
					yy[vi] = seedTree.n;   rn ++;
					qq[ql++] = vi;
				}
			} else 
			{
				int iid = seedTree.chs.get(yy[z]).indexOf(yy[tempTree.getChild(z, ii)]), jjd = iid;
				//如果Nodeii是seedTree中匹配树的最左孩子,则将Nodeii中所有的左兄弟插入seedTree相应子树的左侧
				if (iid == 0)
				{
					for (int i = 0; i < ii; i ++)
					{
						int vi = tempTree.getChild(z, i);
						seedTree.InsertNode(tempTree.dat.get(vi), yy[z], i);
						yy[vi] = seedTree.n;   rn ++;
						qq[ql++] = vi;
					}
				}
				for (; ii < kk; ii = jj, iid = jjd)
				{
					qq[ql++] = tempTree.getChild(z, ii);
					for (jj = ii + 1; jj < kk && yy[tempTree.getChild(z, jj)] == -1; jj ++);
					if (jj == kk) jjd = seedTree.chs.get(yy[z]).size();
					else for (jjd = iid + 1; seedTree.getChild(yy[z], jjd) != yy[tempTree.getChild(z, jj)]; jjd ++);
					if ((jj == kk && iid+1 == seedTree.chs.get(yy[z]).size()) || iid + 1 == jjd)
					{
						for (int i = ii + 1; i < jj; i ++)
						{
							int vi = tempTree.getChild(z, i);
							seedTree.InsertNode(tempTree.dat.get(vi), yy[z], iid + i - ii);
							yy[vi] = seedTree.n;   rn ++;
							qq[ql++] = vi;
						}
						jjd += jj - ii - 1;
					}
				}
			}
		} 
		
		return rn == tempTree.n;
	}
	
	/*
	 * 部分树对齐
	 */
	@SuppressWarnings("unchecked")
	public Tree alignment(List<Tree> treeList)
	{
		Collections.sort(treeList);
		Tree seedTree = treeList.get(treeList.size()-1);
		treeList.remove(treeList.size()-1);
		List<Tree> R = new ArrayList<Tree>();
		while (treeList.size() > 0)
		{
			Tree tempTree = treeList.get(treeList.size()-1);
			treeList.remove(treeList.size()-1);
			int[] stmResult = STM(seedTree, 0, tempTree, 0);
			if (stmResult.length != tempTree.n)
			{
				if (insertIntoSeed(seedTree, tempTree, stmResult))
				{	
					for (int i = R.size()-1; i >= 0; i --)
					{
						treeList.add(R.get(i));
						R.remove(i);
					}
				}
				else R.add(tempTree);
			}
		}
		return seedTree;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		/*
		 * testSTM
		 */
		Tree tree1 = new Tree();
		Tree tree2 = new Tree();
		tree1.LoadTree("tree1.txt");
		tree2.LoadTree("tree2.txt");
		PartialTreeAlignment pta = new PartialTreeAlignment();
//		int[] stmResult = pta.STM(tree1, 0, tree2, 0);
//		for (int value : stmResult)
//		{
//			System.out.print(fecthHead(value));
//			System.out.print("-");
//			System.out.println(fetchTaril(value));
//		}
		
		/*
		 * test两棵树的PTA
		 */
		List<Tree> tl = new ArrayList<Tree>();
		tl.add(tree1);
		tl.add(tree2);
		Tree seedTree = pta.alignment(tl);
//		seedTree.Output();
		
		/*
		 * test三棵树的PTA
		 */
		Tree tt1 = new Tree();
		Tree tt2 = new Tree();
		Tree tt3 = new Tree();
		tt1.LoadTree("tt1.txt");
		tt2.LoadTree("tt2.txt");
		tt3.LoadTree("tt3.txt");
		tl = new ArrayList<Tree>();
		tl.add(tt1);
		tl.add(tt2);
		tl.add(tt3);
		seedTree = pta.alignment(tl);
		seedTree.Output();
	}

}
