package xyz.sunrose.xps;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XpShare implements ModInitializer {
    public static final String MODID = "xpshare";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final String LV_GIVEN = "levels-given";
    public static final String LV_RECIEVED = "levels-recieved";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralCommandNode<ServerCommandSource> xpshareNode = CommandManager
                    .literal("xpshare")
                    .build();

            LiteralCommandNode<ServerCommandSource> levelsGivenNode = CommandManager
                    .literal(LV_GIVEN)
                    .build();

            LiteralCommandNode<ServerCommandSource> levelsRecievedNode = CommandManager
                    .literal(LV_RECIEVED)
                    .build();

            LiteralCommandNode<ServerCommandSource> pointsNode = CommandManager
                    .literal("points")
                    .build();

            ArgumentCommandNode<ServerCommandSource, EntitySelector> levelsGivenPlayerNode = CommandManager
                    .argument("player", EntityArgumentType.player())
                    .build();
            ArgumentCommandNode<ServerCommandSource, EntitySelector> levelsRecievedPlayerNode = CommandManager
                    .argument("player", EntityArgumentType.player())
                    .build();
            ArgumentCommandNode<ServerCommandSource, EntitySelector> pointsPlayerNode = CommandManager
                    .argument("player", EntityArgumentType.player())
                    .build();

            ArgumentCommandNode<ServerCommandSource, Integer> numLevelsGivenNode = CommandManager
                    .argument("levels", IntegerArgumentType.integer(0))
                    .executes((context) -> give(context, TransferType.LEVELS_GIVE))
                    .build();
            ArgumentCommandNode<ServerCommandSource, Integer> numLevelsRecievedNode = CommandManager
                    .argument("levels", IntegerArgumentType.integer(0))
                    .executes((context -> give(context, TransferType.LEVELS_RECIEVE)))
                    .build();
            ArgumentCommandNode<ServerCommandSource, Integer> numPointsNode = CommandManager
                    .argument("points", IntegerArgumentType.integer(0))
                    .executes(context -> give(context, TransferType.POINTS))
                    .build();


            dispatcher.getRoot().addChild(xpshareNode);

            xpshareNode.addChild(levelsGivenNode);
            levelsGivenNode.addChild(levelsGivenPlayerNode);
            levelsGivenPlayerNode.addChild(numLevelsGivenNode);

            xpshareNode.addChild(levelsRecievedNode);
            levelsRecievedNode.addChild(levelsRecievedPlayerNode);
            levelsRecievedPlayerNode.addChild(numLevelsRecievedNode);

            xpshareNode.addChild(pointsNode);
            pointsNode.addChild(pointsPlayerNode);
            pointsPlayerNode.addChild(numPointsNode);
        });
    }

    private enum TransferType {
        LEVELS_GIVE, LEVELS_RECIEVE, POINTS
    }

    public static int give(CommandContext<ServerCommandSource> context, TransferType type) throws CommandSyntaxException{
        ServerCommandSource src = context.getSource();
        Entity e = src.getEntity();
        if(e instanceof ServerPlayerEntity giver) {
            ServerPlayerEntity reciever = context.getArgument("player", EntitySelector.class).getPlayer(src);
            int levelsGive;
            int pointsGive;
            int leftoverPoints = (int) (giver.experienceProgress * giver.getNextLevelExperience());
            int pointsHave = levelRangeAsPoints(0, giver.experienceLevel) + leftoverPoints; //totalExperience isn't updated fsr, fml
            Text feedback;
            Text message;
            switch (type){
                case LEVELS_GIVE -> { //give the xp required to remove some number of levels *from the giver*
                    levelsGive = context.getArgument("levels", Integer.class);
                    int levelsHave = giver.experienceLevel;
                    pointsGive = levelRangeAsPoints(levelsHave-levelsGive, levelsHave);
                    feedback = new LiteralText("Given "+levelsGive+" of your levels to "+reciever.getDisplayName()+" ("+pointsGive+" points)");
                    message = new LiteralText("Recieved xp from "+giver.getDisplayName()); //todo: tell the level amount recieved??
                }
                case LEVELS_RECIEVE -> { //give the xp requried to add some number of levels *to the reciever*
                    levelsGive = context.getArgument("levels", Integer.class);
                    int targetLevelsHave = reciever.experienceLevel;
                    pointsGive = levelRangeAsPoints(targetLevelsHave, targetLevelsHave+levelsGive);
                    feedback = new LiteralText("Shared xp to "+reciever.getDisplayName()+" to increase their levels by "+levelsGive+" ("+pointsGive+" points)");
                    message = new LiteralText("Recieved "+levelsGive+" levels from "+giver.getDisplayName());
                }
                default -> { //just put in point values directly
                    pointsGive = context.getArgument("points", Integer.class);
                    feedback = new LiteralText("Given "+pointsGive+" experience points to "+reciever.getDisplayName());
                    message = new LiteralText("Recieved "+pointsGive+" experience points from "+giver.getDisplayName());
                }
            }

            if(pointsHave >= pointsGive) {
                giver.addExperience(-pointsGive);
                reciever.addExperience(pointsGive);
                src.sendFeedback(feedback, true);
                reciever.sendSystemMessage(message, giver.getUuid());
                return 1;
            }
        }
        return 0;
    }

    protected static int levelRangeAsPoints(int from, int to){
        int total = 0;
        if(to < from){return 0;}
        for (int i = from; i < to; i++) { //mojang whyyyy
            if(i<=15) {total += 2 * i + 7;}
            else if(i<=30) {total += 5 * i - 38;}
            else {total += 9 * i - 158;}
        }
        return total;
    }
}
