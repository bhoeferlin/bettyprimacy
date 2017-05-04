package com.timerchina.pagetreematch;

import java.util.List;

import com.timerchina.utils.TagArrayStack;

/**
 * 十字交叉法
 * */
public class Cross {
	
	public static boolean isContainField = false;
	public static int L = 20; //十字交叉阈值
	
	/**
	 * 初级模板：十字交叉法
	 * */
	public static int primaryCross(List<String> tw, List<String> ts, int index) {
		index = index + 1;
		String dataTw = tw.get(index).replace("(", "").replace(")+", "");
		String dataTs = ts.get(index).replace("(", "").replace(")+", "");
		
		int delta = 0;
		if(dataTw.contains("#PCDATA")&&ts.get(index+1).equals(dataTw)){
			delta = primaryBlockMatch(tw, ts, index, true);
			if(delta > 0)
				tw.remove(index);//多了一个元素，需删除
		}
		else if(dataTs.contains("#PCDATA")&&tw.get(index+1).equals(dataTs)){
			delta = primaryBlockMatch(ts, tw, index, true);
			if(delta > 0)
				ts.remove(index);//多了一个元素，需删除
		}
		else{
			delta = primaryBlockMatch(ts, tw, index, false);
			
			if(delta == -1){
				delta = primaryBlockMatch(tw, ts, index, false);
			}
		}
		
		if(delta == -1) return -1;
		return delta;
	}
	/**
	 * 最终模板：十字交叉法
	 * */
	public static int finalCross(List<String> tw, List<String> ts, int index) {
		index = index + 1;
		int delta = 0;
		delta = finalBlockMatch(ts, tw, index);
		
		if(delta == -1){
			delta = finalBlockMatch(tw, ts, index);
		}
		if(delta == -1) return -1;
		if(isContainField) tw.set(index, "#PCDATA?");
		else
			tw.set(index, "<S?>");
		return delta;
	}
	 /**
	  * 初始模板
	  * 栈存储block块，按块匹配
	  * */
	private static int primaryBlockMatch(List<String> t1, List<String> t2, int index, boolean flag) {
		index = index - 1;
		int delta = 1;
		isContainField = false;
		int insc = 0;
		if(!flag) insc = 1;
		else insc = 2;
		String t1Data = t1.get(index + insc).replace("(", "").replace(")+", "");
		//后三个元素中是否有？的情况，如果有，直接比较下一个元素，否则，比较后三个元素
		String t1Str = "";
		String t2Str = "";
		//判断后三项是否包含未知项，不包含时，可用后三项作为相等的判断,否则，以后一项作为判断
		boolean postContainPCDATAflag = false;
		for (int i = index + insc + 1; i < index + insc + 4; i++) {
			if (i < t1.size()) {
				if (t1.get(i).contains("#PCDATA"))
					postContainPCDATAflag = true;
			}
		}
		if(!postContainPCDATAflag) t1Str = HtmlProcessUtils.getNElement(t1, index + insc + 1, 3);//获取相异数据的后三个元素
		else t1Str = HtmlProcessUtils.getNElement(t1, index + insc + 1, 1);//获取相异数据的后1个元素
		
		for (int si = 1; si <= L; si++) {
			try {
				String t2Data = t2.get(index + si).replace("(", "").replace(")+", "");
				isContainField = isContainField||HtmlProcessUtils.isStr(t1Data)||HtmlProcessUtils.isStr(t2Data);
				if(!postContainPCDATAflag) t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 3);
				else t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 1);
				if (t1Data.equals(t2Data) && t1Str.equals(t2Str)) {
					delta = si - 1;
					break;
				}
				if ((index + si) == t2.size() - 1 || si == L) {
					isContainField = false;
					return -1;
				}
			}catch (Exception e) {
				// 边界处理
			}
		}
		for(int i = index + 1; i <= index + delta; i++){
			if(isContainField){
				t1.add(i, "#PCDATA?");
				t2.set(i, "#PCDATA?");
			}
			else {
				t1.add(i, "<S?>");
				t2.set(i, "<S?>");
			}
		}
		return delta;
	}
	/**
	 * 最终模板
	 * 栈存储block块，按块匹配
	 * */
	private static int finalBlockMatch(List<String> t1, List<String> t2, int index){
		index = index - 1;
		int delta = 1;
		isContainField = false;
		String t1Data = t1.get(index + 1).replace("(", "").replace(")+", "");
		//后三个元素中是否有？的情况，如果有，直接比较下一个元素，否则，比较后三个元素
		String t1Str = "";
		String t2Str = "";
		//判断后三项是否包含可选，不包含时，可用后三项作为相等的判断
		boolean flag = false;
		for (int i = index + 2; i < index + 5; i++) {
			if (i < t1.size()) {
				if (t1.get(i).contains("#PCDATA"))
					flag = true;
			}
		}
		if (!flag)
			t1Str = HtmlProcessUtils.getNElement(t1, index + 2, 3);// 获取相异数据的后三个元素
		else
			t1Str = HtmlProcessUtils.getNElement(t1, index + 2, 1);// 获取相异数据的后1个元素

		for (int si = 0; si <= L; si++) {
			try {
				String t2Data = t2.get(index + si);
				isContainField = isContainField|| HtmlProcessUtils.isStr(t1Data)|| HtmlProcessUtils.isStr(t2Data);
				if (!flag)
					t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 3);
				else
					t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 1);

				if (t1Data.equals(t2Data) && t1Str.equals(t2Str)) {
					delta = si - 1;
					break;
				}
				if ((index + si) == t2.size() - 1 || si == L) {
					isContainField = false;
					return -1;
				}
			} catch (Exception e) {
				// 边界处理
			}
		}
		for(int i = index + 1; i <= index + delta; i++){
			if(isContainField){
				t1.add(i, "#PCDATA?");
				t2.set(i, "#PCDATA?");
			}
			else {
				t1.add(i, "<S?>");
				t2.set(i, "<S?>");
			}
		}
		return delta;
	}
	/**
	 * S?或#PCDATA?的情况，向后推若干元素，相等则全部覆盖
	 * */
	public static int optionMatch(List<String> t1, List<String> t2, int index) {
		int delta = 1;
		String currentT1 = t1.get(index);
		String currentT2 = t2.get(index);
		if(currentT1.contains("#PCDATA")||currentT2.contains("#PCDATA"))
			isContainField = true;
		String t1Data = t1.get(index + 1).replace("(", "");
		//后三个元素中是否有？的情况，如果有，直接比较下一个元素，否则，比较后三个元素
		String t1Str = "";
		String t2Str = "";
		//判断后三项是否包含可选，不包含时，可用后三项作为相等的判断
		boolean flag = false;
		for (int i = index + 2; i < index + 5; i++) {
			if (i < t1.size()) {
				if (t1.get(i).contains("?"))
					flag = true;
			}
		}
		if(!flag) t1Str = HtmlProcessUtils.getNElement(t1, index + 2, 3);//获取相异数据的后三个元素
		else t1Str = HtmlProcessUtils.getNElement(t1, index + 2, 1);//获取相异数据的后1个元素
		
		for (int si = 0; si <= L; si++) {
			try {
				String t2Data = t2.get(index + si);
				isContainField = isContainField||HtmlProcessUtils.isStr(t1Data)||HtmlProcessUtils.isStr(t2Data);
				if(!flag) t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 3);
				else t2Str = HtmlProcessUtils.getNElement(t2, index + si + 1, 1);
				
				if (t1Data.equals(t2Data) && t1Str.equals(t2Str)) {
					delta = si;
					break;
				}
				if (si == L) {
					isContainField = false;
					return -1;
				}
			}catch (Exception e) {
				// 边界处理
			}
		}
		if(delta == 0){
			if(isContainField) t2.add(index,"#PCDATA?");
			else t2.add(index,"<S?>");
			return 1;
		} 
		for(int i = index; i < index + delta; i++){
			if(isContainField){
				t1.add(i, "#PCDATA?");
				t2.set(i, "#PCDATA?");
			}
			else {
				t1.add(i, "<S?>");
				t2.set(i, "<S?>");
			}
		}
		t1.remove(index);//t1本身包含一个可选
		return delta;
	}
	 /**
	  * 栈存储block块，按块匹配
	  * */
	public static int blockMatch(List<String> t1, List<String> t2, int index) {
		isContainField = false;
		int delta = 1;
		TagArrayStack<String> tagStack = new TagArrayStack<String>();
		String firstDataTemp = "";
		String firstData = "";
		int count = 0;
		for (int i = index; i < t1.size(); i++) {
			firstData = t1.get(i).replace("(", "").replace(")+", "");
			firstDataTemp = firstData;
			if (HtmlProcessUtils.isContainWord(firstData, PrimaryTemplateGenerator.wordSet)
					|| HtmlProcessUtils.isStr(firstData)) // 第一个元素为正文或单标签，不入栈
			{
				count ++;
				continue;
			}
			else {
				firstData = firstData.replace("<", "").replace(">", "");
				break;
			}
		}
		tagStack.push(firstData);
		while (!tagStack.isEmpty()) {
			for (int si = count + 1; si <= L; si++) {
				try {
					String data = t1.get(index + si).replace("(", "").replace(")+", "");
					isContainField = HtmlProcessUtils.isStr(firstDataTemp)||HtmlProcessUtils.isStr(data)||isContainField;
					// 正文不入栈
					if (HtmlProcessUtils.isStr(data) || HtmlProcessUtils.isContainWord(data, PrimaryTemplateGenerator.wordSet)) {
						if (si >= L){
							isContainField = false;
							return -1;
						}
						else
							continue;
					}
					String tag = HtmlProcessUtils.getTag(data);

					if (!data.contains("</")) {
						tagStack.push(tag);
					} else if(tagStack.getElement().equals(tag)) {
						tagStack.pop();
						if (tagStack.isEmpty()) {
							delta = si + 1;
							break;
						}
					}
				} catch (Exception e) {
					isContainField = false;
					return -1;
				}
				if (si == L) {
					isContainField = false;
					return -1;
				}
			}
		}
		//向后推三个看是否相等
		boolean flag = false;
		for(int k = index + delta; k < index + delta + 3; k++){
			if (k < t1.size()) {
				if (t1.get(k).contains("#PCDATA"))
					flag = true;
			}
		}
		String t1Str = "";
		String t2Str = "";
		if(!flag){
			t1Str = HtmlProcessUtils.getNElement(t1, index + delta, 3);
			t2Str = HtmlProcessUtils.getNElement(t2, index , 3);
		} 
		else{
			t1Str = HtmlProcessUtils.getNElement(t1, index + delta, 1);
			t2Str = HtmlProcessUtils.getNElement(t2, index , 1);
		} 
		if(t1Str.equals(t2Str)) {
			//数据填充
			for(int i = index; i < index + delta; i++){
				if(isContainField){
					t1.set(i, "#PCDATA?");
					t2.add(i, "#PCDATA?");
				}
				else {
					t1.set(i, "<S?>");
					t2.add(i, "<S?>");
				}
			}
		}
		else return -1;
		return delta;
	}
}
