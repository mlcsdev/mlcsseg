package org.wltea.analyzer.dic;

import java.util.HashMap;
import java.util.Map;

public class DictCharNode {
	private static final Map<Character , Float> charMap = new HashMap<Character , Float>(1024,0.8f);
	
	void addChar(Character key,Float logFreq)
	{
		charMap.put(key, logFreq);
		//(int)(Math.log(Integer.parseInt(w[1]))*100)，默认给0
	}
	
	float getCharFreq(Character singleChar)
	{
		float freq=-2.0f;	//非单字，则表示该路径切分存在某些问题
		if(charMap.containsKey(singleChar))		//如果存在
		{
			freq = charMap.get(singleChar);
		}
		return freq;
	}
}
