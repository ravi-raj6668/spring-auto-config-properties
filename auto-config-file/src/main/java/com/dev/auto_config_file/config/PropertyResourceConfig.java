package com.dev.auto_config_file.config;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class PropertyResourceConfig extends PropertySourcesPlaceholderConfigurer implements EnvironmentAware {

	public static final Logger LOG = LoggerFactory.getLogger(PropertyResourceConfig.class);
	
	private static final String APP_CONFIG_NAME = "ext-application.properties";
	private static final String APP_CONFIG_PATH = Paths.get("").toAbsolutePath().toString();

	@Autowired
	private Environment env;

	@Override
	public void setEnvironment(Environment env) {
		super.setEnvironment(env);
		this.env = env;
		this.prepareProperties();
	}

	public PropertyResourceConfig() {
		checkConfigFileExists(); //validate config file existence
		configFileWatcher();
	}

	private void checkConfigFileExists() {
		File configFile = new File(APP_CONFIG_PATH, APP_CONFIG_NAME);
		if (!configFile.exists()) {
			throw new RuntimeException("Config file not found at " + configFile.getAbsolutePath());
		}
	}

	private void configFileWatcher() {
		try {
			final WatchService watchService = FileSystems.getDefault().newWatchService();
			LOG.info("Watch service initialted");

			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					Path path = Paths.get(APP_CONFIG_PATH);
					path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

					WatchKey watchKey;
					while ((watchKey = watchService.take()) != null) {
						watchKey.pollEvents().forEach(event -> {
							if (event.context().toString().equals(APP_CONFIG_NAME)) {
							LOG.info(String.format("File %s has changed", APP_CONFIG_NAME));
							this.prepareProperties();
							}
						});
						watchKey.reset();
					}
				} catch (Exception e) {
					LOG.error("Error in file watcher() {} ", e.getMessage());
				}
			});

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					watchService.close();
					LOG.info("Watch Service is closed");
				} catch (Exception e) {
					LOG.error("Error closing watch service() {}", e.getMessage());
				}
			}));

		} catch (Exception e) {
			LOG.error("Getting exception while processing config file watcher() {}", e.getMessage());
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void prepareProperties() {
		if (env != null) {
			final ConfigurableEnvironment configEnv = (ConfigurableEnvironment) env;
			final MutablePropertySources propertySource = configEnv.getPropertySources();

			Optional<PropertySource<?>> appConfigProperty = StreamSupport.stream(propertySource.spliterator(), false)
					.filter(ps -> ps.getName().matches("^.*applicationConfig.*")).findFirst();

			if (!appConfigProperty.isPresent()) {
				propertySource.addLast(new PropertiesPropertySource("applicationConfig", this.getProperties()));
			} else {
				propertySource.replace("applicationConfig",
						new PropertiesPropertySource("applicationConfig", this.getProperties()));
			}
		}
	}

	private Properties getProperties() {
		Properties properties = new Properties();
		try {
			FileSystemResource fileSystemResource = new FileSystemResource(new File(APP_CONFIG_PATH, APP_CONFIG_NAME));
			PropertiesLoaderUtils.fillProperties(properties, fileSystemResource);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load properties file from resource", e);
		}
		return properties;
	}

}
