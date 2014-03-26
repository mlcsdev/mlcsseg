package com.mlcs.search.mlcsseg.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.util.ResourceLoader;

import com.mlcs.search.mlcsseg.common.ScheduledExecutor;

/**
 * register it in 'inform(ResourceLoader loader)'
 *  @Description TODO
 *	@author shanbo.liang
 */
public class ReloaderRegister{




	private static Map<String, ConfigChecker> reloadAwares = new HashMap<String, ConfigChecker>();


	public static class ConfigChecker {

		private long lastUpdateTime = Long.MIN_VALUE;




		public static List<String> SplitFileNames(String fileNames) {
			if (fileNames == null || fileNames.isEmpty())
				return Collections.emptyList();

			List<String> result = new ArrayList<String>();
			for (String file : fileNames.split("[,\\s]+")) {
				result.add(file);
			}

			return result;
		}

		public List<String> currentToReload(InputStream confStream){
			try{
				Properties p = new Properties();
				p.load(confStream);
				confStream.close();
				String lastupdate = p.getProperty("lastupdate", "0");
				Long t = new Long(lastupdate);
//				System.out.println(" => " + toString() + "=========loading conf========= : " + p.toString() );
				if (t > this.lastUpdateTime){
					System.out.println("lastUpdateTime is new, files will be loaded!" );
					this.lastUpdateTime = t.longValue();
					String paths = p.getProperty("files");
					if (paths==null || paths.trim().isEmpty()) // 必须有地址
						return Collections.emptyList();

					List<String> dicPaths = SplitFileNames(p.getProperty("files"));
					return dicPaths;
				}else{
					this.lastUpdateTime = t.longValue();
					return Collections.emptyList();
				}
			}catch(IOException e){
				return Collections.emptyList();
			}
		}

		public String toString(){
			return "configchecker@" + lastUpdateTime;
		}

	}


	/**
	 * 向注册机注册一个可定时更新的tokenfactory；register it in 'inform(ResourceLoader loader)'
	 * @param reloadFactory
	 * @param loader
	 * @param confName
	 * @return
	 */
	public static synchronized String register(final ReloadableTokenizerFactory reloadFactory, final ResourceLoader loader, final String confName){
		if ( reloadAwares.containsKey(reloadFactory.getBeanName())){
			return "already";
		}else{
			if(confName != null && !confName.trim().isEmpty()){ //存在conf才注册进来
				final ConfigChecker cc = new ConfigChecker();
				reloadAwares.put(reloadFactory.getBeanName(), cc);
				loadAndUpdate(cc, reloadFactory, loader, confName);
				ScheduledExecutor.submit(new Runnable() {
					public void run() {
						loadAndUpdate(cc, reloadFactory, loader, confName);
					}
				}, 30 * 1000);
				return "ok";
			}
			return "conf is empty";
		}
	}

	private static void loadAndUpdate(final ConfigChecker cc, final ReloadableTokenizerFactory reloadFactory, final ResourceLoader loader, final String confName){

		try {
			List<String> dicts = cc.currentToReload(loader.openResource(confName));
			if (!dicts.isEmpty()){
				List<InputStream> insFromLoader = new ArrayList<InputStream>(dicts.size());
				for(String dictName : dicts){
					try{
						insFromLoader.add(loader.openResource(dictName));
					}catch(IOException e){
						System.out.println("missing dict source : " + dictName);
					}
				}
				reloadFactory.update(insFromLoader);
				System.out.println("reload finish! " + dicts);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
