/**
 * Consulta de CEP via ViaCEP (preenche endereço e código IBGE).
 */

export function soDigitosCep(valor) {
  return String(valor || '').replace(/\D/g, '').slice(0, 8)
}

export function formatarCep(valor) {
  const d = soDigitosCep(valor)
  if (d.length <= 5) return d
  return `${d.slice(0, 5)}-${d.slice(5)}`
}

/**
 * @returns {Promise<{
 *   cep: string,
 *   logradouro: string,
 *   bairro: string,
 *   localidade: string,
 *   uf: string,
 *   codigoMunicipioIbge: string,
 * }>}
 */
export async function buscarEnderecoPorCep(cep) {
  const digits = soDigitosCep(cep)
  if (digits.length !== 8) {
    throw new Error('Informe um CEP com 8 dígitos.')
  }

  const res = await fetch(`https://viacep.com.br/ws/${digits}/json/`)
  if (!res.ok) {
    throw new Error('Não foi possível consultar o CEP. Tente novamente.')
  }

  const data = await res.json()
  if (data?.erro) {
    throw new Error('CEP não encontrado.')
  }

  return {
    cep: formatarCep(data.cep || digits),
    logradouro: data.logradouro || '',
    bairro: data.bairro || '',
    localidade: data.localidade || '',
    uf: data.uf || '',
    codigoMunicipioIbge: data.ibge || '',
  }
}
