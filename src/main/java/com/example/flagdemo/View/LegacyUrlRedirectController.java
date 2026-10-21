package com.example.flagdemo.View;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The flag game used to live under "/Flaggle" before the site was renamed to MundoFlag.
 * Old links (bookmarks, shared invites) are sent to the same page under "/Flags",
 * keeping the rest of the path and the query string.
 */
@Controller
public class LegacyUrlRedirectController {

    private static final String OLD_PREFIX = "/Flaggle";
    private static final String NEW_PREFIX = "/Flags";

    @GetMapping({OLD_PREFIX, OLD_PREFIX + "/**"})
    public ResponseEntity<Void> redirectOldFlagGameUrl(HttpServletRequest request) {
        String rest = request.getRequestURI().substring(request.getContextPath().length() + OLD_PREFIX.length());
        String query = request.getQueryString();
        String target = NEW_PREFIX + rest + (query != null ? "?" + query : "");
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, target).build();
    }
}
