package br.univesp.tcc.cdccnpj.events.mapping;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.events.model.EventTopics;

import java.util.List;
import java.util.Set;

public final class EventTypeResolver {

    private EventTypeResolver() {
    }

    public static String resolve(ChangeRecord change) {
        return switch (change.tabela()) {
            case "empresas" -> resolveEmpresa(change.changeType());
            case "estabelecimentos" -> resolveEstabelecimento(change);
            case "socios" -> resolveSocio(change.changeType());
            case "simples" -> "simples.opcao_alterada";
            default -> "dominio.tabela_alterada";
        };
    }

    private static String resolveEmpresa(ChangeType changeType) {
        return switch (changeType) {
            case INSERT -> "empresa.criada";
            case DELETE -> "empresa.encerrada";
            case UPDATE -> "empresa.alterada";
        };
    }

    private static String resolveSocio(ChangeType changeType) {
        return switch (changeType) {
            case INSERT -> "socio.incluido";
            case DELETE -> "socio.excluido";
            case UPDATE -> "socio.alterado";
        };
    }

    private static String resolveEstabelecimento(ChangeRecord change) {
        return switch (change.changeType()) {
            case INSERT -> "estabelecimento.criado";
            case DELETE -> {
                String situacao = change.before().get("situacao_cadastral");
                if ("08".equals(situacao)) {
                    yield "estabelecimento.situacao_alterada";
                }
                yield "estabelecimento.alterado";
            }
            case UPDATE -> resolveEstabelecimentoUpdate(change.camposAlterados());
        };
    }

    private static String resolveEstabelecimentoUpdate(List<String> camposAlterados) {
        if (camposAlterados.isEmpty()) {
            return "estabelecimento.alterado";
        }
        Set<String> changed = Set.copyOf(camposAlterados);
        if (EventTopics.isSubset(EventTopics.situacaoFields(), changed)) {
            return "estabelecimento.situacao_alterada";
        }
        if (EventTopics.isSubset(EventTopics.enderecoFields(), changed)) {
            return "estabelecimento.endereco_alterado";
        }
        if (EventTopics.isSubset(EventTopics.cnaeFields(), changed)) {
            return "estabelecimento.cnae_alterado";
        }
        return "estabelecimento.alterado";
    }
}
