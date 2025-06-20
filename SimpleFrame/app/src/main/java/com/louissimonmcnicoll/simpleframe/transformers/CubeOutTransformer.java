package com.louissimonmcnicoll.simpleframe.transformers;

import androidx.viewpager.widget.ViewPager;
import android.view.View;

/**
 * Created by ussher on 15.06.15.
 */
public class CubeOutTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        PropertiesResetter.resetPageProperties(page);
        page.setPivotX(position < 0f ? page.getWidth() : 0f);
        page.setPivotY(page.getHeight() * 0.5f);
        page.setRotationY(90f * position);
    }
}

