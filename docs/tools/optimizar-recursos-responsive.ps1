$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$res = Join-Path $raiz 'app/src/main/res'
$destino = Join-Path $res 'drawable-nodpi'
$originales = Join-Path $raiz 'docs/referencias-ui/originales-responsive'
New-Item -ItemType Directory -Force $destino,$originales | Out-Null
$informe = @()
# Only unqualified drawables: preserve previously optimized resources and every launcher density.
foreach ($archivo in Get-ChildItem -LiteralPath (Join-Path $res 'drawable') -Filter '*.png') {
    $fuente=$null; $bitmap=$null; $grafico=$null
    try {
        $fuente=[Drawing.Image]::FromFile($archivo.FullName)
        $nombre=$archivo.BaseName
        $limite=if($nombre -eq 'cuaderno'){768}elseif($nombre -like 'bg_*'){768}elseif($nombre -eq 'marco_rojo'){384}elseif($nombre -like 'letra_*'){384}elseif($nombre -in @('ic_piedra','ic_papel','ic_tijeras','ic_revolver','ic_interrogacion','ic_candados')){384}else{256}
        $escala=[Math]::Min(1.0,$limite/[Math]::Max($fuente.Width,$fuente.Height))
        $w=[Math]::Max(1,[int]($fuente.Width*$escala));$h=[Math]::Max(1,[int]($fuente.Height*$escala))
        $salida=Join-Path $destino $archivo.Name
        $copia=Join-Path $originales $archivo.Name
        if(Test-Path -LiteralPath $salida){throw "Destino existente, no sobrescribir: $salida"}
        if(Test-Path -LiteralPath $copia){throw "Original existente, no sobrescribir: $copia"}
        $bitmap=New-Object Drawing.Bitmap($w,$h)
        $grafico=[Drawing.Graphics]::FromImage($bitmap)
        $grafico.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $grafico.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $grafico.Clear([Drawing.Color]::Transparent);$grafico.DrawImage($fuente,0,0,$w,$h)
        $bitmap.Save($salida,[Drawing.Imaging.ImageFormat]::Png)
        $informe+=[pscustomobject]@{recurso=$nombre;anchoAntes=$fuente.Width;altoAntes=$fuente.Height;anchoDespues=$w;altoDespues=$h;bytesAntes=$archivo.Length;bytesDespues=(Get-Item -LiteralPath $salida).Length;argbAntes=[long]$fuente.Width*$fuente.Height*4;argbDespues=[long]$w*$h*4}
    } finally {
        if($grafico){$grafico.Dispose()};if($bitmap){$bitmap.Dispose()};if($fuente){$fuente.Dispose()}
    }
    Move-Item -LiteralPath $archivo.FullName -Destination $copia
}
if($informe.Count -eq 0){Write-Output 'No hay recursos sin optimizar; se conserva el informe anterior.';return}
$informe | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath (Join-Path $raiz 'docs/RECURSOS_OPTIMIZADOS.csv')
$informe | Measure-Object -Property argbAntes,argbDespues,bytesAntes,bytesDespues -Sum | Select-Object Property,Sum
