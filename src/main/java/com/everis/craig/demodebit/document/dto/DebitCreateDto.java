package com.everis.craig.demodebit.document.dto;

import com.everis.craig.demodebit.document.Client;
import com.everis.craig.demodebit.document.Obtaining;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DebitCreateDto {
    private List<Obtaining> acquires;
    private String accNumber;
    private String cardNumber;
    private List<Client> clientHolder;
    private String productName;
}
