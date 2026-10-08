package com.vita.app.oauth.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** GitHub 令牌接口响应。 */
@Data
class GitHubTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    private String error;
}
