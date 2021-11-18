package com.everis.craig.demodebit.handler;

import com.everis.craig.demodebit.document.Client;
import com.everis.craig.demodebit.document.Debit;
import com.everis.craig.demodebit.document.Obtaining;
import com.everis.craig.demodebit.document.dto.payload.RequestAcquiresDto;
import com.everis.craig.demodebit.service.IDebitService;
import com.everis.craig.demodebit.service.ObtainingService;
import com.everis.craig.demodebit.util.CreditCardNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class DebitHandler {
    private final IDebitService iDebitService;
    private final ObtainingService obtainingService;


    @Autowired
    public DebitHandler(IDebitService iDebitService, ObtainingService obtainingService) {
        this.iDebitService = iDebitService;
        this.obtainingService = obtainingService;
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .body(iDebitService.findAll(), Debit.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("productId");
        return iDebitService.findById(id)
                .flatMap(debit -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit))
                .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found")));
    }

    public Mono<ServerResponse> findByCardNumber(ServerRequest request) {
        String cardNumber = request.pathVariable("cardNumber");
        return iDebitService.findByCardNumber(cardNumber)
                .flatMap(debit -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit))
                .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found")));
    }

    public Mono<ServerResponse> findByAccNumber(ServerRequest request) {
        String accNumber = request.pathVariable("accNumber");
        return iDebitService.findAll()
                .collectList()
                .flatMap(debits -> {
                    List<Obtaining> acquires = debits.stream()
                            .map(Debit::getAcquires)
                            .collect(Collectors.toList())
                            .stream()
                            .flatMap(obtainingList -> obtainingList.stream()
                                    .filter(obtaining -> Objects.equals(obtaining.getDetail(), accNumber)))
                            .collect(Collectors.toList());
                    return iDebitService.findByAcquires(acquires);
                })
                .flatMap(debit -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit))
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<ServerResponse> findByIdentityNumber(ServerRequest request) {
        String identityNumber = request.pathVariable("identityNumber");
        return iDebitService.findAll()
                .collectList()
                .flatMap(debits -> {
                    List<Obtaining> acquires = debits.stream()
                            .map(Debit::getAcquires)
                            .collect(Collectors.toList())
                            .stream()
                            .flatMap(obtainingList -> obtainingList.stream()
                                    .filter(obtaining -> Objects.equals(obtaining.getClientHolder()
                                            .stream()
                                            .findFirst()
                                            .orElse(new Client()).getClientIdentityNumber(), identityNumber)))
                            .collect(Collectors.toList());
                    return iDebitService.findByAcquires(acquires);
                })
                .flatMap(debit -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit))
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<Debit> debitRequest = request.bodyToMono(Debit.class);
        return debitRequest
                .zipWhen(debit -> {
                    log.info("Obtaining, {}", obtainingService.findByIban(debit.getPrincipal().getIban()));
                    return obtainingService.findByIban(debit.getPrincipal().getIban());
                })
                .flatMap(obtaining -> {
                    List<Obtaining> acquires = obtaining.getT1()
                            .getAcquires();
                    acquires.add(obtaining.getT2());
                    log.info("List {}", acquires);
                    obtaining.getT1().setAcquires(acquires);
                    obtaining.getT1().setCardNumber(new CreditCardNumberGenerator().generate("4551", 17));
                    obtaining.getT1().setPrincipal(obtaining.getT2());
                    return iDebitService.create(obtaining.getT1());
                })
                .switchIfEmpty(Mono.error(new RuntimeException("DEBIT CREATE FAILED")))
                .checkpoint("AFTER DEBIT CREATED")
                .flatMap(debit -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit))
                .log()
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<Obtaining> acquiredObtainingWithDebit(Mono<RequestAcquiresDto> debitRequest) {
        return debitRequest
                .zipWhen(requestAcquiresDto -> {
                    Mono<Debit> debit = iDebitService.findByCardNumber(requestAcquiresDto.getCardNumber());
                    Mono<Obtaining> obtaining = obtainingService.findByIban(requestAcquiresDto.getIban());
                    Mono<Obtaining> obtainingUpdateCard = Mono.just(new Obtaining());
                    return Mono.zip(debit, obtaining, obtainingUpdateCard);
                })
                .zipWhen(objects -> {
                    long existObtaining = objects.getT2()
                            .getT1()
                            .getAcquires()
                            .stream()
                            .filter(obtaining -> Objects.equals(obtaining.getIban(), objects.getT1().getIban()))
                            .count();
                    if (existObtaining > 0) {
                        return Mono.error(new RuntimeException("THE ACCOUNT YOU WANT TO ASSOCIATE ALREADY EXIST"));
                    }
                    List<Obtaining> acquires = objects.getT2()
                            .getT1()
                            .getAcquires();
                    acquires.add(objects.getT2().getT2());
                    objects.getT2().getT1().setAcquires(acquires);
                    return iDebitService.update(objects.getT2().getT1());
                })
                .flatMap(objects -> {
                    List<Obtaining> acquires = objects.getT2().getAcquires();
                    Obtaining currentObtaining = acquires.stream()
                            .filter(obtaining -> Objects.equals(obtaining.getIban(), objects.getT1().getT2().getT2().getIban()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Obtaining with iban does not exist"));
                    currentObtaining.setCardNumber(objects.getT2().getCardNumber());
                    return obtainingService.updateObtaining(currentObtaining);
                });
    }

    public Mono<ServerResponse> obtainingListAcquires(ServerRequest request) {
        String cardNumber = request.pathVariable("cardNumber");
        String iban = request.pathVariable("iban");
        Mono<Debit> debit = iDebitService.findByCardNumber(cardNumber);
        Mono<Obtaining> obtaining = obtainingService.findByIban(iban);
        Mono<Obtaining> obtainingUpdateCard = Mono.just(new Obtaining());
        return Mono.zip(debit, obtaining, obtainingUpdateCard)
                .zipWhen(objects -> {
                    long existObtaining = objects.getT1()
                            .getAcquires()
                            .stream()
                            .filter(obtaining1 -> Objects.equals(obtaining1.getIban(), iban))
                            .count();
                    if (existObtaining > 0) {
                        return Mono.error(new RuntimeException("THE ACCOUNT YOU WANT TO ASSOCIATE ALREADY EXIST"));
                    }
                    List<Obtaining> acquires = objects.getT1()
                            .getAcquires();
                    acquires.add(objects.getT2());
                    objects.getT1().setAcquires(acquires);
                    return iDebitService.update(objects.getT1());
                })
                .flatMap(objects -> {
                    List<Obtaining> acquires = objects.getT2().getAcquires();
                    Obtaining currentObtaining = acquires
                            .stream()
                            .filter(obtaining1 -> Objects.equals(obtaining1.getIban(), objects.getT1().getT2().getIban()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("OBTAINING WITH IBAN DOES NOT EXIST"));
                    currentObtaining.setCardNumber(objects.getT2().getCardNumber());
                    return obtainingService.updateObtaining(currentObtaining);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("DEBIT OBTAINING FAILED")))
                .flatMap(o -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(o))
                .log()
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<ServerResponse> obtainingListDisassociate(ServerRequest request) {
        String cardNumber = request.pathVariable("cardNumber");
        String iban = request.pathVariable("iban");
        Mono<Debit> debit = iDebitService.findByCardNumber(cardNumber);
        Mono<Obtaining> obtaining = obtainingService.findByIban(iban);
        Mono<Obtaining> obtainingUpdateCard = Mono.just(new Obtaining());
        return Mono.zip(debit, obtainingUpdateCard, obtaining)
                .zipWhen(objects -> {
                    Boolean isPrincipal = objects.getT1().getPrincipal().getIban().equals(objects.getT2().getIban());
                    long existObtaining = objects.getT1().getAcquires().stream().filter(obtaining1 -> Objects.equals(obtaining1.getIban(), iban)).count();
                    if (existObtaining == 0) {
                        return Mono.error(new RuntimeException("THE ACCOUNT YOU WANT TO DISASSOCIATE DOES NOT EXIST"));
                    }
                    List<Obtaining> acquires = objects.getT1().getAcquires();
                    acquires.remove(objects.getT2());
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        Double maxBalance = acquires.stream()
                                .map(obtaining1 -> obtaining1.getDetail().getBalance())
                                .max(Comparator.comparing(aDouble -> aDouble))
                                .orElse(0.0);
                        Obtaining obtainingMaxBalance = acquires.stream()
                                .filter(obtaining1 -> Objects.equals(obtaining1.getDetail().getBalance(), maxBalance))
                                .findFirst()
                                .orElse(new Obtaining());
                        objects.getT1().setPrincipal(obtainingMaxBalance);

                    }
                    objects.getT1().setAcquires(acquires);
                    return iDebitService.update(objects.getT1());
                })
                .flatMap(objects -> {
                    Obtaining currentAccount = objects.getT1().getT2();
                    currentAccount.setCardNumber("");
                    return obtainingService.updateObtaining(currentAccount);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("DEBIT ASSOCIATE FAILED")))
                .flatMap(obtaining1 -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(obtaining1))
                .log()
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }

    public Mono<ServerResponse> defineAccountAsMain(ServerRequest request) {
        String cardNumber = request.pathVariable("cardNumber");
        String iban = request.pathVariable("iban");
        Mono<Debit> debit = iDebitService.findByCardNumber(cardNumber);
        Mono<Obtaining> obtaining = obtainingService.findByIban(iban);
        return Mono.zip(debit, obtaining)
                .flatMap(objects -> {
                    long existObtaining = objects.getT1().getAcquires().stream().filter(obtaining1 -> Objects.equals(obtaining1.getIban(), iban)).count();
                    Boolean isPrincipal = objects.getT1().getPrincipal().getIban().equals(objects.getT2().getIban());
                    if (existObtaining == 0) {
                        return Mono.error(new RuntimeException("THE ACCOUNT YOU WANT TO DISASSOCIATE DOES NO EXIST"));
                    }
                    if (Boolean.TRUE.equals(isPrincipal)) {
                        return Mono.error(new RuntimeException("THIS ACCOUNT IS ALREADY THE MAIN ONE"));
                    }
                    objects.getT1().setPrincipal(objects.getT2());
                    return iDebitService.update(objects.getT1());
                })
                .flatMap(debit1 -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(debit1))
                .log()
                .onErrorResume(throwable -> Mono.error(new RuntimeException(throwable.getMessage())));
    }


}
