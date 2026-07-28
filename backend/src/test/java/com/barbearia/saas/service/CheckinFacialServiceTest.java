package com.barbearia.saas.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/** Testes unitários do serviço de check-in facial. */
class CheckinFacialServiceTest {

    @Test
    void deveCalcularSimilaridadeIdentica() throws Exception {
        byte[] bytes = "face-demo-bytes-1234567890".getBytes();
        MockMultipartFile f1 = new MockMultipartFile("file", "a.jpg", "image/jpeg", bytes);
        MockMultipartFile f2 = new MockMultipartFile("file", "b.jpg", "image/jpeg", bytes);
        String a = CheckinFacialService.assinaturaImagem(f1);
        String b = CheckinFacialService.assinaturaImagem(f2);
        assertThat(CheckinFacialService.similaridade(a, b)).isEqualTo(1.0);
    }
}
