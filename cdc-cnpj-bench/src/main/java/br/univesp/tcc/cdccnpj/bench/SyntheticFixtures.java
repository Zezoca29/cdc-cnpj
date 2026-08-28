package br.univesp.tcc.cdccnpj.bench;

import java.util.List;

public record SyntheticFixtures(
        List<SyntheticEmpresa> empresasDe,
        List<SyntheticEmpresa> empresasPara,
        List<SyntheticEstabelecimento> estabelecimentosDe,
        List<SyntheticEstabelecimento> estabelecimentosPara,
        List<ExpectedChange> expectedChanges) {
}
