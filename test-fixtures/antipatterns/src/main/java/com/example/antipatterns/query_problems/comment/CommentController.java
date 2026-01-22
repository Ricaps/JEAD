package com.example.antipatterns.query_problems.comment;

import com.example.antipatterns.query_problems.post.Post;
import com.example.antipatterns.query_problems.post.PostDao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@RestController
public class CommentController
{
    private final PostDao postDao;
    private final CommentDao commentDao;
    private final CommentDaoWithEM commentDaoWithEM;

    @Inject
    public CommentController(PostDao postDao, CommentDao commentDao, CommentDaoWithEM commentDaoWithEM)
    {
        this.postDao = postDao;
        this.commentDao = commentDao;
        this.commentDaoWithEM = commentDaoWithEM;
    }

    @PostMapping("/comment/")
    public ResponseEntity<Long> createNewPost(@RequestParam(value = "text")String text, @RequestParam(value = "postId")Long postId)
    {
        Optional<Post> post = postDao.findById(postId);
        if (post.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }
        Comment comment = new Comment();
        comment.setPost(post.get());
        comment.setText(text);
        commentDao.save(comment);
        return ResponseEntity.ok(comment.getId());
    }

    @GetMapping("/comments/")
    public List<Comment> getComments()
    {
        return commentDao.findAllComments();
    }
}
