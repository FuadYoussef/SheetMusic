package com.example.superior_piano;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static int BLUR_SIZE = 3;
    static int CANNY_THRESHOLD = 200;
    static double MIN_PIANO_AREA_RATIO = 1.0 / 5;
    static int WHITE_KEYS = 7;

    static int BLACK_KEY_R = 100; // R of RGB
    static int WHITE_KEY_R = 200; // key "C" = 200, "D" = 201, "E" = 202, ...

    Mat cannyOutput;
    Mat mask;
    Mat hierarchy;
    Mat dilateKernel;
    Mat layout;

    //view holder
    CameraBridgeViewBase cameraBridgeViewBase;

    //camera listener callback
    BaseLoaderCallback baseLoaderCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.cameraViewer);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        //create camera listener callback
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        cannyOutput = new Mat();
                        mask = new Mat();
                        hierarchy = new Mat();
                        dilateKernel = new Mat();
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        layout = new Mat(height, width, CvType.CV_8UC3, new Scalar(0, 0, 0));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "There is a problem", Toast.LENGTH_SHORT).show();
        } else {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat dst = inputFrame.rgba();
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGBA2RGB);

        Mat gray = inputFrame.gray();

        Imgproc.blur(gray, gray, new Size(BLUR_SIZE, 3));

        Imgproc.Canny(gray, cannyOutput, CANNY_THRESHOLD, CANNY_THRESHOLD * 2);
        Imgproc.dilate(cannyOutput, cannyOutput, dilateKernel, new Point(-1, 1), 1);

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int minPianoArea = (int) (gray.width() * gray.height() * MIN_PIANO_AREA_RATIO);
        Point[] vertices = this.getPianoVertices(contours, minPianoArea);
        if (vertices != null) {
            layout.setTo(new Scalar(0, 0, 0));

            MatOfPoint mat = new MatOfPoint(vertices);
            Imgproc.fillPoly(layout, Collections.singletonList(mat), new Scalar(BLACK_KEY_R, 255, 0));

            Arrays.sort(vertices, (p1, p2) -> (int) (p1.x - p2.x));
            int leftX = (int) vertices[1].x; // top left x
            int rightX = (int) vertices[2].x; // top right x
            int whiteKeyWidth = (rightX - leftX) / WHITE_KEYS;

            Arrays.sort(vertices, (p1, p2) -> (int) (p1.y - p2.y));
            int topY = (int) vertices[1].y; // greater y between top left and top right
            int bottomY = (int) vertices[2].y; // lesser y between bottom left and bottom right

            Log.d("vertices", String.format("[%d, %d], %d", leftX, rightX, topY));

            Core.copyMakeBorder(cannyOutput, mask, 1, 1, 1, 1, Core.BORDER_CONSTANT, new Scalar(255));

            for (int whiteKeyIndex = 0; whiteKeyIndex < WHITE_KEYS; whiteKeyIndex++) {
                int x = rightX - whiteKeyIndex * whiteKeyWidth - whiteKeyWidth / 2;
                int y = topY + (bottomY - topY) / 10;
                Point point = new Point(x, y);
                int R = WHITE_KEY_R + whiteKeyIndex;
                Imgproc.floodFill(layout, mask, point, new Scalar(R, 0, 0));
                Imgproc.circle(layout, point, 5, new Scalar(R, 255, 255), 5);
            }
        }

        // TODO: finger detection
        int fingerTipRow = 0;
        int fingerTipCol = 0;
        String pianoKey = getPianoKey(fingerTipRow, fingerTipCol);
        // TODO: send the piano key to arduino

        Core.add(dst, layout, dst);
        return dst;
    }

    Point[] getPianoVertices(List<MatOfPoint> contours, double minArea) {
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);

            double area = Imgproc.contourArea(contour);

            if (area > minArea) {
                MatOfPoint2f poly = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), poly, 10, true);
                Point[] vertices = poly.toArray();
                if (vertices.length == 4) {
                    contours.remove(contour);
                    return vertices;
                }
            }
        }

        return null;
    }

    /**
     * @param row
     * @param col
     * @return "C", "D#", ..., or null
     */
    String getPianoKey(int row, int col) {
        String keys = "CDEFGAB";
        int R = (int) layout.get(row, col)[0];
        if (R == BLACK_KEY_R) {
            while (++col < layout.width()) {
                R = (int) layout.get(row, col)[0];
                if (R >= WHITE_KEY_R) {
                    return keys.charAt(R - WHITE_KEY_R) + "#";
                }
            }
            return null;
        } else if (R >= WHITE_KEY_R) {
            return keys.charAt(R - WHITE_KEY_R) + "";
        }
        return null;
    }
}