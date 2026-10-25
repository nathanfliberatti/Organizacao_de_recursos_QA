package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record ApprovalDecision(User approvedBy, Instant decidedAt) {
}
