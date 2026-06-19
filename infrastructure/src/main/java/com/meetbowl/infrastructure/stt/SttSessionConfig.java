package com.meetbowl.infrastructure.stt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SttSessionProperties.class)
public class SttSessionConfig {}
