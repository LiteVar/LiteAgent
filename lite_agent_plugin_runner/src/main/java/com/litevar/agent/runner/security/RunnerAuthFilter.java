package com.litevar.agent.runner.security;

import com.litevar.agent.runner.config.RunnerProperties;
import com.litevar.agent.runner.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.runner.service.PairingService;
import com.litevar.agent.runner.service.RunnerErrorCode;
import com.litevar.agent.runner.service.RunnerException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runner HMAC authentication filter.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Component
public class RunnerAuthFilter extends OncePerRequestFilter {

    private final PairingService pairingService;
    private final RunnerProperties runnerProperties;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public RunnerAuthFilter(PairingService pairingService, RunnerProperties runnerProperties, CryptoService cryptoService,
                            ObjectMapper objectMapper) {
        this.pairingService = pairingService;
        this.runnerProperties = runnerProperties;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isUploadPath(request)) {
            if (!pairingService.isKeyReady()) {
                writeError(response, new RunnerException(RunnerErrorCode.KEY_NOT_READY, null));
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        if (!pairingService.isKeyReady()) {
            writeError(response, new RunnerException(RunnerErrorCode.KEY_NOT_READY, null));
            return;
        }
        String ts = request.getHeader("X-TS");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");
        if (!StringUtils.hasText(ts) || !StringUtils.hasText(nonce) || !StringUtils.hasText(sign)) {
            writeError(response, new RunnerException(RunnerErrorCode.INVALID_SIGNATURE, "missing headers"));
            return;
        }
        long tsValue;
        try {
            tsValue = Long.parseLong(ts);
        } catch (NumberFormatException ex) {
            writeError(response, new RunnerException(RunnerErrorCode.INVALID_SIGNATURE, "invalid timestamp"));
            return;
        }
        long now = Instant.now().toEpochMilli();
        long windowMillis = runnerProperties.getSecurity().getTimeWindowSeconds() * 1000L;
        if (Math.abs(now - tsValue) > windowMillis) {
            writeError(response, new RunnerException(RunnerErrorCode.INVALID_SIGNATURE, "timestamp out of window"));
            return;
        }
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        byte[] body = wrapped.getCachedBody();
        String bodySha = body.length == 0 ? "" : cryptoService.sha256Hex(body);
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            contentType = "";
        }
        String canonical = request.getMethod().toUpperCase() + "\n"
                + request.getRequestURI() + "\n"
                + ts + "\n"
                + nonce + "\n"
                + normalizeQuery(request.getParameterMap()) + "\n"
                + bodySha + "\n"
                + contentType;
        String expected = cryptoService.hmacSha256(pairingService.getSharedKey(), canonical);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sign.getBytes(StandardCharsets.UTF_8))) {
            writeError(response, new RunnerException(RunnerErrorCode.INVALID_SIGNATURE, "signature mismatch"));
            return;
        }
        filterChain.doFilter(wrapped, response);
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if ("/runner/health".equals(path) || "/error".equals(path)) {
            return true;
        }
        return false;
    }

    private boolean isUploadPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.matches("^/runner/plugins/[^/]+/package$");
    }

    private String normalizeQuery(Map<String, String[]> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        params.keySet().stream().sorted().forEach(key -> {
            String[] values = params.get(key);
            if (values == null || values.length == 0) {
                pairs.add(encode(key) + "=");
                return;
            }
            List<String> sortedValues = new ArrayList<>(List.of(values));
            sortedValues.sort(String::compareTo);
            for (String value : sortedValues) {
                pairs.add(encode(key) + "=" + encode(value));
            }
        });
        return String.join("&", pairs);
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }

    private void writeError(HttpServletResponse response, RunnerException ex) throws IOException {
        ErrorResponse body = new ErrorResponse(ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), ex.getDetail());
        response.setStatus(ex.getErrorCode().getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
