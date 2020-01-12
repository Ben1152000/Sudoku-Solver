package edu.ucsb.cs.cs184.bdarnell.sudoku;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughLines;
import static org.opencv.imgproc.Imgproc.MORPH_CROSS;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.floodFill;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Vision {

    public static void analyzeImage() {
        System.out.println("Hello, world;");
        // Load the image in gray scale
        Mat image_matrix = imread(Environment.getExternalStorageDirectory() + "/sudoku.png", 0);
        System.out.println(image_matrix.size());
        // create blank image of the same size
        saveImage(image_matrix, "out1.png");
        Mat outerBox = preprocess(image_matrix);
        saveImage(outerBox, "out2.png");
        findBox(outerBox);
        // Perform
    }

    private static Mat preprocess(Mat image) {
        Mat outerBox = new Mat(image.size(), CV_8UC1);

        // smooth out noise
        GaussianBlur(image, image, new Size(9,9), 0);
        // keep things illumination independent
        adaptiveThreshold(image, outerBox, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 5, 2);
        bitwise_not(outerBox, outerBox);

        int filter[] = {0, 1, 0, 1, 1, 1, 0, 1, 0};
        //Mat kernel = new Mat(3, 3, CvType.CV_8U);
        //kernel.put(0, 0, filter);
        Mat kernel = getStructuringElement(MORPH_CROSS, new Size(3,3));

        erode(outerBox, outerBox, kernel);
        dilate(outerBox, outerBox, kernel);


        return outerBox;
    }

    private static void saveImage(Mat image, String filename) {

        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        //image.release();
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
    private static int[] findBox(Mat image) {
        /*Imgproc.Canny(image, image, 50, 200, 3, false);
        Mat lines = new Mat();
        HoughLines(image, lines, 1, 3.14/180, 200);
        System.out.println("Size: " + lines.size());
        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0], theta = lines.get(x, 0)[1];
            System.out.println("Rho: " + rho + ", Theta: " + theta);
        }*/

        Imgproc.Canny(image, image, 50, 200, 3, false);
        saveImage(image, "out3.png");
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);

        Mat overlay = image.clone();
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > 50) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                double ratio = rect.width / rect.height;
                if (rect.height > 50 && rect.width > 50 && ratio < 1.25 && ratio > 0.75 && area > rect.height * rect.width * 0.75) {
                    drawContours(overlay, contours, i, new Scalar(255, 0, 0), 20);
                    System.out.println(rect.height + ":" + rect.width + ", " + area + " " + rect.height * rect.width + " " + rect.x + "x" + rect.y);
                }
            }
        }

        saveImage(overlay, "out4.png");
        //saveImage(image, "out.png");
        //saveImage(lines, "lines.png");*/

        int count=0;
        int max=-1;

        Point maxPt;


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
