package hello;

import com.slack.api.bolt.App;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.PlainTextObject;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jayway.jsonpath.*;
import com.quincycheng.cyberark.cem.entities.Remediation;
import com.quincycheng.cyberark.cem.entities.RemediationPolicy;

import static com.slack.api.model.view.Views.*;
import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

public class SlackApp {
    public static App get() {
        var app = new App();
        app.event(AppMentionEvent.class, (req, ctx) -> {
            var userId = req.getEvent().getUser();
            ctx.say("Hey there, <@" + userId + ">! The cache time is " + Cache.getInstance().getTime().toString());
            return ctx.ack();
        });

        app.blockAction("view-fix", (req, ctx) -> {
            // String value = req.getPayload().getActions().get(0).getValue(); // "button's
            // value"
            ctx.ack();

            View fixView = view(view -> view.callbackId("fix-view-details").type("modal").notifyOnClose(false)
                    .title(viewTitle(title -> title.type("plain_text").text("Preview Fix").emoji(true)))
                    .close(viewClose(close -> close.type("plain_text").text("Close").emoji(true)))
                    .blocks(asBlocks(section(section -> section.blockId("category-block")
                            .text(markdownText(":loading: :cloud: Now Loading...")))

                    )));

            ViewsOpenResponse viewsOpenRes = ctx.client().viewsOpen(r -> r.triggerId(ctx.getTriggerId()).view(fixView));

            if (req.getPayload().getResponseUrl() != null) {

                String body = req.getRequestBodyAsString();
                String payload = URLDecoder.decode(body.split("payload=")[1], "UTF-8");
                String checkboxPath = "$.state.values.*.checkboxes-action.selected_options.[*].value";
                String selectPath = "$.state.values.*.static_select-action.selected_option.value";

                ReadContext ctxPayload = JsonPath.parse(payload);

                // Get User Input
                String checkboxString = ctxPayload.read(checkboxPath).toString();
                String selectString = ctxPayload.read(selectPath).toString();

                String[] selectedEntites = checkboxString.replaceAll("\\[", "").replaceAll("\\]", "")
                        .replaceAll("\\s", "").split(",");
                String selectedOption = selectString.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "")
                        .replaceAll("\"", "");

                List<LayoutBlock> theBlocks = new ArrayList<LayoutBlock>();

                if (selectedEntites.length == 0 || (selectedEntites.length == 1 && selectedEntites[0].equals(""))) {
                    // No entities is selected
                    theBlocks.add(section(section -> section
                            .text(markdownText(":rotating_light: Please select 1 or more entities to generate fix"))));

                } else {

                    switch (selectedOption) {
                    case "LEAST_PRIVILEGE_REMOVE_SHADOW_PERMISSIONS":
                        theBlocks.add(section(section -> section.text(markdownText(
                                "The following are the preview of commands & policies for the selected cloud entities that _*remove all shadow admin permission*_"))));
                        break;
                    case "LEAST_PRIVILEGE":
                        theBlocks.add(section(section -> section.text(markdownText(
                                "The following are the preview of commands & policies for the selected cloud entities that _*keep only shadow admin permission that are in use*_"))));
                        break;
                    case "LEAST_PRIVILEGE_KEEP_ALL_SHADOW_PERMISSIONS":
                        theBlocks.add(section(section -> section.text(markdownText(
                                "The following are the preview of commands & policies for the selected cloud entities that _*keep all shadow admin permissions*_"))));
                        break;
                    default:
                        theBlocks.add(section(section -> section.text(markdownText(selectedOption))));
                        break;
                    }

                    // 1 or more entities selected
                    for (String theEntityString : selectedEntites) {

                        if (Cache.getInstance().isViewCached(selectedOption, theEntityString)) {

                            theBlocks.addAll(Cache.getInstance().getView(selectedOption, theEntityString));
                        } else {

                            List<LayoutBlock> theEntityBlocks = new ArrayList<LayoutBlock>();

                            String[] theEntity = theEntityString.replaceAll("\\\\/", "/").replaceAll("\"", "")
                                    .split(Pattern.quote("^^"));

                            CEMApi cemApi = new CEMApi();

                            Remediation theFix = cemApi.getRemediation(selectedOption, theEntity[0], theEntity[1],
                                    theEntity[2]);

                            theEntityBlocks.add(divider());

                            theEntityBlocks.add(
                                    section(section -> section.text(markdownText(":cloud: *" + theEntity[2] + "*"))));

                            // Context
                            List<ContextBlockElement> contextBlockElements = new ArrayList<>();
                            contextBlockElements.add(PlainTextObject.builder()
                                    .text("Platform: " + theEntity[0] + ", Account: " + theEntity[1]).build());
                            theEntityBlocks.add(ContextBlock.builder().elements(contextBlockElements).build());

                            if (!theFix.getCommand().equals("")) {
                                theEntityBlocks.add(section(section -> section
                                        .text(markdownText("*Command*\n ```" + theFix.getCommand() + "```"))));

                                if (theFix.getPolicyList().size() > 0) {

                                    for (RemediationPolicy thePolicy : theFix.getPolicyList()) {
                                        theEntityBlocks.add(section(section -> section
                                                .text(markdownText("*Policy " + thePolicy.getPolicyName() + "*\n```"
                                                        + thePolicy.getPolicyContent() + "```"))));
                                    }
                                }
                            } else {
                                theEntityBlocks.add(section(section -> section.text(markdownText(
                                        "Only manual fixes are avaliable.   Please refer to <https://cem.cyberark.com/cloud-entities/details?platform="
                                                + theEntity[0] + "&accountId=" + theEntity[1] + "&entityId="
                                                + theEntity[2] + "&section=recommendations|CEM> for details"))));
                            }

                            theBlocks.addAll(theEntityBlocks);
                            Cache.getInstance().updateView(selectedOption, theEntityString, theEntityBlocks);
                        }
                    }
                }

                View newView = viewsOpenRes.getView();
                fixView.setBlocks(theBlocks);
                ctx.client().viewsUpdate(r -> r.viewId(newView.getId()).hash(fixView.getHash()).view(fixView));

            }
            return ctx.ack();
        });

