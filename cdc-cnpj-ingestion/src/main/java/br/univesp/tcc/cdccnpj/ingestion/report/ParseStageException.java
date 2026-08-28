package br.univesp.tcc.cdccnpj.ingestion.report;

public final class ParseStageException extends RuntimeException {

    public ParseStageException(String message) {
        super(message);
    }

    public ParseStageException(String message, Throwable cause) {
        super(message, cause);
    }
}
