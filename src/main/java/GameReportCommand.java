import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class GameReportCommand extends Command {

    private final EventWaiter waiter; // This variable is used to define the waiter, and call it from anywhere in this class.

    public String suspect;
    public String reason;
    public String evidence;

    public GameReportCommand(EventWaiter waiter) {
        this.waiter = waiter; // Define the waiter
        this.name = "gamereport"; // The command
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event){
        if(!event.isFromType(ChannelType.PRIVATE)) return;

        if(Storage.checkUserOpenTicket(event.getAuthor().getId()) || Storage.checkUserActiveAppeal(event.getAuthor().getId())){
            event.replyInDm("You already have a ticket open.");
            return;
        }

        //checks is the user has made 5 reports in the last hour
        if(SpamPrevention.checkTimeReports(event.getAuthor().getId())){
            SpamPrevention x = SpamPrevention.spamPreventionReport(event.getAuthor().getId());

            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(new Color(102, 214, 238));
            eb.setAuthor("Report ticket error");
            eb.setDescription("You have made 5 tickets in the last hour. You may make another ticket in " + x.minutes + " minutes & " + x.seconds + " seconds.");

            event.getPrivateChannel().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if(BlackList.isBlackList(event.getAuthor().getId())){
            event.replyInDm("You are blacklisted from making tickets");
            return;
    }

        Storage.addUserToOpenTicket(event.getAuthor().getId());

        event.replyInDm(askForUsername().build()); // Respond to the command with a message.

        // wait for a response
        waiter.waitForEvent(MessageReceivedEvent.class,
                // make sure it's by the same user, and in the same channel, and for safety, a different message
                e -> e.getAuthor().equals(event.getAuthor())
                        && e.getChannel().equals(event.getChannel())
                        && !e.getMessage().getContentRaw().equals(event.getMessage().getContentRaw())
                        && e.getMessage().getAttachments().isEmpty(),
                // response
                e1 -> {
                    event.replyInDm(askForReason().build());
                    suspect = e1.getMessage().getContentRaw();
                    waiter.waitForEvent(MessageReceivedEvent.class,
                            // make sure it's by the same user, and in the same channel, and for safety, a different message
                            e -> e.getAuthor().equals(event.getAuthor())
                                    && e.getChannel().equals(event.getChannel())
                                    && !e.getMessage().getContentRaw().equals(e1.getMessage().getContentRaw())
                                    && !e.getMessage().getContentRaw().equals(event.getMessage().getContentRaw())
                                    && e.getMessage().getAttachments().isEmpty(),
                            // response
                            e2 -> {
                                event.replyInDm(askForEvidence().build());
                                reason = e2.getMessage().getContentRaw();
                                waiter.waitForEvent(MessageReceivedEvent.class,
                                        // make sure it's by the same user, and in the same channel, and for safety, a different message
                                        e -> e.getAuthor().equals(event.getAuthor())
                                                && e.getChannel().equals(event.getChannel())
                                                && !e.getMessage().getContentRaw().equals(e1.getMessage().getContentRaw())
                                                && !e.getMessage().getContentRaw().equals(e2.getMessage().getContentRaw())
                                                && !e.getMessage().getContentRaw().equals(event.getMessage().getContentRaw())
                                                && e.getMessage().getAttachments().isEmpty(),
                                        // response
                                        e3 -> {
                                            evidence = e3.getMessage().getContentRaw();
                                            event.replyInDm("Thank you for reporting");
                                            Storage.removeUserFromOpenTicket(event.getAuthor().getId());
                                            ReportTicket.reportFormat(new ReportTicket(event.getAuthor().getId(), suspect, evidence, System.currentTimeMillis(), reason), event.getJDA());

                                        },
                                        // if the user takes more than 3 minutes, time out and remove user from openTicket storage
                                        1, TimeUnit.MINUTES, () -> {event.replyInDm("Sorry, you took too long. Feel free to try again."); Storage.removeUserFromOpenTicket(event.getAuthor().getId());});

                            },
                            // if the user takes more than 3 minutes, time out and remove user from openTicket storage
                            1, TimeUnit.MINUTES, () -> {event.replyInDm("Sorry, you took too long. Feel free to try again."); Storage.removeUserFromOpenTicket(event.getAuthor().getId());});
                },
                // if the user takes more than 3 minutes, time out and remove user from openTicket storage
                1, TimeUnit.MINUTES, () -> {event.replyInDm("Sorry, you took too long. Feel free to try again."); Storage.removeUserFromOpenTicket(event.getAuthor().getId());});
    }


    public EmbedBuilder askForUsername(){
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(102, 214, 238));
        eb.setTitle("What is the username of the user you are reporting?");
        eb.setDescription("> Make sure you type the username correctly.");

        return eb;
    }

    public EmbedBuilder askForReason(){
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(102, 214, 238));
        eb.setTitle("What is the reason you are reporting this user?");
        eb.setDescription("> Please elaborate on what this user has done.");

        return eb;
    }

    public EmbedBuilder askForEvidence(){
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(new Color(102, 214, 238));
        eb.setTitle("What evidence do you have of this occurring?");
        eb.setDescription("> If you want to post images please use https://imgur.com\n\n> If you want to post a video please use https://www.youtube.com or any other reliable site.");
        eb.setFooter(">> We do not accept any type of files <<");

        return eb;
    }
}
