# =====================================================================
# RWA 플랫폼 데모 테스트 스크립트
# 사용법: 백엔드(8080)와 Hardhat 노드(8545)가 실행 중인 상태에서
#         PowerShell 새 터미널에서 아래 실행:
#           cd c:\rwa\forHanwha
#           .\scripts\demo-test.ps1
# =====================================================================

$ErrorActionPreference = "Stop"
$adminEmail = $env:ADMIN_EMAIL
$adminPassword = $env:ADMIN_PASSWORD
if ([string]::IsNullOrWhiteSpace($adminEmail) -or [string]::IsNullOrWhiteSpace($adminPassword)) {
    throw "ADMIN_EMAIL and ADMIN_PASSWORD environment variables are required."
}

# 한글이 깨지지 않도록 UTF-8 바이트로 POST 하는 헬퍼 함수
function Invoke-JsonPost($url, $obj, $token) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json))
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes -Headers $headers
}
function Invoke-JsonGet($url, $token) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Get -Headers $headers
}

Write-Host "`n===== 1) 투자자 회원가입 =====" -ForegroundColor Cyan
$wallet = "0x90F79bf6EB2c4f870365E785982E1f101E93b906"  # Hardhat 계정 #3
$email = "demo$(Get-Random -Maximum 9999)@test.com"
$reg = Invoke-JsonPost "$base/api/auth/signup" @{ email=$email; password="password123"; walletAddress=$wallet }
$investorToken = $reg.token
Write-Host "가입 완료: $($reg.email) (역할: $($reg.role))"

Write-Host "`n===== 2) KYC 신청 (개인정보 암호화 저장) =====" -ForegroundColor Cyan
$kyc = Invoke-JsonPost "$base/api/kyc" @{ name="홍길동"; idNumber="900101-1234567"; walletAddress=$wallet } $investorToken
Write-Host "KYC 신청 완료: id=$($kyc.id), 상태=$($kyc.status)"

Write-Host "`n===== 3) 내 KYC 상태 조회 =====" -ForegroundColor Cyan
$myKyc = Invoke-JsonGet "$base/api/kyc/me" $investorToken
Write-Host "내 KYC 상태: $($myKyc.status)"

Write-Host "`n===== 4) 관리자 로그인 =====" -ForegroundColor Cyan
$admin = Invoke-JsonPost "$base/api/auth/login" @{ email=$adminEmail; password=$adminPassword }
$adminToken = $admin.token
Write-Host "관리자 로그인 완료 (역할: $($admin.role))"

Write-Host "`n===== 5) 관리자: KYC 신청 목록 =====" -ForegroundColor Cyan
$list = Invoke-JsonGet "$base/api/admin/kyc" $adminToken
Write-Host "심사중 신청 건수: $($list.Count)"

Write-Host "`n===== 6) 관리자: KYC 상세 조회 (복호화 + 접근로그) =====" -ForegroundColor Cyan
$resp = Invoke-WebRequest -UseBasicParsing -Uri "$base/api/admin/kyc/$($kyc.id)" -Method Get -Headers @{ Authorization = "Bearer $adminToken" }
$detail = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
Write-Host "복호화된 신청 정보(원문 JSON):"
Write-Host $detail

Write-Host "`n===== 7) 관리자: KYC 승인 (온체인 화이트리스트 등록) =====" -ForegroundColor Cyan
$approve = Invoke-JsonPost "$base/api/admin/kyc/$($kyc.id)/approve" @{} $adminToken
Write-Host "승인 완료! 상태=$($approve.status)"
Write-Host "온체인 화이트리스트 등록 트랜잭션 해시: $($approve.whitelistTxHash)" -ForegroundColor Green

Write-Host "`n===== 완료 =====" -ForegroundColor Cyan
Write-Host "위 트랜잭션 해시가 나왔다면, 백엔드가 실제 블록체인에 화이트리스트를 등록한 것입니다."
