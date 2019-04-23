package felix.java2uml;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Hello world!
 *
 */
public class App extends Application
{
	static UML uml;
	Label lbl = new Label("Drag a Folder on the Screen");
	
    public static void main( String[] args )
    {
    	Application.launch(args);
    }


	@Override
	public void start(Stage stage) throws Exception {
 
        // Create the GridPane
        GridPane pane = new GridPane();
        pane.setHgap(5);
        pane.setVgap(20);
     
        // Create the VBox
        VBox root = new VBox();
 
        root.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != root
                        && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        root.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                	
                	lbl.setText("Where should the Diagram be saved?");
                	FileChooser fileChooser = new FileChooser();
                	 
                    //Set extension filter for text files
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SGV-Graphics files (*.svg)", "*.svg");
                    fileChooser.getExtensionFilters().add(extFilter);
         
                    //Show save file dialog
                    File file = fileChooser.showSaveDialog(stage);
         
                    if (file != null) {
                    	lbl.setText("processing");
                    	
                    	uml = new UML(db.getFiles().get(0).toString());
                    	uml.disableGetterSetter();
                    	uml.generateUml();
                        saveTextToFile(uml.generateSVG(), file);
                    }
                    lbl.setText("done.");
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                event.setDropCompleted(success);

                event.consume();
            }
        });
 
         // Set the Style of the VBox
        root.setStyle("-fx-padding: 10;" +
            "-fx-border-style: solid inside;" +
            "-fx-border-width: 2;" +
            "-fx-border-insets: 5;" +
            "-fx-border-radius: 5;" +
            "-fx-border-color: blue;");
        
        
        lbl.setFont(Font.font("", FontWeight.NORMAL, 24));
        root.getChildren().add(lbl);

        // Create the Scene
        Scene scene = new Scene(root,600,400);
        
        // Add the Scene to the Stage
        stage.setScene(scene);
        // Set the Title
        stage.setTitle("Java to UML");
        // Display the Stage
        stage.show();
	}
	
	private static void saveTextToFile(String content, File file) {
        try {
            PrintWriter writer;
            writer = new PrintWriter(file);
            writer.println(content);
            writer.close();
        } catch (IOException ex) {
            
        }
    }
}
