package xyz.sunrose.xps;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XpShare implements ModInitializer {
    public static final String MODID = "xpshare";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final int XP_PER_LEVEL = 46;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<ServerCommandSource> xpshareNode = CommandManager
                    .literal("xpshare")
                    .build();

            ArgumentCommandNode<ServerCommandSource, EntitySelector> levelsGivenPlayerNode = CommandManager
                    .argument("player", EntityArgumentType.player())
                    .build();

            ArgumentCommandNode<ServerCommandSource, Integer> numLevelsGivenNode = CommandManager
                    .argument("levels", IntegerArgumentType.integer(0))
                    .executes((context) -> give(context))
                    .build();


            dispatcher.getRoot().addChild(xpshareNode);

            xpshareNode.addChild(levelsGivenPlayerNode);
            levelsGivenPlayerNode.addChild(numLevelsGivenNode);
        });
    }

    public static int give(CommandContext<ServerCommandSource> context) throws CommandSyntaxException{
        ServerCommandSource src = context.getSource();
        Entity e = src.getEntity();
        if(e instanceof ServerPlayerEntity giver) {
            ServerPlayerEntity reciever = context.getArgument("player", EntitySelector.class).getPlayer(src);
            int levelsToGive = context.getArgument("levels", Integer.class);
            int levelsHave = giver.experienceLevel;


            if (levelsHave >= levelsToGive) {
                Text feedback = MutableText.of(new LiteralTextContent("Given " + levelsToGive + " levels to " + reciever.getDisplayName().getString()));
                Text message = MutableText.of(new LiteralTextContent("Recieved " + levelsToGive + " levels from " + giver.getDisplayName().getString()));
                giver.addExperience(-levelsToGive * XP_PER_LEVEL);
                reciever.addExperience(levelsToGive * XP_PER_LEVEL);
                src.sendFeedback(feedback, true);
                reciever.sendMessage(message);
                return 1;
            }
        }
        return 0;
    }
}
