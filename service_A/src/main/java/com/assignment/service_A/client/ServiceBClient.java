package com.assignment.service_A.client;

import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "serviceBClient", url = "${service-b.url}")
public interface ServiceBClient {

    @PostMapping("/api/serviceB/process")
    ResponseDTO processTransaction(@RequestBody RequestDTO requestDTO);

    @PostMapping("/api/compensation/{requestId}")
    void compensateTransaction(@PathVariable String requestId);

    @GetMapping("/api/serviceB/status/{requestId}")
    ResponseDTO getTransactionStatus(@PathVariable("requestId") String requestId);
}