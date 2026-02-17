param(
    # Set PROJECT_TRACKER_API_KEY in your environment or pass -ApiKey explicitly.
    [string]$BaseUrl = "http://localhost:8080/tracker/api",
    [string]$ApiKey = $env:PROJECT_TRACKER_API_KEY,
    [string]$ProviderId = "12",
    [int]$ProjectId = 48689,
    [int]$ActionNextId = 786633,
    [int]$ContactId = 66747
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "ApiKey is required. Set -ApiKey or $env:PROJECT_TRACKER_API_KEY."
}
if ([string]::IsNullOrWhiteSpace($ProviderId)) {
    throw "ProviderId is required. Set -ProviderId or $env:PROVIDER_ID."
}
if ($ProjectId -le 0) {
    throw "ProjectId must be provided and > 0."
}
if ($ActionNextId -le 0) {
    throw "ActionNextId must be provided and > 0."
}
if ($ContactId -le 0) {
    throw "ContactId must be provided and > 0."
}

$headers = @{
    "X-Api-Key" = $ApiKey
}

function Assert-True([bool]$condition, [string]$message) {
    if (-not $condition) {
        throw $message
    }
}

Write-Host "1) Unauthorized request blocked..."
try {
    Invoke-WebRequest -Uri "$BaseUrl/v1/projects" -Method Get -UseBasicParsing | Out-Null
    throw "Expected 401 but request succeeded."
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Assert-True ($status -eq 401) "Expected 401, got $status"
}

Write-Host "2) OpenAPI endpoint returns spec..."
$openApi = Invoke-WebRequest -Uri "$BaseUrl/openapi.json" -Method Get -Headers $headers -UseBasicParsing
Assert-True ($openApi.StatusCode -eq 200) "OpenAPI status $($openApi.StatusCode)"
Assert-True ($openApi.Content -match "openapi") "OpenAPI content missing 'openapi' field"

Write-Host "3) List projects (provider-scoped) and verify providerId in results..."
$projects = Invoke-RestMethod -Uri "$BaseUrl/v1/projects" -Method Get -Headers $headers
Assert-True ($projects.Count -gt 0) "No projects returned."
$foreign = @($projects | Where-Object { $_.providerId -and $_.providerId -ne $ProviderId })
Assert-True ($foreign.Count -eq 0) "Found projects outside provider scope."

Write-Host "4) List next actions for project..."
$actionsNext = Invoke-RestMethod -Uri "$BaseUrl/v1/projects/$ProjectId/actions/next" -Method Get -Headers $headers
Assert-True ($null -ne $actionsNext) "Next actions request failed."

Write-Host "5) List actions taken for project..."
$actionsTaken = Invoke-RestMethod -Uri "$BaseUrl/v1/projects/$ProjectId/actions/taken" -Method Get -Headers $headers
Assert-True ($null -ne $actionsTaken) "Taken actions request failed."

Write-Host "6) Create project-level proposal (supersede prior) and verify list..."
$proposalPayload = @{
    summary = "Smoke test proposal"
    rationale = "Validate supersede logic"
    proposedPatchJson = "{}"
    contactId = $null
    actionNextId = $ActionNextId
}
$createResponse = Invoke-RestMethod -Uri "$BaseUrl/v1/projects/$ProjectId/proposals" -Method Post -Headers $headers -ContentType "application/json" -Body ($proposalPayload | ConvertTo-Json)
Assert-True ($createResponse.proposalId -gt 0) "Proposal creation failed."

$createResponse2 = Invoke-RestMethod -Uri "$BaseUrl/v1/projects/$ProjectId/proposals" -Method Post -Headers $headers -ContentType "application/json" -Body ($proposalPayload | ConvertTo-Json)
Assert-True ($createResponse2.proposalId -ne $createResponse.proposalId) "Expected new proposal id."

Write-Host "7) List proposals for action and verify new proposal exists..."
$proposals = Invoke-RestMethod -Uri "$BaseUrl/v1/actions/$ActionNextId/proposals" -Method Get -Headers $headers
$match = @($proposals | Where-Object { $_.proposalId -eq $createResponse2.proposalId })
Assert-True ($match.Count -eq 1) "New proposal not found in action proposals list."

Write-Host "8) List narratives updated in last 14 days for contact..."
$cutoffDate = (Get-Date).AddDays(-14).ToString("yyyy-MM-dd")
$narratives = Invoke-RestMethod -Uri "$BaseUrl/v1/narratives?contactId=$ContactId&lastUpdatedAfter=$cutoffDate" -Method Get -Headers $headers
Assert-True ($narratives.Count -gt 0) "No narratives returned for last 14 days."

Write-Host "All smoke checks passed."