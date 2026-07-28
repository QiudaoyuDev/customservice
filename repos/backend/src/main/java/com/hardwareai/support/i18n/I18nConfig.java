package com.hardwareai.support.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 国际化配置：基于 Spring MessageSource 的本地化消息能力。
 * 资源文件位于 classpath:i18n/messages_{lang}.properties（UTF-8 编码，支持中文）。
 * 当前提供 en（messages_en）/ zh（messages_zh）两种语言。
 */
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setCacheSeconds(3600);
        return source;
    }
}
