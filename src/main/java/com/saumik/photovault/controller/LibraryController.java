package com.saumik.photovault.controller;

import com.saumik.photovault.dto.ImageKitAssetResponse;
import com.saumik.photovault.dto.ImportPhotosRequest;
import com.saumik.photovault.dto.PhotoResponse;
import com.saumik.photovault.dto.StorageUsageResponse;
import com.saumik.photovault.entity.User;
import com.saumik.photovault.service.LibraryService;
import com.saumik.photovault.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;
    private final UserService userService;


    @GetMapping("/storage")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(libraryService.getStorageUsage(user));
    }

    @GetMapping("/imagekit-assets")
    public ResponseEntity<List<ImageKitAssetResponse>> listImageKitAssets(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(libraryService.listImportableAssets(user));
    }

    @PostMapping("/import")
    public ResponseEntity<List<PhotoResponse>> importAssets(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ImportPhotosRequest request
    ) {
        User user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(libraryService.importAssets(user, request));
    }
}
