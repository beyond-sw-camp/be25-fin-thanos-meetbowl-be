package com.meetbowl.infrastructure.persistence.mail;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

@EntityScan(basePackageClasses = MailEntity.class)
@Configuration
public class MailJpaConfig {}
