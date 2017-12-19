	

echo should already have original image in folder, as well as folders named tiles and samples


basename=0

filename=0.png
extension=jpg
imagemagick=convert
tilesize=256
samplesize=500

tilesfolder=plan

mkdir samples
samplesfolder=samples

echo create tile folders
mkdir $tilesfolder/$basename
mkdir $tilesfolder/$basename/1000
mkdir $tilesfolder/$basename/500
mkdir $tilesfolder/$basename/250
mkdir $tilesfolder/$basename/125

echo "create half-sized versions "
convert $filename -resize 50%  $samplesfolder/$basename-500.$extension
convert $filename -resize 25%  $samplesfolder/$basename-250.$extension
convert $filename -resize 12.5% $samplesfolder/$basename-125.$extension


echo create sample
$imagemagick $filename -thumbnail "$samplesize"x"$samplesize"  ./$samplesfolder/$basename.$extension


echo create tiles

$imagemagick -limit map 0 -limit memory 0 $filename -crop "$tilesize"x"$tilesize" -set filename:tile "%[fx:page.x/$tilesize]_%[fx:page.y/$tilesize]" +repage +adjoin "$tilesfolder/$basename/1000/$basename_%[filename:tile].$extension"

$imagemagick -limit map 0 -limit memory 0 $samplesfolder/$basename-500.$extension -crop "$tilesize"x"$tilesize" -set filename:tile "%[fx:page.x/$tilesize]_%[fx:page.y/$tilesize]" +repage +adjoin "$tilesfolder/$basename/500/$basename_%[filename:tile].$extension"

$imagemagick -limit map 0 -limit memory 0 $samplesfolder/$basename-250.$extension -crop "$tilesize"x"$tilesize" -set filename:tile "%[fx:page.x/$tilesize]_%[fx:page.y/$tilesize]" +repage +adjoin "$tilesfolder/$basename/250/$basename_%[filename:tile].$extension"

$imagemagick -limit map 0 -limit memory 0 $samplesfolder/$basename-125.$extension -crop "$tilesize"x"$tilesize" -set filename:tile "%[fx:page.x/$tilesize]_%[fx:page.y/$tilesize]" +repage +adjoin "$tilesfolder/$basename/125/$basename_%[filename:tile].$extension"

echo cleanup
rm $samplesfolder/$basename.$extension
rm $samplesfolder/$basename-500.$extension
rm $samplesfolder/$basename-250.$extension
rm $samplesfolder/$basename-125.$extension
rmdir $samplesfolder
echo DONE
