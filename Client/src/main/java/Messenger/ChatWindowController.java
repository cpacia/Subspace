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
    private String fromAddress;
    private KeyRing.Key fromKey;
    private Stage stage;
    private FileWriter fileWriter = new FileWriter();
    private boolean scrollToBottom = false;
    private Label lblTo = new Label();

    public void setStage(Stage stage){
        this.stage = stage;
        stage.setOnCloseRequest(event -> {
            Main.controller.removeChatWindow(conversationID);
        });
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

    public void setConversation(String conversationID){
        History.ChatConversation conversation = fileWriter.getConversation(conversationID);
        txtAddress.setText(conversation.getTheirAddress());
        fromAddress = conversation.getMyAddress();
        setupPane2();
        for (History.ChatMessage m : conversation.getChatMessageList()){
            if (!m.getSentFromMe()){
                showIncomingMessage(m.getContent(), m.getName(), conversation.getTheirAddress());
            }
            else {
                showOutGoingMessage(m.getContent());
            }
        }
    }

    @FXML
    void send(ActionEvent e){
        sendMessage();
    }

    void sendMessage(){
        Message m;
        if (paneOne.isVisible()){
            this.fromAddress = cbAddrs.getValue().toString();
            setupPane2();
            m = new Message(toAddress, txtArea1.getText(), fromKey, Payload.MessageType.CHAT);
            showOutGoingMessage(txtArea1.getText());
        }
        else {
            m = new Message(toAddress, txtArea2.getText(), fromKey, Payload.MessageType.CHAT);
            showOutGoingMessage(txtArea2.getText());
        }
        m.send();
        if (!fileWriter.conversationExists(conversationID)) {
            fileWriter.newChatConversation(conversationID, m,
                    "", m.getToAddress().toString(), m.getFromAddress(), true);
        }
        else {
            fileWriter.addChatMessage(conversationID, m, true);
        }
        MessageListener l = Main.controller.getListener();
        l.onMessageSent(m);
        txtArea2.clear();
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
        fromKey = fileWriter.getKeyFromAddress(fromAddress);
        conversationID = this.fromKey.getAddress() + txtAddress.getText().toString();
        Main.controller.addChatWindow(conversationID);
        paneOne.setVisible(false);
        paneTwo.setVisible(true);
        lblTo.setText("  " + toAddress.toString());
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
                    String cID = m.getToAddress().toString() + m.getFromAddress();
                    if (cID.equals(conversationID) && m.getMessageType()== Payload.MessageType.CHAT) {
                        showIncomingMessage(m.getDecryptedMessage(), m.getSenderName(), m.getFromAddress());
                    }
                }
            });
        }

        public void onMessageSent(Message m) {}

        public void onDataReceived(int bytes) {}
    }

    private void showIncomingMessage(String content, String senderName, String senderAddr){
        HBox h = new HBox();
        Label lblMessage = new Label(content);
        lblMessage.setMaxWidth(400);
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-text-fill: #00d0d0; -fx-font-size: 16;");
        lblMessage.setPadding(new Insets(0, 0, 10, 10));
        lblMessage.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER_LEFT);
        Label lblFromName = new Label(senderName);
        lblFromName.setWrapText(true);
        lblFromName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblFromName.setPadding(new Insets(10, 0, 0, 10));
        v.getChildren().addAll(lblFromName, lblMessage);
        ImageView imView = null;
        try {
            imView = Identicon.generate(senderAddr, Color.decode("#393939"));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        imView.setFitWidth(35);
        imView.setFitHeight(35);
        HBox imageHBox = new HBox();
        imageHBox.getChildren().add(imView);
        imageHBox.setPadding(new Insets(11, 0, 0, 0));
        h.getChildren().addAll(imageHBox, v);
        h.setAlignment(Pos.TOP_LEFT);
        h.setPrefWidth(590);
        h.setPadding(new Insets(0, 0, 0, 20));
        lblTo.setText("  " + senderName);
        scrollVBox.getChildren().add(h);
        stage.setTitle("Chat with " + senderName);
        scrollPane.setVvalue(scrollPane.getVmax());
        scrollToBottom = true;
    }

    private void showOutGoingMessage(String content){
        HBox h = new HBox();
        Label lblMessage = new Label(content);
        lblMessage.setMaxWidth(400);
        lblMessage.setWrapText(true);
        lblMessage.setStyle("-fx-text-fill: #a7ec21; -fx-font-size: 16;");
        lblMessage.setPadding(new Insets(0, 10, 10, 0));
        lblMessage.setAlignment(Pos.CENTER_RIGHT);
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER_RIGHT);
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
        h.setAlignment(Pos.TOP_RIGHT);
        h.setPrefWidth(590);
        scrollVBox.getChildren().add(h);
        scrollPane.setVvalue(scrollPane.getVmax());
        scrollToBottom = true;
    }
}
