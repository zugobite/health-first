package com.zugobite.pims;

import com.zugobite.pims.controllers.LoginController;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginController().start());
    }
}