        app.blockAction("apply-fix", (req, ctx) -> {

            if (req.getPayload().getResponseUrl() != null) {

                // Post a message to the same channel if it's a block in a message
                ctx.respond(":star2: Working with Jenkins to apply fixes\nIt will take a few moments");

                String body = req.getRequestBodyAsString();
                String payload = URLDecoder.decode(body.split("payload=")[1], "UTF-8");
                String checkboxPath = "$.state.values.*.checkboxes-action.selected_options.[*].value";
                String selectPath = "$.state.values.*.static_select-action.selected_option.value";

                ReadContext ctxPayload = JsonPath.parse(payload);

                // Get User Input
                String checkboxString = ctxPayload.read(checkboxPath).toString();
                String leastPrivilegeOptionString = ctxPayload.read(selectPath).toString();

                checkboxString = checkboxString.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "");
                leastPrivilegeOptionString = leastPrivilegeOptionString.replaceAll("\\[", "").replaceAll("\\]", "")
                        .replaceAll("\\s", "").replaceAll("\"", "");

                JenkinsApi jenkinsAPI = new JenkinsApi();

                jenkinsAPI.applyFix(leastPrivilegeOptionString, checkboxString, req.getPayload().getUser().getId(),
                        req.getPayload().getChannel().getName());

            }
            return ctx.ack();
        });

        app.blockAction("checkboxes-action", (req, ctx) -> {
            // Do nothing
            return ctx.ack();
        });

        app.blockAction("static_select-action", (req, ctx) -> {
            // Do nothing
            return ctx.ack();
        });

        return app;
    }

}