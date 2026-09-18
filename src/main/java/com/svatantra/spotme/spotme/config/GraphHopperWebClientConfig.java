package com.svatantra.spotme.spotme.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class GraphHopperWebClientConfig {

    /**
     * Local-only escape hatch. When true the GraphHopper WebClient trusts every
     * certificate and skips hostname verification. Never enable outside local dev.
     */
    @Value("${graphhopper.ssl-insecure:false}")
    private boolean sslInsecure;

    /**
     * GraphHopper returns the full route geometry when points_encoded=false,
     * which routinely exceeds WebClient's 256 KB default in-memory buffer.
     */
    @Value("${graphhopper.max-in-memory-bytes:16777216}")
    private int maxInMemoryBytes;

    @Bean("graphHopperWebClient")
    public WebClient graphHopperWebClient() throws Exception {

        if (!sslInsecure) {
            return WebClient.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(maxInMemoryBytes)
                    )
                    .build();
        }

        SslContext insecureSslContext =
                SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();

        HttpClient httpClient =
                HttpClient.create()
                        .secure(sslSpec -> sslSpec
                                .sslContext(insecureSslContext)
                                .handlerConfigurator(sslHandler -> {
                                    SSLEngine engine = sslHandler.engine();
                                    SSLParameters parameters = engine.getSSLParameters();
                                    parameters.setEndpointIdentificationAlgorithm(null);
                                    engine.setSSLParameters(parameters);
                                })
                        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maxInMemoryBytes)
                )
                .build();
    }
}
