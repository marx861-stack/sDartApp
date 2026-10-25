# Script to build standalone Windows Setup Installer (.exe) with embedded JRE via Inno Setup
$ErrorActionPreference = "Stop"

$RootDir = (Get-Item (Join-Path $PSScriptRoot "..")).FullName
$DistDir = Join-Path $RootDir "dist"
$TempDir = Join-Path $RootDir "temp"
$AppVersion = "1.0.0"

$IsccDefault = Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe"
if (Test-Path $IsccDefault) {
    $IsccExe = $IsccDefault
} elseif (Get-Command "ISCC.exe" -ErrorAction SilentlyContinue) {
    $IsccExe = "ISCC.exe"
} elseif (Test-Path "C:\Program Files (x86)\Inno Setup 6\ISCC.exe") {
    $IsccExe = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
} else {
    Write-Error "Inno Setup Compiler (ISCC.exe) nicht gefunden!"
    exit 1
}

$JbrDir = if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
    $env:JAVA_HOME
} elseif (Test-Path "C:\Program Files\Android\Android Studio\jbr") {
    "C:\Program Files\Android\Android Studio\jbr"
} else {
    Write-Error "Java Runtime (JBR / JAVA_HOME) nicht gefunden!"
    exit 1
}

$CscExe = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $CscExe)) {
    Write-Error "C# Compiler '$CscExe' nicht gefunden!"
    exit 1
}

$IconIco = Join-Path $RootDir "composeApp\src\desktopMain\resources\icon.ico"
if (-not (Test-Path $IconIco)) {
    Write-Error "Windows-Icon nicht gefunden: $IconIco"
    exit 1
}

$JarFile = Join-Path $DistDir "sDartApp-windows-x64-$AppVersion.jar"
Write-Host "Baue aktuelle Desktop Standalone JAR via Gradle..."
$env:JAVA_HOME = $JbrDir
$gradlew = Join-Path $RootDir "gradlew.bat"
# --no-daemon: ein Hintergrund-Daemon hielte die Konsolenausgabe offen und das Skript würde nie enden
& $gradlew :composeApp:packageUberJarForCurrentOS --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle-Build fehlgeschlagen (Exit-Code $LASTEXITCODE)!"
    exit 1
}
if (-not (Test-Path $JarFile)) {
    Write-Error "Erstellung von '$JarFile' fehlgeschlagen!"
    exit 1
}

if (-not (Test-Path $TempDir)) {
    New-Item -ItemType Directory -Path $TempDir | Out-Null
}

Write-Host "Kompiliere native Start-Anwendung (sDartApp.exe)..."
$LauncherCs = Join-Path $TempDir "Launcher.cs"
$LauncherExe = Join-Path $TempDir "sDartApp.exe"

$csCode = @"
using System;
using System.Diagnostics;
using System.IO;

class Program
{
    static int Main(string[] args)
    {
        string baseDir = AppDomain.CurrentDomain.BaseDirectory;
        string javawPath = Path.Combine(baseDir, "runtime", "bin", "javaw.exe");
        string jarPath = Path.Combine(baseDir, "app", "sDartApp.jar");

        if (!File.Exists(javawPath))
        {
            javawPath = Path.Combine(baseDir, "bin", "javaw.exe");
        }
        if (!File.Exists(jarPath))
        {
            jarPath = Path.Combine(baseDir, "sDartApp.jar");
        }

        if (!File.Exists(javawPath) || !File.Exists(jarPath))
        {
            System.Windows.Forms.MessageBox.Show(
                "Fehler beim Starten der s'Dart App: Benötigte Dateien wurden nicht gefunden.",
                "s'Dart App",
                System.Windows.Forms.MessageBoxButtons.OK,
                System.Windows.Forms.MessageBoxIcon.Error
            );
            return 1;
        }

        ProcessStartInfo psi = new ProcessStartInfo();
        psi.FileName = javawPath;
        psi.Arguments = "-jar \"" + jarPath + "\"";
        psi.WorkingDirectory = baseDir;
        psi.UseShellExecute = false;

        Process.Start(psi);
        return 0;
    }
}
"@

Set-Content -Path $LauncherCs -Value $csCode -Encoding UTF8
& $CscExe /target:winexe /r:System.Windows.Forms.dll /win32icon:"$IconIco" /out:"$LauncherExe" "$LauncherCs"

if (-not (Test-Path $LauncherExe)) {
    Write-Error "Kompilierung von sDartApp.exe fehlgeschlagen!"
    exit 1
}

Write-Host "Erstelle Inno Setup Skript..."
$IssFile = Join-Path $TempDir "setup.iss"
$issContent = @"
#define MyAppName "s'Dart App"
#define MyAppVersion "$AppVersion"
#define MyAppPublisher "s'Dart"
#define MyAppExeName "sDartApp.exe"

[Setup]
AppId={{C78DF2E3-6789-4B52-8CEB-1E5A96D4D991}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\sDartApp
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=commandline
OutputDir=$DistDir
OutputBaseFilename=sDartApp-windows-x64-$AppVersion-Setup
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
SetupIconFile=$IconIco
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesAssociations=yes
ChangesEnvironment=yes

[Languages]
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "$LauncherExe"; DestDir: "{app}"; Flags: ignoreversion
Source: "$JarFile"; DestDir: "{app}\app"; DestName: "sDartApp.jar"; Flags: ignoreversion
Source: "$JbrDir\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "$IconIco"; DestDir: "{app}"; DestName: "icon.ico"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
"@

Set-Content -Path $IssFile -Value $issContent -Encoding UTF8

Write-Host "Baue Windows Setup Installer (.exe) mit Inno Setup..."
& $IsccExe "$IssFile"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Inno Setup fehlgeschlagen (Exit-Code $LASTEXITCODE)!"
    exit 1
}

$SetupExe = Join-Path $DistDir "sDartApp-windows-x64-$AppVersion-Setup.exe"
if (-not (Test-Path $SetupExe)) {
    Write-Error "Installer konnte nicht in '$SetupExe' erstellt werden!"
    exit 1
}

# Aufräumen von temp
Remove-Item -Force -ErrorAction SilentlyContinue $LauncherCs, $LauncherExe, $IssFile

# Windows Shell Icon-Cache sofort aktualisieren (kein Neustart erforderlich)
Write-Host "Aktualisiere Windows Shell Icon-Cache..."
try {
    $notifyCode = @"
    using System;
    using System.Runtime.InteropServices;
    public class ShellIconNotify {
        [DllImport("shell32.dll")]
        public static extern void SHChangeNotify(int wEventId, int uFlags, IntPtr dwItem1, IntPtr dwItem2);
    }
"@
    Add-Type -TypeDefinition $notifyCode -ErrorAction SilentlyContinue
    [ShellIconNotify]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero)
} catch {}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "ERFOLG: Windows Setup Installer (.exe) wurde erstellt:" -ForegroundColor Green
Write-Host "$SetupExe" -ForegroundColor Cyan
Write-Host "Größe: $([math]::Round((Get-Item $SetupExe).Length / 1MB, 2)) MB" -ForegroundColor Green
Write-Host "========================================================`n" -ForegroundColor Green
