package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.Cliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Orquestra o envio de notificações ao cliente pelos canais disponíveis (email e WhatsApp). */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final EmailService emailService;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    /** Notifica o cliente por email (se cadastrado) e WhatsApp (se possível). */
    public void notificarCliente(Cliente cliente, Long barbeariaId, String assunto, String mensagem) {
        if (cliente == null) {
            return;
        }

        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            emailService.send(barbeariaId, cliente.getEmail(), assunto, mensagem);
        }

        if (cliente.getTelefone() != null && !cliente.getTelefone().isBlank()) {
            try {
                whatsAppCloudApiClient.enviarTexto(cliente.getTelefone(), mensagem);
            } catch (Exception e) {
                log.warn("Falha ao enviar WhatsApp para {}: {}", cliente.getTelefone(), e.getMessage());
            }
        }
    }
}
