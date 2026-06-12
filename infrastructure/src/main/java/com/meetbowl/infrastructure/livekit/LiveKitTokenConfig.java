package com.meetbowl.infrastructure.livekit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LiveKitTokenProperties.class)
public class LiveKitTokenConfig {}
