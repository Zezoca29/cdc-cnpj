package br.univesp.tcc.cdccnpj.socio;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SocioIdentityResolverTest {

    @Test
    void maskedCpfChangeKeepsSameIdentityKey() {
        String keyBefore = SocioIdentityResolver.identityKey(
                "12345678", "1", "João da Silva", "***111111**");
        String keyAfter = SocioIdentityResolver.identityKey(
                "12345678", "1", "JOAO DA SILVA", "***999222**");
        assertEquals(keyBefore, keyAfter);
    }

    @Test
    void differentPeopleProduceDifferentKeys() {
        String personA = SocioIdentityResolver.identityKey("12345678", "1", "Maria Souza", "***111111**");
        String personB = SocioIdentityResolver.identityKey("12345678", "1", "Jose Alves", "***222222**");
        assertNotEquals(personA, personB);
    }

    @Test
    void sameIdentityFromRowMaps() {
        Map<String, String> before = Map.of(
                "cnpj_basico", "12345678",
                "identificador_socio", "1",
                "nome_socio", "João da Silva",
                "cpf_cnpj_socio", "***111111**");
        Map<String, String> after = Map.of(
                "cnpj_basico", "12345678",
                "identificador_socio", "1",
                "nome_socio", "JOAO DA SILVA",
                "cpf_cnpj_socio", "***999111**");
        assertEquals(SocioIdentityResolver.identityKeyFromRow(before), SocioIdentityResolver.identityKeyFromRow(after));
        assertEquals(true, SocioIdentityResolver.sameIdentity(before, after));
    }
}
