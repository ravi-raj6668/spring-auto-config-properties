package com.dev.auto_config_file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
public class ConfigResource {
	
	@Bean
	public PropertySourcesPlaceholderConfigurer properties() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertyResourceConfig();
		ppc.setIgnoreUnresolvablePlaceholders(true);
		return ppc;
	}
}
