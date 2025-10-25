// WorldModelCommand.java - добавим команды для управления вращением
package com.example.storytell.init.world;

import com.example.storytell.init.network.AddWorldModelPacket;
import com.example.storytell.init.network.MoveWorldModelPacket;
import com.example.storytell.init.network.RemoveWorldModelPacket;
import com.example.storytell.init.network.SetRotationSpeedPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;

public class WorldModelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("worldmodel")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("model", StringArgumentType.string())
                                        .then(Commands.argument("x", FloatArgumentType.floatArg())
                                                .then(Commands.argument("y", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("z", FloatArgumentType.floatArg())
                                                                .then(Commands.argument("size", FloatArgumentType.floatArg())
                                                                        .then(Commands.argument("color", IntegerArgumentType.integer())
                                                                                .executes(context -> {
                                                                                    String id = StringArgumentType.getString(context, "id");
                                                                                    String modelStr = StringArgumentType.getString(context, "model");
                                                                                    float x = FloatArgumentType.getFloat(context, "x");
                                                                                    float y = FloatArgumentType.getFloat(context, "y");
                                                                                    float z = FloatArgumentType.getFloat(context, "z");
                                                                                    float size = FloatArgumentType.getFloat(context, "size");
                                                                                    int color = IntegerArgumentType.getInteger(context, "color");

                                                                                    ResourceLocation modelLocation = new ResourceLocation("storytell", modelStr);

                                                                                    AddWorldModelPacket packet = new AddWorldModelPacket(id, modelLocation, x, y, z, size, color);
                                                                                    com.example.storytell.init.network.NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

                                                                                    context.getSource().sendSuccess(() -> Component.literal("Added world model " + id), true);
                                                                                    return 1;
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    RemoveWorldModelPacket packet = new RemoveWorldModelPacket(id);
                                    com.example.storytell.init.network.NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
                                    context.getSource().sendSuccess(() -> Component.literal("Removed world model " + id), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("move")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("x", FloatArgumentType.floatArg())
                                        .then(Commands.argument("y", FloatArgumentType.floatArg())
                                                .then(Commands.argument("z", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("easing", StringArgumentType.string())
                                                                        .suggests((context, builder) -> {
                                                                            builder.suggest("easeInQuad");
                                                                            builder.suggest("easeInCubic");
                                                                            builder.suggest("linear");
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .executes(context -> {
                                                                            String id = StringArgumentType.getString(context, "id");
                                                                            float x = FloatArgumentType.getFloat(context, "x");
                                                                            float y = FloatArgumentType.getFloat(context, "y");
                                                                            float z = FloatArgumentType.getFloat(context, "z");
                                                                            int duration = IntegerArgumentType.getInteger(context, "duration");
                                                                            String easing = StringArgumentType.getString(context, "easing");

                                                                            MoveWorldModelPacket packet = new MoveWorldModelPacket(id, x, y, z, duration, easing);
                                                                            com.example.storytell.init.network.NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

                                                                            context.getSource().sendSuccess(() ->
                                                                                    Component.literal("Moving world model " + id + " to (" + x + ", " + y + ", " + z +
                                                                                            ") over " + duration + "ms with easing: " + easing), true);
                                                                            return 1;
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("rotation")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("speedX", FloatArgumentType.floatArg())
                                        .then(Commands.argument("speedY", FloatArgumentType.floatArg())
                                                .then(Commands.argument("speedZ", FloatArgumentType.floatArg())
                                                        .executes(context -> {
                                                            String id = StringArgumentType.getString(context, "id");
                                                            float speedX = FloatArgumentType.getFloat(context, "speedX");
                                                            float speedY = FloatArgumentType.getFloat(context, "speedY");
                                                            float speedZ = FloatArgumentType.getFloat(context, "speedZ");

                                                            SetRotationSpeedPacket packet = new SetRotationSpeedPacket(id, speedX, speedY, speedZ);
                                                            com.example.storytell.init.network.NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

                                                            context.getSource().sendSuccess(() ->
                                                                    Component.literal("Set rotation speed for " + id + " to (" + speedX + ", " + speedY + ", " + speedZ + ")"), true);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }
}