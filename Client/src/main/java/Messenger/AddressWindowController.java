package Messenger;

import Messenger.Utils.easing.GuiUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import javax.annotation.Nullable;
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
    StackPane uiStack;


    public void initialize() {
        ObservableList<String> data = FXCollections.observableArrayList();
        data.add("init");
        addressList.setItems(data);
        addressList.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
                                       @Override
                                       public ListCell<String> call(ListView<String> list) {
                                           return new AddressListCell();
                                       }
                                   }
        );
        contactList.setItems(data);
        contactList.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
                                       @Override
                                       public ListCell<String> call(ListView<String> list) {
                                           return new ContactListCell();
                                       }
                                   }
        );

    }

    static class AddressListCell extends ListCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            Pane pane = new Pane();
            pane.setPrefHeight(30);
            if (item!=null && item.equals("init")){

            }
            else {setGraphic(pane);}
        }
    }

    static class ContactListCell extends ListCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            Pane pane = new Pane();
            pane.setPrefHeight(30);
            if (item!=null && item.equals("init")){

            }
            else {setGraphic(pane);}
        }
    }

    @FXML
    void addButtonPress(MouseEvent e){
        btnAddAddress.setLayoutY(216);
        btnAddContact.setLayoutY(216);
        btnAddAddress.setLayoutX(376);
        btnAddContact.setLayoutX(376);
    }

    @FXML
    void addButtonRelease(MouseEvent e){
        btnAddAddress.setLayoutY(213);
        btnAddContact.setLayoutY(213);
        btnAddAddress.setLayoutX(375);
        btnAddContact.setLayoutX(375);
    }

    @FXML
    void newAddress(ActionEvent e){
        blurOut(tabPane);
        addAddressPane.setVisible(true);
        fadeIn(addAddressPane);
    }

    @FXML
    void newContact(ActionEvent e){

    }

    private Node stopClickPane = new Pane();

    public class OverlayUI<T> {
        public Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        public void show() {
            checkGuiThread();
            if (currentOverlay == null) {
                uiStack.getChildren().add(stopClickPane);
                uiStack.getChildren().add(ui);
                blurOut(anchorPane);
                //darken(mainUI);
                fadeIn(ui);
                //zoomIn(ui);
            } else {
                // Do a quick transition between the current overlay and the next.
                // Bug here: we don't pay attention to changes in outsideClickDismisses.
                explodeOut(currentOverlay.ui);
                fadeOutAndRemove(uiStack, currentOverlay.ui);
                uiStack.getChildren().add(ui);
                ui.setOpacity(0.0);
                fadeIn(ui, 100);
                zoomIn(ui, 100);
            }
            currentOverlay = this;
        }

        public void outsideClickDismisses() {
            stopClickPane.setOnMouseClicked((ev) -> done());
        }

        public void done() {
            checkGuiThread();
            if (ui == null) return;  // In the middle of being dismissed and got an extra click.
            explodeOut(ui);
            fadeOutAndRemove(uiStack, ui, stopClickPane);
            blurIn(anchorPane);
            //undark(mainUI);
            this.ui = null;
            this.controller = null;
            currentOverlay = null;
        }
    }

    @Nullable
    private OverlayUI currentOverlay;

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<T>(node, controller);
        // Auto-magically set the overlayUI member, if it's there.
        try {
            controller.getClass().getField("overlayUI").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /** Loads the FXML file with the given name, blurs out the main UI and puts this one on top. */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = Main.class.getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<T>(ui, controller);
            // Auto-magically set the overlayUI member, if it's there.
            try {
                if (controller != null)
                    controller.getClass().getField("overlayUI").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
                ignored.printStackTrace();
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

}
