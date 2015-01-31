package Messenger;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.animation.ParallelTransition;
import Messenger.Utils.easing.EasingMode;
import Messenger.Utils.easing.ElasticInterpolator;
import org.controlsfx.control.PopOver;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;


public class Controller {

    @FXML
    ListView chatList;
    @FXML
    ProgressBar torProgressBar;
    @FXML
    Label torLabel;
    @FXML
    ImageView torIcon;
    @FXML
    Button btnChat;
    @FXML
    Button btnEmail;
    @FXML
    Button btnChannel;
    @FXML
    Button btnAddress;
    @FXML
    Label lblNewMessage;
    PopOver pop;

    public void initialize() {
        ObservableList<String> data = FXCollections.observableArrayList();
        data.add("init");
        chatList.setItems(data);
        chatList.setCellFactory(new Callback<ListView<String>,ListCell<String>>() {
                                    @Override
                                    public ListCell<String> call(ListView<String> list) {
                                        return new ChatListCell();
                                    }
                                }
        );
        TorListener listener = new TorListener();
        TorClient tor = Main.torClient;
        if(tor != null) {
            tor.addInitializationListener(listener);
        }
        lblNewMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Parent root;
                try {
                    URL location = getClass().getResource("chat.fxml");
                    FXMLLoader loader = new FXMLLoader(location);
                    AnchorPane addressUI = (AnchorPane) loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("New chat message");
                    stage.setX(700);
                    stage.setY(200);
                    stage.setScene(new Scene(addressUI, 642, 429));
                    stage.setResizable(false);
                    ChatWindowController controller = (ChatWindowController) loader.getController();
                    controller.setStage(stage);
                    String file = Main.class.getResource("gui.css").toString();
                    stage.getScene().getStylesheets().add(file);
                    stage.show();

                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        });

    }

    static class ChatListCell extends ListCell<String> {
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            Pane pane = new Pane();
            pane.setPrefHeight(68);
            if (item!=null && item.equals("init")){

            }
            else {setGraphic(pane);}
        }
    }

    public void readyToGoAnimation() {
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), torProgressBar);
        leave.setByY(80.0);
        leave.play();
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), torLabel);
        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive.setToY(-30.0);
        TranslateTransition arrive2 = new TranslateTransition(Duration.millis(1200), torIcon);
        arrive2.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive2.setToY(-31.0);
        ParallelTransition group = new ParallelTransition(arrive, arrive2);
        group.setDelay(Duration.millis(600));
        group.setCycleCount(1);
        group.play();

        Runnable task = () -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TranslateTransition leave2 = new TranslateTransition(Duration.millis(1200), torLabel);
            leave2.setByY(80.0);
            leave2.play();
        };
        new Thread(task).start();
        FileWriter f = new FileWriter();
        if (!f.hasKeys()) {
            Label lblPopOver = new Label("You don't yet have an address.\nClick here to create one.");
            lblPopOver.setWrapText(true);
            lblPopOver.setStyle("-fx-text-fill: #dc78dc; -fx-padding: 10; -fx-font-size: 16;");
            pop = new PopOver(lblPopOver);
            pop.setDetachable(false);
            pop.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
            pop.show(btnAddress);
            pop.setAutoHide(true);
            pop.setHideOnEscape(true);
        }
    }

    void setEmailTab1(){
        Image imgEmail1 = new Image(getClass().getResourceAsStream("email-icon.png"));
        ImageView ivEmail1 = new ImageView(imgEmail1);
        btnEmail.setGraphic(ivEmail1);
        btnEmail.setLayoutX(62);
        btnEmail.setLayoutY(8);
        Timer animTimer = new Timer();
        animTimer.scheduleAtFixedRate(new TimerTask() {

            int i = 0;

            @Override
            public void run() {
                Stage stage = Main.getStage();
                if (stage.getWidth() > 252) {
                    stage.setWidth(stage.getWidth() - 15);
                } else {
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);
    }

    void setEmailTab2(){
        Image imgEmail2 = new Image(getClass().getResourceAsStream("email-icon2.png"));
        ImageView ivEmail2 = new ImageView(imgEmail2);
        btnEmail.setGraphic(ivEmail2);
        btnEmail.setLayoutX(60);
        btnEmail.setLayoutY(8);
        Timer animTimer = new Timer();
        animTimer.scheduleAtFixedRate(new TimerTask() {

            int i = 0;

            @Override
            public void run() {
                if (i < 50) {
                    Stage stage = Main.getStage();
                    stage.setWidth(stage.getWidth() + 15);
                } else {
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);


    }

    void setChatTab1(){
        Image imgChat1 = new Image(getClass().getResourceAsStream("chat-2-icon.png"));
        ImageView ivChat1 = new ImageView(imgChat1);
        btnChat.setGraphic(ivChat1);
        btnChat.setLayoutX(2);
        btnChat.setLayoutY(6);
    }

    void setChatTab2(){
        Image imgChat2 = new Image(getClass().getResourceAsStream("chat-2-icon2.png"));
        ImageView ivChat2 = new ImageView(imgChat2);
        btnChat.setGraphic(ivChat2);
        btnChat.setLayoutX(0);
        btnChat.setLayoutY(6);

    }

    void setChannelTab1(){
        Image imgChannel1 = new Image(getClass().getResourceAsStream("people-icon.png"));
        ImageView ivChannel1 = new ImageView(imgChannel1);
        btnChannel.setGraphic(ivChannel1);
        btnChannel.setLayoutX(128);
        btnChannel.setLayoutY(9);
    }

    void setChannelTab2(){
        Image imgChannel2 = new Image(getClass().getResourceAsStream("people-icon2.png"));
        ImageView ivChannel2 = new ImageView(imgChannel2);
        btnChannel.setGraphic(ivChannel2);
        btnChannel.setLayoutX(126);
        btnChannel.setLayoutY(9);
    }
    @FXML
    void setAddressTab1(){
        Image imgAddress1 = new Image(getClass().getResourceAsStream("contacts.png"));
        ImageView ivAddress1 = new ImageView(imgAddress1);
        btnAddress.setGraphic(ivAddress1);
        btnAddress.setLayoutX(187);
        btnAddress.setLayoutY(4);
    }
    @FXML
    void setAddressTab2(){
        Image imgAddress2 = new Image(getClass().getResourceAsStream("contacts2.png"));
        ImageView ivAddress2 = new ImageView(imgAddress2);
        btnAddress.setGraphic(ivAddress2);
        btnAddress.setLayoutX(186);
        btnAddress.setLayoutY(4);
    }

    @FXML void chatClick(ActionEvent e){
        setChatTab2();
        if (emailClicked) setEmailTab1();
        setChannelTab1();
        emailClicked = false;
    }

    boolean emailClicked = false;
    @FXML void emailClick(ActionEvent e) {
        if (!emailClicked){
            setEmailTab2();
            emailClicked = true;
        }
        setChatTab1();
        setChannelTab1();
    }

    @FXML void channelClick(ActionEvent e){
        setChannelTab2();
        if (emailClicked) setEmailTab1();
        setChatTab1();
        emailClicked = false;
    }

    @FXML void addressCLick(ActionEvent e){
        if (pop != null){pop.hide();}
        Parent root;
        try {
            URL location = getClass().getResource("addresses.fxml");
            FXMLLoader loader = new FXMLLoader(location);
            AnchorPane addressUI = (AnchorPane) loader.load();
            Stage stage = new Stage();
            stage.setTitle("Addresses");
            stage.setX(700);
            stage.setY(200);
            stage.setScene(new Scene(addressUI, 582, 386));
            stage.setResizable(false);
            String file = Main.class.getResource("gui.css").toString();
            stage.getScene().getStylesheets().add(file);
            stage.show();

        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public class TorListener implements TorInitializationListener {

        @Override
        public void initializationProgress(String message, int percent) {
            Platform.runLater(() -> {
                torProgressBar.setProgress(percent / 100.0);
            });
        }

        @Override
        public void initializationCompleted() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    readyToGoAnimation();
                }
            });
        }
    }
}
