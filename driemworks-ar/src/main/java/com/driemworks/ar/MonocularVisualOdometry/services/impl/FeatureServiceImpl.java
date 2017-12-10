package com.driemworks.ar.MonocularVisualOdometry.services.impl;

import android.util.Log;

import com.driemworks.ar.MonocularVisualOdometry.services.FeatureService;
import com.driemworks.ar.dto.FeatureWrapper;
import com.driemworks.ar.dto.SequentialFrameFeatures;
import com.driemworks.common.utils.ImageConversionUtils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.video.Video;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the FeatureService
 * By Default, it uses FAST/ORB/Brute force Hamming distance
 * @author Tony
 */
public class FeatureServiceImpl implements FeatureService {

    /**
     * The detector
     */
    private FeatureDetector detector;

    /**
     * The descriptor descriptorExtractor
     */
    private DescriptorExtractor descriptorExtractor;

    /**
     * The descriptorMatcher
     */
    private DescriptorMatcher descriptorMatcher;

    /**
     * The term criteria
     */
    private TermCriteria termCriteria;

    /** The size */
    private Size size;

    /**
     * The constant "previous"
     */
    private static final String PREVIOUS = "previous";

    /**
     * The constant "current"
     */
    private static final String CURRENT = "current";

    /**
     * Constructor for the FeatureServiceImpl with default params (FAST/ORB/HAMMING)
     */
    public FeatureServiceImpl() {
        // FAST feature detector
        detector = FeatureDetector.create(FeatureDetector.FAST);
        // ORB descriptor extraction
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        // brute force hamming metric
        descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        size = new Size(21, 21);
        termCriteria = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 10, 0.02);
    }

    /**
     * Constructor for the FeatureServiceImpl
     * @param detector The FeatureDetector
     * @param descriptorExtractor The DescriptorExtractor
     * @param descriptorMatcher The DescriptorMatcher
     */
    public FeatureServiceImpl(FeatureDetector detector, DescriptorExtractor descriptorExtractor, DescriptorMatcher descriptorMatcher) {
        this.detector = detector;
        this.descriptorExtractor = descriptorExtractor;
        this.descriptorMatcher = descriptorMatcher;
    }

    @Override
    public FeatureWrapper featureDetection(Mat frame) {
        Log.d(this.getClass().getCanonicalName(), "START - featureDetection");
        long startTime = System.currentTimeMillis();
        MatOfKeyPoint mKeyPoints = new MatOfKeyPoint();
        Mat mIntermediateMat = new Mat();

        detector.detect(frame, mKeyPoints);
        descriptorExtractor.compute(frame, mKeyPoints, mIntermediateMat);
        Log.d(this.getClass().getCanonicalName(), "END - featureDetection - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
        return new FeatureWrapper("fast", "orb", frame, mIntermediateMat, mKeyPoints, null);
    }

    @Override
    public SequentialFrameFeatures featureTracking(Mat previousFrameGray, Mat currentFrameGray,
                                                   MatOfKeyPoint previousKeyPoints,
                                                   MatOfByte status, MatOfFloat err) {
        Log.d(this.getClass().getCanonicalName(), "START - featureTracking");
        long startTime = System.currentTimeMillis();

        // filter out points not tracked in current frame
        MatOfPoint2f previousKeyPoints2f = ImageConversionUtils.convertMatOfKeyPointsTo2f(previousKeyPoints);
        MatOfPoint2f previousKPConverted = new MatOfPoint2f();
        previousKeyPoints2f.convertTo(previousKPConverted, CvType.CV_32FC2);
        Log.d("previousKeyPoints2f ", "checkVector: " + previousKPConverted.checkVector(2));

        MatOfPoint2f currentKeyPoints2f = new MatOfPoint2f();

        Video.calcOpticalFlowPyrLK(previousFrameGray, currentFrameGray,
                previousKeyPoints2f, currentKeyPoints2f,
                status, err, size, 0, termCriteria, 0, 0.001);

        byte[] statusArray = status.toArray();
        Log.d("statusArray", "length: " + statusArray.length);

        Map<String, LinkedList<Point>> filteredPointMap = filterPoints(previousKeyPoints2f.toList(), currentKeyPoints2f.toList(), statusArray);
        Log.d(this.getClass().getCanonicalName(), "END - featureTracking - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
//        previousKeyPoints2f.release();
//        previousKPConverted.release();
//        currentKeyPoints2f.release();
        return new SequentialFrameFeatures(filteredPointMap.get(PREVIOUS), filteredPointMap.get(CURRENT));
    }

    /**
     * Filters out points which fail tracking, or which were tracked off screen
     * @param previousKeypoints The list of keypoints in the previous image
     * @param currentKeypoints The list of keypoints in the current image
     * @param statusArray The status array
     */
    private Map<String, LinkedList<Point>> filterPoints(List<Point> previousKeypoints, List<Point> currentKeypoints, byte[] statusArray) {
        int indexCorrection = 0;
        LinkedList<Point> currentCopy = new LinkedList<>(currentKeypoints);
        LinkedList<Point> previousCopy = new LinkedList<>(previousKeypoints);
        for (int i = 0; i < currentKeypoints.size(); i++) {
            Point pt = currentKeypoints.get(i - indexCorrection);
            if (statusArray[i] == 0 || (pt.x == 0 || pt.y == 0)) {
                // removes points which are tracked off screen
                if (pt.x == 0 || pt.y == 0) {
                    statusArray[i] = 0;
                }

                // remove points for which tracking has failed
                currentCopy.remove(i - indexCorrection);
                previousCopy.remove(i - indexCorrection);
                indexCorrection++;
            }
        }

        Map<String, LinkedList<Point>> filteredPointsMap = new LinkedHashMap<>();
        filteredPointsMap.put(PREVIOUS, previousCopy);
        filteredPointsMap.put(CURRENT, currentCopy);
        return filteredPointsMap;
    }

    /**
     * Getter for the FeatureDetector
     * @return The FeatureDetector
     */
    public FeatureDetector getDetector() {
        return detector;
    }

    /**
     * Setter for the FeatureDetector
     * @param detector the FeatureDetector
     */
    public void setDetector(FeatureDetector detector) {
        this.detector = detector;
    }

    /**
     * Getter for the DescriptorExtractor
     * @return the descriptorExtractor
     */
    public DescriptorExtractor getDescriptorExtractor() {
        return descriptorExtractor;
    }

    /**
     * Setter for the setDescriptorExtractor
     * @param descriptorExtractor The setDescriptorExtractor to set
     */
    public void setDescriptorExtractor(DescriptorExtractor descriptorExtractor) {
        this.descriptorExtractor = descriptorExtractor;
    }

    /**
     * Getter for the descriptorMatcher
     * @return the descriptorMatcher
     */
    public DescriptorMatcher getDescriptorMatcher() {
        return descriptorMatcher;
    }

    /**
     * Setter for the descriptorMatcher
     * @param descriptorMatcher The descriptorMatcher to set
     */
    public void setDescriptorMatcher(DescriptorMatcher descriptorMatcher) {
        this.descriptorMatcher = descriptorMatcher;
    }
}