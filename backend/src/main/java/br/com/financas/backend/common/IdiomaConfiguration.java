package br.com.financas.backend.common;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration(proxyBeanMethods = false)
public class IdiomaConfiguration {

    @Bean
    public LocaleResolver localeResolver() {
        Locale portugues = Locale.forLanguageTag("pt-BR");
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(portugues);
        resolver.setSupportedLocales(List.of(portugues));
        return resolver;
    }
}
