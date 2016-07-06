package com.timerchina.navcluster;

import java.util.ArrayList;
import java.util.List;

import com.timerchina.navparser.DataBean;

public class ClustererByCosin {
	private List[] clusterList;
    DisjointSets ds;
    private static final int MIN = Integer.MIN_VALUE;
    private int n;
    private int cc;

    // private double ori[] = {1,2,5,7,9,10};

    public ClustererByCosin(int num) {
        ds = new DisjointSets(num);
        n = num;
        cc = n;
        clusterList = new ArrayList[num];
        for (int i = 0; i < n; i++)
            clusterList[i] = new ArrayList();
    }

    public List[] getClusterList() {
        return clusterList;
    }

    public void setClusterList(List[] clusterList) {
        this.clusterList = clusterList;
    }

    public void output(List<DataBean> fileListBeans) {
        int ind = 1;
        for (int i = 0; i < n; i++) {
            clusterList[ds.find(i)].add(fileListBeans.get(i).getContent());
        }
        for (int i = 0; i < n; i++) {
            if (clusterList[i].size() != 0) {
                System.out.print("cluster " + ind + " :");
                for (int j = 0; j < clusterList[i].size(); j++) {
                    System.out.print(clusterList[i].get(j) + "	##$");
                }
                System.out.println();
                ind++;
            }
        }
    }

    /** *//**
     * this method provides a hierachical way for clustering data.
     *
     * @param r
     *            denote the distance matrix
     * @param n
     *            denote the sample num(distance matrix's row number)
     * @param dis
     *            denote the threshold to stop clustering
     */
    public void cluster(float[][] r, int n, float cosinValue) {
        int mx = 0, my = 0;
        float vmax = MIN;
        for (int i = 0; i < n; i++) { // 寻找�?��距离�?��的行�?
            for (int j = 0; j < n; j++) {
                if (j > i) {
                    if (vmax < r[i][j]) {
                    	vmax = r[i][j];
                        mx = i;
                        my = j;
                    }
                }
            }
        }
        if (vmax < cosinValue) {
            return;
        }
        ds.union(ds.find(mx), ds.find(my)); // 将最小距离所在的行列实例聚类合并
        float o1[] = r[mx];
        float o2[] = r[my];
        float v[] = new float[n];
        float vv[] = new float[n];
        for (int i = 0; i < n; i++) {
        	float tm = Math.max(o1[i], o2[i]);
            if (tm != 1)
                v[i] = tm;
            else
                v[i] = MIN;
            vv[i] = MIN;
        }
        r[mx] = v;
        r[my] = vv;
        for (int i = 0; i < n; i++) { // 更新距离矩阵
            r[i][mx] = v[i];
            r[i][my] = vv[i];
        }
        cluster(r, n, cosinValue); // 继续聚类，递归直至所有簇之间距离小于dis�?
    }

    /** *//**
     *
     * @param r
     * @param cnum
     *            denote the number of final clusters
     */
    public void cluster(float[][] r, int cnum) {
        /**//*if(cc< cnum)
            System.err.println("聚类数大于实例数");*/
        while (cc > cnum) {// 继续聚类，循环直至聚类个数等于cnum
            int mx = 0, my = 0;
            float vmax = MIN;
            for (int i = 0; i < n; i++) { // 寻找最小距离�?��的行�?
                for (int j = 0; j < n; j++) {
                    if (j > i) {
                        if (vmax < r[i][j]) {
                        	vmax = r[i][j];
                            mx = i;
                            my = j;
                        }
                    }
                }
            }
            ds.union(ds.find(mx), ds.find(my)); // 将最小距离所在的行列实例聚类合并
            float o1[] = r[mx];
            float o2[] = r[my];
            float v[] = new float[n];
            float vv[] = new float[n];
            for (int i = 0; i < n; i++) {
            	float tm = Math.max(o1[i], o2[i]);
                if (tm != 1)
                    v[i] = tm;
                else
                    v[i] = MIN;
                vv[i] = MIN;
            }
            r[mx] = v;
            r[my] = vv;
            for (int i = 0; i < n; i++) { // 更新距离矩阵
                r[i][mx] = v[i];
                r[i][my] = vv[i];
            }
            cc--;
        }
    }

	public static void main(String[] args) {
		
	}
}
