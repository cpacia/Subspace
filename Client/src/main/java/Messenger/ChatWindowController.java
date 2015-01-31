package Messenger;

import Messenger.Utils.Identicon.Identicon;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bitcoinj.core.AddressFormatException;

import java.awt.*;


/**
 * Created by chris on 1/31/15.
 */
public class ChatWindowController {

    @FXML
    Pane paneOne;
    @FXML
    Pane paneTwo;
    @FXML
    Button btnSend;
    @FXML
    TextArea txtArea1;
    @FXML
    TextArea txtArea2;
    @FXML
    TextField txtAddress;
    @FXML
    HBox hBox;
    @FXML
    ScrollPane scrollPane;
    @FXML
    VBox scrollVBox = new VBox();

    private boolean shift = false;
    private Address toAddress;
    private Stage stage;

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void initialize(){
        scrollPane.setContent(scrollVBox);
        txtArea1.setWrapText(true);
        txtArea2.setWrapText(true);
        txtArea1.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (!shift){
                        if(!btnSend.isDisabled()){sendMessage();}
                    }
                    else {
                        String s = txtArea1.getText();
                        txtArea1.setText(s + "\n");
                        txtArea1.positionCaret(txtArea1.getLength());
                    }
                }
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift=true;
                }
            }
        });
        txtArea1.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift = false;
                }
            }
        });
        txtArea2.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (!shift){
                        sendMessage();
                        keyEvent.consume();
                    }
                    else {
                        String s = txtArea2.getText();
                        txtArea2.setText(s + "\n");
                        txtArea2.positionCaret(txtArea2.getLength());
                    }
                }
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift=true;
                }
            }
        });
        txtArea2.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift = false;
                }
            }
        });
        txtAddress.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
            {
                if (!newPropertyValue) {
                    if(Address.validateAddress(txtAddress.getText())){
                        btnSend.setDisable(false);
                        txtAddress.setStyle("-fx-background-color: #393939; -fx-text-fill: #f92672; -fx-border-color: #00d0d0;");
                    }
                    else {
                        btnSend.setDisable(true);
                        txtAddress.setStyle("-fx-background-color: #393939; -fx-text-fill: #b4b5b1; -fx-border-color: #f92672;");
                    }
                }
            }
        });
    }

    @FXML
    void send(ActionEvent e){
        sendMessage();
    }

    void sendMessage(){
        if (paneOne.isVisible()){setupPane2();}
        else {
            HBox h = new HBox();
            Label lblMessage = new Label(txtArea2.getText());
            lblMessage.setMaxWidth(600);
            lblMessage.setWrapText(true);
            h.getChildren().add(lblMessage);
            scrollVBox.getChildren().add(lblMessage);
            txtArea2.clear();
        }
    }

    @FXML
    void sendButtonPressed(MouseEvent e){
        btnSend.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void sendButtonReleased(MouseEvent e){
        btnSend.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    void setupPane2(){
        paneOne.setVisible(false);
        paneTwo.setVisible(true);
        HBox h = new HBox();
        Label lblMessage = new Label(txtArea1.getText());
        h.getChildren().add(lblMessage);
        scrollVBox.getChildren().add(lblMessage);
        try{toAddress = new Address(txtAddress.getText().toString());}
        catch(AddressFormatException e){e.printStackTrace();}
        Label lblTo = new Label("  " + toAddress.toString());
        lblTo.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16");
        ImageView imView = null;
        try{imView = Identicon.generate(toAddress.toString(), Color.decode("#4d5052"));}
        catch (Exception e1){e1.printStackTrace();}
        imView.setFitWidth(40);
        imView.setFitHeight(40);
        hBox.getChildren().addAll(imView, lblTo);
        hBox.setMargin(imView, new Insets(5, 0, 0, 15));
        hBox.setMargin(lblTo, new Insets(15, 0, 0, 0));
        this.stage.setTitle("Chat message");
    }
}
