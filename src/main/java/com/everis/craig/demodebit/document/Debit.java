package com.everis.craig.demodebit.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "debit")
@Data
public class Debit {
    @Id
    private String id;
    private List<Obtaining> acquires=new ArrayList<>();
    private Obtaining principal;
    private String cardNumber;
    private LocalDateTime debitDate=LocalDateTime.now();
}
