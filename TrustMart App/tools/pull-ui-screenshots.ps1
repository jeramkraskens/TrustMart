param([ValidatePattern('^[A-Za-z0-9_.:-]+$')][string]$DeviceId)
$ErrorActionPreference = 'Stop'
$adbPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$outputDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../app/build/reports/ui-screenshots'))
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
# Binary transfer of generated test PNGs only; never pull credentials or other app files.
$names = & $adbPath -s $DeviceId shell run-as com.trustmart.app ls files/ui-screenshots
if ($LASTEXITCODE -ne 0) { throw 'Cannot list screen-test captures.' }
foreach ($entry in $names) {
    $fileName = $entry.Trim()
    if ($fileName -notmatch '^[a-z0-9-]+\.png$') { continue }
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $adbPath
    $start.Arguments = "-s $DeviceId exec-out run-as com.trustmart.app cat files/ui-screenshots/$fileName"
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $process = [Diagnostics.Process]::Start($start)
    $destination = [IO.File]::Create((Join-Path $outputDirectory $fileName))
    try { $process.StandardOutput.BaseStream.CopyTo($destination) } finally { $destination.Dispose() }
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "Transfer failed: $fileName" }
    $process.Dispose()
    Write-Output $fileName
}
