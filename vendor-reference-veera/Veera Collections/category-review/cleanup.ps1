$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cleanupRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$cleanupPlan = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'cleanup-plan.json') -Raw | ConvertFrom-Json
if ($cleanupRoot.TrimEnd('\') -ne ([IO.Path]::GetFullPath($cleanupPlan.root)).TrimEnd('\')) { throw 'Workspace mismatch' }
$recoveryRoot = 'C:\Users\vishw\.codex\visualizations\2026\09\08\01a07fec-4636-7d31-9750-b3afe0d81d48'
$recoveryFolder = Join-Path $recoveryRoot ('veera-cleanup-recovery-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $recoveryFolder | Out-Null
$zipPath = Join-Path $recoveryFolder 'removed-project-files.zip'
foreach ($entry in $cleanupPlan.files) {
    $target = [IO.Path]::GetFullPath((Join-Path $cleanupRoot $entry.path))
    if (-not $target.StartsWith($cleanupRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe path' }
    if ((Get-Item -LiteralPath $target).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Unexpected link' }
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Changed file: $target" }
}
$zip = [IO.Compression.ZipFile]::Open($zipPath, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($entry in $cleanupPlan.files) {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $cleanupRoot $entry.path), $entry.path, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zip.Dispose() }
$zip = [IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    foreach ($entry in $cleanupPlan.files) {
        $stream = $zip.GetEntry($entry.path).Open()
        $hasher = [Security.Cryptography.SHA256]::Create()
        try { $digest = [Convert]::ToHexString($hasher.ComputeHash($stream)).ToLowerInvariant() }
        finally { $stream.Dispose(); $hasher.Dispose() }
        if ($digest -ne $entry.sha256) { throw "Archive verification failed: $($entry.path)" }
    }
} finally { $zip.Dispose() }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'cleanup-plan.json') -Destination (Join-Path $recoveryFolder 'cleanup-manifest.json')
foreach ($entry in $cleanupPlan.files) {
    $target = [IO.Path]::GetFullPath((Join-Path $cleanupRoot $entry.path))
    if (-not $target.StartsWith($cleanupRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe deletion target' }
    Remove-Item -LiteralPath $target
}
foreach ($directory in $cleanupPlan.removeDirectories) {
    $target = [IO.Path]::GetFullPath((Join-Path $cleanupRoot $directory))
    if (-not $target.StartsWith($cleanupRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe directory target' }
    if (Get-ChildItem -LiteralPath $target -Recurse -File) { throw 'Unexpected remaining file' }
    Remove-Item -LiteralPath $target -Recurse
}
foreach ($property in $cleanupPlan.preserveHashes.PSObject.Properties) {
    if ((Get-FileHash -LiteralPath (Join-Path $cleanupRoot $property.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $property.Value) { throw "Protected file changed: $($property.Name)" }
}
$result = @{removedFiles=$cleanupPlan.files.Count; removedBytes=$cleanupPlan.audit.removeBytes; archive=$zipPath; recoveryVerified=$true; runtimeFilesUnchanged=$true; localReferencesChecked=$cleanupPlan.audit.localReferencesChecked; missingStaticReferences=0}
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'cleanup-result.json')
$result | ConvertTo-Json
