package com.example.rmsservices.htmlannotator.service.main;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = "com.example.rmsservices.htmlannotator.service")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@EnableConfigurationProperties
@Configuration("ccom.example.rmsservices.htmlannotator.service.main.ApplicationConfig")
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class ApplicationConfig {

    @ConditionalOnMissingBean
    @Bean
    public RestTemplate restTemplate() {

        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        return modelMapper;
    }

}
