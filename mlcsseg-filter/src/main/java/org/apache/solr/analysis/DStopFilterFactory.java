package org.apache.solr.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import com.mlcs.search.mlcsseg.common.ScheduledExecutor;


public class DStopFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

	public DStopFilterFactory(Map<String, String> args) {
		super(args);
		ignoreCase = getBoolean(args, "ignoreCase", false);
//		enablePositionIncrements = getBoolean(args, "enablePositionIncrements", false);
		conf = get(args, "conf"); //paths & lastupdate
		System.out.println("construct:::::stop::::::::::::::::::::::" + conf);
	}
	
	private CharArraySet stopWords;
	private boolean ignoreCase;
//	private boolean enablePositionIncrements;

	private ResourceLoader loader;

	private String conf;
	private long lastUpdateTime = -1;

	public void inform(final ResourceLoader loader) throws IOException {
		System.out.println("inform:::::stop::::::::::::::::::::::" + conf);
		this.loader = loader;
		this.update();
		if(conf != null && !conf.trim().isEmpty()){
			ScheduledExecutor.submit(new Runnable() {
				
				public void run() {
					try {
						update();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 1000 * 60 );
		}
	}

	@Override
	public TokenStream create(TokenStream arg0) {
		DStopFilter stopFilter = new DStopFilter( arg0, stopWords);
		return stopFilter;
	}

	public void update() throws IOException {
		Properties p = canUpdate();
		if (p != null){
			System.out.println("<IKStopFilterFactory> updating~~~!! ");
			stopWords = getWordSet(loader, p.getProperty("files"), ignoreCase);
			System.out.println("<IKStopFilterFactory> finish!! ");
		}

	}
		

	private Properties canUpdate() {

		try{
			Properties p = new Properties();
			InputStream confStream = loader.openResource(conf);
			p.load(confStream);
			confStream.close();
			String lastupdate = p.getProperty("lastupdate", "0");
			Long t = new Long(lastupdate);

			if (t > this.lastUpdateTime){
				this.lastUpdateTime = t.longValue();
				String paths = p.getProperty("files");
				if (paths==null || paths.trim().isEmpty()) // 必须有地址
					return null;
				System.out.println("loading conf");
				return p;
			}else{
				this.lastUpdateTime = t.longValue();
				return null;
			}
		}catch(Exception e){
			System.err.println("stop parsing conf NullPointerException~~~~~" + e.getMessage());
			return null;
		} 
	}

}
