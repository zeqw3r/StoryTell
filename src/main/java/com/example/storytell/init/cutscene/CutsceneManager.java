// CutsceneManager.java
package com.example.storytell.init.cutscene;

import net.minecraft.client.gui.GuiGraphics;

public class CutsceneManager {
    private static CutsceneManager instance;
    private Cutscene currentCutscene;

    public static CutsceneManager getInstance() {
        if (instance == null) {
            instance = new CutsceneManager();
        }
        return instance;
    }

    public void startCutscene(String folderName) {
        System.out.println("CutsceneManager: Starting cutscene " + folderName);

        if (currentCutscene != null) {
            System.out.println("Stopping previous cutscene");
            stopCutscene();
        }

        currentCutscene = new Cutscene(folderName);
        if (currentCutscene.hasImages()) {
            currentCutscene.start();
            System.out.println("Cutscene started successfully");
        } else {
            System.out.println("CutsceneManager: No images found for cutscene " + folderName);
            currentCutscene = null;
        }
    }

    public void stopCutscene() {
        if (currentCutscene != null) {
            System.out.println("Stopping current cutscene");
            currentCutscene.stop();
            currentCutscene = null;
        }
    }

    public boolean isCutscenePlaying() {
        return currentCutscene != null && currentCutscene.isActive();
    }

    public void render(GuiGraphics guiGraphics) {
        if (currentCutscene != null) {
            currentCutscene.render(guiGraphics);
        }
    }

    public void tick() {
        if (currentCutscene != null) {
            currentCutscene.tick();
        }
    }
}