package com.medflow.entity.enums;
public enum ConsultaStatus {
    AGENDADA, CONFIRMADA, AGUARDANDO_TRIAGEM, REALIZADA, CANCELADA;
    public boolean podeTransitarPara(ConsultaStatus novoStatus) {
        if (this == novoStatus) return true;
        switch (this) {
            case AGENDADA: return novoStatus == CONFIRMADA || novoStatus == CANCELADA;
            case CONFIRMADA: return novoStatus == AGUARDANDO_TRIAGEM || novoStatus == CANCELADA || novoStatus == REALIZADA;
            case AGUARDANDO_TRIAGEM: return novoStatus == REALIZADA || novoStatus == CANCELADA;
            case REALIZADA:
            case CANCELADA: return false;
            default: return false;
        }
    }
}
