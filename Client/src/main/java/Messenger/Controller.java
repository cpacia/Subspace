package Messenger;

import Messenger.Utils.Identicon.Identicon;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import eu.hansolo.enzo.notification.Notification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.animation.ParallelTransition;
import Messenger.Utils.easing.EasingMode;
import Messenger.Utils.easing.ElasticInterpolator;
import org.controlsfx.control.PopOver;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;


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
    @FXML
    Pane emailPane;
    @FXML
    Pane chatPane;
    @FXML
    Pane newEmailPane;
    @FXML
    Pane emailContentPane;
    @FXML
    HTMLEditor emailEditor;
    @FXML
    ListView emailList;
    private PopOver pop;
    private boolean readyToGo = false;
    private ObservableList<HBox> chatListData;
    private ObservableList<HBox> emailListData = FXCollections.observableArrayList();
    private HBox chatInit = new HBox();
    private AllMessageListener messageListener = new AllMessageListener();
    private FileWriter writer;
    private List<String> chatConversationIDs = new ArrayList<String>();
    private List<String> openChatWindows = new ArrayList<String>();

    public void initialize() {
        emailListData.add(chatInit);
        emailList.setItems(emailListData);
        writer = new FileWriter();
        loadChatConversations();
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
        chatList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getClickCount() == 2) {
                    int index = chatList.getSelectionModel().getSelectedIndex();
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
                        controller.setConversation(chatConversationIDs.get(index));
                        String file = Main.class.getResource("gui.css").toString();
                        stage.getScene().getStylesheets().add(file);
                        stage.show();

                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        });
        MenuItem delete = new MenuItem("Delete");
        ContextMenu contextMenu = new ContextMenu(delete);
        chatList.setCellFactory(ContextMenuListCell.forListView(contextMenu));
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (chatList.getSelectionModel().getSelectedIndex()<= chatConversationIDs.size()-1) {
                    String cID = chatConversationIDs.get(chatList.getSelectionModel().getSelectedIndex());
                    writer.deleteConversation(cID);
                    updateChatListView();
                }
            }
        });
    }

    public void readyToGoAnimation() {
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), torProgressBar);
        leave.setByY(80.0);
        leave.play();
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
        readyToGo = true;
        Main.retriever.addListener(messageListener);
        Main.retriever.start();
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
                    stage.setWidth(stage.getWidth() - 20);
                } else {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {emailPane.setVisible(false);}});
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);
    }

    void setEmailTab2(){
        emailPane.setVisible(true);
        newEmailPane.setVisible(true);
        emailContentPane.setVisible(false);
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
                if (i < 55) {
                    Stage stage = Main.getStage();
                    if (stage.getWidth()<975){
                        stage.setWidth(stage.getWidth() + 20);
                    }
                } else {
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);


    }

    void setChatTab1(){
        chatPane.setVisible(false);
        Image imgChat1 = new Image(getClass().getResourceAsStream("chat-2-icon.png"));
        ImageView ivChat1 = new ImageView(imgChat1);
        btnChat.setGraphic(ivChat1);
        btnChat.setLayoutX(2);
        btnChat.setLayoutY(6);
    }

    void setChatTab2(){
        chatPane.setVisible(true);
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
        if (readyToGo) {
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
                AddressWindowController controller = (AddressWindowController) loader.getController();
                controller.setStage(stage);
                String file = Main.class.getResource("addresses.css").toString();
                stage.getScene().getStylesheets().add(file);
                stage.show();

            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void addChatWindow(String cID){
        this.openChatWindows.add(cID);
    }

    public void removeChatWindow(String cID){
        this.openChatWindows.remove(cID);
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

    private void loadChatConversations(){
        chatListData = FXCollections.observableArrayList();
        if (writer.getSavedCoversations().size()==0){chatListData.add(chatInit);}
        else {
            Map<Long,History.ChatConversation> sortedMap = new TreeMap<Long, History.ChatConversation>(Collections.reverseOrder());
            for (History.ChatConversation conversation : writer.getSavedCoversations()){
                chatConversationIDs.add(conversation.getConversationID());
                History.ChatMessage m = conversation.getChatMessage(conversation.getChatMessageCount()-1);
                sortedMap.put(m.getTimestamp(), conversation);
            }
            this.chatConversationIDs.clear();
            for (Map.Entry<Long, History.ChatConversation> entry : sortedMap.entrySet()) {
                this.chatConversationIDs.add(entry.getValue().getConversationID());
                History.ChatConversation conversation = entry.getValue();
                History.ChatMessage m = conversation.getChatMessage(conversation.getChatMessageCount()-1);
                addToChatListView(conversation.getConversationID(), conversation.getTheirAddress(),
                        conversation.getTheirName(), m.getContent(), m.getSentFromMe());
            }
        }
        chatList.setItems(chatListData);

    }

    public AllMessageListener getListener(){
        return this.messageListener;
    }

    private class AllMessageListener implements MessageListener {
        @Override
        public void onMessageSent(Message m) {

            if (m.getMessageType() == Payload.MessageType.CHAT) {
                String cID = m.getFromAddress() + m.getToAddress().toString();
                if (!chatConversationIDs.contains(cID)) {
                    chatListData.remove(chatInit);
                    chatConversationIDs.add(cID);
                    addToChatListView(cID, m.getToAddress().toString(),
                            writer.getNameFromAddress(m.getFromAddress()), m.getDecryptedMessage(), true);
                    updateChatListView();
                } else {updateChatListView();}
            }
        }

        @Override
        public void onMessageReceived(Message m) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (m.getMessageType() == Payload.MessageType.CHAT) {
                        String cID = m.getToAddress().toString() + m.getFromAddress();
                        if (!openChatWindows.contains(cID)) {
                            Notification info = new Notification("Subspace", "New chat message from " + m.getSenderName());
                            Notification.Notifier.INSTANCE.notify(info);
                        }
                        if (!writer.conversationExists(cID)) {
                            writer.newChatConversation(cID, m, m.getSenderName(),
                                    m.getFromAddress(), m.getToAddress().toString(), false);
                        } else {
                            writer.addChatMessage(cID, m, false);
                        }
                        if (!chatConversationIDs.contains(cID)) {
                            chatConversationIDs.add(cID);
                            addToChatListView(cID, m.getFromAddress(),
                                    m.getSenderName(), m.getDecryptedMessage(), false);
                            updateChatListView();
                        } else {
                            updateChatListView();
                        }
                    }
                }
            });
        }

        @Override
        public void onDataReceived(int bytes) {

        }
    }

    private void updateChatListView(){
        chatList.getItems().clear();
        loadChatConversations();
    }

    private void addToChatListView(String conversationID, String theirAddress,
                                   String theirName, String messageContent,
                                   boolean isSentFromMe) {
        chatListData.remove(chatInit);
        HBox h = new HBox();
        Label lblMessage = new Label(messageContent);
        lblMessage.setMaxWidth(235);
        lblMessage.setStyle("-fx-text-fill: #00d0d0; -fx-font-size: 16;");
        lblMessage.setPadding(new Insets(0, 0, 3, 10));
        lblMessage.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER_LEFT);
        Label lblName;
        if (isSentFromMe) {
            String n = writer.getNameFromConversation(conversationID);
            if (n.equals("")) {
                lblName = new Label(theirAddress);
            } else {
                lblName = new Label(n);
            }
        } else {
            lblName = new Label(theirName);
        }
        lblName.setWrapText(false);
        lblName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblName.setPadding(new Insets(11, 0, 0, 10));
        v.getChildren().addAll(lblName, lblMessage);
        ImageView imView = null;
        try {imView = Identicon.generate(theirAddress, Color.decode("#393939"));}
        catch (Exception e1) {e1.printStackTrace();}
        imView.setFitWidth(35);
        imView.setFitHeight(35);
        HBox imageHBox = new HBox();
        imageHBox.getChildren().add(imView);
        imageHBox.setPadding(new Insets(15, 0, 0, 0));
        h.getChildren().addAll(imageHBox, v);
        h.setAlignment(Pos.TOP_LEFT);
        h.setPrefWidth(235);
        h.setPadding(new Insets(0, 0, 0, 3));
        chatListData.add(h);
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
