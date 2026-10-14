package com.medflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "medflow.consulta")
public record ConsultaProperties(
    @DefaultValue("60") int duracaoMinutos,
    @DefaultValue("150.00") BigDecimal valorPadrao
) {}
