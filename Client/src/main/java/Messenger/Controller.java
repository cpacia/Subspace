package Messenger;

import Messenger.Utils.Identicon.Identicon;
import Messenger.Utils.openname.OpennameListener;
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
import org.controlsfx.control.PopOver;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.List;


/**
 * This is the controller for the Main UI.
 * It handles handles all user mouse clicks as well as the UI response when messages are received.
 */
public class Controller {


    //The following are controls from gui.fxml
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

    //The popover used to tell the user she doesn't haven any addresses yet.
    private PopOver pop;

    //Boolean for checking if the app has fully initialized
    private boolean readyToGo = false;

    //Lists of HBoxs that populate the various listviews used in this UI ― chat, email inbox, email sent, and chatrooms.
    private ObservableList<HBox> chatListData;
    private ObservableList<HBox> emailListData = FXCollections.observableArrayList();
    private ObservableList<HBox> sentEmailListData = FXCollections.observableArrayList();
    private ObservableList<HBox> chatRoomListData = FXCollections.observableArrayList();

    //An empty HBox used to populate the listviews if no data is available.
    private HBox chatInit = new HBox();

    //A message listener for getting messages from the MessageRetriever
    private AllMessageListener messageListener = new AllMessageListener();

    //A instanace of FileWriter used for reading and writing saved data to/from disk
    private FileWriter writer;

    //A list of our chat conversations. We check this to decide whether to add a message to an existing listview element
    //or create a new one.
    private List<String> chatConversationIDs = new ArrayList<String>();

    //A list of open chat windows. If a chat window is open, we won't display notifications when new messages are received.
    private List<String> openChatWindows = new ArrayList<String>();

    //Lists of incoming and outgoing emails sorted by date.
    private List<History.EmailMessage> sortedEmailList = new ArrayList<History.EmailMessage>();
    private List<History.EmailMessage> sortedSentEmailList = new ArrayList<History.EmailMessage>();

    //The email message currently loaded in the UI and visible to the user.
    private History.EmailMessage emailCurrentlyBeingViewed;

