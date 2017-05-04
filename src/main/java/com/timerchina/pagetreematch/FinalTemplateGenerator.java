package com.timerchina.pagetreematch;

import java.util.ArrayList;
import java.util.List;


public class FinalTemplateGenerator {
	/**
	 * 外部迭代不匹配处理 
	 **/
	public List<String> outerIterUnmatchProcess(ArrayList<List<String>> fragmentLists){
	    //将每个fragment片段对比
		List<String> reList = new ArrayList<String>();
		if(fragmentLists.size() == 1){
			reList = fragmentLists.get(0);
			HtmlProcessUtils.delListRepeatElementMore(reList);
		}
		else reList = outFragment(fragmentLists);
	    if(reList == null) return null;
	    reList.set(0, reList.get(0));
	    reList.set(reList.size()-1, reList.get(reList.size()-1));
	  //如果()?前后有成对标签，则删除该标签对
		HtmlProcessUtils.delPairTag(reList);
		HtmlProcessUtils.delListRepeatElement(reList, "#PCDATA?");
	    return reList;
	}
	/**
	 * 通过十字交叉法，对初始模板进一步进行比较及合并
	 * */
	private List<String> outFragment(ArrayList<List<String>> fragmentLists){
		int maxSize = fragmentLists.get(0).size();
		List<String> tw = fragmentLists.get(0);
		for(int i = 1; i < fragmentLists.size(); i++){
			List<String> template = HtmlProcessUtils.deepCopy(tw);
			List<String> ts = fragmentLists.get(i);
			int size = ts.size();
			if(size > maxSize) {
				maxSize = size;
			}
			if(size < maxSize * 0.7) continue;
			int len = Math.max(tw.size(), ts.size());
			int delta = 1;
			for(int j = 0; j < len; j = j + delta){
				delta = 1;
				String currentTw = "";
				try {
					currentTw = tw.get(j);
				}catch (IndexOutOfBoundsException e) {//数据不平衡导致的越界错误，多余部分去除
					break;
				}
				String currentTs = "";
				try {
					currentTs = ts.get(j);
				}catch (IndexOutOfBoundsException e) {//数据不平衡导致的越界错误，多余部分去除
					HtmlProcessUtils.delListByIndex(tw, j, tw.size()-1);
					break;
				}
				// S?或#PCDATA?的情况，向后推若干元素，相等则全部覆盖
				if(currentTw.contains("#PCDATA")&&currentTs.contains("#PCDATA")&&(currentTw + currentTs).contains("?"))//#PCDATA?与#PCDATA共存的情况
					tw.set(j, "#PCDATA?");
				if(currentTw.contains("?")&&!currentTw.equals(currentTs)){
					delta = Cross.optionMatch(tw, ts, j);
					if(delta == -1)//匹配失败，跳出此次匹配
						break;
					continue;
				}
				else if(currentTs.contains("?")&&!currentTw.equals(currentTs)){
					delta = Cross.optionMatch(ts, tw, j);
					if(delta == -1) 
						break;
					continue;
				}
				
				//内部有迭代的情况
				if(!currentTw.equals(currentTs)){
					if(currentTw.contains("(")||currentTs.contains("(")){
						String currentTwTemp = currentTw.replace("(", "").replace(")+", "");
						String currentTsTemp = currentTs.replace("(", "").replace(")+", "");
						if(currentTwTemp.equals(currentTsTemp)){
							String twStr = "";
							String tsStr = "";
							delta = 0;
							for(int k = j; k < len; k++){
								try {
									currentTw = tw.get(k);
									currentTs = ts.get(k);
								} catch (Exception e) {
									break;
								}
								delta ++;
								twStr += tw.get(k).replace("(", "").replace(")+", "");
								tsStr += ts.get(k).replace("(", "").replace(")+", "");
								if(currentTw.contains(")+")||currentTs.contains(")+")){
									if(twStr.equals(tsStr)){
										tw.set(j, "(" + tw.get(j).replace("(", ""));
										tw.set(k, tw.get(k).replace(")+", "") + ")+");
										break;
									}
									else {
										//模板前面包含多余项且多余项起始于模板起始相同
										delta = Cross.blockMatch(ts, tw, j);
										if(delta == -1) {
											delta = Cross.blockMatch(tw, ts, j);
											if(delta == -1){
												delta = 1;
												break;
											}
										}
										break;
									}
								}
							}
						}
						else {
							j = j - 1;
							delta = Cross.finalCross(tw, ts, j);
							j = j + 1;
							if(delta == -1) {
								tw = template;
								break;
							}
						}
					}
					else{
						j = j - 1;
						delta = Cross.finalCross(tw, ts, j);
						j = j + 1;
						if(delta == -1) {
							tw = template;
							break;
						}
					} 
				}
				len = Math.max(tw.size(), ts.size());
			}
			HtmlProcessUtils.delListByElement(tw, "placeholder");
			HtmlProcessUtils.delListRepeatElement(tw, "#PCDATA?");
			HtmlProcessUtils.delListRepeatElement(tw, "<S?>");
			HtmlProcessUtils.delListRepeatElementMore(tw);
		}
		return tw;
	}
}