package com.louissimonmcnicoll.simpleframe.transformers;

import androidx.viewpager.widget.ViewPager;
import android.view.View;

/**
 * Created by ussher on 20.06.15.
 */
public class ZoomInTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        PropertiesResetter.resetPageProperties(page);
        final float scale = position < 0 ? position + 1f : Math.abs(1f - position);
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setPivotX(page.getWidth() * 0.5f);
        page.setPivotY(page.getHeight() * 0.5f);
        page.setAlpha(position < -1f || position > 1f ? 0f : 1f - (scale - 1f));
    }
}
