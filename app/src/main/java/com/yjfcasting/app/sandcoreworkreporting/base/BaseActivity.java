package com.yjfcasting.app.sandcoreworkreporting.base;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.0f;

    private View content;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new PanListener());
        content = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        scaleGestureDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev); // ← 新增這行！
        return super.dispatchTouchEvent(ev);
    }

    private void applyTransform() {
        if (content != null) {
            content.setScaleX(scaleFactor);
            content.setScaleY(scaleFactor);
            content.setTranslationX(translationX);
            content.setTranslationY(translationY);
        }
    }

    private float translationX = 0f;
    private float translationY = 0f;

    private class PanListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 只有當縮放 > 1 才允許拖動
            if (scaleFactor > 1.0f) {
                translationX -= distanceX;
                translationY -= distanceY;
                applyTransform();
            }
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));
            // 取得手指中心點（相對於 View 本身）
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            // 對根 view 進行縮放
            View content = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);

            // 設定 pivot（縮放基準點）
            content.setPivotX(focusX);
            content.setPivotY(focusY);
            content.setScaleX(scaleFactor);
            content.setScaleY(scaleFactor);
            return true;
        }
    }
}
