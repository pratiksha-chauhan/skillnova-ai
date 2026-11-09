package com.skillnova.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Supplies a {@code RestClient.Builder} pre-configured with the Gemini call
 * timeout, qualified so it's only wired into {@code GeminiRoadmapProvider}
 * (not picked up as the default builder elsewhere). Built fresh via
 * {@code RestClient.builder()} here - rather than injecting Boot's
 * auto-configured {@code RestClient.Builder} bean - to avoid a same-type
 * bean ambiguity/self-reference cycle between this bean and that one.
 * Timeout configuration lives here - rather than inside the provider
 * itself - so the provider's constructor never mutates the builder it
 * receives; that keeps it safe to pass a
 * {@code MockRestServiceServer}-bound builder straight into the provider in
 * tests without a competing {@code requestFactory()} call silently
 * overwriting the mock's request factory.
 */
@Configuration
public class GeminiClientConfig {

    @Bean
    @Qualifier("geminiRestClientBuilder")
    public RestClient.Builder geminiRestClientBuilder(@Value("${gemini.timeout-ms}") long timeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
