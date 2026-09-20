import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { MyDistribution } from '../api/types'

export function DistributionsPage() {
  const [data, setData] = useState<MyDistribution | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<MyDistribution>('/distributions/me')
      .then((res) => setData(res.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error">{error}</p>

  const items = data?.items ?? []

  return (
    <div>
      <h2>내 배당 내역</h2>

      <div className="card summary">
        <div><span className="muted">누적 배당액</span>
          <strong>{(data?.totalReceived ?? 0).toLocaleString()}원</strong>
        </div>
        <div><span className="muted">배당 횟수</span><strong>{items.length}회</strong></div>
      </div>

      {items.length === 0 ? (
        <p className="hint">아직 수령한 배당이 없습니다.</p>
      ) : (
        <table className="table">
          <thead>
            <tr><th>일시</th><th>부동산</th><th>회차</th><th>보유량</th><th>지분율</th><th>배당액</th></tr>
          </thead>
          <tbody>
            {items.map((it) => (
              <tr key={it.distributionId}>
                <td>{new Date(it.executedAt).toLocaleString()}</td>
                <td>{it.propertyName}</td>
                <td>{it.round}회차</td>
                <td>{it.holdingQuantity.toLocaleString()}</td>
                <td>{it.holdingRatio}%</td>
                <td>{it.amount.toLocaleString()}원</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
