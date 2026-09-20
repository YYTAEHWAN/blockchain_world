import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useWallet } from '../wallet/useWallet'
import { errorMessage } from '../api/client'

export function SignupPage() {
  const { signup } = useAuth()
  const { account, connect, hasWallet } = useWallet()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [wallet, setWallet] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const onConnect = async () => {
    const addr = await connect()
    if (addr) setWallet(addr)
  }

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await signup(email, password, wallet || undefined)
      navigate('/')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card form-card">
      <h2>회원가입</h2>
      <form onSubmit={onSubmit}>
        <label>이메일
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>비밀번호 (8자 이상)
          <input type="password" value={password} minLength={8} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <label>지갑 주소 (선택, KYC 시 필수)
          <input value={wallet} onChange={(e) => setWallet(e.target.value)} placeholder="0x..." />
        </label>
        {hasWallet && (
          <button type="button" className="secondary" onClick={onConnect}>
            {account ? `연결됨: ${account.slice(0, 8)}...` : 'MetaMask 연결'}
          </button>
        )}
        <p className="tiny-hint">
          데모 지갑은 Hardhat 계정 #1~#9 를 사용하세요 (예: 0x7099...79C8).
          운영자 계정 #0 (0xf39F...2266) 은 계좌관리기관 지갑이라 투자자로 사용할 수 없습니다.
        </p>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>{loading ? '가입 중...' : '회원가입'}</button>
      </form>
    </div>
  )
}
