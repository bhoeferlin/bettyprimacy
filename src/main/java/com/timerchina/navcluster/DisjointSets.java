package com.timerchina.navcluster;

public class DisjointSets {
	private int num;
	private int[] g;

	public DisjointSets(int num) {
		this.num = num;
		initialize();
	}

	void initialize() {
		g = new int[num]; 
		for (int x = 0; x < num; x++)
			g[x] = x;
	}

	public void union(int x, int y) {
		if (g[x] == g[y])
			return;
		x = g[x];
		y = g[y];
		for (int i = 0; i < num; i++)
			if (g[i] == x)
				g[i] = y;
	}

	public int find(int x) {
		return g[x];
	}
	public static void main(String[] args) {
		DisjointSets ds = new DisjointSets(6);
		System.out.println(ds.find(3));
	}
}
