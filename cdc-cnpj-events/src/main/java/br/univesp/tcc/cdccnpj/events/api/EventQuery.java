package br.univesp.tcc.cdccnpj.events.api;

public record EventQuery(
        String eventType, String competenciaDe, String competenciaPara, String search, int page, int size) {

    public EventQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 200) {
            size = 50;
        }
    }
}
