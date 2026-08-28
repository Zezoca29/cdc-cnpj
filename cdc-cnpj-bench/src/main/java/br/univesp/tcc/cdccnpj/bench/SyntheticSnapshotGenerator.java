package br.univesp.tcc.cdccnpj.bench;

import br.univesp.tcc.cdccnpj.cnpj.CnpjValidator;
import br.univesp.tcc.cdccnpj.diff.ChangeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera par de snapshots sintéticos com taxa de mudança conhecida (D1).
 * Registros primários concentram-se no bucket {@code 000} para E2E.
 */
public final class SyntheticSnapshotGenerator {

    public static final String COMPETENCIA_DE = "2026-06";
    public static final String COMPETENCIA_PARA = "2026-07";
    public static final int PRIMARY_BUCKET = 0;

    public SyntheticFixtures generate() {
        String b0 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 0);
        String b1 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 1);
        String b2 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 2);
        String b3 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 3);
        String b4 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 4);
        String b5 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 5);
        String b6 = BucketBasicoFactory.nthBasicoForBucket(PRIMARY_BUCKET, 6);

        SyntheticEmpresa empStable = empresa(b0, "Empresa Estavel LTDA", "1000,00");
        SyntheticEmpresa empDeleted = empresa(b1, "Empresa Encerrada LTDA", "500,00");
        SyntheticEmpresa empInserted = empresa(b2, "Empresa Nova LTDA", "750,00");
        SyntheticEmpresa empAlteradaDe = empresa(b3, "Empresa Mutante LTDA", "1000,00");
        SyntheticEmpresa empAlteradaPara = empresa(b3, "Empresa Mutante LTDA", "2500,00");

        SyntheticEstabelecimento estabStable = estab(b0, "0001", matriz(), "Loja Estavel", "02", "Rua A", "6201500");
        SyntheticEstabelecimento estabSituacaoDe = estab(b4, "0001", matriz(), "Loja Sit", "02", "Rua B", "6201500");
        SyntheticEstabelecimento estabSituacaoPara = copyEstab(estabSituacaoDe, "08", "2026-07-02", "01");
        SyntheticEstabelecimento estabEnderecoDe = estab(b5, "0001", matriz(), "Loja End", "02", "Rua C", "6201500");
        SyntheticEstabelecimento estabEnderecoPara = new SyntheticEstabelecimento(
                estabEnderecoDe.cnpjBasico(),
                estabEnderecoDe.cnpjOrdem(),
                estabEnderecoDe.cnpjDv(),
                estabEnderecoDe.identificadorMatrizFilial(),
                estabEnderecoDe.nomeFantasia(),
                "02",
                "2019-01-01",
                "00",
                estabEnderecoDe.nomeCidadeExterior(),
                estabEnderecoDe.pais(),
                estabEnderecoDe.dataInicioAtividade(),
                estabEnderecoDe.cnaeFiscalPrincipal(),
                estabEnderecoDe.cnaeFiscalSecundaria(),
                estabEnderecoDe.tipoLogradouro(),
                "Avenida Nova",
                estabEnderecoDe.numero(),
                estabEnderecoDe.complemento(),
                estabEnderecoDe.bairro(),
                estabEnderecoDe.cep(),
                estabEnderecoDe.uf(),
                estabEnderecoDe.municipioReceita(),
                estabEnderecoDe.municipioIbge(),
                estabEnderecoDe.ddd1(),
                estabEnderecoDe.telefone1(),
                estabEnderecoDe.ddd2(),
                estabEnderecoDe.telefone2(),
                estabEnderecoDe.dddFax(),
                estabEnderecoDe.fax(),
                estabEnderecoDe.correioEletronico(),
                estabEnderecoDe.situacaoEspecial(),
                estabEnderecoDe.dataSituacaoEspecial());
        SyntheticEstabelecimento estabCnaeDe = estab(b6, "0001", matriz(), "Loja Cnae", "02", "Rua D", "6201500");
        SyntheticEstabelecimento estabCnaePara = new SyntheticEstabelecimento(
                estabCnaeDe.cnpjBasico(),
                estabCnaeDe.cnpjOrdem(),
                estabCnaeDe.cnpjDv(),
                estabCnaeDe.identificadorMatrizFilial(),
                estabCnaeDe.nomeFantasia(),
                estabCnaeDe.situacaoCadastral(),
                estabCnaeDe.dataSituacaoCadastral(),
                estabCnaeDe.motivoSituacaoCadastral(),
                estabCnaeDe.nomeCidadeExterior(),
                estabCnaeDe.pais(),
                estabCnaeDe.dataInicioAtividade(),
                "8599604",
                estabCnaeDe.cnaeFiscalSecundaria(),
                estabCnaeDe.tipoLogradouro(),
                estabCnaeDe.logradouro(),
                estabCnaeDe.numero(),
                estabCnaeDe.complemento(),
                estabCnaeDe.bairro(),
                estabCnaeDe.cep(),
                estabCnaeDe.uf(),
                estabCnaeDe.municipioReceita(),
                estabCnaeDe.municipioIbge(),
                estabCnaeDe.ddd1(),
                estabCnaeDe.telefone1(),
                estabCnaeDe.ddd2(),
                estabCnaeDe.telefone2(),
                estabCnaeDe.dddFax(),
                estabCnaeDe.fax(),
                estabCnaeDe.correioEletronico(),
                estabCnaeDe.situacaoEspecial(),
                estabCnaeDe.dataSituacaoEspecial());
        SyntheticEstabelecimento estabMixedDe = estab(b0, "0002", filial(), "Filial Mix", "02", "Rua E", "6201500");
        SyntheticEstabelecimento estabMixedPara = new SyntheticEstabelecimento(
                estabMixedDe.cnpjBasico(),
                estabMixedDe.cnpjOrdem(),
                estabMixedDe.cnpjDv(),
                estabMixedDe.identificadorMatrizFilial(),
                "Filial Mix Renomeada",
                estabMixedDe.situacaoCadastral(),
                estabMixedDe.dataSituacaoCadastral(),
                estabMixedDe.motivoSituacaoCadastral(),
                estabMixedDe.nomeCidadeExterior(),
                estabMixedDe.pais(),
                estabMixedDe.dataInicioAtividade(),
                estabMixedDe.cnaeFiscalPrincipal(),
                estabMixedDe.cnaeFiscalSecundaria(),
                estabMixedDe.tipoLogradouro(),
                estabMixedDe.logradouro(),
                estabMixedDe.numero(),
                estabMixedDe.complemento(),
                estabMixedDe.bairro(),
                estabMixedDe.cep(),
                estabMixedDe.uf(),
                estabMixedDe.municipioReceita(),
                estabMixedDe.municipioIbge(),
                "21",
                estabMixedDe.telefone1(),
                estabMixedDe.ddd2(),
                estabMixedDe.telefone2(),
                estabMixedDe.dddFax(),
                estabMixedDe.fax(),
                estabMixedDe.correioEletronico(),
                estabMixedDe.situacaoEspecial(),
                estabMixedDe.dataSituacaoEspecial());
        SyntheticEstabelecimento estabDeleted = estab(b1, "0001", matriz(), "Loja Baixada", "08", "Rua F", "6201500");
        SyntheticEstabelecimento estabInserted = estab(b2, "0001", matriz(), "Loja Nova", "02", "Rua G", "7319002");

        List<SyntheticEmpresa> empresasDe = List.of(empStable, empDeleted, empAlteradaDe);
        List<SyntheticEmpresa> empresasPara = List.of(empStable, empInserted, empAlteradaPara);
        List<SyntheticEstabelecimento> estabelecimentosDe = List.of(
                estabStable, estabSituacaoDe, estabEnderecoDe, estabCnaeDe, estabMixedDe, estabDeleted);
        List<SyntheticEstabelecimento> estabelecimentosPara = List.of(
                estabStable, estabSituacaoPara, estabEnderecoPara, estabCnaePara, estabMixedPara, estabInserted);

        List<ExpectedChange> expected = new ArrayList<>();
        expected.add(new ExpectedChange(ChangeType.DELETE, "empresas", empDeleted.entityKey(), List.of()));
        expected.add(new ExpectedChange(ChangeType.INSERT, "empresas", empInserted.entityKey(), List.of()));
        expected.add(new ExpectedChange(
                ChangeType.UPDATE, "empresas", empAlteradaDe.entityKey(), List.of("capital_social")));
        expected.add(new ExpectedChange(
                ChangeType.UPDATE,
                "estabelecimentos",
                estabSituacaoDe.entityKey(),
                List.of("data_situacao_cadastral", "motivo_situacao_cadastral", "situacao_cadastral")));
        expected.add(new ExpectedChange(
                ChangeType.UPDATE, "estabelecimentos", estabEnderecoDe.entityKey(), List.of("logradouro")));
        expected.add(new ExpectedChange(
                ChangeType.UPDATE,
                "estabelecimentos",
                estabCnaeDe.entityKey(),
                List.of("cnae_fiscal_principal")));
        expected.add(new ExpectedChange(
                ChangeType.UPDATE,
                "estabelecimentos",
                estabMixedDe.entityKey(),
                List.of("ddd_1", "nome_fantasia")));
        expected.add(new ExpectedChange(ChangeType.DELETE, "estabelecimentos", estabDeleted.entityKey(), List.of()));
        expected.add(new ExpectedChange(ChangeType.INSERT, "estabelecimentos", estabInserted.entityKey(), List.of()));

        return new SyntheticFixtures(
                empresasDe,
                empresasPara,
                estabelecimentosDe,
                estabelecimentosPara,
                List.copyOf(expected));
    }

    private static SyntheticEmpresa empresa(String basico, String razao, String capital) {
        return new SyntheticEmpresa(basico, razao, "2062", "49", capital, "01", "");
    }

    private static SyntheticEstabelecimento estab(
            String basico,
            String ordem,
            String matrizFilial,
            String fantasia,
            String situacao,
            String logradouro,
            String cnae) {
        String dv = CnpjValidator.computeCheckDigits(basico + ordem);
        return new SyntheticEstabelecimento(
                basico,
                ordem,
                dv,
                matrizFilial,
                fantasia,
                situacao,
                "2019-01-01",
                "00",
                "",
                "105",
                "2018-06-01",
                cnae,
                "",
                "RUA",
                logradouro,
                "100",
                "",
                "Centro",
                "01310100",
                "SP",
                "7107",
                "3550308",
                "11",
                "30000000",
                "",
                "",
                "",
                "",
                "contato@example.org",
                "",
                "");
    }

    private static SyntheticEstabelecimento copyEstab(
            SyntheticEstabelecimento base, String situacao, String dataSituacao, String motivo) {
        return new SyntheticEstabelecimento(
                base.cnpjBasico(),
                base.cnpjOrdem(),
                base.cnpjDv(),
                base.identificadorMatrizFilial(),
                base.nomeFantasia(),
                situacao,
                dataSituacao,
                motivo,
                base.nomeCidadeExterior(),
                base.pais(),
                base.dataInicioAtividade(),
                base.cnaeFiscalPrincipal(),
                base.cnaeFiscalSecundaria(),
                base.tipoLogradouro(),
                base.logradouro(),
                base.numero(),
                base.complemento(),
                base.bairro(),
                base.cep(),
                base.uf(),
                base.municipioReceita(),
                base.municipioIbge(),
                base.ddd1(),
                base.telefone1(),
                base.ddd2(),
                base.telefone2(),
                base.dddFax(),
                base.fax(),
                base.correioEletronico(),
                base.situacaoEspecial(),
                base.dataSituacaoEspecial());
    }

    private static String matriz() {
        return "1";
    }

    private static String filial() {
        return "2";
    }
}
