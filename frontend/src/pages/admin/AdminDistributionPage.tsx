import { useEffect, useState } from 'react'
import { api, errorMessage } from '../../api/client'
import type { Property, DistributionResult } from '../../api/types'

export function AdminDistributionPage() {
  const [properties, setProperties] = useState<Property[]>([])
  const [propertyId, setPropertyId] = useState<number | ''>('')
  const [totalAmount, setTotalAmount] = useState('')
  const [round, setRound] = useState('')
  const [rounds, setRounds] = useState<DistributionResult[]>([])
  const [lastResult, setLastResult] = useState<DistributionResult | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => {
    api.get<Property[]>('/properties').then((res) => setProperties(res.data)).catch(() => {})
  }, [])

  const loadRounds = (pid: number) => {
    api.get<DistributionResult[]>(`/admin/distributions?propertyId=${pid}`)
      .then((res) => setRounds(res.data))
      .catch(() => setRounds([]))
  }

  const onSelectProperty = (v: string) => {
    const pid = v ? Number(v) : ''
    setPropertyId(pid)
    if (pid) loadRounds(pid as number)
    else setRounds([])
  }

  const execute = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!propertyId) return
    setBusy(true); setError(null); setMsg(null)
    try {
      const body: Record<string, unknown> = { propertyId, totalAmount: Number(totalAmount) }
      if (round) body.round = Number(round)
      const { data } = await api.post<DistributionResult>('/admin/distributions', body)
      setLastResult(data)
      setMsg(`${data.round}회차 배당 집행 완료: 수령자 ${data.recipientCount}명, 분배 ${data.distributedAmount.toLocaleString()}원`)
      setTotalAmount(''); setRound('')
      loadRounds(propertyId as number)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h2>배당 집행</h2>
      <div className="two-col">
        <div className="card">
          <h3>새 배당 집행</h3>
          <form onSubmit={execute} className="stack">
            <label>부동산
              <select value={propertyId} onChange={(e) => onSelectProperty(e.target.value)} required>
                <option value="">선택</option>
                {properties.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </label>
            <label>배당 총액(원)<input type="number" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} required /></label>
            <label>회차(선택, 미입력 시 자동)<input type="number" value={round} onChange={(e) => setRound(e.target.value)} placeholder="자동" /></label>
            {msg && <p className="success">{msg}</p>}
            {error && <p className="error">{error}</p>}
            <button type="submit" disabled={busy || !propertyId}>{busy ? '집행 중...' : '배당 집행'}</button>
            <p className="tiny-hint">집행 시점 각 투자자의 온체인 잔고로 지분율을 산정하며, 미보유자는 제외됩니다.</p>
          </form>

          {lastResult && (
            <div className="card" style={{ marginTop: 16 }}>
              <h4>집행 결과 (#{lastResult.round}회차)</h4>
              <table className="table">
                <thead><tr><th>투자자ID</th><th>보유량</th><th>지분율</th><th>배당액</th></tr></thead>
                <tbody>
                  {lastResult.details.map((d) => (
                    <tr key={d.investorId}>
                      <td>{d.investorId}</td><td>{d.holdingQuantity.toLocaleString()}</td>
                      <td>{d.holdingRatio}%</td><td>{d.amount.toLocaleString()}원</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div>
          <h3>배당 회차 이력</h3>
          {!propertyId ? (
            <p className="hint">부동산을 선택하면 회차 이력이 표시됩니다.</p>
          ) : rounds.length === 0 ? (
            <p className="hint">집행된 배당이 없습니다.</p>
          ) : (
            <table className="table">
              <thead><tr><th>회차</th><th>총액</th><th>분배액</th><th>수령자</th><th>집행일</th></tr></thead>
              <tbody>
                {rounds.map((r) => (
                  <tr key={r.id}>
                    <td>{r.round}회</td>
                    <td>{r.totalAmount.toLocaleString()}</td>
                    <td>{r.distributedAmount.toLocaleString()}</td>
                    <td>{r.recipientCount}명</td>
                    <td>{new Date(r.executedAt).toLocaleDateString()}</td>
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
