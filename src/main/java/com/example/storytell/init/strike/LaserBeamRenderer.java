// LaserBeamRenderer.java
package com.example.storytell.init.strike;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

public class LaserBeamRenderer {
    public static void createTextureBeam(ServerLevel level, Vec3 start, Vec3 end) {
        // Используем только частицы для создания лазерного луча
        createLaserWithParticles(level, start, end);
    }

    private static void createLaserWithParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        direction = direction.normalize();

        // Создаем плотные частицы вдоль линии луча
        int particleCount = (int) (length * 15); // 15 частиц на блок для большей плотности
        double step = length / particleCount;

        for (int i = 0; i < particleCount; i++) {
            double progress = i * step;
            Vec3 particlePos = start.add(direction.scale(progress));

            // Основные красные частицы луча
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 2.0f),
                    particlePos.x, particlePos.y, particlePos.z,
                    5, // количество частиц в этой позиции
                    0.05, 0.05, 0.05, // маленький разброс для плотного луча
                    0.0 // скорость
            );

            // Белые частицы для ядра луча (более яркий центр)
            if (i % 2 == 0) {
                level.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 1.5f),
                        particlePos.x, particlePos.y, particlePos.z,
                        3, 0.02, 0.02, 0.02, 0.0
                );
            }

            // Синие/фиолетовые частицы для эффекта энергии
            if (i % 5 == 0) {
                level.sendParticles(
                        new DustParticleOptions(new Vector3f(0.5f, 0.2f, 1.0f), 1.0f),
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0.1, 0.1, 0.01
                );
            }
        }

        // Эффект на концах луча
        createBeamEndEffects(level, start, end);

        // Анимируем луч в течение 2 секунд
        animateBeam(level, start, end, 40); // 40 тиков = 2 секунды
    }

    private static void createBeamEndEffects(ServerLevel level, Vec3 start, Vec3 end) {
        // Эффект у начала луча (в небе) - заряженная энергия
        for (int i = 0; i < 25; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.8f, 0.8f, 1.0f), 2.0f),
                    start.x + offsetX, start.y + offsetY, start.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.05
            );
        }

        // Эффект у конца луча (у земли) - взрыв энергии
        for (int i = 0; i < 40; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 3;

            // Красные и оранжевые частицы
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.1f), 2.0f),
                    end.x + offsetX, end.y + offsetY, end.z + offsetZ,
                    5, 0.1, 0.1, 0.1, 0.1
            );

            // Огненные частицы
            if (i % 3 == 0) {
                level.sendParticles(
                        ParticleTypes.FLAME,
                        end.x + offsetX * 0.5, end.y + offsetY * 0.5, end.z + offsetZ * 0.5,
                        2, 0.05, 0.05, 0.05, 0.02
                );
            }

            // Дым
            if (i % 4 == 0) {
                level.sendParticles(
                        ParticleTypes.SMOKE,
                        end.x + offsetX, end.y + offsetY, end.z + offsetZ,
                        1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }
    }

    private static void animateBeam(ServerLevel level, Vec3 start, Vec3 end, int durationTicks) {
        // Анимация пульсации луча в течение заданного времени
        new Thread(() -> {
            try {
                for (int tick = 0; tick < durationTicks; tick++) {
                    final int currentTick = tick; // Создаем final копию для использования в лямбде
                    Thread.sleep(50); // 20 тиков в секунду

                    level.getServer().execute(() -> {
                        // Периодически добавляем дополнительные частицы для эффекта пульсации
                        if (currentTick % 5 == 0) {
                            createPulseEffect(level, start, end);
                        }

                        // Особо интенсивный эффект в начале и в конце
                        final int finalDurationTicks = durationTicks; // Создаем final копию
                        if (currentTick < 10 || currentTick > finalDurationTicks - 10) {
                            createIntenseBeamEffect(level, start, end);
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void createPulseEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double length = start.distanceTo(end);

        // Создаем волну частиц вдоль луча
        for (int i = 0; i < 3; i++) {
            double progress = Math.random();
            Vec3 pulsePos = start.add(direction.scale(length * progress));

            // Яркие белые частицы для эффекта пульсации
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 2.5f),
                    pulsePos.x, pulsePos.y, pulsePos.z,
                    8, 0.3, 0.3, 0.3, 0.05
            );
        }
    }

    private static void createIntenseBeamEffect(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double length = start.distanceTo(end);

        // Дополнительные частицы для интенсивного эффекта
        for (int i = 0; i < 20; i++) {
            double progress = Math.random();
            Vec3 particlePos = start.add(direction.scale(length * progress));

            // Разноцветные частицы для интенсивного эффекта
            float r = (float) Math.random();
            float g = (float) Math.random() * 0.5f;
            float b = 1.0f;

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(r, g, b), 1.5f),
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.2, 0.2, 0.2, 0.02
            );
        }
    }
}