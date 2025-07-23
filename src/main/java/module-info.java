module com.cmaxhm.fileextractor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens com.cmaxhm.fileextractor to javafx.fxml;
    exports com.cmaxhm.fileextractor;
}