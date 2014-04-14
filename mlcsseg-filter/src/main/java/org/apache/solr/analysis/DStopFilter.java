package org.apache.solr.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;


public class DStopFilter extends FilteringTokenFilter {

	private final CharArraySet stopWords;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	
	
	public DStopFilter(TokenStream input,	CharArraySet stopWords, boolean enablePositionIncrements) {
		super(enablePositionIncrements, input);
		this.stopWords = stopWords;
	}

	@Override
	protected boolean accept() throws IOException {

		// System.out.println("<IKStopFilter>accept()"+termAtt.toString());
		return !stopWords.contains(termAtt.buffer(), 0, termAtt.length()); // 未被赋值过？隐藏操作在哪里实现？
	}

}
