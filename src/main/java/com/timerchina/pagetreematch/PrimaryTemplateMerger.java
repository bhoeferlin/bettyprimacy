package com.timerchina.pagetreematch;

import java.util.List;
import java.util.Queue;

import com.timerchina.utils.LimitQueue;

public class PrimaryTemplateMerger {
	/**
	 * 每个小段的数据处理:合并所有连续模板
	 * */
	public static void fragmentProcess(int start, int end,List<String> template){
		String upperData = template.get(start);
		for(int i = start; i < end; i++){
			String lowerData = template.get(i);
			if(upperData.equals(lowerData.replace("<", "</"))){
				String lowerStr = "";
				//向下找最近的模板结束标签
				String templateStr = "";
				int templateLen = 0;
				boolean flag = false;
				for(int j = i; j < end;j++){
					lowerStr += template.get(j);
					if(!template.get(j).equals(upperData)) continue;
					// 向上找最近的模板开始标签
					String upperStr = "";
					for (int k = i - 1; k >= start; k--) {
						upperStr = template.get(k) + upperStr;
						templateLen++;
						String preData = template.get(k);
						if (!preData.equals(lowerData)) continue;
						if (!lowerStr.equals(upperStr)) continue;
						templateStr = lowerStr;// 找到字符串模板
						flag = true;
						break;
					}
					if(flag) break;
				}
				//根据找到的模板替换所有的模板元素，并填充
				if(templateStr.trim().length()!=0&&!templateStr.equals("placeholder"))
					templateProcess(start, end, template, templateStr,templateLen);
			}
			else {
				upperData = lowerData;
				continue;
			}
		}
		HtmlProcessUtils.delListByElement(template, "placeholder");
	}
	//根据找到的模板替换所有的元素，并填充
	private static void templateProcess(int start, int end,List<String> template, String templateStr,int templateLen) {
		int count = 0;
		int lastTemplateIndex = start;
		Queue<String> limitQueue = new LimitQueue<String>(templateLen);//当达到限定数量的元素时，最先进队的自动出队列
		String tempStr = "";
		int delta = 1;
		for(int interI = start; interI < end; interI = interI + delta){
			delta = 1;
			if(!template.get(interI).contains("placeholder")){//!template.get(interI).contains("?")||
				for(int i = 0;i<templateLen;i++)
					try {
						limitQueue.offer(template.get(interI + i));
					} catch (Exception e) {
						return;		//发生越界
					}
				tempStr = LimitQueue.getElemnetStr(limitQueue);
				if(tempStr.equals(templateStr)){
					count ++;
					lastTemplateIndex = interI + templateLen - 1;
					delta = templateLen;
					if((lastTemplateIndex + templateLen > end)&&(count >= 2)){
						templateProcess(template, templateLen, count,
								lastTemplateIndex);//, limitQueue
					}
				}
				else {
					if(count >= 2){
						//处理模板数据
						templateProcess(template, templateLen, count,
								lastTemplateIndex);//, limitQueue
					}
					else if(count == 1){
						interI = interI - delta ;//回退到下一个
					}
					count = 0;
				}
			}
		}
	}

	private static void templateProcess(List<String> template, int templateLen,int count, int lastTemplateIndex) {
		int startTemplateIndex;
		startTemplateIndex = lastTemplateIndex - count * templateLen + 1;
		template.set(startTemplateIndex, "(" + template.get(startTemplateIndex));
		template.set(startTemplateIndex + templateLen -1, template.get(lastTemplateIndex) + ")+");
		HtmlProcessUtils.delListByIndex(template, startTemplateIndex + templateLen, lastTemplateIndex);
	}
}
