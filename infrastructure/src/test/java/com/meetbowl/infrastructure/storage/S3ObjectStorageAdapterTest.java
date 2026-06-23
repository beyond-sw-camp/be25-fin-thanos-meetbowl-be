package com.meetbowl.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.meetbowl.domain.storage.StoredObject;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3ObjectStorageAdapterTest {

    @Test
    void upload_puts_object_with_content_metadata() {
        S3Client client = Mockito.mock(S3Client.class);
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(client, "meetbowl-files");
        byte[] content = "document".getBytes();

        adapter.upload("personal-drive/user/file", "application/pdf", content);

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("meetbowl-files", request.bucket());
        assertEquals("personal-drive/user/file", request.key());
        assertEquals("application/pdf", request.contentType());
        assertEquals(content.length, request.contentLength());
    }

    @Test
    void download_reads_object_with_content_metadata() throws Exception {
        S3Client client = Mockito.mock(S3Client.class);
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(client, "meetbowl-files");
        byte[] content = "document".getBytes();
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(
                        new ResponseInputStream<>(
                                GetObjectResponse.builder()
                                        .contentType("text/plain")
                                        .contentLength((long) content.length)
                                        .build(),
                                AbortableInputStream.create(new ByteArrayInputStream(content))));

        StoredObject result = adapter.download("personal-drive/user/file");

        assertEquals("text/plain", result.contentType());
        assertEquals(content.length, result.contentLength());
        assertEquals("document", new String(result.content().readAllBytes()));
        ArgumentCaptor<GetObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObject(requestCaptor.capture());
        GetObjectRequest request = requestCaptor.getValue();
        assertEquals("meetbowl-files", request.bucket());
        assertEquals("personal-drive/user/file", request.key());
    }
}
