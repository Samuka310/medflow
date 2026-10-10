package com.medflow.dto.generic;

public interface BaseMapper<E, D> {
    D toDto(E entity);
    E toEntity(D dto);
}
