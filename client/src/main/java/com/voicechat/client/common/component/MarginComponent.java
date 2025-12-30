package com.voicechat.client.common.component;

import javafx.scene.layout.Region;

public class MarginComponent {

    public Region initHorizontalMargin(int margin) {
        Region region = new Region();
        region.setPrefWidth(margin);
        return region;
    }

    public Region initVerticalMargin(int margin) {
        Region region = new Region();
        region.setPrefHeight(margin);
        return region;
    }

}
