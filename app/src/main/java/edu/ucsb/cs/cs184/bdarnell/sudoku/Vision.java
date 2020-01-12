package edu.ucsb.cs.cs184.bdarnell.sudoku;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;

import org.opencv.core.Size;

import java.io.File;
import java.io.FileOutputStream;

public class Vision {

    public static void analyzeImage() {
        System.out.println("Hello, world;");
        // Load the image in gray scale
        Mat image_matrix = imread(Environment.getExternalStorageDirectory() + "/sudoku.png", 0);
        System.out.println(image_matrix.size());
        // create blank image of the same size
        Mat outerBox = new Mat(image_matrix.size(), CV_8UC1);
        // smooth out noise
        GaussianBlur(image_matrix, image_matrix, new Size(11,11), 0);
        // keep things illumination independent
        adaptiveThreshold(image_matrix, outerBox, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 5, 2);
        bitwise_not(outerBox, outerBox);
        saveImage(outerBox);
        // Perform
    }
    private static void saveImage(Mat image) {

        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        image.release();
        String filename = "out.png";
        File destination = new File(Environment.getExternalStorageDirectory(), filename);
        FileOutputStream out;
        try {
            out = new FileOutputStream(destination);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e){}



    }
    /**
     * Input: image in array form
     * @param image
     * @return array of boundary coordinates, or null if no box
     */
    private int[] findBox(Object image) {
       return null; 
    }

    /**
     * Do OCR on the image
     * @param image
     * @param boundaries
     * @return
     */
    private int[][] parsePuzzle(Object image, int[] boundaries) {
        return null;
    }



}
