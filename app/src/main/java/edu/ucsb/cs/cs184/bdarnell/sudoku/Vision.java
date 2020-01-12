package edu.ucsb.cs.cs184.bdarnell.sudoku;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.compare;
import static org.opencv.core.Core.subtract;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughLines;
import static org.opencv.imgproc.Imgproc.MORPH_CROSS;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.connectedComponentsWithStats;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.floodFill;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.warpPerspective;
import static org.opencv.photo.Photo.fastNlMeansDenoisingMulti;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
        // Load the image in gray scale
        Mat image = imread(Environment.getExternalStorageDirectory() + "/sudoku.png", 0);
        saveImage(image, "out1.png");
        image = preProcess(image);
        saveImage(image, "out2.png");
        List<Point> box = findBox(image);
        image = deSkew(image, box);
        saveImage(image, "out4.png");
        image = parsePuzzle(image);
        bitwise_not(image, image);
        saveImage(image, "solution.png");
    }

    private static Mat preProcess(Mat image) {
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
        saveImage(overlay, "out3.png");

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

    private static Mat deSkew(Mat image, List<Point> bounds) {
        MatOfPoint2f source = new MatOfPoint2f(
                bounds.get(0), bounds.get(1), bounds.get(2), bounds.get(3)
        );
        System.out.println(bounds);
        double size = Math.abs(bounds.get(2).x - bounds.get(1).x);
        MatOfPoint2f target = new MatOfPoint2f(
                new Point(0, 0), new Point(size, 0), new Point(0, size), new Point(size, size)
        );
        System.out.println(size);
        Mat transform = getPerspectiveTransform(source, target);
        Mat newImage = new Mat();
        warpPerspective(image, newImage, transform, new Size(size, size));
        return newImage;
    }

    private static Mat parsePuzzle(Mat image) {
        int size = image.width() / 9;
        Mat representatives[] = new Mat[9];
        Mat repSectors[] = new Mat[9];
        int puzzle[][] = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // Create sector:
                Mat sector = new Mat(image,
                        new Rect(i * size, j * size, size, size)
                );

                Mat cropped = cropToNumeral(sector, size);

                if(cropped == null) {
                    puzzle[j][i] = 0;
                } else {
                    for (int r = 0; r < representatives.length; r++) {
                        if (representatives[r] == null) {
                            representatives[r] = cropped;
                            repSectors[r] = sector;
                            puzzle[j][i] = r + 1;
                            break;
                        } else if (compareImage(representatives[r], cropped, (i == 1 && j == 2 && r == 0))) {
                            puzzle[j][i] = r + 1;
                            break;
                        }
                    }
                }

                // Display all boxes
                if (cropped != null) {
                    saveImage(cropped, "sector_" + i + "_" + j + ".png");
                } else {
                    saveImage(sector, "sector_" + i + "_" + j + ".png");
                }

            }
        }

        int[][] solution = solve(puzzle);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[j][i] == 0) {
                    Mat overlay = repSectors[solution[j][i] - 1];
                    overlay.copyTo(image.submat(new Rect(i * size, j * size, size, size)));
                }
            }
        }

        line(image, new Point(0, 0), new Point(image.width(), 0),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(0, 3 * size), new Point(image.width(), 3 * size),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(0, 6 * size), new Point(image.width(), 6 * size),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(0, image.width()), new Point(image.width(), image.width()),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(0, 0), new Point(0, image.width()),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(3 * size, 0), new Point(3 * size, image.width()),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(6 * size, 0), new Point(6 * size, image.width()),
                new Scalar(255, 255, 255), 20);
        line(image, new Point(image.width(), 0), new Point(image.width(), image.width()),
                new Scalar(255, 255, 255), 20);

        return image;
    }

    private static int[][] solve(int[][] grid) {
        Puzzle puzzle = new Puzzle(grid.clone());
        puzzle.display();
        try {
            puzzle.solve();
        } catch (InvalidPuzzleException exception) {
            System.out.println("You screwed up big time");
        }
        System.out.println();
        puzzle.display();
        return puzzle.getGrid();
    }

    private static Mat cropToNumeral(Mat sector, int size) {
        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(sector, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Remove small contours
        MatOfPoint numeral = null;
        double area = 0;
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) > (size * size) / 100.0) {
                Rect rect = Imgproc.boundingRect(contour);
                if (rect.height < size * 0.8 && rect.width < size * 0.8 && rect.height > size * 0.3) {
                    if (rect.width * rect.height > area) {
                        numeral = contour;
                        area = rect.width * rect.height;
                    }
                }
            }
        }

        if (numeral != null) {
            return new Mat(sector, Imgproc.boundingRect(numeral));
        } else {
            return null;
        }
    }

    private static boolean compareImage(Mat source, Mat target, boolean save) {
        Rect frame = new Rect(0, 0,
                Math.min(source.width(), target.width()),
                Math.min(source.height(), target.height())
        );
        Mat newSource = new Mat(source, frame);
        Mat newTarget = new Mat(target, frame);
        Mat result = new Mat(frame.size(), CV_8UC1);
        absdiff(newSource, newTarget, result);
        Mat kernel = getStructuringElement(MORPH_CROSS, new Size(3,3));
        erode(result, result, kernel);
        Scalar mean = Core.mean(result);
        System.out.println(mean);
        if (save) {
            saveImage(newSource, "out5-0.png");
            saveImage(newTarget, "out5-1.png");
            saveImage(result, "out5.png");
        }
        return mean.val[0] < 8;
    }


}
