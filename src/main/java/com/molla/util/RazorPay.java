package com.molla.util;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RazorPay {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    public Map<String, Object> createOrder(Double amount, String currency, String receipt) throws RazorpayException {
        // Logic to create order using RazorPay API
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.intValue() * 100); // amount in the smallest currency unit (paise)
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);
        Order order = razorpayClient.orders.create(orderRequest);
        
        // Convert Order to Map for response
        Map<String, Object> orderResponse = new HashMap<>();
        orderResponse.put("id", order.get("id"));
        orderResponse.put("amount", order.get("amount"));
        orderResponse.put("currency", order.get("currency"));
        orderResponse.put("receipt", order.get("receipt"));
        orderResponse.put("status", order.get("status"));
        orderResponse.put("created_at", order.get("created_at"));
        
        return orderResponse;
    }

}
