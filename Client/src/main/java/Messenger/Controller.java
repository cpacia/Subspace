package Messenger;

import Messenger.Utils.Identicon.Identicon;
import Messenger.Utils.openname.OpennameListener;
import Messenger.Utils.openname.OpennameUtils;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import eu.hansolo.enzo.notification.Notification;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.animation.ParallelTransition;
import Messenger.Utils.easing.EasingMode;
import Messenger.Utils.easing.ElasticInterpolator;
import org.bitcoinj.core.AddressFormatException;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.Dialogs;

import java.awt.*;
import java.io.File;
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
    @FXML
    Button btnEmailSend;
    @FXML
    Button btnEmailDelete;
    @FXML
    TextField emailToField;
    @FXML
    TextField emailSubjectField;
    @FXML
    ChoiceBox cbEmailFrom;
    @FXML
    WebView wvEmailBody;
    @FXML
    TextField txtTo;
    @FXML
    Label lblFrom;
    @FXML
    Label lblSubject;
    @FXML
    Label lblDate;
    @FXML
    Button btnReply;
    @FXML
    Button btnReplyAll;
    @FXML
    Button btnForward;
    @FXML
    Label lblSentOrInbox;
    @FXML
    ListView sentEmailList;
    @FXML
    Pane chatRoomPane;
    @FXML
    Label lblNewChatRoom;
    @FXML
    ListView chatRoomList;

    private PopOver pop;
    private boolean readyToGo = false;
    private ObservableList<HBox> chatListData;
    private ObservableList<HBox> emailListData = FXCollections.observableArrayList();
    private ObservableList<HBox> sentEmailListData = FXCollections.observableArrayList();
    private ObservableList<HBox> chatRoomListData = FXCollections.observableArrayList();
    private HBox chatInit = new HBox();
    private AllMessageListener messageListener = new AllMessageListener();
    private FileWriter writer;
    private List<String> chatConversationIDs = new ArrayList<String>();
    private List<String> openChatWindows = new ArrayList<String>();
    private List<History.EmailMessage> sortedEmailList = new ArrayList<History.EmailMessage>();
    private List<History.EmailMessage> sortedSentEmailList = new ArrayList<History.EmailMessage>();
    private History.EmailMessage emailCurrentlyBeingViewed;

    public void initialize() {
        emailListData.add(chatInit);
        emailList.setItems(emailListData);
        sentEmailListData.add(chatInit);
        sentEmailList.setItems(emailListData);
        chatRoomList.setItems(chatRoomListData);
        writer = new FileWriter();
        loadChatConversations();
        loadEmails();
        loadChatRooms();
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
        lblNewChatRoom.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Parent root;
                try {
                    URL location = getClass().getResource("chatroom.fxml");
                    FXMLLoader loader = new FXMLLoader(location);
                    AnchorPane addressUI = (AnchorPane) loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("New chat room");
                    stage.setX(700);
                    stage.setY(200);
                    stage.setScene(new Scene(addressUI, 650, 450));
                    stage.setResizable(false);
                    ChatRoomController controller = (ChatRoomController) loader.getController();
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
        chatRoomList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getClickCount() == 2) {
                    int index = chatRoomList.getSelectionModel().getSelectedIndex();
                    Parent root;
                    try {
                        URL location = getClass().getResource("chatroom.fxml");
                        FXMLLoader loader = new FXMLLoader(location);
                        AnchorPane addressUI = (AnchorPane) loader.load();
                        Stage stage = new Stage();
                        stage.setTitle(writer.getChatRooms().get(index).getRoomName());
                        stage.setX(700);
                        stage.setY(200);
                        stage.setScene(new Scene(addressUI, 650, 450));
                        stage.setResizable(false);
                        ChatRoomController controller = (ChatRoomController) loader.getController();
                        controller.setStage(stage);
                        controller.setChatRoom(index);
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
        MenuItem deleteEmail = new MenuItem("Delete");
        ContextMenu contextMenuEmail = new ContextMenu(deleteEmail);
        emailList.setCellFactory(ContextMenuListCell.forListView(contextMenuEmail));
        deleteEmail.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (emailList.getSelectionModel().getSelectedIndex()<= sortedEmailList.size()-1) {
                    History.EmailMessage m = sortedEmailList.get(emailList.getSelectionModel().getSelectedIndex());
                    writer.deleteEmail(m);
                    updateEmailListView();
                    newEmailPane.setVisible(true);
                    emailContentPane.setVisible(false);
                }
            }
        });
        MenuItem deleteSentEmail = new MenuItem("Delete");
        ContextMenu contextMenuSentEmail = new ContextMenu(deleteSentEmail);
        sentEmailList.setCellFactory(ContextMenuListCell.forListView(contextMenuSentEmail));
        deleteSentEmail.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (sentEmailList.getSelectionModel().getSelectedIndex()<= sortedSentEmailList.size()-1) {
                    History.EmailMessage m = sortedSentEmailList.get(sentEmailList.getSelectionModel().getSelectedIndex());
                    writer.deleteEmail(m);
                    updateEmailListView();
                    newEmailPane.setVisible(true);
                    emailContentPane.setVisible(false);
                }
            }
        });
        MenuItem chatroomdelete = new MenuItem("Delete");
        ContextMenu chatRoomContextMenu = new ContextMenu(chatroomdelete);
        chatRoomList.setCellFactory(ContextMenuListCell.forListView(chatRoomContextMenu));
        chatroomdelete.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (chatRoomList.getSelectionModel().getSelectedIndex()<= writer.getNumberOfChatRooms()-1) {
                    String roomname = writer.getChatRooms().get(chatRoomList.getSelectionModel()
                            .getSelectedIndex()).getRoomName();
                    writer.deleteChatRoom(chatRoomList.getSelectionModel().getSelectedIndex());
                    KeyRing.Key k = writer.getKeyFromName(roomname);
                    Main.retriever.closeDeletedAddressThread(k.getAddress());
                    writer.deleteKey(k.getAddress());
                    updateChatRoomListView();
                }
            }
        });
        emailList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getButton() != MouseButton.SECONDARY) {
                    emailContentPane.setVisible(true);
                    newEmailPane.setVisible(false);
                    History.EmailMessage m = sortedEmailList.get(emailList.getSelectionModel().getSelectedIndex());
                    setEmailContent(m, false);
                }
            }
        });
        sentEmailList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getButton() != MouseButton.SECONDARY) {
                    emailContentPane.setVisible(true);
                    newEmailPane.setVisible(false);
                    History.EmailMessage m = sortedSentEmailList.get(sentEmailList.getSelectionModel().getSelectedIndex());
                    setEmailContent(m, true);
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

    @FXML
    void reply(ActionEvent e){
        String emailBody = emailCurrentlyBeingViewed.getBody();
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
        emailEditor.setHtmlText("<br><br>On " + new Date(emailCurrentlyBeingViewed.getTimestamp()*1000) + " " +
                emailCurrentlyBeingViewed.getSenderName() + " wrote:<br><blockquote>" +
                emailBody + "</blockquote>");
        emailToField.setText(emailCurrentlyBeingViewed.getFromAddress());
        emailSubjectField.setText("Re: " + emailCurrentlyBeingViewed.getSubject());
        String sentTo = writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) +
                " <" + emailCurrentlyBeingViewed.getToAddress() + ">";
        cbEmailFrom.getSelectionModel().select(sentTo);
    }

    @FXML
    void replyAll(ActionEvent e){

        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
        String emailBody = emailCurrentlyBeingViewed.getBody();
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
            emailToField.setText(toAddresses);
        }
        else{
            emailToField.setText(emailCurrentlyBeingViewed.getFromAddress());
        }
        emailSubjectField.setText("Re: " + emailCurrentlyBeingViewed.getSubject());
        String sentTo = writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) +
                " <" + emailCurrentlyBeingViewed.getToAddress() + ">";
        cbEmailFrom.getSelectionModel().select(sentTo);
        emailEditor.setHtmlText("<br><br>On " + new Date(emailCurrentlyBeingViewed.getTimestamp()*1000) + " " +
                emailCurrentlyBeingViewed.getSenderName() + " wrote:<br><blockquote>" +
                emailBody + "</blockquote>");
    }

    @FXML
    void forward(ActionEvent e){
        String emailBody = emailCurrentlyBeingViewed.getBody();
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
        emailEditor.setHtmlText("<br><br>-------- Forwarded Message --------<br><b>Subject: </b>" +
                emailCurrentlyBeingViewed.getSubject() + "<br><b>Date: </b>" +
                new Date(emailCurrentlyBeingViewed.getTimestamp() * 1000) +
                "<br><b>From: </b>" + emailCurrentlyBeingViewed.getSenderName() + " <" +
                emailCurrentlyBeingViewed.getFromAddress() + ">" +
                "<br><b>To: </b>" + writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) + " <" +
                emailCurrentlyBeingViewed.getToAddress() + "><br>" +
                emailBody);
        emailSubjectField.setText("Fwd: " + emailCurrentlyBeingViewed.getSubject());
        String sentTo = writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) +
                " <" + emailCurrentlyBeingViewed.getToAddress() + ">";
        cbEmailFrom.getSelectionModel().select(sentTo);
    }

    @FXML
    void emailPaneClear(ActionEvent e){
        clear();
    }

    void clear(){
        emailEditor.setHtmlText("<html><div><head></head><body text=\"#00d0d0\" style=\"width: 728px;word-wrap: break-word;\" bgcolor=\"#393939\" contenteditable=\"true\"></body></div></html>");
        emailSubjectField.setText("");
        emailToField.setText("");
        cbEmailFrom.getSelectionModel().select(0);
        lblSubject.setText("");
        lblFrom.setText("");
        txtTo.setText("");
        lblDate.setText("");
    }

    @FXML
    void showNewEmailPane(MouseEvent e){
        clear();
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
    }

    @FXML
    void showSentOrInbox(MouseEvent e){
        if (lblSentOrInbox.getText().equals("   Sent [-]")){
            lblSentOrInbox.setText("   Inbox [-]");
            emailList.setVisible(false);
            sentEmailList.setVisible(true);
        }
        else {
            lblSentOrInbox.setText("   Sent [-]");
            sentEmailList.setVisible(false);
            emailList.setVisible(true);
        }
    }

    private void setEmailContent(History.EmailMessage m, boolean sentFromMe){
        String emailBody = m.getBody();
        if (emailBody.contains("contenteditable=\"true\"")) {
            emailBody = emailBody.replace("contenteditable=\"true\"", "contenteditable=\"false\"");
        }
        String[] addrArray = null;
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            addrArray = toAddresses.split("\\;",-1);
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        emailCurrentlyBeingViewed = m;
        wvEmailBody.getEngine().loadContent(emailBody);
        lblSubject.setText(m.getSubject());
        lblDate.setText(new Date(m.getTimestamp() * 1000).toString());
        lblFrom.setText(m.getSenderName() + " <" + m.getFromAddress() + ">");
        if (!sentFromMe){
            txtTo.setText(writer.getNameFromAddress(m.getToAddress()) + " <" + m.getToAddress() + ">");
            String withOtherRecipients = txtTo.getText();
            for (String s : addrArray){
                if (!s.equals(m.getToAddress()) && !s.equals("")){
                    if (writer.contactExists(s)){
                        withOtherRecipients = withOtherRecipients + ", " + writer.getNameForContact(s) + " <" + s + ">";
                    }
                    else {
                        withOtherRecipients = withOtherRecipients + ", " + s;
                    }
                }
            }
            txtTo.setText(withOtherRecipients);
        } else {
            if (writer.contactExists(m.getToAddress())){
                txtTo.setText(writer.getNameForContact(m.getToAddress()) + " <" + m.getToAddress() + ">");
            } else {txtTo.setText(m.getToAddress());}
        }
    }

    @FXML
    void emailSend(ActionEvent e){
        String[] addrsArray = emailToField.getText().split("\\;",-1);
        String fromAddress = cbEmailFrom.getValue().toString().substring(
                cbEmailFrom.getValue().toString().indexOf("<") + 1,
                cbEmailFrom.getValue().toString().length() - 1);
        String body = "<ToAddresses>" + emailToField.getText() + "</ToAddresses>" + emailEditor.getHtmlText();
        for (String strAddr : addrsArray) {
            if (!strAddr.equals("")) {
                Address addr = null;
                try {addr = new Address(strAddr);}
                catch (AddressFormatException e1) {e1.printStackTrace();}
                Message m = new Message(addr, body,
                        writer.getKeyFromAddress(fromAddress),
                        Payload.MessageType.EMAIL, emailSubjectField.getText());
                m.send();
            }
        }
        writer.addEmail(addrsArray[0], fromAddress, writer.getNameFromAddress(fromAddress),
                body, emailSubjectField.getText(), System.currentTimeMillis()/1000L, true);
        emailEditor.setHtmlText("<html><head></head><body text=\"#00d0d0\" bgcolor=\"#393939\" contenteditable=\"true\"></body></html>");
        emailSubjectField.setText("");
        emailToField.setText("");
        cbEmailFrom.getSelectionModel().select(0);
        String name = addrsArray[0];
        if (writer.contactExists(name)){name = writer.getNameFromAddress(addrsArray[0]);}
        Image i = new Image(Main.class.getResourceAsStream("logo.png"));
        Notification info = new Notification("Subspace", "Sent email to: " + name, i);
        Notification.Notifier.INSTANCE.notify(info);
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
                        public void run() {
                            emailPane.setVisible(false);
                        }
                    });
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);
    }

    void setEmailTab2(){
        ObservableList<String> cbData = FXCollections.observableArrayList("Select your from address");
        FileWriter f = new FileWriter();
        if (f.hasKeys()) {
            for (KeyRing.Key key : f.getSavedKeys()) {
                String content = key.getName() + " <" + key.getAddress() + ">";
                cbData.add(content);
            }
        }
        emailToField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (!newPropertyValue) {
                    boolean isValid = true;
                    String[] array = emailToField.getText().split("\\;", -1);
                    for (String s : array) {
                        if (!s.equals("")){
                            if (!Address.validateAddress(s)){isValid=false;}
                        }
                    }
                    if (isValid) {
                        if (cbEmailFrom.getSelectionModel().getSelectedIndex() != 0) {
                            btnEmailSend.setDisable(false);
                        }
                        emailToField.setStyle("-fx-background-color: #393939; -fx-text-fill: #f92672; -fx-border-color: #00d0d0;");
                    } else {
                        btnEmailSend.setDisable(true);
                        emailToField.setStyle("-fx-background-color: #393939; -fx-text-fill: #b4b5b1; -fx-border-color: #f92672;");
                    }
                }
            }
        });
        cbEmailFrom.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                if ((Integer) number2 != 0) {
                    cbEmailFrom.setStyle("-fx-background-color: #393939;-fx-border-color: #00d0d0;");
                }
                if ((Integer) number2 != 0 && Address.validateAddress(emailToField.getText())) {
                    btnEmailSend.setDisable(false);
                } else {
                    btnEmailSend.setDisable(true);
                }
            }
        });
        cbEmailFrom.setItems(cbData);
        cbEmailFrom.getSelectionModel().selectFirst();
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
                    if (stage.getWidth() < 975) {
                        stage.setWidth(stage.getWidth() + 20);
                    }
                } else {
                    this.cancel();
                }

                i++;
            }
        }, 0, 25);
    }

    @FXML
    void sendButtonPressed(MouseEvent e){
        btnEmailSend.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void sendButtonReleased(MouseEvent e){
        btnEmailSend.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void deleteButtonPressed(MouseEvent e){
        btnEmailDelete.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void deleteButtonReleased(MouseEvent e){
        btnEmailDelete.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void replyButtonPressed(MouseEvent e){
        btnReply.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void replyButtonReleased(MouseEvent e){
        btnReply.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void replyAllButtonPressed(MouseEvent e){
        btnReplyAll.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void replyAllButtonReleased(MouseEvent e){
        btnReplyAll.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void forwardButtonPressed(MouseEvent e){
        btnForward.setStyle("-fx-background-color: #4d5052; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    @FXML
    void forwardButtonReleased(MouseEvent e){
        btnForward.setStyle("-fx-background-color: #393939; -fx-text-fill: #dc78dc; -fx-border-color: #dc78dc;");
    }

    void setChatTab1(){
        chatPane.setVisible(false);
        chatRoomPane.setVisible(true);
        Image imgChat1 = new Image(getClass().getResourceAsStream("chat-2-icon.png"));
        ImageView ivChat1 = new ImageView(imgChat1);
        btnChat.setGraphic(ivChat1);
        btnChat.setLayoutX(2);
        btnChat.setLayoutY(6);
    }

    void setChatTab2(){
        chatPane.setVisible(true);
        chatRoomPane.setVisible(false);
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

    public void addChatRoom(String roomID){
        chatRoomListData.remove(chatInit);
        Label lblName = new Label(roomID);
        lblName.setWrapText(false);
        lblName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblName.setPadding(new Insets(13, 0, 0, 4));
        HBox h = new HBox();
        h.getChildren().add(lblName);
        chatRoomListData.add(h);
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

    private void loadChatRooms() {
        if (writer.getNumberOfChatRooms()==0){
            chatRoomListData.add(chatInit);
        }
        else {
            for (History.GroupChat g : writer.getChatRooms()) {
                addChatRoom(g.getRoomName());
            }
        }
    }

    private void loadEmails(){
        emailListData = FXCollections.observableArrayList();
        if (writer.getEmailsSentToMe().size()==0){emailListData.add(chatInit);}
        else {
            Map<Long,History.EmailMessage> sortedEmails = new TreeMap<Long, History.EmailMessage>(Collections.reverseOrder());
            for (History.EmailMessage email : writer.getEmailsSentToMe()){
                sortedEmails.put(email.getTimestamp(), email);
            }
            sortedEmailList.clear();
            for (Map.Entry<Long, History.EmailMessage> entry : sortedEmails.entrySet()) {
                History.EmailMessage m = entry.getValue();
                addToEmailListView(m.getFromAddress(), m.getSenderName(), m.getSubject(), true);
                sortedEmailList.add(m);
            }
        }
        emailList.setItems(emailListData);

        sentEmailListData = FXCollections.observableArrayList();
        if (writer.getEmailSentFromMe().size()==0){sentEmailListData.add(chatInit);}
        else {
            Map<Long,History.EmailMessage> sortedEmails = new TreeMap<Long, History.EmailMessage>(Collections.reverseOrder());
            for (History.EmailMessage email : writer.getEmailSentFromMe()){
                sortedEmails.put(email.getTimestamp(), email);
            }
            sortedSentEmailList.clear();
            for (Map.Entry<Long, History.EmailMessage> entry : sortedEmails.entrySet()) {
                History.EmailMessage m = entry.getValue();
                String name;
                if (writer.contactExists(m.getToAddress())){
                    name = writer.getNameForContact(m.getToAddress());
                }
                else {name = m.getToAddress();}
                addToEmailListView(m.getToAddress(), name, m.getSubject(), false);
                sortedSentEmailList.add(m);
            }
        }
        sentEmailList.setItems(sentEmailListData);
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
            } else if (m.getMessageType() == Payload.MessageType.EMAIL){
                String name;
                if (writer.contactExists(m.getToAddress().toString())){
                    name = writer.getNameForContact(m.getToAddress().toString());
                }
                else {name = m.getToAddress().toString();}
                addToEmailListView(m.getToAddress().toString(), name, m.getSubject(), false);
                updateEmailListView();
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
                            Image image = null;
                            if (writer.getOpenname(m.getFromAddress())!=null) {
                                File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+
                                        writer.getOpenname(m.getFromAddress())+".jpg");
                                image = new Image(file.toURI().toString());
                            }
                            else {
                                image = new Image(Main.class.getResourceAsStream("logo.png"));
                            }
                            Notification info = new Notification("Subspace", m.getSenderName()+": " + m.getDecryptedMessage(), image);
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
                    } else if (m.getMessageType() == Payload.MessageType.EMAIL){
                        writer.addEmail(m.getToAddress().toString(), m.getFromAddress(), m.getSenderName(),
                                m.getDecryptedMessage(), m.getSubject(), m.getTimeStamp(), false);
                        addToEmailListView(m.getFromAddress(), m.getSenderName(), m.getSubject(), true);
                        updateEmailListView();
                        Image image = null;
                        if (writer.getOpenname(m.getFromAddress())!=null) {
                            File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+
                                    writer.getOpenname(m.getFromAddress())+".jpg");
                            image = new Image(file.toURI().toString());
                        }
                        else {
                            image = new Image(Main.class.getResourceAsStream("logo.png"));
                        }
                        Notification info = new Notification("Subspace", "New email from: " + m.getSenderName(), image);
                        Notification.Notifier.INSTANCE.notify(info);
                    } else if (m.getMessageType() == Payload.MessageType.CHATROOM){
                        String roomName = writer.getNameFromAddress(m.getToAddress().toString());
                        writer.addChatRoomMessage(roomName, m);
                    }
                }
            });
        }

        @Override
        public void onDataReceived(int bytes) {

        }
    }

    private class OpennameDownloadListener implements OpennameListener{
        @Override
        public void onDownloadComplete(Address addr, String formattedName) {

        }

        @Override
        public void onDownloadFailed() {

        }
    }

    private void updateChatListView(){
        chatList.getItems().clear();
        loadChatConversations();
    }

    private void updateChatRoomListView(){
        chatRoomList.getItems().clear();
        loadChatRooms();
    }

    private void updateEmailListView(){
        emailList.getItems().clear();
        sentEmailList.getItems().clear();
        loadEmails();
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
                if (writer.contactExists(theirAddress)) {
                    lblName = new Label(writer.getNameForContact(theirAddress));
                } else {
                    lblName = new Label(theirAddress);
                }
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
        if (writer.contactExists(theirAddress) && writer.hasOpenname(theirAddress)){
            File f = new File(Main.params.getApplicationDataFolder()+"/avatars/"+writer.getOpenname(theirAddress)+".jpg");
            Image image = new Image(f.toURI().toString());
            imView = new ImageView(image);
        } else {
            try {imView = Identicon.generate(theirAddress, Color.decode("#393939"));}
            catch (Exception e1) {e1.printStackTrace();}
        }
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

    private void addToEmailListView(String theirAddress, String theirName, String subject, boolean sentToMe) {
        if (sentToMe){emailListData.remove(chatInit);}
        else {sentEmailListData.remove(chatInit);}
        HBox h = new HBox();
        Label lblSubject = new Label(subject);
        lblSubject.setMaxWidth(235);
        lblSubject.setStyle("-fx-text-fill: #00d0d0; -fx-font-size: 16;");
        lblSubject.setPadding(new Insets(0, 0, 0, 4));
        lblSubject.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(theirName);
        lblName.setWrapText(false);
        lblName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblName.setPadding(new Insets(0, 0, 0, 4));
        v.getChildren().addAll(lblName, lblSubject);
        h.getChildren().addAll(v);
        h.setAlignment(Pos.TOP_LEFT);
        h.setPrefWidth(235);
        h.setPadding(new Insets(0, 0, 0, 0));
        if (sentToMe){emailListData.add(h);}
        else {sentEmailListData.add(h);}
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
