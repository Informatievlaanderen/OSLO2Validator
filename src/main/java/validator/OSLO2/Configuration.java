package validator.OSLO2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Configuration {
    //private String apConfig = "https://data.vlaanderen.be/src/shacl-validator-config.json";
    private String apConfig = "https://raw.githubusercontent.com/Informatievlaanderen/Data.Vlaanderen.be/production/config/shacl-validator-config.json";
    private static final String AP_CONFIG = "AP_CONFIG";
    private final LoadingCache<String, Map<String, APModel>> cache;

    public Configuration() {
        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1)
                .build(new CacheLoader<String, Map<String, APModel>>() {
                    @Override
                    public Map<String, APModel> load(String s) throws Exception {
                        if (!"options".equals(s)) throw new IllegalArgumentException("Only options is supported as a key");
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(new URL(getApConfig()), new TypeReference<Map<String, APModel>>(){});
                    }
                });
    }

	public static Configuration loadFromEnvironment() {
        Configuration config = new Configuration();

        String envApConfig = System.getenv(AP_CONFIG);
        if (envApConfig != null) config.setApConfig(System.getenv(AP_CONFIG));

        return config;
    }

    @Override
    public String toString() {
	    return "{\"apConfig\": \"" + apConfig + "\"}";
    }

	public String getApConfig() {
		return apConfig;
	}
   
	public void setApConfig(String apConfig) {
		this.apConfig = apConfig;
        if (null == this.apConfig) {
            throw new IllegalArgumentException("apConfig must be a valid local path or remote server");
        }
        // TODO: do more input validation
        // TODO: eagerly fill the cache with the config
	}

	public Map<String, APModel> getApplicationProfiles() throws ExecutionException {
        return cache.get("options");
    }
}
