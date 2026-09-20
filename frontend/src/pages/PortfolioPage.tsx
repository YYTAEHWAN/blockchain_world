import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Holding, InvestmentTx, AccountStatus } from '../api/types'

export function PortfolioPage() {
  const [holdings, setHoldings] = useState<Holding[]>([])
  const [txs, setTxs] = useState<InvestmentTx[]>([])
  const [account, setAccount] = useState<AccountStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [requesting, setRequesting] = useState(false)

  function loadAll() {
    return Promise.all([
      api.get<Holding[]>('/investments/me'),
      api.get<InvestmentTx[]>('/investments/me/txs'),
      api.get<AccountStatus>('/investments/me/account'),
    ]).then(([h, t, a]) => {
      setHoldings(h.data)
      setTxs(t.data)
      setAccount(a.data)
    })
  }

  useEffect(() => {
    loadAll()
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  async function handleUnfreezeRequest() {
    const note = window.prompt('동결 해제를 요청합니다. 소명 내용을 입력하세요.\n(예: 정상적인 분산 매수였습니다 등)', '')
    if (note === null) return
    setRequesting(true)
    setError(null)
    try {
      await api.post('/investments/me/unfreeze-request', { note })
      await loadAll()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setRequesting(false)
    }
  }

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error">{error}</p>

  const totalValuation = holdings.reduce((s, h) => s + h.valuation, 0)

  return (
    <div>
      <h2>내 포트폴리오</h2>

      {account?.frozen && (
        <div className="card" style={{ borderLeft: '4px solid #c0392b', background: '#fdecea', marginBottom: 16 }}>
          <strong className="sev-high">⚠ 계정(지갑)이 동결되었습니다</strong>
          <p style={{ margin: '8px 0 4px' }}>
            이상거래 심사 결과에 따라 현재 지갑의 매수·전송이 제한됩니다.
          </p>
          <p style={{ margin: '2px 0' }}>
            <span className="muted">사유</span> : {account.reason?.trim() ? account.reason : '(별도 사유 미기재)'}
          </p>
          {account.frozenAt && (
            <p style={{ margin: '2px 0' }}>
              <span className="muted">처리일시</span> : {new Date(account.frozenAt).toLocaleString()}
            </p>
          )}

          {account.unfreezeRequested ? (
            <div style={{ marginTop: 10, padding: '8px 10px', background: '#fff5e6', borderRadius: 6 }}>
              <strong className="sev-medium">해제 요청 접수됨</strong>
              <p className="hint" style={{ margin: '4px 0 0' }}>
                소명 내용 : {account.unfreezeRequestNote?.trim() ? account.unfreezeRequestNote : '(내용 없음)'}
              </p>
              <p className="hint" style={{ margin: '2px 0 0' }}>
                관리자 검토 후 해제됩니다. 필요하면 아래 버튼으로 소명을 다시 제출할 수 있습니다.
              </p>
              <button className="sm secondary" style={{ marginTop: 8 }} disabled={requesting} onClick={handleUnfreezeRequest}>
                소명 다시 제출
              </button>
            </div>
          ) : (
            <div style={{ marginTop: 10 }}>
              <p className="hint" style={{ margin: '0 0 6px' }}>
                동결이 부당하다고 생각되면 소명과 함께 해제를 요청할 수 있습니다.
              </p>
              <button className="danger sm" disabled={requesting} onClick={handleUnfreezeRequest}>
                {requesting ? '요청 중...' : '동결 해제 요청(소명)'}
              </button>
            </div>
          )}
        </div>
      )}

      <div className="card summary">
        <div><span className="muted">보유 부동산</span><strong>{holdings.length}건</strong></div>
        <div><span className="muted">총 평가금액</span><strong>{totalValuation.toLocaleString()}원</strong></div>
      </div>

      <h3>보유 현황</h3>
      {holdings.length === 0 ? (
        <p className="hint">보유 중인 부동산 토큰이 없습니다. 부동산 목록에서 매수해 보세요.</p>
      ) : (
        <table className="table">
          <thead>
            <tr><th>부동산</th><th>보유량</th><th>지분율</th><th>평가금액</th></tr>
          </thead>
          <tbody>
            {holdings.map((h) => (
              <tr key={h.propertyId}>
                <td>{h.propertyName}</td>
                <td>{h.holdingQuantity.toLocaleString()}</td>
                <td>{h.holdingRatio}%</td>
                <td>{h.valuation.toLocaleString()}원</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h3 style={{ marginTop: 28 }}>거래 이력</h3>
      {txs.length === 0 ? (
        <p className="hint">거래 이력이 없습니다.</p>
      ) : (
        <table className="table">
          <thead>
            <tr><th>일시</th><th>유형</th><th>수량</th><th>금액</th><th>txHash</th></tr>
          </thead>
          <tbody>
            {txs.map((t) => (
              <tr key={t.id}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.type}</td>
                <td>{t.quantity.toLocaleString()}</td>
                <td>{t.amount.toLocaleString()}원</td>
                <td className="addr">{t.txHash?.slice(0, 18)}...</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
