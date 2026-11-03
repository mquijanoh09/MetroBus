# Test all 15 cases from test_cases.csv
$ErrorActionPreference = "Continue"

cd C:\Users\marti\MetroBus

Write-Host "`n=== Testing All 15 Cases from test_cases.csv ===`n"

# Read test cases
$cases = Import-Csv -Path "data\test_cases.csv"

# Create input file for each case
$results = @()

foreach ($case in $cases) {
    $origin = $case.origin
    $destination = $case.destination
    $expectedMin = $case.minutes
    $expectedCost = $case.cost_cop
    $expectedVehicles = $case.vehicles
    
    Write-Host "Test case: $origin -> $destination"
    Write-Host "Expected: ${expectedMin}min, ${expectedCost} COP, vehicles: $expectedVehicles"
    
    # Create input file
    $input = "$origin`n$destination`nq`n"
    $input | Out-File -FilePath "test_input.txt" -Encoding ASCII -NoNewline
    
    # Run the app
    try {
        $output = Get-Content "test_input.txt" | java -cp "target\transit-routing-1.0.0.jar" co.medellin.transit.Main 2>&1 | Out-String
        
        # Parse output for duration, cost, and vehicles
        if ($output -match "Duraci?n total:\s+(\d+)\s+minutos") {
            $actualMin = [int]$Matches[1]
        } else {
            $actualMin = "N/A"
        }
        
        if ($output -match "Costo total:\s+\$\s*([\d,]+)\s+COP") {
            $actualCost = $Matches[1] -replace ',',''
            $actualCost = [int]$actualCost
        } else {
            $actualCost = "N/A"
        }
        
        # Extract vehicles from output
        $vehicles = @()
        if ($output -match "(?s)Ruta\d+:.*?(?=\n\nDuraci?n|$)") {
            $routeSection = $Matches[0]
            if ($routeSection -match "bus:\s*(\S+)") {
                $vehicles += "bus:$($Matches[1])"
            }
            if ($routeSection -match "metro l?nea\s+(\w+)") {
                $vehicles += "metro:$($Matches[1])"
            }
            if ($routeSection -match "Metropl?s l?nea\s+(\w+)") {
                $vehicles += "brt:$($Matches[1])"
            }
            if ($routeSection -match "metrocable l?nea\s+(\w+)") {
                $vehicles += "cable_car:$($Matches[1])"
            }
        }
        
        if ($vehicles.Count -eq 0 -and $output -match "No se encontr? una ruta") {
            $actualVehicles = "NO_ROUTE"
        } elseif ($vehicles.Count -eq 0) {
            $actualVehicles = "walk"
        } else {
            $actualVehicles = $vehicles -join " > "
        }
        
        Write-Host "Actual:   ${actualMin}min, ${actualCost} COP, vehicles: $actualVehicles"
        
        $results += [PSCustomObject]@{
            Origin = $origin
            Destination = $destination
            ExpectedMin = $expectedMin
            ActualMin = $actualMin
            ExpectedCost = $expectedCost
            ActualCost = $actualCost
            ExpectedVehicles = $expectedVehicles
            ActualVehicles = $actualVehicles
            Match = ($actualMin -eq $expectedMin -and $actualCost -eq $expectedCost)
        }
    }
    catch {
        Write-Host "ERROR: $_"
        $results += [PSCustomObject]@{
            Origin = $origin
            Destination = $destination
            ExpectedMin = $expectedMin
            ActualMin = "ERROR"
            ExpectedCost = $expectedCost
            ActualCost = "ERROR"
            ExpectedVehicles = $expectedVehicles
            ActualVehicles = "ERROR"
            Match = $false
        }
    }
    
    Write-Host ""
}

# Clean up
Remove-Item "test_input.txt" -ErrorAction SilentlyContinue

# Summary
Write-Host "`n=== SUMMARY ===`n"
$results | Format-Table -AutoSize
Write-Host "`nPassing: $($results | Where-Object { $_.Match } | Measure-Object | Select-Object -ExpandProperty Count) / $($results.Count)"
