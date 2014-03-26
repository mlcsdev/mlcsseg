package org.wltea.analyzer.test;

import java.io.IOException;
import java.io.StringReader;

import org.wltea.analyzer.core.IKSegmenter;

public class TestIk {
	public static void main(String[] args) throws IOException {
		IKSegmenter ik = new IKSegmenter(new StringReader(""), true);
		ik.next();
	}
}	
