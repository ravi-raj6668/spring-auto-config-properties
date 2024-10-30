package com.dev.auto_config_file.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

@RestController
@RequestScope
public class ConfigFileController {

	@Autowired
	private Environment env;

	@Value("${message}")
	private String msg;

	@GetMapping
	public String getMessage() {
		return String.format("From env: %s, from @value: %s", env.getProperty("message"), msg);
	}
}
