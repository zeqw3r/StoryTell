package com.example.storytell.init.cutscene;

import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CutsceneManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static CutsceneManager instance;
    private Cutscene currentCutscene;

    // Статический блок инициализации
    static {
        instance = new CutsceneManager();
    }

    public static CutsceneManager getInstance() {
        return instance;
    }

    // Приватный конструктор для синглтона
    private CutsceneManager() {}

    public void startCutscene(String folderName) {
        LOGGER.debug("Starting cutscene: {}", folderName);

        // Останавливаем предыдущую катсцену и очищаем ресурсы
        if (currentCutscene != null) {
            LOGGER.debug("Stopping previous cutscene");
            currentCutscene.cleanup();
            currentCutscene = null;
        }

        currentCutscene = new Cutscene(folderName);
        if (currentCutscene.hasImages()) {
            currentCutscene.start();
            LOGGER.debug("Cutscene started successfully");
        } else {
            LOGGER.warn("No images found for cutscene: {}", folderName);
            currentCutscene = null;
        }
    }

    public void stopCutscene() {
        if (currentCutscene != null) {
            LOGGER.debug("Stopping current cutscene");
            currentCutscene.cleanup();
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

            // Автоматическая очистка если катсцена завершилась
            if (!currentCutscene.isActive()) {
                stopCutscene();
            }
        }
    }

    // Метод для принудительной очистки всех ресурсов
    public void cleanupAll() {
        stopCutscene();
        Cutscene.clearTextureCache();
    }
}