param(
    [string]$KeystorePath = "$PSScriptRoot\..\keystore\hamdel-release.jks",
    [string]$CredentialsPath = "$PSScriptRoot\..\release-signing.properties"
)

$properties = @{}
Get-Content $CredentialsPath | Where-Object { $_ -match '=' } | ForEach-Object {
    $key, $value = $_ -split '=', 2
    $properties[$key] = $value
}

$keytoolCommand = Get-Command keytool -ErrorAction SilentlyContinue
$keytool = if ($keytoolCommand) { $keytoolCommand.Source } else { 'C:\Program Files\Java\jre1.8.0_251\bin\keytool.exe' }
if (-not (Test-Path $keytool)) {
    throw "keytool was not found. Install a JDK or add keytool to PATH."
}
& $keytool -list -v -keystore $KeystorePath -storepass $properties.RELEASE_STORE_PASSWORD -alias $properties.RELEASE_KEY_ALIAS |
    Select-String 'SHA1:|SHA256:'

Write-Output "`nANDROID_KEYSTORE_BASE64:"
Write-Output ([Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $KeystorePath))))
