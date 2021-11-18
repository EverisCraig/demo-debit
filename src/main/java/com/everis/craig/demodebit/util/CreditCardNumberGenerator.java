package com.everis.craig.demodebit.util;

import java.util.Random;

public class CreditCardNumberGenerator {
    private final Random random=new Random(System.currentTimeMillis());
    public String generate(String bin, int length){
        int randomNumberLength=length-(bin.length()+1);
        StringBuilder builder=new StringBuilder(bin);
        for (int i=0;i<randomNumberLength; i++){
            int digit=this.random.nextInt(10);
            if (i%4==0)builder.append("-");
            builder.append(digit);
        }
        return builder.toString();
    }
}
