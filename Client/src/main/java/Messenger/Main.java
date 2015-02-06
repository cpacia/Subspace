package Messenger;

import com.subgraph.orchid.TorClient;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main extends Application {

    static Controller controller;
    static TorClient torClient;
    private static Stage stg;
    public static ApplicationParams params;
    public static MessageRetriever retriever;

    @Override
    public void start(Stage primaryStage) throws Exception{
        URL location = getClass().getResource("gui.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        AnchorPane mainUI = (AnchorPane) loader.load();
        primaryStage.setTitle("Subspace");
        primaryStage.setScene(new Scene(mainUI, 252, 630));
        final String file = Main.class.getResource("gui.css").toString();
        this.stg = primaryStage;
        controller = loader.getController();
        primaryStage.getScene().getStylesheets().add(file);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            torClient.stop();
        });
    }


    public static void main(String[] args) {
        try{params = new ApplicationParams(args);}
        catch (IOException e){e.printStackTrace();}
        torClient = new TorClient();
        torClient.getConfig().setDataDirectory(params.getApplicationDataFolder());
        torClient.enableSocksListener();
        torClient.start();
        retriever = new MessageRetriever(new FileWriter().getSavedKeys());
        launch(args);
        try{torClient.waitUntilReady(5000);}
        catch (TimeoutException | InterruptedException e){e.printStackTrace();}
        System.out.println("Tor Ready");

    }

    static Stage getStage(){
        return stg;
    }

}
