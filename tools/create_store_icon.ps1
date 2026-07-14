Add-Type -AssemblyName System.Drawing

$outputDirectory = Join-Path $PSScriptRoot '..\assets'
$outputPath = Join-Path $outputDirectory 'hamdel-myket-icon.png'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$size = 512
$bitmap = [System.Drawing.Bitmap]::new($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(15, 118, 110))

# A quiet outer ring makes the mark remain recognizable at small store-icon sizes.
$ringPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(35, 216, 199), 10)
$graphics.DrawEllipse($ringPen, 40, 40, 432, 432)

$heart = [System.Drawing.Drawing2D.GraphicsPath]::new()
$heart.AddBezier(256, 416, 90, 316, 104, 135, 213, 144)
$heart.AddBezier(213, 144, 243, 146, 256, 177, 256, 198)
$heart.AddBezier(256, 198, 269, 177, 282, 146, 312, 144)
$heart.AddBezier(312, 144, 421, 135, 422, 316, 256, 416)
$heart.CloseFigure()
$heartBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$graphics.FillPath($heartBrush, $heart)

$connectionPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(20, 184, 166), 26)
$connectionPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$connectionPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$connectionPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$graphics.DrawLines($connectionPen, [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(165, 260),
    [System.Drawing.Point]::new(218, 260),
    [System.Drawing.Point]::new(256, 214),
    [System.Drawing.Point]::new(300, 314),
    [System.Drawing.Point]::new(347, 260)
))

$graphics.Dispose()
$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output $outputPath
