package Messenger;

import Messenger.Utils.easing.GuiUtils;
import Messenger.Utils.easing.Identicon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.util.Callback;
import org.bouncycastle.util.encoders.Hex;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static Messenger.Utils.easing.GuiUtils.*;

/**
 * Created by chris on 1/27/15.
 */
public class AddressWindowController {
    @FXML
    Button btnAddAddress;
    @FXML
    Button btnAddContact;
    @FXML
    ListView addressList;
    @FXML
    ListView contactList;
    @FXML
    AnchorPane anchorPane;
    @FXML
    TabPane tabPane;
    @FXML
    Pane addAddressPane;
    @FXML
    Button btnAddressDone;
    @FXML
    Button btnAddressCancel;
    @FXML
    ChoiceBox cbNode;
    @FXML
    Slider prefixSlider;
    @FXML
    StackPane uiStack;
    @FXML
    TextField txtName;
    ObservableList<HBox> data;
    HBox init = new HBox();


    public void initialize() {
        data = FXCollections.observableArrayList();
        data.add(init);
        addressList.setItems(data);

    }

    @FXML
    void addButtonPress(MouseEvent e) {
        btnAddAddress.setLayoutY(270);
        btnAddContact.setLayoutY(270);
        btnAddAddress.setLayoutX(484);
        btnAddContact.setLayoutX(484);
    }

    @FXML
    void addButtonRelease(MouseEvent e) {
        btnAddAddress.setLayoutY(268);
        btnAddContact.setLayoutY(268);
        btnAddAddress.setLayoutX(483);
        btnAddContact.setLayoutX(483);
    }

    @FXML
    void newAddress(ActionEvent e) {
        blurOut(tabPane);
        addAddressPane.setVisible(true);
        fadeIn(addAddressPane);
        cbNode.setItems(FXCollections.observableArrayList(
                        "bitcoinauthenticator.org", "localhost")
        );
        cbNode.getSelectionModel().selectFirst();

    }

    @FXML
    void newContact(ActionEvent e) {
    }

    @FXML
    void addressCancelClicked(ActionEvent e) {
        fadeOut(addAddressPane);
        addAddressPane.setVisible(false);
        blurIn(tabPane);
    }

    @FXML
    void addressDoneClicked(ActionEvent e){
        Address addr = null;
        try{addr = new Address((int) prefixSlider.getValue());}
        catch (InvalidPrefixLengthException e2){e2.printStackTrace();}
        data.remove(init);
        Label lblAddress = new Label(addr.toString());
        lblAddress.setStyle("-fx-text-fill: #dc78dc;");
        ImageView imView = null;
        if ( (data.size()+1) % 2 == 0 ) {
            try{imView = Identicon.generate(addr.toString(),Color.decode("#3b3b3b"));}
            catch (Exception e1){e1.printStackTrace();}
        } else {
            try{imView = Identicon.generate(addr.toString(), Color.decode("#393939"));}
            catch (Exception e1){e1.printStackTrace();}
        }
        HBox hBox = new HBox();
        imView.setFitWidth(25);
        imView.setFitHeight(25);
        GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");
        Button btnCopy = new Button("", fontAwesome.create("COPY").color(javafx.scene.paint.Color.CYAN));
        Tooltip.install(btnCopy, new Tooltip("Copy address to clipboard"));
        btnCopy.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        btnCopy.setPrefSize(10, 10);
        btnCopy.setOnMousePressed(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
                hBox.setMargin(btnCopy, new Insets(-1, 2, 0, 10));
            }
        });
        btnCopy.setOnMouseReleased(new EventHandler<MouseEvent>(){
            @Override
            public void handle(MouseEvent t) {
                hBox.setMargin(btnCopy, new Insets(-2, 0, 0, 10));
            }
        });
        btnCopy.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(lblAddress.getText());
                clipboard.setContent(content);
            }
        });

        hBox.getChildren().addAll(imView, lblAddress, btnCopy);
        hBox.setMargin(lblAddress, new Insets(4, 0, 0, 10));
        hBox.setMargin(btnCopy, new Insets(-2, 0, 0, 10));
        lblAddress.setPrefWidth(485);
        data.add(hBox);
        fadeOut(addAddressPane);
        addAddressPane.setVisible(false);
        blurIn(tabPane);
        txtName.setText("");
    }

    @FXML
    void doneButtonPressed(MouseEvent e){
        btnAddressDone.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void doneButtonReleased(MouseEvent e){
        btnAddressDone.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void cancelButtonPressed(MouseEvent e){
        btnAddressCancel.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void cancelButtonReleased(MouseEvent e){
        btnAddressCancel.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

}