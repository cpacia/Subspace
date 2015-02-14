package Messenger;

import Messenger.Utils.Base58Check;
import Messenger.Utils.Identicon.Identicon;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.*;
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
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Utils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

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
    @FXML
    TextArea txtArea;
    @FXML
    Label lblRoomName;
    @FXML
    ScrollPane scrollPane;
    @FXML
    ChoiceBox addressChoiceBox;
    @FXML
    ChoiceBox cbNode;

    private Stage stage;
    private FileWriter fileWriter;
    private String chatRoomName;
    private boolean shift = false;
    private VBox scrollVBox = new VBox();
    private boolean scrollToBottom = false;
    private Address roomAddress;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initialize() {
        fileWriter = new FileWriter();
        scrollPane.setContent(scrollVBox);
        ObservableList<String> cbData = FXCollections.observableArrayList("Select your from address");
        if (fileWriter.hasKeys()) {
            for (KeyRing.Key key : fileWriter.getSavedKeys()) {
                String content = key.getName() + " <" + key.getAddress() + ">";
                cbData.add(content);
            }
        }
        cbNode.setItems(FXCollections.observableArrayList(
                        "bitcoinauthenticator.org", "localhost")
        );
        cbNode.getSelectionModel().selectFirst();
        addressChoiceBox.setItems(cbData);
        addressChoiceBox.getSelectionModel().selectFirst();
        txtArea.setWrapText(true);
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
        txtRoomKey.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    rbPrivate.setSelected(true);
                }
            }
        });
        txtRoomName.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    rbPublic.setSelected(true);
                }
            }
        });
        txtArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (txtArea.getText().equals("")) {
                        txtArea.clear();
                        keyEvent.consume();
                    } else {
                        if (!shift) {
                            if (addressChoiceBox.getSelectionModel().getSelectedIndex() != 0) {
                                sendMessage();
                            }
                            keyEvent.consume();
                        } else {
                            String s = txtArea.getText();
                            txtArea.setText(s + "\n");
                            txtArea.positionCaret(txtArea.getLength());
                        }
                    }
                }
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift = true;
                }
            }
        });
        txtArea.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.SHIFT) {
                    shift = false;
                }
            }
        });
        scrollPane.vvalueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (scrollToBottom) {
                    scrollPane.setVvalue(scrollPane.getVmax());
                    scrollToBottom = false;
                }
            }
        });
        txtRoomName.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                if (!txtRoomName.getText().equals("") && rbPublic.isSelected()) {
                    btnDone.setDisable(false);
                } else {
                    btnDone.setDisable(true);
                }
            }
        });
        txtRoomKey.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                if (!txtRoomKey.getText().equals("") && rbPrivate.isSelected()) {
                    btnDone.setDisable(false);
                } else {
                    btnDone.setDisable(true);
                }
            }
        });
        blurOut(chatRoomPane);
        addPane.setVisible(true);
        fadeIn(addPane);
        final ToggleGroup group = new ToggleGroup();
        rbPublic.setToggleGroup(group);
        rbPrivate.setToggleGroup(group);
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            public void changed(ObservableValue<? extends Toggle> ov,
                                Toggle old_toggle, Toggle new_toggle) {
                if (group.getSelectedToggle() != null) {
                    if (rbPublic.isSelected() && !txtRoomName.getText().equals("")) {
                        btnDone.setDisable(false);
                    } else if (rbPrivate.isSelected() && !txtRoomKey.getText().equals("")) {
                        btnDone.setDisable(false);
                    } else {
                        btnDone.setDisable(true);
                    }
                }
            }
        });
    }

    public void setChatRoom(int index) {
        blurIn(chatRoomPane);
        fadeOut(addPane);
        addPane.setVisible(false);
        this.chatRoomName = fileWriter.getChatRooms().get(index).getRoomName();
        setRoomUI();
    }

    @FXML
    void doneButtonPressed(MouseEvent e) {
        btnDone.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void doneButtonReleased(MouseEvent e) {
        btnDone.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void createRoom(ActionEvent e) {
        if (rbPrivate.isSelected()) {
            try {
                byte[] roomKey = Base58Check.decode(txtRoomKey.getText());
                byte[] privKey = Utils.doubleDigest(roomKey);
                byte[] fingerprint = Utils.sha256hash160(privKey);
                ECKey ecKey = ECKey.fromPrivOnly(privKey);
                String roomName = "#PrivateRoom_" + Hex.encodeHexString(Arrays.copyOfRange(fingerprint, 0, 4));
                Address addr = null;
                try {addr = new Address(32, ecKey);}
                catch (InvalidPrefixLengthException e2) {e2.printStackTrace();}
                fileWriter.addKey(ecKey, roomName, 32, addr.toString(),
                        cbNode.getValue().toString(), null);
                fileWriter.addChatRoom(roomName, false);
                Main.retriever.addWatchKey(fileWriter.getKeyFromAddress(addr.toString()));
                Main.controller.addChatRoom(roomName);
                blurIn(chatRoomPane);
                fadeOut(addPane);
                addPane.setVisible(false);
                this.chatRoomName = roomName;
                setRoomUI();
            } catch (AddressFormatException e2) {
                Dialogs.create()
                        .owner(stage)
                        .title("Error")
                        .masthead("You entered an invalid room key.")
                        .message("Room keys must be at least 128 bits and in Base58Check format.")
                        .showError();
            }
        } else {
            String roomName = "#" + txtRoomName.getText().toLowerCase();
            byte[] privKey = Utils.doubleDigest(roomName.getBytes());
            ECKey ecKey = ECKey.fromPrivOnly(privKey);
            Address addr = null;
            try {
                addr = new Address(32, ecKey);
            } catch (InvalidPrefixLengthException e2) {
                e2.printStackTrace();
            }
            fileWriter.addKey(ecKey, roomName, 32, addr.toString(),
                    cbNode.getValue().toString(), null);
            fileWriter.addChatRoom(roomName, false);
            Main.retriever.addWatchKey(fileWriter.getKeyFromAddress(addr.toString()));
            Main.controller.addChatRoom(roomName);
            blurIn(chatRoomPane);
            fadeOut(addPane);
            addPane.setVisible(false);
            this.chatRoomName = roomName;
            setRoomUI();
        }
    }

    private class ChatRoomListener implements MessageListener {
        @Override
        public void onMessageReceived(Message m) {
            if (!fileWriter.keyExists(m.getFromAddress())) {
                String senderName = m.getSenderName();
                if (fileWriter.contactExists(m.getFromAddress())) {
                    senderName = fileWriter.getNameForContact(m.getFromAddress());
                }
                showMessage(m.getDecryptedMessage(), senderName, m.getFromAddress(), null, false);
            }
        }

        @Override
        public void onMessageSent(Message m) {

        }

        @Override
        public void onDataReceived(int bytes) {

        }
    }

    private void setRoomUI() {
        ECKey ecKey = ECKey.fromPrivOnly(fileWriter.getKeyFromName(this.chatRoomName).getPrivateKey().toByteArray());
        try {
            this.roomAddress = new Address(32, ecKey);
        } catch (InvalidPrefixLengthException e) {
            e.printStackTrace();
        }
        this.stage.setTitle(this.chatRoomName);
        lblRoomName.setText(this.chatRoomName);
        lblRoomName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16");
        ChatRoomListener listener = new ChatRoomListener();
        Main.retriever.addListener(listener);
        loadMessages();
    }

    private void sendMessage() {
        String fromaddr = addressChoiceBox.getValue().toString().substring(
                addressChoiceBox.getValue().toString().indexOf("<") + 1,
                addressChoiceBox.getValue().toString().length() - 1);
        KeyRing.Key fromKey = fileWriter.getKeyFromAddress(fromaddr);
        showMessage(txtArea.getText(), fromKey.getName(), fromKey.getAddress(), fromKey, true);
        Message m = new Message(roomAddress, txtArea.getText(), fromKey,
                Payload.MessageType.CHATROOM, null);
        m.send();
        txtArea.clear();
    }

    private void showMessage(String content, String name, String address,
                             @Nullable KeyRing.Key fromKey, boolean isSentFromMe) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                HBox h = new HBox();
                Label lblMessage = new Label(content);
                lblMessage.setMaxWidth(595);
                lblMessage.setWrapText(true);
                if (isSentFromMe) {
                    lblMessage.setStyle("-fx-text-fill: #a7ec21; -fx-font-size: 12;");
                } else {
                    lblMessage.setStyle("-fx-text-fill: #00d0d0; -fx-font-size: 12;");
                }
                lblMessage.setPadding(new javafx.geometry.Insets(0, 0, 6, 10));
                lblMessage.setAlignment(Pos.CENTER_LEFT);
                VBox v = new VBox();
                v.setAlignment(Pos.CENTER_LEFT);
                Label lblFromName = new Label(name);
                lblFromName.setWrapText(true);
                if (isSentFromMe) {
                    lblFromName.setStyle("-fx-text-fill: #f92672; -fx-font-size: 12;");
                } else {
                    lblFromName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 12;");
                }
                lblFromName.setPadding(new javafx.geometry.Insets(12, 0, 0, 10));
                v.getChildren().addAll(lblFromName, lblMessage);
                ImageView imView = null;
                if (isSentFromMe) {
                    if (fromKey.hasOpenname()) {
                        File f = new File(Main.params.getApplicationDataFolder() + "/avatars/" + fromKey.getOpenname() + ".jpg");
                        javafx.scene.image.Image image = new javafx.scene.image.Image(f.toURI().toString());
                        imView = new ImageView(image);
                    } else {
                        try {
                            imView = Identicon.generate(address, Color.decode("#393939"));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    if (fileWriter.contactExists(address) && fileWriter.hasOpenname(address)) {
                        File f = new File(Main.params.getApplicationDataFolder() + "/avatars/" + fileWriter.getOpenname(address) + ".jpg");
                        javafx.scene.image.Image image = new javafx.scene.image.Image(f.toURI().toString());
                        imView = new ImageView(image);
                    } else {
                        try {
                            imView = Identicon.generate(address, Color.decode("#393939"));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                imView.setFitWidth(30);
                imView.setFitHeight(30);
                HBox imageHBox = new HBox();
                imageHBox.getChildren().add(imView);
                imageHBox.setPadding(new javafx.geometry.Insets(11, 0, 0, 0));
                h.getChildren().addAll(imageHBox, v);
                h.setAlignment(Pos.TOP_LEFT);
                h.setPrefWidth(595);
                h.setPadding(new javafx.geometry.Insets(0, 0, 0, 20));
                scrollVBox.getChildren().add(h);
                scrollPane.setVvalue(scrollPane.getVmax());
                scrollToBottom = true;
            }
        });
    }

    private void loadMessages(){
        List<History.RoomMessage> messages = fileWriter.getChatRoomMessages(this.chatRoomName);
        for (History.RoomMessage m : messages){
            if (!fileWriter.keyExists(m.getSenderAddress())){
                showMessage(m.getContent(), m.getSenderName(), m.getSenderAddress(), null, false);
            } else {
                KeyRing.Key fromKey = fileWriter.getKeyFromAddress(m.getSenderAddress());
                showMessage(m.getContent(), fromKey.getName() , m.getSenderAddress(), fromKey, true);
            }
        }
    }
}