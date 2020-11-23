package com.github.rmtmckenzie.qrmobilevision;

import android.util.Log;

import androidx.annotation.GuardedBy;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzer;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.huawei.hms.mlsdk.common.MLFrame;

import java.util.List;

/**
 * Allows QrCamera classes to send frames to a Detector
 */

class QrDetector implements OnSuccessListener<List<HmsScan>>, OnFailureListener {
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrReaderCallbacks communicator;
    private final HmsScanAnalyzer detector;

    public interface Frame {
        MLFrame toImage();
        void close();
    }

    @GuardedBy("this")
    private Frame latestFrame;

    @GuardedBy("this")
    private Frame processingFrame;

    QrDetector(QrReaderCallbacks communicator, HmsScanAnalyzerOptions options) {
        this.communicator = communicator;
        this.detector = new HmsScanAnalyzer(options);
    }

    void detect(Frame frame) {
        if (latestFrame != null) latestFrame.close();
        latestFrame = frame;

        if (processingFrame == null) {
            processLatest();
        }
    }

    private synchronized void processLatest() {
        if (processingFrame != null) processingFrame.close();
        processingFrame = latestFrame;
        latestFrame = null;
        if (processingFrame != null) {
            processFrame(processingFrame);
        }
    }

    private void processFrame(Frame image) {
        detector.analyzInAsyn(image.toImage())
            .addOnSuccessListener(this)
            .addOnFailureListener(this);
    }

    @Override
    public void onSuccess(List<HmsScan> hmsScans) {
        for (HmsScan barcode : hmsScans) {
            communicator.qrRead(barcode.originalValue);
        }
        processLatest();
    }

    @Override
    public void onFailure(Exception e) {
        Log.w(TAG, "Barcode Reading Failure: ", e);
    }
}
