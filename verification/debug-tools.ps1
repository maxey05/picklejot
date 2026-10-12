$Package = "com.maxeydev.picklejot"
$Uri = "content://$Package.debugtools"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    $env:PATH += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
}

function Invoke-PickleDebug {
    param(
        [Parameter(Mandatory)][string]$Method,
        [string]$Arg = "",
        [string[]]$Extra = @()
    )
    $arguments = @("shell", "content", "call", "--uri", $Uri, "--method", $Method)
    if ($Arg) {
        $arguments += @("--arg", $Arg)
    }
    foreach ($item in $Extra) {
        $arguments += @("--extra", $item)
    }
    & adb @arguments
}

function Reset-PickleApp {
    & adb shell pm clear $Package
}

function Start-PickleApp {
    & adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
}

function Stop-PickleApp {
    & adb shell am force-stop $Package
}

function Get-PickleStatus {
    Invoke-PickleDebug -Method status
}

function Set-PicklePro {
    param(
        [Parameter(Mandatory)][ValidateSet("on", "off")][string]$State,
        [int]$DaysAgo = 0
    )
    Invoke-PickleDebug -Method set_pro -Arg $State -Extra @("days_ago:i:$DaysAgo")
}

function Add-PickleMatches {
    param(
        [Parameter(Mandatory)][int]$Count,
        [int]$DaysAgo = 400
    )
    Invoke-PickleDebug -Method seed_matches -Extra @("count:i:$Count", "days_ago:i:$DaysAgo")
}

function Add-PickleStreak {
    param(
        [Parameter(Mandatory)][int]$Weeks,
        [int]$EndOffset = 1,
        [string]$Gaps = ""
    )
    Invoke-PickleDebug -Method seed_streak -Extra @("weeks:i:$Weeks", "end_offset:i:$EndOffset", "gaps:s:$Gaps")
}

function Add-PickleMatchToday {
    param([int]$DaysAgo = 0)
    Invoke-PickleDebug -Method log_match -Extra @("days_ago:i:$DaysAgo")
}

function Clear-PickleMatches {
    Invoke-PickleDebug -Method clear_matches
}

function Complete-PickleOnboarding {
    Invoke-PickleDebug -Method finish_onboarding
}

function Send-PickleReminder {
    param([ValidateSet("plain", "skip")][string]$Kind = "plain")
    Invoke-PickleDebug -Method notify -Arg $Kind
}

function Reset-PickleState {
    Complete-PickleOnboarding | Out-Null
    Clear-PickleMatches | Out-Null
    Set-PicklePro -State off | Out-Null
}

function Start-PickleScenario {
    param(
        [Parameter(Mandatory)]
        [ValidateSet(
            "first-match", "cap-40", "cap-48", "cap-50",
            "milestone-4", "milestone-8", "milestone-12", "milestone-26", "milestone-52",
            "missed-skip", "skip-used", "pro-unlocked", "pro-themes", "export-prompt", "reminder"
        )]
        [string]$Name,
        [switch]$Reset
    )
    if ($Reset) {
        Reset-PickleApp
        Complete-PickleOnboarding | Out-Null
        Start-PickleApp
        Start-Sleep -Seconds 6
    }
    else {
        Reset-PickleState
        Start-PickleApp
    }
    switch -Wildcard ($Name) {
        "first-match" {
        }
        "cap-*" {
            Add-PickleMatches -Count ([int]($Name -replace "cap-", "")) | Out-Null
        }
        "milestone-*" {
            $target = [int]($Name -replace "milestone-", "")
            Add-PickleStreak -Weeks ($target - 1) -EndOffset 1 | Out-Null
            Start-Sleep -Seconds 3
            Add-PickleMatchToday | Out-Null
        }
        "missed-skip" {
            Add-PickleStreak -Weeks 3 -EndOffset 2 | Out-Null
        }
        "pro-unlocked" {
            Add-PickleStreak -Weeks 3 -EndOffset 2 | Out-Null
        }
        "skip-used" {
            Add-PickleStreak -Weeks 3 -EndOffset 2 | Out-Null
            Set-PicklePro -State on -DaysAgo 70 | Out-Null
        }
        "pro-themes" {
            Add-PickleStreak -Weeks 3 -EndOffset 1 | Out-Null
        }
        "export-prompt" {
            Add-PickleMatches -Count 26 | Out-Null
        }
        "reminder" {
            Add-PickleStreak -Weeks 3 -EndOffset 1 | Out-Null
        }
    }
    Get-PickleStatus
}
