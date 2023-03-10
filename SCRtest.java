//
// A Testing Code for Sky Color Regression
// by Yuji Ayatsuka, 2023/01
//
// - to compile - 
// javac SCRtest.java SCRsample.java
//
// - usage -
// java SCRtest target_file.jpg
//

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class SCRtest {
  final static String output_file = "output.jpg";

  // - - - - - - - - - - - - - - - -
  public static void main(String[] args) {
    if (args.length > 0) {
      BufferedImage image = loadImage(args[0]);
      BufferedImage result = SCRsample.SkyColorRegression(image);
      saveImage(result, output_file, "JPG");
    }
  }

  // - - - - - - - - - - - - - - - -
  public static BufferedImage loadImage(String filename) {
    BufferedImage img = null;

    java.io.File f = new java.io.File(filename);

    if (!f.exists() || f.isDirectory()) {
      System.err.println("no such file, or it is not a file: " + filename);
      return null;
    }

    try {
      img = ImageIO.read(f);
    } catch (IOException ie) {
      System.err.println(ie);
    }

    return img;
  }

  // - - - - - - - - - - - - - - - -
  public static void saveImage(BufferedImage img, String filename, String format) {
    File f = new File(filename);
    System.err.println("Saving: " + filename);

    try {
      ImageIO.write(img, format, f);
      System.err.println("done");
    } catch (IOException ie) {
      System.err.println(ie);
    }
  }
}

// End of File
