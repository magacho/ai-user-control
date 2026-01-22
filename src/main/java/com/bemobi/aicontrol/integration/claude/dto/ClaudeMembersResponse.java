package com.bemobi.aicontrol.integration.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for Claude API members list endpoint.
 */
public class ClaudeMembersResponse {

    private String object;
    private List<ClaudeMember> data;

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("first_id")
    private String firstId;

    @JsonProperty("last_id")
    private String lastId;

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<ClaudeMember> getData() {
        return data;
    }

    public void setData(List<ClaudeMember> data) {
        this.data = data;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getFirstId() {
        return firstId;
    }

    public void setFirstId(String firstId) {
        this.firstId = firstId;
    }

    public String getLastId() {
        return lastId;
    }

    public void setLastId(String lastId) {
        this.lastId = lastId;
    }
}
