import { useState } from 'react'
import { api, errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'

interface SystemStatus {
  investorCount: number
  propertyCount: number
  isDevelopment: boolean
  timestamp: string
}

export function DevToolsPage() {
  const { isAdmin } = useAuth()
  const [status, setStatus] = useState<SystemStatus | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const loadStatus = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get<SystemStatus>('/dev/status')
      setStatus(response.data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const resetDatabase = async () => {
    if (!window.confirm('정말로 데이터베이스를 초기화하시겠습니까?\n모든 데이터가 삭제됩니다!')) {
      return
    }
    
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      const response = await api.post('/dev/reset-db')
      setMessage(response.data)
      await loadStatus()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const createSampleData = async () => {
    if (!window.confirm('샘플 데이터를 생성하시겠습니까?\n기존 데이터는 모두 삭제됩니다.')) {
      return
    }
    
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      const response = await api.post('/dev/create-sample-data')
      setMessage(response.data)
      await loadStatus()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (!isAdmin) {
    return (
      <div className="card">
        <h2>접근 거부</h2>
        <p className="error">관리자만 접근할 수 있는 페이지입니다.</p>
      </div>
    )
  }

  return (
    <div>
      <h2>개발 도구</h2>
      <p className="hint">시스템 초기화 및 테스트 데이터 생성을 위한 도구입니다. 신중하게 사용하세요.</p>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <div className="card" style={{ marginBottom: '20px' }}>
        <h3>시스템 상태</h3>
        <div className="btn-row">
          <button onClick={loadStatus} disabled={loading}>
            {loading ? '불러오는 중...' : '상태 조회'}
          </button>
        </div>
        
        {status && (
          <div style={{ marginTop: '16px' }}>
            <dl className="kv">
              <div><dt>투자자 수</dt><dd>{status.investorCount}명</dd></div>
              <div><dt>부동산 수</dt><dd>{status.propertyCount}개</dd></div>
              <div><dt>개발 환경</dt><dd>{status.isDevelopment ? '예' : '아니오'}</dd></div>
              <div><dt>조회 시간</dt><dd>{new Date(status.timestamp).toLocaleString()}</dd></div>
            </dl>
          </div>
        )}
      </div>

      <div className="two-col">
        <div className="card">
          <h3>데이터베이스 초기화</h3>
          <p className="hint">주의: 모든 데이터가 삭제됩니다.</p>
          <ul style={{ fontSize: '14px', color: '#666', margin: '10px 0' }}>
            <li>투자자 계정 삭제</li>
            <li>부동산 정보 삭제</li>
            <li>KYC 신청 내역 삭제</li>
            <li>매수 기록 삭제</li>
          </ul>
          <button 
            onClick={resetDatabase} 
            disabled={loading}
            className="danger"
            style={{ marginTop: '10px' }}
          >
            데이터베이스 초기화
          </button>
        </div>

        <div className="card">
          <h3>샘플 데이터 생성</h3>
          <p className="hint">테스트용 샘플 데이터를 생성합니다.</p>
          <ul style={{ fontSize: '14px', color: '#666', margin: '10px 0' }}>
            <li>관리자 계정: admin@local.test / admin1234</li>
            <li>투자자 1: user1@rwa.com / admin1234</li>
            <li>투자자 2: user2@rwa.com / admin1234</li>
            <li>투자자 3: user3@rwa.com / admin1234</li>
            <li>부동산 3개: 검증 빌딩 1, 2, 3</li>
          </ul>
          <button 
            onClick={createSampleData} 
            disabled={loading}
            style={{ marginTop: '10px' }}
          >
            샘플 데이터 생성
          </button>
        </div>
      </div>

      <div className="card" style={{ marginTop: '20px' }}>
        <h3>계정 정보</h3>
        <div className="two-col">
          <div>
            <h4>관리자 계정</h4>
            <dl className="kv">
              <div><dt>이메일</dt><dd>admin@local.test</dd></div>
              <div><dt>비밀번호</dt><dd>admin1234</dd></div>
              <div><dt>지갑 주소</dt><dd>0xf39Fd6e...92266</dd></div>
            </dl>
          </div>
          <div>
            <h4>투자자 계정</h4>
            <dl className="kv">
              <div><dt>이메일</dt><dd>user1@rwa.com</dd></div>
              <div><dt>비밀번호</dt><dd>user1234</dd></div>
              <div><dt>지갑 주소</dt><dd>0x7099797...79C8</dd></div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  )
}