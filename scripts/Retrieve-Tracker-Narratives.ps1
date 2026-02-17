param(
    # Set PROJECT_TRACKER_API_KEY in your environment or pass -ApiKey explicitly.
    [string]$BaseUrl = "http://localhost:8080/tracker/api",
    [string]$ApiKey = $env:PROJECT_TRACKER_API_KEY,
    [int]$ContactId = 66747
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "ApiKey is required. Set -ApiKey or $env:PROJECT_TRACKER_API_KEY."
}
if ($ContactId -le 0) {
    throw "ContactId must be provided and > 0."
}

$headers = @{
    "X-Api-Key" = $ApiKey
}

$cutoffDate = (Get-Date).AddDays(-14).ToString("yyyy-MM-dd")
$endpoint = "$BaseUrl/v1/tracker-narratives?contactId=$ContactId&lastUpdatedAfter=$cutoffDate"

Write-Host "Querying tracker narratives updated since $cutoffDate..."
$trackerNarratives = Invoke-RestMethod -Uri $endpoint -Method Get -Headers $headers

if ($null -eq $trackerNarratives -or $trackerNarratives.Count -eq 0) {
    Write-Host "No tracker narratives returned."
    return
}

$first = $trackerNarratives | Select-Object -First 1

Write-Host "Review status values: GENERATING, GENERATED, APPROVED, REJECTED, DELETED"
Write-Host "Narrative types: DAILY, WEEKLY, MONTHLY"
Write-Host "First tracker narrative:" 
Write-Host "  narrativeId: $($first.narrativeId)"
Write-Host "  projectId: $($first.projectId)"
Write-Host "  contactId: $($first.contactId)"
Write-Host "  displayTitle: $($first.displayTitle)"
Write-Host "  narrativeType: $($first.narrativeType)"
Write-Host "  periodStart: $($first.periodStart)"
Write-Host "  periodEnd: $($first.periodEnd)"
Write-Host "  reviewStatus: $($first.reviewStatus)"
Write-Host "  dateGenerated: $($first.dateGenerated)"
Write-Host "  dateApproved: $($first.dateApproved)"
Write-Host "  promptVersion: $($first.promptVersion)"
Write-Host "  modelName: $($first.modelName)"
Write-Host "  lastUpdated: $($first.lastUpdated)"
Write-Host "  markdownFinal:"
Write-Host "" 
Write-Host $first.markdownFinal
