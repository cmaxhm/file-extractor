package com.cmaxhm.fileextractor;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MainController {
  @FXML private TextArea outputTextArea;
  @FXML private Button extractButton;
  @FXML private ToggleGroup optionsGroup;
  @FXML private Label sourceDirectoryLabel;
  @FXML private Label destinationDirectoryLabel;
  private File selectedSourceDirectory;
  private File selectedDestinationDirectory;
  private String selectedFileFormat;
  private final StringProperty sourceDirectorySelected = new SimpleStringProperty();
  private final StringProperty destinationDirectorySelected = new SimpleStringProperty();

  @FXML
  public void initialize() {
    this.selectedFileFormat = FileTypes.ALL;
    this.selectedSourceDirectory = new File(System.getProperty("user.home"));
    this.selectedDestinationDirectory = new File(System.getProperty("user.home"));

    this.optionsGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      RadioButton selectedRadioButton = (RadioButton) newValue;
      String radioButtonId = selectedRadioButton.getId();

      switch (radioButtonId) {
        case "imagesRadioButton":
          this.selectedFileFormat = FileTypes.IMAGES;

          break;
        case "videosRadioButton":
          this.selectedFileFormat = FileTypes.VIDEOS;

          break;
        default:
          this.selectedFileFormat = FileTypes.ALL;
      }

      this.outputTextArea.appendText("▶ Selected file format: " + selectedRadioButton.getText() + "\n");
    });

    this.extractButton.disableProperty().bind(
      Bindings.createBooleanBinding(() ->
          this.sourceDirectorySelected.get() == null
            || this.sourceDirectorySelected.get().isEmpty()
            || this.destinationDirectorySelected.get() == null
            || this.destinationDirectorySelected.get().isEmpty(),
        this.sourceDirectorySelected,
        this.destinationDirectorySelected
      )
    );
  }

  @FXML
  protected void onOpenSourceDirectoryChooser() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    this.selectedSourceDirectory = directoryChooser.showDialog(null);

    if (this.selectedSourceDirectory != null) {
      this.sourceDirectorySelected.set(this.selectedSourceDirectory.getAbsolutePath());
      this.sourceDirectoryLabel.setText(selectedSourceDirectory.getAbsolutePath());
    } else {
      this.sourceDirectorySelected.set(null);
      this.sourceDirectoryLabel.setText("No source directory selected");
    }
  }

  @FXML
  protected void onOpenDestinationDirectoryChooser() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    this.selectedDestinationDirectory = directoryChooser.showDialog(null);

    if (this.selectedDestinationDirectory != null) {
      this.destinationDirectorySelected.set(this.selectedDestinationDirectory.getAbsolutePath());
      this.destinationDirectoryLabel.setText(selectedDestinationDirectory.getAbsolutePath());
    } else {
      this.destinationDirectorySelected.set(null);
      this.destinationDirectoryLabel.setText("No source directory selected");
    }
  }

  @FXML
  protected void onExtract() {
    this.outputTextArea.clear();
    this.outputTextArea.appendText("▶ Initializing...\n");

    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        try {
          StringBuilder batchUpdate = new StringBuilder();
          final int[] totalFiles = {0};
          final int BATCH_SIZE = 50;

          processDirectory(MainController.this.selectedSourceDirectory, batchUpdate, totalFiles, BATCH_SIZE);

          if (!batchUpdate.isEmpty()) {
            final String finalBatch = batchUpdate.toString();

            Platform.runLater(() -> MainController.this.outputTextArea.appendText(finalBatch));
          }

          final int finalCount = totalFiles[0];

          Platform.runLater(() -> MainController.this.outputTextArea.appendText("\n▶ Total files: " + finalCount + "\n"));
        } catch (Exception e) {
          updateMessage("Error: " + e.getMessage());
        }

        return null;
      }
    };

    task.setOnFailed(e -> {
      Throwable exception = task.getException();
      Platform.runLater(() -> this.outputTextArea.appendText("Error during extraction: " + exception.getMessage() + "\n"));
    });

    task.setOnSucceeded(e -> Platform.runLater(() -> this.outputTextArea.appendText("▶ Extraction complete!\n----------\n")));

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private void processDirectory(File directory, StringBuilder batchUpdate, int[] counter, int batchSize) {
    if (directory == null || !directory.isDirectory()) return;

    File[] files = directory.listFiles();

    if (files == null) return;

    for (File file : files) {
      if (Thread.currentThread().isInterrupted()) return;

      if (file.isDirectory()) {
        processDirectory(file, batchUpdate, counter, batchSize);
      } else if (matchesSelectedFormat(file)) {
        try {
          counter[0]++;
          batchUpdate.append("Copying file: ").append(file.getAbsolutePath()).append("\n");

          Files.copy(new File(file.getAbsolutePath()).toPath(), new File(selectedDestinationDirectory, file.getName()).toPath());

          if (counter[0] % batchSize == 0) {
            final String currentBatch = batchUpdate.toString();

            Platform.runLater(() -> this.outputTextArea.appendText(currentBatch));
            batchUpdate.setLength(0);
          }
        } catch (IOException e) {
          batchUpdate.append("Error copying file: ").append(e.getMessage()).append("\n");
        }
      }
    }
  }

  private boolean matchesSelectedFormat(File file) {
    String fileName = file.getName().toLowerCase();

    return switch (this.selectedFileFormat) {
      case FileTypes.IMAGES ->
        fileName.endsWith(".jpg")
        || fileName.endsWith(".jpeg")
        || fileName.endsWith(".png")
        || fileName.endsWith(".gif")
        || fileName.endsWith(".bmp");
      case FileTypes.VIDEOS ->
        fileName.endsWith(".mp4")
        || fileName.endsWith(".avi")
        || fileName.endsWith(".mov")
        || fileName.endsWith(".mkv");
      default -> true;
    };
  }
}
