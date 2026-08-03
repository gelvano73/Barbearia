/**
 * Campo de CEP com botão para pesquisar endereço (ViaCEP).
 */
import { useState } from 'react'
import { buscarEnderecoPorCep, formatarCep, soDigitosCep } from '../utils/cep'

export type EnderecoCep = {
  cep: string
  logradouro: string
  bairro: string
  localidade: string
  uf: string
  codigoMunicipioIbge: string
}

type Props = {
  value: string
  onChange: (cepFormatado: string) => void
  onFound: (endereco: EnderecoCep) => void
  onError?: (mensagem: string) => void
  onInfo?: (mensagem: string) => void
  hint?: boolean
  className?: string
}

/** Monta uma linha de endereço a partir do retorno do ViaCEP. */
export function montarLinhaEndereco(end: EnderecoCep, numero = '') {
  const partes = [
    end.logradouro,
    numero ? `nº ${numero}` : null,
    end.bairro,
    end.localidade && end.uf ? `${end.localidade}/${end.uf}` : end.localidade || end.uf,
    end.cep ? `CEP ${end.cep}` : null,
  ].filter(Boolean)
  return partes.join(', ')
}

export default function CepLookupField({
  value,
  onChange,
  onFound,
  onError,
  onInfo,
  hint = true,
  className,
}: Props) {
  const [buscando, setBuscando] = useState(false)
  const cepPronto = soDigitosCep(value).length === 8

  const pesquisar = async () => {
    if (!cepPronto) {
      onError?.('Digite o CEP completo (8 dígitos) para pesquisar o endereço.')
      return
    }
    setBuscando(true)
    try {
      const end = await buscarEnderecoPorCep(value)
      onChange(end.cep)
      onFound(end)
      onInfo?.(
        end.localidade
          ? `Endereço encontrado: ${end.localidade}/${end.uf}. Confira e complete o número, se precisar.`
          : 'Endereço encontrado. Confira os campos.',
      )
    } catch (err) {
      onError?.(err instanceof Error ? err.message : 'Falha ao pesquisar CEP')
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div className={className} style={{ display: 'grid', gap: '0.4rem' }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '0.75rem', alignItems: 'end' }}>
        <label style={{ margin: 0 }}>
          CEP
          <input
            value={value}
            onChange={(e) => onChange(formatarCep(e.target.value))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                pesquisar()
              }
            }}
            placeholder="00000-000"
            inputMode="numeric"
            maxLength={9}
            autoComplete="postal-code"
          />
        </label>
        <button
          className="btn secondary"
          type="button"
          onClick={pesquisar}
          disabled={!cepPronto || buscando}
          style={{ whiteSpace: 'nowrap', height: '2.65rem' }}
        >
          {buscando ? 'Pesquisando…' : 'Pesquisar endereço'}
        </button>
      </div>
      {hint && cepPronto && !buscando && (
        <p className="subtitle" style={{ margin: 0 }}>
          CEP preenchido — clique em <strong>Pesquisar endereço</strong> para completar os dados.
        </p>
      )}
    </div>
  )
}
