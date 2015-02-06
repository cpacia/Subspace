package Messenger;

import Messenger.Utils.Identicon.Identicon;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.List;


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
    ChoiceBox cbAddrs;

    private String conversationID;
    private VBox scrollVBox = new VBox();
    private boolean shift = false;
    private Address toAddress;
    private KeyRing.Key fromKey;
    private Stage stage;
    private FileWriter fileWriter = new FileWriter();
    private String toName;
    private boolean iSentLastMessage=false;
    private boolean scrollToBottom = false;
    private Label lblTo;

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void initialize(){
        ChatMessageListener listener = new ChatMessageListener();
        Main.retriever.addListener(listener);
        ObservableList<String> cbData = FXCollections.observableArrayList("Select your from address");
        FileWriter f = new FileWriter();
        if (f.hasKeys()) {
            for (KeyRing.Key key : f.getSavedKeys()) {
                cbData.add(key.getAddress());
            }
        }
        cbAddrs.setItems(cbData);
        cbAddrs.getSelectionModel().selectFirst();
        scrollPane.setContent(scrollVBox);
        txtArea1.setWrapText(true);
        txtArea2.setWrapText(true);
        txtArea1.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (txtArea1.getText().equals("")) {
                        txtArea1.clear();
                        keyEvent.consume();
                    } else {
                        if (!shift) {
                            if (cbAddrs.getSelectionModel().getSelectedIndex() == 0) {
                                cbAddrs.setStyle("-fx-background-color: #393939; -fx-border-color: #f92672;");
                            }
                            if (!btnSend.isDisabled()) {
                                sendMessage();
                            }
                        } else {
                            String s = txtArea1.getText();
                            txtArea1.setText(s + "\n");
                            txtArea1.positionCaret(txtArea1.getLength());
                        }
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
        txtArea1.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                if (!txtArea1.getText().equals("") && cbAddrs.getSelectionModel().getSelectedIndex() != 0 && Address.validateAddress(txtAddress.getText())){
                    btnSend.setDisable(false);
                }
                else {
                    btnSend.setDisable(true);
                }
            }
        });
        txtArea2.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (txtArea2.getText().equals("")) {
                        txtArea2.clear();
                        keyEvent.consume();
                    } else {
                        if (!shift) {
                            sendMessage();
                            keyEvent.consume();
                        } else {
                            String s = txtArea2.getText();
                            txtArea2.setText(s + "\n");
                            txtArea2.positionCaret(txtArea2.getLength());
                        }
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
                        if (cbAddrs.getSelectionModel().getSelectedIndex()!=0 && !txtArea1.getText().equals("")){btnSend.setDisable(false);}
                        txtAddress.setStyle("-fx-background-color: #393939; -fx-text-fill: #f92672; -fx-border-color: #00d0d0;");
                    }
                    else {
                        btnSend.setDisable(true);
                        txtAddress.setStyle("-fx-background-color: #393939; -fx-text-fill: #b4b5b1; -fx-border-color: #f92672;");
                    }
                }
            }
        });
        cbAddrs.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                if((Integer)number2 != 0){cbAddrs.setStyle("-fx-background-color: #393939;-fx-border-color: #00d0d0;");}
                if((Integer)number2!=0 && Address.validateAddress(txtAddress.getText()) && !txtArea1.getText().equals("")){
                    btnSend.setDisable(false);
                }
                else {btnSend.setDisable(true);}
            }
        });

        scrollPane.vvalueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if(scrollToBottom) {
                    scrollPane.setVvalue(scrollPane.getVmax());
                    scrollToBottom = false;
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
            lblMessage.setMaxWidth(400);
            lblMessage.setWrapText(true);
            lblMessage.setStyle("-fx-text-fill: #a7ec21; -fx-font-size: 16;");
            lblMessage.setPadding(new Insets(0, 10, 10, 0));
            lblMessage.setAlignment(Pos.CENTER_RIGHT);
            VBox v = new VBox();
            v.setAlignment(Pos.CENTER_RIGHT);
            if (!iSentLastMessage) {
                Label lblFromName = new Label(fromKey.getName());
                lblFromName.setWrapText(true);
                lblFromName.setStyle("-fx-text-fill: #f92672; -fx-font-size: 16;");
                lblFromName.setPadding(new Insets(10, 10, 0, 0));
                v.getChildren().addAll(lblFromName, lblMessage);
                ImageView imView = null;
                try {imView = Identicon.generate(fromKey.getAddress(), Color.decode("#393939"));
                } catch (Exception e1) {e1.printStackTrace();}
                imView.setFitWidth(35);
                imView.setFitHeight(35);
                HBox imageHBox = new HBox();
                imageHBox.getChildren().add(imView);
                imageHBox.setPadding(new Insets(11, 0, 0, 0));
                h.getChildren().addAll(v, imageHBox);
            } else {h.getChildren().add(lblMessage);}
            h.setAlignment(Pos.TOP_RIGHT);
            h.setPrefWidth(590);
            scrollVBox.getChildren().add(h);
            scrollPane.setVvalue(scrollPane.getVmax());
            scrollToBottom = true;

            Message m = new Message(toAddress, txtArea2.getText(), fromKey, Payload.MessageType.CHAT);
            m.send();
            MessageListener l = Main.controller.getListener();
            fileWriter.addChatMessage(conversationID, m, true);
            l.onMessageSent(m);
            txtArea2.clear();
            iSentLastMessage = true;

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
        try{toAddress = new Address(txtAddress.getText().toString());}
        catch(AddressFormatException e){e.printStackTrace();}
        fromKey = fileWriter.getKeyFromAddress(cbAddrs.getValue().toString());
        conversationID = this.fromKey.getAddress() + txtAddress.getText().toString();
        Message m = new Message(toAddress, txtArea1.getText(), fromKey, Payload.MessageType.CHAT);
        m.send();
        MessageListener l = Main.controller.getListener();
        l.onMessageSent(m);
        fileWriter.newChatConversation(conversationID, m);

        iSentLastMessage = true;
        paneOne.setVisible(false);
        paneTwo.setVisible(true);
        HBox h = new HBox();
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER_RIGHT);
        Label lblMessage = new Label(txtArea1.getText());
        lblMessage.setMaxWidth(400);
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-text-fill: #a7ec21; -fx-font-size: 16;");
        lblMessage.setPadding(new Insets(0, 10, 10, 0));
        lblMessage.setAlignment(Pos.CENTER_RIGHT);
        Label lblFromName = new Label(fromKey.getName());
        lblFromName.setWrapText(true);
        lblFromName.setStyle("-fx-text-fill: #f92672; -fx-font-size: 16;");
        lblFromName.setPadding(new Insets(10, 10, 0, 0));
        v.getChildren().addAll(lblFromName, lblMessage);
        ImageView imView = null;
        try {imView = Identicon.generate(fromKey.getAddress(), Color.decode("#393939"));
        } catch (Exception e1) {e1.printStackTrace();}
        imView.setFitWidth(35);
        imView.setFitHeight(35);
        HBox imageHBox = new HBox();
        imageHBox.getChildren().add(imView);
        imageHBox.setPadding(new Insets(11, 0, 0, 0));
        h.getChildren().addAll(v, imageHBox);
        h.setAlignment(Pos.CENTER_RIGHT);
        h.setPrefWidth(590);
        scrollVBox.getChildren().add(h);
        lblTo = new Label("  " + toAddress.toString());
        lblTo.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16");
        ImageView imView2 = null;
        try{imView2 = Identicon.generate(toAddress.toString(), Color.decode("#4d5052"));}
        catch (Exception e1){e1.printStackTrace();}
        imView2.setFitWidth(40);
        imView2.setFitHeight(40);
        hBox.getChildren().addAll(imView2, lblTo);
        hBox.setMargin(imView2, new Insets(5, 0, 0, 15));
        hBox.setMargin(lblTo, new Insets(15, 0, 0, 0));
        this.stage.setTitle("Chat message");
    }


    private class ChatMessageListener implements MessageListener {
        public void onMessageReceived(Message m) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    System.out.println(m.getToAddress().toString());
                    System.out.println(fromKey.getAddress());
                    if (m.getToAddress().toString().equals(fromKey.getAddress()) && m.getMessageType()== Payload.MessageType.CHAT) {
                        HBox h = new HBox();
                        Label lblMessage = new Label(m.getDecryptedMessage());
                        lblMessage.setMaxWidth(400);
                        lblMessage.setWrapText(true);
                        lblMessage.setStyle("-fx-text-fill: #a7ec21; -fx-font-size: 16;");
                        lblMessage.setPadding(new Insets(0, 0, 10, 10));
                        lblMessage.setAlignment(Pos.CENTER_LEFT);
                        VBox v = new VBox();
                        v.setAlignment(Pos.CENTER_LEFT);
                        if (iSentLastMessage){
                            Label lblFromName = new Label(m.getSenderName());
                            lblFromName.setWrapText(true);
                            lblFromName.setStyle("-fx-text-fill: #f92672; -fx-font-size: 16;");
                            lblFromName.setPadding(new Insets(10, 0, 0, 10));
                            v.getChildren().addAll(lblFromName, lblMessage);
                            ImageView imView = null;
                            try {
                                imView = Identicon.generate(m.getFromAddress(), Color.decode("#393939"));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            imView.setFitWidth(35);
                            imView.setFitHeight(35);
                            HBox imageHBox = new HBox();
                            imageHBox.getChildren().add(imView);
                            imageHBox.setPadding(new Insets(11, 0, 0, 0));
                            h.getChildren().addAll(imageHBox, v);
                        } else {h.getChildren().add(lblMessage);}
                        h.setAlignment(Pos.TOP_LEFT);
                        h.setPrefWidth(590);
                        h.setPadding(new Insets(0, 0, 0, 20));
                        lblTo.setText("  " + m.getSenderName());
                        scrollVBox.getChildren().add(h);
                        stage.setTitle("Chat with " + m.getSenderName());
                        iSentLastMessage = false;
                        scrollPane.setVvalue(scrollPane.getVmax());
                        scrollToBottom = true;
                        fileWriter.addChatMessage(conversationID, m, false);
                    }
                }
            });
        }

        public void onMessageSent(Message m) {}

        public void onDataReceived(int bytes) {}
    }
}
