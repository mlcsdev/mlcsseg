package com.mlcs.search.mlcsseg.lucene;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;


public abstract class ReloadableTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware{
	
	protected String conf;
	
	protected ReloadableTokenizerFactory(Map<String, String> args) {
		super(args);
		assureMatchVersion();
		conf = get(args, "conf");
	}

	public abstract void update(List<InputStream> inputStreams);
	
	public String getBeanName(){
		return this.getClass().toString();
	}
}
