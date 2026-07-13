package com.ranjan.sharenow.dto;

import java.io.InputStream;

public record MultipartFile(

        String filename,

        InputStream inputStream

) {
}