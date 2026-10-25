package br.com.organizacao.recursos.reservas;

import java.util.List;

public record ReservationResponse(
	String id,
	String status,
	ApprovalDecision approvalDecision,
	List<ReservationTransition> transitions) {
}
