package com.example.docker_demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class utils {
    private static final Logger logger = LoggerFactory.getLogger(utils.class.getName());
    abstract public void logWeb3Action(String msg);
    public <T extends Response> T send(Request<?, T> request, String msg){
        logWeb3Action(msg);
        try {
            return request.send();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while " + msg + " the node. Is the node down?"
            );
        }
    }

    public static BigInteger toBigInteger(String value, String name) {
        try {
            return new BigInteger(value, 16);
        }
        catch (NumberFormatException e){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error while parsing " + name
            );
        }
    }

    public static BigDecimal toBigDecimal(String value, String name) {
        try {
            return new BigDecimal(value);
        }
        catch (NumberFormatException e){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error while parsing " + name
            );
        }
    }
}
