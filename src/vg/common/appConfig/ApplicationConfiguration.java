package vg.common.appConfig;



import java.io.IOException;
import java.util.Properties;

public class ApplicationConfiguration {
	
	static protected ApplicationConfiguration self;
	
	protected Properties fileConfig;
	
	public static ApplicationConfiguration getInstance() {
		if(self == null)
			self = new ApplicationConfiguration();
		return self;
	}
	
	protected ApplicationConfiguration() {
		fileConfig = new Properties();
		try {
			fileConfig.load(getClass().getClassLoader().getResourceAsStream(ApplicationField.CONFIG_FILE_NAME));
		}
		catch (IOException e) {MBSLogger.logError(e);}
	}
	public String get(String key){
		return fileConfig.getProperty(key);
	}
	
}
