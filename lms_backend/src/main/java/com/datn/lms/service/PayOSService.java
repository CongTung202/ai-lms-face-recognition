package com.datn.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class PayOSService {

    @Value("${payos.client-id:ef278d93-dc87-492b-80e5-50a31ca580b6}")
    private String clientId;

    @Value("${payos.api-key:87b589f1-048a-44bc-97ec-e7b886342b34}")
    private String apiKey;

    @Value("${payos.checksum-key:ef3972c57a254e7ae93d1f245df4b4d229d8bf665a9ff778ffef515d1f154ca1}")
    private String checksumKey;

    @Value("${payos.return-url:http://localhost:8080/portal/index.html?payment=success}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:8080/portal/index.html?payment=cancel}")
    private String cancelUrl;

    private static final String PAYOS_API_ENDPOINT = "https://api-merchant.payos.vn/v2/payment-requests";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo yêu cầu thanh toán tới cổng PayOS hoặc trả về thông tin VietQR chuẩn
     */
    public Map<String, Object> createPaymentLink(Long orderCode, int amount, String description, String buyerName, String buyerEmail) {
        Map<String, Object> responseData = new HashMap<>();
        
        // Chuẩn hóa mô tả (PayOS yêu cầu description tối đa 25 ký tự, không dấu)
        String safeDescription = cleanDescription(description, orderCode);

        // 1. Dựng chuỗi ký chữ ký số HMAC-SHA256 theo thứ tự alphabet: amount, cancelUrl, description, orderCode, returnUrl
        String dataToSign = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                amount, cancelUrl, safeDescription, orderCode, returnUrl);
        String signature = hmacSha256(checksumKey, dataToSign);

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("orderCode", orderCode);
        requestPayload.put("amount", amount);
        requestPayload.put("description", safeDescription);
        requestPayload.put("buyerName", buyerName != null ? buyerName : "Sinh Vien NTU");
        requestPayload.put("buyerEmail", buyerEmail != null ? buyerEmail : "student@daihocnguyentrai.edu.vn");
        requestPayload.put("cancelUrl", cancelUrl);
        requestPayload.put("returnUrl", returnUrl);
        requestPayload.put("signature", signature);

        log.info("[PayOS] Sending payment request for orderCode: {}, amount: {}, signature: {}", orderCode, amount, signature);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(PAYOS_API_ENDPOINT, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                String code = String.valueOf(body.get("code"));
                if ("00".equals(code) && body.get("data") != null) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    log.info("[PayOS] Successfully created payment link: {}", data.get("checkoutUrl"));
                    
                    String qrCodeStr = data.get("qrCode") != null ? data.get("qrCode").toString() : null;
                    String payosDesc = data.get("description") != null ? data.get("description").toString() : null;
                    
                    // Trích xuất nội dung chuyển khoản chính xác nhất từ chuỗi QR EMVCo chuẩn của PayOS
                    String extractedDesc = extractContentFromEmvco(qrCodeStr);
                    String finalDescription = (extractedDesc != null && !extractedDesc.isBlank())
                            ? extractedDesc
                            : (payosDesc != null && !payosDesc.isBlank() ? payosDesc : safeDescription);

                    log.info("[PayOS] Raw PayOS desc: '{}', Extracted from QR: '{}', Final description used: '{}'",
                            payosDesc, extractedDesc, finalDescription);

                    responseData.put("success", true);
                    responseData.put("orderCode", orderCode);
                    responseData.put("amount", amount);
                    responseData.put("description", finalDescription);
                    responseData.put("accountNumber", data.get("accountNumber"));
                    responseData.put("accountName", data.get("accountName"));
                    responseData.put("bin", data.get("bin"));
                    responseData.put("checkoutUrl", data.get("checkoutUrl"));
                    responseData.put("qrCode", qrCodeStr);
                    responseData.put("paymentLinkId", data.get("paymentLinkId"));
                    return responseData;
                } else {
                    log.warn("[PayOS] Response code was not 00: {}", body);
                }
            }
        } catch (Exception ex) {
            log.error("[PayOS] Call API error: {}. Fallback to VietQR direct generator.", ex.getMessage());
        }

        // 2. Chế độ Fallback / Sandbox Offline: Tự động sinh VietQR chuẩn ngân hàng quốc gia
        String fallbackBin = "970422"; // MBBank
        String fallbackAccountNo = "30334402527";
        String fallbackAccountName = "NGUYEN CONG TUNG";
        String vietQrUrl = String.format("https://api.vietqr.io/image/%s-%s-compact2.jpg?amount=%d&addInfo=%s&accountName=%s",
                fallbackBin,
                fallbackAccountNo,
                amount,
                URLEncoder.encode(safeDescription, StandardCharsets.UTF_8),
                URLEncoder.encode(fallbackAccountName, StandardCharsets.UTF_8)
        );

        responseData.put("success", true);
        responseData.put("isFallback", true);
        responseData.put("orderCode", orderCode);
        responseData.put("amount", amount);
        responseData.put("description", safeDescription);
        responseData.put("bin", fallbackBin);
        responseData.put("bankName", "MBBank (Quân Đội)");
        responseData.put("accountNumber", fallbackAccountNo);
        responseData.put("accountName", fallbackAccountName);
        responseData.put("qrCode", vietQrUrl);
        responseData.put("checkoutUrl", "https://pay.payos.vn/web/" + orderCode);
        responseData.put("paymentLinkId", "PAYOS_" + orderCode);
        return responseData;
    }

    /**
     * Trích xuất nội dung chuyển khoản từ chuỗi chuẩn EMVCo VietQR (Tag 62 -> Sub-tag 08 hoặc 05/01)
     */
    public static String extractContentFromEmvco(String emvcoQr) {
        if (emvcoQr == null || emvcoQr.length() < 12) return null;
        try {
            int i = 0;
            int n = emvcoQr.length();
            while (i + 4 <= n) {
                String tag = emvcoQr.substring(i, i + 2);
                int len = Integer.parseInt(emvcoQr.substring(i + 2, i + 4));
                i += 4;
                if (i + len > n) break;
                String val = emvcoQr.substring(i, i + len);
                if ("62".equals(tag)) {
                    // Tag 62: Additional Data Field Template
                    int j = 0;
                    int m = val.length();
                    while (j + 4 <= m) {
                        String subTag = val.substring(j, j + 2);
                        int subLen = Integer.parseInt(val.substring(j + 2, j + 4));
                        j += 4;
                        if (j + subLen > m) break;
                        String subVal = val.substring(j, j + subLen);
                        if ("08".equals(subTag)) { // VietQR Purpose of Transaction
                            return subVal.trim();
                        }
                        j += subLen;
                    }
                    // Fallback trong trường hợp gắn vào tag 05 (Reference) hoặc 01 (Bill Number)
                    j = 0;
                    while (j + 4 <= m) {
                        String subTag = val.substring(j, j + 2);
                        int subLen = Integer.parseInt(val.substring(j + 2, j + 4));
                        j += 4;
                        if (j + subLen > m) break;
                        String subVal = val.substring(j, j + subLen);
                        if ("05".equals(subTag) || "01".equals(subTag)) {
                            return subVal.trim();
                        }
                        j += subLen;
                    }
                }
                i += len;
            }
        } catch (Exception e) {
            log.warn("[PayOS] Could not parse EMVCo QR code: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Tra cứu thông tin đơn hàng thanh toán trực tiếp từ cổng PayOS qua API chuẩn
     * GET https://api-merchant.payos.vn/v2/payment-requests/{orderCode}
     */
    public Map<String, Object> getPaymentLinkInformation(Long orderCode) {
        if (orderCode == null) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = PAYOS_API_ENDPOINT + "/" + orderCode;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                if ("00".equals(String.valueOf(body.get("code"))) && body.get("data") != null) {
                    log.info("[PayOS] Query payment link info for orderCode {} successful: status = {}",
                            orderCode, ((Map) body.get("data")).get("status"));
                    return (Map<String, Object>) body.get("data");
                }
            }
        } catch (Exception e) {
            log.warn("[PayOS] Query payment status for orderCode {} error: {}", orderCode, e.getMessage());
        }
        return null;
    }

    /**
     * Xác thực chữ ký webhook từ PayOS
     */
    public boolean verifyWebhookSignature(Map<String, Object> webhookPayload) {
        if (webhookPayload == null) return false;
        try {
            String incomingSignature = (String) webhookPayload.get("signature");
            Object dataObj = webhookPayload.get("data");
            if (incomingSignature == null || dataObj == null) {
                return false;
            }

            Map<String, Object> dataMap;
            if (dataObj instanceof Map) {
                dataMap = (Map<String, Object>) dataObj;
            } else {
                dataMap = objectMapper.readValue(objectMapper.writeValueAsString(dataObj), Map.class);
            }

            // Sắp xếp các key theo alphabet
            List<String> sortedKeys = new ArrayList<>(dataMap.keySet());
            Collections.sort(sortedKeys);

            StringBuilder sb = new StringBuilder();
            for (String key : sortedKeys) {
                Object val = dataMap.get(key);
                if (val != null) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(key).append("=").append(val.toString());
                }
            }

            String calculatedSignature = hmacSha256(checksumKey, sb.toString());
            boolean isValid = calculatedSignature.equalsIgnoreCase(incomingSignature);
            log.info("[PayOS Webhook] Signature verification result: {} (Calc: {}, Recv: {})",
                    isValid, calculatedSignature, incomingSignature);
            return isValid;
        } catch (Exception e) {
            log.error("[PayOS Webhook] Verification exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Tính toán chữ ký HMAC-SHA256 chuẩn
     */
    public static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC-SHA256", e);
        }
    }

    /**
     * Làm sạch chuỗi description (tối đa 25 ký tự, không dấu)
     */
    private String cleanDescription(String raw, Long orderCode) {
        String base = "HOCPHI " + orderCode;
        if (base.length() > 25) {
            base = base.substring(0, 25);
        }
        return base;
    }
}
