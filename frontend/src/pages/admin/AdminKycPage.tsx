import { useEffect, useState } from 'react'
import { api, errorMessage } from '../../api/client'
import type { KycAdminView, KycHistory } from '../../api/types'

interface Pending {
  id: number
  status: string
  walletAddress: string
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: '심사중', APPROVED: '승인', REJECTED: '반려',
}
const STATUS_CLASS: Record<string, string> = {
  PENDING: 'status-pending', APPROVED: 'status-approved', REJECTED: 'status-rejected',
}

export function AdminKycPage() {
  const [pending, setPending] = useState<Pending[]>([])
  const [history, setHistory] = useState<KycHistory[]>([])
  const [detail, setDetail] = useState<KycAdminView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    Promise.all([
      api.get<Pending[]>('/admin/kyc'),
      api.get<KycHistory[]>('/admin/kyc/history'),
    ])
      .then(([p, h]) => {
        setPending(p.data)
        setHistory(h.data)
      })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const openDetail = async (id: number) => {
    setError(null); setMsg(null); setDetail(null)
    try {
      const { data } = await api.get<KycAdminView>(`/admin/kyc/${id}`)
      setDetail(data)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const approve = async (id: number) => {
    setBusy(true); setError(null); setMsg(null)
    try {
      await api.post(`/admin/kyc/${id}/approve`)
      setMsg(`KYC #${id} 승인 완료 (온체인 화이트리스트 등록)`)
      setDetail(null); load()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const reject = async (id: number) => {
    const reason = prompt('반려 사유를 입력하세요')
    if (!reason) return
    setBusy(true); setError(null); setMsg(null)
    try {
      await api.post(`/admin/kyc/${id}/reject`, { reason })
      setMsg(`KYC #${id} 반려 완료`)
      setDetail(null); load()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <p>불러오는 중...</p>

  return (
    <div>
      <h2>KYC 심사</h2>
      {msg && <p className="success">{msg}</p>}
      {error && <p className="error">{error}</p>}

      <div className="two-col">
        <div>
          <h3>심사중 신청</h3>
          {pending.length === 0 ? (
            <p className="hint">심사 대기 중인 신청이 없습니다.</p>
          ) : (
            <table className="table">
              <thead><tr><th>ID</th><th>지갑</th><th></th></tr></thead>
              <tbody>
                {pending.map((p) => (
                  <tr key={p.id}>
                    <td>{p.id}</td>
                    <td className="addr">{p.walletAddress?.slice(0, 12)}...</td>
                    <td><button className="secondary sm" onClick={() => openDetail(p.id)}>상세</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div>
          <h3>상세 심사</h3>
          {detail ? (
            <div className="card">
              <dl className="kv">
                <div><dt>신청 ID</dt><dd>{detail.id}</dd></div>
                <div><dt>이름</dt><dd>{detail.name}</dd></div>
                <div><dt>식별번호</dt><dd>{detail.idNumber}</dd></div>
                <div><dt>지갑</dt><dd className="addr">{detail.walletAddress}</dd></div>
                <div><dt>상태</dt><dd>{detail.status}</dd></div>
              </dl>
              <p className="tiny-hint">복호화 조회 시 개인신용정보 접근 로그가 기록됩니다 (요구사항 7-3).</p>
              <div className="btn-row">
                <button onClick={() => approve(detail.id)} disabled={busy}>승인 (화이트리스트 등록)</button>
                <button className="secondary" onClick={() => reject(detail.id)} disabled={busy}>반려</button>
              </div>
            </div>
          ) : (
            <p className="hint">왼쪽 목록에서 신청을 선택하세요.</p>
          )}
        </div>
      </div>

      <h3 style={{ marginTop: 32 }}>KYC 심사 내역</h3>
      {history.length === 0 ? (
        <p className="hint">심사 내역이 없습니다.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>ID</th><th>투자자</th><th>지갑</th><th>상태</th>
              <th>심사자</th><th>심사일시</th><th>신청일시</th><th>비고</th>
            </tr>
          </thead>
          <tbody>
            {history.map((h) => (
              <tr key={h.id}>
                <td>{h.id}</td>
                <td>{h.investorEmail ?? h.investorId}</td>
                <td className="addr">{h.walletAddress?.slice(0, 12)}...</td>
                <td><span className={STATUS_CLASS[h.status]}>{STATUS_LABEL[h.status] ?? h.status}</span></td>
                <td>{h.reviewedBy ?? '-'}</td>
                <td>{h.reviewedAt ? new Date(h.reviewedAt).toLocaleString() : '-'}</td>
                <td>{new Date(h.createdAt).toLocaleString()}</td>
                <td>{h.status === 'REJECTED' ? (h.rejectReason ?? '') : ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
