package edu.ucsb.cs.cs184.bdarnell.sudoku;b

import org.opencv.core.Mat;

public class Vision {

    public static void analyzeImage() {
        System.out.println("Hello, world;");
        // Load the image into array type
        Mat image_matrix = imread("test.jpg", 0);
        // Perform
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
