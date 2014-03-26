package com.mlcs.search.mlcsseg.lucene;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Tokenizer;

/**
 * 增加基础的停用词过滤，切长句的能力。分词细节没做.
 *  @Description TODO
 *	@author shanbo.liang
 */
public abstract class CnTokenizer extends Tokenizer{
	public final static String SPACES = " 　\t\r\n";
	public final static String PUNCTUATION = "。，！？；,!?;";
	public final static String stop = "',.`-_=?\'|\"(){}[]<>*#&^$@!~:;+/《》—－，。、：；！·？“”）（【】［］●'";
	public static Set<String> stopwords = new HashSet<String>();
	
	protected final StringBuilder buffer = new StringBuilder();
	protected int tokenStart = 0, tokenEnd = 0;
	
	
	static
	{
		for(String c : stop.split("")){
			stopwords.add(c);
		}
	}
	
	protected CnTokenizer(Reader input) {
		super(input);
	}

	protected String checkSentences() throws IOException{
		buffer.setLength(0);
		int ci;
		char ch, pch;
		boolean atBegin = true;
		tokenStart = tokenEnd;
		ci = input.read();
		ch = (char) ci;

		while (true) {
			if (ci == -1) {
				break;
			} else if (PUNCTUATION.indexOf(ch) != -1) {
				// End of a sentence
				buffer.append(ch);
				tokenEnd++;
				break;
			} else if (atBegin && SPACES.indexOf(ch) != -1) {
				tokenStart++;
				tokenEnd++;
				ci = input.read();
				ch = (char) ci;
			} else {
				buffer.append(ch);
				atBegin = false;
				tokenEnd++;
				pch = ch;
				ci = input.read();
				ch = (char) ci;
				// Two spaces, such as CR, LF
				if (SPACES.indexOf(ch) != -1
						&& SPACES.indexOf(pch) != -1) {
					// buffer.append(ch);
					tokenEnd++;
					break;
				}
			}
		}
		if (buffer.length() == 0){
			//sentences finished~	
			return null; 
		}else {
			return buffer.toString();
		}

	}

	public void reset() throws IOException {
		super.reset();
		tokenStart = tokenEnd = 0;
	}
}