    /**Fires when the controller loads*/
    public void initialize() {
        writer = new FileWriter();

        //Initialize our listviews
        emailListData.add(chatInit);
        emailList.setItems(emailListData);
        sentEmailListData.add(chatInit);
        sentEmailList.setItems(emailListData);
        chatRoomList.setItems(chatRoomListData);

        //Load the listviews
        loadChatConversations();
        loadEmails();
        loadChatRooms();

        //Setup the Tor tooltip and initialization listener
        Tooltip.install(torIcon, new Tooltip("Connected to Tor"));
        TorListener listener = new TorListener();
        TorClient tor = Main.torClient;
        if(tor != null) {
            tor.addInitializationListener(listener);
        }

        //Launches a new chat window when the user clicks the 'new chat message' label
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

        //Launches the new chat room window when the label is clicked.
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

        //When the user double clicks on a conversation in the chat listview, launch a new chat
        //window loaded with the conversation
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
                        //Set the conversation in the new window.
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

        //When the user double clicks on a chat room in the listview, load a new chat room window loaded
        //with the messages for that room.
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

        //Create a right-click context menu for deleting chat conversations from the listview and from disk
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

        //Create a right-click context menu for deleting emails from the listview and from disk
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

        //Create a right-click context menu for deleting sent emails from the sent email listview and from disk
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

        //Create a right-click context menu for deleting chatrooms from the listview and from disk
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

        //When the user clicks on an email in the listview, show it in the email content pane.
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

        //When the user clicks on a sent email in the listview, show it in the email content pane.
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

    /**
     * After Tor finishes loading play the animation that bounces in the 'Tor Ready!' label and the Tor icon.
     */
    public void readyToGoAnimation() {
        //Bounce in the label and icon
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

        //Wait few seconds and bounce out the 'Tor Ready!' label
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

        //Check if the user has an address. If not, show the pop up pointing them to create a new one.
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

        //If this is running on linux, move /etc/hosts back where it belongs.
        //This is related to the bug described in Main.java.
        if (Main.params.getOsType() == ApplicationParams.OS_TYPE.LINUX){
            File target = new File("/etc/hosts");
            File source = new File(Main.params.getApplicationDataFolder().toString() + "/hosts");
            if(source.exists()){
                try {Files.copy(source.toPath(), target.toPath());}
                catch (IOException e) {e.printStackTrace();}
            }
        }
        //Add our listener to the MessageRetriever
        Main.retriever.addListener(messageListener);
        //Start the Retreiver
        Main.retriever.start();
    }

    /**Handle a click on the 'Reply' button in the email pane */
    @FXML
    void reply(ActionEvent e){
        String emailBody = emailCurrentlyBeingViewed.getBody();
        //Emails can be sent to multiple recipients. Since the standard protobuf message format doesn't support multiple
        //recipients we just put the recipients in XML in the email body.
        //For purposes of the formatting the reply, we just remove it.
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);

        //Add the standard 'On [date] so and so wrote:'
        emailEditor.setHtmlText("<br><br>On " + new Date(emailCurrentlyBeingViewed.getTimestamp()*1000) + " " +
                emailCurrentlyBeingViewed.getSenderName() + " wrote:<br><blockquote>" +
                emailBody + "</blockquote>");

        //Set the reply address to the address of the sender
        emailToField.setText(emailCurrentlyBeingViewed.getFromAddress());
        //Set the subject field to the standard 'Re: previous subject'
        emailSubjectField.setText("Re: " + emailCurrentlyBeingViewed.getSubject());
        //Set 'from' choice box with the address the previous email was sent too
        String sentTo = writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) +
                " <" + emailCurrentlyBeingViewed.getToAddress() + ">";
        cbEmailFrom.getSelectionModel().select(sentTo);
    }

    /**
     * Handle a click on the 'Reply all' button in the email pane
     * Exactly the same as above, but this adds all the recipient addresses to the 'to' field.
     */
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

    /**Handle click on the forward button*/
    @FXML
    void forward(ActionEvent e){
        String emailBody = emailCurrentlyBeingViewed.getBody();
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
        //Set the content with a standard forward header
        emailEditor.setHtmlText("<br><br>-------- Forwarded Message --------<br><b>Subject: </b>" +
                emailCurrentlyBeingViewed.getSubject() + "<br><b>Date: </b>" +
                new Date(emailCurrentlyBeingViewed.getTimestamp() * 1000) +
                "<br><b>From: </b>" + emailCurrentlyBeingViewed.getSenderName() + " <" +
                emailCurrentlyBeingViewed.getFromAddress() + ">" +
                "<br><b>To: </b>" + writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) + " <" +
                emailCurrentlyBeingViewed.getToAddress() + "><br>" +
                emailBody);
        emailSubjectField.setText("Fwd: " + emailCurrentlyBeingViewed.getSubject());
        //Set the from address equal to the to address in the email
        String sentTo = writer.getNameFromAddress(emailCurrentlyBeingViewed.getToAddress()) +
                " <" + emailCurrentlyBeingViewed.getToAddress() + ">";
        cbEmailFrom.getSelectionModel().select(sentTo);
    }

    /**Clears the contents of the email pane. Used when the user clicks 'delete'.*/
    @FXML
    void emailPaneClear(ActionEvent e){
        clear();
    }

    /**Resets the email pane to its default state and adds in some html for styling*/
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

    /**Handles click on the new email label*/
    @FXML
    void showNewEmailPane(MouseEvent e){
        clear();
        emailContentPane.setVisible(false);
        newEmailPane.setVisible(true);
    }

    /**
     * Handles clicks on the 'inbox' or 'sent' labels
     * It will set visibility of the listviews depending on which label has been clicked
     */
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

    /**Displays the content of an email when the user double clicks on an email in the listview*/
    private void setEmailContent(History.EmailMessage m, boolean sentFromMe){
        String emailBody = m.getBody();
        //We need to remove the contenteditable flag because we don't want the user to be able to edit sent emails.
        if (emailBody.contains("contenteditable=\"true\"")) {
            emailBody = emailBody.replace("contenteditable=\"true\"", "contenteditable=\"false\"");
        }
        //We remove the <ToAddresses> XML from the body and save the addresses into an array so we can display them in
        //the 'to' field instead of in the email body.
        String[] addrArray = null;
        if (emailBody.contains("<ToAddresses>")){
            String toAddresses = emailBody.substring(emailBody.indexOf("<ToAddresses>") + 13, emailBody.indexOf("</ToAddresses>"));
            addrArray = toAddresses.split("\\;",-1);
            emailBody = emailBody.replace("<ToAddresses>" + toAddresses + "</ToAddresses>", "");
        }
        //Set the current email on our static variable
        emailCurrentlyBeingViewed = m;
        //Set the rest of the email elements
        wvEmailBody.getEngine().loadContent(emailBody);
        lblSubject.setText(m.getSubject());
        lblDate.setText(new Date(m.getTimestamp() * 1000).toString());
        lblFrom.setText(m.getSenderName() + " <" + m.getFromAddress() + ">");
        //If the email wasn't sent from us, then add all the recipients to the 'to' field.
        if (!sentFromMe){
            txtTo.setText(writer.getNameFromAddress(m.getToAddress()) + " <" + m.getToAddress() + ">");
            //If more than one recipient then add them to the 'to' field as well.
            String withOtherRecipients = txtTo.getText();
            for (String s : addrArray){
                if (!s.equals(m.getToAddress()) && !s.equals("")){
                    //If we have a contact for this address, display it.
                    if (writer.contactExists(s)){
                        withOtherRecipients = withOtherRecipients + ", " + writer.getNameForContact(s) + " <" + s + ">";
                    }
                    //Otherwise just show the address
                    else {
                        withOtherRecipients = withOtherRecipients + ", " + s;
                    }
                }
            }
            txtTo.setText(withOtherRecipients);
        //If it was sent from us, show the contact name if we have one, otherwise just show the address.
        } else {
            if (writer.contactExists(m.getToAddress())){
                txtTo.setText(writer.getNameForContact(m.getToAddress()) + " <" + m.getToAddress() + ">");
            } else {txtTo.setText(m.getToAddress());}
        }
    }

    /**Handles a click on the send button in the email pane*/
    @FXML
    void emailSend(ActionEvent e){
        //Try to acquire 15 permits from our rate limiter. If we can't the user needs to wait a little before sending
        //this email.
        if (RateLimiter.tryAcquire(15)) {
            //If the email is being sent to more than one address, split the addresses into an array.
            String[] addrsArray = emailToField.getText().split("\\;", -1);
            //Parse the 'from' choicebox to get the address.
            String fromAddress = cbEmailFrom.getValue().toString().substring(
                    cbEmailFrom.getValue().toString().indexOf("<") + 1,
                    cbEmailFrom.getValue().toString().length() - 1);
            //We're adding any additional recipients to the email body as XML since our protobuf payload format
            //only allows for one recipient.
            String body = "<ToAddresses>" + emailToField.getText() + "</ToAddresses>" + emailEditor.getHtmlText();
            //Check to make sure the email isn't too large. We can increase the size later on if necessary.
            if (body.getBytes().length < 10000) {
                //For each 'to' address, send an email.
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
                //Save the email to disk.
                writer.addEmail(addrsArray[0], fromAddress, writer.getNameFromAddress(fromAddress),
                        body, emailSubjectField.getText(), System.currentTimeMillis() / 1000L, true);
                //Reset the email pane back to its default state
                emailEditor.setHtmlText("<html><head></head><body text=\"#00d0d0\" style=\"width: 728px;word-wrap: break-word;\" bgcolor=\"#393939\" contenteditable=\"true\"></body></html>");
                emailSubjectField.setText("");
                emailToField.setText("");
                cbEmailFrom.getSelectionModel().select(0);
                //Display a notification telling the user the email was sent
                String name = addrsArray[0];
                if (writer.contactExists(name)) {
                    name = writer.getNameForContact(addrsArray[0]);
                }
                Image i = new Image(Main.class.getResourceAsStream("logo.png"));
                Notification info = new Notification("Subspace", "Sent email to: " + name, i);
                Notification.Notifier.INSTANCE.notify(info);
                //Update email listview to reflect that we sent an email.
                updateEmailListView();
            //Show an error dialog if the email is too long.
            } else {
                Action response = Dialogs.create()
                        .owner(Main.getStage())
                        .title("Error")
                        .style(DialogStyle.CROSS_PLATFORM_DARK)
                        .masthead("Your email is too long")
                        .message("Keep it under 10 KB.")
                        .actions(Dialog.Actions.OK)
                        .showError();
            }
        //If we counldn't acquire 15 permits, show an error dialog and tell them to slow down.
        } else {
            Dialogs.create()
                    .owner(Main.getStage())
                    .title("Error")
                    .style(DialogStyle.CROSS_PLATFORM_DARK)
                    .masthead("You're doing that too fast.")
                    .message("Slow down bub.")
                    .showError();
        }
    }

    /**Sets the email button to its unclicked state*/
    void setEmailTab1(){
        //Show the unclicked email button
        Image imgEmail1 = new Image(getClass().getResourceAsStream("email-icon.png"));
        ImageView ivEmail1 = new ImageView(imgEmail1);
        btnEmail.setGraphic(ivEmail1);
        btnEmail.setLayoutX(62);
        btnEmail.setLayoutY(8);
        //Shrink the main UI stage back to the standard size
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

    /**Sets the email button to its clicked state. Will expand the stage to reveal the email pane*/
    void setEmailTab2(){
        //Populate the 'from' choice box.
        ObservableList<String> cbData = FXCollections.observableArrayList("Select your from address");
        FileWriter f = new FileWriter();
        if (f.hasKeys()) {
            for (KeyRing.Key key : f.getSavedKeys()) {
                String content = key.getName() + " <" + key.getAddress() + ">";
                cbData.add(content);
            }
        }
        //Validates the 'to' addresses as the user enters them. If they are invalid, the send button is disable and the
        //font color is changed to show the address is invalid.
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
        //A change listener which enables the send button when the user selects a valid 'from address'.
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
        //Set the pane data
        cbEmailFrom.setItems(cbData);
        cbEmailFrom.getSelectionModel().selectFirst();
        emailPane.setVisible(true);
        newEmailPane.setVisible(true);
        emailContentPane.setVisible(false);
        //Change the email icon to clicked
        Image imgEmail2 = new Image(getClass().getResourceAsStream("email-icon2.png"));
        ImageView ivEmail2 = new ImageView(imgEmail2);
        btnEmail.setGraphic(ivEmail2);
        btnEmail.setLayoutX(60);
        btnEmail.setLayoutY(8);
        //Expand the stage to reveal the email pane
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

    /**
     * The following methods change the style of the send, delete, reply, reply all, and forward buttons
     * when the user presses and releases the mouse.
     */
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

    /**Sets the chat icon to its unclicked state*/
    void setChatTab1(){
        chatPane.setVisible(false);
        chatRoomPane.setVisible(true);
        Image imgChat1 = new Image(getClass().getResourceAsStream("chat-2-icon.png"));
        ImageView ivChat1 = new ImageView(imgChat1);
        btnChat.setGraphic(ivChat1);
        btnChat.setLayoutX(2);
        btnChat.setLayoutY(6);
    }

    /**Sets the chat icon to its clicked state*/
    void setChatTab2(){
        chatPane.setVisible(true);
        chatRoomPane.setVisible(false);
        Image imgChat2 = new Image(getClass().getResourceAsStream("chat-2-icon2.png"));
        ImageView ivChat2 = new ImageView(imgChat2);
        btnChat.setGraphic(ivChat2);
        btnChat.setLayoutX(0);
        btnChat.setLayoutY(6);

    }

    /**Sets the chat room icon to its unclicked state*/
    void setChannelTab1(){
        Image imgChannel1 = new Image(getClass().getResourceAsStream("people-icon.png"));
        ImageView ivChannel1 = new ImageView(imgChannel1);
        btnChannel.setGraphic(ivChannel1);
        btnChannel.setLayoutX(128);
        btnChannel.setLayoutY(9);
    }

    /**Sets the chat room icon to its clicked state*/
    void setChannelTab2(){
        Image imgChannel2 = new Image(getClass().getResourceAsStream("people-icon2.png"));
        ImageView ivChannel2 = new ImageView(imgChannel2);
        btnChannel.setGraphic(ivChannel2);
        btnChannel.setLayoutX(126);
        btnChannel.setLayoutY(9);
    }

    /**Sets the addresses icon to its unclicked state*/
    @FXML
    void setAddressTab1(){
        Image imgAddress1 = new Image(getClass().getResourceAsStream("contacts.png"));
        ImageView ivAddress1 = new ImageView(imgAddress1);
        btnAddress.setGraphic(ivAddress1);
        btnAddress.setLayoutX(187);
        btnAddress.setLayoutY(4);
    }

    /**Sets the addresses icon to its clicked state*/
    @FXML
    void setAddressTab2(){
        Image imgAddress2 = new Image(getClass().getResourceAsStream("contacts2.png"));
        ImageView ivAddress2 = new ImageView(imgAddress2);
        btnAddress.setGraphic(ivAddress2);
        btnAddress.setLayoutX(186);
        btnAddress.setLayoutY(4);
    }

    /**Handles clicks on the chat icon*/
    @FXML void chatClick(ActionEvent e){
        setChatTab2();
        if (emailClicked) setEmailTab1();
        setChannelTab1();
        emailClicked = false;
    }

    /**Handles clicks on the email icon*/
    boolean emailClicked = false;
    @FXML void emailClick(ActionEvent e) {
        if (!emailClicked){
            setEmailTab2();
            emailClicked = true;
        }
        setChatTab1();
        setChannelTab1();
    }

    /**Handles clicks on the chat room icon*/
    @FXML void channelClick(ActionEvent e){
        setChannelTab2();
        if (emailClicked) setEmailTab1();
        setChatTab1();
        emailClicked = false;
    }

    /**Handles clicks on the addresses icon. Opens the addresses window*/
    @FXML void addressCLick(ActionEvent e){
        //Hide the popup if its still showing
        if (pop != null){pop.hide();}
        //Only display the address window if Tor has finished loading, otherwise it could freeze.
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

    /**When the user opens a new chat window, add it to this list so we can keep track of what windows are open*/
    public void addChatWindow(String cID){
        this.openChatWindows.add(cID);
    }

    /**When the user closes a chat window, remove it from this list*/
    public void removeChatWindow(String cID){
        this.openChatWindows.remove(cID);
    }

    /**Adds a new chat room to the listview*/
    public void addChatRoom(String roomID){
        //Remove the blank HBox if it's still there
        chatRoomListData.remove(chatInit);
        Label lblName = new Label(roomID);
        lblName.setWrapText(false);
        lblName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblName.setPadding(new Insets(13, 0, 0, 4));
        HBox h = new HBox();
        h.getChildren().add(lblName);
        chatRoomListData.add(h);
    }

    /**Tor initialization listener*/
    public class TorListener implements TorInitializationListener {

        @Override
        public void initializationProgress(String message, int percent) {
            Platform.runLater(() -> {
                //Set the progress bar as Tor initializes
                torProgressBar.setProgress(percent / 100.0);
            });
        }

        @Override
        public void initializationCompleted() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    //Play our read-to-go animation when Tor finishes loading.
                    readyToGoAnimation();
                }
            });
        }
    }

    /**Loads our chat conversations from disk and shows them in the listview*/
    private void loadChatConversations(){
        chatListData = FXCollections.observableArrayList();
        //If there are no chat conversations just load a blank HBox in the listview
        if (writer.getSavedCoversations().size()==0){chatListData.add(chatInit);}
        else {
            //Load the chat conversations sorted by the one with the newest message first.
            Map<Long,History.ChatConversation> sortedMap = new TreeMap<Long, History.ChatConversation>(Collections.reverseOrder());
            for (History.ChatConversation conversation : writer.getSavedCoversations()){
                chatConversationIDs.add(conversation.getConversationID());
                History.ChatMessage m = conversation.getChatMessage(conversation.getChatMessageCount()-1);
                sortedMap.put(m.getTimestamp(), conversation);
            }
            //Reset the conversationID list. We'll add our sorted list of conversation IDs back in.
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

    /**Loads the chat rooms from disk and shows them in the listview*/
    private void loadChatRooms() {
        //If we have no chat rooms, then just load a blank HBox
        if (writer.getNumberOfChatRooms()==0){
            chatRoomListData.add(chatInit);
        }
        else {
            for (History.GroupChat g : writer.getChatRooms()) {
                addChatRoom(g.getRoomName());
            }
        }
    }

    /**Load both incoming and sent emails from disk and add them to the listviews*/
    private void loadEmails(){
        //Incoming emails
        emailListData = FXCollections.observableArrayList();
        //If we have no emails then just load a blank HBox
        if (writer.getEmailsSentToMe().size()==0){emailListData.add(chatInit);}
        else {
            //Sort the emails by date
            Map<Long,History.EmailMessage> sortedEmails = new TreeMap<Long, History.EmailMessage>(Collections.reverseOrder());
            for (History.EmailMessage email : writer.getEmailsSentToMe()){
                sortedEmails.put(email.getTimestamp(), email);
            }
            sortedEmailList.clear();
            //Add them to the listview
            for (Map.Entry<Long, History.EmailMessage> entry : sortedEmails.entrySet()) {
                History.EmailMessage m = entry.getValue();
                addToEmailListView(m.getFromAddress(), m.getSenderName(), m.getSubject(), true);
                sortedEmailList.add(m);
            }
        }
        emailList.setItems(emailListData);

        //Sent emails
        sentEmailListData = FXCollections.observableArrayList();
        //If we hve no emails then just load a blank HBox
        if (writer.getEmailSentFromMe().size()==0){sentEmailListData.add(chatInit);}
        else {
            //Sort emails by data
            Map<Long,History.EmailMessage> sortedEmails = new TreeMap<Long, History.EmailMessage>(Collections.reverseOrder());
            for (History.EmailMessage email : writer.getEmailSentFromMe()){
                sortedEmails.put(email.getTimestamp(), email);
            }
            sortedSentEmailList.clear();
            //Add them to the listview
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

    /**Returns the message listener used by this controller*/
    public AllMessageListener getListener(){
        return this.messageListener;
    }

    /**The main message listener for this class*/
    private class AllMessageListener implements MessageListener {
        @Override
        public void onMessageSent(Message m) {
            //Re-sort the chat listview and put the conversation with the newest message at the top
            if (m.getMessageType() == Payload.MessageType.CHAT) {
                String cID = m.getFromAddress() + m.getToAddress().toString();
                if (!chatConversationIDs.contains(cID)) {
                    chatListData.remove(chatInit);
                    chatConversationIDs.add(cID);
                    addToChatListView(cID, m.getToAddress().toString(),
                            writer.getNameFromAddress(m.getFromAddress()), m.getDecryptedMessage(), true);
                    updateChatListView();
                } else {updateChatListView();}
            //Add the email to the sent email listview
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
                    //For chat messages we need to re-sort the chat listview and put the message with the newest
                    //conversation at the top.
                    if (m.getMessageType() == Payload.MessageType.CHAT) {
                        //We make the conversation ID a concatenation of the to and from address
                        String cID = m.getToAddress().toString() + m.getFromAddress();
                        //If the chat conversation isn't open in a window then display a notification and play a sound
                        if (!openChatWindows.contains(cID)) {
                            //If there is an openname for the sender then use the avatar in the notification
                            Image image = null;
                            if (writer.getOpenname(m.getFromAddress())!=null) {
                                File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+
                                        writer.getOpenname(m.getFromAddress())+".jpg");
                                image = new Image(file.toURI().toString());
                            }
                            //Otherwise just use the subspace logo
                            else {
                                image = new Image(Main.class.getResourceAsStream("logo.png"));
                            }
                            //Show the notifiction
                            Notification info = new Notification("Subspace", m.getSenderName()+": " + m.getDecryptedMessage(), image);
                            Notification.Notifier.INSTANCE.notify(info);
                            //Play a sound
                            try{
                                AudioInputStream audioInputStream =
                                    AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("chime.wav"));
                                Clip clip = AudioSystem.getClip();
                                clip.open(audioInputStream);
                                clip.start();}
                            catch (IOException | UnsupportedAudioFileException | LineUnavailableException e){e.printStackTrace();}
                        }
                        //If the conversation doesn't exist on disk then save it to disk
                        if (!writer.conversationExists(cID)) {
                            writer.newChatConversation(cID, m, m.getSenderName(),
                                    m.getFromAddress(), m.getToAddress().toString(), false);
                        //If it does, then just add a new message to it
                        } else {
                            writer.addChatMessage(cID, m, false);
                        }
                        //If the conversation isn't already in the chat listview add it to it.
                        if (!chatConversationIDs.contains(cID)) {
                            chatConversationIDs.add(cID);
                            addToChatListView(cID, m.getFromAddress(),
                                    m.getSenderName(), m.getDecryptedMessage(), false);
                            updateChatListView();
                        } else {
                            updateChatListView();
                        }
                    } else if (m.getMessageType() == Payload.MessageType.EMAIL){
                        //Save the email to disk and add it to the inbox listview
                        writer.addEmail(m.getToAddress().toString(), m.getFromAddress(), m.getSenderName(),
                                m.getDecryptedMessage(), m.getSubject(), m.getTimeStamp(), false);
                        addToEmailListView(m.getFromAddress(), m.getSenderName(), m.getSubject(), true);
                        updateEmailListView();
                        //Display a notification telling the user he received an email
                        Image image = null;
                        //If an openname exists for this address, use that avatar in the notification
                        if (writer.getOpenname(m.getFromAddress())!=null) {
                            File file = new File(Main.params.getApplicationDataFolder()+"/avatars/"+
                                    writer.getOpenname(m.getFromAddress())+".jpg");
                            image = new Image(file.toURI().toString());
                        }
                        //Otherwise just use the subspace logo
                        else {
                            image = new Image(Main.class.getResourceAsStream("logo.png"));
                        }
                        //Show the notification
                        Notification info = new Notification("Subspace", "New email from: " + m.getSenderName(), image);
                        Notification.Notifier.INSTANCE.notify(info);
                        //Play a sound
                        try{
                            AudioInputStream audioInputStream =
                                    AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("chime.wav"));
                            Clip clip = AudioSystem.getClip();
                            clip.open(audioInputStream);
                            clip.start();}
                        catch (IOException | UnsupportedAudioFileException | LineUnavailableException e){e.printStackTrace();}
                    //For chatroom messages we just need to save the message to disk
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

    /**Refreshes the chat listview*/
    private void updateChatListView(){
        chatList.getItems().clear();
        loadChatConversations();
    }

    /**Refreshes the chatroom listview*/
    private void updateChatRoomListView(){
        chatRoomList.getItems().clear();
        loadChatRooms();
    }

    /**Refreshes both the inbox and sent email listviews*/
    private void updateEmailListView(){
        emailList.getItems().clear();
        sentEmailList.getItems().clear();
        loadEmails();
    }

    /**Adds the content to the chat listview*/
    private void addToChatListView(String conversationID, String theirAddress,
                                   String theirName, String messageContent,
                                   boolean isSentFromMe) {
        //Remove the blank HBox if it's there
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
        //If we sent the message, check to see if we have a name for this contact to display.
        //If not we'll just the address
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
        //If we didn't send the message we will have a name to disply.
        } else {
            lblName = new Label(theirName);
        }
        lblName.setWrapText(false);
        lblName.setStyle("-fx-text-fill: #dc78dc; -fx-font-size: 16;");
        lblName.setPadding(new Insets(11, 0, 0, 10));
        v.getChildren().addAll(lblName, lblMessage);
        //See if we have an avatar to display for this person.
        ImageView imView = null;
        if (writer.contactExists(theirAddress) && writer.hasOpenname(theirAddress)){
            File f = new File(Main.params.getApplicationDataFolder()+"/avatars/"+writer.getOpenname(theirAddress)+".jpg");
            Image image = new Image(f.toURI().toString());
            imView = new ImageView(image);
        //If not use an identicon.
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

    /**Adds an content to the email listview*/
    private void addToEmailListView(String theirAddress, String theirName, String subject, boolean sentToMe) {
        //Remove the blank HBox if its there
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
        //Set the Hbox on the correct listview
        if (sentToMe){emailListData.add(h);}
        else {sentEmailListData.add(h);}
    }

    /**Class for creating a right-click context menu on our listviews*/
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

    /**The ListCell used by ContextMenuListCell for creating the right-click context menu*/
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
