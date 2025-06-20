package com.louissimonmcnicoll.simpleframe.transformers;

import androidx.viewpager.widget.ViewPager;
import android.view.View;

/**
 * Created by ussher on 20.06.15.
 */
public class FlipVerticalTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        PropertiesResetter.resetPageProperties(page);

        final float rotation = -180f * position;

        page.setAlpha(rotation > 90f || rotation < -90f ? 0f : 1f);
        page.setPivotX(page.getWidth() * 0.5f);
        page.setPivotY(page.getHeight() * 0.5f);
        page.setRotationX(rotation);
    }
}
