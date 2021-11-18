package com.everis.craig.demodebit.service;

import com.everis.craig.demodebit.document.Debit;
import com.everis.craig.demodebit.document.Obtaining;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDebitService extends IBaseService<Debit, String> {

    Mono<Debit> findByCardNumber(String cardNumber);

    Mono<Debit> findByAcquires(List<Obtaining> obtainingList);
}
