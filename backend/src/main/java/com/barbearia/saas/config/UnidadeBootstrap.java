package com.barbearia.saas.config;

import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.service.UnidadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Bootstrap que garante dados iniciais de unidades ao subir a aplicação. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnidadeBootstrap implements ApplicationRunner {

    private final BarbeariaRepository barbeariaRepository;
    private final UnidadeService unidadeService;

    /** Executa a inicialização de dados padrão de unidades. */
    @Override
    public void run(ApplicationArguments args) {
        barbeariaRepository.findAll().forEach(barbearia -> {
            try {
                unidadeService.criarPadrao(barbearia);
            } catch (Exception ex) {
                log.warn("Não foi possível garantir unidade padrão da barbearia {}: {}",
                        barbearia.getId(), ex.getMessage());
            }
        });
    }
}
