package com.everis.craig.demodebit.document;

import lombok.Data;

@Data
public class Politic {
    private String clientType;
    private boolean commissionMaintenance;
    private boolean maxLimitMonthlyMovements;
    private Integer maxLimitMonthlyMovementsQuantity;
}
