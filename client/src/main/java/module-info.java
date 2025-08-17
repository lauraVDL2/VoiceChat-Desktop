module com.voicechat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.graphics;
    requires io.github.resilience4j.retry;
    requires voicechat.common;
    requires com.fasterxml.jackson.databind;

    opens com.voicechat.client to javafx.fxml;
    opens com.voicechat.client.login to javafx.fxml;
    opens com.voicechat.client.mainpage to javafx.fxml;

    exports com.voicechat.client;
    exports com.voicechat.client.login;
    exports com.voicechat.client.login.controller;
    exports com.voicechat.client.mainpage;

    opens com.voicechat.client.login.controller to javafx.fxml;
}
