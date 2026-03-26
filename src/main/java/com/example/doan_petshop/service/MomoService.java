package com.example.doan_petshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class MomoService {

    @Value("${momo.partner-code:MOMO}")
    private String partnerCode;

    @Value("${momo.access-key:F8BBA842ECF85}")
    private String accessKey;

    @Value("${momo.secret-key:K951B6PE1waDMi640xX08PD3vg6EkVlz}")
    private String secretKey;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${momo.ipn-url:http://localhost:8080/payment/momo/ipn}")
    private String ipnUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ===== Tạo payment url =====
    public MomoPaymentResponse createPayment(Long orderId, long amount, String orderInfo) throws Exception {
        String requestId    = partnerCode + System.currentTimeMillis();
        String momoOrderId  = partnerCode + "_" + orderId + "_" + System.currentTimeMillis();
        String redirectUrl  = appBaseUrl + "/payment/momo/return";
        String requestType  = "captureWallet";
        String extraData    = "orderId=" + orderId;

        // ===== Tạo chữ kí  =====
        String rawSignature = "accessKey="   + accessKey
                + "&amount="     + amount
                + "&extraData="  + extraData
                + "&ipnUrl="     + ipnUrl
                + "&orderId="    + momoOrderId
                + "&orderInfo="  + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId="  + requestId
                + "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("accessKey",   accessKey);
        body.put("requestId",   requestId);
        body.put("amount",      amount);
        body.put("orderId",     momoOrderId);
        body.put("orderInfo",   orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl",      ipnUrl);
        body.put("extraData",   extraData);
        body.put("requestType", requestType);
        body.put("signature",   signature);
        body.put("lang",        "vi");

        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("[MoMo] Request body: {}", jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(momoEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[MoMo] Response: {}", response.body());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

        int resultCode  = (int) responseMap.getOrDefault("resultCode", -1);
        String payUrl   = (String) responseMap.getOrDefault("payUrl", "");
        String message  = (String) responseMap.getOrDefault("message", "Unknown error");

        return new MomoPaymentResponse(resultCode, payUrl, message, momoOrderId);
    }

    public boolean verifyIpnSignature(Map<String, String> params) {
        try {
            String rawSignature = "accessKey="   + accessKey
                    + "&amount="     + params.get("amount")
                    + "&extraData="  + params.getOrDefault("extraData", "")
                    + "&message="    + params.getOrDefault("message", "")
                    + "&orderId="    + params.get("orderId")
                    + "&orderInfo="  + params.getOrDefault("orderInfo", "")
                    + "&orderType="  + params.getOrDefault("orderType", "")
                    + "&partnerCode=" + params.get("partnerCode")
                    + "&payType="    + params.getOrDefault("payType", "")
                    + "&requestId="  + params.get("requestId")
                    + "&responseTime=" + params.getOrDefault("responseTime", "")
                    + "&resultCode=" + params.get("resultCode")
                    + "&transId="    + params.get("transId");

            String expectedSignature = hmacSHA256(rawSignature, secretKey);
            String receivedSignature = params.getOrDefault("signature", "");

            boolean valid = expectedSignature.equals(receivedSignature);
            if (!valid) {
                log.warn("[MoMo] Signature mismatch! Expected: {} | Received: {}", expectedSignature, receivedSignature);
            }
            return valid;
        } catch (Exception e) {
            log.error("[MoMo] Error verifying signature", e);
            return false;
        }
    }

    public Long extractOrderId(String extraData) {
        // extraData = "orderId=123"
        if (extraData == null || extraData.isBlank()) return null;
        try {
            String[] parts = extraData.split("=");
            if (parts.length == 2) return Long.parseLong(parts[1].trim());
        } catch (NumberFormatException e) {
            log.warn("[MoMo] Cannot parse orderId from extraData: {}", extraData);
        }
        return null;
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public record MomoPaymentResponse(
            int resultCode,
            String payUrl,
            String message,
            String momoOrderId
    ) {
        public boolean isSuccess() {
            return resultCode == 0;
        }
    }
}
