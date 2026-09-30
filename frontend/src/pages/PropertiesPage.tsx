import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Property, InvestmentTx } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { TxHashDisplay } from '../components/TxHashDisplay'

export function PropertiesPage() {
  const { user, isAdmin } = useAuth()
  const [items, setItems] = useState<Property[]>([])
  const [txs, setTxs] = useState<InvestmentTx[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState<string | null>(null)

  const canBuy = !!user && !isAdmin

  const loadProperties = useCallback(() => {
    return api.get<Property[]>('/properties')
      .then((res) => setItems(res.data))
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const loadTxs = useCallback(() => {
    if (!canBuy) return Promise.resolve()
    return api.get<InvestmentTx[]>('/investments/me/txs')
      .then((res) => setTxs(res.data))
      .catch(() => { /* 매매 내역은 조회 실패해도 화면은 유지 */ })
  }, [canBuy])

  useEffect(() => {
    Promise.all([loadProperties(), loadTxs()]).finally(() => setLoading(false))
  }, [loadProperties, loadTxs])

  // 매수 성공 시: 완료 알림 + 목록/내역 갱신
  const onBought = useCallback((quantity: number, amount: number, propertyName: string) => {
    setToast(`매수 완료: ${propertyName} ${quantity.toLocaleString()}개 (${amount.toLocaleString()}원)`)
    loadProperties()
    loadTxs()
    // 5초 후 알림 자동 닫기
    window.setTimeout(() => setToast(null), 5000)
  }, [loadProperties, loadTxs])

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error">{error}</p>

  return (
    <div>
      {toast && (
        <div className="toast" role="status">
          <span>✅ {toast}</span>
          <button className="toast-close" onClick={() => setToast(null)}>✕</button>
        </div>
      )}

      <h2>부동산 목록</h2>
      {items.length === 0 && (
        <p className="hint">등록된 부동산이 없습니다. 관리자가 부동산을 등록하면 여기에 표시됩니다.</p>
      )}
      <div className="grid">
        {items.map((p) => (
          <PropertyCard key={p.id} property={p} canBuy={canBuy} onBought={onBought} />
        ))}
      </div>

      {canBuy && (
        <section style={{ marginTop: 32 }}>
          <h3>내 매매 내역</h3>
          {txs.length === 0 ? (
            <p className="hint">아직 매매 내역이 없습니다. 위 목록에서 부동산을 매수해 보세요.</p>
          ) : (
            <table className="table">
              <thead>
                <tr><th>일시</th><th>유형</th><th>수량</th><th>금액</th><th>txHash</th></tr>
              </thead>
              <tbody>
                {txs.map((t) => (
                  <tr key={t.id}>
                    <td>{new Date(t.createdAt).toLocaleString()}</td>
                    <td>{t.type === 'BUY' ? '매수' : t.type}</td>
                    <td>{t.quantity.toLocaleString()}</td>
                    <td>{t.amount.toLocaleString()}원</td>
                    <td><TxHashDisplay hash={t.txHash} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </div>
  )
}

function PropertyCard({
  property,
  canBuy,
  onBought,
}: {
  property: Property
  canBuy: boolean
  onBought: (quantity: number, amount: number, propertyName: string) => void
}) {
  const [qty, setQty] = useState('1')  // 문자열로 변경
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const onBuy = async () => {
    setErr(null)
    
    // 입력값 검증
    const quantity = parseInt(qty)
    if (isNaN(quantity) || quantity <= 0) {
      setErr('매수할 개수를 입력해주세요 (1개 이상)')
      return
    }
    
    if (quantity > property.remainingSupply) {
      setErr(`최대 ${property.remainingSupply.toLocaleString()}개까지 매수 가능합니다`)
      return
    }
    
    setBusy(true)
    try {
      const { data } = await api.post('/investments', { propertyId: property.id, quantity })
      onBought(Number(data.quantity), Number(data.amount), property.name)
      setQty('1')  // 매수 후 초기화
    } catch (e) {
      setErr(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const handleQuantityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    
    // 빈 문자열 허용
    if (value === '') {
      setQty('')
      return
    }
    
    // 숫자만 입력 가능
    const numValue = parseInt(value)
    if (isNaN(numValue)) {
      return
    }
    
    // 최소값 0 (사용자가 0을 입력할 수 있도록)
    // 실제 매수 시에는 0 이상 체크
    if (numValue < 0) {
      setQty('0')
      return
    }
    
    // 최대값 제한 (잔여 청약량)
    if (numValue > property.remainingSupply) {
      setQty(property.remainingSupply.toString())
      return
    }
    
    setQty(numValue.toString())
  }

  return (
    <div className="card">
      <h3>{property.name}</h3>
      <p className="muted">{property.address}</p>
      <dl className="kv">
        <div><dt>감정가</dt><dd>{property.appraisalValue.toLocaleString()}원</dd></div>
        <div><dt>토큰 단가</dt><dd>{property.pricePerToken.toLocaleString()}원</dd></div>
        <div><dt>총 발행</dt><dd>{property.totalSupply.toLocaleString()}</dd></div>
        <div><dt>잔여 청약</dt><dd>{property.remainingSupply.toLocaleString()}</dd></div>
      </dl>

      {canBuy && (
        <>
          <div className="buy-box">
            <input
              type="number"
              min="0"
              max={property.remainingSupply}
              value={qty}
              onChange={handleQuantityChange}
              placeholder="매수할 개수"
            />
            <button onClick={onBuy} disabled={busy || property.remainingSupply <= 0}>
              {busy ? '처리 중...' : '매수'}
            </button>
          </div>
          <p className="tiny-hint">매수할 개수를 입력해주세요 (1개 이상)</p>
          {property.remainingSupply > 0 && (
            <p className="hint">최대 {property.remainingSupply.toLocaleString()}개까지 매수 가능합니다</p>
          )}
          <p className="tiny-hint">매수 시 계좌관리기관(운영자)이 온체인 이전을 대행합니다. KYC 승인이 필요합니다.</p>
        </>
      )}
      {err && <p className="error">{err}</p>}
      <p className="addr">토큰: {property.tokenContractAddress ? property.tokenContractAddress : '없음'}</p>
    </div>
  )
}
