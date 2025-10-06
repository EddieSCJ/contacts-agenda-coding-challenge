package com.contacts.agenda.config.http;

import com.contacts.agenda.client.KenectLabsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public KenectLabsClient kenectLabsClient(
            @Value("${kenect.api.host}") String host,
            @Value("${kenect.api.token}") String token
    ) {
        RestClient restClient = RestClient.builder()
                .baseUrl(host)
                .defaultHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(KenectLabsClient.class);
    }
}
