package br.univesp.tcc.cdccnpj.events.quarantine;

/**
 * Constantes da quarentena de deletes (ADR-006, spec Expansão X4).
 */
public final class DeleteQuarantineConstants {

    /** Transições de competência necessárias para confirmar um delete pendente. */
    public static final int CONFIRMATION_COMPETENCIAS = 1;

    private DeleteQuarantineConstants() {
    }
}
