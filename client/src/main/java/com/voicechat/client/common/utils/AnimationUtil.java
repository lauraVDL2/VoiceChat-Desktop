package com.voicechat.client.common.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;


public class AnimationUtil {

    public static void smoothVScrollTo(ScrollPane scrollPane, double targetVvalue) {
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(scrollPane.vvalueProperty(), targetVvalue);
        KeyFrame kf = new KeyFrame(Duration.millis(300), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    public static void fadeInNode(Node node) {
        node.setOpacity(0);
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(node.opacityProperty(), 1))
        );
        fadeIn.play();
    }

    // Optional: slide in from bottom
    public static void slideInNode(Node node, double fromY, double toY) {
        node.setTranslateY(fromY);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.translateYProperty(), fromY)),
                new KeyFrame(Duration.millis(300), new KeyValue(node.translateYProperty(), toY))
        );
        timeline.play();
    }
}
