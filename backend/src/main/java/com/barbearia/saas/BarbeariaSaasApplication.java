package com.barbearia.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação Spring Boot do SaaS de barbearias.
 * Inicializa o contexto, habilita agendamento de tarefas e o scan de
 * propriedades de configuração tipadas.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class BarbeariaSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarbeariaSaasApplication.class, args);
    }
}
