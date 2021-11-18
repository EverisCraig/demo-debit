package com.everis.craig.demodebit.repository;

import com.everis.craig.demodebit.document.Debit;
import com.everis.craig.demodebit.document.Obtaining;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDebitRepository extends IRepository<Debit, String>{
    Mono<Debit> findByCardNumber(String cardNumber);
    Mono<Debit> findByAcquiresContains (List<Obtaining> acquiresList);
}
