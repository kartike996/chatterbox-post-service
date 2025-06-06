package com.chatterbox.postservice.controller;

import com.chatterbox.postservice.kafka.PostEventProducer;
import com.chatterbox.postservice.model.Post;
import com.chatterbox.postservice.service.PostService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing post-related operations in the ChatterBox platform.
 * <p>
 * Provides endpoints for creating, updating, retrieving, and deleting posts. Integrates with Kafka
 * to publish post creation events for asynchronous communication with other microservices. Ensures
 * validation and consistent error handling for unsupported routes.
 * </p>
 *
 * <p><strong>Exposed Endpoints:</strong></p>
 * <ul>
 *     <li>POST /api/posts - Create a new post and publish a Kafka event.</li>
 *     <li>PUT /api/posts/{postId} - Update an existing post by its ID.</li>
 *     <li>GET /api/posts/user/{username} - Retrieve all posts created by a specific user.</li>
 *     <li>GET /api/posts/{postId} - Retrieve a single post by its ID.</li>
 *     <li>GET /api/posts - Retrieve all posts in the system.</li>
 *     <li>DELETE /api/posts/{postId} - Delete a post by its ID.</li>
 *     <li>DELETE /api/posts/user/{username} - Delete all posts by a specific user.</li>
 *     <li>DELETE /api/posts - Delete all posts (admin/cleanup purpose).</li>
 *     <li>Any other /api/posts/** path - Fallback for invalid or unsupported endpoints.</li>
 * </ul>
 *
 * <p>
 * This controller uses {@link PostService} for business logic and {@link PostEventProducer}
 * for emitting post creation events to Kafka.
 * </p>
 */
@RestController
@RequestMapping("/api/posts")
@AllArgsConstructor
@Log4j2
public class PostController {

    private final PostService postService;
    private final PostEventProducer postEventProducer;

    // Create a new post
    @PostMapping
    public ResponseEntity<String> createPost(@Valid @RequestBody Post post) {
        log.info("Creating post for username: {}", post.getUsername());
        String result = postService.createPost(post);
        postEventProducer.sendPostCreatedEvent(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Update an existing post
    @PutMapping("/{postId}")
    public ResponseEntity<String> updatePost(@PathVariable String postId, @Valid @RequestBody Post post) {
        log.info("Updating post with id: {}", postId);
        post.setPostId(postId);
        String result = postService.updatePost(post);
        return ResponseEntity.ok(result);
    }

    // Get all posts by user
    @GetMapping("/user/{username}")
    public ResponseEntity<List<Post>> getPostsByUsername(@PathVariable String username) {
        log.info("Fetching posts for username: {}", username);
        List<Post> posts = postService.getPostsByUsername(username);
        return ResponseEntity.ok(posts);
    }

    // Get post by ID
    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPostById(@PathVariable String postId) {
        log.info("Fetching post by id: {}", postId);
        Post post = postService.getPostByPostId(postId);
        return ResponseEntity.ok(post);
    }

    // Get post by ID
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        log.info("Fetching all posts");
        List<Post> post = postService.getAllPosts();
        return ResponseEntity.ok(post);
    }

    // Delete a single post by postId
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePostByPostId(@PathVariable String postId) {
        log.info("Deleting post with id: {}", postId);
        String response = postService.deletePostByPostId(postId);
        return ResponseEntity.ok(response);
    }

    // Delete all posts by a specific user
    @DeleteMapping("/user/{username}")
    public ResponseEntity<String> deletePostsByUsername(@PathVariable String username) {
        log.info("Deleting all posts by username: {}", username);
        String response = postService.deletePostByUsername(username);
        return ResponseEntity.ok(response);
    }

    // Delete all posts (admin/cleanup)
    @DeleteMapping
    public ResponseEntity<String> deleteAllPosts() {
        log.warn("Deleting all posts from the system");
        String response = postService.deleteAllPosts();
        return ResponseEntity.ok(response);
    }

    // Fallback for invalid endpoints under /api/posts
    @RequestMapping("**")
    public ResponseEntity<Map<String, Object>> handleInvalidPath() {
        log.warn("Invalid endpoint hit under /api/posts");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Invalid Endpoint");
        body.put("message", "The requested endpoint is not valid. Please check the URL.");

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
}
