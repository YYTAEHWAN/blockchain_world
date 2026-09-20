/** 백엔드 응답 DTO 타입 정의 */

export interface Property {
  id: number
  name: string
  address: string
  appraisalValue: number
  totalSupply: number
  remainingSupply: number
  pricePerToken: number
  tokenContractAddress: string
  deployTxHash: string | null
  status: string
  description: string | null
  imageUrl: string | null
  docUrl: string | null
}

export interface KycStatus {
  id: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  walletAddress: string
  rejectReason: string | null
  whitelistTxHash: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string | null
}

export interface Holding {
  propertyId: number
  propertyName: string
  tokenContractAddress: string
  holdingQuantity: number
  totalSupply: number
  holdingRatio: number
  valuation: number
}

export interface InvestmentTx {
  id: number
  propertyId: number
  type: string
  quantity: number
  amount: number
  txHash: string
  createdAt: string
}

export interface MyDistributionItem {
  distributionId: number
  propertyId: number
  propertyName: string
  round: number
  holdingQuantity: number
  holdingRatio: number
  amount: number
  executedAt: string
}

export interface MyDistribution {
  totalReceived: number
  items: MyDistributionItem[]
}

/** ===== 관리자용 DTO ===== */

export interface KycAdminView {
  id: number
  investorId: number
  name: string
  idNumber: string
  walletAddress: string
  status: string
  rejectReason: string | null
}

export interface KycHistory {
  id: number
  investorId: number
  investorEmail: string | null
  walletAddress: string
  status: string
  rejectReason: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string
}

export interface DistributionDetail {
  investorId: number
  holdingQuantity: number
  holdingRatio: number
  amount: number
}

export interface DistributionResult {
  id: number
  propertyId: number
  round: number
  totalAmount: number
  distributedAmount: number
  snapshotAt: string
  executedAt: string
  recipientCount: number
  details: DistributionDetail[]
}

export interface MonitoringSummary {
  totalTransactions: number
  totalQuantity: number
  totalAmount: number
  distinctInvestors: number
  propertyCount: number
  anomalyCount: number
  unreviewedAnomalyCount: number
}

export interface AdminTx {
  txId: number
  investorId: number
  investorEmail: string
  propertyId: number
  propertyName: string
  type: string
  quantity: number
  amount: number
  txHash: string
  createdAt: string
}

export interface AnomalyFlag {
  id: number
  investmentTxId: number
  investorId: number
  investorEmail: string | null
  propertyId: number
  propertyName: string | null
  rule: string
  detail: string
  severity: string
  reviewed: boolean
  action: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  reviewNote: string | null
  unfreezeRequested: boolean
  unfreezeRequestNote: string | null
  unfreezeRequestedAt: string | null
  detectedAt: string
}

export interface AccountStatus {
  frozen: boolean
  reason: string | null
  frozenAt: string | null
  frozenBy: string | null
  unfreezeRequested: boolean
  unfreezeRequestNote: string | null
}
