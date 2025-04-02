package com.assignment.service_A.service;

import com.assignment.service_A.dto.RequestDTO;
import com.assignment.service_A.dto.ResponseDTO;


public interface UserRequestService {

    ResponseDTO processRequest(RequestDTO requestDTO);

}
