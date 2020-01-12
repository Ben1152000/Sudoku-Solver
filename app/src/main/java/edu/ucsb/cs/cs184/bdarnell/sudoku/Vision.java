package edu.ucsb.cs.cs184.bdarnell.sudoku;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.compare;
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
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.circle;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        List<Point> box = findBox(outerBox);
        System.out.println("box: " + box);
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
    private static List<Point> findBox(Mat image) {
        saveImage(image, "out3.png");

        // Find contours of the image:
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);

        // Find boxes in the contours:
        List<MatOfPoint> boxes = new ArrayList<MatOfPoint>();
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > 100) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if (rect.height > 50 && rect.width > 50 && area > rect.height * rect.width * 0.8) {
                    boxes.add(contours.get(i));
                }
            }
        }

        // Eliminate poorly sized boxes
        List<Double> sizes = new ArrayList<Double>();
        for (int i = 0; i < boxes.size(); i++) {
            sizes.add(Imgproc.contourArea(boxes.get(i)));
        }
        Collections.sort(sizes);
        double median = sizes.get(sizes.size() / 2);
        List<MatOfPoint> newBoxes = new ArrayList<MatOfPoint>();
        for (MatOfPoint box : boxes) {
            double area = Imgproc.contourArea(box);
            if (area < (median * 3) && area > (median / 3)) {
                newBoxes.add(box);
            }
        }
        boxes = newBoxes;

        // Sort boxes by diagonal:
        Comparator<MatOfPoint> compareDiagonally = new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint c1, MatOfPoint c2) {
                Rect r1 = Imgproc.boundingRect(c1);
                Rect r2 = Imgproc.boundingRect(c2);
                return (r1.x + r1.y) - (r2.x + r2.y);
            }
        };
        Collections.sort(boxes, compareDiagonally);

        // Remove all non-connected boxes, IMPORTANT: ASSUME THE TOP-LEFT BOX IS CORRECT
        Rect bounds = new Rect();
        List<MatOfPoint> grid = new ArrayList<MatOfPoint>();
        for (MatOfPoint box : boxes) {
            Rect rect = Imgproc.boundingRect(box);
            if (grid.size() == 0) {
                bounds = rect;
                grid.add(box);
                System.out.println(rect.x + " " + rect.y + " " + rect.width + " " + rect.height);
            } else if (rect.x < (bounds.x + bounds.width + 10) && rect.y < (bounds.y + bounds.height + 10)) {
                System.out.println((bounds.x + bounds.width) + " " + (bounds.y + bounds.height) + " " + rect.x + " " + rect.y);
                bounds = new Rect(
                        Math.min(bounds.x, rect.x),
                        Math.min(bounds.y, rect.y),
                        Math.max(bounds.x + bounds.width, rect.x + rect.width) - bounds.x,
                        Math.max(bounds.y + bounds.height, rect.y + rect.height) - bounds.y);
                grid.add(box);
            }
        }

        // Find the corners:
        // find the two points defining each boundary of the rectangle:
        Point left1 = new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        Point left2 = new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        for (int i = 0; i < grid.size(); i++) {
            Rect rect = Imgproc.boundingRect(grid.get(i));
            if (rect.x < left1.x) {
                left2 = left1;
                left1 = new Point(rect.x, rect.y);
            } else if (rect.x < left2.x) {
                left2 = new Point(rect.x, rect.y);
            }
        }

        Point right1 = new Point(Double.MIN_VALUE, Double.MIN_VALUE);
        Point right2 = new Point(Double.MIN_VALUE, Double.MIN_VALUE);
        for (int i = 0; i < grid.size(); i++) {
            Rect rect = Imgproc.boundingRect(grid.get(i));
            if (rect.x + rect.width > right1.x) {
                right2 = right1;
                right1 = new Point(rect.x + rect.width, rect.y);
            } else if (rect.x + rect.width > right2.x) {
                right2 = new Point(rect.x + rect.width, rect.y);
            }
        }

        Point top1 = new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        Point top2 = new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        for (int i = 0; i < grid.size(); i++) {
            Rect rect = Imgproc.boundingRect(grid.get(i));
            if (rect.y < top1.y) {
                top2 = top1;
                top1 = new Point(rect.x, rect.y);
            } else if (rect.y < top2.y) {
                top2 = new Point(rect.x, rect.y);
            }
        }

        Point bottom1 = new Point(Double.MIN_VALUE, Double.MIN_VALUE);
        Point bottom2 = new Point(Double.MIN_VALUE, Double.MIN_VALUE);
        for (int i = 0; i < grid.size(); i++) {
            Rect rect = Imgproc.boundingRect(grid.get(i));
            if (rect.y + rect.height > bottom1.y) {
                bottom2 = bottom1;
                bottom1 = new Point(rect.x, rect.y + rect.height);
            } else if (rect.y + rect.height > bottom2.y) {
                bottom2 = new Point(rect.x, rect.y + rect.height);
            }
        }


        // Display all boxes
        Mat overlay = image.clone();
        for (int i = 0; i < grid.size(); i++) {
            Rect rect = Imgproc.boundingRect(grid.get(i));
            System.out.println("X: " + rect.x + ", Y: " + rect.y);
            drawContours(overlay, grid, i, new Scalar(255, 0, 0), 20);
        }

        Point topLeft = intersection(left1, left2, top1, top2);
        Point topRight = intersection(right1, right2, top1, top2);
        Point bottomLeft = intersection(left1, left2, bottom1, bottom2);
        Point bottomRight = intersection(right1, right2, bottom1, bottom2);

        circle(overlay, topLeft, 80, new Scalar(255, 0, 0), 20);
        circle(overlay, topRight, 80, new Scalar(255, 0, 0), 20);
        circle(overlay, bottomLeft, 80, new Scalar(255, 0, 0), 20);
        circle(overlay, bottomRight, 80, new Scalar(255, 0, 0), 20);

        circle(overlay, left1, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, left2, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, right1, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, right2, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, top1, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, top2, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, bottom1, 50, new Scalar(255, 0, 0), 20);
        circle(overlay, bottom2, 50, new Scalar(255, 0, 0), 20);
        saveImage(overlay, "out4.png");

        return new ArrayList<Point>(Arrays.asList(topLeft, topRight, bottomLeft, bottomRight));

        /*
        for(int i = 0; i < boxes.size(); i++) {
            Rect rect = Imgproc.boundingRect(boxes.get(i));
            System.out.println(rect.x + " " + rect.y + " " + rect.width + " " + rect.height + " ");
        }

        int threshold = 10;
        List<Rect> bounds = new ArrayList<Rect>();
        List<List<MatOfPoint>> grids = new ArrayList<List<MatOfPoint>>();
        for (int i = 0; i < boxes.size(); i++) {
            Rect rect = Imgproc.boundingRect(boxes.get(i));
            for (int j = 0; j < grids.size(); j++) {
                if (((rect.x > bounds.get(j).x - threshold
                        && rect.x < bounds.get(j).x + bounds.get(j).width + threshold)
                        || (rect.x + rect.width > bounds.get(j).x - threshold
                        && rect.x + rect.width < bounds.get(j).x + bounds.get(j).width + threshold))
                        && ((rect.y > bounds.get(j).y - threshold
                        && rect.y < bounds.get(j).y + bounds.get(j).height + threshold)
                        || (rect.y + rect.height > bounds.get(j).y - threshold
                        && rect.y + rect.height < bounds.get(j).y + bounds.get(j).height + threshold)))
                {
                    bounds.get(j).x = Math.min(rect.x, bounds.get(j).x);
                    bounds.get(j).y = Math.min(rect.y, bounds.get(j).y);
                    bounds.get(j).width = Math.max(bounds.get(j).width, rect.x + rect.width - bounds.get(j).x);
                    bounds.get(j).height = Math.max(bounds.get(j).height, rect.y + rect.height - bounds.get(j).y);
                    grids.get(j).add(boxes.get(i));
                    break;
                }
            }
            bounds.add(rect);
            List<MatOfPoint> newlist = new ArrayList<>();
            newlist.add(boxes.get(i));
            grids.add(newlist);
        }

        // find the largest grid
        List<MatOfPoint> largest_grid = new ArrayList<MatOfPoint>();
        int max_size = 0;
        for(int i = 0; i < grids.size(); i++) {
            if(grids.get(i).size() > max_size) {
                max_size = grids.get(i).size();
                largest_grid = grids.get(i);
            }
        }

        Mat grid_overlay = image.clone();
        for (int i = 0; i < largest_grid.size(); i++) {
            double area = Imgproc.contourArea(largest_grid.get(i));
            Rect rect = Imgproc.boundingRect(largest_grid.get(i));
            drawContours(grid_overlay, boxes, i, new Scalar(255, 0, 0), 20);
            //System.out.println(rect.height + ":" + rect.width + ", " + area + " " + rect.height * rect.width + " " + rect.x + "x" + rect.y);
        }
        saveImage(grid_overlay, "out5.png");

         */
    }

    private static Point intersection(Point p1, Point p2, Point q1, Point q2) {
        return new Point(
                ((p1.x * p2.y - p1.y * p2.x) * (q1.x - q2.x) - (p1.x - p2.x) * (q1.x * q2.y - q1.y * q2.x))
                        / ((p1.x - p2.x) * (q1.y - q2.y) - (p1.y - p2.y) * (q1.x - q2.x)),
                ((p1.x * p2.y - p1.y * p2.x) * (q1.y - q2.y) - (p1.y - p2.y) * (q1.x * q2.y - q1.y * q2.x))
                        / ((p1.x - p2.x) * (q1.y - q2.y) - (p1.y - p2.y) * (q1.x - q2.x))
        );
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
