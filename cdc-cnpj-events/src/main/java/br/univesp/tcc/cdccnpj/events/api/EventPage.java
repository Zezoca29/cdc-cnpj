package br.univesp.tcc.cdccnpj.events.api;

import java.util.List;

public record EventPage(List<ChangelogEventRecord> items, long total, int page, int size) {}
