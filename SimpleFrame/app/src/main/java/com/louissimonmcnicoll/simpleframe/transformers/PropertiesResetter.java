package com.louissimonmcnicoll.simpleframe.transformers;

import android.view.View;

public class PropertiesResetter {
    /**
     * Resets all the attributes used in any of the transformers. This prevents a change of
     * transformer from leaving invalid state. For example if a transformer that changes the rotation
     * is changed for one that doesn't, the old rotation changes might be left over when changing.
     *
     * @param page The view page to reset
     */
    public static void resetPageProperties(View page) {
        page.setAlpha(1f);
        page.setPivotX(page.getWidth() * 0.5f);
        page.setPivotY(page.getHeight() * 0.5f);
        page.setRotation(0f);
        page.setRotationX(0f);
        page.setRotationY(0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
        page.setTranslationX(0f);
        page.setTranslationY(0f);
    }
}
