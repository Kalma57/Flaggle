package com.example.flagdemo.View;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Gives every page the site's own absolute address ("siteUrl", e.g. https://mundoflag.onrender.com),
 * which link previews (LinkedIn, WhatsApp...) need for their image. It is read from the current
 * request, so it follows the site to any host or domain without a hard-coded URL; behind a proxy
 * the original https scheme and host come from the forwarded headers
 * (server.forward-headers-strategy in application.properties).
 */
@ControllerAdvice
public class SiteMetaAdvice {

    @ModelAttribute("siteUrl")
    public String siteUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
