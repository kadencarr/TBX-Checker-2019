package org.ttt.salt.gui;

public class Singleton {

    private static Singleton singletonInstance;
    private int versionSelection;

    static {
        initialize();
    }

    private static void initialize() {
        singletonInstance = new Singleton();
    }

    public static Singleton getSingletonInstance() {
        return singletonInstance;
    }

    public void setVersionSelection(String versionSelection) {
        if (versionSelection.equals("validate-two")) {
            this.versionSelection = 0;
        }
        else if (versionSelection.equals("validate-three")) {
            this.versionSelection = 1;
        }
    }

    public int getVersionSelection() {
        return versionSelection;
    }
}
