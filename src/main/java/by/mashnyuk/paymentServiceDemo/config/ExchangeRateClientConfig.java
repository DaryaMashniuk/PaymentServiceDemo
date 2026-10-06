package by.mashnyuk.paymentServiceDemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ExchangeRateClientConfig {

    @Bean
    public HttpComponentsClientHttpRequestFactory exchangeRateRequestFactory(
            @Value("${exchange-rate.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${exchange-rate.client.read-timeout:3s}") Duration readTimeout
    ) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Bean
    public RestClient exchangeRateRestClient(
            @Value("${exchange-rate.client.base-url}") String baseUrl,
            @Value("${exchange-rate.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${exchange-rate.client.read-timeout:3s}") Duration readTimeout
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}