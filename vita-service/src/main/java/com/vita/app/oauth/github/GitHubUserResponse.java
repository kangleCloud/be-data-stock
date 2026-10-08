package com.vita.app.oauth.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** GitHub 用户接口响应。 */
@Data
class GitHubUserResponse {
    private Long id;
    private String login;
    private String name;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("html_url")
    private String htmlUrl;
}
