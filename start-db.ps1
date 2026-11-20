param(
    [ValidateRange(1, 65535)]
    [int]$Port = 5432
)

$ErrorActionPreference = 'Stop'
$shareitRoot = Join-Path $PSScriptRoot '.local-postgres'
$shareitInstall = Join-Path $shareitRoot 'pgsql'
$shareitData = Join-Path $shareitRoot 'data'
$shareitDriver = Join-Path $shareitRoot 'postgresql-42.7.3.jar'
$shareitBootstrap = Join-Path $shareitRoot 'ShareItDatabase.java'

$shareitJavaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
$shareitJava = if ($shareitJavaCommand) { $shareitJavaCommand.Source } else { $null }
if (-not $shareitJava -and $env:JAVA_HOME) {
    $shareitCandidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path -LiteralPath $shareitCandidate) {
        $shareitJava = $shareitCandidate
    }
}
if (-not $shareitJava) {
    $shareitJava = Get-ChildItem (Join-Path $env:USERPROFILE '.jdks\*\bin\java.exe') -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $shareitJava) {
    throw 'Java was not found. Install JDK 21 or set JAVA_HOME to the JDK used by IntelliJ IDEA.'
}

[System.IO.Directory]::CreateDirectory($shareitRoot) | Out-Null
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if (-not (Test-Path -LiteralPath $shareitDriver)) {
    Write-Output 'Downloading the PostgreSQL JDBC driver...'
    Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar' -OutFile ($shareitDriver + '.part')
    Move-Item -LiteralPath ($shareitDriver + '.part') -Destination $shareitDriver
}
$shareitJavaSource = @'
import java.sql.DriverManager;

class ShareItDatabase {
    public static void main(String[] args) throws Exception {
        DriverManager.setLoginTimeout(5);
        try (var connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:" + Integer.parseInt(args[0]) + "/postgres", "postgres", "postgres");
             var statement = connection.createStatement()) {
            boolean exists;
            try (var result = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = 'shareit'")) {
                exists = result.next();
            }
            if (!exists) {
                statement.executeUpdate("CREATE DATABASE shareit");
            }
        }
        System.out.println("Database shareit is ready on localhost:" + args[0]);
    }
}
'@
[System.IO.File]::WriteAllText($shareitBootstrap, $shareitJavaSource, (New-Object System.Text.UTF8Encoding($false)))

if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    & $shareitJava --class-path $shareitDriver $shareitBootstrap $Port
    if ($LASTEXITCODE -ne 0) {
        throw "Port $Port is already in use, but PostgreSQL did not accept the project's postgres credentials. Check the existing server."
    }
    exit 0
}

$shareitControl = Join-Path $shareitInstall 'bin\pg_ctl.exe'
if (-not (Test-Path -LiteralPath (Join-Path $shareitInstall 'ready'))) {
    if (-not (Get-Command tar.exe -ErrorAction SilentlyContinue)) {
        throw 'tar.exe was not found. This script requires Windows 10 or newer.'
    }
    Write-Output 'Downloading portable PostgreSQL (about 25 MB)...'
    $shareitArchive = Join-Path $shareitRoot 'postgresql-windows.jar'
    Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/18.6.0/embedded-postgres-binaries-windows-amd64-18.6.0.jar' -OutFile ($shareitArchive + '.part')
    Move-Item -LiteralPath ($shareitArchive + '.part') -Destination $shareitArchive -Force
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $shareitZip = [System.IO.Compression.ZipFile]::OpenRead($shareitArchive)
    try {
        $shareitEntry = $shareitZip.GetEntry('postgres-windows-x86_64.txz')
        $shareitTar = Join-Path $shareitRoot 'postgres.txz'
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($shareitEntry, $shareitTar, $true)
    } finally {
        $shareitZip.Dispose()
    }
    [System.IO.Directory]::CreateDirectory($shareitInstall) | Out-Null
    & tar.exe -xf $shareitTar -C $shareitInstall
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not extract PostgreSQL.'
    }
    [System.IO.File]::WriteAllText((Join-Path $shareitInstall 'ready'), '18.6.0')
}

if (-not (Test-Path -LiteralPath (Join-Path $shareitData 'PG_VERSION'))) {
    $shareitPasswordFile = Join-Path $shareitRoot 'initial-password.tmp'
    try {
        [System.IO.File]::WriteAllText($shareitPasswordFile, 'postgres')
        & (Join-Path $shareitInstall 'bin\initdb.exe') -D $shareitData -U postgres -A scram-sha-256 --encoding=UTF8 --locale-provider=icu --icu-locale=und --pwfile=$shareitPasswordFile --no-instructions
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not initialize PostgreSQL. Existing data has not been deleted.'
        }
    } finally {
        Remove-Item -LiteralPath $shareitPasswordFile -ErrorAction SilentlyContinue
    }
}

$shareitArguments = @('-D', ('"' + $shareitData + '"'), '-l', ('"' + (Join-Path $shareitRoot 'server.log') + '"'), '-o', ('"-p ' + $Port + ' -h localhost"'), '-w', 'start')
$shareitProcess = Start-Process -FilePath $shareitControl -ArgumentList $shareitArguments -WindowStyle Hidden -PassThru
$shareitProcess.WaitForExit()
if ($shareitProcess.ExitCode -ne 0) {
    throw ('PostgreSQL could not start. See ' + (Join-Path $shareitRoot 'server.log'))
}

& $shareitJava --class-path $shareitDriver $shareitBootstrap $Port
if ($LASTEXITCODE -ne 0) {
    throw 'PostgreSQL started, but database creation failed. See the error above.'
}
exit 0
