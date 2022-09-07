package com.example.docker_demo;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull Object handler
    ) throws Exception {
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

//    @Override
//    public void postHandle(
//            @NotNull HttpServletRequest request,
//            @NotNull HttpServletResponse response,
//            @NotNull Object handler,
//            ModelAndView modelAndView
//    ) throws Exception {
//        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
//    }

    @Override
    public void afterCompletion(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull Object handler,
            Exception ex
    ) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
