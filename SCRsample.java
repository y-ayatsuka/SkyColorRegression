//
// A Sample Code of Sky Color Regression
// by Yuji Ayatsuka, 2023/01
//

import java.awt.*;
import java.awt.image.*;

public class SCRsample {
  BufferedImage base_image;
  BufferedImage target;
  int width;
  int height;
  int target_width;
  int target_height;

  int count;

  float r_ave;
  float g_ave;
  float b_ave;

  float br_regression;
  float rg_regression;
  float gb_regression;

  int shift_br;

  // magnification ratios
  final static float br_ratio = 2.5f;
  final static float rg_ratio = 1.5f;
  final static float gb_ratio = 1.0f;

  // thresholds to ignore for processing
  final static int dark_threshold = 50;
  final static int bright_threshold = 240;

  // - - - - - - - - - - - - - - - -
  static BufferedImage SkyColorRegression(BufferedImage img) {
    SCRsample scr = new SCRsample(img);
    return scr.proc();
  }

  // - - - - - - - - - - - - - - - -
  SCRsample(BufferedImage img) {
    base_image = img;
    target = base_image;
    width = base_image.getWidth(null);
    height = base_image.getHeight(null);

    count = 0;

    r_ave = 0.0f;
    g_ave = 0.0f;
    b_ave = 0.0f;

    br_regression = 0.0f;
    rg_regression = 0.0f;
    gb_regression = 0.0f;

    shift_br = 0;
  }

  // - - - - - - - - - - - - - - - -
  BufferedImage proc() {
    int x, y;
    int i, j;
    int pixel, red, green, blue;
    int gray;

    int size = (width > height) ? width : height;
    int block = size / 180;
    int window_size = 31;

    if (block < 1)
      block = 1;

    final int small_width = width / block;
    final int small_height = height / block;

    if (block > 1) { // creating a small image
      target = new BufferedImage(small_width, small_height, 
				 BufferedImage.TYPE_INT_RGB);
      Graphics2D target_g = (Graphics2D)target.getGraphics();

      target_g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      target_g.drawImage(base_image, 0, 0, small_width, small_height, null);
    }

    target_width = target.getWidth(null);
    target_height = target.getHeight(null);

    BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics dst_g = dst.getGraphics();
    dst_g.drawImage(base_image, 0, 0, null);

    int steps = 0;

    for (y = 0; y < height; y += block) {
      if ((y / 64) > steps) {
	steps++;
	System.err.print(".");
      }

      for (x = 0; x < width; x += block) {
	setupWithArea(x / block - window_size, y / block - window_size, 
		      x / block + window_size, y / block + window_size);

	for (j = 0; j < block; j++) {
	  if (y + j >= height)
	    break;

	  for (i = 0; i < block; i++) {
	    if (x + i >= width)
	      break;

	    pixel = base_image.getRGB(x + i, y + j);
	    red = (pixel >> 16) & 0xFF;
	    green = (pixel >> 8) & 0xFF;
	    blue = pixel & 0xFF;
	    gray = (int)getMixed(red, green, blue);

	    if (gray < 0)
	      gray = 0;
	    else if (gray > 255)
	      gray = 255;

	    dst.setRGB(x + i, y + j, (gray << 16) | (gray << 8) | gray);
	  }
	}
      }
    }

    System.err.println(".");

    return dst;
  }

  // - - - - - - - - - - - - - - - -
  void setupWithArea(int from_x, int from_y, int to_x, int to_y) {
    int fx = from_x;
    int fy = from_y;
    int tx = to_x;
    int ty = to_y;

    if (fx < 0)
      fx = 0;

    if (tx >= target_width)
      tx = target_width - 1;
    
    if (fy < 0)
      fy = 0;

    if (ty >= target_height)
      ty = target_height - 1;

    // calc 
    calcAverage(fx, fy, tx, ty);
    calcRegression(fx, fy, tx, ty);
    calcShift(fx, fy, tx, ty);
  }

