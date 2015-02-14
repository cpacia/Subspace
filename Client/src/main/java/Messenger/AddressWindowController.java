package Messenger;

import Messenger.Utils.Base58Check;
import Messenger.Utils.Identicon.Identicon;
import Messenger.Utils.openname.OpennameListener;
import Messenger.Utils.openname.OpennameUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.bouncycastle.util.encoders.Hex;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Optional;

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
    @FXML
    HBox addKeyHBox;
    @FXML
    Pane spinner;
    ObservableList<HBox> data;
    ObservableList<HBox> contacts;
    HBox init = new HBox();
    Stage stage;
    String importedPrivKey = "";

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public void initialize() {
        FileWriter f = new FileWriter();
        data = FXCollections.observableArrayList();
        if (!f.hasKeys()) {data.add(init);}
        else {
            for (KeyRing.Key key : f.getSavedKeys()){
                HBox node;
                if (key.hasOpenname()) {
                    node = getAddressListViewNode(key.getAddress(), key.getOpenname());
                }
                else{
                    node = getAddressListViewNode(key.getAddress(), key.getName());
                }
                data.add(node);
            }
        }
        addressList.setItems(data);
        contacts = FXCollections.observableArrayList();
        if (f.getContacts().size()==0) {contacts.add(init);}
        else {
            for (Contacts.Contact c : f.getContacts()){
                HBox node;
                if (c.hasOpenname()) {
                    node = getContactListViewNode(c.getAddress(), c.getName(), c.getOpenname());
                }
                else{
                    node = getContactListViewNode(c.getAddress(), c.getName(), null);
                }
                contacts.add(node);
            }
        }
        contactList.setItems(contacts);
        MenuItem copyPrivKey = new MenuItem("Copy private key to clipboard");
        MenuItem delete = new MenuItem("Delete");
        ContextMenu contextMenu = new ContextMenu(copyPrivKey, delete);
        addressList.setCellFactory(ContextMenuListCell.forListView(contextMenu));
        copyPrivKey.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                List<KeyRing.Key> keys = f.getSavedKeys();
                if (addressList.getSelectionModel().getSelectedIndex()<= keys.size()-1) {
                    KeyRing.Key keyToCopy = keys.get(addressList.getSelectionModel().getSelectedIndex());
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    String k = Hex.toHexString(keyToCopy.getPrivateKey().toByteArray());
                    content.putString(k);
                    clipboard.setContent(content);
                }
            }
        });
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                List<KeyRing.Key> keys = f.getSavedKeys();
                if (addressList.getSelectionModel().getSelectedIndex()<= keys.size()-1) {
                    Action response = Dialogs.create()
                            .owner(stage)
                            .title("Warning")
                            .style(DialogStyle.CROSS_PLATFORM_DARK)
                            .masthead("You are about to permanently delete a private key. " +
                                    "You will no longer be able to send or receive messages from this address.")
                            .message("Are you ok with this?")
                            .actions(Dialog.Actions.YES, Dialog.Actions.CANCEL)
                            .showError();

                    if (response == Dialog.Actions.YES) {
                        KeyRing.Key keyToRemove = keys.get(addressList.getSelectionModel().getSelectedIndex());
                        f.deleteKey(keyToRemove.getAddress());
                        addressList.getItems().clear();
                        if (!f.hasKeys()) {
                            data.add(init);
                        } else {
                            for (KeyRing.Key key : f.getSavedKeys()) {
                                HBox node;
                                if (key.hasOpenname()) {
                                    node = getAddressListViewNode(key.getAddress(), key.getOpenname());
                                }
                                else{
                                    node = getAddressListViewNode(key.getAddress(), key.getName());
                                }
                                data.add(node);
                            }
                        }
                    }
                }
            }
        });
        MenuItem deleteContact = new MenuItem("Delete");
        ContextMenu contextMenuContacts = new ContextMenu(deleteContact);
        contactList.setCellFactory(ContextMenuListCell.forListView(contextMenuContacts));
        deleteContact.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                List<Contacts.Contact> cl = f.getContacts();
                if (contactList.getSelectionModel().getSelectedIndex()<= cl.size()-1) {
                    Action response = Dialogs.create()
                            .owner(stage)
                            .title("Delete Contact")
                            .masthead("You are about to delete a contact.")
                            .style(DialogStyle.CROSS_PLATFORM_DARK)
                            .message("Are you ok with this?")
                            .actions(Dialog.Actions.YES, Dialog.Actions.CANCEL)
                            .showConfirm();

                    if (response == Dialog.Actions.YES) {
                        Contacts.Contact contactToRemove = cl.get(contactList.getSelectionModel().getSelectedIndex());
                        f.deleteContact(contactToRemove.getAddress());
                        contactList.getItems().clear();
                        if (f.getContacts().size()==0) {
                            data.add(init);
                        } else {
                            for (Contacts.Contact c : f.getContacts()) {
                                HBox node;
                                if (c.hasOpenname()) {
                                    node = getContactListViewNode(c.getAddress(), c.getName(), c.getOpenname());
                                }
                                else{
                                    node = getContactListViewNode(c.getAddress(), c.getName(), null);
                                }
                                contacts.add(node);
                            }
                        }
                    }
                }
            }
        });
        GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");
        Button btnKey = new Button("", fontAwesome.create("KEY").color(javafx.scene.paint.Color.CYAN));
        addKeyHBox.getChildren().add(btnKey);
        Tooltip.install(btnKey, new Tooltip("Import a private key"));
        btnKey.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        btnKey.setPrefSize(10, 10);
        btnKey.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                Optional<String> response = Dialogs.create()
                        .owner(stage)
                        .title("Import")
                        .style(DialogStyle.CROSS_PLATFORM_DARK)
                        .masthead("If you would like to import a private key, enter it below.")
                        .message("Private key (hex)")
                        .showTextInput("");

                response.ifPresent(key -> importedPrivKey = key);
            }
        });
        txtName.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                if (txtName.getText().equals("") || txtName.getText().substring(0,1).equals("#")) {
                    btnAddressDone.setDisable(true);
                }
                else {
                    btnAddressDone.setDisable(false);
                }
            }
        });

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
        txtName.setText("");
        importedPrivKey = "";
        btnAddressDone.setDisable(true);
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
        boolean createdAddressOK = true;
        Address addr = null;
        try {
            if (!importedPrivKey.equals("")) {
                ECKey importedKey = ECKey.fromPrivOnly(Hex.decode(importedPrivKey));
                try {
                    addr = new Address((int) prefixSlider.getValue(), importedKey);
                } catch (InvalidPrefixLengthException e2) {
                    e2.printStackTrace();
                }
            } else {
                try {
                    addr = new Address((int) prefixSlider.getValue());
                } catch (InvalidPrefixLengthException e2) {
                    e2.printStackTrace();
                }
            }
        } catch (Exception e2){
            createdAddressOK = false;
            e2.printStackTrace();
            Dialogs.create()
                    .owner(stage)
                    .title("Error")
                    .style(DialogStyle.CROSS_PLATFORM_DARK)
                    .masthead("Ooops, looks like you entered an invalid private key")
                    .showError();
        }
        if (createdAddressOK){finishUp(addr);}
    }

    private void finishUp(Address addr){
        if (txtName.getText().substring(0,1).equals("+")){
            spinner.setVisible(true);
            blurOut(addAddressPane);
            fadeIn(spinner);
            DownloadListener listener = new DownloadListener();
            OpennameUtils.downloadAvatar(txtName.getText().substring(1),
                    Main.params.getApplicationDataFolder().toString(),
                    listener, addr);
        }
        else {
            FileWriter writer = new FileWriter();
            writer.addKey(addr.getECKey(), txtName.getText(), (int) prefixSlider.getValue(), addr.toString(),
                    cbNode.getValue().toString(), null, null);
            Main.retriever.addWatchKey(writer.getKeyFromAddress(addr.toString()));
            data.remove(init);
            HBox hBox = getAddressListViewNode(addr.toString(), txtName.getText());
            data.add(hBox);
            fadeOut(addAddressPane);
            addAddressPane.setVisible(false);
            blurIn(tabPane);
        }
    }

    private class DownloadListener implements OpennameListener {
        public void onDownloadComplete(Address addr, String formatted) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    FileWriter writer = new FileWriter();
                    writer.addKey(addr.getECKey(), formatted, (int) prefixSlider.getValue(),
                            addr.toString(), cbNode.getValue().toString(), txtName.getText().substring(1), null);
                    Main.retriever.addWatchKey(writer.getKeyFromAddress(addr.toString()));
                    data.remove(init);
                    HBox hBox = getAddressListViewNode(addr.toString(), txtName.getText().substring(1));
                    data.add(hBox);
                    blurIn(addAddressPane);
                    fadeOut(addAddressPane);
                    fadeOut(spinner);
                    spinner.setVisible(false);
                    addAddressPane.setVisible(false);
                    blurIn(tabPane);
                }
            });
        }
        public void onDownloadFailed() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    fadeOut(spinner);
                    blurIn(addAddressPane);
                    Dialogs.create()
                            .owner(stage)
                            .title("Error")
                            .style(DialogStyle.CROSS_PLATFORM_DARK)
                            .masthead("Unable to download your openname profile")
                            .showError();
                }
            });
        }
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

    HBox getAddressListViewNode(String address, String name){
        Label lblAddress = new Label(address);
        lblAddress.setStyle("-fx-text-fill: #dc78dc;");
        ImageView imView = null;
        File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+name+".jpg");
        if (file.exists()){
            Image image = new Image(file.toURI().toString());
            imView = new ImageView(image);
        }
        else {
            if ((data.size() + 1) % 2 == 0) {
                try {
                    imView = Identicon.generate(address, Color.decode("#3b3b3b"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                try {
                    imView = Identicon.generate(address, Color.decode("#393939"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
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
        lblAddress.setPrefWidth(482);
        return hBox;
    }

    HBox getContactListViewNode(String address, String name, @Nullable String openname){
        Label lblAddress = new Label(name + " <" + address + ">");
        lblAddress.setStyle("-fx-text-fill: #dc78dc;");
        ImageView imView = null;
        File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+openname+".jpg");
        if (file.exists()){
            Image image = new Image(file.toURI().toString());
            imView = new ImageView(image);
        }
        else {
            if ((data.size() + 1) % 2 == 0) {
                try {
                    imView = Identicon.generate(address, Color.decode("#3b3b3b"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                try {
                    imView = Identicon.generate(address, Color.decode("#393939"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
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
                String addr = lblAddress.getText().substring(
                        lblAddress.getText().indexOf("<")+1,
                        lblAddress.getText().length()-1);
                content.putString(addr);
                clipboard.setContent(content);
            }
        });

        hBox.getChildren().addAll(imView, lblAddress, btnCopy);
        hBox.setMargin(lblAddress, new Insets(4, 0, 0, 10));
        hBox.setMargin(btnCopy, new Insets(-2, 0, 0, 10));
        lblAddress.setPrefWidth(482);
        return hBox;
    }

    public static class ContextMenuListCell<T> extends ListCell<T> {

        public static <T> Callback<ListView<T>,ListCell<T>> forListView(ContextMenu contextMenu) {
            return forListView(contextMenu, null);
        }

        public static <T> Callback<ListView<T>,ListCell<T>> forListView(final ContextMenu contextMenu, final Callback<ListView<T>,ListCell<T>> cellFactory) {
            return new Callback<ListView<T>,ListCell<T>>() {
                @Override public ListCell<T> call(ListView<T> listView) {
                    ListCell<T> cell = cellFactory == null ? new DefaultListCell<T>() : cellFactory.call(listView);
                    cell.setContextMenu(contextMenu);
                    return cell;
                }
            };
        }

        public ContextMenuListCell(ContextMenu contextMenu) {
            setContextMenu(contextMenu);
        }
    }

    public static class DefaultListCell<T> extends ListCell<T> {
        @Override public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof Node) {
                setText(null);
                Node currentNode = getGraphic();
                Node newNode = (Node) item;
                if (currentNode == null || ! currentNode.equals(newNode)) {
                    setGraphic(newNode);
                }
            } else {
                setText(item == null ? "null" : item.toString());
                setGraphic(null);
            }
        }
    }

}