package Messenger.Utils.easing;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.net.URL;
import static com.google.common.base.Preconditions.checkState;

public class GuiUtils {

    public static final int UI_ANIMATION_TIME_MSEC = 600;
    public static final Duration UI_ANIMATION_TIME = Duration.millis(UI_ANIMATION_TIME_MSEC);

    public static Animation fadeIn(Node ui) {
        return fadeIn(ui, 0);
    }

    public static Animation fadeIn(Node ui, int delayMillis) {
        ui.setCache(true);
        FadeTransition ft = new FadeTransition(Duration.millis(UI_ANIMATION_TIME_MSEC), ui);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setOnFinished(ev -> ui.setCache(false));
        ft.setDelay(Duration.millis(delayMillis));
        ft.play();
        return ft;
    }

    public static Animation fadeOut(Node ui) {
        FadeTransition ft = new FadeTransition(Duration.millis(UI_ANIMATION_TIME_MSEC), ui);
        ft.setFromValue(ui.getOpacity());
        ft.setToValue(0.0);
        ft.play();
        return ft;
    }

    public static Animation fadeOutAndRemove(Pane parentPane, Node... nodes) {
        Animation animation = fadeOut(nodes[0]);
        animation.setOnFinished(actionEvent -> parentPane.getChildren().removeAll(nodes));
        return animation;
    }

    public static Animation fadeOutAndRemove(Duration duration, Pane parentPane, Node... nodes) {
        nodes[0].setCache(true);
        FadeTransition ft = new FadeTransition(duration, nodes[0]);
        ft.setFromValue(nodes[0].getOpacity());
        ft.setToValue(0.0);
        ft.setOnFinished(actionEvent -> parentPane.getChildren().removeAll(nodes));
        ft.play();
        return ft;
    }

    public static void blurOut(Node node) {
        GaussianBlur blur = new GaussianBlur(0.0);
        node.setEffect(blur);
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(blur.radiusProperty(), 10.0);
        KeyFrame kf = new KeyFrame(Duration.millis(UI_ANIMATION_TIME_MSEC), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    public static void blurIn(Node node) {
        GaussianBlur blur = (GaussianBlur) node.getEffect();
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(blur.radiusProperty(), 0.0);
        KeyFrame kf = new KeyFrame(Duration.millis(UI_ANIMATION_TIME_MSEC), kv);
        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(actionEvent -> node.setEffect(null));
        timeline.play();
    }

    public static ScaleTransition zoomIn(Node node) {
        return zoomIn(node, 0);
    }

    public static ScaleTransition zoomIn(Node node, int delayMillis) {
        return scaleFromTo(node, 0.95, 1.0, delayMillis);
    }

    public static ScaleTransition explodeOut(Node node) {
        return scaleFromTo(node, 1.0, 1.05, 0);
    }

    private static ScaleTransition scaleFromTo(Node node, double from, double to, int delayMillis) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(UI_ANIMATION_TIME_MSEC / 2), node);
        scale.setFromX(from);
        scale.setFromY(from);
        scale.setToX(to);
        scale.setToY(to);
        scale.setDelay(Duration.millis(delayMillis));
        scale.play();
        return scale;
    }

    public static void checkGuiThread() {
        checkState(Platform.isFxApplicationThread());
    }
}
