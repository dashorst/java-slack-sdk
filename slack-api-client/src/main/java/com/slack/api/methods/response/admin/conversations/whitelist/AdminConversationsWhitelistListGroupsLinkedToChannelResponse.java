package com.slack.api.methods.response.admin.conversations.whitelist;

import com.slack.api.methods.SlackApiResponse;
import com.slack.api.model.ResponseMetadata;
import lombok.Data;

import java.util.List;

@Deprecated
@Data
public class AdminConversationsWhitelistListGroupsLinkedToChannelResponse implements SlackApiResponse {

    private boolean ok;
    private String warning;
    private String error;
    private String needed;
    private String provided;

    private List<String> groupIds;
    private ResponseMetadata responseMetadata;
}