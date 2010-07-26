#!/bin/bash

#ruby generate_images.rb
#for i in res/drawable/b[0-9]*.png; do mogrify -resize 25x25 $i; done
#gimp res/drawable/b100.png
#for i in res/drawable/*.png; do mogrify -sharpen 25x25 $i; done

# shift ("roll") images 1px to the right
# for i in numbers/*.png; do convert $i -roll +1+0 $i; done

d=`dirname $(readlink -f $0)`

cd $d/numbers-hdpi
for i in [0-9]*.png;      do cp $i ../../res/drawable-hdpi/b$i; done
for i in 0[0-2][0-9].png; do cp $i ../../res/drawable-hdpi/r$i; done
for i in 0[1-4][0-9].png; do cp $i ../../res/drawable-hdpi/a$i; done
for i in 0[3-9][0-9].png; do cp $i ../../res/drawable-hdpi/g$i; done
cp 100.png ../../res/drawable-hdpi/g100.png

#for i in res/drawable/b[0-9]*.png; do composite $i battery-outline24.png $i; done
#for i in res/drawable/r[0-9]*.png; do composite $i battery-outline24-red04.png $i; done
#for i in res/drawable/g[0-9]*.png; do composite $i battery-outline24-green09.png $i; done

cd $d
for i in ../res/drawable-hdpi/b[0-9]*.png; do composite $i battery-outline24-hires09.png $i; done
for i in ../res/drawable-hdpi/r[0-9]*.png; do composite $i battery-outline24-hires09-red01.png $i; done
for i in ../res/drawable-hdpi/a[0-9]*.png; do composite $i battery-outline24-hires09-amber01.png $i; done
#for i in ../res/drawable-hdpi/g[0-9]*.png; do composite $i battery-outline24-hires09-green01.png $i; done
for i in ../res/drawable-hdpi/g[0-9]*.png; do composite $i battery-outline-hires-green-0101.png $i; done


cd $d/numbers-mdpi
for i in [0-9]*.png;      do cp $i ../../res/drawable-mdpi/b$i; done
for i in 0[0-2][0-9].png; do cp $i ../../res/drawable-mdpi/r$i; done
for i in 0[1-4][0-9].png; do cp $i ../../res/drawable-mdpi/a$i; done
for i in 0[3-9][0-9].png; do cp $i ../../res/drawable-mdpi/g$i; done
cp 100.png ../../res/drawable-mdpi/g100.png

cd $d
for i in ../res/drawable-mdpi/b[0-9]*.png; do composite $i battery-outline24.png $i; done
for i in ../res/drawable-mdpi/r[0-9]*.png; do composite $i battery-outline24-red04.png $i; done
for i in ../res/drawable-mdpi/a[0-9]*.png; do composite $i battery-outline24-amber01.png $i; done
#for i in ../res/drawable-mdpi/g[0-9]*.png; do composite $i battery-outline24-green09.png $i; done
for i in ../res/drawable-mdpi/g[0-9]*.png; do composite $i battery-outline-mres-green-0101.png $i; done
