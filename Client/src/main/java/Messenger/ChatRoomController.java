package Messenger;

import Messenger.Utils.Base58Check;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import javax.xml.soap.Text;
import java.security.SecureRandom;
import java.util.Optional;

import static Messenger.Utils.easing.GuiUtils.*;

/**
 * Created by chris on 2/13/15.
 */
public class ChatRoomController {

    @FXML
    Pane chatRoomPane;
    @FXML
    Pane addPane;
    @FXML
    RadioButton rbPublic;
    @FXML
    RadioButton rbPrivate;
    @FXML
    HBox randomHBox;
    @FXML
    TextField txtRoomKey;
    @FXML
    TextField txtRoomName;
    @FXML
    Button btnDone;

    private Stage stage;

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void initialize(){
        GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");
        Button btnRandom = new Button("", fontAwesome.create("RANDOM").color(javafx.scene.paint.Color.CYAN));
        randomHBox.getChildren().add(btnRandom);
        Tooltip.install(btnRandom, new Tooltip("Generate a new room key"));
        btnRandom.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        btnRandom.setPrefSize(10, 10);
        btnRandom.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                rbPrivate.setSelected(true);
                byte[] key = new byte[16];
                Digest digest = new SHA256Digest();
                HKDFBytesGenerator kDF1BytesGenerator = new HKDFBytesGenerator(digest);
                kDF1BytesGenerator.init(new HKDFParameters(new SecureRandom().generateSeed(32),
                        null, null));
                kDF1BytesGenerator.generateBytes(key, 0, 16);
                String base58key = Base58Check.encode(Hex.encodeHexString(key));
                txtRoomKey.setText(base58key);
            }
        });
        txtRoomKey.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (newPropertyValue){rbPrivate.setSelected(true);}
            }
        });
        txtRoomName.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (newPropertyValue){rbPublic.setSelected(true);}
            }
        });
        blurOut(chatRoomPane);
        addPane.setVisible(true);
        fadeIn(addPane);
        final ToggleGroup group = new ToggleGroup();
        rbPublic.setToggleGroup(group);
        rbPrivate.setToggleGroup(group);
    }

    @FXML
    void doneButtonPressed(MouseEvent e){
        btnDone.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void doneButtonReleased(MouseEvent e){
        btnDone.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

}
