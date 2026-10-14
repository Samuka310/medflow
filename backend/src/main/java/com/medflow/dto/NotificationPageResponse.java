package com.medflow.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificacaoResumoDTO> content,
    long totalElements,
    int number,
    int size
) {}
