module com.tvz.java.jamb {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.graphics;
    requires lombok;
    requires java.desktop;
    requires java.rmi;

    exports com.tvz.java.jamb.rmi to java.rmi;

    opens com.tvz.java.jamb to javafx.fxml;
    exports com.tvz.java.jamb;
}