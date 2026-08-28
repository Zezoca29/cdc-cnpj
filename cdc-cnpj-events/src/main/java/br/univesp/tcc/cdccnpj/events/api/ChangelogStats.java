package br.univesp.tcc.cdccnpj.events.api;

public record ChangelogStats(
        long totalEvents,
        String competenciaDe,
        String competenciaPara,
        long distinctEventTypes) {}
