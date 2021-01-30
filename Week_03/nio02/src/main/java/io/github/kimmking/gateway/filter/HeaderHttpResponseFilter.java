package io.github.kimmking.gateway.filter;

import io.netty.handler.codec.http.FullHttpResponse;

public class HeaderHttpResponseFilter implements HttpResponseFilter {
    @Override
    public void filter(FullHttpResponse response) {
        response.headers().set("random-response-header-1", "I did the homework");
        response.headers().set("random-response-header-2", "Is this actually working?");
    }
}
