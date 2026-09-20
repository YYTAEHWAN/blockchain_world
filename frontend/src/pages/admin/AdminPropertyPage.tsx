import { useEffect, useState } from 'react'
import { api, errorMessage } from '../../api/client'
import type { Property } from '../../api/types'

export function AdminPropertyPage() {
  const [items, setItems] = useState<Property[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const [form, setForm] = useState({
    name: '', address: '', appraisalValue: '', totalSupply: '', pricePerToken: '', description: '',
  })

  const load = () => {
    setLoading(true)
    api.get<Property[]>('/properties')
      .then((res) => setItems(res.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setBusy(true); setError(null); setMsg(null)
    try {
      const body = {
        name: form.name,
        address: form.address,
        appraisalValue: Number(form.appraisalValue),
        totalSupply: Number(form.totalSupply),
        pricePerToken: Number(form.pricePerToken),
        description: form.description || undefined,
      }
      const { data } = await api.post<Property>('/admin/properties', body)
      setMsg(`등록 완료: ${data.name} (토큰 ${data.tokenContractAddress})`)
      setForm({ name: '', address: '', appraisalValue: '', totalSupply: '', pricePerToken: '', description: '' })
      load()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h2>부동산 등록 · 토큰 발행</h2>
      <div className="two-col">
        <div className="card">
          <h3>새 부동산 등록</h3>
          <form onSubmit={submit} className="stack">
            <label>명칭<input value={form.name} onChange={set('name')} required /></label>
            <label>주소<input value={form.address} onChange={set('address')} required /></label>
            <label>감정가(원)<input type="number" value={form.appraisalValue} onChange={set('appraisalValue')} required /></label>
            <label>총 발행 수량<input type="number" value={form.totalSupply} onChange={set('totalSupply')} required /></label>
            <label>토큰 단가(원)<input type="number" value={form.pricePerToken} onChange={set('pricePerToken')} required /></label>
            <label>설명(선택)<input value={form.description} onChange={set('description')} /></label>
            {msg && <p className="success">{msg}</p>}
            {error && <p className="error">{error}</p>}
            <button type="submit" disabled={busy}>{busy ? '발행 중 (온체인 배포)...' : '등록 + 토큰 발행'}</button>
            <p className="tiny-hint">등록 시 Factory 컨트랙트가 호출되어 새 PropertyToken 이 온체인 배포됩니다.</p>
          </form>
        </div>

        <div>
          <h3>등록된 부동산</h3>
          {loading ? <p>불러오는 중...</p> : items.length === 0 ? (
            <p className="hint">등록된 부동산이 없습니다.</p>
          ) : (
            <table className="table">
              <thead><tr><th>ID</th><th>명칭</th><th>총량</th><th>잔여</th></tr></thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.id}>
                    <td>{p.id}</td><td>{p.name}</td>
                    <td>{p.totalSupply.toLocaleString()}</td>
                    <td>{p.remainingSupply.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
