$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$raiz=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$source=[Drawing.Bitmap]::new((Join-Path $raiz 'docs/referencias-ui/basta_online_icon.png'))
try {
    foreach($entrada in @{mdpi=1;hdpi=1.5;xhdpi=2;xxhdpi=3;xxxhdpi=4}.GetEnumerator()) {
        $dir=Join-Path $raiz ('app/src/main/res/mipmap-'+$entrada.Key)
        foreach($tipo in @('ic_launcher','ic_launcher_round','ic_launcher_foreground')) {
            $adaptativo=$tipo -eq 'ic_launcher_foreground'
            $lado=[int]($(if($adaptativo){108}else{48})*$entrada.Value)
            $arte=[int]($(if($adaptativo){46}elseif($tipo -eq 'ic_launcher_round'){32}else{44})*$entrada.Value)
            $destino=[Drawing.Bitmap]::new($lado,$lado)
            $g=[Drawing.Graphics]::FromImage($destino)
            try {
                $g.Clear($(if($adaptativo){[Drawing.Color]::Transparent}else{[Drawing.Color]::FromArgb(7,61,235)}))
                $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $g.DrawImage($source,[int](($lado-$arte)/2),[int](($lado-$arte)/2),$arte,$arte)
                $destino.Save((Join-Path $dir ($tipo+'.png')),[Drawing.Imaging.ImageFormat]::Png)
            } finally {$g.Dispose();$destino.Dispose()}
        }
    }
} finally {$source.Dispose()}
