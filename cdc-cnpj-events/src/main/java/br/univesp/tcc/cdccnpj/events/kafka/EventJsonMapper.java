package br.univesp.tcc.cdccnpj.events.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class EventJsonMapper {

    private EventJsonMapper() {
    }

    public static ObjectMapper create() {
        return new ObjectMapper();
    }
}