  // - - - - - - - - - - - - - - - -
  void calcAverage(int from_x, int from_y, int to_x, int to_y) {
    int x, y;
    int pixel, red, green, blue;

    count = 0;

    r_ave = 0.0f;
    g_ave = 0.0f;
    b_ave = 0.0f;

    for (y = from_y; y < to_y; y++) {
      for (x = from_x; x < to_x; x++) {
	pixel = target.getRGB(x, y);
	red = (pixel >> 16) & 0xFF;
	green = (pixel >> 8) & 0xFF;
	blue = pixel & 0xFF;

	if (checkThreshold(red, green, blue)) {
	  count++;
	  r_ave += red;
	  g_ave += green;
	  b_ave += blue;
	}
      }
    }

    if (count == 0)
      return;

    r_ave /= count * 255.0f;
    g_ave /= count * 255.0f;
    b_ave /= count * 255.0f;
  }

  // - - - - - - - - - - - - - - - -
  void calcRegression(int from_x, int from_y, int to_x, int to_y) {
    int x, y;
    int pixel, red, green, blue;
    float r, g, b;

    float r_sum = 0.0f;
    float g_sum = 0.0f;
    float b_sum = 0.0f;

    float br_sum = 0.0f;
    float rg_sum = 0.0f;
    float gb_sum = 0.0f;

    for (y = from_y; y < to_y; y++) {
      for (x = from_x; x < to_x; x++) {
	pixel = target.getRGB(x, y);
	red = (pixel >> 16) & 0xFF;
	green = (pixel >> 8) & 0xFF;
	blue = pixel & 0xFF;

	if (checkThreshold(red, green, blue)) {
	  r = red / 255.0f - r_ave;
	  g = green / 255.0f - g_ave;
	  b = blue / 255.0f - b_ave;

	  r_sum += r * r;
	  g_sum += g * g;
	  b_sum += b * b;

	  br_sum += b * r;
	  rg_sum += r * g;
	  gb_sum += g * b;
	}
      }
    }

    br_regression = (b_sum > 0.0f) ? (br_sum / b_sum) : 1.0f;
    rg_regression = (r_sum > 0.0f) ? (rg_sum / r_sum) : 1.0f;
    gb_regression = (g_sum > 0.0f) ? (gb_sum / g_sum) : 1.0f;
  }

  // - - - - - - - - - - - - - - - -
  void calcShift(int from_x, int from_y, int to_x, int to_y) {
    int x, y;
    float sum_br = 0.0f;
    int pixel, red, green, blue;

    if (count == 0) {
      shift_br = 128;
      return;
    }

    for (y = from_y; y < to_y; y++) {
      for (x = from_x; x < to_x; x++) {
	pixel = target.getRGB(x, y);
	red = (pixel >> 16) & 0xFF;
	green = (pixel >> 8) & 0xFF;
	blue = pixel & 0xFF;

	if (checkThreshold(red, green, blue)) 
	  sum_br += calcPlainBR(red, green, blue);
      }
    }

    shift_br = 128 - (int)(sum_br / count);
  }

  // - - - - - - - - - - - - - - - -
  boolean checkThreshold(int red, int green, int blue) {
    return !(((red < dark_threshold) 
	      && (green < dark_threshold) 
	      && (blue < dark_threshold))
	     || ((red > bright_threshold) 
		 && (green > bright_threshold) 
		 && (blue > bright_threshold)));
  }

  // - - - - - - - - - - - - - - - -
  float getMixed(int red, int green, int blue) {
    return calcPlainBR(red, green, blue) + shift_br;
  }

  // - - - - - - - - - - - - - - - -
  float calcPlainBR(int red, int green, int blue) {
    return blue * br_regression * br_ratio - red * br_ratio
      + green * rg_regression * gb_regression;
  }
}

// End of File
