$requiredVariables = @(
    'DB_URL',
    'DB_USER',
    'DB_PASSWORD',
    'APP_TRACKING_CODE_PEPPER'
)
$missingVariables = @(
    $requiredVariables | Where-Object {
        [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
    }
)

if ($missingVariables.Count -gt 0) {
    throw "Faltan variables locales requeridas: $($missingVariables -join ', '). Configúralas en esta terminal antes de ejecutar el script."
}

$trackingPepper = [Environment]::GetEnvironmentVariable('APP_TRACKING_CODE_PEPPER', 'Process')
if ([System.Text.Encoding]::UTF8.GetByteCount($trackingPepper) -lt 32) {
    throw 'APP_TRACKING_CODE_PEPPER debe tener al menos 32 bytes y conservar el mismo valor usado por la base de datos.'
}

$secureKey = Read-Host 'Ingresa la nueva Gemini API key' -AsSecureString
if ($secureKey.Length -eq 0) {
    throw 'La Gemini API key es obligatoria para iniciar Gemini.'
}

$apiKey = [System.Net.NetworkCredential]::new('', $secureKey).Password
$configuredVariables = @(
    'GEMINI_API_KEY',
    'APP_CHAT_GEMINI_ENABLED',
    'APP_CHAT_GEMINI_MODEL',
    'APP_PRIVACY_POLICY_VERSION',
    'SPRING_PROFILES_ACTIVE',
    'APP_CAPTCHA_ENABLED'
)
$previousValues = @{}
foreach ($name in $configuredVariables) {
    $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $env:GEMINI_API_KEY = $apiKey
    $env:APP_CHAT_GEMINI_ENABLED = 'true'
    $env:APP_CHAT_GEMINI_MODEL = 'gemini-3.6-flash'
    $env:APP_PRIVACY_POLICY_VERSION = '1.1'
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:APP_CAPTCHA_ENABLED = 'false'

    & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "El backend terminó con código $LASTEXITCODE."
    }
} finally {
    foreach ($name in $configuredVariables) {
        if ($null -eq $previousValues[$name]) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$name" $previousValues[$name]
        }
    }
    $apiKey = $null
    $secureKey.Dispose()
}
