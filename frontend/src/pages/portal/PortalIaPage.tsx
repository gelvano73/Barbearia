// @ts-nocheck
/**
 * Chat de atendimento com IA no portal do cliente.
 */
import { useEffect, useRef, useState } from 'react'
import { portalApi } from '../../services/resources'

function bubble(role, text, extra = {}) {
  return { id: crypto.randomUUID(), role, text, ...extra }
}

export default function PortalIaPage() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [contexto, setContexto] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const endRef = useRef(null)

  // Effect: carga inicial dos dados
  useEffect(() => {
    setMessages([
      bubble(
        'assistant',
        'Olá! Sou a IA de atendimento. Posso sugerir serviços, mostrar horários livres e agendar para você.',
        { acoesRapidas: ['Sugerir serviços', 'Horários disponíveis', 'Quero agendar', 'Ver barbeiros'] }
      ),
    ])
  }, [])

  // Effect: carga inicial dos dados
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  // Envia mensagem ao assistente
  const enviar = async (texto, contextoOverride) => {
    const mensagem = (texto ?? input).trim()
    if (!mensagem || loading) return

    const ctx = contextoOverride !== undefined ? contextoOverride : contexto
    setInput('')
    setError('')
    setMessages((prev) => [...prev, bubble('user', mensagem)])
    setLoading(true)

    try {
      const { data } = await portalApi.iaChat({ mensagem, contexto: ctx })
      setContexto(data.contexto || null)
      setMessages((prev) => [
        ...prev,
        bubble('assistant', data.resposta, {
          intencao: data.intencao,
          servicosSugeridos: data.servicosSugeridos,
          horariosSugeridos: data.horariosSugeridos,
          acoesRapidas: data.acoesRapidas,
          agendamento: data.agendamento,
        }),
      ])
    } catch (err) {
      const data = err.response?.data
      const msg =
        data?.mensagem ||
        data?.message ||
        data?.error ||
        (typeof data === 'string' ? data : null) ||
        (err.code === 'ERR_NETWORK' ? 'API offline. Suba o backend na porta 8080.' : null) ||
        err.message ||
        'Falha ao falar com a assistente'
      setError(msg)
      setMessages((prev) => [
        ...prev,
        bubble('assistant', 'Não consegui responder agora. ' + msg, {
          acoesRapidas: ['Sugerir serviços', 'Horários disponíveis'],
        }),
      ])
    } finally {
      setLoading(false)
    }
  }

  // Seleciona horário sugerido pela IA
  const escolherHorario = (h) => {
    const next = {
      ...(contexto || {}),
      barbeiroId: h.barbeiroId,
      servicoId: h.servicoId || contexto?.servicoId || null,
      dataHora: h.dataHora,
      aguardandoConfirmacao: false,
    }
    setContexto(next)
    enviar(`Agendar ${h.label}`, next)
  }

  // Seleciona serviço sugerido pela IA
  const escolherServico = (s) => {
    const next = {
      ...(contexto || {}),
      servicoId: s.id,
      aguardandoConfirmacao: false,
    }
    setContexto(next)
    enviar(`Agendar ${s.nome}`, next)
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>IA de atendimento</h1>
          <p>Responde, sugere serviços e agenda horários</p>
        </div>
      </div>

      <div className="panel ia-chat">
        <div className="ia-messages">
          {messages.map((m) => (
            <div key={m.id} className={`ia-bubble ${m.role}`}>
              <div className="ia-text">{m.text}</div>

              {m.servicosSugeridos?.length > 0 && (
                <div className="ia-cards">
                  {m.servicosSugeridos.map((s) => (
                    <button key={s.id} type="button" className="ia-card" onClick={() => escolherServico(s)}>
                      <strong>{s.nome}</strong>
                      <span>
                        R$ {Number(s.preco).toFixed(2)} · {s.duracaoMinutos} min
                      </span>
                      {s.motivo && <em>{s.motivo}</em>}
                    </button>
                  ))}
                </div>
              )}

              {m.horariosSugeridos?.length > 0 && (
                <div className="ia-cards">
                  {m.horariosSugeridos.map((h) => (
                    <button
                      key={h.dataHora + h.barbeiroId}
                      type="button"
                      className="ia-card"
                      onClick={() => escolherHorario(h)}
                    >
                      <strong>{h.label}</strong>
                      <span>
                        {h.barbeiroNome}
                        {h.servicoNome ? ` · ${h.servicoNome}` : ''}
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {m.agendamento && (
                <div className="ia-ok">
                  Agendado #{m.agendamento.id} — {m.agendamento.barbeiroNome}
                </div>
              )}

              {m.acoesRapidas?.length > 0 && (
                <div className="ia-actions">
                  {m.acoesRapidas.map((a) => (
                    <button key={a} type="button" className="btn secondary small" onClick={() => enviar(a)}>
                      {a}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}
          {loading && <div className="ia-bubble assistant">Pensando...</div>}
          <div ref={endRef} />
        </div>

        {error && <div className="error">{error}</div>}

        <form
          className="ia-input"
          onSubmit={(e) => {
            e.preventDefault()
            enviar()
          }}
        >
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder='Ex.: "quero um corte amanhã às 15h"'
            disabled={loading}
          />
          <button className="btn" type="submit" disabled={loading || !input.trim()}>
            Enviar
          </button>
        </form>
      </div>
    </>
  )
}
