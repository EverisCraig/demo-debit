package com.everis.craig.demodebit.service;

import com.everis.craig.demodebit.document.Obtaining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static com.everis.craig.demodebit.util.LogTraceResponse.logTraceResponse;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class ObtainingService {
    private static final String url = "http://localhost:8093/obtaining";
    private final Logger logger = LoggerFactory.getLogger(ObtainingService.class);
    private final WebClient.Builder webClientBuilder;


    public ObtainingService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Obtaining> findByDetailAccNumber(String accNumber) {
        return webClientBuilder
                .baseUrl(url)
                .build()
                .get().uri("", Collections.singletonMap("", accNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING FIND FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }

    public Mono<Obtaining> findByIban(String iban) {
        return webClientBuilder
                .baseUrl(url)
                .build()
                .get()
                .uri("", Collections.singletonMap("iban", iban))
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING FIND FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }

    public Mono<Obtaining> updateObtaining(Obtaining obtaining) {
        return webClientBuilder
                .baseUrl(url)
                .build()
                .post()
                .uri("")
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(obtaining), Obtaining.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE OBTAINING UPDATE FAILED"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    return Mono.error(new RuntimeException("THE OBTAINING UPDATE FAILED"));
                })
                .bodyToMono(Obtaining.class);
    }
}
