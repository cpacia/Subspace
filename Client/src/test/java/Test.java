import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by chris on 2/2/15.
 */
public class Test {
    public static void main (String[] args) {
        HBox h = new HBox();
        h.getChildren().addAll(new Label("sadfsdf"));
        for (Node n : h.getChildren()){
            System.out.println(n.toString());
        }

    }
}
