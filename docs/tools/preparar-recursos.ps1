$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$res = Join-Path $raiz 'app/src/main/res'
$backup = Join-Path $raiz 'docs/referencias-ui/originales-recursos'
New-Item -ItemType Directory -Force $backup | Out-Null
function GuardarImagen($source, $dest, [int]$width, [int]$height, [int]$art = 0) {
    $bmp = New-Object System.Drawing.Bitmap($width,$height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    if ($art -gt 0) { $g.DrawImage($source,[int](($width-$art)/2),[int](($height-$art)/2),$art,$art) }
    else { $g.DrawImage($source,0,0,$width,$height) }
    $bmp.Save($dest,[System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
}
$rutas=@('layout/fragment_tienda.xml','layout/item_comodin_tienda.xml','layout/item_carril_tienda.xml') | ForEach-Object {Join-Path $res $_}
$rutas += @('Fragments/TiendaFragment.java','Adapters/ComodinTiendaAdapter.java') | ForEach-Object {Join-Path $raiz "app/src/main/java/com/example/basta_game/$_"}
$texto=($rutas | ForEach-Object {Get-Content -LiteralPath $_ -Raw}) -join "`n"
$nombres=[regex]::Matches($texto,'(?:@drawable/|R\.drawable\.)(\w+)') | ForEach-Object {$_.Groups[1].Value} | Sort-Object -Unique
New-Item -ItemType Directory -Force (Join-Path $res 'drawable-nodpi') | Out-Null
foreach($nombre in $nombres) {
    $src=Join-Path $res "drawable/$nombre.png"
    if (!(Test-Path -LiteralPath $src)) {$src=Join-Path $backup "$nombre.png"}
    if (!(Test-Path -LiteralPath $src)) {continue}
    $image=[System.Drawing.Image]::FromFile($src)
    if($image.Width -le 384 -and $image.Height -le 768) {$image.Dispose();continue}
    $limite=if($nombre -like 'bg_fondo*'){960}elseif($nombre -like 'bg_*'){768}else{256}
    $scale=[math]::Min(1.0,$limite/[math]::Max($image.Width,$image.Height))
    GuardarImagen $image (Join-Path $res "drawable-nodpi/$nombre.png") ([int]($image.Width*$scale)) ([int]($image.Height*$scale))
    $image.Dispose()
    # Preserve source artwork outside packaged resources; only explicit non-recursive file moves.
    if($src -ne (Join-Path $backup "$nombre.png")) {Move-Item -LiteralPath $src -Destination (Join-Path $backup "$nombre.png")}
}
$icon=[System.Drawing.Image]::FromFile((Join-Path $raiz 'docs/referencias-ui/basta_online_icon.png'))
$densidades=@{mdpi=1;hdpi=1.5;xhdpi=2;xxhdpi=3;xxxhdpi=4}
foreach($nombre in $densidades.Keys) {
    $dir=Join-Path $res "mipmap-$nombre"; $factor=$densidades[$nombre]
    foreach($base in @('ic_launcher','ic_launcher_round')) {
        $old=Join-Path $dir "$base.webp"
        if(Test-Path -LiteralPath $old){Move-Item -LiteralPath $old -Destination (Join-Path $backup "$base-$nombre.webp")}
        $size=[int](48*$factor)
        GuardarImagen $icon (Join-Path $dir "$base.png") $size $size ([int]($size*0.66))
    }
    GuardarImagen $icon (Join-Path $dir 'ic_launcher_foreground.png') ([int](108*$factor)) ([int](108*$factor)) ([int](46*$factor))
}
$icon.Dispose()
