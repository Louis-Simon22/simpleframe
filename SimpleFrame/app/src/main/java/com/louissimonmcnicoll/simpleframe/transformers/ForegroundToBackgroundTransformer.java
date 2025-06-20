package com.louissimonmcnicoll.simpleframe.transformers;

import androidx.viewpager.widget.ViewPager;
import android.view.View;

/**
 * Created by ussher on 15.06.15.
 */
public class ForegroundToBackgroundTransformer implements ViewPager.PageTransformer{
    @Override
    public void transformPage(View page, float position) {
        PropertiesResetter.resetPageProperties(page);
        final float height = page.getHeight();
        final float width = page.getWidth();
        final float scale = min(position > 0 ? 1f : Math.abs(1f + position));

        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setPivotX(width * 0.5f);
        page.setPivotY(height * 0.5f);
        page.setTranslationX(position > 0 ? width * position : -width * position * 0.25f);
    }

    private static float min(float val) {
        return Math.max(val, (float) 0.5);
    }
}
