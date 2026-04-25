package api.giybat.uz.api.giybat.uz.controller;

import api.giybat.uz.api.giybat.uz.dto.AppResponse;
import api.giybat.uz.api.giybat.uz.dto.post.*;
import api.giybat.uz.api.giybat.uz.dto.profile.ProfileSatusDTO;
import api.giybat.uz.api.giybat.uz.enums.AppLanguage;
import api.giybat.uz.api.giybat.uz.service.PostService;
import api.giybat.uz.api.giybat.uz.util.PageUtil;
import api.giybat.uz.api.giybat.uz.util.SpringSecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@Slf4j
@Tag(name = "PostController", description = "API list for working with Post")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("")
    @Operation(summary = "Create Post", description = "Api used for Post creation ")
    public ResponseEntity<PostDTO> create(@Valid @RequestBody PostCreateDTO dto){
        return ResponseEntity.ok(postService.create(dto));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get Post List", description = "Api used for Get All Profile Post List")
    public ResponseEntity<Page<PostDTO>> postListByProfile(@RequestParam(value = "page", defaultValue = "1") int page,
                                                           @RequestParam(value = "size", defaultValue = "3") int size){
        return ResponseEntity.ok(postService.getProfilePostList(PageUtil.page(page),size));
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get Post by ID", description = "Api used for Get Post by id")
    public ResponseEntity<PostDTO> postById(@PathVariable("id") String id){
        return ResponseEntity.ok(postService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Post", description = "Api used for Post update")
    public ResponseEntity<PostDTO> update(@PathVariable("id") String id,
                                            @Valid @RequestBody PostCreateDTO dto){
        return ResponseEntity.ok(postService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Post", description = "Api used for Post Delete")
    public ResponseEntity<AppResponse<String>> delete(@PathVariable("id") String id){
        return ResponseEntity.ok(postService.delete(id));
    }

    @PostMapping("/public/filter")
    @Operation(summary = "Post public filter", description = "Api used for Post filter ")
    public ResponseEntity<Page<PostDTO>> filter(@Valid @RequestBody PostFilterDTO dto,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "size", defaultValue = "10") int size){
        return ResponseEntity.ok(postService.filter(dto, PageUtil.page(page), size));
    }

    @PostMapping("/public/similar")
    @Operation(summary = "Get similar Post list", description = "Api used for getting for similar post list ")
    public ResponseEntity<List<PostDTO>> similarPostList(@Valid @RequestBody SimilarPostListDTO dto){
        return ResponseEntity.ok(postService.getSimilarPostList(dto));
    }

    @PostMapping("/admin-post-list/filter")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Post list filter for admin", description = "Api used for filtering for admin post list ")
    public ResponseEntity<PageImpl<PostDTO>> filter(@Valid @RequestBody PostAdminFilterDTO dto,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "size", defaultValue = "10") int size){
        return ResponseEntity.ok(postService.adminFilter(dto, PageUtil.page(page), size));
    }
}
