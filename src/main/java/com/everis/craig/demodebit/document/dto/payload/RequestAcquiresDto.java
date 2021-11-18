package com.everis.craig.demodebit.document.dto.payload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestAcquiresDto {
    private String cardNumber;
    private String iban;
}
