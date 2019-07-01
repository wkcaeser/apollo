package com.ctrip.framework.apollo.portal.interceptor;

import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;


@Component
public class AuditInterceptor implements HandlerInterceptor {

    @Value("${ES_HOST}")
    private String esHost;

    @Value("${ES_SECRET}")
    private String esSecret;

    private UserInfoHolder userInfoHolder;

    private final AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();


    public AuditInterceptor(UserInfoHolder userInfoHolder) {
        this.userInfoHolder = userInfoHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", Instant.now().toEpochMilli());
        jsonObject.addProperty("method", request.getMethod());
        jsonObject.addProperty("uri", request.getRequestURI());
        jsonObject.addProperty("ip", request.getRemoteHost());
        jsonObject.addProperty("user_id", userInfoHolder.getUser().getUserId());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        httpHeaders.set("Authorization", esSecret);
        HttpEntity<String> httpEntity = new HttpEntity<>(jsonObject.toString(), httpHeaders);
        asyncRestTemplate.exchange(esHost+ "/apollo-audit/doc", HttpMethod.POST, httpEntity, String.class).completable();

        return true;
    }
}
