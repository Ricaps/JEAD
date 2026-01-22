package com.example.antipatterns.query_problems.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class PostController
{
    private final PostDao postDao;

    @Inject
    public PostController(PostDao postDao)
    {
        this.postDao = postDao;
    }

    @PostMapping("/post/")
    public Long createNewPost(@RequestParam(value = "text")String text, @RequestParam(value = "title")String title)
    {
        Post post = new Post();
        post.setText(text);
        post.setTitle(title);
        post = postDao.save(post);
        return post.getId();
    }
}
