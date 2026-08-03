/**
 * CRUD de unidades/filiais da barbearia.
 */
import { useEffect, useState } from 'react'
import CepLookupField, { montarLinhaEndereco } from '../components/CepLookupField'
import Modal from '../components/Modal'
import { unidadesApi } from '../services/resources'

/** === Estado inicial === */
const empty = { nome: '', cep: '', endereco: '', telefone: '', padrao: false }

export default function UnidadesPage() {
  /** === Estado === */
  const [itens, setItens] = useState([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [okCep, setOkCep] = useState('')

  /** === Carga de dados === */
  const carregar = async () => {
    const { data } = await unidadesApi.listar(true)
    setItens(data)
  }

  useEffect(() => {
    carregar().catch(() => setError('Não foi possível carregar unidades'))
  }, [])

  /** === Ações CRUD === */
  const abrirNovo = () => {
    setEditing(null)
    setForm(empty)
    setError('')
    setOkCep('')
    setOpen(true)
  }

  const abrirEdicao = (item) => {
    setEditing(item)
    setForm({
      nome: item.nome || '',
      cep: '',
      endereco: item.endereco || '',
      telefone: item.telefone || '',
      padrao: !!item.padrao,
    })
    setError('')
    setOkCep('')
    setOpen(true)
  }

  const payload = () => ({
    nome: form.nome.trim(),
    endereco: form.endereco.trim() || null,
    telefone: form.telefone.trim() || null,
    padrao: !!form.padrao,
  })

  const salvar = async (e) => {
    e.preventDefault()
    setError('')
    try {
      if (editing) await unidadesApi.atualizar(editing.id, payload())
      else await unidadesApi.criar(payload())
      setOpen(false)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao salvar unidade')
    }
  }

  const desativar = async (id) => {
    if (!confirm('Desativar esta unidade?')) return
    try {
      await unidadesApi.desativar(id)
      await carregar()
    } catch (err) {
      setError(err.response?.data?.mensagem || 'Erro ao desativar')
    }
  }

  return (
    <>
      {/* === Cabeçalho === */}
      <div className="page-header">
        <div>
          <h1>Unidades</h1>
          <p>Filiais e lojas da barbearia</p>
        </div>
        <button className="btn" type="button" onClick={abrirNovo}>
          Nova unidade
        </button>
      </div>

      {error && !open && <div className="error">{error}</div>}

      {/* === Tabela de unidades === */}
      <div className="panel">
        {itens.length === 0 ? (
          <div className="empty">Nenhuma unidade cadastrada.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Telefone</th>
                <th>Endereço</th>
                <th>Padrão</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id}>
                  <td>{item.nome}</td>
                  <td>{item.telefone || '—'}</td>
                  <td>{item.endereco || '—'}</td>
                  <td>{item.padrao ? 'Sim' : '—'}</td>
                  <td>
                    <div className="actions-row">
                      <button className="btn secondary" type="button" onClick={() => abrirEdicao(item)}>
                        Editar
                      </button>
                      {!item.padrao && (
                        <button className="btn secondary" type="button" onClick={() => desativar(item.id)}>
                          Desativar
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* === Modal === */}
      <Modal open={open} title={editing ? 'Editar unidade' : 'Nova unidade'} onClose={() => setOpen(false)}>
        <form onSubmit={salvar}>
          <div className="form-grid">
            <label className="full">
              Nome
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </label>
            <label>
              Telefone
              <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
            </label>
            <label>
              Unidade padrão
              <select
                value={form.padrao ? 'sim' : 'nao'}
                onChange={(e) => setForm({ ...form, padrao: e.target.value === 'sim' })}
              >
                <option value="nao">Não</option>
                <option value="sim">Sim</option>
              </select>
            </label>
            <div className="full">
              <CepLookupField
                value={form.cep}
                onChange={(cep) => {
                  setError('')
                  setOkCep('')
                  setForm((prev) => ({ ...prev, cep }))
                }}
                onFound={(end) => {
                  setForm((prev) => ({
                    ...prev,
                    cep: end.cep,
                    endereco: montarLinhaEndereco(end),
                  }))
                }}
                onError={setError}
                onInfo={setOkCep}
              />
            </div>
            <label className="full">
              Endereço
              <input
                value={form.endereco}
                onChange={(e) => setForm({ ...form, endereco: e.target.value })}
                placeholder="Logradouro, nº, bairro — cidade/UF"
              />
            </label>
          </div>
          {okCep && <div className="success" style={{ marginTop: '0.75rem' }}>{okCep}</div>}
          {error && <div className="error">{error}</div>}
          <button className="btn" type="submit" style={{ marginTop: '1rem' }}>
            Salvar
          </button>
        </form>
      </Modal>
    </>
  )
}
