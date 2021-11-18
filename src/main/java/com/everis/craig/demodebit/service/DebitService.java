package com.everis.craig.demodebit.service;

import com.everis.craig.demodebit.document.Debit;
import com.everis.craig.demodebit.document.Obtaining;
import com.everis.craig.demodebit.repository.IDebitRepository;
import com.everis.craig.demodebit.repository.IRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DebitService extends BaseService<Debit, String> implements IDebitService{

    private final IDebitRepository iDebitRepository;

    public DebitService(IDebitRepository iDebitRepository) {
        this.iDebitRepository = iDebitRepository;
    }

    @Override
    protected IRepository<Debit, String> getRepository() {
        return iDebitRepository;
    }

    @Override
    public Mono<Debit> findByCardNumber(String cardNumber) {
        return iDebitRepository.findByCardNumber(cardNumber);
    }

    @Override
    public Mono<Debit> findByAcquires(List<Obtaining> obtainingList) {
        return iDebitRepository.findByAcquiresContains(obtainingList);
    }
}
