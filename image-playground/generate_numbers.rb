require 'RMagick'

class NumberImageGenerator
  @@IMAGE_DIR = 'numbers-hdpi/'

  def generate(text, fs=18)
    #filename = "b" + sprintf("%03d", text) + ".png";
    filename = "" + sprintf("%03d", text) + ".png";
    font_size = fs;

    height = 35;
    width = 35;
    image = Magick::Image.new(width, height) {self.background_color = "transparent"}

    font = Magick::Draw.new();
    font.font_family = 'URW Chancery';
    #font.font_family = 'Helvetica';
    font.pointsize = font_size;
    font.gravity = Magick::CenterGravity;
    font.font_weight = 500;

    image.annotate(font, 0,0,0,0, text) {self.fill = 'black';};

    image.write(@@IMAGE_DIR + filename) {self.depth = 8;};
  end
end

ig = NumberImageGenerator.new;
for i in 0..99
  ig.generate(i.to_s);
end

ig.generate(100.to_s, 18);
